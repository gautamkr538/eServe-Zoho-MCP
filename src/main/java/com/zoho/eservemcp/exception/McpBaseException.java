package com.zoho.eservemcp.exception;

/**
 * Base exception class for all MCP server exceptions
 */
public abstract class McpBaseException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;
    
    protected McpBaseException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    protected McpBaseException(String message, Throwable cause, String errorCode, int httpStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
}
