package com.platform.user.events;

import java.time.Instant;

/**
 * Event published when a role is removed from a user.
 * This event enables other modules to react to role removals for audit logging,
 * notifications, cache invalidation, and access revocation.
 *
 * Following Spring Modulith event-driven communication patterns.
 */
public record UserRoleRemovedEvent(
    Long userId,
    Long roleId,
    String roleName,
    Long organizationId,
    String organizationName,
    Long removedBy,
    Instant removedAt,
    RemovalReason reason,
    String correlationId
) {
    public UserRoleRemovedEvent {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (removedBy == null) {
            throw new IllegalArgumentException("Removed by cannot be null");
        }
        if (removedAt == null) {
            throw new IllegalArgumentException("Removed at cannot be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Removal reason cannot be null");
        }
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
    }

    /**
     * Create a UserRoleRemovedEvent for manual removal.
     */
    public static UserRoleRemovedEvent forManualRemoval(
        Long userId, Long roleId, String roleName,
        Long organizationId, String organizationName,
        Long removedBy, String correlationId
    ) {
        return new UserRoleRemovedEvent(
            userId, roleId, roleName,
            organizationId, organizationName,
            removedBy, Instant.now(),
            RemovalReason.MANUAL_REMOVAL, correlationId
        );
    }

    /**
     * Create a UserRoleRemovedEvent for expiration.
     */
    public static UserRoleRemovedEvent forExpiration(
        Long userId, Long roleId, String roleName,
        Long organizationId, String organizationName,
        String correlationId
    ) {
        return new UserRoleRemovedEvent(
            userId, roleId, roleName,
            organizationId, organizationName,
            -1L, Instant.now(), // System removal
            RemovalReason.EXPIRED, correlationId
        );
    }

    /**
     * Create a UserRoleRemovedEvent for role deletion.
     */
    public static UserRoleRemovedEvent forRoleDeletion(
        Long userId, Long roleId, String roleName,
        Long organizationId, String organizationName,
        Long deletedBy, String correlationId
    ) {
        return new UserRoleRemovedEvent(
            userId, roleId, roleName,
            organizationId, organizationName,
            deletedBy, Instant.now(),
            RemovalReason.ROLE_DELETED, correlationId
        );
    }

    /**
     * Check if this is a high-privilege role removal.
     * Used for security monitoring and alerting.
     */
    public boolean isHighPrivilegeRole() {
        return roleName != null && (
            roleName.equalsIgnoreCase("owner") ||
            roleName.equalsIgnoreCase("admin") ||
            roleName.contains("admin")
        );
    }

    /**
     * Check if removal was automatic (system-driven).
     */
    public boolean isAutomaticRemoval() {
        return reason == RemovalReason.EXPIRED || removedBy == -1L;
    }

    /**
     * Get event type for routing and processing.
     */
    public String getEventType() {
        return "USER_ROLE_REMOVED";
    }

    /**
     * Enumeration of role removal reasons.
     */
    public enum RemovalReason {
        MANUAL_REMOVAL,     // Explicitly removed by an administrator
        EXPIRED,            // Automatically removed due to expiration
        ROLE_DELETED,       // Removed because the role was deleted
        USER_DEACTIVATED,   // Removed because user was deactivated
        ORG_MEMBERSHIP_REMOVED // Removed because user left organization
    }
}