package com.zoho.eservemcp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Standard error response structure for all API errors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success = false;

    private String errorCode;

    private String errorMessage;

    private String timestamp;

    private String path;

    private Integer httpStatus;

    private List<FieldError> fieldErrors;

    private Map<String, Object> additionalInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    // Builder method to set timestamp automatically
    public static class ErrorResponseBuilder {
        public ErrorResponseBuilder timestamp() {
            this.timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return this;
        }
    }

    // Factory methods
    public static ErrorResponse of(String errorCode, String errorMessage, int httpStatus) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .httpStatus(httpStatus)
                .timestamp()
                .build();
    }

    public static ErrorResponse of(String errorCode, String errorMessage, int httpStatus, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .httpStatus(httpStatus)
                .path(path)
                .timestamp()
                .build();
    }
}
