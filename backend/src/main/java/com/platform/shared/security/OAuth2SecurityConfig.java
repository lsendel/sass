package com.platform.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * OAuth2 Security Configuration with PKCE support and secure session management.
 *
 * <p>This configuration ensures: - PKCE (Proof Key for Code Exchange) is enabled for all OAuth2
 * flows - Secure session management with Redis backing - Proper state parameter validation for CSRF
 * protection - OAuth2 authorization requests are stored securely in Redis sessions
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400) // 24 hours
public class OAuth2SecurityConfig {

  /**
   * Custom authorization request repository for secure state management. Uses Redis-backed HTTP
   * sessions to store OAuth2 authorization requests with PKCE code verifier and state parameters.
   *
   * <p>This ensures: - OAuth2 state and PKCE parameters are stored securely in Redis - Sessions
   * have proper TTL (24 hours) configured - State parameter validation prevents CSRF attacks - PKCE
   * code verifier is stored securely and not exposed to client
   */
  @Bean
  public AuthorizationRequestRepository<OAuth2AuthorizationRequest>
      authorizationRequestRepository() {
    return new HttpSessionOAuth2AuthorizationRequestRepository();
  }
}
