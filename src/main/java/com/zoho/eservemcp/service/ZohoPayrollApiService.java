package com.zoho.eservemcp.service;

import com.zoho.eservemcp.dto.response.ZohoBookedAndBalanceReport;
import com.zoho.eservemcp.dto.response.ZohoHolidaysResponse;
import com.zoho.eservemcp.dto.response.ZohoLeaveRecordsResponseV2;
import com.zoho.eservemcp.exception.ZohoApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ZohoPayrollApiService {

    private static final Logger log = LoggerFactory.getLogger(ZohoPayrollApiService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Value("${zoho.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    private final ZohoTokenManager tokenManager;

    public ZohoPayrollApiService(RestTemplate restTemplate, ZohoTokenManager tokenManager) {
        this.restTemplate = restTemplate;
        this.tokenManager = tokenManager;
    }

    // Create HTTP headers with OAuth token
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String oauthToken = tokenManager.getValidAccessToken();
        headers.set("Authorization", "Zoho-oauthtoken " + oauthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    /**
     * Fetch leave records using Zoho V2 API.
     */
    public ZohoLeaveRecordsResponseV2 fetchLeaveRecords(
            String portalID, String from, String to, List<String> employeeIds, String dateFormat) {
        var uri = UriComponentsBuilder.fromUriString(baseUrl + "/api/v2/leavetracker/leaves/records")
                .queryParam("portalID", portalID)
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("dateFormat", dateFormat)
                .queryParam("employee", String.join(",", employeeIds))
                .build().toUri();
        HttpHeaders headers = createHeaders();
        try {
            ResponseEntity<ZohoLeaveRecordsResponseV2> resp = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), ZohoLeaveRecordsResponseV2.class
            );
            return resp.getBody();
        } catch (Exception e) {
            log.error("Error fetching leave records", e);
            throw new ZohoApiException("Fetch leave records failed", e);
        }
    }

    /**
     * Fetch Booked & Balance report using Zoho V2 API.
     */
    public ZohoBookedAndBalanceReport fetchBookedAndBalance(
            String from, String to, String unit, List<String> employeeIds, List<String> leaveTypeIds) {
        var uri = UriComponentsBuilder.fromUriString(baseUrl + "/api/v2/leavetracker/reports/bookedAndBalance")
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("unit", unit)
                .queryParam("employee", String.join(",", employeeIds))
                .queryParam("leavetype", String.join(",", leaveTypeIds))
                .build().toUri();
        HttpHeaders headers = createHeaders();
        try {
            ResponseEntity<ZohoBookedAndBalanceReport> resp = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), ZohoBookedAndBalanceReport.class
            );
            return resp.getBody();
        } catch (Exception e) {
            log.error("Error fetching booked and balance report", e);
            throw new ZohoApiException("Fetch booked and balance failed", e);
        }
    }

    /**
     * Download payslip PDF for an employee for a specific pay period.
     */
    public byte[] downloadPayslip(String userErecNo, String payPeriodId) {
        log.info("Downloading payslip for user: {} period: {}", userErecNo, payPeriodId);
        String url = String.format("%s/api/timesheet/downloadPayslip?userErecNo=%s&payPeriodId=%s", baseUrl, userErecNo, payPeriodId);
        HttpHeaders headers = createHeaders();
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("Payslip download failed, response code: {}", response.getStatusCode());
                throw new ZohoApiException("Payslip download failed: HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Exception downloading payslip: {}", e.getMessage(), e);
            throw new ZohoApiException("Exception downloading payslip", e);
        }
    }

    /**
     * Fetch holidays from Zoho API
     */
    public ZohoHolidaysResponse fetchHolidays(
            String location, String shift, String employee, boolean upcoming, String from, String to, String dateFormat) {

        String url = String.format(
                "%s/api/leave/v2/holidays/get?location=%s&shift=%s&employee=%s&upcoming=%s&from=%s&to=%s&dateFormat=%s",
                baseUrl, location, shift, employee, upcoming, from, to, dateFormat);

        HttpHeaders headers = createHeaders();
        try {
            ResponseEntity<ZohoHolidaysResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), ZohoHolidaysResponse.class);
            ZohoHolidaysResponse body = response.getBody();
            if (body == null || body.getData() == null || body.getStatus() != 1) {
                throw new ZohoApiException("Holidays API failure or empty result");
            }
            return body;
        } catch (Exception ex) {
            log.error("Failed fetching holidays from Zoho", ex);
            throw new ZohoApiException("Failed to fetch holidays from Zoho", ex);
        }
    }
}