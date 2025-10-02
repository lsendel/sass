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
     *
     * @param http the HttpSecurity to configure
     * @return the configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain testFilterChain(final HttpSecurity http) throws Exception {
        http
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
