package com.platform.user.events;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Event published when a role is assigned to a user.
 * This event enables other modules to react to role assignments for audit logging,
 * notifications, cache invalidation, and other cross-cutting concerns.
 *
 * Following Spring Modulith event-driven communication patterns.
 */
public record UserRoleAssignedEvent(
    Long userId,
    Long roleId,
    String roleName,
    Long organizationId,
    String organizationName,
    Long assignedBy,
    Instant assignedAt,
    LocalDateTime expiresAt,
    boolean isTemporary,
    String correlationId
) {
    public UserRoleAssignedEvent {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (assignedBy == null) {
            throw new IllegalArgumentException("Assigned by cannot be null");
        }
        if (assignedAt == null) {
            throw new IllegalArgumentException("Assigned at cannot be null");
        }
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
    }

    /**
     * Create a UserRoleAssignedEvent for a permanent role assignment.
     */
    public static UserRoleAssignedEvent forPermanentAssignment(
        Long userId, Long roleId, String roleName,
        Long organizationId, String organizationName,
        Long assignedBy, String correlationId
    ) {
        return new UserRoleAssignedEvent(
            userId, roleId, roleName,
            organizationId, organizationName,
            assignedBy, Instant.now(), null, false, correlationId
        );
    }

    /**
     * Create a UserRoleAssignedEvent for a temporary role assignment.
     */
    public static UserRoleAssignedEvent forTemporaryAssignment(
        Long userId, Long roleId, String roleName,
        Long organizationId, String organizationName,
        Long assignedBy, LocalDateTime expiresAt, String correlationId
    ) {
        return new UserRoleAssignedEvent(
            userId, roleId, roleName,
            organizationId, organizationName,
            assignedBy, Instant.now(), expiresAt, true, correlationId
        );
    }

    /**
     * Check if this is a high-privilege role assignment.
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
     * Get event type for routing and processing.
     */
    public String getEventType() {
        return "USER_ROLE_ASSIGNED";
    }
}