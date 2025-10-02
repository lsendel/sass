package com.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Configuration for JPA auditing.
 */
@Configuration
public class JpaAuditingConfiguration {

    /**
     * Provides the current auditor for JPA auditing.
     * This method is called by Spring Data JPA to populate createdBy and lastModifiedBy fields.
     * Returns the authenticated user's name or "system" for anonymous requests.
     *
     * @return AuditorAware that retrieves the current authenticated user
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.of(authentication.getName());
        };
    }
}
