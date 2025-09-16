package com.platform.shared.config;

import com.platform.auth.internal.OpaqueTokenStore;
import com.platform.shared.security.PlatformUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to authenticate requests using opaque tokens from the Authorization header.
 */
public class OpaqueTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(OpaqueTokenAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final OpaqueTokenStore tokenStore;

    public OpaqueTokenAuthenticationFilter(OpaqueTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateToken(token);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        return null;
    }

    private void authenticateToken(String token) {
        try {
            tokenStore.validateToken(token)
                .ifPresent(userPrincipal -> {
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            userPrincipal.getAuthorities()
                        );

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.debug("Authenticated user {} via opaque token",
                               userPrincipal.getUserId());
                });
        } catch (Exception e) {
            logger.debug("Token authentication failed: {}", e.getMessage());
            // Don't set authentication, let the request proceed as unauthenticated
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip filter for public endpoints that don't need authentication
        return path.startsWith("/actuator/health") ||
               path.startsWith("/api/v1/auth/oauth2/providers") ||
               path.startsWith("/api/v1/auth/oauth2/authorize") ||
               path.startsWith("/api/v1/auth/oauth2/callback") ||
               path.startsWith("/api/v1/plans") ||
               path.startsWith("/api/v1/webhooks/");
    }
}