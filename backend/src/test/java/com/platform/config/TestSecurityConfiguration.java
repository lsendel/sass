package com.platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration.
 * Provides a permissive security configuration and cache manager for integration tests.
 * Used by BaseIntegrationTest and test profile.
 */
@TestConfiguration
@EnableWebSecurity
@Profile("test") // Only active for "test" profile
public class TestSecurityConfiguration {

    /**
     * Configures a permissive security filter chain for tests.
     * Allows all requests without authentication.
     * CSRF is disabled as this is test-only configuration and tests use stateless requests.
     *
     * @param http the HttpSecurity to configure
     * @return the configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    @SuppressWarnings("lgtm[java/spring-disabled-csrf-protection]") // CSRF protection intentionally disabled for test environment
    public SecurityFilterChain testFilterChain(final HttpSecurity http) throws Exception {
        http
                // lgtm[java/spring-disabled-csrf-protection] - Safe for test environment only
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * Provides a simple in-memory cache manager for tests.
     *
     * @return cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
