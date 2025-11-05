package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaveSummaryResponse {
    private String employeeId;
    private String employeeName;
    private String email;
    private List<LeaveRecordsResponse.LeaveEntitlement> entitlements;
    private LeaveRecordsResponse.LeaveSummary summary;
}
