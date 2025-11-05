package com.zoho.eservemcp.tools;

import com.zoho.eservemcp.dto.response.LeaveRecordsResponse;
import com.zoho.eservemcp.dto.response.PayslipResponse;
import com.zoho.eservemcp.dto.response.ZohoLeaveReportResponse;
import com.zoho.eservemcp.dto.response.ZohoPayrollReportResponse;
import com.zoho.eservemcp.entity.Employee;
import com.zoho.eservemcp.exception.*;
import com.zoho.eservemcp.repository.EmployeeRepository;
import com.zoho.eservemcp.service.ZohoPayrollApiService;
import com.zoho.eservemcp.utils.PayrollResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Service
public class ZohoPayrollMcpTools {

    private static final Logger log = LoggerFactory.getLogger(ZohoPayrollMcpTools.class);
    private static final String ALLOWED_DOMAIN = "eservecloud.in";

    private final ZohoPayrollApiService zohoApiService;
    private final EmployeeRepository employeeRepository;
    private final PayrollResponseMapper responseMapper;

    public ZohoPayrollMcpTools(
            ZohoPayrollApiService zohoApiService,
            EmployeeRepository employeeRepository,
            PayrollResponseMapper responseMapper) {
        this.zohoApiService = zohoApiService;
        this.employeeRepository = employeeRepository;
        this.responseMapper = responseMapper;
    }

    @Tool(description = "Fetch employee payslip for a specific pay period from Zoho Payroll. " +
            "Returns detailed salary breakdown including regular hours, overtime, and leave amounts.")
    public PayslipResponse getEmployeePayslip(
            @ToolParam(description = "Employee email (@eservecloud.in domain required)") String email,
            @ToolParam(description = "Pay period start date (yyyy-MM-dd)") String fromDate,
            @ToolParam(description = "Pay period end date (yyyy-MM-dd)") String toDate) {

        log.info("Fetching payslip for employee: {} from {} to {}", email, fromDate, toDate);

        try {
            // Validate employee
            Employee employee = validateAndFetchEmployee(email);

            // Parse and validate dates
            LocalDate from = parseDate(fromDate, "fromDate");
            LocalDate to = parseDate(toDate, "toDate");
            validateDateRange(from, to);

            // Fetch directly from Zoho API
            ZohoPayrollReportResponse zohoResponse = zohoApiService.fetchPayrollReport(
                    employee.getZohoErecNo(), from, to);

            // Transform and return
            PayslipResponse response = responseMapper.mapToPayslipResponse(employee, zohoResponse);

            log.info("Successfully fetched payslip for employee: {}", email);
            return response;

        } catch (McpBaseException ex) {
            log.error("MCP exception while fetching payslip: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while fetching payslip for employee: {}", email, ex);
            throw new McpToolException("getEmployeePayslip", "Failed to fetch payslip", ex);
        }
    }

    @Tool(description = "Fetch employee leave records for a specific period from Zoho Payroll. " +
            "Returns leave type, dates, duration, and status.")
    public LeaveRecordsResponse getEmployeeLeaves(
            @ToolParam(description = "Employee email (@eservecloud.in domain required)") String email,
            @ToolParam(description = "Start date (yyyy-MM-dd)") String fromDate,
            @ToolParam(description = "End date (yyyy-MM-dd)") String toDate) {

        log.info("Fetching leave records for employee: {} from {} to {}", email, fromDate, toDate);

        try {
            // Validate employee
            Employee employee = validateAndFetchEmployee(email);

            // Parse and validate dates
            LocalDate from = parseDate(fromDate, "fromDate");
            LocalDate to = parseDate(toDate, "toDate");
            validateDateRange(from, to);

            // Fetch directly from Zoho API
            ZohoLeaveReportResponse zohoResponse = zohoApiService.fetchLeaveReport(
                    employee.getZohoErecNo(), from, to);

            // Transform and return
            LeaveRecordsResponse response = responseMapper.mapToLeaveRecordsResponse(employee, zohoResponse);

            log.info("Successfully fetched leave records for employee: {}", email);
            return response;

        } catch (McpBaseException ex) {
            log.error("MCP exception while fetching leave records: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while fetching leave records for employee: {}", email, ex);
            throw new McpToolException("getEmployeeLeaves", "Failed to fetch leave records", ex);
        }
    }

    // Validation helpers
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

    private LocalDate parseDate(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw InputValidationException.emptyField(fieldName);
        }

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw DateValidationException.invalidFormat(fieldName, dateStr);
        }
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        // Check if from date is after to date
        if (fromDate.isAfter(toDate)) {
            throw DateValidationException.invalidRange(fromDate, toDate);
        }

        // Check if date range exceeds 1 month
        long monthsBetween = ChronoUnit.MONTHS.between(fromDate, toDate);
        if (monthsBetween > 1) {
            throw DateValidationException.rangeExceedsLimit(1);
        }

        // Check if from date is in the future
        if (fromDate.isAfter(LocalDate.now())) {
            throw DateValidationException.futureDate("fromDate");
        }
    }
}