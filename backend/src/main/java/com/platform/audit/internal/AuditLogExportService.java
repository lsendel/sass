package com.platform.audit.internal;

import com.platform.audit.api.dto.ExportResponseDTO;
import com.platform.audit.api.dto.ExportStatusResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for audit log export operations.
 *
 * This service handles the complete export lifecycle including request processing,
 * status tracking, file generation, and secure download token management.
 */
@Service
@Transactional
public class AuditLogExportService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogExportService.class);

    private final AuditLogExportRepository exportRepository;
    private final AuditLogPermissionService permissionService;
    private final AuditLogViewService viewService;

    public AuditLogExportService(
            AuditLogExportRepository exportRepository,
            AuditLogPermissionService permissionService,
            AuditLogViewService viewService) {
        this.exportRepository = exportRepository;
        this.permissionService = permissionService;
        this.viewService = viewService;
    }

    /**
     * Request a new audit log export.
     *
     * @param userId the user requesting the export
     * @param format the export format
     * @param filter the filter criteria for export
     * @return export response with tracking information
     */
    public ExportResponseDTO requestExport(
            UUID userId,
            AuditLogExportRequest.ExportFormat format,
            AuditLogFilter filter) {

        log.debug("Processing export request for user: {} with format: {}", userId, format);

        // Check permissions
        if (!permissionService.canExportAuditLogs(userId)) {
            throw new SecurityException("User does not have permission to export audit logs");
        }

        // Check rate limiting - prevent too many concurrent exports
        long activeExports = exportRepository.countActiveExportsForUser(userId);
        if (activeExports >= 3) { // Allow max 3 concurrent exports per user
            throw new IllegalStateException("Too many active exports. Please wait for current exports to complete.");
        }

        // Check recent export frequency (prevent spam)
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        long recentExports = exportRepository.countExportsForUserSince(userId, oneHourAgo);
        if (recentExports >= 10) { // Max 10 exports per hour
            throw new IllegalStateException("Export rate limit exceeded. Please wait before requesting another export.");
        }

        // Get user's organization for tenant isolation
        var permissions = permissionService.getUserAuditPermissions(userId);
        UUID organizationId = permissions.organizationId();

        // Create export request entity
        AuditLogExportRequest exportRequest = new AuditLogExportRequest(userId, organizationId, format);
        exportRequest.setDateFrom(filter.dateFrom());
        exportRequest.setDateTo(filter.dateTo());
        exportRequest.setSearchTerm(filter.searchText());
        if (filter.actionTypes() != null) {
            exportRequest.setActionTypes(String.join(",", filter.actionTypes()));
        }

        // Save the request
        exportRequest = exportRepository.save(exportRequest);

        log.info("Created export request: {} for user: {}", exportRequest.getId(), userId);

        // Queue for async processing (in a real implementation, this would trigger a background job)
        // For TDD approach, we'll simulate the queuing
        scheduleExportProcessing(exportRequest);

        ExportResponseDTO response = new ExportResponseDTO(
            exportRequest.getId().toString(),
            exportRequest.getStatus().name()
        );
        response.setRequestedAt(exportRequest.getCreatedAt());
        response.setMessage(estimateCompletionTime(exportRequest));
        return response;
    }

    /**
     * Get the status of an export request.
     *
     * @param userId the user requesting the status
     * @param exportId the export request ID
     * @return export status if accessible
     */
    @Transactional(readOnly = true)
    public Optional<ExportStatusResponse> getExportStatus(UUID userId, UUID exportId) {
        log.debug("Getting export status for ID: {} and user: {}", exportId, userId);

        // Find the export request
        Optional<AuditLogExportRequest> exportOpt = exportRepository.findById(exportId);
        if (exportOpt.isEmpty()) {
            return Optional.empty();
        }

        AuditLogExportRequest export = exportOpt.get();

        // Verify user can access this export (owner or admin)
        if (!export.getUserId().equals(userId) && !canAccessOtherUsersExports(userId)) {
            throw new SecurityException("Access denied to this export request");
        }

        // Convert to response
        ExportStatusResponse response = new ExportStatusResponse(
            export.getId(),
            export.getStatus(),
            export.getFormat(),
            calculateProgressPercentage(export),
            export.getTotalRecords(),
            export.getProcessedRecords(),
            export.getCreatedAt(),
            export.getStartedAt(),
            export.getCompletedAt(),
            export.getDownloadToken(),
            export.getDownloadExpiresAt(),
            export.getDownloadCount(),
            export.getMaxDownloads(),
            export.getErrorMessage()
        );

        return Optional.of(response);
    }

    /**
     * Get download information for a completed export.
     *
     * @param downloadToken the secure download token
     * @return download information if valid
     */
    @Transactional(readOnly = true)
    public Optional<ExportDownload> getExportDownload(String downloadToken) {
        log.debug("Processing download request for token: {}", downloadToken);

        // Find export by download token
        Optional<AuditLogExportRequest> exportOpt = exportRepository.findByDownloadToken(downloadToken);
        if (exportOpt.isEmpty()) {
            log.warn("Export not found for download token");
            return Optional.empty();
        }

        AuditLogExportRequest export = exportOpt.get();

        // Check if download is still valid
        if (export.getStatus() != AuditLogExportRequest.ExportStatus.COMPLETED) {
            log.warn("Export not completed for download token");
            return Optional.empty();
        }

        if (export.getDownloadExpiresAt() != null &&
            export.getDownloadExpiresAt().isBefore(Instant.now())) {
            log.warn("Download token expired");
            return Optional.empty();
        }

        if (export.getMaxDownloads() != null &&
            export.getDownloadCount() >= export.getMaxDownloads()) {
            log.warn("Maximum download count exceeded");
            return Optional.empty();
        }

        // Increment download count
        export.incrementDownloadCount();
        exportRepository.save(export);

        // Generate download response
        String filename = generateFilename(export);
        String mimeType = getMimeType(export.getFormat());

        // TODO: In real implementation, load actual file from storage
        // For TDD, return placeholder
        Resource resource = createPlaceholderResource(export);

        return Optional.of(new ExportDownload(
            filename,
            mimeType,
            export.getFileSize(),
            resource
        ));
    }

    /**
     * Clean up expired exports and downloads.
     * This would typically be called by a scheduled job.
     */
    @Transactional
    public void cleanupExpiredExports() {
        log.debug("Cleaning up expired exports");

        Instant now = Instant.now();

        // Mark expired downloads
        int expiredDownloads = exportRepository.markExpiredDownloads(now);
        log.info("Marked {} downloads as expired", expiredDownloads);

        // Delete old export records (keep for 30 days after completion)
        Instant cutoffTime = now.minusSeconds(30 * 24 * 60 * 60);
        int deletedExports = exportRepository.deleteOldExports(cutoffTime);
        log.info("Deleted {} old export records", deletedExports);
    }

    // Helper methods

    private void scheduleExportProcessing(AuditLogExportRequest exportRequest) {
        // TODO: In real implementation, this would queue the export for background processing
        // For TDD approach, we'll just log that it would be scheduled
        log.debug("Export {} queued for processing", exportRequest.getId());

        // Simulate immediate processing start for testing
        exportRequest.markAsStarted();
        exportRepository.save(exportRequest);
    }

    private String estimateCompletionTime(AuditLogExportRequest exportRequest) {
        // Estimate based on typical processing time
        // Small exports: 1-2 minutes, large exports: 5-10 minutes
        int estimatedMinutes = estimateProcessingTime(exportRequest);
        return Instant.now().plusSeconds(estimatedMinutes * 60).toString();
    }

    private int estimateProcessingTime(AuditLogExportRequest exportRequest) {
        // Simple estimation based on date range
        if (exportRequest.getDateFrom() != null && exportRequest.getDateTo() != null) {
            long daysDiff = (exportRequest.getDateTo().getEpochSecond() -
                           exportRequest.getDateFrom().getEpochSecond()) / (24 * 60 * 60);
            if (daysDiff > 30) return 10; // Large range
            if (daysDiff > 7) return 5;   // Medium range
        }
        return 2; // Small range or no date filter
    }

    private int calculateProgressPercentage(AuditLogExportRequest export) {
        if (export.getTotalRecords() == null || export.getTotalRecords() == 0) {
            return switch (export.getStatus()) {
                case PENDING -> 0;
                case PROCESSING -> 50;
                case COMPLETED -> 100;
                case FAILED, EXPIRED -> 0;
            };
        }

        if (export.getProcessedRecords() == null) {
            return 0;
        }

        return (int) ((export.getProcessedRecords() * 100L) / export.getTotalRecords());
    }

    private boolean canAccessOtherUsersExports(UUID userId) {
        // Check if user has admin permissions
        var permissions = permissionService.getUserAuditPermissions(userId);
        return permissions.canViewSystemActions(); // Admins can view all exports
    }

    private String generateFilename(AuditLogExportRequest export) {
        String timestamp = export.getCreatedAt().toString().substring(0, 19).replace(":", "-");
        String extension = export.getFormat().name().toLowerCase();
        return String.format("audit-logs-%s.%s", timestamp, extension);
    }

    private String getMimeType(AuditLogExportRequest.ExportFormat format) {
        return switch (format) {
            case CSV -> "text/csv";
            case JSON -> "application/json";
            case PDF -> "application/pdf";
        };
    }

    private Resource createPlaceholderResource(AuditLogExportRequest export) {
        // TODO: In real implementation, load file from storage
        // For TDD, return minimal placeholder
        return new org.springframework.core.io.ByteArrayResource(
            "Placeholder export content".getBytes()
        );
    }

    // Record classes for responses

    public record ExportStatusResponse(
        UUID exportId,
        AuditLogExportRequest.ExportStatus status,
        AuditLogExportRequest.ExportFormat format,
        int progressPercentage,
        Long totalRecords,
        Long processedRecords,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        String downloadToken,
        Instant downloadExpiresAt,
        Integer downloadCount,
        Integer maxDownloads,
        String errorMessage
    ) {}

    public record ExportDownload(
        String filename,
        String mimeType,
        Long fileSize,
        Resource resource
    ) {}
}