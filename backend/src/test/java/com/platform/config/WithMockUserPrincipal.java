package com.platform.config;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Custom annotation for testing with UserPrincipal authentication.
 * This creates a properly configured UserPrincipal with UUID-based identity.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserPrincipalSecurityContextFactory.class)
public @interface WithMockUserPrincipal {

    /**
     * The user ID (UUID format).
     */
    String userId() default "22222222-2222-2222-2222-222222222222";

    /**
     * The organization ID (UUID format).
     */
    String organizationId() default "11111111-1111-1111-1111-111111111111";

    /**
     * The username.
     */
    String username() default "testuser";

    /**
     * The email address.
     */
    String email() default "test@example.com";

    /**
     * The roles for the user (without ROLE_ prefix).
     */
    String[] roles() default {"USER"};

    /**
     * Whether the account is enabled.
     */
    boolean enabled() default true;

    /**
     * Whether the account is locked.
     */
    boolean accountNonLocked() default true;
}
