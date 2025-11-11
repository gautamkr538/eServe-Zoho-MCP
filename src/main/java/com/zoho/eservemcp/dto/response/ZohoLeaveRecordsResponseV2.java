package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ZohoLeaveRecordsResponseV2(
        Map<String, LeaveRecord> records
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record LeaveRecord(
            @JsonProperty("Zoho.ID") String zohoId,
            @JsonProperty("Employee") String employeeName,
            @JsonProperty("Employee.ID") String employeeId,
            @JsonProperty("Leavetype") String leaveTypeName,
            @JsonProperty("Leavetype.ID") String leaveTypeId,
            @JsonProperty("From") String from,
            @JsonProperty("To") String to,
            @JsonProperty("Unit") String unit,
            @JsonProperty("ApprovalStatus") String approvalStatus,
            @JsonProperty("Type") String type,
            @JsonProperty("Reason") String reason,
            @JsonProperty("Days") Map<String, DayInfo> days,
            @JsonProperty("DateOfRequest") String dateOfRequest
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static record DayInfo(
                @JsonProperty("LeaveCount") String leaveCount,
                @JsonProperty("StartTime") String startTime,
                @JsonProperty("EndTime") String endTime,
                @JsonProperty("Session") Integer session
        ) {
        }
    }
}
