package com.platform.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Custom user principal for the platform with tenant context.
 */
public class PlatformUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String name;
    private final UUID organizationId;
    private final String organizationSlug;
    private final String role;
    private final boolean enabled;

    public PlatformUserPrincipal(UUID userId, String email, String name,
                               UUID organizationId, String organizationSlug,
                               String role, boolean enabled) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.organizationId = organizationId;
        this.organizationSlug = organizationSlug;
        this.role = role;
        this.enabled = enabled;
    }

    // Factory method for system user (no organization context)
    public static PlatformUserPrincipal systemUser(UUID userId, String email, String name) {
        return new PlatformUserPrincipal(userId, email, name, null, null, "SYSTEM", true);
    }

    // Factory method for organization member
    public static PlatformUserPrincipal organizationMember(UUID userId, String email, String name,
                                                         UUID organizationId, String organizationSlug,
                                                         String role) {
        return new PlatformUserPrincipal(userId, email, name, organizationId, organizationSlug, role, true);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }

        // Convert role to Spring Security authority
        String authority = switch (role.toUpperCase()) {
            case "OWNER" -> "ROLE_OWNER";
            case "ADMIN" -> "ROLE_ADMIN";
            case "MEMBER" -> "ROLE_MEMBER";
            case "SYSTEM" -> "ROLE_SYSTEM";
            default -> "ROLE_USER";
        };

        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        // No password for OAuth2 users
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Custom getters
    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationSlug() {
        return organizationSlug;
    }

    public String getRole() {
        return role;
    }

    // Role checking methods
    public boolean isOwner() {
        return "OWNER".equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role) || isOwner();
    }

    public boolean isMember() {
        return "MEMBER".equalsIgnoreCase(role) || isAdmin();
    }

    public boolean isSystemUser() {
        return "SYSTEM".equalsIgnoreCase(role);
    }

    public boolean hasOrganizationContext() {
        return organizationId != null;
    }

    public boolean belongsToOrganization(UUID orgId) {
        return organizationId != null && organizationId.equals(orgId);
    }

    @Override
    public String toString() {
        return "PlatformUserPrincipal{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", organizationId=" + organizationId +
                ", organizationSlug='" + organizationSlug + '\'' +
                ", role='" + role + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}