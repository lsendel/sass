package com.platform.audit.internal;

import com.platform.audit.api.AuditLogExportResponse;
import com.platform.audit.api.AuditLogExportStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling audit log export operations with async processing.
 * Supports CSV, JSON, and PDF export formats with secure download tokens.
 */
@Service
@Transactional
public class AuditLogExportService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogExportService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 32;
    private static final int MAX_ACTIVE_EXPORTS_PER_USER = 3;
    private static final int MAX_EXPORTS_PER_HOUR = 5;

    private final AuditLogExportRepository exportRepository;
    private final AuditLogViewService auditLogViewService;
    private final UserPermissionScopeService permissionScopeService;

    @Value("${app.audit.export.storage-path:./exports}")
    private String exportStoragePath;

    @Value("${app.audit.export.max-records:10000}")
    private int maxExportRecords;

    public AuditLogExportService(AuditLogExportRepository exportRepository,
                               AuditLogViewService auditLogViewService,
                               UserPermissionScopeService permissionScopeService) {
        this.exportRepository = exportRepository;
        this.auditLogViewService = auditLogViewService;
        this.permissionScopeService = permissionScopeService;
    }

    /**
     * Request an audit log export with rate limiting and validation.
     */
    public AuditLogExportResponse requestExport(UUID userId,
                                              AuditLogExportRequest.ExportFormat format,
                                              AuditLogFilter filter) {
        log.debug("Processing export request for user: {} with format: {}", userId, format);

        // Validate rate limits
        validateExportLimits(userId);

        // Apply permission-based filtering
        AuditLogFilter scopedFilter = permissionScopeService.createUserScopedFilter(userId, filter);

        // Create export request
        AuditLogExportRequest exportRequest = new AuditLogExportRequest(
            userId,
            scopedFilter.organizationId(),
            format
        );

        // Set filter criteria
        exportRequest.setDateFrom(scopedFilter.dateFrom());
        exportRequest.setDateTo(scopedFilter.dateTo());
        exportRequest.setSearchTerm(scopedFilter.search());
        if (scopedFilter.actionTypes() != null) {
            exportRequest.setActionTypes(String.join(",", scopedFilter.actionTypes()));
        }

        // Save the request
        exportRequest = exportRepository.save(exportRequest);

        // Start async processing
        processExportAsync(exportRequest.getId(), scopedFilter);

        log.info("Created export request: {} for user: {}", exportRequest.getId(), userId);

        return AuditLogExportResponse.pending(exportRequest.getId().toString());
    }

    /**
     * Get the status of an export request.
     */
    @Transactional(readOnly = true)
    public Optional<AuditLogExportStatus> getExportStatus(UUID userId, UUID exportId) {
        log.debug("Getting export status for user: {} and export: {}", userId, exportId);

        Optional<AuditLogExportRequest> exportOpt = exportRepository.findById(exportId);
        if (exportOpt.isEmpty()) {
            return Optional.empty();
        }

        AuditLogExportRequest export = exportOpt.get();

        // Verify user owns this export
        if (!export.getUserId().equals(userId)) {
            log.warn("User {} attempted to access export {} owned by {}",
                userId, exportId, export.getUserId());
            return Optional.empty();
        }

        return Optional.of(createExportStatus(export));
    }

    /**
     * Get a downloadable resource for an export.
     */
    @Transactional
    public Optional<ExportDownload> getExportDownload(String downloadToken) {
        log.debug("Processing download request for token: {}", downloadToken);

        Optional<AuditLogExportRequest> exportOpt = exportRepository.findByDownloadToken(downloadToken);
        if (exportOpt.isEmpty()) {
            log.warn("Download token not found: {}", downloadToken);
            return Optional.empty();
        }

        AuditLogExportRequest export = exportOpt.get();

        // Check if download is still valid
        if (!export.canDownload()) {
            log.warn("Download not available for export: {} (status: {}, expired: {})",
                export.getId(), export.getStatus(), export.isDownloadExpired());
            return Optional.empty();
        }

        try {
            Path filePath = Paths.get(export.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error("Export file not found or not readable: {}", export.getFilePath());
                return Optional.empty();
            }

            // Increment download count
            export.incrementDownloadCount();
            exportRepository.save(export);

            log.info("Serving download for export: {} (download #{}/{})",
                export.getId(), export.getDownloadCount(), export.getMaxDownloads());

            return Optional.of(new ExportDownload(
                resource,
                export.getFormat().getMimeType(),
                generateDownloadFilename(export),
                export.getFileSizeBytes()
            ));

        } catch (MalformedURLException e) {
            log.error("Invalid file path for export: {}", export.getFilePath(), e);
            return Optional.empty();
        }
    }

    /**
     * Get user's export history.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogExportStatus> getUserExportHistory(UUID userId, Pageable pageable) {
        Page<AuditLogExportRequest> exports = exportRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return exports.map(this::createExportStatus);
    }

    /**
     * Process export asynchronously.
     */
    @Async("auditExportExecutor")
    public CompletableFuture<Void> processExportAsync(UUID exportId, AuditLogFilter filter) {
        return CompletableFuture.runAsync(() -> {
            try {
                processExport(exportId, filter);
            } catch (Exception e) {
                log.error("Failed to process export: {}", exportId, e);
                markExportAsFailed(exportId, "Export processing failed: " + e.getMessage());
            }
        });
    }

    /**
     * Process the export synchronously.
     */
    private void processExport(UUID exportId, AuditLogFilter filter) {
        log.debug("Starting export processing for: {}", exportId);

        Optional<AuditLogExportRequest> exportOpt = exportRepository.findById(exportId);
        if (exportOpt.isEmpty()) {
            log.error("Export request not found: {}", exportId);
            return;
        }

        AuditLogExportRequest export = exportOpt.get();

        try {
            // Mark as started
            export.markAsStarted();
            exportRepository.save(export);

            // Count total records first
            long totalRecords = countExportRecords(filter);
            export.setTotalRecords(totalRecords);

            if (totalRecords > maxExportRecords) {
                throw new IllegalArgumentException(
                    "Export would contain " + totalRecords + " records, which exceeds the maximum of " + maxExportRecords);
            }

            // Generate the export file
            String filePath = generateExportFile(export, filter);

            // Calculate file size
            long fileSize = Files.size(Paths.get(filePath));

            // Generate secure download token
            String downloadToken = generateSecureToken();

            // Mark as completed
            export.markAsCompleted(filePath, fileSize, downloadToken);
            exportRepository.save(export);

            log.info("Completed export processing for: {} (file: {}, size: {} bytes)",
                exportId, filePath, fileSize);

        } catch (Exception e) {
            log.error("Export processing failed for: {}", exportId, e);
            export.markAsFailed("Export processing failed: " + e.getMessage());
            exportRepository.save(export);
        }
    }

    /**
     * Generate the export file based on format.
     */
    private String generateExportFile(AuditLogExportRequest export, AuditLogFilter filter) throws IOException {
        // Ensure export directory exists
        Path exportDir = Paths.get(exportStoragePath);
        Files.createDirectories(exportDir);

        // Generate filename
        String filename = String.format("audit-export-%s-%s%s",
            export.getId(),
            DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replaceAll("[:.]+", "-"),
            export.getFormat().getFileExtension()
        );

        Path filePath = exportDir.resolve(filename);

        switch (export.getFormat()) {
            case CSV -> generateCSVExport(export, filter, filePath);
            case JSON -> generateJSONExport(export, filter, filePath);
            case PDF -> generatePDFExport(export, filter, filePath);
            default -> throw new IllegalArgumentException("Unsupported export format: " + export.getFormat());
        }

        return filePath.toString();
    }

    /**
     * Generate CSV export file.
     */
    private void generateCSVExport(AuditLogExportRequest export, AuditLogFilter filter, Path filePath)
            throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            // Write CSV header
            writer.write("Timestamp,Actor,Action,Resource,Description,Outcome,IP Address,Session ID\n");

            // Process in batches
            int batchSize = 1000;
            int pageNumber = 0;
            long processedRecords = 0;

            while (true) {
                AuditLogFilter batchFilter = new AuditLogFilter(
                    filter.organizationId(),
                    filter.userId(),
                    filter.dateFrom(),
                    filter.dateTo(),
                    filter.search(),
                    filter.actionTypes(),
                    filter.actorEmails(),
                    filter.includeSystemActions(),
                    pageNumber,
                    batchSize
                );

                var searchResponse = auditLogViewService.getAuditLogs(export.getUserId(), batchFilter);

                if (searchResponse.entries().isEmpty()) {
                    break;
                }

                for (var entry : searchResponse.entries()) {
                    writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        entry.timestamp(),
                        escapeCSV(entry.actorName()),
                        escapeCSV(entry.actionType()),
                        escapeCSV(entry.resourceType()),
                        escapeCSV(entry.description()),
                        entry.outcome(),
                        escapeCSV(entry.ipAddress()),
                        entry.sessionId()
                    ));
                    processedRecords++;
                }

                // Update progress
                export.setProcessedRecords(processedRecords);
                exportRepository.save(export);

                pageNumber++;

                if (searchResponse.isLast()) {
                    break;
                }
            }
        }
    }

    /**
     * Generate JSON export file.
     */
    private void generateJSONExport(AuditLogExportRequest export, AuditLogFilter filter, Path filePath)
            throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("{\n");
            writer.write("  \"exportInfo\": {\n");
            writer.write("    \"exportId\": \"" + export.getId() + "\",\n");
            writer.write("    \"exportedAt\": \"" + Instant.now() + "\",\n");
            writer.write("    \"format\": \"JSON\"\n");
            writer.write("  },\n");
            writer.write("  \"auditLogs\": [\n");

            // Process in batches
            int batchSize = 1000;
            int pageNumber = 0;
            long processedRecords = 0;
            boolean first = true;

            while (true) {
                AuditLogFilter batchFilter = new AuditLogFilter(
                    filter.organizationId(),
                    filter.userId(),
                    filter.dateFrom(),
                    filter.dateTo(),
                    filter.search(),
                    filter.actionTypes(),
                    filter.actorEmails(),
                    filter.includeSystemActions(),
                    pageNumber,
                    batchSize
                );

                var searchResponse = auditLogViewService.getAuditLogs(export.getUserId(), batchFilter);

                if (searchResponse.entries().isEmpty()) {
                    break;
                }

                for (var entry : searchResponse.entries()) {
                    if (!first) {
                        writer.write(",\n");
                    }
                    writer.write("    {\n");
                    writer.write("      \"id\": \"" + entry.id() + "\",\n");
                    writer.write("      \"timestamp\": \"" + entry.timestamp() + "\",\n");
                    writer.write("      \"actorName\": \"" + escapeJSON(entry.actorName()) + "\",\n");
                    writer.write("      \"actionType\": \"" + escapeJSON(entry.actionType()) + "\",\n");
                    writer.write("      \"resourceType\": \"" + escapeJSON(entry.resourceType()) + "\",\n");
                    writer.write("      \"description\": \"" + escapeJSON(entry.description()) + "\",\n");
                    writer.write("      \"outcome\": \"" + entry.outcome() + "\",\n");
                    writer.write("      \"ipAddress\": \"" + escapeJSON(entry.ipAddress()) + "\",\n");
                    writer.write("      \"sessionId\": \"" + entry.sessionId() + "\"\n");
                    writer.write("    }");
                    first = false;
                    processedRecords++;
                }

                export.setProcessedRecords(processedRecords);
                exportRepository.save(export);

                pageNumber++;

                if (searchResponse.isLast()) {
                    break;
                }
            }

            writer.write("\n  ]\n");
            writer.write("}\n");
        }
    }

    /**
     * Generate PDF export file (placeholder implementation).
     */
    private void generatePDFExport(AuditLogExportRequest export, AuditLogFilter filter, Path filePath)
            throws IOException {
        // For now, create a simple text file as PDF generation requires additional dependencies
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("AUDIT LOG EXPORT REPORT\n");
            writer.write("======================\n\n");
            writer.write("Export ID: " + export.getId() + "\n");
            writer.write("Generated: " + Instant.now() + "\n");
            writer.write("Format: PDF (placeholder)\n\n");
            writer.write("NOTE: Full PDF generation requires additional dependencies.\n");
            writer.write("This is a placeholder implementation.\n");
        }
    }

    // Helper methods

    private void validateExportLimits(UUID userId) {
        // Check active exports limit
        long activeExports = exportRepository.countActiveExportsForUser(userId);
        if (activeExports >= MAX_ACTIVE_EXPORTS_PER_USER) {
            throw new IllegalStateException("Maximum number of active exports reached (" + MAX_ACTIVE_EXPORTS_PER_USER + ")");
        }

        // Check hourly rate limit
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        long recentExports = exportRepository.countExportsForUserSince(userId, oneHourAgo);
        if (recentExports >= MAX_EXPORTS_PER_HOUR) {
            throw new IllegalStateException("Export rate limit exceeded (" + MAX_EXPORTS_PER_HOUR + " per hour)");
        }
    }

    private long countExportRecords(AuditLogFilter filter) {
        // Use a modified filter with large page size to count efficiently
        AuditLogFilter countFilter = new AuditLogFilter(
            filter.organizationId(),
            filter.userId(),
            filter.dateFrom(),
            filter.dateTo(),
            filter.search(),
            filter.actionTypes(),
            filter.actorEmails(),
            filter.includeSystemActions(),
            0,
            1
        );

        var response = auditLogViewService.getAuditLogs(filter.organizationId(), countFilter);
        return response.totalElements();
    }

    private void markExportAsFailed(UUID exportId, String errorMessage) {
        exportRepository.findById(exportId).ifPresent(export -> {
            export.markAsFailed(errorMessage);
            exportRepository.save(export);
        });
    }

    private AuditLogExportStatus createExportStatus(AuditLogExportRequest export) {
        return new AuditLogExportStatus(
            export.getId().toString(),
            export.getStatus().name(),
            export.getFormat().name(),
            export.getProgressPercentage(),
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
    }

    private String generateSecureToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return token.toString();
    }

    private String generateDownloadFilename(AuditLogExportRequest export) {
        return String.format("audit-logs-%s%s",
            DateTimeFormatter.ISO_LOCAL_DATE.format(export.getCreatedAt()),
            export.getFormat().getFileExtension()
        );
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private String escapeJSON(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"")
                   .replace("\\", "\\\\")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Record for export download information.
     */
    public record ExportDownload(
        Resource resource,
        String mimeType,
        String filename,
        Long fileSize
    ) {}
}