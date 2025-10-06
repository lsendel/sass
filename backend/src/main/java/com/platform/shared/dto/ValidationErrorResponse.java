package com.platform.shared.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Specialized error response for validation failures.
 * Includes field-specific error details for form validation.
 */
@Data
@Builder
public class ValidationErrorResponse {

    /**
     * General validation error message.
     */
    private String message;

    /**
     * Unique correlation ID for tracking requests.
     */
    private String correlationId;

    /**
     * Timestamp when the validation error occurred.
     */
    private Instant timestamp;

    /**
     * API path where the validation failed.
     */
    private String path;

    /**
     * Field-specific validation errors.
     * Key: field name, Value: error message
     */
    private Map<String, String> fieldErrors;

    /**
     * Total number of validation errors.
     */
    public int getErrorCount() {
        return fieldErrors != null ? fieldErrors.size() : 0;
    }
}