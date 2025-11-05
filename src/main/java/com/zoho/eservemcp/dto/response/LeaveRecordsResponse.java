package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaveRecordsResponse {
    
    private boolean success;
    private String message;
    private EmployeeInfo employee;
    private List<LeaveDetail> leaves;
    private LeaveSummary summary;
    
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
    public static class LeaveDetail {
        
        private String leaveId;
        private String leaveType;
        
        // Date information
        private String fromDate;
        private String toDate;
        private BigDecimal totalDays;
        
        // Status information
        private String status; // Pending, Approved, Rejected
        private String approvalStatus; // For display
        
        // Application details
        private String reason;
        private String appliedOn;
        
        // Approval/Rejection details
        private String approvedBy;
        private String approvedOn;
        private String rejectedBy;
        private String rejectedOn;
        private String comments;
        
        // Additional metadata
        private boolean isPending;
        private boolean isApproved;
        private boolean isRejected;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaveSummary {
        private Integer totalLeaveRecords;
        private BigDecimal totalLeaveDays;
        private LeaveStatusCount statusCounts;
        private LeaveTypeBreakdown typeBreakdown;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class LeaveStatusCount {
            private Integer pending;
            private Integer approved;
            private Integer rejected;
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class LeaveTypeBreakdown {
            private Map<String, BigDecimal> leavesByType; // LeaveType -> Days
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaveEntitlement {
        private String leaveType;
        private BigDecimal totalEntitled;
        private BigDecimal used;
        private BigDecimal remaining;
    }
}
