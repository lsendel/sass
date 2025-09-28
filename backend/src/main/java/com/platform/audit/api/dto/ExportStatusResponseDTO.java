package com.platform.audit.api.dto;

import java.time.Instant;

/**
 * DTO for audit log export status response.
 */
public record ExportStatusResponseDTO(
    String exportId,
    String status,
    int progress,
    Instant requestedAt,
    Instant completedAt,
    String downloadUrl,
    long totalRecords,
    String errorMessage
) {
    // Export statuses
    public static class Status {
        public static final String PENDING = "PENDING";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String COMPLETED = "COMPLETED";
        public static final String FAILED = "FAILED";
    }
}