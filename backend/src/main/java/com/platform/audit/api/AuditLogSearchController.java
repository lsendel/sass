package com.platform.audit.api;

import com.platform.audit.api.dto.AuditLogFilterRequest;
import com.platform.audit.api.dto.AuditLogSearchResponse;
import com.platform.audit.api.dto.AuditLogDetailDTO;
import com.platform.audit.api.dto.PaginationRequest;
import com.platform.audit.api.dto.SortRequest;
import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditRequestValidator;
import com.platform.shared.validation.ValidationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.platform.audit.internal.AuditConstants.ERROR_ACCESS_DENIED;
import static com.platform.audit.internal.AuditConstants.ERROR_INTERNAL_ERROR;
import static com.platform.audit.internal.AuditConstants.LOG_SECURITY_VIOLATION_ACCESS;
import static com.platform.audit.internal.AuditConstants.MSG_ACCESS_DENIED_LOGS;
import static com.platform.audit.internal.AuditConstants.MSG_UNABLE_RETRIEVE_LOGS;

/**
 * REST controller for audit log search and detail operations.
 *
 * <p>This controller handles:
 * <ul>
 *   <li>Paginated audit log search with filtering</li>
 *   <li>Individual audit log detail retrieval</li>
 *   <li>Search parameter validation</li>
 * </ul>
 *
 * <p>Refactored from AuditLogViewController to follow Single Responsibility Principle.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/audit/logs")
public class AuditLogSearchController {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogSearchController.class);

    private final AuditLogViewService auditLogViewService;
    private final AuditRequestValidator validator;

    public AuditLogSearchController(
            AuditLogViewService auditLogViewService,
            AuditRequestValidator validator) {
        this.auditLogViewService = auditLogViewService;
        this.validator = validator;
    }

    /**
     * Retrieve paginated audit log entries with filtering and search capabilities.
     *
     * <p>Supports filtering by:
     * <ul>
     *   <li>Date range (dateFrom, dateTo)</li>
     *   <li>Text search in action descriptions</li>
     *   <li>Action types, resource types, outcomes</li>
     *   <li>Pagination and sorting</li>
     * </ul>
     *
     * @param page page number (0-based, default: 0)
     * @param size page size (1-100, default: 50)
     * @param dateFrom start date filter (ISO-8601 format)
     * @param dateTo end date filter (ISO-8601 format)
     * @param search text search term
     * @param actionTypes list of action types to include
     * @param resourceTypes list of resource types to include
     * @param outcomes list of outcomes to include
     * @param sortField field to sort by (default: timestamp)
     * @param sortDirection sort direction ASC/DESC (default: DESC)
     * @return paginated audit log response
     */
    @GetMapping
    public ResponseEntity<?> searchAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> actionTypes,
            @RequestParam(required = false) List<String> resourceTypes,
            @RequestParam(required = false) List<String> outcomes,
            @RequestParam(defaultValue = "timestamp") String sortField,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        LOG.debug("Searching audit logs - page: {}, size: {}, search: '{}'", page, size, search);

        try {
            // Validate pagination parameters
            var paginationValidation = ValidationUtils.validatePageSize(size, PaginationRequest.MAX_PAGE_SIZE);
            if (!paginationValidation.isValid()) {
                return ResponseEntity.badRequest()
                    .body(ValidationUtils.createErrorResponse(
                        paginationValidation.getErrorCode(),
                        paginationValidation.getErrorMessage()
                    ));
            }

            var pageValidation = ValidationUtils.validatePageNumber(page);
            if (!pageValidation.isValid()) {
                return ResponseEntity.badRequest()
                    .body(ValidationUtils.createErrorResponse(
                        pageValidation.getErrorCode(),
                        pageValidation.getErrorMessage()
                    ));
            }

            // Get authenticated user
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return createUnauthorizedResponse();
            }

            // Parse and validate dates
            var dateValidation = validateDateParameters(dateFrom, dateTo);
            if (!dateValidation.isValid()) {
                return ResponseEntity.badRequest()
                    .body(ValidationUtils.createErrorResponse(
                        dateValidation.getErrorCode(),
                        dateValidation.getErrorMessage()
                    ));
            }

            // Create filter request using parameter object pattern
            var filterRequest = createFilterRequest(
                dateValidation.getDateFrom(),
                dateValidation.getDateTo(),
                search, actionTypes, resourceTypes, outcomes,
                page, size, sortField, sortDirection
            );

            // Execute search
            var searchResponse = auditLogViewService.getAuditLogs(userId, filterRequest.toInternalFilter());

            LOG.debug("Retrieved {} audit log entries for user: {}",
                searchResponse.entries().size(), userId);
            return ResponseEntity.ok(searchResponse);

        } catch (SecurityException e) {
            return handleSecurityException(e);
        } catch (Exception e) {
            return handleGenericException(e, "Error searching audit logs");
        }
    }

    /**
     * Get detailed information for a specific audit log entry.
     *
     * @param id the audit log entry ID (UUID format)
     * @return detailed audit log information
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAuditLogDetail(@PathVariable String id) {
        LOG.debug("Getting audit log detail for ID: {}", id);

        try {
            // Validate UUID format using centralized utility
            var uuidValidation = ValidationUtils.validateUuid(id, "auditLogId");
            if (!uuidValidation.isValid()) {
                return ValidationUtils.createValidationErrorResponse(uuidValidation);
            }

            UUID userId = getCurrentUserId();
            if (userId == null) {
                return createUnauthorizedResponse();
            }

            Optional<AuditLogDetailDTO> detailOpt = auditLogViewService.getAuditLogDetail(
                userId, uuidValidation.getUuid()
            );

            if (detailOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ValidationUtils.createErrorResponse(
                        "AUDIT_LOG_NOT_FOUND",
                        "Audit log entry not found or not accessible"
                    ));
            }

            LOG.debug("Retrieved audit log detail for ID: {} and user: {}", id, userId);
            return ResponseEntity.ok(detailOpt.get());

        } catch (SecurityException e) {
            return handleSecurityException(e);
        } catch (Exception e) {
            return handleGenericException(e, "Error retrieving audit log detail for ID: " + id);
        }
    }

    /**
     * Validates date parameters for search requests.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Date formats must be ISO-8601 compliant</li>
     *   <li>dateFrom must be before dateTo if both provided</li>
     *   <li>Date range cannot exceed 1 year for performance</li>
     * </ul>
     */
    private DateValidationResult validateDateParameters(String dateFrom, String dateTo) {
        var fromResult = validator.parseDate(dateFrom, "dateFrom");
        if (!fromResult.validation().isValid()) {
            return DateValidationResult.failure(fromResult.validation());
        }

        var toResult = validator.parseDate(dateTo, "dateTo");
        if (!toResult.validation().isValid()) {
            return DateValidationResult.failure(toResult.validation());
        }

        var rangeValidation = validator.validateDateRange(fromResult.date(), toResult.date());
        if (!rangeValidation.isValid()) {
            return DateValidationResult.failure(rangeValidation);
        }

        return DateValidationResult.success(fromResult.date(), toResult.date());
    }

    private AuditLogFilterRequest createFilterRequest(
            Instant dateFrom, Instant dateTo, String search,
            List<String> actionTypes, List<String> resourceTypes, List<String> outcomes,
            int page, int size, String sortField, String sortDirection) {

        return AuditLogFilterRequest.from(
            dateFrom, dateTo, search, actionTypes, resourceTypes, outcomes,
            page, size, sortField, sortDirection
        );
    }

    private UUID getCurrentUserId() {
        return com.platform.shared.security.SecurityUtils.getCurrentUserId()
                .orElse(null);
    }

    private ResponseEntity<?> createUnauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ValidationUtils.createErrorResponse(
                "USER_NOT_AUTHENTICATED",
                "User authentication required"
            ));
    }

    private ResponseEntity<?> handleSecurityException(SecurityException e) {
        LOG.warn("{}: {}", LOG_SECURITY_VIOLATION_ACCESS, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ValidationUtils.createErrorResponse(ERROR_ACCESS_DENIED, MSG_ACCESS_DENIED_LOGS));
    }

    private ResponseEntity<?> handleGenericException(Exception e, String logMessage) {
        LOG.error(logMessage, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ValidationUtils.createErrorResponse(ERROR_INTERNAL_ERROR, MSG_UNABLE_RETRIEVE_LOGS));
    }

    /**
     * Internal record for date validation results.
     */
    private record DateValidationResult(
            boolean valid,
            Instant dateFrom,
            Instant dateTo,
            ValidationUtils.ValidationResult validation
    ) {
        static DateValidationResult success(Instant dateFrom, Instant dateTo) {
            return new DateValidationResult(true, dateFrom, dateTo, null);
        }

        static DateValidationResult failure(ValidationUtils.ValidationResult validation) {
            return new DateValidationResult(false, null, null, validation);
        }

        boolean isValid() {
            return valid;
        }

        String getErrorCode() {
            return validation != null ? validation.getErrorCode() : null;
        }

        String getErrorMessage() {
            return validation != null ? validation.getErrorMessage() : null;
        }

        Instant getDateFrom() {
            return dateFrom;
        }

        Instant getDateTo() {
            return dateTo;
        }
    }
}