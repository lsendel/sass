package com.platform.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom UserPrincipal implementation for Spring Security.
 *
 * This principal contains essential user information needed across the application,
 * including user ID, organization ID, and roles. It implements UserDetails for
 * compatibility with Spring Security.
 *
 * @see org.springframework.security.core.userdetails.UserDetails
 */
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID organizationId;
    private final String username;
    private final String email;
    private final Set<String> roles;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;

    /**
     * Constructor with all fields.
     */
    public UserPrincipal(
            UUID userId,
            UUID organizationId,
            String username,
            String email,
            Set<String> roles,
            boolean enabled,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired) {
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.email = email;
        this.roles = roles != null ? Set.copyOf(roles) : Collections.emptySet();
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
    }

    /**
     * Static factory method for creating a principal for an active user.
     */
    public static UserPrincipal of(
            UUID userId,
            UUID organizationId,
            String username,
            String email,
            Set<String> roles) {
        return new UserPrincipal(
                userId,
                organizationId,
                username,
                email,
                roles,
                true,  // enabled
                true,  // accountNonExpired
                true,  // accountNonLocked
                true   // credentialsNonExpired
        );
    }

    /**
     * Static factory method for creating a principal with account status.
     */
    public static UserPrincipal withStatus(
            UUID userId,
            UUID organizationId,
            String username,
            String email,
            Set<String> roles,
            boolean enabled,
            boolean accountNonLocked) {
        return new UserPrincipal(
                userId,
                organizationId,
                username,
                email,
                roles,
                enabled,
                true,  // accountNonExpired
                accountNonLocked,
                true   // credentialsNonExpired
        );
    }

    // Core identity methods

    /**
     * Get the user's unique identifier.
     *
     * @return user ID (never null)
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * Get the user's organization identifier for multi-tenancy.
     *
     * @return organization ID (never null)
     */
    public UUID getOrganizationId() {
        return organizationId;
    }

    /**
     * Get the user's email address.
     *
     * @return email address (may be null)
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get the user's roles.
     *
     * @return unmodifiable set of role names
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Check if user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Check if user has any of the specified roles.
     *
     * @param rolesToCheck roles to check
     * @return true if user has at least one of the roles
     */
    public boolean hasAnyRole(String... rolesToCheck) {
        for (String role : rolesToCheck) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    // UserDetails implementation

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getPassword() {
        // Password is not stored in the principal for security
        // Authentication is handled by the authentication filter using tokens
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Object methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return userId.equals(that.userId) && organizationId.equals(that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, organizationId);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId=" + userId +
                ", organizationId=" + organizationId +
                ", username='" + username + '\'' +
                ", roles=" + roles +
                ", enabled=" + enabled +
                '}';
    }
}
