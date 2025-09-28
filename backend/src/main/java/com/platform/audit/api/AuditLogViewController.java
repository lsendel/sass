package com.platform.audit.api;

import com.platform.audit.api.dto.*;
import com.platform.audit.internal.AuditLogFilter;
import com.platform.audit.internal.AuditLogViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for user-facing audit log viewer endpoints.
 * Integrated with service layer for proper audit log viewing functionality.
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('USER')")
public class AuditLogViewController {

    private static final Logger log = LoggerFactory.getLogger(AuditLogViewController.class);

    private final AuditLogViewService auditLogViewService;

    public AuditLogViewController(AuditLogViewService auditLogViewService) {
        this.auditLogViewService = auditLogViewService;
    }

    /**
     * Retrieve paginated audit log entries with filtering and search capabilities.
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getAuditLogs(
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

        log.debug("Getting audit logs - page: {}, size: {}, search: '{}'", page, size, search);

        try {
            // Validate page size
            if (size > 100) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_PAGE_SIZE", "Page size cannot exceed 100"));
            }

            // Parse and validate date range
            Instant instantFrom = null;
            Instant instantTo = null;

            if (dateFrom != null) {
                try {
                    instantFrom = Instant.parse(dateFrom);
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                        .body(createErrorResponse("INVALID_DATE_FORMAT", "Invalid dateFrom format"));
                }
            }

            if (dateTo != null) {
                try {
                    instantTo = Instant.parse(dateTo);
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                        .body(createErrorResponse("INVALID_DATE_FORMAT", "Invalid dateTo format"));
                }
            }

            if (instantFrom != null && instantTo != null && instantFrom.isAfter(instantTo)) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_DATE_RANGE", "dateFrom must be before dateTo"));
            }

            // Get current user ID
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("USER_NOT_AUTHENTICATED", "User authentication required"));
            }

            // Create filter
            AuditLogFilter filter = new AuditLogFilter(
                null, // Will be set by permission service
                null, // Will be set by permission service
                instantFrom,
                instantTo,
                search,
                actionTypes,
                null, // Actor emails not supported in this endpoint
                null, // Include system actions determined by permissions
                page,
                size
            );

            // Get audit logs through service
            var searchResponse = auditLogViewService.getAuditLogs(userId, filter);

            // Convert to DTO format
            AuditLogResponseDTO response = AuditLogResponseDTO.of(
                searchResponse.entries(),
                searchResponse.pageNumber(),
                searchResponse.pageSize(),
                searchResponse.totalElements()
            );

            log.debug("Retrieved {} audit log entries for user: {}", response.content().size(), userId);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("Security violation in audit log access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse("ACCESS_DENIED", "Access denied to audit logs"));
        } catch (Exception e) {
            log.error("Error retrieving audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Unable to retrieve audit logs"));
        }
    }

    /**
     * Get detailed information for a specific audit log entry.
     */
    @GetMapping("/logs/{id}")
    public ResponseEntity<?> getAuditLogDetails(@PathVariable UUID id) {
        log.debug("Getting audit log detail for ID: {}", id);

        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("USER_NOT_AUTHENTICATED", "User authentication required"));
            }

            Optional<AuditLogDetailDTO> detailOpt = auditLogViewService.getAuditLogDetail(userId, id);

            if (detailOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            log.debug("Retrieved audit log detail for ID: {} and user: {}", id, userId);
            return ResponseEntity.ok(detailOpt.get());

        } catch (SecurityException e) {
            log.warn("Security violation accessing audit log {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse("ACCESS_DENIED", "Access denied to this audit log entry"));
        } catch (Exception e) {
            log.error("Error retrieving audit log detail for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Unable to retrieve audit log detail"));
        }
    }

    /**
     * Get current authenticated user ID.
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // TODO: Extract user ID from authentication principal
        // This is a placeholder implementation
        String principal = authentication.getName();
        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException e) {
            log.warn("Could not parse user ID from authentication principal: {}", principal);
            return null;
        }
    }

    /**
     * Initiate audit log export.
     */
    @PostMapping("/export")
    public ResponseEntity<?> exportAuditLogs(@RequestBody Object request) {
        // Intentionally not implemented - TDD RED phase
        return ResponseEntity.notFound().build();
    }

    /**
     * Get audit log export status.
     */
    @GetMapping("/export/{exportId}/status")
    public ResponseEntity<?> getExportStatus(@PathVariable UUID exportId) {
        // Intentionally not implemented - TDD RED phase
        return ResponseEntity.notFound().build();
    }

    /**
     * Download completed audit log export.
     */
    @GetMapping("/export/{token}/download")
    public ResponseEntity<?> downloadExport(@PathVariable String token) {
        // Intentionally not implemented - TDD RED phase
        return ResponseEntity.notFound().build();
    }

    /**
     * Create standardized error response.
     */
    private Map<String, Object> createErrorResponse(String code, String message) {
        return Map.of(
            "code", code,
            "message", message,
            "timestamp", Instant.now()
        );
    }
}