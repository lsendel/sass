package com.platform.shared.ratelimit;

import com.platform.shared.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter using Redis for distributed rate limiting.
 * Implements sliding window algorithm for accurate rate limiting.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final String BLOCKED_IP_KEY_PREFIX = "blocked_ip:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RateLimitConfig rateLimitConfig;

    public RateLimitingFilter(RedisTemplate<String, String> redisTemplate,
                             ObjectMapper objectMapper,
                             RateLimitConfig rateLimitConfig) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String clientIp = getClientIpAddress(request);

        // Check if IP is temporarily blocked
        if (isIpBlocked(clientIp)) {
            handleRateLimitExceeded(response, clientIp, "IP temporarily blocked");
            return;
        }

        // Apply rate limiting to sensitive endpoints
        if (shouldApplyRateLimit(requestPath)) {
            RateLimitResult result = checkRateLimit(clientIp, requestPath);

            if (!result.isAllowed()) {
                // Block IP if too many rate limit violations
                if (result.getViolationCount() >= rateLimitConfig.getMaxViolations()) {
                    blockIpTemporarily(clientIp);
                }

                handleRateLimitExceeded(response, clientIp, result.getReason());
                return;
            }

            // Add rate limit headers
            addRateLimitHeaders(response, result);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Checks if rate limit should be applied to the given path.
     */
    private boolean shouldApplyRateLimit(String path) {
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/api/v1/payment/") ||
               path.contains("password") ||
               path.contains("login") ||
               path.contains("register");
    }

    /**
     * Implements sliding window rate limiting using Redis.
     */
    private RateLimitResult checkRateLimit(String clientIp, String path) {
        RateLimitRule rule = rateLimitConfig.getRuleForPath(path);
        String key = RATE_LIMIT_KEY_PREFIX + clientIp + ":" + rule.getName();

        try {
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - rule.getWindowSizeMs();

            // Remove old entries outside the window
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

            // Count current requests in window
            Long currentCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);

            if (currentCount != null && currentCount >= rule.getMaxRequests()) {
                // Get violation count
                String violationKey = key + ":violations";
                String violationCountStr = redisTemplate.opsForValue().get(violationKey);
                int violationCount = violationCountStr != null ? Integer.parseInt(violationCountStr) : 0;

                // Increment violation count
                redisTemplate.opsForValue().increment(violationKey);
                redisTemplate.expire(violationKey, Duration.ofMinutes(15));

                return RateLimitResult.denied(
                    rule.getMaxRequests(),
                    currentCount.intValue(),
                    rule.getWindowSizeMs(),
                    violationCount + 1,
                    "Rate limit exceeded: " + currentCount + "/" + rule.getMaxRequests()
                );
            }

            // Add current request to window
            redisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);
            redisTemplate.expire(key, Duration.ofMillis(rule.getWindowSizeMs()));

            return RateLimitResult.allowed(
                rule.getMaxRequests(),
                currentCount != null ? currentCount.intValue() + 1 : 1,
                rule.getWindowSizeMs()
            );

        } catch (Exception e) {
            logger.error("Error checking rate limit for IP: {}, path: {}", clientIp, path, e);
            // On Redis errors, allow the request (fail open)
            return RateLimitResult.allowed(rule.getMaxRequests(), 0, rule.getWindowSizeMs());
        }
    }

    /**
     * Checks if IP is temporarily blocked.
     */
    private boolean isIpBlocked(String clientIp) {
        String blockKey = BLOCKED_IP_KEY_PREFIX + clientIp;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
    }

    /**
     * Temporarily blocks an IP address.
     */
    private void blockIpTemporarily(String clientIp) {
        String blockKey = BLOCKED_IP_KEY_PREFIX + clientIp;
        redisTemplate.opsForValue().set(blockKey, "blocked", Duration.ofMinutes(30));

        logger.warn("IP {} temporarily blocked due to repeated rate limit violations", clientIp);
    }

    /**
     * Handles rate limit exceeded responses.
     */
    private void handleRateLimitExceeded(HttpServletResponse response,
                                       String clientIp,
                                       String reason) throws IOException {

        logger.warn("Rate limit exceeded for IP: {} - {}", clientIp, reason);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Too many requests. Please try again later.")
                .timestamp(Instant.now())
                .errorCode("RATE_LIMIT_EXCEEDED")
                .build();

        response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60"); // Suggest retry after 60 seconds

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    /**
     * Adds rate limit headers to the response.
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining",
                          String.valueOf(Math.max(0, result.getLimit() - result.getCurrentCount())));
        response.setHeader("X-RateLimit-Reset",
                          String.valueOf(System.currentTimeMillis() + result.getWindowSizeMs()));
    }

    /**
     * Extracts client IP address considering proxies.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}