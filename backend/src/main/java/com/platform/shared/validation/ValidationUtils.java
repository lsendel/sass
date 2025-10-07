package com.platform.shared.validation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized validation utilities for common validation patterns across the platform.
 *
 * <p>This utility class provides standardized validation methods and error responses
 * to ensure consistency across all controllers and reduce code duplication.
 *
 * @since 1.0.0
 */
@Component
public final class ValidationUtils {

    private ValidationUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Result of UUID validation containing the parsed UUID and validation status.
     */
    public static class UuidValidationResult {
        private final boolean valid;
        private final UUID uuid;
        private final String errorMessage;
        private final String errorCode;

        private UuidValidationResult(boolean valid, UUID uuid, String errorMessage, String errorCode) {
            this.valid = valid;
            this.uuid = uuid;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        public static UuidValidationResult success(UUID uuid) {
            return new UuidValidationResult(true, uuid, null, null);
        }

        public static UuidValidationResult failure(String errorMessage, String errorCode) {
            return new UuidValidationResult(false, null, errorMessage, errorCode);
        }

        public boolean isValid() {
            return valid;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    /**
     * Validates and parses a UUID string with comprehensive error handling.
     *
     * @param uuidString the string to validate and parse
     * @param fieldName the field name for error messages
     * @return validation result containing the UUID or error details
     */
    public static UuidValidationResult validateUuid(String uuidString, String fieldName) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            return UuidValidationResult.failure(
                fieldName + " is required",
                "MISSING_REQUIRED_FIELD"
            );
        }

        try {
            UUID uuid = UUID.fromString(uuidString.trim());
            return UuidValidationResult.success(uuid);
        } catch (IllegalArgumentException e) {
            return UuidValidationResult.failure(
                fieldName + " must be a valid UUID format",
                "INVALID_UUID_FORMAT"
            );
        }
    }

    /**
     * Creates a standardized error response for UUID validation failures.
     *
     * @param fieldName the name of the field that failed validation
     * @return ResponseEntity with standardized error format
     */
    public static ResponseEntity<?> createUuidErrorResponse(String fieldName) {
        return ResponseEntity.badRequest()
            .body(createErrorResponse(
                "INVALID_UUID_FORMAT",
                fieldName + " must be a valid UUID format"
            ));
    }

    /**
     * Creates a standardized error response for validation failures.
     *
     * @param validationResult the validation result containing error details
     * @return ResponseEntity with standardized error format
     */
    public static ResponseEntity<?> createValidationErrorResponse(UuidValidationResult validationResult) {
        return ResponseEntity.badRequest()
            .body(createErrorResponse(
                validationResult.getErrorCode(),
                validationResult.getErrorMessage()
            ));
    }

    /**
     * Creates a standardized error response map.
     *
     * @param code the error code
     * @param message the error message
     * @return error response map
     */
    public static Map<String, Object> createErrorResponse(String code, String message) {
        return Map.of(
            "code", code,
            "message", message,
            "timestamp", Instant.now(),
            "correlationId", UUID.randomUUID().toString()
        );
    }

    /**
     * Validates page size parameters for pagination.
     *
     * @param size the page size to validate
     * @param maxSize the maximum allowed page size
     * @return validation result
     */
    public static ValidationResult validatePageSize(int size, int maxSize) {
        if (size < 1) {
            return ValidationResult.failure(
                "PAGE_SIZE_TOO_SMALL",
                "Page size must be at least 1"
            );
        }
        if (size > maxSize) {
            return ValidationResult.failure(
                "PAGE_SIZE_TOO_LARGE",
                "Page size must not exceed " + maxSize
            );
        }
        return ValidationResult.success();
    }

    /**
     * Validates page number parameters for pagination.
     *
     * @param page the page number to validate
     * @return validation result
     */
    public static ValidationResult validatePageNumber(int page) {
        if (page < 0) {
            return ValidationResult.failure(
                "PAGE_NUMBER_NEGATIVE",
                "Page number must be non-negative"
            );
        }
        return ValidationResult.success();
    }

    /**
     * General validation result for non-UUID validations.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorCode;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorCode, String errorMessage) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(String errorCode, String errorMessage) {
            return new ValidationResult(false, errorCode, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}