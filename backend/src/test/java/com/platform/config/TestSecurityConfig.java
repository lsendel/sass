package com.platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for integration tests that need basic authentication.
 * This provides minimal security setup for @WithMockUser tests.
 * CSRF protection is disabled as tests use stateless authentication.
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("integration-test")
public class TestSecurityConfig {

    @Bean
    @Primary
    @SuppressWarnings("lgtm[java/spring-disabled-csrf-protection]") // CSRF protection intentionally disabled for test environment
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
            // lgtm[java/spring-disabled-csrf-protection] - Safe for test environment only
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}