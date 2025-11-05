package com.zoho.eservemcp.tools;

import com.zoho.eservemcp.dto.response.*;
import com.zoho.eservemcp.dto.response.ZohoEntitlementObject;
import com.zoho.eservemcp.entity.Employee;
import com.zoho.eservemcp.exception.*;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

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

    @Tool(description = "Fetch employee payslip for a specific pay period from Zoho Payroll. " +
            "Returns detailed salary breakdown including regular hours, overtime, and leave amounts.")
    public PayslipResponse getEmployeePayslip(
            @ToolParam(description = "Employee email (@eservecloud.in domain required)") String email,
            @ToolParam(description = "Pay period start date (yyyy-MM-dd)") String fromDate,
            @ToolParam(description = "Pay period end date (yyyy-MM-dd)") String toDate) {

        log.info("Fetching payslip for employee: {} from {} to {}", email, fromDate, toDate);

        try {
            Employee employee = validateAndFetchEmployee(email);

            LocalDate from = parseDate(fromDate, "fromDate");
            LocalDate to = parseDate(toDate, "toDate");
            validateDateRange(from, to);

            ZohoPayrollReportResponse zohoResponse = zohoApiService.fetchPayrollReport(
                    employee.getZohoErecNo(), from, to);

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
            Employee employee = validateAndFetchEmployee(email);

            LocalDate from = parseDate(fromDate, "fromDate");
            LocalDate to = parseDate(toDate, "toDate");
            validateDateRange(from, to);

            ZohoLeaveReportResponse zohoResponse = zohoApiService.fetchLeaveReport(
                    employee.getZohoErecNo(), from, to);

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

    /**
     * Fetch and summarize leave: entitlements, counts taken/left/total for dashboard.
     */
    @Tool(description = "Get detailed leave summary (taken/left/total) for employee, including entitlements.")
    public LeaveSummaryResponse getEmployeeLeaveSummary(
            @ToolParam(description = "Employee email (@eservecloud.in domain required)") String email) {

        log.info("Fetching leave summary for employee: {}", email);
        try {
            Employee employee = validateAndFetchEmployee(email);

            // Fetch leave entitlements (balances) from Zoho (use proper DTO)
            ZohoLeaveBalanceResponse balanceResponse = zohoApiService.fetchLeaveBalance(employee.getZohoErecNo());
            List<ZohoEntitlementObject> zohoEntitlements =
                    (balanceResponse != null && balanceResponse.getResponse() != null)
                            ? balanceResponse.getResponse().getResult()
                            : Collections.emptyList();
            // Fetch all leaves taken so far this year (for summary)
            ZohoLeaveReportResponse yearLeavesResponse = zohoApiService.fetchLeaveReport(
                    employee.getZohoErecNo(),
                    LocalDate.now().withDayOfYear(1),
                    LocalDate.now()
            );

            // Map to MCP DTOs
            List<LeaveRecordsResponse.LeaveEntitlement> entitlements =
                    responseMapper.mapEntitlementsFromZoho(zohoEntitlements);

            List<LeaveRecordsResponse.LeaveDetail> leaveDetails = yearLeavesResponse != null &&
                    yearLeavesResponse.getResponse() != null &&
                    yearLeavesResponse.getResponse().getResult() != null ?
                    yearLeavesResponse.getResponse().getResult().stream()
                            .map(responseMapper::mapToLeaveDetail)
                            .collect(java.util.stream.Collectors.toList()) :
                    Collections.emptyList();

            LeaveSummaryResponse summary = responseMapper.mapToLeaveSummary(employee, entitlements, leaveDetails);

            log.info("Successfully fetched leave summary for employee: {}", email);
            return summary;
        } catch (McpBaseException ex) {
            log.error("MCP exception while fetching leave summary: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while fetching leave summary for employee: {}", email, ex);
            throw new McpToolException("getEmployeeLeaveSummary", "Failed to fetch leave summary", ex);
        }
    }

    @Tool(description = "Download employee payslip as PDF for agent workflow and save to file, returning the file path.")
    public String downloadPayslipAsPdfAndReturnPath(@ToolParam(description = "Employee email (@eservecloud.in domain required)") String email,
            @ToolParam(description = "Pay period ID") String payPeriodId) {

        Logger log = LoggerFactory.getLogger(getClass());
        Employee employee = null;
        try {
            employee = validateAndFetchEmployee(email);
            if (payPeriodId == null || payPeriodId.trim().isEmpty()) {
                throw new McpToolException("PayPeriodId cannot be empty");
            }
        } catch (McpBaseException ex) {
            log.error("Validation error for downloadPayslip: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected validation error: {}", ex.getMessage(), ex);
            throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "Validation failed", ex);
        }

        byte[] pdfBytes;
        try {
            pdfBytes = zohoApiService.downloadPayslip(employee.getZohoErecNo(), payPeriodId);
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("Payslip PDF bytes are empty for employee: {} and period: {}", email, payPeriodId);
                throw new McpToolException("Received empty PDF from payroll API");
            }
        } catch (McpBaseException ex) {
            log.error("Payroll API error: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to download payslip from Zoho for {} period {}: {}", email, payPeriodId, ex.getMessage(), ex);
            throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "Error downloading payslip from Zoho", ex);
        }

        String safeEmail = email.replaceAll("@.*$", "").replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = "payslip_" + safeEmail + "_" + payPeriodId + ".pdf";
        String pathString = "/tmp/" + fileName;
        Path path = Paths.get(pathString);

        try {
            Files.write(path, pdfBytes);
            log.info("Payslip PDF saved for user {}: {}", email, pathString);
            return pathString;
        } catch (IOException e) {
            log.error("Failed to write PDF to disk: {}", e.getMessage(), e);
            throw new McpToolException("downloadPayslipAsPdfAndReturnPath", "Failed to write PDF file to disk", e);
        }
    }

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
        if (fromDate.isAfter(toDate)) {
            throw DateValidationException.invalidRange(fromDate, toDate);
        }
        long monthsBetween = ChronoUnit.MONTHS.between(fromDate, toDate);
        if (monthsBetween > 1) {
            throw DateValidationException.rangeExceedsLimit(1);
        }
        if (fromDate.isAfter(LocalDate.now())) {
            throw DateValidationException.futureDate("fromDate");
        }
    }
}