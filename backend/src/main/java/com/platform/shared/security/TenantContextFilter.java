package com.platform.shared.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Filter to extract and set tenant context from authenticated user. */
public class TenantContextFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(TenantContextFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      setTenantContextFromAuthentication();
      filterChain.doFilter(request, response);
    } finally {
      // Always clear tenant context after request
      TenantContext.clear();
    }
  }

  private void setTenantContextFromAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal())) {

      try {
        if (authentication.getPrincipal() instanceof PlatformUserPrincipal userPrincipal) {
          TenantContext.setTenantInfo(
              userPrincipal.getOrganizationId(),
              userPrincipal.getOrganizationSlug(),
              userPrincipal.getUserId());

          logger.debug(
              "Set tenant context for user: {} in organization: {}",
              userPrincipal.getUserId(),
              userPrincipal.getOrganizationId());
        }
      } catch (Exception e) {
        logger.warn("Failed to set tenant context", e);
        // Don't fail the request, just continue without tenant context
      }
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();

    // Skip filter for public endpoints
    return path.startsWith("/actuator/health")
        || path.startsWith("/api/v1/auth/providers")
        || path.startsWith("/api/v1/auth/authorize")
        || path.startsWith("/api/v1/auth/callback")
        || path.startsWith("/api/v1/plans")
        || path.startsWith("/api/v1/webhooks/");
  }
}
