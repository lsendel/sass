package com.platform.audit.internal;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import static com.platform.audit.internal.AuditConstants.ERROR_INVALID_DATE_FORMAT;
import static com.platform.audit.internal.AuditConstants.ERROR_INVALID_DATE_RANGE;
import static com.platform.audit.internal.AuditConstants.ERROR_INVALID_FORMAT;
import static com.platform.audit.internal.AuditConstants.ERROR_INVALID_PAGE_SIZE;
import static com.platform.audit.internal.AuditConstants.MAX_PAGE_SIZE;
import static com.platform.audit.internal.AuditConstants.MSG_INVALID_DATE_FROM;
import static com.platform.audit.internal.AuditConstants.MSG_INVALID_DATE_RANGE;
import static com.platform.audit.internal.AuditConstants.MSG_INVALID_DATE_TO;
import static com.platform.audit.internal.AuditConstants.MSG_PAGE_SIZE_EXCEEDED;

/**
 * Validator for audit log request parameters.
 * Centralizes validation logic to maintain consistency and reduce controller complexity.
 */
@Component
public class AuditRequestValidator {

    /**
     * Validation result containing error information if validation fails.
     */
    public record ValidationResult(boolean valid, String errorCode, String errorMessage) {
        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult invalid(final String errorCode, final String errorMessage) {
            return new ValidationResult(false, errorCode, errorMessage);
        }
    }

    /**
     * Validate page size parameter.
     */
    public ValidationResult validatePageSize(final int size) {
        if (size > MAX_PAGE_SIZE) {
            return ValidationResult.invalid(ERROR_INVALID_PAGE_SIZE, MSG_PAGE_SIZE_EXCEEDED);
        }
        return ValidationResult.success();
    }

    /**
     * Parse and validate date parameter.
     */
    public ParsedDateResult parseDate(final String dateStr, final String fieldName) {
        if (dateStr == null) {
            return new ParsedDateResult(null, ValidationResult.success());
        }

        try {
            Instant instant = Instant.parse(dateStr);
            return new ParsedDateResult(instant, ValidationResult.success());
        } catch (DateTimeParseException e) {
            String errorCode = "dateFrom".equals(fieldName) ? ERROR_INVALID_DATE_FORMAT : ERROR_INVALID_DATE_FORMAT;
            String errorMsg = "dateFrom".equals(fieldName) ? MSG_INVALID_DATE_FROM : MSG_INVALID_DATE_TO;
            return new ParsedDateResult(null, ValidationResult.invalid(errorCode, errorMsg));
        }
    }

    /**
     * Validate date range.
     */
    public ValidationResult validateDateRange(final Instant dateFrom, final Instant dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            return ValidationResult.invalid(ERROR_INVALID_DATE_RANGE, MSG_INVALID_DATE_RANGE);
        }
        return ValidationResult.success();
    }

    /**
     * Validate export format.
     */
    public ValidationResult validateExportFormat(final String formatStr) {
        try {
            AuditLogExportRequest.ExportFormat.valueOf(formatStr);
            return ValidationResult.success();
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid(ERROR_INVALID_FORMAT, "Invalid export format: " + formatStr);
        }
    }

    /**
     * Result of date parsing operation.
     */
    public record ParsedDateResult(Instant date, ValidationResult validation) { }
}