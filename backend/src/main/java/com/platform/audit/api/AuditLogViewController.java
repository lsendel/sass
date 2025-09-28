package com.platform.audit.api;

import com.platform.audit.api.dto.*;
import com.platform.audit.internal.AuditLogFilter;
import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditLogExportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final AuditLogExportService auditLogExportService;

    public AuditLogViewController(AuditLogViewService auditLogViewService,
                                 AuditLogExportService auditLogExportService) {
        this.auditLogViewService = auditLogViewService;
        this.auditLogExportService = auditLogExportService;
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
                resourceTypes,
                null, // Actor emails not supported in this endpoint
                null, // Include system actions determined by permissions
                page,
                size,
                sortField,
                sortDirection
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
    public ResponseEntity<?> exportAuditLogs(@RequestBody AuditLogExportRequestDTO exportRequest) {
        log.debug("Processing export request: {}", exportRequest);

        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("USER_NOT_AUTHENTICATED", "User authentication required"));
            }

            // Validate format
            AuditLogExportRequest.ExportFormat format;
            try {
                format = AuditLogExportRequest.ExportFormat.valueOf(exportRequest.format());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_FORMAT", "Invalid export format: " + exportRequest.format()));
            }

            // Extract filter criteria
            Instant dateFrom = exportRequest.dateFrom();
            Instant dateTo = exportRequest.dateTo();
            String search = exportRequest.search();

            if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_DATE_RANGE", "dateFrom must be before dateTo"));
            }

            // Create filter
            AuditLogFilter filter = new AuditLogFilter(
                null, // Will be set by permission service
                null, // Will be set by permission service
                dateFrom,
                dateTo,
                search,
                null, // actionTypes - would need conversion from DTO enum to String list
                null, // resourceTypes - not supported in export
                null, // Actor emails not supported
                null, // Include system actions determined by permissions
                0,
                50, // Page size not relevant for export
                "timestamp",
                "DESC"
            );

            // Request export
            var exportResponse = auditLogExportService.requestExport(userId, format, filter);

            log.info("Created export request: {} for user: {}", exportResponse.getExportId(), userId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(exportResponse);

        } catch (IllegalStateException e) {
            log.warn("Export request rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(createErrorResponse("RATE_LIMIT_EXCEEDED", e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Security violation in export request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse("ACCESS_DENIED", "Access denied for export"));
        } catch (Exception e) {
            log.error("Error processing export request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Unable to process export request"));
        }
    }

    /**
     * Get audit log export status.
     */
    @GetMapping("/export/{exportId}/status")
    public ResponseEntity<?> getExportStatus(@PathVariable UUID exportId) {
        log.debug("Getting export status for ID: {}", exportId);

        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("USER_NOT_AUTHENTICATED", "User authentication required"));
            }

            var statusOpt = auditLogExportService.getExportStatus(userId, exportId);

            if (statusOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            var status = statusOpt.get();

            // Convert to DTO
            ExportStatusResponseDTO response = new ExportStatusResponseDTO(
                status.exportId().toString(),
                status.status().toString(),
                status.progressPercentage(),
                status.createdAt(),
                status.completedAt(),
                status.downloadToken(),
                status.totalRecords() != null ? status.totalRecords() : 0L,
                status.errorMessage()
            );

            log.debug("Retrieved export status for ID: {} and user: {}", exportId, userId);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("Security violation accessing export status {}: {}", exportId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse("ACCESS_DENIED", "Access denied to this export"));
        } catch (Exception e) {
            log.error("Error retrieving export status for ID: {}", exportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("INTERNAL_ERROR", "Unable to retrieve export status"));
        }
    }

    /**
     * Download completed audit log export.
     */
    @GetMapping("/export/{token}/download")
    public ResponseEntity<?> downloadExport(@PathVariable String token) {
        log.debug("Processing download request for token: {}", token);

        try {
            var downloadOpt = auditLogExportService.getExportDownload(token);

            if (downloadOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("DOWNLOAD_NOT_FOUND", "Download not found or expired"));
            }

            var download = downloadOpt.get();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(download.mimeType()));
            headers.setContentDispositionFormData("attachment", download.filename());

            if (download.fileSize() != null) {
                headers.setContentLength(download.fileSize());
            }

            log.info("Serving download: {} (size: {} bytes)", download.filename(), download.fileSize());

            return ResponseEntity.ok()
                .headers(headers)
                .body(download.resource());

        } catch (Exception e) {
            log.error("Error processing download for token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("DOWNLOAD_ERROR", "Unable to process download"));
        }
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