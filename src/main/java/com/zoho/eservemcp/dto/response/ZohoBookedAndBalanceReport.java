package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ZohoBookedAndBalanceReport(
        Map<String, LeaveTypeMeta> leavetypes,
        Map<String, EmployeeReport> report,
        List<String> employees
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record LeaveTypeMeta(
            String unit,
            String name,
            String type,
            String code
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record EmployeeReport(
            @JsonProperty("totals") Totals totals,
            @JsonProperty("employee") EmployeeInfo employee,
            Map<String, LeaveRecordStats> leaveTypeData // custom parsing, see note below
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static record LeaveRecordStats(
                Double booked,
                Double balance
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static record Totals(
                Double ondutyBooked,
                Double unpaidBooked,
                Double unpaidBalance,
                Double paidBalance,
                Double ondutyBalance,
                Double paidBooked
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static record EmployeeInfo(
                String name,
                String id
        ) {}
    }
}
