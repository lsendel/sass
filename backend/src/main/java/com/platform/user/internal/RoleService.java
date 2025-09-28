package com.platform.user.internal;

import com.platform.user.events.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing roles in the RBAC system.
 * Handles role creation, modification, permission assignment, and organization-scoped operations.
 *
 * Supports both predefined system roles and custom user-created roles with comprehensive
 * validation and caching for optimal performance.
 */
@Service
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public RoleService(RoleRepository roleRepository,
                      PermissionRepository permissionRepository,
                      RolePermissionRepository rolePermissionRepository,
                      UserRoleRepository userRoleRepository,
                      ApplicationEventPublisher eventPublisher) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Get all roles for an organization.
     * Returns both predefined and custom roles.
     */
    @Cacheable(value = "organizationRoles", key = "#organizationId")
    public List<Role> getRolesByOrganization(Long organizationId) {
        return roleRepository.findByOrganizationId(organizationId);
    }

    /**
     * Get a specific role by ID within an organization.
     * Ensures organization-scoped access control.
     */
    public Optional<Role> getRoleByIdAndOrganization(Long roleId, Long organizationId) {
        return roleRepository.findByIdAndOrganizationId(roleId, organizationId);
    }

    /**
     * Find a role by name and type within an organization.
     * Used for role lookup and validation.
     */
    public Optional<Role> findRoleByName(Long organizationId, String name, RoleType roleType) {
        return roleRepository.findByOrganizationIdAndNameAndRoleType(organizationId, name, roleType);
    }

    /**
     * Get all custom roles for an organization.
     * Used for custom role management.
     */
    @Cacheable(value = "customRoles", key = "#organizationId")
    public List<Role> getCustomRolesByOrganization(Long organizationId) {
        return roleRepository.findCustomRolesByOrganizationId(organizationId);
    }

    /**
     * Get all predefined roles for an organization.
     * Used for system role validation.
     */
    @Cacheable(value = "predefinedRoles", key = "#organizationId")
    public List<Role> getPredefinedRolesByOrganization(Long organizationId) {
        return roleRepository.findPredefinedRolesByOrganizationId(organizationId);
    }

    /**
     * Create a new custom role in an organization.
     * Validates role limits, name uniqueness, and permission assignments.
     */
    @Transactional
    @CacheEvict(value = {"organizationRoles", "customRoles"}, key = "#organizationId")
    public Role createCustomRole(CreateRoleRequest request, Long organizationId, Long createdBy) {
        // Validate request
        validateCreateRoleRequest(request, organizationId);

        // Check role limits
        validateRoleLimits(organizationId);

        // Create the role
        Role role = new Role(
            organizationId,
            request.getName().toLowerCase(),
            request.getDescription(),
            RoleType.CUSTOM,
            createdBy
        );

        role = roleRepository.save(role);

        // Assign permissions
        assignPermissionsToRole(role.getId(), request.getPermissionIds());

        // Publish role created event
        publishRoleCreatedEvent(role, request.getPermissionIds(), organizationId);

        return role;
    }

    /**
     * Update an existing custom role.
     * Only custom roles can be modified; predefined roles are immutable.
     */
    @Transactional
    @CacheEvict(value = {"organizationRoles", "customRoles", "rolePermissions"}, key = "#organizationId")
    public Role updateCustomRole(Long roleId, UpdateRoleRequest request, Long organizationId, Long updatedBy) {
        Role role = getRoleByIdAndOrganization(roleId, organizationId)
            .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));

        if (!role.canBeModified()) {
            throw new RoleModificationException("Cannot modify predefined or inactive role: " + roleId);
        }

        // Validate name uniqueness if changed
        if (!role.getName().equals(request.getName())) {
            validateRoleNameUniqueness(organizationId, request.getName(), roleId);
        }

        // Update role properties
        role.setName(request.getName().toLowerCase());
        role.setDescription(request.getDescription());
        role.setUpdatedBy(updatedBy);

        role = roleRepository.save(role);

        // Update permissions if provided
        if (request.getPermissionIds() != null) {
            updateRolePermissions(role.getId(), request.getPermissionIds());
        }

        return role;
    }

    /**
     * Delete a custom role.
     * Only custom roles with no active assignments can be deleted.
     */
    @Transactional
    @CacheEvict(value = {"organizationRoles", "customRoles", "rolePermissions"}, key = "#organizationId")
    public void deleteCustomRole(Long roleId, Long organizationId) {
        Role role = getRoleByIdAndOrganization(roleId, organizationId)
            .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));

        if (!role.canBeDeleted()) {
            throw new RoleModificationException("Cannot delete predefined or inactive role: " + roleId);
        }

        // Check for active assignments
        long activeAssignments = roleRepository.countAssignedUsers(roleId);
        if (activeAssignments > 0) {
            throw new RoleInUseException("Cannot delete role with active user assignments: " + activeAssignments);
        }

        // Soft delete the role
        role.setIsActive(false);
        roleRepository.save(role);

        // Remove all permission assignments
        rolePermissionRepository.deleteByRoleId(roleId);
    }

    /**
     * Assign permissions to a role.
     * Replaces existing permission assignments.
     */
    @Transactional
    @CacheEvict(value = "rolePermissions", key = "#roleId")
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        // Validate permissions exist and are active
        List<Permission> permissions = permissionRepository.findByIds(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new InvalidPermissionException("One or more permission IDs are invalid");
        }

        // Remove existing permissions
        rolePermissionRepository.deleteByRoleId(roleId);

        // Add new permissions
        for (Long permissionId : permissionIds) {
            RolePermission rolePermission = new RolePermission(roleId, permissionId);
            rolePermissionRepository.save(rolePermission);
        }
    }

    /**
     * Update role permissions by replacing existing assignments.
     */
    @Transactional
    @CacheEvict(value = "rolePermissions", key = "#roleId")
    public void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        assignPermissionsToRole(roleId, permissionIds);
    }

    /**
     * Add a single permission to a role.
     */
    @Transactional
    @CacheEvict(value = "rolePermissions", key = "#roleId")
    public void addPermissionToRole(Long roleId, Long permissionId) {
        // Check if permission already assigned
        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new DuplicateRolePermissionException("Permission already assigned to role");
        }

        // Validate permission exists
        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new InvalidPermissionException("Permission not found: " + permissionId));

        if (!permission.isActive()) {
            throw new InvalidPermissionException("Cannot assign inactive permission: " + permissionId);
        }

        RolePermission rolePermission = new RolePermission(roleId, permissionId);
        rolePermissionRepository.save(rolePermission);
    }

    /**
     * Remove a permission from a role.
     */
    @Transactional
    @CacheEvict(value = "rolePermissions", key = "#roleId")
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    /**
     * Get all roles assigned to a user.
     */
    @Cacheable(value = "userRoles", key = "#userId")
    public List<Role> getUserRoles(Long userId) {
        return roleRepository.findByUserId(userId);
    }

    /**
     * Get roles assigned to a user in a specific organization.
     */
    @Cacheable(value = "userOrganizationRoles", key = "#userId + ':' + #organizationId")
    public List<Role> getUserRolesByOrganization(Long userId, Long organizationId) {
        return roleRepository.findByUserIdAndOrganizationId(userId, organizationId);
    }

    /**
     * Get role statistics for an organization.
     */
    public RoleStatistics getRoleStatistics(Long organizationId) {
        List<Role> allRoles = getRolesByOrganization(organizationId);
        long customRoleCount = roleRepository.countByOrganizationIdAndRoleType(organizationId, RoleType.CUSTOM);
        long predefinedRoleCount = allRoles.size() - customRoleCount;

        return new RoleStatistics(
            allRoles.size(),
            customRoleCount,
            predefinedRoleCount,
            allRoles
        );
    }

    // Validation methods

    private void validateCreateRoleRequest(CreateRoleRequest request, Long organizationId) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new InvalidRoleException("Role name cannot be null or empty");
        }
        if (request.getName().length() > 100) {
            throw new InvalidRoleException("Role name cannot exceed 100 characters");
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new InvalidRoleException("Role description cannot exceed 500 characters");
        }
        if (request.getPermissionIds() == null || request.getPermissionIds().isEmpty()) {
            throw new InvalidRoleException("Role must have at least one permission");
        }

        validateRoleNameUniqueness(organizationId, request.getName(), null);
    }

    private void validateRoleNameUniqueness(Long organizationId, String name, Long excludeRoleId) {
        boolean exists = excludeRoleId != null ?
            roleRepository.existsByOrganizationIdAndNameExcluding(organizationId, name.toLowerCase(), excludeRoleId) :
            roleRepository.existsByOrganizationIdAndName(organizationId, name.toLowerCase());

        if (exists) {
            throw new DuplicateRoleNameException("Role name already exists: " + name);
        }
    }

    private void validateRoleLimits(Long organizationId) {
        // Implementation would check configured role limits
        // For now, we'll use a simple limit
        long customRoleCount = roleRepository.countByOrganizationIdAndRoleType(organizationId, RoleType.CUSTOM);
        if (customRoleCount >= 50) { // Configurable limit
            throw new RoleLimitExceededException("Maximum number of custom roles exceeded");
        }
    }

    // Data classes for requests and responses

    public static class CreateRoleRequest {
        private String name;
        private String description;
        private List<Long> permissionIds;

        public CreateRoleRequest(String name, String description, List<Long> permissionIds) {
            this.name = name;
            this.description = description;
            this.permissionIds = permissionIds;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<Long> getPermissionIds() { return permissionIds; }
    }

    public static class UpdateRoleRequest {
        private String name;
        private String description;
        private List<Long> permissionIds;

        public UpdateRoleRequest(String name, String description, List<Long> permissionIds) {
            this.name = name;
            this.description = description;
            this.permissionIds = permissionIds;
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<Long> getPermissionIds() { return permissionIds; }
    }

    public static class RoleStatistics {
        private final long totalRoles;
        private final long customRoles;
        private final long predefinedRoles;
        private final List<Role> roles;

        public RoleStatistics(long totalRoles, long customRoles, long predefinedRoles, List<Role> roles) {
            this.totalRoles = totalRoles;
            this.customRoles = customRoles;
            this.predefinedRoles = predefinedRoles;
            this.roles = roles;
        }

        // Getters
        public long getTotalRoles() { return totalRoles; }
        public long getCustomRoles() { return customRoles; }
        public long getPredefinedRoles() { return predefinedRoles; }
        public List<Role> getRoles() { return roles; }
    }

    // Exception classes
    public static class RoleNotFoundException extends RuntimeException {
        public RoleNotFoundException(String message) { super(message); }
    }

    public static class RoleModificationException extends RuntimeException {
        public RoleModificationException(String message) { super(message); }
    }

    public static class RoleInUseException extends RuntimeException {
        public RoleInUseException(String message) { super(message); }
    }

    public static class InvalidRoleException extends RuntimeException {
        public InvalidRoleException(String message) { super(message); }
    }

    public static class DuplicateRoleNameException extends RuntimeException {
        public DuplicateRoleNameException(String message) { super(message); }
    }

    public static class RoleLimitExceededException extends RuntimeException {
        public RoleLimitExceededException(String message) { super(message); }
    }

    public static class InvalidPermissionException extends RuntimeException {
        public InvalidPermissionException(String message) { super(message); }
    }

    public static class DuplicateRolePermissionException extends RuntimeException {
        public DuplicateRolePermissionException(String message) { super(message); }
    }

    // Event publishing methods

    private void publishRoleCreatedEvent(Role role, List<Long> permissionIds, Long organizationId) {
        List<String> permissionKeys = permissionRepository.findByIds(permissionIds).stream()
            .map(Permission::getPermissionKey)
            .collect(Collectors.toList());

        String correlationId = UUID.randomUUID().toString();

        RoleCreatedEvent event = RoleCreatedEvent.create(
            role.getId(),
            role.getName(),
            role.getDescription(),
            organizationId,
            "Organization", // Would get actual org name
            role.getCreatedBy(),
            permissionKeys,
            correlationId
        );

        eventPublisher.publishEvent(event);
    }

    private void publishRoleModifiedEvent(Role role, List<String> previousPermissions,
                                        List<String> newPermissions, Long organizationId) {
        String correlationId = UUID.randomUUID().toString();

        RoleModifiedEvent event = RoleModifiedEvent.create(
            role.getId(),
            role.getName(),
            role.getDescription(),
            organizationId,
            "Organization", // Would get actual org name
            role.getUpdatedBy(),
            previousPermissions,
            newPermissions,
            correlationId
        );

        eventPublisher.publishEvent(event);
    }

    private void publishRoleDeletedEvent(Role role, List<String> permissions,
                                       long affectedUserCount, Long organizationId) {
        String correlationId = UUID.randomUUID().toString();

        RoleDeletedEvent event = RoleDeletedEvent.create(
            role.getId(),
            role.getName(),
            role.getDescription(),
            organizationId,
            "Organization", // Would get actual org name
            role.getUpdatedBy(),
            permissions,
            affectedUserCount,
            correlationId
        );

        eventPublisher.publishEvent(event);
    }
}