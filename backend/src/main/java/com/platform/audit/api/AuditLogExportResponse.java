package com.platform.audit.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Response object for audit log export requests.
 * Returned when an export is successfully initiated.
 */
public record AuditLogExportResponse(
    @NotNull
    String exportId,

    @NotNull
    String status,

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    Instant requestedAt,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    Instant estimatedCompletionTime
) {
    /**
     * Creates a pending export response.
     */
    public static AuditLogExportResponse pending(String exportId) {
        Instant now = Instant.now();
        Instant estimated = now.plusSeconds(300); // Estimate 5 minutes

        return new AuditLogExportResponse(
            exportId,
            "PENDING",
            now,
            estimated
        );
    }
}