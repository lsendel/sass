package com.platform.user.internal;

import com.platform.user.events.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing user role assignments in the RBAC system.
 * Handles role assignment, removal, expiration, and publishes events for cross-module integration.
 *
 * Following Spring Modulith principles with event-driven communication and proper caching.
 */
@Service
@Transactional(readOnly = true)
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public UserRoleService(UserRoleRepository userRoleRepository,
                          RoleRepository roleRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Assign a role to a user with optional expiration.
     * Validates role assignment rules and publishes events.
     */
    @Transactional
    @CacheEvict(value = {"userRoles", "userOrganizationRoles", "userPermissions", "userPermissionKeys"},
                key = "#userId")
    public UserRole assignRoleToUser(AssignRoleRequest request) {
        // Validate request
        validateAssignRoleRequest(request);

        // Check for existing assignment
        if (userRoleRepository.hasActiveAssignment(request.getUserId(), request.getRoleId())) {
            throw new DuplicateRoleAssignmentException(
                "User already has active assignment to role: " + request.getRoleId());
        }

        // Get role information for validation and events
        Role role = roleRepository.findByIdAndOrganizationId(request.getRoleId(), request.getOrganizationId())
            .orElseThrow(() -> new RoleNotFoundException("Role not found: " + request.getRoleId()));

        // Check role assignment limits
        validateRoleAssignmentLimits(request.getUserId(), request.getOrganizationId());

        // Create the assignment
        UserRole userRole = new UserRole(
            request.getUserId(),
            request.getRoleId(),
            request.getAssignedBy(),
            request.getExpiresAt()
        );

        userRole = userRoleRepository.save(userRole);

        // Publish event
        publishRoleAssignedEvent(userRole, role, request.getOrganizationId());

        return userRole;
    }

    /**
     * Remove a role assignment from a user.
     * Marks the assignment as removed and publishes events.
     */
    @Transactional
    @CacheEvict(value = {"userRoles", "userOrganizationRoles", "userPermissions", "userPermissionKeys"},
                key = "#userId")
    public void removeRoleFromUser(Long userId, Long roleId, Long organizationId, Long removedBy) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
            .orElseThrow(() -> new RoleAssignmentNotFoundException(
                "No active role assignment found for user " + userId + " and role " + roleId));

        if (!userRole.isActive()) {
            throw new RoleAssignmentException("Role assignment is not active");
        }

        // Get role information for events
        Role role = roleRepository.findByIdAndOrganizationId(roleId, organizationId)
            .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));

        // Mark as removed
        userRole.markAsRemoved(removedBy);
        userRoleRepository.save(userRole);

        // Publish event
        publishRoleRemovedEvent(userRole, role, organizationId, UserRoleRemovedEvent.RemovalReason.MANUAL_REMOVAL);
    }

    /**
     * Get all active role assignments for a user in an organization.
     */
    @Cacheable(value = "userOrganizationRoles", key = "#userId + ':' + #organizationId")
    public List<UserRole> getUserRoleAssignments(Long userId, Long organizationId) {
        return userRoleRepository.findActiveByUserIdAndOrganizationId(userId, organizationId);
    }

    /**
     * Get all users assigned to a specific role.
     */
    @Cacheable(value = "roleAssignments", key = "#roleId")
    public List<UserRole> getRoleAssignments(Long roleId, Long organizationId) {
        // Validate role exists in organization
        roleRepository.findByIdAndOrganizationId(roleId, organizationId)
            .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleId));

        return userRoleRepository.findActiveByRoleId(roleId);
    }

    /**
     * Check if a user has an active role assignment.
     */
    public boolean hasActiveRoleAssignment(Long userId, Long roleId, Long organizationId) {
        return userRoleRepository.hasActiveAssignment(userId, roleId);
    }

    /**
     * Get role assignments expiring within specified days.
     */
    public List<UserRole> getExpiringAssignments(Long organizationId, int daysAhead) {
        LocalDateTime threshold = LocalDateTime.now().plusDays(daysAhead);
        return userRoleRepository.findExpiringWithinDays(threshold).stream()
            .filter(ur -> {
                // Filter by organization through role
                Optional<Role> role = roleRepository.findById(ur.getRoleId());
                return role.isPresent() && role.get().getOrganizationId().equals(organizationId);
            })
            .toList();
    }

    /**
     * Extend the expiration of a role assignment.
     */
    @Transactional
    @CacheEvict(value = {"userRoles", "userOrganizationRoles"}, key = "#userId")
    public void extendRoleAssignment(Long userId, Long roleId, Long organizationId,
                                   LocalDateTime newExpirationDate, Long updatedBy) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
            .orElseThrow(() -> new RoleAssignmentNotFoundException(
                "No role assignment found for user " + userId + " and role " + roleId));

        if (!userRole.isActive()) {
            throw new RoleAssignmentException("Cannot extend inactive role assignment");
        }

        if (newExpirationDate != null && newExpirationDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New expiration date cannot be in the past");
        }

        userRole.setExpiresAt(newExpirationDate);
        userRoleRepository.save(userRole);

        // Could publish role extended event here if needed
    }

    /**
     * Get role assignment statistics for an organization.
     */
    public UserRoleStatistics getUserRoleStatistics(Long organizationId) {
        List<UserRole> allAssignments = userRoleRepository.findActiveByOrganizationId(organizationId);

        long activeAssignments = allAssignments.size();
        long temporaryAssignments = allAssignments.stream()
            .mapToLong(ur -> ur.isTemporary() ? 1 : 0)
            .sum();

        long expiringWithinWeek = getExpiringAssignments(organizationId, 7).size();

        // This would be optimized with proper queries
        long expiredAssignments = userRoleRepository.findExpiredAssignments().stream()
            .filter(ur -> {
                Optional<Role> role = roleRepository.findById(ur.getRoleId());
                return role.isPresent() && role.get().getOrganizationId().equals(organizationId);
            })
            .count();

        return new UserRoleStatistics(
            activeAssignments + expiredAssignments,
            activeAssignments,
            temporaryAssignments,
            expiringWithinWeek,
            expiredAssignments
        );
    }

    /**
     * Process expired role assignments (typically called by scheduled job).
     */
    @Transactional
    public void processExpiredAssignments() {
        List<UserRole> expiredAssignments = userRoleRepository.findExpiredAssignments();

        for (UserRole userRole : expiredAssignments) {
            userRole.markAsRemoved(-1L); // System removal
            userRoleRepository.save(userRole);

            // Get role information for events
            Optional<Role> roleOpt = roleRepository.findById(userRole.getRoleId());
            if (roleOpt.isPresent()) {
                Role role = roleOpt.get();
                publishRoleRemovedEvent(userRole, role, role.getOrganizationId(),
                    UserRoleRemovedEvent.RemovalReason.EXPIRED);
            }
        }
    }

    // Validation methods

    private void validateAssignRoleRequest(AssignRoleRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request.getRoleId() == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        if (request.getOrganizationId() == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        if (request.getAssignedBy() == null) {
            throw new IllegalArgumentException("Assigned by cannot be null");
        }
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expiration date cannot be in the past");
        }
    }

    private void validateRoleAssignmentLimits(Long userId, Long organizationId) {
        long currentAssignments = userRoleRepository.countActiveByUserId(userId);
        if (currentAssignments >= 10) { // Configurable limit
            throw new RoleAssignmentLimitExceededException(
                "User has reached maximum number of role assignments: " + currentAssignments);
        }
    }

    // Event publishing methods

    private void publishRoleAssignedEvent(UserRole userRole, Role role, Long organizationId) {
        String correlationId = UUID.randomUUID().toString();

        UserRoleAssignedEvent event = userRole.isTemporary() ?
            UserRoleAssignedEvent.forTemporaryAssignment(
                userRole.getUserId(), userRole.getRoleId(), role.getName(),
                organizationId, "Organization", // Would get actual org name
                userRole.getAssignedBy(), userRole.getExpiresAt(), correlationId
            ) :
            UserRoleAssignedEvent.forPermanentAssignment(
                userRole.getUserId(), userRole.getRoleId(), role.getName(),
                organizationId, "Organization", // Would get actual org name
                userRole.getAssignedBy(), correlationId
            );

        eventPublisher.publishEvent(event);
    }

    private void publishRoleRemovedEvent(UserRole userRole, Role role, Long organizationId,
                                       UserRoleRemovedEvent.RemovalReason reason) {
        String correlationId = UUID.randomUUID().toString();

        UserRoleRemovedEvent event = switch (reason) {
            case EXPIRED -> UserRoleRemovedEvent.forExpiration(
                userRole.getUserId(), userRole.getRoleId(), role.getName(),
                organizationId, "Organization", correlationId
            );
            case ROLE_DELETED -> UserRoleRemovedEvent.forRoleDeletion(
                userRole.getUserId(), userRole.getRoleId(), role.getName(),
                organizationId, "Organization", userRole.getRemovedBy(), correlationId
            );
            default -> UserRoleRemovedEvent.forManualRemoval(
                userRole.getUserId(), userRole.getRoleId(), role.getName(),
                organizationId, "Organization", userRole.getRemovedBy(), correlationId
            );
        };

        eventPublisher.publishEvent(event);
    }

    // Data classes

    public static class AssignRoleRequest {
        private final Long userId;
        private final Long roleId;
        private final Long organizationId;
        private final Long assignedBy;
        private final LocalDateTime expiresAt;

        public AssignRoleRequest(Long userId, Long roleId, Long organizationId,
                               Long assignedBy, LocalDateTime expiresAt) {
            this.userId = userId;
            this.roleId = roleId;
            this.organizationId = organizationId;
            this.assignedBy = assignedBy;
            this.expiresAt = expiresAt;
        }

        // Getters
        public Long getUserId() { return userId; }
        public Long getRoleId() { return roleId; }
        public Long getOrganizationId() { return organizationId; }
        public Long getAssignedBy() { return assignedBy; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }

    public record UserRoleStatistics(
        long totalAssignments,
        long activeAssignments,
        long temporaryAssignments,
        long expiringWithinWeek,
        long expiredAssignments
    ) {}

    // Exception classes
    public static class RoleAssignmentException extends RuntimeException {
        public RoleAssignmentException(String message) { super(message); }
    }

    public static class RoleAssignmentNotFoundException extends RoleAssignmentException {
        public RoleAssignmentNotFoundException(String message) { super(message); }
    }

    public static class DuplicateRoleAssignmentException extends RoleAssignmentException {
        public DuplicateRoleAssignmentException(String message) { super(message); }
    }

    public static class RoleAssignmentLimitExceededException extends RoleAssignmentException {
        public RoleAssignmentLimitExceededException(String message) { super(message); }
    }

    public static class RoleNotFoundException extends RuntimeException {
        public RoleNotFoundException(String message) { super(message); }
    }
}