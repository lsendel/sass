package com.platform.user.events;

import java.time.Instant;
import java.util.List;

/**
 * Event published when a custom role is deleted.
 * This event enables other modules to react to role deletion for audit logging,
 * cleanup operations, and user access revocation.
 *
 * Following Spring Modulith event-driven communication patterns.
 */
public record RoleDeletedEvent(
    Long roleId,
    String roleName,
    String roleDescription,
    Long organizationId,
    String organizationName,
    Long deletedBy,
    Instant deletedAt,
    List<String> permissions,
    long affectedUserCount,
    String correlationId
) {
    public RoleDeletedEvent {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (deletedBy == null) {
            throw new IllegalArgumentException("Deleted by cannot be null");
        }
        if (deletedAt == null) {
            throw new IllegalArgumentException("Deleted at cannot be null");
        }
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions list cannot be null");
        }
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
    }

    /**
     * Create a RoleDeletedEvent with role information.
     */
    public static RoleDeletedEvent create(
        Long roleId, String roleName, String roleDescription,
        Long organizationId, String organizationName,
        Long deletedBy, List<String> permissions,
        long affectedUserCount, String correlationId
    ) {
        return new RoleDeletedEvent(
            roleId, roleName, roleDescription,
            organizationId, organizationName,
            deletedBy, Instant.now(),
            List.copyOf(permissions),
            affectedUserCount, correlationId
        );
    }

    /**
     * Check if this role had high-privilege permissions.
     * Used for security monitoring and alerting.
     */
    public boolean hadHighPrivilegePermissions() {
        return permissions.stream().anyMatch(permission ->
            permission.contains("ADMIN") ||
            permission.contains("DELETE") ||
            permission.endsWith(":WRITE")
        );
    }

    /**
     * Check if this deletion affects multiple users.
     * Used for impact assessment and notification.
     */
    public boolean affectsMultipleUsers() {
        return affectedUserCount > 1;
    }

    /**
     * Check if this deletion has significant impact.
     * Used for approval workflows and security monitoring.
     */
    public boolean hasSignificantImpact() {
        return affectedUserCount > 0 && (affectsMultipleUsers() || hadHighPrivilegePermissions());
    }

    /**
     * Get event type for routing and processing.
     */
    public String getEventType() {
        return "ROLE_DELETED";
    }
}