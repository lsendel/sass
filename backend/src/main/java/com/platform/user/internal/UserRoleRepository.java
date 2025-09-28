package com.platform.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserRole entity operations.
 * Manages user-role assignments including expiration and removal tracking.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * Find all active role assignments for a user.
     * Excludes removed and expired assignments.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP) " +
           "ORDER BY ur.assignedAt")
    List<UserRole> findActiveByUserId(@Param("userId") Long userId);

    /**
     * Find all role assignments for a user (including inactive ones).
     * Used for audit and history tracking.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId ORDER BY ur.assignedAt DESC")
    List<UserRole> findByUserId(@Param("userId") Long userId);

    /**
     * Find all active user assignments for a specific role.
     * Used for role management and user listing.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.roleId = :roleId " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP) " +
           "ORDER BY ur.assignedAt")
    List<UserRole> findActiveByRoleId(@Param("roleId") Long roleId);

    /**
     * Find a specific user-role assignment.
     * Used for assignment validation and management.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.roleId = :roleId " +
           "AND ur.removedAt IS NULL " +
           "ORDER BY ur.assignedAt DESC")
    Optional<UserRole> findByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * Find all expired role assignments that haven't been cleaned up.
     * Used for expired role cleanup processes.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt IS NOT NULL " +
           "AND ur.expiresAt <= CURRENT_TIMESTAMP " +
           "AND ur.removedAt IS NULL " +
           "ORDER BY ur.expiresAt")
    List<UserRole> findExpiredAssignments();

    /**
     * Find role assignments expiring within a specific timeframe.
     * Used for expiration notifications and warnings.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt IS NOT NULL " +
           "AND ur.expiresAt BETWEEN CURRENT_TIMESTAMP AND :expiryThreshold " +
           "AND ur.removedAt IS NULL " +
           "ORDER BY ur.expiresAt")
    List<UserRole> findExpiringAssignments(@Param("expiryThreshold") LocalDateTime expiryThreshold);

    /**
     * Check if a user has an active assignment to a specific role.
     * Used for permission validation and duplicate assignment prevention.
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.userId = :userId AND ur.roleId = :roleId " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    boolean hasActiveAssignment(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * Find all assignments for roles in a specific organization.
     * Uses JOIN with roles table to filter by organization.
     */
    @Query("SELECT ur FROM UserRole ur " +
           "JOIN Role r ON ur.roleId = r.id " +
           "WHERE r.organizationId = :organizationId " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP) " +
           "ORDER BY ur.assignedAt")
    List<UserRole> findActiveByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Find all active role assignments for a user within a specific organization.
     * Combines user and organization filters.
     */
    @Query("SELECT ur FROM UserRole ur " +
           "JOIN Role r ON ur.roleId = r.id " +
           "WHERE ur.userId = :userId AND r.organizationId = :organizationId " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP) " +
           "ORDER BY ur.assignedAt")
    List<UserRole> findActiveByUserIdAndOrganizationId(@Param("userId") Long userId,
                                                       @Param("organizationId") Long organizationId);

    /**
     * Count active role assignments for a user.
     * Used for role limit enforcement and metrics.
     */
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.userId = :userId " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    long countActiveByUserId(@Param("userId") Long userId);

    /**
     * Count active user assignments for a role.
     * Used for role usage metrics.
     */
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.roleId = :roleId " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    long countActiveByRoleId(@Param("roleId") Long roleId);

    /**
     * Find assignments assigned by a specific user.
     * Used for audit and management tracking.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.assignedBy = :assignedBy " +
           "ORDER BY ur.assignedAt DESC")
    List<UserRole> findByAssignedBy(@Param("assignedBy") Long assignedBy);

    /**
     * Find assignments that will expire within a specific number of days.
     * Used for proactive expiration management.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt IS NOT NULL " +
           "AND ur.expiresAt BETWEEN CURRENT_TIMESTAMP AND :days " +
           "AND ur.removedAt IS NULL " +
           "ORDER BY ur.expiresAt")
    List<UserRole> findExpiringWithinDays(@Param("days") LocalDateTime days);

    /**
     * Find temporary (expiring) role assignments for a user.
     * Used for temporary access management.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId " +
           "AND ur.expiresAt IS NOT NULL " +
           "AND ur.removedAt IS NULL " +
           "ORDER BY ur.expiresAt")
    List<UserRole> findTemporaryByUserId(@Param("userId") Long userId);

    /**
     * Find permanent (non-expiring) role assignments for a user.
     * Used for permanent access management.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId " +
           "AND ur.expiresAt IS NULL " +
           "AND ur.removedAt IS NULL " +
           "ORDER BY ur.assignedAt")
    List<UserRole> findPermanentByUserId(@Param("userId") Long userId);
}