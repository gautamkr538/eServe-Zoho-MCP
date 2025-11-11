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
     * Always resolves userErecNo from the provided email (DB/Zoho lookup).
     */
    @Tool(description = "Download payslip PDF for an employee from Zoho Payroll and save, return file path. Only an email is required.")
    public String downloadPayslipAsPdfAndReturnPath(@ToolParam(description = "Employee email (@eservecloud.in domain required)") String email,
            @ToolParam(description = "Pay period ID from Zoho") String payPeriodId) {
        try {
            String userErecNo = resolveAndUpdateErecNoForEmail(email);
            if (payPeriodId == null || payPeriodId.trim().isEmpty()) {
                throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "PayPeriodId cannot be empty");
            }
            byte[] pdfBytes = zohoApiService.downloadPayslip(userErecNo, payPeriodId);
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
        } catch (IOException ex) {
            log.error("Failed to write PDF to disk: {}", ex.getMessage(), ex);
            throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "Failed to write PDF file to disk", ex);
        } catch (Exception ex) {
            log.error("Unexpected error during payslip PDF download: {}", ex.getMessage(), ex);
            throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "Error downloading payslip", ex);
        }
    }

    /**
     * Fetch leave records using Zoho People API V2.
     * Provide one or more employee emails; their ZohoErecNos will be resolved and stored in DB if needed.
     * All other params are passed through to the leave records API.
     */
    @Tool(description = """
    Fetch leave records for employees from Zoho People V2.
    Provide corporate email(s) instead of Erecno(s); mapping and DB update is automatic.
    """)
    public Map<String, ZohoLeaveRecordsResponseV2.LeaveRecord> getEmployeeLeaveRecords(
            @ToolParam(description = "From date (yyyy-MM-dd or org date format)") String from,
            @ToolParam(description = "To date (yyyy-MM-dd or org date format)") String to,
            @ToolParam(description = "List of corporate emails for employees") List<String> employeeEmails,
            @ToolParam(description = "Date format, e.g. dd-MMM-yyyy") String dateFormat) {
        try {
            if (employeeEmails == null || employeeEmails.isEmpty())
                throw new McpToolException("getEmployeeLeaveRecords", "At least one employee email is required");
            List<String> erecnoList = employeeEmails.stream().map(this::resolveAndUpdateErecNoForEmail).toList();
            ZohoLeaveRecordsResponseV2 records = zohoApiService.fetchLeaveRecords(from, to, erecnoList, dateFormat);
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
     * Fetch Zoho Booked/Balance report (leave entitlements/statistics) for employees.
     * Always accepts only a list of corporate emails, NOT Zoho Erecnos.
     * Required params: from (start date), to (end date), unit (Day/Hour), employeeEmails.
     * All Zoho Erecnos are transparently resolved and updated in DB if needed.
     */
    @Tool(description = """
    Fetch Zoho Booked/Balance report (leave entitlements/statistics) for employees.
    Provide employeeEmails (corporate emails); Zoho Erecno lookup and DB update is automatic.
    Required: from (start date), to (end date), unit (Day/Hour), employeeEmails.
    """)
    public ZohoBookedAndBalanceReport getBookedAndBalanceReport(
            @ToolParam(description = "Report FROM date (e.g. start of leave year, yyyy-MM-dd)") String from,
            @ToolParam(description = "Report TO date (e.g. current date, yyyy-MM-dd)") String to,
            @ToolParam(description = "Unit for report, e.g. 'Day' or 'Hour'") String unit,
            @ToolParam(description = "List of employee corporate emails") List<String> employeeEmails) {
        try {
            if (from == null || from.isBlank() || to == null || to.isBlank() || unit == null || unit.isBlank())
                throw new McpToolException("getBookedAndBalanceReport", "from, to, unit are required.");
            if (employeeEmails == null || employeeEmails.isEmpty())
                throw new McpToolException("getBookedAndBalanceReport", "At least one employee email must be specified.");
            List<String> erecnoList = employeeEmails.stream().map(this::resolveAndUpdateErecNoForEmail).toList();
            ZohoBookedAndBalanceReport report = zohoApiService.fetchBookedAndBalance(from, to, unit, erecnoList);
            if (report == null || report.report() == null || report.report().isEmpty())
                throw new McpToolException("getBookedAndBalanceReport", "No booked/balance data found.");
            return report;
        } catch (McpToolException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in getBookedAndBalanceReport", e);
            throw new McpToolException("getBookedAndBalanceReport", "Error fetching booked/balance report", e);
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
            @ToolParam(description = "Employee corporate email") String employee,
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
     * Utility: Resolve or update Employee zohoErecNo for a corporate email, else throw agent-friendly error.
     */
    private String resolveAndUpdateErecNoForEmail(String email) {
        try {
            Employee emp = employeeRepository.findByEmailAndIsActive(email, true)
                    .orElseThrow(() -> new EmployeeNotFoundException(email, false));
            if (emp.getZohoErecNo() != null && !emp.getZohoErecNo().isBlank()) {
                return emp.getZohoErecNo();
            }
            String erecno = zohoApiService.fetchZohoMailZuidByEmail(email);
            if (erecno == null || erecno.isBlank()) {
                throw new McpToolException("resolveAndUpdateErecNoForEmail", "Could not resolve ZohoErecNo (employeeId) for: " + email);
            }
            emp.setZohoErecNo(erecno);
            employeeRepository.save(emp);
            return erecno;
        } catch (McpBaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to resolve/store ZohoErecNo for email {}: {}", email, e.getMessage(), e);
            throw new McpToolException("resolveAndUpdateErecNoForEmail", "Error resolving/saving employee zohoErecNo", e);
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