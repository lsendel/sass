package com.platform.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Role entity operations.
 * Provides access to organization-scoped roles and supports role management operations.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find all active roles for a specific organization.
     * Returns both predefined and custom roles.
     */
    @Query("SELECT r FROM Role r WHERE r.organizationId = :organizationId AND r.isActive = true ORDER BY r.roleType, r.name")
    List<Role> findByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Find a specific role by organization, name, and type.
     * Used for role lookup and validation.
     */
    @Query("SELECT r FROM Role r WHERE r.organizationId = :organizationId AND r.name = :name AND r.roleType = :roleType AND r.isActive = true")
    Optional<Role> findByOrganizationIdAndNameAndRoleType(@Param("organizationId") Long organizationId,
                                                          @Param("name") String name,
                                                          @Param("roleType") RoleType roleType);

    /**
     * Find all predefined roles for an organization.
     * Used for system role validation and management.
     */
    @Query("SELECT r FROM Role r WHERE r.organizationId = :organizationId AND r.roleType = 'PREDEFINED' AND r.isActive = true ORDER BY r.name")
    List<Role> findPredefinedRolesByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Find all custom roles for an organization.
     * Used for custom role management.
     */
    @Query("SELECT r FROM Role r WHERE r.organizationId = :organizationId AND r.roleType = 'CUSTOM' AND r.isActive = true ORDER BY r.name")
    List<Role> findCustomRolesByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Find all roles assigned to a specific user.
     * Uses JOIN with user_roles table and filters out expired/removed assignments.
     */
    @Query("SELECT r FROM Role r " +
           "JOIN UserRole ur ON r.id = ur.roleId " +
           "WHERE ur.userId = :userId AND r.isActive = true " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP) " +
           "ORDER BY r.name")
    List<Role> findByUserId(@Param("userId") Long userId);

    /**
     * Find all roles assigned to a user in a specific organization.
     * Uses JOIN with user_roles table and includes organization filter.
     */
    @Query("SELECT r FROM Role r " +
           "JOIN UserRole ur ON r.id = ur.roleId " +
           "WHERE ur.userId = :userId AND r.organizationId = :organizationId AND r.isActive = true " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP) " +
           "ORDER BY r.name")
    List<Role> findByUserIdAndOrganizationId(@Param("userId") Long userId, @Param("organizationId") Long organizationId);

    /**
     * Check if a role name exists in an organization (for uniqueness validation).
     * Includes both active and inactive roles to prevent name reuse.
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.organizationId = :organizationId AND r.name = :name")
    boolean existsByOrganizationIdAndName(@Param("organizationId") Long organizationId, @Param("name") String name);

    /**
     * Check if a role name exists in an organization excluding a specific role ID.
     * Used for role name uniqueness validation during updates.
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.organizationId = :organizationId AND r.name = :name AND r.id != :excludeId")
    boolean existsByOrganizationIdAndNameExcluding(@Param("organizationId") Long organizationId,
                                                   @Param("name") String name,
                                                   @Param("excludeId") Long excludeId);

    /**
     * Count custom roles for an organization.
     * Used for role limit enforcement.
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.organizationId = :organizationId AND r.roleType = 'CUSTOM' AND r.isActive = true")
    long countByOrganizationIdAndRoleType(@Param("organizationId") Long organizationId, @Param("roleType") RoleType roleType);

    /**
     * Find roles created by a specific user.
     * Used for audit and management purposes.
     */
    @Query("SELECT r FROM Role r WHERE r.createdBy = :userId AND r.isActive = true ORDER BY r.createdAt DESC")
    List<Role> findByCreatedBy(@Param("userId") Long userId);

    /**
     * Find a role by ID and organization (for authorization checks).
     * Ensures users can only access roles within their organization.
     */
    @Query("SELECT r FROM Role r WHERE r.id = :roleId AND r.organizationId = :organizationId AND r.isActive = true")
    Optional<Role> findByIdAndOrganizationId(@Param("roleId") Long roleId, @Param("organizationId") Long organizationId);

    /**
     * Find roles with specific permissions.
     * Used for permission-based role discovery.
     */
    @Query("SELECT DISTINCT r FROM Role r " +
           "JOIN RolePermission rp ON r.id = rp.roleId " +
           "JOIN Permission p ON rp.permissionId = p.id " +
           "WHERE r.organizationId = :organizationId AND r.isActive = true " +
           "AND p.resource = :resource AND p.action = :action AND p.isActive = true " +
           "ORDER BY r.name")
    List<Role> findByOrganizationIdAndPermission(@Param("organizationId") Long organizationId,
                                                 @Param("resource") String resource,
                                                 @Param("action") String action);

    /**
     * Count users assigned to a role.
     * Used for role usage metrics and deletion validation.
     */
    @Query("SELECT COUNT(DISTINCT ur.userId) FROM UserRole ur " +
           "WHERE ur.roleId = :roleId " +
           "AND ur.removedAt IS NULL " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)")
    long countAssignedUsers(@Param("roleId") Long roleId);
}