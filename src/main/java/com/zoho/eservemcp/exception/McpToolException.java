package com.zoho.eservemcp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown during MCP tool execution, designed for agent/automation error handling.
 * Use this to signal: param/validation/network errors and to provide clear feedback to MCP clients/agents.
 */
@Getter
public class McpToolException extends McpBaseException {

    private static final String ERROR_CODE = "MCP_TOOL_EXECUTION_ERROR";

    private final String toolName;

    /**
     * Basic constructor—use for simple user-facing errors in agent workflows where only a message is relevant.
     * The toolName will be undefined.
     */
    public McpToolException(String message) {
        super(message, ERROR_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        this.toolName = null;
    }

    /**
     * Wraps another exception, without specifying tool name. Includes full cause.
     */
    public McpToolException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        this.toolName = null;
    }

    /**
     * Tool-aware and cause-aware constructor.
     * Use this for agent methods to clarify which tool failed, with both a concise message and a complete stack/cause.
     */
    public McpToolException(String toolName, String message, Throwable cause) {
        super(
                String.format("Tool '%s' execution failed: %s", toolName, message),
                cause,
                ERROR_CODE,
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        this.toolName = toolName;
    }

    /**
     * Tool-aware constructor for user/validation failures that do not have an underlying exception.
     * This is the most common usage in agent/method validation.
     */
    public McpToolException(String toolName, String message) {
        super(
                String.format("Tool '%s' execution failed: %s", toolName, message),
                ERROR_CODE,
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        this.toolName = toolName;
    }

}