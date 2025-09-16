package com.platform.shared.security;

import java.util.UUID;

/**
 * Thread-local tenant context for multi-tenant operations.
 * Provides organization isolation at the application level.
 */
public class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Tenant information holder
     */
    public static class TenantInfo {
        private final UUID organizationId;
        private final String organizationSlug;
        private final UUID userId;

        public TenantInfo(UUID organizationId, String organizationSlug, UUID userId) {
            this.organizationId = organizationId;
            this.organizationSlug = organizationSlug;
            this.userId = userId;
        }

        public UUID getOrganizationId() {
            return organizationId;
        }

        public String getOrganizationSlug() {
            return organizationSlug;
        }

        public UUID getUserId() {
            return userId;
        }

        @Override
        public String toString() {
            return "TenantInfo{" +
                    "organizationId=" + organizationId +
                    ", organizationSlug='" + organizationSlug + '\'' +
                    ", userId=" + userId +
                    '}';
        }
    }

    /**
     * Set the current tenant context
     */
    public static void setTenantInfo(UUID organizationId, String organizationSlug, UUID userId) {
        CURRENT_TENANT.set(new TenantInfo(organizationId, organizationSlug, userId));
    }

    /**
     * Set the current tenant context with TenantInfo object
     */
    public static void setTenantInfo(TenantInfo tenantInfo) {
        CURRENT_TENANT.set(tenantInfo);
    }

    /**
     * Get the current tenant information
     */
    public static TenantInfo getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * Get the current organization ID
     */
    public static UUID getCurrentOrganizationId() {
        TenantInfo tenant = getCurrentTenant();
        return tenant != null ? tenant.getOrganizationId() : null;
    }

    /**
     * Get the current organization slug
     */
    public static String getCurrentOrganizationSlug() {
        TenantInfo tenant = getCurrentTenant();
        return tenant != null ? tenant.getOrganizationSlug() : null;
    }

    /**
     * Get the current user ID
     */
    public static UUID getCurrentUserId() {
        TenantInfo tenant = getCurrentTenant();
        return tenant != null ? tenant.getUserId() : null;
    }

    /**
     * Check if tenant context is set
     */
    public static boolean hasTenantContext() {
        return getCurrentTenant() != null;
    }

    /**
     * Check if current user belongs to the specified organization
     */
    public static boolean belongsToOrganization(UUID organizationId) {
        UUID currentOrgId = getCurrentOrganizationId();
        return currentOrgId != null && currentOrgId.equals(organizationId);
    }

    /**
     * Validate that current user can access the specified organization
     */
    public static void validateOrganizationAccess(UUID organizationId) {
        if (!belongsToOrganization(organizationId)) {
            throw new SecurityException("Access denied to organization: " + organizationId);
        }
    }

    /**
     * Clear the current tenant context
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Execute a block of code within a specific tenant context
     */
    public static <T> T executeInTenantContext(UUID organizationId, String organizationSlug,
                                             UUID userId, TenantContextCallback<T> callback) {
        TenantInfo previousTenant = getCurrentTenant();
        try {
            setTenantInfo(organizationId, organizationSlug, userId);
            return callback.execute();
        } finally {
            if (previousTenant != null) {
                setTenantInfo(previousTenant);
            } else {
                clear();
            }
        }
    }

    /**
     * Execute a block of code within a specific tenant context
     */
    public static <T> T executeInTenantContext(TenantInfo tenantInfo, TenantContextCallback<T> callback) {
        return executeInTenantContext(
            tenantInfo.getOrganizationId(),
            tenantInfo.getOrganizationSlug(),
            tenantInfo.getUserId(),
            callback
        );
    }

    /**
     * Execute a block of code without tenant context (system operations)
     */
    public static <T> T executeAsSystem(TenantContextCallback<T> callback) {
        TenantInfo previousTenant = getCurrentTenant();
        try {
            clear();
            return callback.execute();
        } finally {
            if (previousTenant != null) {
                setTenantInfo(previousTenant);
            }
        }
    }

    /**
     * Functional interface for tenant context callbacks
     */
    @FunctionalInterface
    public interface TenantContextCallback<T> {
        T execute();
    }

    /**
     * Runnable version for operations that don't return values
     */
    public static void executeInTenantContext(UUID organizationId, String organizationSlug,
                                            UUID userId, Runnable operation) {
        executeInTenantContext(organizationId, organizationSlug, userId, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Runnable version for operations that don't return values
     */
    public static void executeInTenantContext(TenantInfo tenantInfo, Runnable operation) {
        executeInTenantContext(tenantInfo, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * Runnable version for system operations
     */
    public static void executeAsSystem(Runnable operation) {
        executeAsSystem(() -> {
            operation.run();
            return null;
        });
    }
}