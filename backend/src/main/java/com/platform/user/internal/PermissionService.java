package com.platform.user.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class for managing permissions in the RBAC system.
 * Provides business logic for permission operations, validation, and caching.
 *
 * This service handles system-level permissions that are shared across all organizations.
 * Individual organizations cannot create custom permissions but can assign existing
 * permissions to their custom roles.
 */
@Service
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Autowired
    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /**
     * Get all active permissions in the system.
     * Results are cached for performance.
     */
    @Cacheable(value = "permissions", key = "'all'")
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAllActive();
    }

    /**
     * Find a permission by its resource and action.
     * Used for permission lookup and validation.
     */
    @Cacheable(value = "permissions", key = "#resource + ':' + #action")
    public Optional<Permission> findPermission(String resource, String action) {
        return permissionRepository.findByResourceAndAction(resource, action);
    }

    /**
     * Get all permissions for a specific resource.
     * Used for resource-based permission management.
     */
    @Cacheable(value = "permissions", key = "'resource:' + #resource")
    public List<Permission> getPermissionsByResource(String resource) {
        return permissionRepository.findByResource(resource);
    }

    /**
     * Get all unique resources in the system.
     * Used for permission management interfaces.
     */
    @Cacheable(value = "permissions", key = "'resources'")
    public List<String> getAllResources() {
        return permissionRepository.findAllResources();
    }

    /**
     * Get all actions for a specific resource.
     * Used for permission management interfaces.
     */
    @Cacheable(value = "permissions", key = "'actions:' + #resource")
    public List<String> getActionsByResource(String resource) {
        return permissionRepository.findActionsByResource(resource);
    }

    /**
     * Get permissions by their IDs.
     * Used for bulk permission operations in role management.
     */
    public List<Permission> getPermissionsByIds(List<Long> permissionIds) {
        return permissionRepository.findByIds(permissionIds);
    }

    /**
     * Check if all provided permission IDs are valid and active.
     * Used for role creation and modification validation.
     */
    public boolean areValidPermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return false;
        }

        List<Permission> permissions = permissionRepository.findByIds(permissionIds);
        return permissions.size() == permissionIds.size();
    }

    /**
     * Get all permissions assigned to a role.
     * Used for role permission display and management.
     */
    @Cacheable(value = "rolePermissions", key = "#roleId")
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    /**
     * Get all permissions for a user in a specific organization.
     * This is the core permission lookup method that uses the optimized database function.
     */
    @Cacheable(value = "userPermissions", key = "#userId + ':' + #organizationId")
    public List<Permission> getUserPermissions(Long userId, Long organizationId) {
        return permissionRepository.findByUserId(userId, organizationId);
    }

    /**
     * Check if a user has a specific permission in an organization.
     * Optimized single permission check using the database function.
     */
    @Cacheable(value = "userPermissionCheck", key = "#userId + ':' + #organizationId + ':' + #resource + ':' + #action")
    public boolean hasUserPermission(Long userId, Long organizationId, String resource, String action) {
        return permissionRepository.hasUserPermission(userId, organizationId, resource, action);
    }

    /**
     * Check multiple permissions for a user in an organization.
     * Batch permission checking for efficiency.
     */
    public List<PermissionCheckResult> checkUserPermissions(Long userId, Long organizationId,
                                                           List<PermissionCheckRequest> requests) {
        return requests.stream()
            .map(request -> new PermissionCheckResult(
                request.getResource(),
                request.getAction(),
                hasUserPermission(userId, organizationId, request.getResource(), request.getAction())
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get user permissions as a set of permission keys (RESOURCE:ACTION).
     * Used for quick permission checking and caching.
     */
    @Cacheable(value = "userPermissionKeys", key = "#userId + ':' + #organizationId")
    public Set<String> getUserPermissionKeys(Long userId, Long organizationId) {
        return getUserPermissions(userId, organizationId).stream()
            .map(Permission::getPermissionKey)
            .collect(Collectors.toSet());
    }

    /**
     * Validate permission format and values.
     * Used for permission creation and validation.
     */
    public void validatePermission(String resource, String action) {
        if (resource == null || resource.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission resource cannot be null or empty");
        }
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission action cannot be null or empty");
        }
        if (resource.length() > 50) {
            throw new IllegalArgumentException("Permission resource cannot exceed 50 characters");
        }
        if (action.length() > 50) {
            throw new IllegalArgumentException("Permission action cannot exceed 50 characters");
        }
        if (!resource.matches("^[A-Z_]+$")) {
            throw new IllegalArgumentException("Permission resource must contain only uppercase letters and underscores");
        }
        if (!action.matches("^[A-Z_]+$")) {
            throw new IllegalArgumentException("Permission action must contain only uppercase letters and underscores");
        }
    }

    /**
     * Get system metrics about permissions.
     * Used for monitoring and analytics.
     */
    public PermissionMetrics getPermissionMetrics() {
        long totalPermissions = permissionRepository.countActive();
        List<String> resources = getAllResources();

        return new PermissionMetrics(
            totalPermissions,
            resources.size(),
            resources
        );
    }

    /**
     * Data class for permission check requests.
     */
    public static class PermissionCheckRequest {
        private final String resource;
        private final String action;

        public PermissionCheckRequest(String resource, String action) {
            this.resource = resource;
            this.action = action;
        }

        public String getResource() { return resource; }
        public String getAction() { return action; }
    }

    /**
     * Data class for permission check results.
     */
    public static class PermissionCheckResult {
        private final String resource;
        private final String action;
        private final boolean hasPermission;

        public PermissionCheckResult(String resource, String action, boolean hasPermission) {
            this.resource = resource;
            this.action = action;
            this.hasPermission = hasPermission;
        }

        public String getResource() { return resource; }
        public String getAction() { return action; }
        public boolean hasPermission() { return hasPermission; }
    }

    /**
     * Data class for permission system metrics.
     */
    public static class PermissionMetrics {
        private final long totalPermissions;
        private final long totalResources;
        private final List<String> resources;

        public PermissionMetrics(long totalPermissions, long totalResources, List<String> resources) {
            this.totalPermissions = totalPermissions;
            this.totalResources = totalResources;
            this.resources = resources;
        }

        public long getTotalPermissions() { return totalPermissions; }
        public long getTotalResources() { return totalResources; }
        public List<String> getResources() { return resources; }
    }
}