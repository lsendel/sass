package com.platform.audit.api.dto;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for detailed audit log entry information.
 */
public record AuditLogDetailDTO(
    String id,
    Instant timestamp,
    String actorName,
    String actorId,
    String actorType,
    String actionType,
    String actionDescription,
    String resourceType,
    String resourceName,
    String resourceId,
    String outcome,
    String sensitivity,
    String ipAddress,
    String userAgent,
    String correlationId,
    Map<String, Object> additionalData
) {
    public static AuditLogDetailDTO fromBasicEntry(
        AuditLogEntryDTO basicEntry,
        String actorId,
        String resourceId,
        String ipAddress,
        String userAgent,
        Map<String, Object> additionalData
    ) {
        return new AuditLogDetailDTO(
            basicEntry.id(),
            basicEntry.timestamp(),
            basicEntry.actorName(),
            actorId,
            basicEntry.actorType(),
            basicEntry.actionType(),
            basicEntry.actionDescription(),
            basicEntry.resourceType(),
            basicEntry.resourceName(),
            resourceId,
            basicEntry.outcome(),
            basicEntry.sensitivity(),
            ipAddress,
            userAgent,
            null, // correlationId
            additionalData
        );
    }
}