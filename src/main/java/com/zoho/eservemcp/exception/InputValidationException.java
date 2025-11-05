package com.zoho.eservemcp.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Exception thrown when input parameter validation fails
 */
public class InputValidationException extends McpBaseException {
    
    private static final String ERROR_CODE = "INVALID_INPUT";
    
    private final Map<String, String> fieldErrors;
    
    public InputValidationException(String message) {
        super(message, ERROR_CODE, HttpStatus.BAD_REQUEST.value());
        this.fieldErrors = null;
    }
    
    public InputValidationException(String message, Map<String, String> fieldErrors) {
        super(message, ERROR_CODE, HttpStatus.BAD_REQUEST.value());
        this.fieldErrors = fieldErrors;
    }
    
    public static InputValidationException emptyField(String fieldName) {
        return new InputValidationException(
            String.format("%s cannot be empty or null", fieldName)
        );
    }
    
    public static InputValidationException invalidEmail(String email) {
        return new InputValidationException(
            String.format("Invalid email format: %s", email)
        );
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
