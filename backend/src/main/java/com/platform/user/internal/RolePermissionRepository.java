package com.platform.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RolePermission entity operations.
 * Manages the assignment of permissions to roles.
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * Find all permission assignments for a specific role.
     * Used for role permission management and display.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.roleId = :roleId ORDER BY rp.createdAt")
    List<RolePermission> findByRoleId(@Param("roleId") Long roleId);

    /**
     * Find all role assignments for a specific permission.
     * Used for permission usage analysis.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.permissionId = :permissionId ORDER BY rp.createdAt")
    List<RolePermission> findByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * Find a specific role-permission assignment.
     * Used for assignment validation and management.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.roleId = :roleId AND rp.permissionId = :permissionId")
    Optional<RolePermission> findByRoleIdAndPermissionId(@Param("roleId") Long roleId,
                                                         @Param("permissionId") Long permissionId);

    /**
     * Check if a role has a specific permission assigned.
     * Used for permission validation and duplicate assignment prevention.
     */
    @Query("SELECT COUNT(rp) > 0 FROM RolePermission rp WHERE rp.roleId = :roleId AND rp.permissionId = :permissionId")
    boolean existsByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * Find permission assignments for multiple roles.
     * Used for bulk permission operations and aggregation.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.roleId IN :roleIds ORDER BY rp.roleId, rp.createdAt")
    List<RolePermission> findByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * Find role assignments for multiple permissions.
     * Used for permission impact analysis.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.permissionId IN :permissionIds ORDER BY rp.permissionId, rp.createdAt")
    List<RolePermission> findByPermissionIds(@Param("permissionIds") List<Long> permissionIds);

    /**
     * Count permissions assigned to a role.
     * Used for role complexity metrics and validation.
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp WHERE rp.roleId = :roleId")
    long countByRoleId(@Param("roleId") Long roleId);

    /**
     * Count roles that have a specific permission.
     * Used for permission usage metrics.
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp WHERE rp.permissionId = :permissionId")
    long countByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * Find all permission assignments for roles in a specific organization.
     * Uses JOIN with roles table to filter by organization.
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "JOIN Role r ON rp.roleId = r.id " +
           "WHERE r.organizationId = :organizationId " +
           "ORDER BY rp.roleId, rp.createdAt")
    List<RolePermission> findByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Find roles in an organization that have a specific permission.
     * Used for permission-based role discovery within organizations.
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "JOIN Role r ON rp.roleId = r.id " +
           "WHERE r.organizationId = :organizationId AND rp.permissionId = :permissionId " +
           "ORDER BY r.name")
    List<RolePermission> findByOrganizationIdAndPermissionId(@Param("organizationId") Long organizationId,
                                                             @Param("permissionId") Long permissionId);

    /**
     * Find permission assignments for custom roles only.
     * Used for custom role management.
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "JOIN Role r ON rp.roleId = r.id " +
           "WHERE r.roleType = 'CUSTOM' " +
           "ORDER BY r.name, rp.createdAt")
    List<RolePermission> findByCustomRoles();

    /**
     * Find permission assignments for predefined roles only.
     * Used for system role analysis.
     */
    @Query("SELECT rp FROM RolePermission rp " +
           "JOIN Role r ON rp.roleId = r.id " +
           "WHERE r.roleType = 'PREDEFINED' " +
           "ORDER BY r.name, rp.createdAt")
    List<RolePermission> findByPredefinedRoles();

    /**
     * Delete all permission assignments for a role.
     * Used for role cleanup and modification.
     */
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * Delete a specific role-permission assignment.
     * Used for permission removal from roles.
     */
    @Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId AND rp.permissionId = :permissionId")
    void deleteByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * Find the most recently assigned permissions for a role.
     * Used for recent changes tracking.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.roleId = :roleId ORDER BY rp.createdAt DESC")
    List<RolePermission> findRecentByRoleId(@Param("roleId") Long roleId);
}