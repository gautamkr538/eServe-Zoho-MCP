package com.zoho.eservemcp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when Zoho API calls fail
 */
@Getter
public class ZohoApiException extends McpBaseException {
    
    private static final String ERROR_CODE = "ZOHO_API_ERROR";
    
    private final Integer zohoStatusCode;
    private final String zohoErrorMessage;
    
    public ZohoApiException(String message) {
        super(message, ERROR_CODE, HttpStatus.BAD_GATEWAY.value());
        this.zohoStatusCode = null;
        this.zohoErrorMessage = null;
    }
    
    public ZohoApiException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE, HttpStatus.BAD_GATEWAY.value());
        this.zohoStatusCode = null;
        this.zohoErrorMessage = null;
    }
    
    public ZohoApiException(String message, Integer zohoStatusCode, String zohoErrorMessage) {
        super(
            String.format("%s - Zoho Status: %d, Message: %s", message, zohoStatusCode, zohoErrorMessage),
            ERROR_CODE,
            HttpStatus.BAD_GATEWAY.value()
        );
        this.zohoStatusCode = zohoStatusCode;
        this.zohoErrorMessage = zohoErrorMessage;
    }
    
    public static ZohoApiException authenticationError() {
        return new ZohoApiException("Zoho API authentication failed. Check OAuth token.");
    }
    
    public static ZohoApiException rateLimitExceeded() {
        return new ZohoApiException("Zoho API rate limit exceeded. Please try again later.");
    }
    
    public static ZohoApiException timeout() {
        return new ZohoApiException("Zoho API request timed out. Please try again.");
    }

}
