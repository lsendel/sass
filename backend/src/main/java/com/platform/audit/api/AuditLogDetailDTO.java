package com.platform.audit.api;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Detailed Data Transfer Object for individual audit log entries.
 * Extends the basic audit log entry with additional context and metadata
 * that's only available in the detailed view.
 */
public record AuditLogDetailDTO(
    @NotNull
    String id,

    @NotNull
    Instant timestamp,

    String actorName,

    String actorEmail,

    @NotNull
    String actionType,

    @NotNull
    String resourceType,

    String resourceName,

    @NotNull
    String description,

    @NotNull
    String outcome,

    @NotNull
    String severity,

    String correlationId,

    String ipAddress,

    String userAgent,

    Map<String, Object> metadata,

    String errorDetails,

    List<AuditLogEntryDTO> relatedEntries
) {
    /**
     * Creates a detailed audit log DTO from a basic audit log entry.
     */
    public static AuditLogDetailDTO fromBasicEntry(
        AuditLogEntryDTO basicEntry,
        String correlationId,
        String ipAddress,
        String userAgent,
        Map<String, Object> metadata,
        String errorDetails,
        List<AuditLogEntryDTO> relatedEntries
    ) {
        return new AuditLogDetailDTO(
            basicEntry.id(),
            basicEntry.timestamp(),
            basicEntry.actorName(),
            basicEntry.actorEmail(),
            basicEntry.actionType(),
            basicEntry.resourceType(),
            basicEntry.resourceName(),
            basicEntry.description(),
            basicEntry.outcome(),
            basicEntry.severity(),
            correlationId,
            ipAddress,
            userAgent,
            metadata,
            errorDetails,
            relatedEntries
        );
    }

    /**
     * Creates a redacted detailed view with sensitive information removed.
     */
    public static AuditLogDetailDTO createRedacted(
        String id,
        Instant timestamp,
        String actionType,
        String resourceType,
        String description,
        String outcome,
        String severity,
        String correlationId
    ) {
        return new AuditLogDetailDTO(
            id,
            timestamp,
            "[REDACTED]", // actorName
            "[REDACTED]", // actorEmail
            actionType,
            resourceType,
            "[REDACTED]", // resourceName
            description,
            outcome,
            severity,
            correlationId,
            "[REDACTED]", // ipAddress
            "[REDACTED]", // userAgent
            Map.of(), // empty metadata
            null, // errorDetails
            List.of() // no related entries
        );
    }
}