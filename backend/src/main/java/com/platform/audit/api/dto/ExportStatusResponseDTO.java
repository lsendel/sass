package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * DTO for audit log export status response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportStatusResponseDTO(
    String exportId,
    String status,
    int progress,
    Instant requestedAt,
    Instant completedAt,
    String downloadUrl,
    String downloadToken,  // Token for download endpoint
    Long entryCount,       // Number of entries in export
    Long fileSize,         // Size of export file in bytes
    Instant expiresAt,     // When download link expires
    String errorMessage
) {
    // Constructor for backward compatibility with existing code
    public ExportStatusResponseDTO(
        String exportId,
        String status,
        int progress,
        Instant requestedAt,
        Instant completedAt,
        String downloadUrl,
        long totalRecords,
        String errorMessage
    ) {
        this(exportId, status, progress, requestedAt, completedAt, downloadUrl,
             extractToken(downloadUrl), totalRecords > 0 ? totalRecords : null,
             null, null, errorMessage);
    }

    private static String extractToken(String downloadUrl) {
        if (downloadUrl != null && downloadUrl.contains("/download")) {
            String[] parts = downloadUrl.split("/");
            if (parts.length >= 2) {
                return parts[parts.length - 2];
            }
        }
        return null;
    }

    // Export statuses
    public static class Status {
        public static final String PENDING = "PENDING";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String PROCESSING = "PROCESSING";
        public static final String COMPLETED = "COMPLETED";
        public static final String FAILED = "FAILED";
    }
}