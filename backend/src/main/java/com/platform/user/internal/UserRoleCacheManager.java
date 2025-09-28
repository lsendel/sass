package com.platform.user.internal;

import com.platform.user.events.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Cache management component for user role-related caches.
 * Listens to user role events and invalidates relevant cache entries to maintain consistency.
 *
 * Following Spring Modulith event-driven architecture for cache coherence.
 */
@Component
class UserRoleCacheManager {

    private final CacheManager cacheManager;

    @Autowired
    public UserRoleCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Handle user role assigned events by invalidating user-specific caches.
     */
    @EventListener
    @Async("eventExecutor")
    public void handleUserRoleAssigned(UserRoleAssignedEvent event) {
        invalidateUserCaches(event.userId(), event.organizationId());
        invalidateRoleCaches(event.roleId());

        // If it's a high-privilege role, we might want to invalidate broader caches
        if (event.isHighPrivilegeRole()) {
            invalidateOrganizationCaches(event.organizationId());
        }
    }

    /**
     * Handle user role removed events by invalidating user-specific caches.
     */
    @EventListener
    @Async("eventExecutor")
    public void handleUserRoleRemoved(UserRoleRemovedEvent event) {
        invalidateUserCaches(event.userId(), event.organizationId());
        invalidateRoleCaches(event.roleId());

        // If it's a high-privilege role removal, invalidate broader caches
        if (event.isHighPrivilegeRole()) {
            invalidateOrganizationCaches(event.organizationId());
        }
    }

    /**
     * Handle role created events by invalidating organization role caches.
     */
    @EventListener
    @Async("eventExecutor")
    public void handleRoleCreated(RoleCreatedEvent event) {
        invalidateOrganizationRoleCaches(event.organizationId());

        // If it has high-privilege permissions, invalidate broader caches
        if (event.hasHighPrivilegePermissions()) {
            invalidateOrganizationCaches(event.organizationId());
        }
    }

    /**
     * Handle role modified events by invalidating role and user permission caches.
     */
    @EventListener
    @Async("eventExecutor")
    public void handleRoleModified(RoleModifiedEvent event) {
        invalidateRoleCaches(event.roleId());
        invalidateOrganizationRoleCaches(event.organizationId());

        // If permissions changed, invalidate user permission caches for affected users
        if (event.hasPermissionChanges()) {
            invalidateUsersWithRole(event.roleId(), event.organizationId());
        }

        // If high-privilege permissions were changed, invalidate broader caches
        if (event.hasHighPrivilegePermissionsAdded() || event.hasHighPrivilegePermissionsRemoved()) {
            invalidateOrganizationCaches(event.organizationId());
        }
    }

    /**
     * Handle role deleted events by invalidating all related caches.
     */
    @EventListener
    @Async("eventExecutor")
    public void handleRoleDeleted(RoleDeletedEvent event) {
        invalidateRoleCaches(event.roleId());
        invalidateOrganizationRoleCaches(event.organizationId());

        // Invalidate caches for all affected users
        if (event.affectedUserCount() > 0) {
            invalidateUsersWithRole(event.roleId(), event.organizationId());
        }

        // If it had high-privilege permissions, invalidate broader caches
        if (event.hadHighPrivilegePermissions()) {
            invalidateOrganizationCaches(event.organizationId());
        }
    }

    /**
     * Invalidate all user-specific caches.
     */
    private void invalidateUserCaches(Long userId, Long organizationId) {
        // User roles caches
        evictCacheEntry("userRoles", userId.toString());
        evictCacheEntry("userOrganizationRoles", userId + ":" + organizationId);

        // User permissions caches
        evictCacheEntry("userPermissions", userId + ":" + organizationId);
        evictCacheEntry("userPermissionKeys", userId + ":" + organizationId);
        evictCacheEntry("userPermissionCheck", userId + ":" + organizationId + ":*");
    }

    /**
     * Invalidate role-specific caches.
     */
    private void invalidateRoleCaches(Long roleId) {
        evictCacheEntry("rolePermissions", roleId.toString());
        evictCacheEntry("roleAssignments", roleId.toString());
    }

    /**
     * Invalidate organization role caches.
     */
    private void invalidateOrganizationRoleCaches(Long organizationId) {
        evictCacheEntry("organizationRoles", organizationId.toString());
        evictCacheEntry("customRoles", organizationId.toString());
        evictCacheEntry("predefinedRoles", organizationId.toString());
    }

    /**
     * Invalidate broader organization caches.
     */
    private void invalidateOrganizationCaches(Long organizationId) {
        invalidateOrganizationRoleCaches(organizationId);

        // Could invalidate other organization-wide caches here
        // This is where we might invalidate session caches, navigation caches, etc.
    }

    /**
     * Invalidate caches for all users who have a specific role.
     * This is more expensive but necessary when role permissions change.
     */
    private void invalidateUsersWithRole(Long roleId, Long organizationId) {
        // In a production system, we would:
        // 1. Query for all users with this role
        // 2. Invalidate their individual caches
        // 3. Or use cache tags/patterns for more efficient invalidation

        // For now, we'll invalidate organization-wide user caches
        // This is less efficient but ensures consistency

        // Pattern-based cache eviction (if supported by cache provider)
        evictCachePattern("userPermissions", "*:" + organizationId);
        evictCachePattern("userPermissionKeys", "*:" + organizationId);
        evictCachePattern("userOrganizationRoles", "*:" + organizationId);
    }

    /**
     * Evict a specific cache entry.
     */
    private void evictCacheEntry(String cacheName, String key) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
            }
        } catch (Exception e) {
            // Log error but don't fail the event processing
            // In production, you'd want proper logging here
            System.err.println("Failed to evict cache entry: " + cacheName + ":" + key + " - " + e.getMessage());
        }
    }

    /**
     * Evict cache entries matching a pattern.
     * Note: This is a simplified implementation. In production, you'd use
     * cache-specific pattern matching or cache tags.
     */
    private void evictCachePattern(String cacheName, String pattern) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // This is a simplified approach
                // In production, you'd implement proper pattern matching
                // based on your cache provider (Redis, Caffeine, etc.)

                // For now, we'll clear the entire cache for patterns
                // This is less efficient but ensures consistency
                if (pattern.contains("*")) {
                    cache.clear();
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the event processing
            System.err.println("Failed to evict cache pattern: " + cacheName + ":" + pattern + " - " + e.getMessage());
        }
    }
}