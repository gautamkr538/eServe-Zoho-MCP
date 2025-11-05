package com.zoho.eservemcp.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when email domain validation fails
 */
public class DomainValidationException extends McpBaseException {
    
    private static final String ERROR_CODE = "DOMAIN_VALIDATION_FAILED";
    
    public DomainValidationException(String email, String allowedDomain) {
        super(
            String.format("Only @%s domain is allowed. Provided: %s", allowedDomain, email),
            ERROR_CODE,
            HttpStatus.FORBIDDEN.value()
        );
    }
    
    public DomainValidationException(String message) {
        super(message, ERROR_CODE, HttpStatus.FORBIDDEN.value());
    }
}
