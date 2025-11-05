package com.zoho.eservemcp.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when employee is not found or inactive
 */
public class EmployeeNotFoundException extends McpBaseException {
    
    private static final String ERROR_CODE = "EMPLOYEE_NOT_FOUND";
    
    public EmployeeNotFoundException(String message) {
        super(message, ERROR_CODE, HttpStatus.NOT_FOUND.value());
    }
    
    public EmployeeNotFoundException(String employeeIdentifier, boolean isInactive) {
        super(
            isInactive 
                ? String.format("Employee is inactive: %s", employeeIdentifier)
                : String.format("Employee not found: %s", employeeIdentifier),
            ERROR_CODE,
            HttpStatus.NOT_FOUND.value()
        );
    }
}
