package com.platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for integration tests that need basic authentication.
 * This provides minimal security setup for @WithMockUser tests.
 */
@TestConfiguration
@EnableWebSecurity
@Profile("integration-test")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz ->
                    authz.anyRequest().authenticated())
                .httpBasic(basic -> { })
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}