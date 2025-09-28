package com.platform.audit.api;

import com.platform.audit.api.dto.*;
import com.platform.audit.internal.AuditLogExportRequest;
import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditLogFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for audit log export functionality.
 * Integrated with service layer for proper export processing.
 */
@RestController
@RequestMapping("/api/audit/export")
@PreAuthorize("hasRole('USER')")
public class AuditLogExportController {

    private static final Logger log = LoggerFactory.getLogger(AuditLogExportController.class);

    private final AuditLogExportService auditLogExportService;

    public AuditLogExportController(AuditLogExportService auditLogExportService) {
        this.auditLogExportService = auditLogExportService;
    }

    /**
     * Request audit log export in specified format.
     */
    @PostMapping
    public ResponseEntity<?> requestExport(
            @RequestBody AuditLogExportRequestDTO exportRequest) {

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
                format = AuditLogExportRequest.ExportFormat.valueOf(exportRequest.format().name().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_FORMAT", "Invalid export format: " + exportRequest.format()));
            }

            // Extract dates from filters
            Instant dateFrom = null;
            Instant dateTo = null;
            String search = null;

            if (exportRequest.filters() != null) {
                dateFrom = exportRequest.filters().dateFrom();
                dateTo = exportRequest.filters().dateTo();
                search = exportRequest.filters().searchText();
            }

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
                null, // Actor emails not supported
                null, // Include system actions determined by permissions
                0,
                50 // Page size not relevant for export
            );

            // Request export
            var exportResponse = auditLogExportService.requestExport(userId, format, filter);

            log.info("Created export request: {} for user: {}", exportResponse.exportId(), userId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(exportResponse);

        } catch (IllegalStateException e) {
            log.warn("Export request rejected for user: {}", e.getMessage());
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
     * Get export status by export ID.
     */
    @GetMapping("/{exportId}/status")
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
                status.exportId(),
                status.status(),
                status.format(),
                status.progressPercentage(),
                status.totalRecords(),
                status.processedRecords(),
                status.createdAt(),
                status.startedAt(),
                status.completedAt(),
                status.downloadToken(),
                status.downloadExpiresAt(),
                status.downloadCount(),
                status.maxDownloads(),
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
     * Download export file using secure token.
     */
    @GetMapping("/{token}/download")
    public ResponseEntity<?> downloadExport(@PathVariable String token) {

        log.debug("Processing download request for token: {}", token);

        try {
            var downloadOpt = auditLogExportService.getExportDownload(token);

            if (downloadOpt.isEmpty()) {
                return ResponseEntity.notFound()
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