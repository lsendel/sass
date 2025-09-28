package com.platform.user.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Public API interface for role operations in the User module.
 * This interface defines the contract for external modules to interact with role functionality.
 *
 * Following Spring Modulith principles, this API is the only way external modules
 * should access role-related functionality.
 */
public interface RoleApi {

    /**
     * Get all roles available in an organization.
     * Returns both predefined and custom roles for comprehensive role management.
     *
     * @param organizationId the organization ID
     * @return list of role information objects
     */
    List<RoleInfo> getRolesByOrganization(Long organizationId);

    /**
     * Get detailed information about a specific role.
     * Includes role metadata and assigned permissions.
     *
     * @param roleId the role ID
     * @param organizationId the organization context for security
     * @return optional role details, empty if not found or no access
     */
    Optional<RoleDetails> getRoleDetails(Long roleId, Long organizationId);

    /**
     * Get all roles assigned to a user in a specific organization.
     * Used for user profile management and permission calculation.
     *
     * @param userId the user ID
     * @param organizationId the organization context
     * @return list of roles assigned to the user
     */
    List<RoleInfo> getUserRoles(Long userId, Long organizationId);

    /**
     * Check if a user has a specific role in an organization.
     * Used for role-based authorization checks.
     *
     * @param userId the user ID
     * @param organizationId the organization context
     * @param roleName the role name to check
     * @return true if user has the role, false otherwise
     */
    boolean hasUserRole(Long userId, Long organizationId, String roleName);

    /**
     * Get roles that have a specific permission.
     * Used for permission impact analysis and role discovery.
     *
     * @param organizationId the organization context
     * @param resource the permission resource
     * @param action the permission action
     * @return list of roles that have the specified permission
     */
    List<RoleInfo> getRolesWithPermission(Long organizationId, String resource, String action);

    /**
     * Get statistics about roles in an organization.
     * Used for organizational metrics and reporting.
     *
     * @param organizationId the organization ID
     * @return role statistics object
     */
    RoleStatistics getRoleStatistics(Long organizationId);

    /**
     * Data class representing basic role information for external consumption.
     */
    record RoleInfo(
        Long id,
        String name,
        String description,
        RoleType roleType,
        boolean isActive,
        Instant createdAt,
        Long assignedUserCount
    ) {}

    /**
     * Data class representing detailed role information with permissions.
     */
    record RoleDetails(
        Long id,
        String name,
        String description,
        RoleType roleType,
        boolean isActive,
        Instant createdAt,
        Long createdBy,
        List<PermissionApi.PermissionInfo> permissions,
        Long assignedUserCount
    ) {}

    /**
     * Data class representing role statistics for an organization.
     */
    record RoleStatistics(
        long totalRoles,
        long customRoles,
        long predefinedRoles,
        long totalAssignedUsers
    ) {}

    /**
     * Enumeration for role types visible to external modules.
     */
    enum RoleType {
        PREDEFINED,
        CUSTOM
    }
}