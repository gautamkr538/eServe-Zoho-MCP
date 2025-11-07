package com.zoho.eservemcp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponse {
    private String name;
    private String date;
    private String remarks;
    private String locationName;
    private String shiftName;
    private boolean isRestrictedHoliday;
    private boolean isHalfday;
    private int session;
}
