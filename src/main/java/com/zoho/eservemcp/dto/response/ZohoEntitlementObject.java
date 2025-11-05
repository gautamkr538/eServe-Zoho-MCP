package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZohoEntitlementObject {

    @JsonProperty("LeaveType")
    private String leaveType;

    @JsonProperty("TotalEntitled")
    private BigDecimal totalEntitled;

    @JsonProperty("Used")
    private BigDecimal used;

    @JsonProperty("Balance")
    private BigDecimal balance;
}
