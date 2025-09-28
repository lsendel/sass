package com.platform.user.events;

import java.time.Instant;
import java.util.List;

/**
 * Event published when a new custom role is created in an organization.
 * This event enables other modules to react to role creation for audit logging,
 * notifications, and organizational analytics.
 *
 * Following Spring Modulith event-driven communication patterns.
 */
public record RoleCreatedEvent(
    Long roleId,
    String roleName,
    String roleDescription,
    Long organizationId,
    String organizationName,
    Long createdBy,
    Instant createdAt,
    List<String> permissions,
    String correlationId
) {
    public RoleCreatedEvent {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Created by cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at cannot be null");
        }
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions list cannot be null");
        }
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
    }

    /**
     * Create a RoleCreatedEvent with permission information.
     */
    public static RoleCreatedEvent create(
        Long roleId, String roleName, String roleDescription,
        Long organizationId, String organizationName,
        Long createdBy, List<String> permissions, String correlationId
    ) {
        return new RoleCreatedEvent(
            roleId, roleName, roleDescription,
            organizationId, organizationName,
            createdBy, Instant.now(),
            List.copyOf(permissions), correlationId
        );
    }

    /**
     * Check if this role has high-privilege permissions.
     * Used for security monitoring and approval workflows.
     */
    public boolean hasHighPrivilegePermissions() {
        return permissions.stream().anyMatch(permission ->
            permission.contains("ADMIN") ||
            permission.contains("DELETE") ||
            permission.endsWith(":WRITE")
        );
    }

    /**
     * Get count of permissions assigned to this role.
     */
    public int getPermissionCount() {
        return permissions.size();
    }

    /**
     * Get event type for routing and processing.
     */
    public String getEventType() {
        return "ROLE_CREATED";
    }
}