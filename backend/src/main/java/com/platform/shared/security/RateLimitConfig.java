package com.platform.shared.security;

import java.time.Duration;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rate limiting configuration for different endpoint types.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public Map<String, RateLimitingFilter.RateLimitRule> rateLimitRules() {
        return Map.of(
            // Authentication endpoints (strict)
            "/api/v1/auth/login", new RateLimitingFilter.RateLimitRule(5, Duration.ofMinutes(15)),
            "/api/v1/auth/register", new RateLimitingFilter.RateLimitRule(3, Duration.ofMinutes(30)),
            "/api/v1/auth/request-password-reset", new RateLimitingFilter.RateLimitRule(3, Duration.ofMinutes(30)),
            
            // OAuth2 endpoints (moderate)
            "/api/v1/auth/oauth2/**", new RateLimitingFilter.RateLimitRule(20, Duration.ofMinutes(5)),
            
            // API endpoints (generous)
            "/api/v1/payments/**", new RateLimitingFilter.RateLimitRule(100, Duration.ofMinutes(1)),
            "/api/v1/subscriptions/**", new RateLimitingFilter.RateLimitRule(50, Duration.ofMinutes(1)),
            "/api/v1/users/**", new RateLimitingFilter.RateLimitRule(30, Duration.ofMinutes(1)),
            "/api/v1/organizations/**", new RateLimitingFilter.RateLimitRule(30, Duration.ofMinutes(1)),
            
            // Public endpoints (moderate)
            "/api/v1/plans", new RateLimitingFilter.RateLimitRule(20, Duration.ofMinutes(1)),
            "/actuator/health", new RateLimitingFilter.RateLimitRule(60, Duration.ofMinutes(1))
        );
    }
}
