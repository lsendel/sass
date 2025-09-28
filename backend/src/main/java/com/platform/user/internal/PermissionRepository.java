package com.platform.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Permission entity operations.
 * Provides access to system permissions and supports permission lookups for RBAC.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Find all active permissions in the system.
     * Used for permission management and role creation.
     */
    @Query("SELECT p FROM Permission p WHERE p.isActive = true ORDER BY p.resource, p.action")
    List<Permission> findAllActive();

    /**
     * Find a specific permission by resource and action.
     * Used for permission validation and lookup.
     */
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.action = :action AND p.isActive = true")
    Optional<Permission> findByResourceAndAction(@Param("resource") String resource, @Param("action") String action);

    /**
     * Find all permissions for a specific resource.
     * Used for resource-based permission management.
     */
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.isActive = true ORDER BY p.action")
    List<Permission> findByResource(@Param("resource") String resource);

    /**
     * Find all permissions assigned to a specific role.
     * Uses JOIN with role_permissions table.
     */
    @Query("SELECT p FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permissionId " +
           "WHERE rp.roleId = :roleId AND p.isActive = true " +
           "ORDER BY p.resource, p.action")
    List<Permission> findByRoleId(@Param("roleId") Long roleId);

    /**
     * Find all permissions for a user in a specific organization.
     * This is the core permission lookup query that uses the database function
     * created in migration V015 for optimal performance.
     */
    @Query(value = "SELECT p.* FROM permissions p " +
                   "WHERE (p.resource, p.action) IN (" +
                   "  SELECT resource, action FROM get_user_permissions(:userId, :organizationId)" +
                   ") AND p.is_active = true " +
                   "ORDER BY p.resource, p.action",
           nativeQuery = true)
    List<Permission> findByUserId(@Param("userId") Long userId, @Param("organizationId") Long organizationId);

    /**
     * Check if a user has a specific permission in an organization.
     * Optimized single permission check using the database function.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM get_user_permissions(:userId, :organizationId) " +
                   "WHERE resource = :resource AND action = :action",
           nativeQuery = true)
    boolean hasUserPermission(@Param("userId") Long userId,
                             @Param("organizationId") Long organizationId,
                             @Param("resource") String resource,
                             @Param("action") String action);

    /**
     * Find permissions by multiple IDs.
     * Used for bulk permission operations in role management.
     */
    @Query("SELECT p FROM Permission p WHERE p.id IN :permissionIds AND p.isActive = true")
    List<Permission> findByIds(@Param("permissionIds") List<Long> permissionIds);

    /**
     * Find all unique resources in the system.
     * Used for permission management interfaces.
     */
    @Query("SELECT DISTINCT p.resource FROM Permission p WHERE p.isActive = true ORDER BY p.resource")
    List<String> findAllResources();

    /**
     * Find all actions for a specific resource.
     * Used for permission management interfaces.
     */
    @Query("SELECT DISTINCT p.action FROM Permission p WHERE p.resource = :resource AND p.isActive = true ORDER BY p.action")
    List<String> findActionsByResource(@Param("resource") String resource);

    /**
     * Count total active permissions.
     * Used for system metrics and validation.
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.isActive = true")
    long countActive();
}