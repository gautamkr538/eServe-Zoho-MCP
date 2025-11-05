package com.zoho.eservemcp.exception;

import com.zoho.eservemcp.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for MCP server
 * Handles all exceptions and returns standardized error responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle Employee Not Found Exception
     */
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFoundException(
            EmployeeNotFoundException ex,
            HttpServletRequest request) {
        
        logger.error("Employee not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .errorMessage(ex.getMessage())
            .httpStatus(ex.getHttpStatus())
            .path(request.getRequestURI())
            .timestamp()
            .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * Handle Domain Validation Exception
     */
    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ErrorResponse> handleDomainValidationException(
            DomainValidationException ex,
            HttpServletRequest request) {
        
        logger.warn("Domain validation failed: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .errorMessage(ex.getMessage())
            .httpStatus(ex.getHttpStatus())
            .path(request.getRequestURI())
            .timestamp()
            .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * Handle Date Validation Exception
     */
    @ExceptionHandler(DateValidationException.class)
    public ResponseEntity<ErrorResponse> handleDateValidationException(
            DateValidationException ex,
            HttpServletRequest request) {
        
        logger.warn("Date validation failed: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .errorMessage(ex.getMessage())
            .httpStatus(ex.getHttpStatus())
            .path(request.getRequestURI())
            .timestamp()
            .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * Handle Zoho API Exception
     */
    @ExceptionHandler(ZohoApiException.class)
    public ResponseEntity<ErrorResponse> handleZohoApiException(
            ZohoApiException ex,
            HttpServletRequest request) {
        
        logger.error("Zoho API error: {}", ex.getMessage(), ex);
        
        Map<String, Object> additionalInfo = new HashMap<>();
        if (ex.getZohoStatusCode() != null) {
            additionalInfo.put("zohoStatusCode", ex.getZohoStatusCode());
        }
        if (ex.getZohoErrorMessage() != null) {
            additionalInfo.put("zohoErrorMessage", ex.getZohoErrorMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .errorMessage(ex.getMessage())
            .httpStatus(ex.getHttpStatus())
            .path(request.getRequestURI())
            .additionalInfo(additionalInfo.isEmpty() ? null : additionalInfo)
            .timestamp()
            .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * Handle MCP Tool Exception
     */
    @ExceptionHandler(McpToolException.class)
    public ResponseEntity<ErrorResponse> handleMcpToolException(
            McpToolException ex,
            HttpServletRequest request) {
        
        logger.error("MCP tool execution failed: {}", ex.getMessage(), ex);
        
        Map<String, Object> additionalInfo = new HashMap<>();
        if (ex.getToolName() != null) {
            additionalInfo.put("toolName", ex.getToolName());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .errorMessage(ex.getMessage())
            .httpStatus(ex.getHttpStatus())
            .path(request.getRequestURI())
            .additionalInfo(additionalInfo.isEmpty() ? null : additionalInfo)
            .timestamp()
            .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * Handle Input Validation Exception
     */
    @ExceptionHandler(InputValidationException.class)
    public ResponseEntity<ErrorResponse> handleInputValidationException(
            InputValidationException ex,
            HttpServletRequest request) {
        
        logger.warn("Input validation failed: {}", ex.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = null;
        if (ex.getFieldErrors() != null) {
            fieldErrors = ex.getFieldErrors().entrySet().stream()
                .map(entry -> ErrorResponse.FieldError.builder()
                    .field(entry.getKey())
                    .message(entry.getValue())
                    .build())
                .collect(Collectors.toList());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ex.getErrorCode())
            .errorMessage(ex.getMessage())
            .httpStatus(ex.getHttpStatus())
            .path(request.getRequestURI())
            .fieldErrors(fieldErrors)
            .timestamp()
            .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * Handle Jakarta Validation exceptions (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        logger.warn("Validation failed: {}", ex.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> ErrorResponse.FieldError.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .rejectedValue(error.getRejectedValue())
                .build())
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode("VALIDATION_ERROR")
            .errorMessage("Input validation failed")
            .httpStatus(HttpStatus.BAD_REQUEST.value())
            .path(request.getRequestURI())
            .fieldErrors(fieldErrors)
            .timestamp()
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle Constraint Violation Exception
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        
        logger.warn("Constraint violation: {}", ex.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
            .stream()
            .map(violation -> ErrorResponse.FieldError.builder()
                .field(violation.getPropertyPath().toString())
                .message(violation.getMessage())
                .rejectedValue(violation.getInvalidValue())
                .build())
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode("CONSTRAINT_VIOLATION")
            .errorMessage("Constraint validation failed")
            .httpStatus(HttpStatus.BAD_REQUEST.value())
            .path(request.getRequestURI())
            .fieldErrors(fieldErrors)
            .timestamp()
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle Type Mismatch Exception
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        logger.warn("Type mismatch: {}", ex.getMessage());
        
        String message = String.format(
            "Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(),
            ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode("TYPE_MISMATCH")
            .errorMessage(message)
            .httpStatus(HttpStatus.BAD_REQUEST.value())
            .path(request.getRequestURI())
            .timestamp()
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle Generic Runtime Exception
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        
        logger.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode("INTERNAL_SERVER_ERROR")
            .errorMessage("An unexpected error occurred. Please try again later.")
            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .path(request.getRequestURI())
            .timestamp()
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handle All Other Exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        logger.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode("UNKNOWN_ERROR")
            .errorMessage("An unexpected error occurred. Please contact support.")
            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .path(request.getRequestURI())
            .timestamp()
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
