package com.zoho.eservemcp.utils;

import com.zoho.eservemcp.dto.response.*;
import com.zoho.eservemcp.entity.Employee;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayrollResponseMapper {
    
    /**
     * Map Zoho payroll response to MCP payslip response
     */
    public PayslipResponse mapToPayslipResponse(Employee employee, ZohoPayrollReportResponse zohoResponse) {
        
        if (zohoResponse == null || zohoResponse.getResponse() == null) {
            return PayslipResponse.builder()
                .success(false)
                .message("No payroll data available")
                .build();
        }
        
        List<PayslipResponse.PayslipDetail> payslipDetails =
            zohoResponse.getResponse().getResult().stream()
                .map(this::mapToPayslipDetail)
                .collect(Collectors.toList());
        
        PayslipResponse.PayslipSummary summary = calculatePayslipSummary(payslipDetails);
        
        return PayslipResponse.builder()
            .success(true)
            .message("Payslip data fetched successfully")
            .employee(PayslipResponse.EmployeeInfo.builder()
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getFullName())
                .email(employee.getEmail())
                .build())
            .payslips(payslipDetails)
            .summary(summary)
            .build();
    }
    
    private PayslipResponse.PayslipDetail mapToPayslipDetail(ZohoPayrollReportResponse.PayrollResponse.PayrollRecord record) {
        
        PayslipResponse.PayslipDetail.EarningsBreakdown earnings = 
            PayslipResponse.PayslipDetail.EarningsBreakdown.builder()
                .regularPay(record.getRegularTimeAmount())
                .overtimePay(record.getOvertimeAmount() != null ? 
                    record.getOvertimeAmount().add(
                        record.getExtendedOvertimeAmount() != null ? 
                            record.getExtendedOvertimeAmount() : BigDecimal.ZERO) 
                    : BigDecimal.ZERO)
                .leavePay(record.getPaidLeaveAmount())
                .totalEarnings(record.getTotalAmount())
                .build();
        
        return PayslipResponse.PayslipDetail.builder()
            .fromDate(record.getFromDate())
            .toDate(record.getToDate())
            .payPeriodId(record.getPayPeriodId())
            .approvalStatus(record.getApprovalStatus())
            .regularHours(record.getRegularHours())
            .ratePerHour(record.getRatePerHour())
            .regularAmount(record.getRegularTimeAmount())
            .overtimeHours(record.getOvertimeHours())
            .overtimeRatePerHour(record.getOvertimeRatePerHour())
            .overtimeAmount(record.getOvertimeAmount())
            .extendedOvertimeHours(record.getExtendedOvertimeHours())
            .extendedOvertimeAmount(record.getExtendedOvertimeAmount())
            .paidLeaveHours(record.getPaidLeaveHours())
            .paidLeaveAmount(record.getPaidLeaveAmount())
            .weekendOvertimeHours(record.getWeekendOvertimeHours())
            .holidayOvertimeHours(record.getHolidayOvertimeHours())
            .totalAmount(record.getTotalAmount())
            .earnings(earnings)
            .build();
    }
    
    private PayslipResponse.PayslipSummary calculatePayslipSummary(List<PayslipResponse.PayslipDetail> details) {
        
        return PayslipResponse.PayslipSummary.builder()
            .totalRecords(details.size())
            .totalRegularHours(details.stream()
                .map(PayslipResponse.PayslipDetail::getRegularHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .totalOvertimeHours(details.stream()
                .map(d -> d.getOvertimeHours() != null ? 
                    d.getOvertimeHours().add(
                        d.getExtendedOvertimeHours() != null ? 
                            d.getExtendedOvertimeHours() : BigDecimal.ZERO) 
                    : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .totalPaidLeaveHours(details.stream()
                .map(PayslipResponse.PayslipDetail::getPaidLeaveHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .totalAmount(details.stream()
                .map(PayslipResponse.PayslipDetail::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .currency("INR")
            .build();
    }
    
    /**
     * Map Zoho leave response to MCP leave records response
     */
    public LeaveRecordsResponse mapToLeaveRecordsResponse(Employee employee, ZohoLeaveReportResponse zohoResponse) {

        if (zohoResponse == null || zohoResponse.getResponse() == null) {
            return LeaveRecordsResponse.builder()
                .success(false)
                .message("No leave data available")
                .build();
        }
        
        List<LeaveRecordsResponse.LeaveDetail> leaveDetails = 
            zohoResponse.getResponse().getResult().stream()
                .map(this::mapToLeaveDetail)
                .collect(Collectors.toList());
        
        LeaveRecordsResponse.LeaveSummary summary = calculateLeaveSummary(leaveDetails);
        
        return LeaveRecordsResponse.builder()
            .success(true)
            .message("Leave records fetched successfully")
            .employee(LeaveRecordsResponse.EmployeeInfo.builder()
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getFullName())
                .email(employee.getEmail())
                .build())
            .leaves(leaveDetails)
            .summary(summary)
            .build();
    }
    
    public LeaveRecordsResponse.LeaveDetail mapToLeaveDetail(ZohoLeaveReportResponse.LeaveResponse.LeaveRecord record) {
        
        String status = record.getStatus();
        
        return LeaveRecordsResponse.LeaveDetail.builder()
            .leaveId(record.getLeaveId())
            .leaveType(record.getLeaveType())
            .fromDate(record.getFromDate())
            .toDate(record.getToDate())
            .totalDays(record.getDays())
            .status(status)
            .approvalStatus(formatApprovalStatus(status))
            .reason(record.getReason())
            .appliedOn(record.getAppliedOn())
            .approvedBy(record.getApprovedBy())
            .approvedOn(record.getApprovedOn())
            .rejectedBy(record.getRejectedBy())
            .rejectedOn(record.getRejectedOn())
            .comments(record.getComments())
            .isPending("Pending".equalsIgnoreCase(status))
            .isApproved("Approved".equalsIgnoreCase(status))
            .isRejected("Rejected".equalsIgnoreCase(status))
            .build();
    }
    
    private String formatApprovalStatus(String status) {
        if (status == null) return "Unknown";
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }
    
    private LeaveRecordsResponse.LeaveSummary calculateLeaveSummary(List<LeaveRecordsResponse.LeaveDetail> details) {
        
        Map<String, BigDecimal> leavesByType = details.stream()
            .collect(Collectors.groupingBy(
                LeaveRecordsResponse.LeaveDetail::getLeaveType,
                Collectors.mapping(
                    LeaveRecordsResponse.LeaveDetail::getTotalDays,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
        
        return LeaveRecordsResponse.LeaveSummary.builder()
            .totalLeaveRecords(details.size())
            .totalLeaveDays(details.stream()
                .map(LeaveRecordsResponse.LeaveDetail::getTotalDays)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .statusCounts(LeaveRecordsResponse.LeaveSummary.LeaveStatusCount.builder()
                .pending((int) details.stream().filter(LeaveRecordsResponse.LeaveDetail::isPending).count())
                .approved((int) details.stream().filter(LeaveRecordsResponse.LeaveDetail::isApproved).count())
                .rejected((int) details.stream().filter(LeaveRecordsResponse.LeaveDetail::isRejected).count())
                .build())
            .typeBreakdown(LeaveRecordsResponse.LeaveSummary.LeaveTypeBreakdown.builder()
                .leavesByType(leavesByType)
                .build())
            .build();
    }
    /**
     * Map to LeaveSummaryResponse combining employee, entitlements and summary.
     */
    public LeaveSummaryResponse mapToLeaveSummary(Employee employee, List<LeaveRecordsResponse.LeaveEntitlement> entitlements, List<LeaveRecordsResponse.LeaveDetail> allLeaveDetails) {
        // Provide an overall summary (approved leaves etc)
        LeaveRecordsResponse.LeaveSummary summary = calculateLeaveSummary(allLeaveDetails);

        return LeaveSummaryResponse.builder()
                .employeeId(employee.getEmployeeId())
                .employeeName(employee.getFullName())
                .email(employee.getEmail())
                .entitlements(entitlements)
                .summary(summary)
                .build();
    }

    /**
     * Utility to map from Zoho entitlement API result to MCP LeaveEntitlement.
     * (Assume zohoEntitlementResponse is parsed as List<ZohoEntitlementObject>.)
     */
    public List<LeaveRecordsResponse.LeaveEntitlement> mapEntitlementsFromZoho(List<ZohoEntitlementObject> zohoEntitlements) {
        if (zohoEntitlements == null) return Collections.emptyList();
        return zohoEntitlements.stream()
                .map(e -> LeaveRecordsResponse.LeaveEntitlement.builder()
                        .leaveType(e.getLeaveType())
                        .totalEntitled(e.getTotalEntitled())
                        .used(e.getUsed())
                        .remaining(e.getBalance() != null ? e.getBalance() : (
                                e.getTotalEntitled() != null && e.getUsed() != null
                                        ? e.getTotalEntitled().subtract(e.getUsed())
                                        : null))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Map Zoho holidays response to MCP holiday response list
     */
    public List<HolidayResponse> mapToHolidayResponses(List<ZohoHolidaysResponse.Holiday> zohoHolidays) {
        if (zohoHolidays == null) return Collections.emptyList();
        return zohoHolidays.stream().map(z ->
                HolidayResponse.builder()
                        .name(z.getName() != null ? z.getName() : "")
                        .date(z.getDate() != null ? z.getDate() : "")
                        .remarks(z.getRemarks() != null ? z.getRemarks() : "")
                        .locationName(z.getLocationName() != null ? z.getLocationName() : "")
                        .shiftName(z.getShiftName() != null ? z.getShiftName() : "")
                        .isRestrictedHoliday(Boolean.TRUE.equals(z.getIsRestrictedHoliday()))
                        .isHalfday(Boolean.TRUE.equals(z.getIsHalfday()))
                        .session(Optional.of(z.getSession()).orElse(0))
                        .build()
        ).collect(Collectors.toList());
    }
}
