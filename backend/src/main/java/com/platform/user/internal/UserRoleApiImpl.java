package com.platform.user.internal;

import com.platform.user.api.UserRoleApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of UserRoleApi that bridges the public API to internal services.
 * This class is the only way external modules can access user role assignment functionality,
 * ensuring proper module boundaries are maintained.
 *
 * Following Spring Modulith principles, this implementation exposes only
 * the contracted API surface while keeping internal details hidden.
 */
@Component
class UserRoleApiImpl implements UserRoleApi {

    private final UserRoleService userRoleService;
    private final RoleRepository roleRepository;

    @Autowired
    public UserRoleApiImpl(UserRoleService userRoleService, RoleRepository roleRepository) {
        this.userRoleService = userRoleService;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserRoleAssignment assignRoleToUser(Long userId, Long roleId, Long organizationId,
                                             Long assignedBy, LocalDateTime expiresAt) {
        try {
            UserRoleService.AssignRoleRequest request = new UserRoleService.AssignRoleRequest(
                userId, roleId, organizationId, assignedBy, expiresAt
            );

            UserRole userRole = userRoleService.assignRoleToUser(request);
            return toUserRoleAssignment(userRole);
        } catch (UserRoleService.DuplicateRoleAssignmentException e) {
            throw new DuplicateRoleAssignmentException(e.getMessage());
        } catch (UserRoleService.RoleAssignmentLimitExceededException e) {
            throw new RoleAssignmentLimitExceededException(e.getMessage());
        } catch (Exception e) {
            throw new RoleAssignmentException("Failed to assign role", e);
        }
    }

    @Override
    public void removeRoleFromUser(Long userId, Long roleId, Long organizationId, Long removedBy) {
        try {
            userRoleService.removeRoleFromUser(userId, roleId, organizationId, removedBy);
        } catch (Exception e) {
            throw new RoleAssignmentException("Failed to remove role", e);
        }
    }

    @Override
    public List<UserRoleAssignment> getUserRoleAssignments(Long userId, Long organizationId) {
        return userRoleService.getUserRoleAssignments(userId, organizationId).stream()
            .map(this::toUserRoleAssignment)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserRoleAssignment> getRoleAssignments(Long roleId, Long organizationId) {
        return userRoleService.getRoleAssignments(roleId, organizationId).stream()
            .map(this::toUserRoleAssignment)
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasActiveRoleAssignment(Long userId, Long roleId, Long organizationId) {
        return userRoleService.hasActiveRoleAssignment(userId, roleId, organizationId);
    }

    @Override
    public List<UserRoleAssignment> getExpiringAssignments(Long organizationId, int daysAhead) {
        return userRoleService.getExpiringAssignments(organizationId, daysAhead).stream()
            .map(this::toUserRoleAssignment)
            .collect(Collectors.toList());
    }

    @Override
    public void extendRoleAssignment(Long userId, Long roleId, Long organizationId,
                                   LocalDateTime newExpirationDate, Long updatedBy) {
        try {
            userRoleService.extendRoleAssignment(userId, roleId, organizationId, newExpirationDate, updatedBy);
        } catch (Exception e) {
            throw new RoleAssignmentException("Failed to extend role assignment", e);
        }
    }

    @Override
    public UserRoleStatistics getUserRoleStatistics(Long organizationId) {
        UserRoleService.UserRoleStatistics stats = userRoleService.getUserRoleStatistics(organizationId);
        return new UserRoleStatistics(
            stats.totalAssignments(),
            stats.activeAssignments(),
            stats.temporaryAssignments(),
            stats.expiringWithinWeek(),
            stats.expiredAssignments()
        );
    }

    private UserRoleAssignment toUserRoleAssignment(UserRole userRole) {
        // Get role name for the assignment
        String roleName = roleRepository.findById(userRole.getRoleId())
            .map(Role::getName)
            .orElse("Unknown Role");

        return new UserRoleAssignment(
            userRole.getId(),
            userRole.getUserId(),
            userRole.getRoleId(),
            roleName,
            userRole.getAssignedAt(),
            userRole.getAssignedBy(),
            userRole.getExpiresAt(),
            userRole.isActive(),
            userRole.isExpired(),
            userRole.isTemporary()
        );
    }
}