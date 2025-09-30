package com.platform.audit.api.dto;

/**
 * Wrapper class for validation results in audit log API requests.
 */
public class ValidatedRequest {

    private final com.platform.audit.internal.AuditRequestValidator.ValidationResult validationResult;
    private final java.time.Instant dateFrom;
    private final java.time.Instant dateTo;

    public ValidatedRequest(com.platform.audit.internal.AuditRequestValidator.ValidationResult validationResult,
                           java.time.Instant dateFrom,
                           java.time.Instant dateTo) {
        this.validationResult = validationResult;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public boolean valid() {
        return validationResult.valid();
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

    public com.platform.audit.internal.AuditRequestValidator.ValidationResult validation() {
        return validationResult;
    }
}
