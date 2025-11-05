package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayslipResponse {
    
    private boolean success;
    private String message;
    private EmployeeInfo employee;
    private List<PayslipDetail> payslips;
    private PayslipSummary summary;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeInfo {
        private String employeeId;
        private String employeeName;
        private String email;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PayslipDetail {
        
        // Period information
        private String fromDate;
        private String toDate;
        private String payPeriodId;
        private String approvalStatus;
        
        // Regular time details
        @JsonProperty("regularHours")
        private BigDecimal regularHours;
        
        @JsonProperty("ratePerHour")
        private BigDecimal ratePerHour;
        
        @JsonProperty("regularAmount")
        private BigDecimal regularAmount;
        
        // Overtime details
        @JsonProperty("overtimeHours")
        private BigDecimal overtimeHours;
        
        @JsonProperty("overtimeRatePerHour")
        private BigDecimal overtimeRatePerHour;
        
        @JsonProperty("overtimeAmount")
        private BigDecimal overtimeAmount;
        
        // Extended overtime
        @JsonProperty("extendedOvertimeHours")
        private BigDecimal extendedOvertimeHours;
        
        @JsonProperty("extendedOvertimeAmount")
        private BigDecimal extendedOvertimeAmount;
        
        // Paid leave
        @JsonProperty("paidLeaveHours")
        private BigDecimal paidLeaveHours;
        
        @JsonProperty("paidLeaveAmount")
        private BigDecimal paidLeaveAmount;
        
        // Weekend & Holiday overtime
        @JsonProperty("weekendOvertimeHours")
        private BigDecimal weekendOvertimeHours;
        
        @JsonProperty("holidayOvertimeHours")
        private BigDecimal holidayOvertimeHours;
        
        // Total
        @JsonProperty("totalAmount")
        private BigDecimal totalAmount;
        
        // Breakdown
        private EarningsBreakdown earnings;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class EarningsBreakdown {
            private BigDecimal regularPay;
            private BigDecimal overtimePay;
            private BigDecimal leavePay;
            private BigDecimal totalEarnings;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PayslipSummary {
        private Integer totalRecords;
        private BigDecimal totalRegularHours;
        private BigDecimal totalOvertimeHours;
        private BigDecimal totalPaidLeaveHours;
        private BigDecimal totalAmount;
        private String currency = "INR";
    }
}
