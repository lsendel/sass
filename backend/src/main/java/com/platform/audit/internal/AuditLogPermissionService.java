package com.platform.audit.internal;

import com.platform.user.UserProfile;
import com.platform.user.internal.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for determining user permissions for audit log access.
 *
 * This service enforces access control policies for the audit log viewer,
 * including tenant isolation, role-based permissions, and data sensitivity controls.
 */
@Service
public class AuditLogPermissionService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogPermissionService.class);

    private static final int ADMIN_EXPORT_LIMIT = 100000;
    private static final int REGULAR_EXPORT_LIMIT = 10000;
    private static final int RETENTION_DAYS_FULL = 365;
    private static final int RETENTION_DAYS_LIMITED = 90;

    private final UserProfileService userProfileService;

    public AuditLogPermissionService(final UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Get audit permissions for a user.
     * Results are cached for improved performance.
     *
     * @param userId the user to check permissions for
     * @return user's audit permissions
     */
    @Cacheable(value = "auditPermissions", key = "#userId")
    public UserAuditPermissions getUserAuditPermissions(final UUID userId) {
        LOG.debug("Computing audit permissions for user: {}", userId);

        try {
            // Query user service to get user's organization and roles
            final UserProfile userProfile = userProfileService.findById(userId);
            final UUID organizationId = userProfile.getOrganization().getId();
            final UserProfile.UserRole userRole = userProfile.getRole();

            // Map role to permissions
            return mapRoleToPermissions(organizationId, userRole);

        } catch (Exception e) {
            LOG.error("Error fetching user profile for permission check: {}", userId, e);
            // Fall back to minimal permissions on error
            return UserAuditPermissions.basicUser(UUID.randomUUID());
        }
    }

    /**
     * Map user role to audit permissions.
     *
     * @param organizationId the user's organization
     * @param role the user's role
     * @return mapped permissions
     */
    private UserAuditPermissions mapRoleToPermissions(
            final UUID organizationId,
            final UserProfile.UserRole role) {

        return switch (role) {
            case OWNER, ADMIN ->
                    // Admins and owners have full audit access
                    UserAuditPermissions.admin(organizationId);

            case MEMBER ->
                    // Regular members can view basic logs
                    UserAuditPermissions.basicUser(organizationId);

            case GUEST ->
                    // Guests have minimal access
                    new UserAuditPermissions(
                            organizationId,
                            false,  // Cannot view audit logs
                            false,  // Cannot view system actions
                            false,  // Cannot view sensitive data
                            false   // Cannot view technical data
                    );
        };
    }

    /**
     * Check if user can view a specific audit log entry.
     *
     * @param userId the user requesting access
     * @param auditEvent the audit event to check
     * @return true if user can view the entry
     */
    public boolean canViewAuditEntry(final UUID userId, final AuditEvent auditEvent) {
        var permissions = getUserAuditPermissions(userId);

        // Basic tenant isolation
        if (!auditEvent.getOrganizationId().equals(permissions.organizationId())) {
            return false;
        }

        // User can view their own actions
        if (auditEvent.getActorId() != null && auditEvent.getActorId().equals(userId)) {
            return true;
        }

        // System actions require special permission
        if (auditEvent.getActorId() == null && !permissions.canViewSystemActions()) {
            return false;
        }

        // Other users' actions require system permission
        return permissions.canViewSystemActions();
    }

    /**
     * Check if user can export audit logs.
     *
     * @param userId the user requesting export
     * @return true if user can export
     */
    public boolean canExportAuditLogs(final UUID userId) {
        var permissions = getUserAuditPermissions(userId);
        return permissions.canViewAuditLogs(); // Basic export permission same as view
    }

    /**
     * Get the maximum number of entries a user can export at once.
     *
     * @param userId the user requesting export
     * @return maximum export limit
     */
    public int getExportLimit(final UUID userId) {
        var permissions = getUserAuditPermissions(userId);

        if (permissions.canViewSystemActions()) {
            return ADMIN_EXPORT_LIMIT; // Admin users can export more
        } else {
            return REGULAR_EXPORT_LIMIT;  // Regular users have lower limit
        }
    }

    /**
     * Check if user can view sensitive fields in audit logs.
     *
     * @param userId the user to check
     * @return true if user can view sensitive data
     */
    public boolean canViewSensitiveFields(final UUID userId) {
        return getUserAuditPermissions(userId).canViewSensitiveData();
    }

    /**
     * Check if user can view technical fields (IP addresses, user agents).
     *
     * @param userId the user to check
     * @return true if user can view technical data
     */
    public boolean canViewTechnicalFields(final UUID userId) {
        return getUserAuditPermissions(userId).canViewTechnicalData();
    }

    /**
     * Get the date range limit for audit log queries.
     * Prevents users from querying too far back in history.
     *
     * @param userId the user to check
     * @return maximum days back the user can query
     */
    public int getQueryDateRangeLimit(final UUID userId) {
        var permissions = getUserAuditPermissions(userId);

        if (permissions.canViewSystemActions()) {
            return RETENTION_DAYS_FULL * 2; // Admins can query 2 years back
        } else {
            return RETENTION_DAYS_LIMITED;      // Regular users limited to 90 days
        }
    }


    /**
     * Record class representing comprehensive user audit permissions.
     */
    public record UserAuditPermissions(
        UUID organizationId,
        boolean canViewAuditLogs,
        boolean canViewSystemActions,
        boolean canViewSensitiveData,
        boolean canViewTechnicalData
    ) {
        /**
         * Factory method for basic user permissions (view only).
         */
        public static UserAuditPermissions basicUser(final UUID organizationId) {
            return new UserAuditPermissions(
                organizationId,
                true,   // Can view basic audit logs
                false,  // Cannot view system actions
                false,  // Cannot view sensitive data
                false   // Cannot view technical data
            );
        }

        /**
         * Factory method for admin permissions (full access).
         */
        public static UserAuditPermissions admin(final UUID organizationId) {
            return new UserAuditPermissions(
                organizationId,
                true,   // Can view audit logs
                true,   // Can view system actions
                true,   // Can view sensitive data
                true    // Can view technical data
            );
        }

        /**
         * Factory method for compliance officer permissions.
         */
        public static UserAuditPermissions complianceOfficer(final UUID organizationId) {
            return new UserAuditPermissions(
                organizationId,
                true,   // Can view audit logs
                true,   // Can view system actions
                true,   // Can view sensitive data
                false   // Cannot view technical data
            );
        }
    }
}