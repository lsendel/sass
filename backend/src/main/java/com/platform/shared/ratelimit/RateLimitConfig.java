package com.platform.shared.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for rate limiting rules.
 * Supports different rules for different endpoints.
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitConfig {

    private int maxViolations = 5; // Max violations before IP block
    private Map<String, RateLimitRule> rules = new HashMap<>();

    public RateLimitConfig() {
        // Default rules
        initializeDefaultRules();
    }

    private void initializeDefaultRules() {
        // Authentication endpoints - more restrictive
        rules.put("auth", RateLimitRule.builder()
                .name("auth")
                .maxRequests(5)
                .windowSizeMs(TimeUnit.MINUTES.toMillis(1))
                .build());

        // Password-related endpoints - very restrictive
        rules.put("password", RateLimitRule.builder()
                .name("password")
                .maxRequests(3)
                .windowSizeMs(TimeUnit.MINUTES.toMillis(5))
                .build());

        // Payment endpoints - restrictive
        rules.put("payment", RateLimitRule.builder()
                .name("payment")
                .maxRequests(10)
                .windowSizeMs(TimeUnit.MINUTES.toMillis(1))
                .build());

        // General API endpoints - moderate
        rules.put("api", RateLimitRule.builder()
                .name("api")
                .maxRequests(100)
                .windowSizeMs(TimeUnit.MINUTES.toMillis(1))
                .build());
    }

    /**
     * Gets the appropriate rate limit rule for a given path.
     */
    public RateLimitRule getRuleForPath(String path) {
        if (path.contains("password")) {
            return rules.get("password");
        }
        if (path.startsWith("/api/v1/auth/")) {
            return rules.get("auth");
        }
        if (path.startsWith("/api/v1/payment/")) {
            return rules.get("payment");
        }
        return rules.get("api");
    }

    // Getters and setters
    public int getMaxViolations() {
        return maxViolations;
    }

    public void setMaxViolations(int maxViolations) {
        this.maxViolations = maxViolations;
    }

    public Map<String, RateLimitRule> getRules() {
        return rules;
    }

    public void setRules(Map<String, RateLimitRule> rules) {
        this.rules = rules;
    }
}