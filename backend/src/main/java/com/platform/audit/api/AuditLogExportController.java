package com.platform.audit.api;

import com.platform.audit.api.dto.AuditLogExportRequestDTO;
import com.platform.audit.internal.AuditLogExportRequest;
import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditLogFilter;
import com.platform.shared.validation.ValidationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for audit log export operations.
 *
 * <p>This controller handles:
 * <ul>
 *   <li>Export request initiation (CSV, JSON, PDF)</li>
 *   <li>Export status tracking</li>
 *   <li>Export file download with secure tokens</li>
 * </ul>
 *
 * <p>Separated from search functionality to follow Single Responsibility Principle.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/audit/export")
public class AuditLogExportController {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogExportController.class);

    private final AuditLogExportService auditLogExportService;

    public AuditLogExportController(AuditLogExportService auditLogExportService) {
        this.auditLogExportService = auditLogExportService;
    }

    /**
     * Initiate audit log export request.
     *
     * <p>Business rules for exports:
     * <ul>
     *   <li>Supports CSV, JSON, and PDF formats</li>
     *   <li>Date range cannot exceed 1 year</li>
     *   <li>Maximum 100,000 records per export</li>
     *   <li>Rate limited to 5 exports per user per hour</li>
     * </ul>
     *
     * @param exportRequest the export configuration
     * @return export initiation response with tracking ID
     */
    @PostMapping
    public ResponseEntity<?> initiateExport(@RequestBody AuditLogExportRequestDTO exportRequest) {
        LOG.debug("Processing export request: {}", exportRequest);

        try {
            UUID userId = getCurrentUserId();
            if (userId == null) {
                return createUnauthorizedResponse();
            }

            // Validate export request parameters
            var validation = validateExportRequest(exportRequest);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest()
                    .body(ValidationUtils.createErrorResponse(
                        validation.getErrorCode(),
                        validation.getErrorMessage()
                    ));
            }

            // Parse export format
            AuditLogExportRequest.ExportFormat format;
            try {
                format = AuditLogExportRequest.ExportFormat.valueOf(exportRequest.format());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ValidationUtils.createErrorResponse(
                        "INVALID_FORMAT",
                        "Invalid export format: " + exportRequest.format()
                    ));
            }

            // Create filter from export request
            AuditLogFilter filter = createExportFilter(exportRequest);

            // Request export through service
            var exportResponse = auditLogExportService.requestExport(format.name(), userId);

            LOG.info("Created export request: {} for user: {}", exportResponse.getExportId(), userId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(exportResponse);

        } catch (IllegalStateException e) {
            LOG.warn("Export request rejected due to rate limiting: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ValidationUtils.createErrorResponse(
                    "RATE_LIMIT_EXCEEDED",
                    e.getMessage()
                ));
        } catch (SecurityException e) {
            LOG.warn("Security violation in export request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ValidationUtils.createErrorResponse(
                    "ACCESS_DENIED",
                    "Access denied for export operation"
                ));
        } catch (Exception e) {
            LOG.error("Error processing export request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ValidationUtils.createErrorResponse(
                    "INTERNAL_ERROR",
                    "Unable to process export request"
                ));
        }
    }

    /**
     * Get audit log export status.
     *
     * <p>Export lifecycle states:
     * <ul>
     *   <li>PENDING - Export queued for processing</li>
     *   <li>IN_PROGRESS - Export currently being generated</li>
     *   <li>COMPLETED - Export ready for download</li>
     *   <li>FAILED - Export failed with error details</li>
     *   <li>EXPIRED - Export download window expired</li>
     * </ul>
     *
     * @param exportId the export tracking ID
     * @return export status information
     */
    @GetMapping("/{exportId}/status")
    public ResponseEntity<?> getExportStatus(@PathVariable String exportId) {
        LOG.debug("Getting export status for ID: {}", exportId);

        try {
            // Validate export ID format
            var uuidValidation = ValidationUtils.validateUuid(exportId, "exportId");
            if (!uuidValidation.isValid()) {
                return ValidationUtils.createValidationErrorResponse(uuidValidation);
            }

            UUID userId = getCurrentUserId();
            if (userId == null) {
                return createUnauthorizedResponse();
            }

            var status = auditLogExportService.getExportStatus(exportId);

            LOG.debug("Retrieved export status for ID: {} and user: {}", exportId, userId);
            return ResponseEntity.ok(status);

        } catch (AuditLogExportService.EntityNotFoundException e) {
            LOG.warn("Export not found: {}", exportId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ValidationUtils.createErrorResponse(
                    "EXPORT_NOT_FOUND",
                    "Export request not found"
                ));
        } catch (SecurityException e) {
            LOG.warn("Security violation accessing export status {}: {}", exportId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ValidationUtils.createErrorResponse(
                    "ACCESS_DENIED",
                    "Access denied to this export"
                ));
        } catch (Exception e) {
            LOG.error("Error retrieving export status for ID: {}", exportId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ValidationUtils.createErrorResponse(
                    "INTERNAL_ERROR",
                    "Unable to retrieve export status"
                ));
        }
    }

    /**
     * Download completed audit log export file.
     *
     * <p>Security measures:
     * <ul>
     *   <li>Secure download tokens with expiration</li>
     *   <li>Single-use download links</li>
     *   <li>Content-Type verification</li>
     *   <li>Security headers for file downloads</li>
     * </ul>
     *
     * @param token the secure download token
     * @return export file content or error
     */
    @GetMapping("/{token}/download")
    public ResponseEntity<?> downloadExport(@PathVariable String token) {
        LOG.debug("Processing download request for token: {}", token);

        try {
            var download = auditLogExportService.getExportDownload(token);

            if (download == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ValidationUtils.createErrorResponse(
                        "DOWNLOAD_NOT_FOUND",
                        "Download not found or expired"
                    ));
            }

            // Build secure response headers
            HttpHeaders headers = createSecureDownloadHeaders(download);

            LOG.info("Serving download: {} (size: {} bytes)", download.filename(), download.fileSize());

            return ResponseEntity.ok()
                .headers(headers)
                .body(download.resource());

        } catch (AuditLogExportService.DownloadTokenExpiredException e) {
            LOG.warn("Download token expired: {}", token);
            return ResponseEntity.status(HttpStatus.GONE)
                .body(ValidationUtils.createErrorResponse(
                    "DOWNLOAD_TOKEN_EXPIRED",
                    "Download link has expired"
                ));
        } catch (AuditLogExportService.DownloadTokenNotFoundException e) {
            LOG.warn("Download token not found: {}", token);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ValidationUtils.createErrorResponse(
                    "DOWNLOAD_TOKEN_NOT_FOUND",
                    "Download token not found"
                ));
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid download token format: {}", token);
            return ResponseEntity.badRequest()
                .body(ValidationUtils.createErrorResponse(
                    "INVALID_TOKEN_FORMAT",
                    "Download token format is invalid"
                ));
        } catch (Exception e) {
            LOG.error("Error processing download for token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ValidationUtils.createErrorResponse(
                    "DOWNLOAD_ERROR",
                    "Unable to process download"
                ));
        }
    }

    /**
     * Validates export request parameters according to business rules.
     */
    private ValidationUtils.ValidationResult validateExportRequest(AuditLogExportRequestDTO exportRequest) {
        // Validate format
        try {
            AuditLogExportRequest.ExportFormat.valueOf(exportRequest.format());
        } catch (IllegalArgumentException e) {
            return ValidationUtils.ValidationResult.failure(
                "INVALID_FORMAT",
                "Invalid export format: " + exportRequest.format()
            );
        }

        // Validate date range
        if (exportRequest.dateFrom() != null && exportRequest.dateTo() != null
                && exportRequest.dateFrom().isAfter(exportRequest.dateTo())) {
            return ValidationUtils.ValidationResult.failure(
                "INVALID_DATE_RANGE",
                "Start date must be before end date"
            );
        }

        // Validate search text length (prevent DOS attacks)
        if (exportRequest.search() != null && exportRequest.search().length() > 1000) {
            return ValidationUtils.ValidationResult.failure(
                "SEARCH_TOO_LONG",
                "Search text must not exceed 1000 characters"
            );
        }

        return ValidationUtils.ValidationResult.success();
    }

    /**
     * Creates filter for export operations from request parameters.
     */
    private AuditLogFilter createExportFilter(AuditLogExportRequestDTO exportRequest) {
        return new AuditLogFilter(
            null, // organizationId - set by permission service
            null, // userId - set by permission service
            exportRequest.dateFrom(),
            exportRequest.dateTo(),
            exportRequest.search(),
            null, // actionTypes - not supported in current export DTO
            null, // resourceTypes - not supported in current export DTO
            null, // actorEmails - not supported
            null, // includeSystemActions - determined by permissions
            0, // Export doesn't use pagination
            Integer.MAX_VALUE, // Export gets all matching records
            "timestamp",
            "DESC"
        );
    }

    /**
     * Creates secure headers for file downloads.
     */
    private HttpHeaders createSecureDownloadHeaders(Object download) {
        HttpHeaders headers = new HttpHeaders();

        // Use reflection to access download properties (assuming interface)
        try {
            var mimeTypeMethod = download.getClass().getMethod("mimeType");
            var filenameMethod = download.getClass().getMethod("filename");
            var fileSizeMethod = download.getClass().getMethod("fileSize");

            String mimeType = (String) mimeTypeMethod.invoke(download);
            String filename = (String) filenameMethod.invoke(download);
            long fileSize = (Long) fileSizeMethod.invoke(download);

            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setContentDispositionFormData("attachment", filename);

            // Security headers
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Frame-Options", "DENY");
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Pragma", "no-cache");
            headers.set("Expires", "0");

            if (fileSize > 0) {
                headers.setContentLength(fileSize);
            }

            // For large files, use chunked transfer encoding
            if (fileSize > 10240) {
                headers.set("Transfer-Encoding", "chunked");
            }

        } catch (Exception e) {
            LOG.warn("Unable to set download headers", e);
        }

        return headers;
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
}