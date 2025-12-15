package com.zoho.eservemcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.zoho.eservemcp.dto.response.HolidayResponse;
import com.zoho.eservemcp.dto.response.ZohoBookedAndBalanceReport;
import com.zoho.eservemcp.dto.response.ZohoHolidaysResponse;
import com.zoho.eservemcp.dto.response.ZohoLeaveRecordsResponseV2;
import com.zoho.eservemcp.entity.Employee;
import com.zoho.eservemcp.exception.EmployeeNotFoundException;
import com.zoho.eservemcp.exception.InputValidationException;
import com.zoho.eservemcp.exception.McpBaseException;
import com.zoho.eservemcp.exception.McpToolException;
import com.zoho.eservemcp.repository.EmployeeRepository;
import com.zoho.eservemcp.service.ZohoPayrollApiService;
import com.zoho.eservemcp.utils.PayrollResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ZohoPayrollMcpTools {

    private static final Logger log = LoggerFactory.getLogger(ZohoPayrollMcpTools.class);

    private final ZohoPayrollApiService zohoApiService;
    private final EmployeeRepository employeeRepository;
    @Autowired
    private final PayrollResponseMapper responseMapper;

    public ZohoPayrollMcpTools(
            ZohoPayrollApiService zohoApiService,
            EmployeeRepository employeeRepository,
            PayrollResponseMapper responseMapper) {
        this.zohoApiService = zohoApiService;
        this.employeeRepository = employeeRepository;
        this.responseMapper = responseMapper;
    }

    /**
     * Fetch leave records using Zoho People API V2 (sid-based).
     */
    @Tool(description = """
    Fetch leave records for employees from Zoho People V2.
    Provide list of employee sids (UUIDs); their ZohoErecNos are fetched from DB.
    """)
    public Map<String, ZohoLeaveRecordsResponseV2.LeaveRecord> getEmployeeLeaveRecords(
            @ToolParam(description = "From date (yyyy-MM-dd); default is start date of the current year") String from,
            @ToolParam(description = "To date (yyyy-MM-dd); default is end date of the current year") String to,
            @ToolParam(description = "List of employee sids (UUIDs); Required") List<UUID> employeeSids,
            @ToolParam(description = "Date format, e.g. dd-MMM-yyyy") String dateFormat) {
        try {
            if (employeeSids == null || employeeSids.isEmpty())
                throw new McpToolException("getEmployeeLeaveRecords", "At least one employee sid is required");

            List<String> erecnoList = employeeSids.stream()
                    .map(this::fetchErecNoFromDb)
                    .toList();

            ZohoLeaveRecordsResponseV2 records = zohoApiService.fetchLeaveRecords(from, to, erecnoList, dateFormat);
            if (records == null || records.records() == null || records.records().isEmpty())
                throw new McpToolException("getEmployeeLeaveRecords", "No leave records found.");
            return records.records();
        } catch (Exception e) {
            log.error("Error in getEmployeeLeaveRecords", e);
            throw new McpToolException("getEmployeeLeaveRecords", "Error fetching leave records", e);
        }
    }

    /**
     * Fetch Zoho Booked/Balance report (sid-based).
     */
    @Tool(description = """
    Fetch Zoho Booked/Balance report (leave entitlements/statistics) for employees.
    Provide list of employee sids (UUIDs); their ZohoErecNos are fetched from DB.
    """)
    public ZohoBookedAndBalanceReport getBookedAndBalanceReport(
            @ToolParam(description = "Report FROM date (yyyy-MM-dd); default is start date of the current year") String from,
            @ToolParam(description = "Report TO date (yyyy-MM-dd); default is end date of the current year") String to,
            @ToolParam(description = "Unit for report, e.g. 'Day' or 'Hour'; default Day") String unit,
            @ToolParam(description = "List of employee sids (UUIDs); Required") List<UUID> employeeSids) {
        try {
            if (from == null || from.isBlank() || to == null || to.isBlank() || unit == null || unit.isBlank())
                throw new McpToolException("getBookedAndBalanceReport", "from, to, and unit are required.");
            if (employeeSids == null || employeeSids.isEmpty())
                throw new McpToolException("getBookedAndBalanceReport", "At least one employee sid must be specified.");

            List<String> erecnoList = employeeSids.stream()
                    .map(this::fetchErecNoFromDb)
                    .toList();

            ZohoBookedAndBalanceReport report = zohoApiService.fetchLeaveBookedAndBalance(from, to, unit, erecnoList);
            if (report == null || report.report() == null || report.report().isEmpty())
                throw new McpToolException("getBookedAndBalanceReport", "No booked/balance data found.");
            return report;
        } catch (Exception e) {
            log.error("Error in getBookedAndBalanceReport", e);
            throw new McpToolException("getBookedAndBalanceReport", "Error fetching booked/balance report", e);
        }
    }

    /**
     * Fetch holidays.
     */
    @Tool(description = """
        Get list of holidays for specified or default location/shift/employee/date-range from Zoho (V2).
        Provide user sid to get holiday list.
        """)
    public List<HolidayResponse> getEmployeeHolidays(
            @ToolParam(description = "True for only upcoming holidays; default is false", required = false) Boolean upcoming,
            @ToolParam(description = "From date (dd-MMM-yyyy or org format); default start date of current year", required = false) String from,
            @ToolParam(description = "To date (dd-MMM-yyyy or org format); default end date of current year", required = false) String to,
            @ToolParam(description = "Date format, e.g. dd-MMM-yyyy", required = false) String dateFormat
    ) {
        try {
            boolean resolvedUpcoming = (upcoming == null) ? false : upcoming;
            ZohoHolidaysResponse response = zohoApiService.fetchHolidays(resolvedUpcoming, from, to, dateFormat);
            return responseMapper.mapToHolidayResponses(response.getData());
        } catch (Exception e) {
            log.error("Error in getEmployeeHolidays", e);
            throw new McpToolException("getEmployeeHolidays", "Unexpected error fetching holidays", e);
        }
    }

    /**
     * Fetch and return employee ZohoErecNo (employeeId) by sid (UUID).
     * If not present in DB, fetch from Zoho Mail API using employee's email,
     * update DB, and return it.
     */
    @Tool(description = "Fetch employee ZohoErecNo (employeeId) by sid (UUID). If not in DB, it fetches from Zoho and updates the record.")
    public Map<String, String> fetchAndSaveZohoErecNo(@ToolParam(description = "Employee sid (UUID); Required") UUID sid) {
        Map<String, String> response = new HashMap<>();
        try {
            Employee emp = employeeRepository.findBySid(sid)
                    .filter(Employee::getIsActive)
                    .orElseThrow(() -> new EmployeeNotFoundException("Employee not found for sid: " + sid, false));

            // If already present, return as STRING
            if (emp.getZohoErecNo() != null && !emp.getZohoErecNo().isBlank()) {
                response.put("zohoErecNo", emp.getZohoErecNo());
                return response;
            }
            String email = emp.getEmail();
            if (email == null || email.isBlank()) {
                throw new McpToolException("fetchAndSaveZohoErecNo", "Employee email not found for sid: " + sid);
            }
            String erecNo = zohoApiService.fetchZohoIdByEmail(email);
            if (erecNo == null || erecNo.isBlank()) {
                throw new McpToolException("fetchAndSaveZohoErecNo", "Zoho Employee ErecNo not found for email: " + email);
            }
            emp.setZohoErecNo(erecNo);
            employeeRepository.save(emp);
            log.info("Fetched and updated ZohoErecNo for sid {}: {}", sid, erecNo);
            response.put("zohoErecNo", erecNo);
            return response;
        } catch (McpBaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch/save Zoho ErecNo for sid {}: {}", sid, e.getMessage(), e);
            throw new McpToolException("fetchAndSaveZohoErecNo", "Unexpected error fetching/saving zohoErecNo for sid: " + sid, e);
        }
    }

    @Tool(description = "Fetch user leave summary (taken, available, type, leave name).")
    public Map<String, Object> getUserLeaveSummary(@ToolParam(description = "Employee sid; Required") UUID employeeSid,
            @ToolParam(description = "Report end date (dd-MMM-yyyy); Optional", required = false) String to
    ) {
        try {
            String erecNo = fetchErecNoFromDb(employeeSid);
            if(erecNo == null || erecNo.isBlank()) {
                throw new McpToolException("getUserLeaveSummary", "Zoho ErecNo not found for employee sid: " + employeeSid);
            }
            JsonNode raw = zohoApiService.fetchUserLeaveReport(erecNo, to);

            Map<String, Object> result = new HashMap<>();
            result.put("employeeName", raw.path("employeeName").asText());

            List<Map<String, Object>> leaves = new java.util.ArrayList<>();

            for (JsonNode lt : raw.path("leavetypes")) {
                Map<String, Object> item = new HashMap<>();
                item.put("leaveType", lt.path("leavetypeName").asText());
                item.put("taken", lt.path("taken").asInt());
                item.put("available", lt.path("available").asInt());
                item.put("type", lt.path("type").asText());
                leaves.add(item);
            }

            result.put("leaves", leaves);
            log.info("Fetched user leave summary for sid {}: {}", employeeSid, result);
            return result;
        } catch (Exception e) {
                throw new McpToolException("getUserLeaveSummary", "Error fetching user leave summary", e);
        }
    }

    /**
     * Utility: Fetch Employee ErecNo from DB using sid.
     * If not present in DB, automatically fetches from Zoho API and updates DB.
     */
    private String fetchErecNoFromDb(UUID sid) {
        Employee emp = validateAndFetchEmployee(sid);
        String erecNo = emp.getZohoErecNo();
        if (erecNo == null || erecNo.isBlank()) {
            log.info("ZohoErecNo not found in DB for sid {}. Fetching from Zoho API...", sid);
            Map<String, String> result = fetchAndSaveZohoErecNo(sid);
            erecNo = result != null ? result.get("zohoErecNo") : null;
        }
        return erecNo;
    }

    /**
     * Validate and fetch employee by sid.
     */
    private Employee validateAndFetchEmployee(UUID sid) {
        if (sid == null) {
            throw InputValidationException.emptyField("sid");
        }
        return employeeRepository.findBySid(sid)
                .filter(Employee::getIsActive)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found for sid: " + sid, false));
    }
}