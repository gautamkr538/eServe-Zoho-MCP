package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZohoHolidaysResponse {
    @JsonProperty("data")
    private List<Holiday> data;
    private String message;
    private String uri;
    private Integer status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Holiday {
        private Boolean isRestrictedHoliday;
        private String ShiftName;
        private String Remarks;
        private String LocationId;
        private String ShiftId;
        private String Id;
        private String Date; // or LocalDate if you add parsing
        private Boolean isHalfday;
        private String Name;
        private String LocationName;
        private int Session;
    }
}
