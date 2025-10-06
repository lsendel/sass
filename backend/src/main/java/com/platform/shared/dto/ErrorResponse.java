package com.platform.shared.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Standardized error response for API endpoints.
 * Provides consistent error format across all controllers.
 */
@Data
@Builder
public class ErrorResponse {

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * Unique correlation ID for tracking requests across services.
     */
    private String correlationId;

    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;

    /**
     * API path where the error occurred.
     */
    private String path;

    /**
     * Optional error code for client categorization.
     */
    private String errorCode;

    /**
     * Optional additional details (only in development/debug mode).
     */
    private Object details;
}