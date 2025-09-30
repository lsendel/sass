package com.platform.audit.api.dto;

import java.util.List;

/**
 * Request validation result for audit log operations.
 * Encapsulates validation outcomes and error handling.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final String errorCode;

    private ValidationResult(boolean valid, List<String> errors, String errorCode) {
        this.valid = valid;
        this.errors = errors;
        this.errorCode = errorCode;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, List.of(), null);
    }

    public static ValidationResult failure(String errorCode, List<String> errors) {
        return new ValidationResult(false, errors, errorCode);
    }

    public static ValidationResult failure(String errorCode, String error) {
        return new ValidationResult(false, List.of(error), errorCode);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
}
