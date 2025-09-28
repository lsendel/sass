package com.platform.audit.internal;

import com.platform.audit.api.AuditLogEntryDTO;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for determining user permission scope for audit log access.
 * Handles role-based filtering and data redaction based on user permissions.
 */
@Service
public class UserPermissionScopeService {

    private static final Logger log = LoggerFactory.getLogger(UserPermissionScopeService.class);

    private final UserRepository userRepository;

    public UserPermissionScopeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Check if user has organization-level audit access.
     */
    public boolean hasOrganizationAuditAccess(UUID userId, UUID organizationId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found for audit access check: {}", userId);
            return false;
        }

        User user = userOpt.get();

        // Check if user belongs to the organization
        if (!organizationId.equals(user.getOrganizationId())) {
            log.warn("User {} attempted to access audit logs for different organization: {}",
                userId, organizationId);
            return false;
        }

        // In a full RBAC implementation, this would check specific roles
        // For now, any authenticated user in the organization can view audit logs
        // TODO: Implement proper role-based permissions once RBAC is fully integrated
        return true;
    }

    /**
     * Check if user can access a specific audit log entry.
     */
    public boolean canAccessAuditEntry(UUID userId, AuditEvent auditEvent) {
        if (auditEvent == null) {
            return false;
        }

        // Check organization-level access first
        if (!hasOrganizationAuditAccess(userId, auditEvent.getOrganizationId())) {
            return false;
        }

        // Additional entry-specific checks could go here
        // For example, limiting access to certain sensitive actions
        return true;
    }

    /**
     * Apply data redaction to audit log entry based on user permissions.
     */
    public AuditLogEntryDTO applyDataRedaction(UUID userId, AuditEvent auditEvent) {
        if (!canAccessAuditEntry(userId, auditEvent)) {
            throw new SecurityException("User does not have access to this audit entry");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new SecurityException("User not found");
        }

        User user = userOpt.get();

        // Determine if user has full access or needs redacted view
        boolean hasFullAccess = hasFullAuditAccess(user);

        if (hasFullAccess) {
            return AuditLogEntryDTO.fromAuditEventFull(auditEvent);
        } else {
            return AuditLogEntryDTO.fromAuditEventRedacted(auditEvent);
        }
    }

    /**
     * Create an appropriate filter based on user permissions.
     */
    public AuditLogFilter createUserScopedFilter(UUID userId,
                                                AuditLogFilter requestedFilter) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new SecurityException("User not found");
        }

        User user = userOpt.get();
        UUID userOrgId = user.getOrganizationId();

        // Ensure the filter is scoped to user's organization
        if (requestedFilter.organizationId() != null &&
            !userOrgId.equals(requestedFilter.organizationId())) {
            throw new SecurityException("User cannot access audit logs for different organization");
        }

        // Create organization-scoped filter
        return new AuditLogFilter(
            userOrgId,
            hasRestrictedAccess(user) ? userId : null, // Restrict to user's own actions if needed
            requestedFilter.dateFrom(),
            requestedFilter.dateTo(),
            requestedFilter.search(),
            requestedFilter.actionTypes(),
            requestedFilter.actorEmails(),
            hasFullAccess(user) ? requestedFilter.includeSystemActions() : false,
            requestedFilter.pageNumber(),
            requestedFilter.pageSize()
        );
    }

    /**
     * Check if user has full audit access (can see all org data).
     */
    private boolean hasFullAuditAccess(User user) {
        // TODO: Implement proper role-based checks once RBAC is integrated
        // For now, all users have full access within their organization
        return true;
    }

    /**
     * Check if user has restricted access (can only see their own actions).
     */
    private boolean hasRestrictedAccess(User user) {
        // TODO: Implement proper role-based checks once RBAC is integrated
        // For now, no users have restricted access
        return false;
    }

    /**
     * Check if user can see system-generated audit entries.
     */
    private boolean hasFullAccess(User user) {
        // TODO: Implement proper role-based checks once RBAC is integrated
        // For now, all users can see system actions
        return true;
    }
}