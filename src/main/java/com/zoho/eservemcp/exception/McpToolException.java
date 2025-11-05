package com.zoho.eservemcp.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown during MCP tool execution
 */
public class McpToolException extends McpBaseException {
    
    private static final String ERROR_CODE = "MCP_TOOL_EXECUTION_ERROR";
    
    private final String toolName;
    
    public McpToolException(String message) {
        super(message, ERROR_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        this.toolName = null;
    }
    
    public McpToolException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        this.toolName = null;
    }
    
    public McpToolException(String toolName, String message, Throwable cause) {
        super(
            String.format("Tool '%s' execution failed: %s", toolName, message),
            cause,
            ERROR_CODE,
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        this.toolName = toolName;
    }
    
    public String getToolName() {
        return toolName;
    }
}
