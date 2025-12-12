package com.zoho.eservemcp.utils;

import com.zoho.eservemcp.dto.response.HolidayResponse;
import com.zoho.eservemcp.dto.response.ZohoHolidaysResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PayrollResponseMapper {

    /**
     * Map Zoho holidays response to MCP holiday response list
     */
    public List<HolidayResponse> mapToHolidayResponses(List<ZohoHolidaysResponse.Holiday> zohoHolidays) {
        if (zohoHolidays == null) return Collections.emptyList();
        return zohoHolidays.stream().map(z -> HolidayResponse.builder()
                .name(z.getName())
                .date(z.getDate())
                .remarks(z.getRemarks())
                .locationName(z.getLocationName())
                .shiftName(z.getShiftName())
                .isRestrictedHoliday(Boolean.TRUE.equals(z.getRestrictedHoliday()))
                .isHalfday(Boolean.TRUE.equals(z.getHalfday()))
                .session(Optional.of(z.getSession()).orElse(0))
                .build()
        ).collect(Collectors.toList());
    }
}
