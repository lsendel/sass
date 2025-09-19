package com.platform.shared.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Rate limiting filter for authentication endpoints to prevent brute force attacks.
 * Implements sliding window rate limiting with automatic cleanup.
 */
@Component
public class RateLimitingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Rate limiting configuration
    private static final int MAX_ATTEMPTS_PER_WINDOW = 5;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(15);
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(30);

    // Thread-safe storage for rate limiting data
    private final ConcurrentHashMap<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();
    private volatile Instant lastCleanup = Instant.now();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Only apply rate limiting to authentication endpoints
        if (shouldApplyRateLimit(httpRequest)) {
            String clientKey = getClientKey(httpRequest);

            if (isRateLimited(clientKey)) {
                logger.warn("Rate limit exceeded for client: {} on endpoint: {}",
                    clientKey, httpRequest.getRequestURI());

                sendRateLimitExceededResponse(httpResponse);
                return;
            }
        }

        // Continue with the filter chain
        chain.doFilter(request, response);
    }

    private boolean shouldApplyRateLimit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Apply rate limiting to authentication endpoints
        return "POST".equals(method) && (
            uri.startsWith("/api/v1/auth/login") ||
            uri.startsWith("/api/v1/auth/register") ||
            uri.startsWith("/api/v1/auth/request-password-reset") ||
            uri.startsWith("/api/v1/auth/reset-password") ||
            uri.startsWith("/api/v1/auth/oauth2/callback")
        );
    }

    private String getClientKey(HttpServletRequest request) {
        // Use IP address as the primary identifier
        String ipAddress = getClientIpAddress(request);

        // For additional granularity, could combine with User-Agent or other headers
        // String userAgent = request.getHeader("User-Agent");
        // return ipAddress + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");

        return ipAddress;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        // Check for X-Forwarded-For header (proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // Check for X-Real-IP header (nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    private boolean isRateLimited(String clientKey) {
        Instant now = Instant.now();

        // Clean up old entries periodically
        if (now.isAfter(lastCleanup.plus(CLEANUP_INTERVAL))) {
            cleanupOldEntries(now);
            lastCleanup = now;
        }

        RateLimitEntry entry = rateLimitStore.compute(clientKey, (key, existing) -> {
            if (existing == null) {
                return new RateLimitEntry(now);
            }

            // Reset window if enough time has passed
            if (now.isAfter(existing.windowStart.plus(WINDOW_DURATION))) {
                return new RateLimitEntry(now);
            }

            // Increment attempt count within current window
            existing.attempts.incrementAndGet();
            return existing;
        });

        return entry.attempts.get() > MAX_ATTEMPTS_PER_WINDOW;
    }

    private void cleanupOldEntries(Instant now) {
        rateLimitStore.entrySet().removeIf(entry -> {
            Instant windowEnd = entry.getValue().windowStart.plus(WINDOW_DURATION);
            return now.isAfter(windowEnd);
        });

        logger.debug("Rate limit cleanup completed. Active entries: {}", rateLimitStore.size());
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_ATTEMPTS_PER_WINDOW));
        response.setHeader("X-RateLimit-Window", String.valueOf(WINDOW_DURATION.toSeconds()));
        response.setHeader("Retry-After", String.valueOf(WINDOW_DURATION.toSeconds()));

        String jsonResponse = """
            {
                "error": "RATE_LIMIT_EXCEEDED",
                "message": "Too many authentication attempts. Please try again later.",
                "retryAfter": %d
            }
            """.formatted(WINDOW_DURATION.toSeconds());

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * Rate limit entry storing attempt count and window start time.
     */
    private static class RateLimitEntry {
        final Instant windowStart;
        final AtomicInteger attempts;

        RateLimitEntry(Instant windowStart) {
            this.windowStart = windowStart;
            this.attempts = new AtomicInteger(1);
        }
    }
}