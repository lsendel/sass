package com.platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for contract tests.
 * Provides minimal security setup for @WithMockUser tests with method-level security.
 * CSRF protection is disabled as contract tests use stateless requests.
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("contract-test")
public class ContractTestSecurityConfig {

    @Bean
    @org.springframework.context.annotation.Primary
    @SuppressWarnings("lgtm[java/spring-disabled-csrf-protection]") // CSRF protection intentionally disabled for test environment
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz ->
                    authz.anyRequest().permitAll())
                .httpBasic(basic -> { })
                // lgtm[java/spring-disabled-csrf-protection] - Safe for test environment only
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}