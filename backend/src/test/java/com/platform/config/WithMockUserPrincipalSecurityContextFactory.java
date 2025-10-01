package com.platform.config;

import com.platform.shared.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Set;
import java.util.UUID;

/**
 * Security context factory for creating test authentication with UserPrincipal.
 * This is used by the @WithMockUserPrincipal annotation to set up proper test security.
 */
public class WithMockUserPrincipalSecurityContextFactory
        implements WithSecurityContextFactory<WithMockUserPrincipal> {

    @Override
    public SecurityContext createSecurityContext(WithMockUserPrincipal annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // Create UserPrincipal from annotation parameters
        UserPrincipal principal = UserPrincipal.withStatus(
                UUID.fromString(annotation.userId()),
                UUID.fromString(annotation.organizationId()),
                annotation.username(),
                annotation.email(),
                Set.of(annotation.roles()),
                annotation.enabled(),
                annotation.accountNonLocked()
        );

        // Create authentication token with UserPrincipal
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        context.setAuthentication(authentication);
        return context;
    }
}
