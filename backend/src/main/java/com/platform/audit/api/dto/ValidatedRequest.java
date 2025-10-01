package com.platform.audit.api.dto;

/**
 * Wrapper class for validation results in audit log API requests.
 */
public class ValidatedRequest {

    /**
     * Simple validation result record.
     */
    public record ValidationResult(boolean isValid, String errorCode, String errorMessage) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(String errorCode, String errorMessage) {
            return new ValidationResult(false, errorCode, errorMessage);
        }
    }

    private final ValidationResult validationResult;
    private final java.time.Instant dateFrom;
    private final java.time.Instant dateTo;

    public ValidatedRequest(final ValidationResult validationResult,
                           final java.time.Instant dateFrom,
                           final java.time.Instant dateTo) {
        this.validationResult = validationResult;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public static ValidatedRequest of(final String dateFrom, final String dateTo) {
        return new ValidatedRequest(ValidationResult.valid(), parseDate(dateFrom), parseDate(dateTo));
    }

    public static ValidatedRequest of(final String dateFrom, final String dateTo, final String searchTerm) {
        return new ValidatedRequest(ValidationResult.valid(), parseDate(dateFrom), parseDate(dateTo));
    }

    private static java.time.Instant parseDate(final String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return java.time.Instant.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean valid() {
        return validationResult.isValid();
    }

    public String errorCode() {
        return validationResult.errorCode();
    }

    public String errorMessage() {
        return validationResult.errorMessage();
    }

    public java.time.Instant dateFrom() {
        return dateFrom;
    }

    public java.time.Instant dateTo() {
        return dateTo;
    }

    public ValidationResult validation() {
        return validationResult;
    }
}
