package com.zoho.eservemcp.tools;

import com.zoho.eservemcp.dto.response.HolidayResponse;
import com.zoho.eservemcp.dto.response.ZohoBookedAndBalanceReport;
import com.zoho.eservemcp.dto.response.ZohoHolidaysResponse;
import com.zoho.eservemcp.dto.response.ZohoLeaveRecordsResponseV2;
import com.zoho.eservemcp.entity.Employee;
import com.zoho.eservemcp.exception.DomainValidationException;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class ZohoPayrollMcpTools {

    private static final Logger log = LoggerFactory.getLogger(ZohoPayrollMcpTools.class);
    private static final String ALLOWED_DOMAIN = "eservecloud.in";

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
     * Download employee payslip as PDF, return saved file path.
     */
    @Tool(description = "Download payslip PDF for an employee from Zoho Payroll and save, return file path.")
    public String downloadPayslipAsPdfAndReturnPath(
            @ToolParam(description = "Employee email (@eservecloud.in domain required)") String email,
            @ToolParam(description = "Pay period ID from Zoho") String payPeriodId) {
        try {
            Employee employee = validateAndFetchEmployee(email);
            if (payPeriodId == null || payPeriodId.trim().isEmpty()) {
                throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "PayPeriodId cannot be empty");
            }
            byte[] pdfBytes = zohoApiService.downloadPayslip(employee.getZohoErecNo(), payPeriodId);
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "No PDF downloaded from Zoho.");
            }
            String safeEmail = email.replaceAll("@.*$", "").replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = "payslip_" + safeEmail + "_" + payPeriodId + ".pdf";
            Path path = Paths.get("/tmp/" + fileName);
            Files.write(path, pdfBytes);
            return path.toString();
        } catch (McpBaseException ex) {
            log.error("Payslip PDF error: {}", ex.getMessage());
            throw ex;
        } catch (IOException e) {
            log.error("Failed to write PDF to disk: {}", e.getMessage(), e);
            throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "Failed to write PDF file to disk", e);
        } catch (Exception ex) {
            log.error("Unexpected error during payslip PDF download: {}", ex.getMessage(), ex);
            throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "Error downloading payslip", ex);
        }
    }

    /**
     * Fetch leave records using Zoho People API V2 for multiple employees.
     */
    @Tool(description = "Fetch leave records by employee(s) from Zoho People V2. Maps Zoho recordID to leave record DTO.")
    public Map<String, ZohoLeaveRecordsResponseV2.LeaveRecord> getEmployeeLeaveRecords(
            @ToolParam(description = "Zoho People Org portalID (ZSOID)") String portalID,
            @ToolParam(description = "From date (yyyy-MM-dd or org date format)") String from,
            @ToolParam(description = "To date (yyyy-MM-dd or org date format)") String to,
            @ToolParam(description = "Employee Erecno list") List<String> employeeIds,
            @ToolParam(description = "Date format for Zoho API") String dateFormat) {
        try {
            if (portalID == null || portalID.isEmpty())
                throw new McpToolException("getEmployeeLeaveRecords", "portalID required");
            if (employeeIds == null || employeeIds.isEmpty())
                throw new McpToolException("getEmployeeLeaveRecords", "employeeIds required (at least one needed)");
            ZohoLeaveRecordsResponseV2 records = zohoApiService.fetchLeaveRecords(portalID, from, to, employeeIds, dateFormat);
            if (records == null || records.records() == null || records.records().isEmpty())
                throw new McpToolException("getEmployeeLeaveRecords", "No leave records found.");
            return records.records();
        } catch (McpToolException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in getEmployeeLeaveRecords", e);
            throw new McpToolException("getEmployeeLeaveRecords", "Error fetching leave records", e);
        }
    }

    /**
     * Fetch booked and balance leave report.
     */
    @Tool(description = "Fetch booked/balance leave report for employees. Returns leave entitlements/statistics.")
    public ZohoBookedAndBalanceReport getBookedAndBalanceReport(
            @ToolParam(description = "From date") String from,
            @ToolParam(description = "To date") String to,
            @ToolParam(description = "Unit (Day|Hour)") String unit,
            @ToolParam(description = "Employee Erecno list") List<String> employeeIds,
            @ToolParam(description = "Optional leave type IDs") List<String> leaveTypeIds) {
        try {
            if (employeeIds == null || employeeIds.isEmpty())
                throw new McpToolException("getBookedAndBalanceReport", "employeeIds required");
            ZohoBookedAndBalanceReport report = zohoApiService.fetchBookedAndBalance(from, to, unit, employeeIds, leaveTypeIds);
            if (report == null || report.report() == null || report.report().isEmpty())
                throw new McpToolException("getBookedAndBalanceReport", "No booked/balance data found.");
            return report;
        } catch (McpToolException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in getBookedAndBalanceReport", e);
            throw new McpToolException("getBookedAndBalanceReport", "Error fetching BookedAndBalance report", e);
        }
    }

    /**
     * Fetch holidays from Zoho (V2). If location not provided, default to Bangalore. If upcoming is not provided, use false.
     */
    @Tool(description = """
        Get list of holidays for specified or default location/shift/employee/date-range from Zoho (V2).
        Defaults: location = Bangalore if not provided, upcoming = false.
        """)
    public List<HolidayResponse> getEmployeeHolidays(
            @ToolParam(description = "Location name as per Zoho (default: Bangalore)") String location,
            @ToolParam(description = "Shift name as per Zoho") String shift,
            @ToolParam(description = "Employee Zoho Erecno or email") String employee,
            @ToolParam(description = "True for only upcoming holidays, otherwise all; default is false") Boolean upcoming,
            @ToolParam(description = "From date (dd-MMM-yyyy or org format)") String from,
            @ToolParam(description = "To date (dd-MMM-yyyy or org format)") String to,
            @ToolParam(description = "Date format, e.g. dd-MMM-yyyy") String dateFormat
    ) {
        try {
            String resolvedLocation = (location == null || location.isBlank()) ? "Bangalore" : location;
            boolean resolvedUpcoming = (upcoming == null) ? false : upcoming;
            ZohoHolidaysResponse response = zohoApiService.fetchHolidays(resolvedLocation, shift, employee, resolvedUpcoming, from, to, dateFormat);
            if (response == null || response.getData() == null || response.getData().isEmpty())
                throw new McpToolException("getEmployeeHolidays", "No holidays found for given query.");
            return responseMapper.mapToHolidayResponses(response.getData());
        } catch (McpToolException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in getEmployeeHolidays", e);
            throw new McpToolException("getEmployeeHolidays", "Unexpected error fetching holidays", e);
        }
    }

    /**
     * Fetch Zoho ErecNo by corporate email and update employee record in DB.
     */
    @Tool(description = "Fetch and update employee userErecNo (Zoho employeeId) by corporate email.")
    public String fetchAndSaveZohoErecNo(@ToolParam(description = "Employee email (@eservecloud.in domain required)") String email) {
        try {
            if (email == null || !email.contains("@")) {
                throw new McpToolException("fetchAndSaveZohoErecNo", "A valid email is required.");
            }
            String erecNo = zohoApiService.fetchZohoMailZuidByEmail(email);
            if (erecNo == null) throw new McpToolException("fetchAndSaveZohoErecNo", "Zoho Employee Erecno not found for email: " + email);

            Employee emp = employeeRepository.findByEmailAndIsActive(email, true)
                    .orElseThrow(() -> new EmployeeNotFoundException(email, false));
            emp.setZohoErecNo(erecNo);
            employeeRepository.save(emp);
            return erecNo;
        } catch (McpBaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch/save Zoho ErecNo: {}", e.getMessage(), e);
            throw new McpToolException("fetchAndSaveZohoErecNo", "Unexpected error fetching/saving zohoErecNo", e);
        }
    }

    // Validate email format and domain, fetch active employee or throw
    private Employee validateAndFetchEmployee(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw InputValidationException.emptyField("email");
        }
        if (!email.contains("@")) {
            throw InputValidationException.invalidEmail(email);
        }
        if (!email.endsWith("@" + ALLOWED_DOMAIN)) {
            throw new DomainValidationException(email, ALLOWED_DOMAIN);
        }
        return employeeRepository.findByEmailAndIsActive(email, true)
                .orElseThrow(() -> new EmployeeNotFoundException(email, false));
    }
}