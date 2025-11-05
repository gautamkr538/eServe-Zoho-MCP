package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZohoLeaveBalanceResponse {
    private LeaveBalanceResponse response;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveBalanceResponse {
        private Integer status;
        private String message;
        private List<ZohoEntitlementObject> result;
    }
}
