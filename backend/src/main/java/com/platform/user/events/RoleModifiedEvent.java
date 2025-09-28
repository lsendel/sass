package com.platform.user.events;

import java.time.Instant;
import java.util.List;

/**
 * Event published when a custom role is modified.
 * This event enables other modules to react to role changes for audit logging,
 * cache invalidation, and permission recalculation.
 *
 * Following Spring Modulith event-driven communication patterns.
 */
public record RoleModifiedEvent(
    Long roleId,
    String roleName,
    String roleDescription,
    Long organizationId,
    String organizationName,
    Long modifiedBy,
    Instant modifiedAt,
    List<String> previousPermissions,
    List<String> newPermissions,
    List<String> addedPermissions,
    List<String> removedPermissions,
    String correlationId
) {
    public RoleModifiedEvent {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (modifiedBy == null) {
            throw new IllegalArgumentException("Modified by cannot be null");
        }
        if (modifiedAt == null) {
            throw new IllegalArgumentException("Modified at cannot be null");
        }
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
    }

    /**
     * Create a RoleModifiedEvent with permission change details.
     */
    public static RoleModifiedEvent create(
        Long roleId, String roleName, String roleDescription,
        Long organizationId, String organizationName,
        Long modifiedBy, List<String> previousPermissions,
        List<String> newPermissions, String correlationId
    ) {
        List<String> added = newPermissions.stream()
            .filter(p -> !previousPermissions.contains(p))
            .toList();

        List<String> removed = previousPermissions.stream()
            .filter(p -> !newPermissions.contains(p))
            .toList();

        return new RoleModifiedEvent(
            roleId, roleName, roleDescription,
            organizationId, organizationName,
            modifiedBy, Instant.now(),
            List.copyOf(previousPermissions),
            List.copyOf(newPermissions),
            added, removed, correlationId
        );
    }

    /**
     * Check if permissions were actually changed.
     */
    public boolean hasPermissionChanges() {
        return !addedPermissions.isEmpty() || !removedPermissions.isEmpty();
    }

    /**
     * Check if high-privilege permissions were added.
     * Used for security monitoring and approval workflows.
     */
    public boolean hasHighPrivilegePermissionsAdded() {
        return addedPermissions.stream().anyMatch(permission ->
            permission.contains("ADMIN") ||
            permission.contains("DELETE") ||
            permission.endsWith(":WRITE")
        );
    }

    /**
     * Check if high-privilege permissions were removed.
     * Used for security monitoring and access auditing.
     */
    public boolean hasHighPrivilegePermissionsRemoved() {
        return removedPermissions.stream().anyMatch(permission ->
            permission.contains("ADMIN") ||
            permission.contains("DELETE") ||
            permission.endsWith(":WRITE")
        );
    }

    /**
     * Get count of affected users (would need to be populated by service).
     */
    public boolean requiresUserPermissionRecalculation() {
        return hasPermissionChanges();
    }

    /**
     * Get event type for routing and processing.
     */
    public String getEventType() {
        return "ROLE_MODIFIED";
    }
}