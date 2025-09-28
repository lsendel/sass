package com.platform.user.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public API interface for user role assignment operations in the User module.
 * This interface defines the contract for external modules to manage user role assignments.
 *
 * Following Spring Modulith principles, this API is the only way external modules
 * should access user role assignment functionality.
 */
public interface UserRoleApi {

    /**
     * Assign a role to a user in an organization.
     * Creates a new role assignment with optional expiration.
     *
     * @param userId the user ID to assign the role to
     * @param roleId the role ID to assign
     * @param organizationId the organization context
     * @param assignedBy the user ID performing the assignment
     * @param expiresAt optional expiration date for temporary assignments
     * @return the created user role assignment information
     */
    UserRoleAssignment assignRoleToUser(Long userId, Long roleId, Long organizationId,
                                       Long assignedBy, LocalDateTime expiresAt);

    /**
     * Remove a role assignment from a user.
     * Marks the assignment as removed with audit information.
     *
     * @param userId the user ID
     * @param roleId the role ID to remove
     * @param organizationId the organization context
     * @param removedBy the user ID performing the removal
     */
    void removeRoleFromUser(Long userId, Long roleId, Long organizationId, Long removedBy);

    /**
     * Get all active role assignments for a user in an organization.
     * Excludes expired and removed assignments.
     *
     * @param userId the user ID
     * @param organizationId the organization context
     * @return list of active role assignments
     */
    List<UserRoleAssignment> getUserRoleAssignments(Long userId, Long organizationId);

    /**
     * Get all users assigned to a specific role.
     * Used for role management and user listing.
     *
     * @param roleId the role ID
     * @param organizationId the organization context
     * @return list of user role assignments for the role
     */
    List<UserRoleAssignment> getRoleAssignments(Long roleId, Long organizationId);

    /**
     * Check if a user has an active role assignment.
     * Used for quick role membership validation.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @param organizationId the organization context
     * @return true if user has active assignment, false otherwise
     */
    boolean hasActiveRoleAssignment(Long userId, Long roleId, Long organizationId);

    /**
     * Get role assignments that are expiring within a specified number of days.
     * Used for proactive role expiration management and notifications.
     *
     * @param organizationId the organization context
     * @param daysAhead number of days to look ahead for expiring assignments
     * @return list of assignments expiring within the timeframe
     */
    List<UserRoleAssignment> getExpiringAssignments(Long organizationId, int daysAhead);

    /**
     * Extend the expiration of a role assignment.
     * Updates the expiration date for temporary role assignments.
     *
     * @param userId the user ID
     * @param roleId the role ID
     * @param organizationId the organization context
     * @param newExpirationDate the new expiration date
     * @param updatedBy the user ID performing the update
     */
    void extendRoleAssignment(Long userId, Long roleId, Long organizationId,
                             LocalDateTime newExpirationDate, Long updatedBy);

    /**
     * Get role assignment statistics for an organization.
     * Used for organizational metrics and reporting.
     *
     * @param organizationId the organization ID
     * @return role assignment statistics
     */
    UserRoleStatistics getUserRoleStatistics(Long organizationId);

    /**
     * Data class representing a user role assignment for external consumption.
     */
    record UserRoleAssignment(
        Long id,
        Long userId,
        Long roleId,
        String roleName,
        Instant assignedAt,
        Long assignedBy,
        LocalDateTime expiresAt,
        boolean isActive,
        boolean isExpired,
        boolean isTemporary
    ) {}

    /**
     * Data class representing user role assignment statistics.
     */
    record UserRoleStatistics(
        long totalAssignments,
        long activeAssignments,
        long temporaryAssignments,
        long expiringWithinWeek,
        long expiredAssignments
    ) {}

    /**
     * Exception thrown when role assignment operations fail.
     */
    class RoleAssignmentException extends RuntimeException {
        public RoleAssignmentException(String message) {
            super(message);
        }

        public RoleAssignmentException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when attempting to assign a role that's already assigned.
     */
    class DuplicateRoleAssignmentException extends RoleAssignmentException {
        public DuplicateRoleAssignmentException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when role assignment limits are exceeded.
     */
    class RoleAssignmentLimitExceededException extends RoleAssignmentException {
        public RoleAssignmentLimitExceededException(String message) {
            super(message);
        }
    }
}