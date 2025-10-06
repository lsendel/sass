package com.platform.shared.ratelimit;

import lombok.Builder;
import lombok.Data;

/**
 * Result of a rate limit check.
 */
@Data
@Builder
public class RateLimitResult {

    /**
     * Whether the request is allowed.
     */
    private boolean allowed;

    /**
     * Maximum requests allowed in the window.
     */
    private int limit;

    /**
     * Current number of requests in the window.
     */
    private int currentCount;

    /**
     * Window size in milliseconds.
     */
    private long windowSizeMs;

    /**
     * Number of rate limit violations (for IP blocking).
     */
    private int violationCount;

    /**
     * Reason for denial (if not allowed).
     */
    private String reason;

    /**
     * Creates a result indicating the request is allowed.
     */
    public static RateLimitResult allowed(int limit, int currentCount, long windowSizeMs) {
        return RateLimitResult.builder()
                .allowed(true)
                .limit(limit)
                .currentCount(currentCount)
                .windowSizeMs(windowSizeMs)
                .violationCount(0)
                .build();
    }

    /**
     * Creates a result indicating the request is denied.
     */
    public static RateLimitResult denied(int limit, int currentCount, long windowSizeMs,
                                       int violationCount, String reason) {
        return RateLimitResult.builder()
                .allowed(false)
                .limit(limit)
                .currentCount(currentCount)
                .windowSizeMs(windowSizeMs)
                .violationCount(violationCount)
                .reason(reason)
                .build();
    }
}