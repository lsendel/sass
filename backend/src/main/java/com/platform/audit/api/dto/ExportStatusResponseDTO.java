package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Data Transfer Object for export status responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportStatusResponseDTO(
    String exportId,
    String status,
    String format,
    Double progressPercentage,
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
) {

    /**
     * Create status response for pending export.
     */
    public static ExportStatusResponseDTO pending(String exportId, String format, Instant createdAt) {
        return new ExportStatusResponseDTO(
            exportId,
            "PENDING",
            format,
            0.0,
            null,
            null,
            createdAt,
            null,
            null,
            null,
            null,
            0,
            5,
            null
        );
    }

    /**
     * Create status response for processing export.
     */
    public static ExportStatusResponseDTO processing(String exportId, String format,
                                                   Double progress, Long totalRecords,
                                                   Long processedRecords, Instant createdAt,
                                                   Instant startedAt) {
        return new ExportStatusResponseDTO(
            exportId,
            "PROCESSING",
            format,
            progress,
            totalRecords,
            processedRecords,
            createdAt,
            startedAt,
            null,
            null,
            null,
            0,
            5,
            null
        );
    }

    /**
     * Create status response for completed export.
     */
    public static ExportStatusResponseDTO completed(String exportId, String format,
                                                  Long totalRecords, Instant createdAt,
                                                  Instant startedAt, Instant completedAt,
                                                  String downloadToken, Instant downloadExpiresAt,
                                                  Integer downloadCount, Integer maxDownloads) {
        return new ExportStatusResponseDTO(
            exportId,
            "COMPLETED",
            format,
            100.0,
            totalRecords,
            totalRecords,
            createdAt,
            startedAt,
            completedAt,
            downloadToken,
            downloadExpiresAt,
            downloadCount,
            maxDownloads,
            null
        );
    }

    /**
     * Create status response for failed export.
     */
    public static ExportStatusResponseDTO failed(String exportId, String format,
                                               Instant createdAt, Instant startedAt,
                                               String errorMessage) {
        return new ExportStatusResponseDTO(
            exportId,
            "FAILED",
            format,
            null,
            null,
            null,
            createdAt,
            startedAt,
            Instant.now(),
            null,
            null,
            0,
            5,
            errorMessage
        );
    }
}