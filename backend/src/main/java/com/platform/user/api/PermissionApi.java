package com.platform.user.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Public API interface for permission operations in the User module.
 * This interface defines the contract for external modules to interact with permission functionality.
 *
 * Following Spring Modulith principles, this API is the only way external modules
 * should access permission-related functionality.
 */
public interface PermissionApi {

    /**
     * Check if a user has a specific permission in an organization.
     * This is the primary permission checking method used across modules.
     *
     * @param userId the user ID to check
     * @param organizationId the organization context
     * @param resource the permission resource (e.g., "PAYMENTS", "USERS")
     * @param action the permission action (e.g., "READ", "WRITE", "ADMIN")
     * @return true if user has the permission, false otherwise
     */
    boolean hasUserPermission(Long userId, Long organizationId, String resource, String action);

    /**
     * Check multiple permissions for a user in an organization.
     * Efficient batch permission checking for complex authorization scenarios.
     *
     * @param userId the user ID to check
     * @param organizationId the organization context
     * @param requests list of permission check requests
     * @return list of permission check results
     */
    List<PermissionCheckResult> checkUserPermissions(Long userId, Long organizationId,
                                                   List<PermissionCheckRequest> requests);

    /**
     * Get all permissions assigned to a user in an organization.
     * Returns the complete set of permissions for comprehensive authorization.
     *
     * @param userId the user ID
     * @param organizationId the organization context
     * @return set of permission keys in "RESOURCE:ACTION" format
     */
    Set<String> getUserPermissionKeys(Long userId, Long organizationId);

    /**
     * Get all available system permissions.
     * Used for permission management interfaces and role creation.
     *
     * @return list of all active permissions in the system
     */
    List<PermissionInfo> getAllPermissions();

    /**
     * Get all unique resources in the permission system.
     * Used for resource-based permission management interfaces.
     *
     * @return list of resource names
     */
    List<String> getAllResources();

    /**
     * Get all actions available for a specific resource.
     * Used for building dynamic permission management interfaces.
     *
     * @param resource the resource name
     * @return list of action names for the resource
     */
    List<String> getActionsByResource(String resource);

    /**
     * Validate permission format and values.
     * Used by external modules before making permission-related requests.
     *
     * @param resource the permission resource to validate
     * @param action the permission action to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validatePermission(String resource, String action);

    /**
     * Data class representing a permission check request.
     */
    record PermissionCheckRequest(String resource, String action) {
        public PermissionCheckRequest {
            if (resource == null || resource.trim().isEmpty()) {
                throw new IllegalArgumentException("Resource cannot be null or empty");
            }
            if (action == null || action.trim().isEmpty()) {
                throw new IllegalArgumentException("Action cannot be null or empty");
            }
        }
    }

    /**
     * Data class representing a permission check result.
     */
    record PermissionCheckResult(String resource, String action, boolean hasPermission) {}

    /**
     * Data class representing permission information for external consumption.
     */
    record PermissionInfo(Long id, String resource, String action, String description) {}
}