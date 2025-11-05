package com.zoho.eservemcp.service;

import com.zoho.eservemcp.dto.response.ZohoLeaveReportResponse;
import com.zoho.eservemcp.dto.response.ZohoPayrollReportResponse;
import com.zoho.eservemcp.exception.DateValidationException;
import com.zoho.eservemcp.exception.ZohoApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

    /**
     * Fetch payroll report directly from Zoho API
     * No caching, no persistence - pure pass-through
     */
    public ZohoPayrollReportResponse fetchPayrollReport(String userErecNo, LocalDate fromDate, LocalDate toDate) {

        log.info("Fetching payroll report for user: {} from {} to {}", userErecNo, fromDate, toDate);

        validateDateRange(fromDate, toDate);

        String url = buildPayrollReportUrl(userErecNo, fromDate, toDate);
        HttpHeaders headers = createHeaders();

        try {
            ResponseEntity<ZohoPayrollReportResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    ZohoPayrollReportResponse.class
            );

            validateResponse(response, "payroll report");

            ZohoPayrollReportResponse responseBody = response.getBody();
            validateZohoApiStatus(responseBody.getResponse());

            log.info("Successfully fetched payroll report for user: {}", userErecNo);
            return responseBody;

        } catch (HttpClientErrorException e) {
            handleHttpClientError(e, "payroll report");
            throw e; // Won't reach here, but needed for compilation
        } catch (HttpServerErrorException e) {
            handleHttpServerError(e, "payroll report");
            throw e; // Won't reach here, but needed for compilation
        } catch (ResourceAccessException e) {
            log.error("Zoho API timeout while fetching payroll report: {}", e.getMessage());
            throw ZohoApiException.timeout();
        } catch (Exception e) {
            log.error("Unexpected error while fetching payroll report", e);
            throw new ZohoApiException("Unexpected error while fetching payroll report", e);
        }
    }

    /**
     * Fetch leave records directly from Zoho API
     */
    public ZohoLeaveReportResponse fetchLeaveReport(
            String userErecNo,
            LocalDate fromDate,
            LocalDate toDate) {

        log.info("Fetching leave report for user: {} from {} to {}", userErecNo, fromDate, toDate);

        validateDateRange(fromDate, toDate);

        String url = buildLeaveReportUrl(userErecNo, fromDate, toDate);
        HttpHeaders headers = createHeaders();

        try {
            ResponseEntity<ZohoLeaveReportResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    ZohoLeaveReportResponse.class
            );

            validateResponse(response, "leave report");

            ZohoLeaveReportResponse responseBody = response.getBody();
            validateZohoApiStatus(responseBody.getResponse());

            log.info("Successfully fetched leave report for user: {}", userErecNo);
            return responseBody;

        } catch (HttpClientErrorException e) {
            handleHttpClientError(e, "leave report");
            throw e; // Won't reach here
        } catch (HttpServerErrorException e) {
            handleHttpServerError(e, "leave report");
            throw e; // Won't reach here
        } catch (ResourceAccessException e) {
            log.error("Zoho API timeout while fetching leave report: {}", e.getMessage());
            throw ZohoApiException.timeout();
        } catch (Exception e) {
            log.error("Unexpected error while fetching leave report", e);
            throw new ZohoApiException("Unexpected error while fetching leave report", e);
        }
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw DateValidationException.invalidRange(fromDate, toDate);
        }

        long monthsBetween = ChronoUnit.MONTHS.between(fromDate, toDate);
        if (monthsBetween > 1) {
            throw DateValidationException.rangeExceedsLimit(1);
        }

        if (fromDate.isAfter(LocalDate.now())) {
            throw DateValidationException.futureDate("fromDate");
        }
    }

    private void validateResponse(ResponseEntity<?> response, String reportType) {
        if (response.getBody() == null) {
            log.error("Empty response received from Zoho API for {}", reportType);
            throw new ZohoApiException("Empty response from Zoho API for " + reportType);
        }
    }

    private void validateZohoApiStatus(Object responseObject) {
        try {
            Integer status = null;
            String message = null;

            if (responseObject instanceof ZohoPayrollReportResponse.PayrollResponse) {
                ZohoPayrollReportResponse.PayrollResponse payrollResponse =
                        (ZohoPayrollReportResponse.PayrollResponse) responseObject;
                status = payrollResponse.getStatus();
                message = payrollResponse.getMessage();
            } else if (responseObject instanceof ZohoLeaveReportResponse.LeaveResponse) {
                ZohoLeaveReportResponse.LeaveResponse leaveResponse =
                        (ZohoLeaveReportResponse.LeaveResponse) responseObject;
                status = leaveResponse.getStatus();
                message = leaveResponse.getMessage();
            }

            if (status != null && status != 0) {
                log.error("Zoho API returned error status: {} - {}", status, message);
                throw new ZohoApiException(
                        "Zoho API returned error",
                        status,
                        message != null ? message : "Unknown error"
                );
            }
        } catch (ClassCastException e) {
            log.error("Invalid response type from Zoho API", e);
            throw new ZohoApiException("Invalid response format from Zoho API", e);
        }
    }

    private void handleHttpClientError(HttpClientErrorException e, String reportType) {
        HttpStatus statusCode = (HttpStatus) e.getStatusCode();
        String responseBody = e.getResponseBodyAsString();

        log.error("Zoho API client error ({}): {} - {}", statusCode, reportType, responseBody);

        switch (statusCode) {
            case UNAUTHORIZED:
                throw ZohoApiException.authenticationError();
            case FORBIDDEN:
                throw new ZohoApiException("Access forbidden. Check API permissions.");
            case NOT_FOUND:
                throw new ZohoApiException("Zoho API endpoint not found: " + reportType);
            case TOO_MANY_REQUESTS:
                throw ZohoApiException.rateLimitExceeded();
            case BAD_REQUEST:
                throw new ZohoApiException("Bad request to Zoho API: " + responseBody);
            default:
                throw new ZohoApiException(
                        String.format("Zoho API client error (%s): %s", statusCode, responseBody),
                        e
                );
        }
    }

    private void handleHttpServerError(HttpServerErrorException e, String reportType) {
        HttpStatus statusCode = (HttpStatus) e.getStatusCode();
        String responseBody = e.getResponseBodyAsString();

        log.error("Zoho API server error ({}): {} - {}", statusCode, reportType, responseBody);

        throw new ZohoApiException(
                String.format("Zoho API server error (%s) for %s: %s", statusCode, reportType, responseBody),
                e
        );
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String oauthToken = tokenManager.getValidAccessToken();
        headers.set("Authorization", "Zoho-oauthtoken " + oauthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    private String buildPayrollReportUrl(String userErecNo, LocalDate fromDate, LocalDate toDate) {
        return String.format(
                "%s/api/timesheet/getpayrollreport?userErecNo=%s&fromDate=%s&toDate=%s&dateFormat=yyyy-MM-dd&sIndex=0&limit=100",
                baseUrl,
                userErecNo,
                fromDate.format(DATE_FORMATTER),
                toDate.format(DATE_FORMATTER)
        );
    }

    private String buildLeaveReportUrl(String userErecNo, LocalDate fromDate, LocalDate toDate) {
        return String.format(
                "%s/api/leave/getLeaveRecords?userErecNo=%s&fromDate=%s&toDate=%s",
                baseUrl,
                userErecNo,
                fromDate.format(DATE_FORMATTER),
                toDate.format(DATE_FORMATTER)
        );
    }
}