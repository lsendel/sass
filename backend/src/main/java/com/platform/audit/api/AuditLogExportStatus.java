package com.platform.audit.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Status response for audit log export requests.
 * Provides progress and completion information for exports.
 */
public record AuditLogExportStatus(
    @NotNull
    String exportId,

    @NotNull
    String status,

    @NotNull
    @Min(0)
    @Max(100)
    Integer progress,

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    Instant requestedAt,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    Instant completedAt,

    String downloadToken,

    Integer entryCount,

    Long fileSize,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    Instant expiresAt,

    String errorMessage
) {
    /**
     * Creates a pending status response.
     */
    public static AuditLogExportStatus pending(String exportId, Instant requestedAt) {
        return new AuditLogExportStatus(
            exportId,
            "PENDING",
            0,
            requestedAt,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Creates a processing status response.
     */
    public static AuditLogExportStatus processing(String exportId, Instant requestedAt, Integer progress) {
        return new AuditLogExportStatus(
            exportId,
            "PROCESSING",
            progress,
            requestedAt,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Creates a completed status response.
     */
    public static AuditLogExportStatus completed(
        String exportId,
        Instant requestedAt,
        Instant completedAt,
        String downloadToken,
        Integer entryCount,
        Long fileSize,
        Instant expiresAt
    ) {
        return new AuditLogExportStatus(
            exportId,
            "COMPLETED",
            100,
            requestedAt,
            completedAt,
            downloadToken,
            entryCount,
            fileSize,
            expiresAt,
            null
        );
    }

    /**
     * Creates a failed status response.
     */
    public static AuditLogExportStatus failed(
        String exportId,
        Instant requestedAt,
        String errorMessage
    ) {
        return new AuditLogExportStatus(
            exportId,
            "FAILED",
            0,
            requestedAt,
            null,
            null,
            null,
            null,
            null,
            errorMessage
        );
    }
}