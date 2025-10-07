package com.platform.auth.internal;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Rate limiting filter for authentication endpoints.
 * Uses distributed rate limiting with Redis to prevent brute force attacks.
 *
 * @since 1.0.0
 */
@Component
@Profile("!test & !integration-test") // Disable in test profiles
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:auth:";

    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> bucketConfiguration;

    /**
     * Constructor with dependency injection.
     *
     * @param proxyManager the distributed bucket proxy manager
     * @param bucketConfiguration the bucket configuration supplier
     */
    public RateLimitingFilter(
            final ProxyManager<String> proxyManager,
            final Supplier<BucketConfiguration> bucketConfiguration) {
        this.proxyManager = proxyManager;
        this.bucketConfiguration = bucketConfiguration;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();

        // Only apply rate limiting to authentication endpoints
        if (!isAuthEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String clientKey = getClientKey(request);
        final Bucket bucket = proxyManager.builder()
                .build(RATE_LIMIT_PREFIX + clientKey, bucketConfiguration);

        final ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers for client visibility
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.addHeader("X-Rate-Limit-Retry-After-Seconds",
                    String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));

            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds",
                    String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));

            response.getWriter().write(
                    "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
        }
    }

    /**
     * Checks if the request URI is an authentication endpoint.
     *
     * @param uri the request URI
     * @return true if it's an auth endpoint
     */
    private boolean isAuthEndpoint(final String uri) {
        return uri.startsWith("/api/v1/auth/");
    }

    /**
     * Generates a client key for rate limiting based on IP address.
     * In production, consider using user ID for authenticated requests.
     *
     * @param request the HTTP request
     * @return the client key
     */
    private String getClientKey(final HttpServletRequest request) {
        final String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        final String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}