package com.platform.audit.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO for detailed audit log entry information matching OpenAPI spec.
 * This extends the basic AuditLogEntryDTO with additional detail fields.
 */
public record AuditLogDetailDTO(
    // Base fields from AuditLogEntryDTO
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
    boolean hasDetails,

    // Additional detail fields
    String correlationId,
    String ipAddress,
    String userAgent,
    Map<String, Object> metadata,
    String errorDetails,
    List<AuditLogEntryDTO> relatedEntries
) {
    /**
     * Create from AuditEvent with full details.
     */
    public static AuditLogDetailDTO fromAuditEvent(
            com.platform.audit.internal.AuditEvent event,
            String actorName,
            String actorEmail) {
        return new AuditLogDetailDTO(
            event.getId().toString(),
            event.getCreatedAt(),
            actorName,
            actorEmail,
            event.getAction(),
            event.getResourceType() != null ? event.getResourceType() : "UNKNOWN",
            event.getResourceId() != null ? event.getResourceId() : "N/A",
            event.getDescription() != null ? event.getDescription() : generateDescription(event),
            "SUCCESS", // Default outcome
            event.getSeverity() != null ? event.getSeverity() : "LOW",
            true, // Has details

            // Additional detail fields
            event.getCorrelationId(),
            event.getIpAddress() != null ? hashIpAddress(event.getIpAddress()) : null,
            event.getUserAgent() != null ? truncateUserAgent(event.getUserAgent()) : null,
            event.getDetails(),
            null, // No error for successful actions
            List.of() // Empty related entries for now
        );
    }

    /**
     * Generate human-readable description from event.
     */
    private static String generateDescription(com.platform.audit.internal.AuditEvent event) {
        String action = event.getAction();
        String resourceType = event.getResourceType();

        if (action == null) {
            return "Unknown action";
        }

        return switch (action.toLowerCase()) {
            case "user.login" -> "User logged in";
            case "user.logout" -> "User logged out";
            case "data.created" -> "Created " + (resourceType != null ? resourceType : "resource");
            case "data.updated" -> "Updated " + (resourceType != null ? resourceType : "resource");
            case "data.deleted" -> "Deleted " + (resourceType != null ? resourceType : "resource");
            case "data.exported" -> "Exported " + (resourceType != null ? resourceType : "data");
            case "payment.processed" -> "Processed payment";
            case "subscription.created" -> "Created subscription";
            case "subscription.updated" -> "Updated subscription";
            default -> action.replace(".", " ").replace("_", " ");
        };
    }

    /**
     * Hash IP address for privacy.
     */
    private static String hashIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }
        // Simple hash representation - in production would use proper hashing
        return "hashed_" + ipAddress.hashCode();
    }

    /**
     * Truncate user agent to reasonable length.
     */
    private static String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        if (userAgent.length() <= 100) {
            return userAgent;
        }
        return userAgent.substring(0, 97) + "...";
    }
}
