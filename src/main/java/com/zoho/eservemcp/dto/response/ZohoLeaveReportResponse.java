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
public class ZohoLeaveReportResponse {
    
    private LeaveResponse response;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeaveResponse {
        
        private Integer status; // 0 = success
        private String message;
        private String uri;
        private List<LeaveRecord> result;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class LeaveRecord {
            
            @JsonProperty("LeaveId")
            private String leaveId;
            
            @JsonProperty("EmployeeID")
            private String employeeId;
            
            @JsonProperty("Employeename")
            private String employeeName;
            
            @JsonProperty("EmailId")
            private String emailId;
            
            @JsonProperty("LeaveType")
            private String leaveType;
            
            @JsonProperty("From")
            private String fromDate;
            
            @JsonProperty("To")
            private String toDate;
            
            @JsonProperty("Days")
            private BigDecimal days;
            
            @JsonProperty("Status")
            private String status; // Pending, Approved, Rejected
            
            @JsonProperty("Reason")
            private String reason;
            
            @JsonProperty("AppliedOn")
            private String appliedOn;
            
            @JsonProperty("ApprovedBy")
            private String approvedBy;
            
            @JsonProperty("ApprovedOn")
            private String approvedOn;
            
            @JsonProperty("RejectedBy")
            private String rejectedBy;
            
            @JsonProperty("RejectedOn")
            private String rejectedOn;
            
            @JsonProperty("Comments")
            private String comments;
        }
    }
}
