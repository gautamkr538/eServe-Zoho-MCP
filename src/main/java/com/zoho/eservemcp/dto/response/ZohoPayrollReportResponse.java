package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZohoPayrollReportResponse {
    
    private PayrollResponse response;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayrollResponse {
        
        private Integer status; // 0 = success, non-zero = error
        private String message;
        private String uri;
        private List<PayrollRecord> result;
        private Integer totalCount;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PayrollRecord {
            
            @JsonProperty("EmployeeID")
            private String employeeId;
            
            @JsonProperty("Employeename")
            private String employeeName;
            
            @JsonProperty("Emailid")
            private String emailId;
            
            @JsonProperty("fromdate")
            private String fromDate;
            
            @JsonProperty("todate")
            private String toDate;
            
            @JsonProperty("payPeriodId")
            private String payPeriodId;
            
            // Regular time
            @JsonProperty("regularhrs")
            private BigDecimal regularHours;
            
            @JsonProperty("ratePerHr")
            private BigDecimal ratePerHour;
            
            @JsonProperty("regularTimeAmt")
            private BigDecimal regularTimeAmount;
            
            // Overtime
            @JsonProperty("othrs")
            private BigDecimal overtimeHours;
            
            @JsonProperty("otRatePerHr")
            private BigDecimal overtimeRatePerHour;
            
            @JsonProperty("overtimeAmt")
            private BigDecimal overtimeAmount;
            
            // Extended overtime
            @JsonProperty("extendedothrs")
            private BigDecimal extendedOvertimeHours;
            
            @JsonProperty("extendedotAmt")
            private BigDecimal extendedOvertimeAmount;
            
            // Paid leave
            @JsonProperty("paidLeaveHours")
            private BigDecimal paidLeaveHours;
            
            @JsonProperty("paidLeaveAmt")
            private BigDecimal paidLeaveAmount;
            
            // Weekend & Holiday overtime
            @JsonProperty("weekendOtHours")
            private BigDecimal weekendOvertimeHours;
            
            @JsonProperty("holidayOtHours")
            private BigDecimal holidayOvertimeHours;
            
            // Total
            @JsonProperty("totalAmount")
            private BigDecimal totalAmount;
            
            // Additional flags
            @JsonProperty("hasLeaveDataOnly")
            private Boolean hasLeaveDataOnly;
            
            @JsonProperty("approvalStatus")
            private String approvalStatus; // approved, unapproved
        }
    }
}
