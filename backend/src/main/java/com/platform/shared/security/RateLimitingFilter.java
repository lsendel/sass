package com.platform.shared.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

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

import com.platform.auth.internal.AuthenticationAttempt;
import com.platform.auth.internal.AuthenticationAttemptRepository;

/**
 * Rate limiting filter for all endpoints to prevent abuse and brute force attacks.
 * Uses Redis-backed counters when available with a local fallback.
 */
@Component
public class RateLimitingFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

  private final Map<String, RateLimitRule> rules;
  private final AuthenticationAttemptRepository authenticationAttemptRepository;
  private final RateLimitService rateLimitService;

  public RateLimitingFilter(
      Map<String, RateLimitRule> rules,
      AuthenticationAttemptRepository authenticationAttemptRepository,
      RateLimitService rateLimitService) {
    this.rules = rules;
    this.authenticationAttemptRepository = authenticationAttemptRepository;
    this.rateLimitService = rateLimitService;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    RateLimitRule rule = resolveRule(httpRequest);
    if (rule != null) {
      String clientKey = getClientKey(httpRequest);
      String limiterKey = buildLimiterKey(httpRequest, clientKey, rule);
      long attempts = rateLimitService.increment(limiterKey, rule.window());

      if (attempts > rule.maxAttempts()) {
        logger.warn(
            "Rate limit exceeded for client {} on {} (limit {} in {}s)",
            clientKey,
            httpRequest.getRequestURI(),
            rule.maxAttempts(),
            rule.window().toSeconds());

        recordRateLimitEvent(httpRequest, clientKey);
        sendRateLimitExceededResponse(httpResponse, rule);
        return;
      }
    }

    chain.doFilter(request, response);
  }

  private RateLimitRule resolveRule(HttpServletRequest request) {
    String path = request.getRequestURI();
    String method = request.getMethod();

    for (Map.Entry<String, RateLimitRule> entry : RULES.entrySet()) {
      if (!path.startsWith(entry.getKey())) {
        continue;
      }

      // GET requests only limited for OAuth flows and verification endpoints
      if ("GET".equals(method)) {
        if (OAUTH_ENDPOINTS.stream().noneMatch(path::contains)) {
          return null;
        }
      }

      return entry.getValue();
    }

    return null;
  }

  private String getClientKey(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isBlank()) {
      return ip.split(",")[0].trim();
    }

    ip = request.getHeader("X-Real-IP");
    if (ip != null && !ip.isBlank()) {
      return ip;
    }

    return request.getRemoteAddr();
  }

  private String buildLimiterKey(
      HttpServletRequest request, String clientKey, RateLimitRule rule) {
    return clientKey + '|' + request.getRequestURI() + '|' + rule.maxAttempts();
  }

  private void sendRateLimitExceededResponse(HttpServletResponse response, RateLimitRule rule)
      throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("X-RateLimit-Limit", String.valueOf(rule.maxAttempts()));
    response.setHeader("X-RateLimit-Window", String.valueOf(rule.window().toSeconds()));
    response.setHeader("Retry-After", String.valueOf(rule.window().toSeconds()));

    String body =
        """
            {
                "error": "RATE_LIMIT_EXCEEDED",
                "message": "Too many authentication attempts. Please try again later.",
                "retryAfter": %d
            }
            """
            .formatted(rule.window().toSeconds());

    response.getWriter().write(body);
    response.getWriter().flush();
  }

  private void recordRateLimitEvent(HttpServletRequest request, String clientKey) {
    if (authenticationAttemptRepository == null) {
      return;
    }

    try {
      String endpoint = request.getRequestURI();
      AuthenticationAttempt.AuthenticationMethod method =
          endpoint.contains("/oauth2/")
              ? AuthenticationAttempt.AuthenticationMethod.OAUTH2
              : AuthenticationAttempt.AuthenticationMethod.PASSWORD;

      String email = request.getParameter("email");
      if (email == null || email.isBlank()) {
        email = "limit-" + clientKey.replace(':', '_') + "@rate-limit.local";
      }

      authenticationAttemptRepository.save(
          AuthenticationAttempt.failureUnknownUser(
              email,
              method,
              "RATE_LIMIT_EXCEEDED:" + endpoint,
              clientKey,
              request.getHeader("User-Agent")));
    } catch (Exception ex) {
      logger.debug("Failed to persist rate limit audit event", ex);
    }
  }

  private static final Set<String> OAUTH_ENDPOINTS =
      Set.of("/oauth2/authorize", "/oauth2/callback", "/oauth2/session", "/verify-email");

  private record RateLimitRule(int maxAttempts, Duration window) {
    RateLimitRule {
      if (window == null) {
        window = Duration.ofMinutes(15);
      }
    }
  }
}

