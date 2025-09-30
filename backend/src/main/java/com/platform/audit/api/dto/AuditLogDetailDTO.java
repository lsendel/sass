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
    String actionType,
    String description,
    Map<String, Object> details
) {
    public static AuditLogDetailDTO fromAuditEvent(com.platform.audit.internal.AuditEvent event, AuditLogEntryDTO entry) {
        return new AuditLogDetailDTO(
            event.getId().toString(),
            event.getCreatedAt(),
            entry.actorName(),
            event.getAction(),
            "Action: " + event.getAction() + " on " + event.getResourceType(), // Generate description from action
            event.getDetails() // Use actual event details
        );
    }
}
