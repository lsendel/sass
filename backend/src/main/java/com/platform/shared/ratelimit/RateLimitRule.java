package com.platform.shared.ratelimit;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a rate limiting rule.
 */
@Data
@Builder
public class RateLimitRule {

    /**
     * Name/identifier for this rule.
     */
    private String name;

    /**
     * Maximum number of requests allowed in the window.
     */
    private int maxRequests;

    /**
     * Time window size in milliseconds.
     */
    private long windowSizeMs;

    /**
     * Whether this rule is enabled.
     */
    @Builder.Default
    private boolean enabled = true;
}