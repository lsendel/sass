package com.platform.audit.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Data Transfer Object for audit log entries in the user-facing audit log viewer.
 * This DTO represents the public view of audit events with appropriate data filtering
 * and redaction based on user permissions.
 */
public record AuditLogEntryDTO(
    @NotNull
    String id,

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
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

    @NotNull
    Boolean hasDetails
) {
    /**
     * Creates an AuditLogEntryDTO with redacted sensitive information.
     * Used when user doesn't have permission to view full details.
     */
    public static AuditLogEntryDTO createRedacted(
        String id,
        Instant timestamp,
        String actionType,
        String resourceType,
        String description,
        String outcome,
        String severity
    ) {
        return new AuditLogEntryDTO(
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
            false // hasDetails - no access to details
        );
    }

    /**
     * Creates a full AuditLogEntryDTO for users with appropriate permissions.
     */
    public static AuditLogEntryDTO createFull(
        String id,
        Instant timestamp,
        String actorName,
        String actorEmail,
        String actionType,
        String resourceType,
        String resourceName,
        String description,
        String outcome,
        String severity,
        Boolean hasDetails
    ) {
        return new AuditLogEntryDTO(
            id,
            timestamp,
            actorName,
            actorEmail,
            actionType,
            resourceType,
            resourceName,
            description,
            outcome,
            severity,
            hasDetails
        );
    }
}