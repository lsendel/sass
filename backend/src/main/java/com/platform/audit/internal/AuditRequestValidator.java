package com.platform.audit.internal;

import com.platform.audit.api.dto.ValidatedRequest;
import com.platform.audit.api.dto.ValidatedRequest.ValidationResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Service for validating audit log requests.
 * Performs input validation and converts requests to validated objects.
 */
@Service
public final class AuditRequestValidator {

    private static final int MAX_PAGE_SIZE = 1000;

    public ValidatedRequest validateDateRange(final String dateFrom, final String dateTo) {
        return ValidatedRequest.of(dateFrom, dateTo);
    }

    public ValidatedRequest validateSearchRequest(final String dateFrom, final String dateTo, final String searchTerm) {
        return ValidatedRequest.of(dateFrom, dateTo, searchTerm);
    }

    public ValidationResult validatePageSize(final int size) {
        if (size <= 0) {
            return ValidationResult.failure("INVALID_PAGE_SIZE", "Page size must be positive");
        }
        if (size > MAX_PAGE_SIZE) {
            return ValidationResult.failure("PAGE_SIZE_TOO_LARGE",
                "Page size cannot exceed " + MAX_PAGE_SIZE);
        }
        return ValidationResult.valid();
    }

    public ValidationResult validateDateRange(final Instant dateFrom, final Instant dateTo) {
        if (dateFrom == null && dateTo == null) {
            return ValidationResult.valid();
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            return ValidationResult.failure("INVALID_DATE_RANGE",
                "Date from must be before date to");
        }
        return ValidationResult.valid();
    }

    public ParsedDateResult parseDate(final String dateStr, final String fieldName) {
        if (dateStr == null || dateStr.isEmpty()) {
            return new ParsedDateResult(null, ValidationResult.valid());
        }
        try {
            Instant parsed = Instant.parse(dateStr);
            return new ParsedDateResult(parsed, ValidationResult.valid());
        } catch (DateTimeParseException e) {
            return new ParsedDateResult(null,
                ValidationResult.failure("INVALID_DATE_FORMAT",
                    "Invalid date format for " + fieldName));
        }
    }

    public record ParsedDateResult(Instant date, ValidationResult validation) {}
}
