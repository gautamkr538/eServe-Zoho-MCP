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

        @JsonProperty("isRestrictedHoliday")
        private Boolean restrictedHoliday;

        @JsonProperty("isHalfday")
        private Boolean halfday;

        @JsonProperty("ShiftName")
        private String shiftName;

        @JsonProperty("Remarks")
        private String remarks;

        @JsonProperty("LocationId")
        private String locationId;

        @JsonProperty("ShiftId")
        private String shiftId;

        @JsonProperty("Id")
        private String id;

        @JsonProperty("Date")
        private String date;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("LocationName")
        private String locationName;

        @JsonProperty("Session")
        private Integer session;
    }
}
