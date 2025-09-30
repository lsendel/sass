package com.platform.audit.api;

import com.platform.audit.api.dto.AuditLogExportRequestDTO;
import com.platform.audit.internal.AuditRequestValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

import static com.platform.audit.internal.AuditConstants.ERROR_ACCESS_DENIED;
import static com.platform.audit.internal.AuditConstants.ERROR_INTERNAL_ERROR;
import static com.platform.audit.internal.AuditConstants.ERROR_RATE_LIMIT_EXCEEDED;
import static com.platform.audit.internal.AuditConstants.ERROR_USER_NOT_AUTHENTICATED;
import static com.platform.audit.internal.AuditConstants.MSG_ACCESS_DENIED_LOGS;
import static com.platform.audit.internal.AuditConstants.MSG_UNABLE_RETRIEVE_LOGS;
import static com.platform.audit.internal.AuditConstants.MSG_USER_AUTH_REQUIRED;

/**
 * Helper service to extract validation and response creation logic from controller.
 * Improves testability and follows Single Responsibility Principle.
 */
@Component
public final class AuditControllerHelper {

    public AuditRequestValidator.ValidationResult validateExportRequest(final AuditLogExportRequestDTO exportRequest) {
        // Validate format
        try {
            com.platform.audit.internal.AuditLogExportRequest.ExportFormat.valueOf(exportRequest.format());
        } catch (IllegalArgumentException e) {
            return AuditRequestValidator.ValidationResult.invalid("INVALID_FORMAT",
                "Invalid export format: " + exportRequest.format());
        }

        // Validate date range
        if (exportRequest.dateFrom() != null && exportRequest.dateTo() != null
                && exportRequest.dateFrom().isAfter(exportRequest.dateTo())) {
            return AuditRequestValidator.ValidationResult.invalid("INVALID_DATE_RANGE", "Date range is invalid");
        }

        // Validate search text length
        if (exportRequest.search() != null && exportRequest.search().length() > 1000) {
            return AuditRequestValidator.ValidationResult.invalid("SEARCH_TOO_LONG", "Search text too long");
        }

        return AuditRequestValidator.ValidationResult.success();
    }

    public ResponseEntity<?> createValidationErrorResponse(final AuditRequestValidator.ValidationResult validationResult) {
        return ResponseEntity.badRequest()
            .body(createErrorResponse(validationResult.errorCode(), validationResult.errorMessage()));
    }

    public ResponseEntity<?> createUnauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(createErrorResponse(ERROR_USER_NOT_AUTHENTICATED, MSG_USER_AUTH_REQUIRED));
    }

    public ResponseEntity<?> createRateLimitResponse(final String message) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(createErrorResponse(ERROR_RATE_LIMIT_EXCEEDED, message));
    }

    public ResponseEntity<?> createAccessDeniedResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(createErrorResponse(ERROR_ACCESS_DENIED, MSG_ACCESS_DENIED_LOGS));
    }

    public ResponseEntity<?> createInternalErrorResponse() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse(ERROR_INTERNAL_ERROR, MSG_UNABLE_RETRIEVE_LOGS));
    }

    public Map<String, Object> createErrorResponse(final String code, final String message) {
        return Map.of(
            "code", code,
            "message", message,
            "timestamp", Instant.now()
        );
    }
}
