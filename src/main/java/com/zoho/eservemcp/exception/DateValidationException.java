package com.zoho.eservemcp.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;

/**
 * Exception thrown when date validation fails
 */
public class DateValidationException extends McpBaseException {
    
    private static final String ERROR_CODE = "INVALID_DATE_RANGE";
    
    public DateValidationException(String message) {
        super(message, ERROR_CODE, HttpStatus.BAD_REQUEST.value());
    }
    
    public static DateValidationException invalidFormat(String fieldName, String providedValue) {
        return new DateValidationException(
            String.format("Invalid %s format. Expected yyyy-MM-dd, got: %s", fieldName, providedValue)
        );
    }
    
    public static DateValidationException invalidRange(LocalDate fromDate, LocalDate toDate) {
        return new DateValidationException(
            String.format("From date (%s) cannot be after to date (%s)", fromDate, toDate)
        );
    }
    
    public static DateValidationException rangeExceedsLimit(int months) {
        return new DateValidationException(
            String.format("Date range cannot exceed %d month(s)", months)
        );
    }
    
    public static DateValidationException futureDate(String fieldName) {
        return new DateValidationException(
            String.format("%s cannot be in the future", fieldName)
        );
    }
}
