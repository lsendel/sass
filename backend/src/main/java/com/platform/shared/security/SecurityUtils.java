package com.platform.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for common security operations.
 *
 * Provides convenient methods to access the current user's information
 * from the Spring Security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Get the current authenticated user's ID.
     *
     * @return user ID if authenticated, empty otherwise
     */
    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getUserId);
    }

    /**
     * Get the current authenticated user's organization ID.
     *
     * @return organization ID if authenticated, empty otherwise
     */
    public static Optional<UUID> getCurrentOrganizationId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getOrganizationId);
    }

    /**
     * Get the current authenticated user's username.
     *
     * @return username if authenticated, empty otherwise
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getUsername);
    }

    /**
     * Get the current UserPrincipal from the security context.
     *
     * @return UserPrincipal if authenticated with custom principal, empty otherwise
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) principal);
        }

        return Optional.empty();
    }

    /**
     * Check if the current user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the role, false otherwise
     */
    public static boolean hasRole(String role) {
        return getCurrentUserPrincipal()
                .map(principal -> principal.hasRole(role))
                .orElse(false);
    }

    /**
     * Check if the current user has any of the specified roles.
     *
     * @param roles roles to check
     * @return true if user has at least one role, false otherwise
     */
    public static boolean hasAnyRole(String... roles) {
        return getCurrentUserPrincipal()
                .map(principal -> principal.hasAnyRole(roles))
                .orElse(false);
    }

    /**
     * Check if there is a currently authenticated user.
     *
     * @return true if user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * Get the current user ID or throw exception if not authenticated.
     *
     * @return user ID
     * @throws IllegalStateException if user is not authenticated
     */
    public static UUID requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User is not authenticated"));
    }

    /**
     * Get the current organization ID or throw exception if not authenticated.
     *
     * @return organization ID
     * @throws IllegalStateException if user is not authenticated
     */
    public static UUID requireCurrentOrganizationId() {
        return getCurrentOrganizationId()
                .orElseThrow(() -> new IllegalStateException("User is not authenticated"));
    }
}
