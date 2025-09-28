package com.platform.user.internal;

import com.platform.user.api.PermissionApi;
import com.platform.user.api.RoleApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of RoleApi that bridges the public API to internal services.
 * This class is the only way external modules can access role functionality,
 * ensuring proper module boundaries are maintained.
 *
 * Following Spring Modulith principles, this implementation exposes only
 * the contracted API surface while keeping internal details hidden.
 */
@Component
class RoleApiImpl implements RoleApi {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RoleRepository roleRepository;

    @Autowired
    public RoleApiImpl(RoleService roleService,
                       PermissionService permissionService,
                       RoleRepository roleRepository) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.roleRepository = roleRepository;
    }

    @Override
    public List<RoleInfo> getRolesByOrganization(Long organizationId) {
        return roleService.getRolesByOrganization(organizationId).stream()
            .map(this::toRoleInfo)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<RoleDetails> getRoleDetails(Long roleId, Long organizationId) {
        return roleService.getRoleByIdAndOrganization(roleId, organizationId)
            .map(this::toRoleDetails);
    }

    @Override
    public List<RoleInfo> getUserRoles(Long userId, Long organizationId) {
        return roleService.getUserRolesByOrganization(userId, organizationId).stream()
            .map(this::toRoleInfo)
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasUserRole(Long userId, Long organizationId, String roleName) {
        return roleService.getUserRolesByOrganization(userId, organizationId).stream()
            .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }

    @Override
    public List<RoleInfo> getRolesWithPermission(Long organizationId, String resource, String action) {
        return roleRepository.findByOrganizationIdAndPermission(organizationId, resource, action).stream()
            .map(this::toRoleInfo)
            .collect(Collectors.toList());
    }

    @Override
    public RoleStatistics getRoleStatistics(Long organizationId) {
        RoleService.RoleStatistics stats = roleService.getRoleStatistics(organizationId);
        return new RoleStatistics(
            stats.getTotalRoles(),
            stats.getCustomRoles(),
            stats.getPredefinedRoles(),
            calculateTotalAssignedUsers(organizationId)
        );
    }

    private RoleInfo toRoleInfo(Role role) {
        long assignedUserCount = roleRepository.countAssignedUsers(role.getId());
        return new RoleInfo(
            role.getId(),
            role.getName(),
            role.getDescription(),
            mapRoleType(role.getRoleType()),
            role.isActive(),
            role.getCreatedAt(),
            assignedUserCount
        );
    }

    private RoleDetails toRoleDetails(Role role) {
        List<Permission> permissions = permissionService.getPermissionsByRoleId(role.getId());
        List<PermissionApi.PermissionInfo> permissionInfos = permissions.stream()
            .map(p -> new PermissionApi.PermissionInfo(p.getId(), p.getResource(), p.getAction(), p.getDescription()))
            .collect(Collectors.toList());

        long assignedUserCount = roleRepository.countAssignedUsers(role.getId());

        return new RoleDetails(
            role.getId(),
            role.getName(),
            role.getDescription(),
            mapRoleType(role.getRoleType()),
            role.isActive(),
            role.getCreatedAt(),
            role.getCreatedBy(),
            permissionInfos,
            assignedUserCount
        );
    }

    private RoleType mapRoleType(com.platform.user.internal.RoleType internalType) {
        return switch (internalType) {
            case PREDEFINED -> RoleType.PREDEFINED;
            case CUSTOM -> RoleType.CUSTOM;
        };
    }

    private long calculateTotalAssignedUsers(Long organizationId) {
        // This would be optimized with a proper query
        return roleService.getRolesByOrganization(organizationId).stream()
            .mapToLong(role -> roleRepository.countAssignedUsers(role.getId()))
            .sum();
    }
}