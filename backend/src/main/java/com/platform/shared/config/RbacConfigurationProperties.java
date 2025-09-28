package com.platform.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RBAC (Role-Based Access Control) configuration properties for the platform.
 * Contains limits, caching, and performance settings for role and permission management.
 *
 * Configuration prefix: platform.rbac
 *
 * Example application.yml:
 * <pre>
 * platform:
 *   rbac:
 *     limits:
 *       maxRolesPerOrganization: 20
 *       maxRolesPerUser: 8
 *       maxPermissionsPerRole: 150
 *     cache:
 *       permissionTtlMinutes: 15
 *       batchSize: 50
 *     performance:
 *       enableDatabaseFunctions: true
 *       enableQueryOptimizations: true
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "platform.rbac")
public class RbacConfigurationProperties {

    /**
     * Limits configuration for roles, users, and permissions
     */
    private Limits limits = new Limits();

    /**
     * Cache configuration for permission data
     */
    private Cache cache = new Cache();

    /**
     * Performance optimization settings
     */
    private Performance performance = new Performance();

    /**
     * Audit configuration for RBAC operations
     */
    private Audit audit = new Audit();

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Performance getPerformance() {
        return performance;
    }

    public void setPerformance(Performance performance) {
        this.performance = performance;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    /**
     * Limits configuration for RBAC entities
     */
    public static class Limits {
        /**
         * Maximum number of custom roles allowed per organization
         * (Predefined roles don't count towards this limit)
         */
        private int maxRolesPerOrganization = 10;

        /**
         * Maximum number of roles a single user can have within one organization
         */
        private int maxRolesPerUser = 5;

        /**
         * Maximum number of permissions that can be assigned to a single role
         */
        private int maxPermissionsPerRole = 100;

        /**
         * Maximum number of organizations a user can belong to
         */
        private int maxOrganizationsPerUser = 10;

        /**
         * Enable strict validation of role limits
         */
        private boolean enableStrictValidation = true;

        public int getMaxRolesPerOrganization() {
            return maxRolesPerOrganization;
        }

        public void setMaxRolesPerOrganization(int maxRolesPerOrganization) {
            this.maxRolesPerOrganization = maxRolesPerOrganization;
        }

        public int getMaxRolesPerUser() {
            return maxRolesPerUser;
        }

        public void setMaxRolesPerUser(int maxRolesPerUser) {
            this.maxRolesPerUser = maxRolesPerUser;
        }

        public int getMaxPermissionsPerRole() {
            return maxPermissionsPerRole;
        }

        public void setMaxPermissionsPerRole(int maxPermissionsPerRole) {
            this.maxPermissionsPerRole = maxPermissionsPerRole;
        }

        public int getMaxOrganizationsPerUser() {
            return maxOrganizationsPerUser;
        }

        public void setMaxOrganizationsPerUser(int maxOrganizationsPerUser) {
            this.maxOrganizationsPerUser = maxOrganizationsPerUser;
        }

        public boolean isEnableStrictValidation() {
            return enableStrictValidation;
        }

        public void setEnableStrictValidation(boolean enableStrictValidation) {
            this.enableStrictValidation = enableStrictValidation;
        }
    }

    /**
     * Cache configuration for permission data
     */
    public static class Cache {
        /**
         * Time-to-live for permission cache entries in minutes
         */
        private int permissionTtlMinutes = 15;

        /**
         * Maximum number of permission cache entries per organization
         */
        private int maxEntriesPerOrganization = 1000;

        /**
         * Batch size for permission loading operations
         */
        private int batchSize = 50;

        /**
         * Enable Redis-based distributed caching
         */
        private boolean enableDistributedCache = true;

        /**
         * Enable in-memory local cache for hot permissions
         */
        private boolean enableLocalCache = true;

        /**
         * Local cache size (number of entries)
         */
        private int localCacheSize = 500;

        /**
         * Cache key prefix for Redis keys
         */
        private String keyPrefix = "rbac:permissions";

        /**
         * Enable cache statistics collection
         */
        private boolean enableStatistics = true;

        public int getPermissionTtlMinutes() {
            return permissionTtlMinutes;
        }

        public void setPermissionTtlMinutes(int permissionTtlMinutes) {
            this.permissionTtlMinutes = permissionTtlMinutes;
        }

        public int getMaxEntriesPerOrganization() {
            return maxEntriesPerOrganization;
        }

        public void setMaxEntriesPerOrganization(int maxEntriesPerOrganization) {
            this.maxEntriesPerOrganization = maxEntriesPerOrganization;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public boolean isEnableDistributedCache() {
            return enableDistributedCache;
        }

        public void setEnableDistributedCache(boolean enableDistributedCache) {
            this.enableDistributedCache = enableDistributedCache;
        }

        public boolean isEnableLocalCache() {
            return enableLocalCache;
        }

        public void setEnableLocalCache(boolean enableLocalCache) {
            this.enableLocalCache = enableLocalCache;
        }

        public int getLocalCacheSize() {
            return localCacheSize;
        }

        public void setLocalCacheSize(int localCacheSize) {
            this.localCacheSize = localCacheSize;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public boolean isEnableStatistics() {
            return enableStatistics;
        }

        public void setEnableStatistics(boolean enableStatistics) {
            this.enableStatistics = enableStatistics;
        }
    }

    /**
     * Performance optimization settings
     */
    public static class Performance {
        /**
         * Enable database stored functions for complex permission queries
         */
        private boolean enableDatabaseFunctions = true;

        /**
         * Enable query optimization with composite indexes
         */
        private boolean enableQueryOptimizations = true;

        /**
         * Maximum query timeout for permission checks in milliseconds
         */
        private int queryTimeoutMs = 200;

        /**
         * Enable async processing for non-critical permission operations
         */
        private boolean enableAsyncProcessing = true;

        /**
         * Thread pool size for async permission processing
         */
        private int asyncThreadPoolSize = 5;

        /**
         * Enable permission denormalization for ultra-fast lookups
         */
        private boolean enableDenormalization = false;

        /**
         * Batch size for bulk permission operations
         */
        private int bulkOperationBatchSize = 100;

        public boolean isEnableDatabaseFunctions() {
            return enableDatabaseFunctions;
        }

        public void setEnableDatabaseFunctions(boolean enableDatabaseFunctions) {
            this.enableDatabaseFunctions = enableDatabaseFunctions;
        }

        public boolean isEnableQueryOptimizations() {
            return enableQueryOptimizations;
        }

        public void setEnableQueryOptimizations(boolean enableQueryOptimizations) {
            this.enableQueryOptimizations = enableQueryOptimizations;
        }

        public int getQueryTimeoutMs() {
            return queryTimeoutMs;
        }

        public void setQueryTimeoutMs(int queryTimeoutMs) {
            this.queryTimeoutMs = queryTimeoutMs;
        }

        public boolean isEnableAsyncProcessing() {
            return enableAsyncProcessing;
        }

        public void setEnableAsyncProcessing(boolean enableAsyncProcessing) {
            this.enableAsyncProcessing = enableAsyncProcessing;
        }

        public int getAsyncThreadPoolSize() {
            return asyncThreadPoolSize;
        }

        public void setAsyncThreadPoolSize(int asyncThreadPoolSize) {
            this.asyncThreadPoolSize = asyncThreadPoolSize;
        }

        public boolean isEnableDenormalization() {
            return enableDenormalization;
        }

        public void setEnableDenormalization(boolean enableDenormalization) {
            this.enableDenormalization = enableDenormalization;
        }

        public int getBulkOperationBatchSize() {
            return bulkOperationBatchSize;
        }

        public void setBulkOperationBatchSize(int bulkOperationBatchSize) {
            this.bulkOperationBatchSize = bulkOperationBatchSize;
        }
    }

    /**
     * Audit configuration for RBAC operations
     */
    public static class Audit {
        /**
         * Enable comprehensive audit logging for all RBAC operations
         */
        private boolean enableFullAuditing = true;

        /**
         * Enable audit logging for permission checks (high volume)
         */
        private boolean enablePermissionCheckAuditing = false;

        /**
         * Enable audit logging for cache operations
         */
        private boolean enableCacheAuditing = false;

        /**
         * Audit log retention period in days
         */
        private int retentionDays = 90;

        /**
         * Include stack trace in audit logs for security violations
         */
        private boolean includeStackTrace = true;

        public boolean isEnableFullAuditing() {
            return enableFullAuditing;
        }

        public void setEnableFullAuditing(boolean enableFullAuditing) {
            this.enableFullAuditing = enableFullAuditing;
        }

        public boolean isEnablePermissionCheckAuditing() {
            return enablePermissionCheckAuditing;
        }

        public void setEnablePermissionCheckAuditing(boolean enablePermissionCheckAuditing) {
            this.enablePermissionCheckAuditing = enablePermissionCheckAuditing;
        }

        public boolean isEnableCacheAuditing() {
            return enableCacheAuditing;
        }

        public void setEnableCacheAuditing(boolean enableCacheAuditing) {
            this.enableCacheAuditing = enableCacheAuditing;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }

        public boolean isIncludeStackTrace() {
            return includeStackTrace;
        }

        public void setIncludeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
        }
    }

    /**
     * Convenience methods for commonly accessed values
     */

    /**
     * Get the permission cache TTL in seconds (for Redis TTL settings)
     */
    public long getPermissionCacheTtlSeconds() {
        return cache.getPermissionTtlMinutes() * 60L;
    }

    /**
     * Get the permission query timeout in seconds
     */
    public int getPermissionQueryTimeoutSeconds() {
        return performance.getQueryTimeoutMs() / 1000;
    }

    /**
     * Check if role limits should be strictly enforced
     */
    public boolean shouldEnforceRoleLimits() {
        return limits.isEnableStrictValidation();
    }

    /**
     * Check if caching is enabled (either distributed or local)
     */
    public boolean isCachingEnabled() {
        return cache.isEnableDistributedCache() || cache.isEnableLocalCache();
    }

    /**
     * Check if performance optimizations are enabled
     */
    public boolean arePerformanceOptimizationsEnabled() {
        return performance.isEnableDatabaseFunctions() && performance.isEnableQueryOptimizations();
    }

    /**
     * Get the full Redis cache key for a user's permissions
     */
    public String getPermissionCacheKey(Long userId, Long organizationId) {
        return String.format("%s:%d:%d", cache.getKeyPrefix(), userId, organizationId);
    }

    /**
     * Get the Redis cache key pattern for invalidating organization permissions
     */
    public String getOrganizationPermissionCachePattern(Long organizationId) {
        return String.format("%s:*:%d", cache.getKeyPrefix(), organizationId);
    }
}