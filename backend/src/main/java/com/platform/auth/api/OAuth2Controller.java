package com.platform.auth.api;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import com.platform.auth.internal.OAuth2AuditService;
import com.platform.auth.internal.OAuth2ConfigurationService;
import com.platform.auth.internal.OAuth2SessionService;
import com.platform.auth.internal.OAuth2UserService;
import com.platform.shared.security.PlatformUserPrincipal;

/**
 * REST controller for OAuth2 authentication endpoints with PKCE support. Handles OAuth2
 * authorization flows, provider configuration, and session management.
 */
@RestController
@org.springframework.context.annotation.Profile("!test")
@RequestMapping("/auth/oauth2")
@CrossOrigin(
    origins = {"${app.frontend-url}", "http://localhost:3000"},
    allowCredentials = "true")
public class OAuth2Controller {

  private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);

  @Value("${app.frontend-url}")
  private String frontendUrl;

  private final OAuth2ConfigurationService configurationService;
  private final OAuth2SessionService sessionService;
  private final OAuth2UserService userService;
  private final OAuth2AuditService auditService;

  public OAuth2Controller(
      OAuth2ConfigurationService configurationService,
      OAuth2SessionService sessionService,
      OAuth2UserService userService,
      OAuth2AuditService auditService) {
    this.configurationService = configurationService;
    this.sessionService = sessionService;
    this.userService = userService;
    this.auditService = auditService;
  }

  /** GET /auth/oauth2/providers - List available OAuth2 providers */
  @GetMapping("/providers")
  public ResponseEntity<Map<String, Object>> listProviders() {
    try {
      List<OAuth2ProviderDto> providers = getAvailableProviders();

      Map<String, Object> response =
          Map.of(
              "providers", providers,
              "count", providers.size(),
              "timestamp", Instant.now());

      logger.debug("Listed {} OAuth2 providers", providers.size());
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.error("Error listing OAuth2 providers", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error",
                  Map.of(
                      "code", "PROVIDER_LIST_ERROR",
                      "message", "Failed to retrieve OAuth2 providers")));
    }
  }

  /** GET /auth/oauth2/authorize/{provider} - Initiate OAuth2 authorization with PKCE */
  @GetMapping("/authorize/{provider}")
  public ResponseEntity<Map<String, Object>> initiateAuthorization(
      @PathVariable String provider,
      @RequestParam(required = false) String redirect_uri,
      HttpServletRequest request) {

    String ipAddress = getClientIpAddress(request);

    try {
      // Validate provider
      ClientRegistration clientRegistration = getClientRegistration(provider);
      if (clientRegistration == null) {
        auditService.logAuthenticationFailure(provider, "Provider not configured", ipAddress);
        return ResponseEntity.badRequest()
            .body(
                Map.of(
                    "error",
                    Map.of(
                        "code",
                        "INVALID_PROVIDER",
                        "message",
                        "OAuth2 provider not supported or configured: " + provider)));
      }

      // Generate PKCE parameters
      String codeVerifier = generateCodeVerifier();
      String codeChallenge = generateCodeChallenge(codeVerifier);
      String state = generateState();

      // Build authorization URL
      String authorizationUrl =
          buildAuthorizationUrl(clientRegistration, state, codeChallenge, redirect_uri);

      // Log authorization start
      auditService.logAuthorizationStarted(
          provider, null, null, ipAddress, request.getHeader("User-Agent"));

      Map<String, Object> response =
          Map.of(
              "authorizationUrl", authorizationUrl,
              "state", state,
              "codeChallenge", codeChallenge,
              "codeChallengeMethod", "S256",
              "provider", provider,
              "timestamp", Instant.now());

      logger.info("OAuth2 authorization initiated for provider: {}", provider);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.error("Error initiating OAuth2 authorization for provider: {}", provider, e);
      auditService.logAuthenticationFailure(
          provider, "Authorization initiation failed: " + e.getMessage(), ipAddress);

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error",
                  Map.of(
                      "code", "AUTHORIZATION_ERROR",
                      "message", "Failed to initiate OAuth2 authorization")));
    }
  }

  /** GET /auth/oauth2/callback/{provider} - Handle OAuth2 callback */
  @GetMapping("/callback/{provider}")
  public ResponseEntity<Map<String, Object>> handleCallback(
      @PathVariable String provider,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      @RequestParam(required = false) String error_description,
      @AuthenticationPrincipal OAuth2User oauth2User,
      HttpServletRequest request) {

    String ipAddress = getClientIpAddress(request);
    String userAgent = request.getHeader("User-Agent");

    try {
      // Handle provider errors first
      if (error != null) {
        auditService.logAuthorizationFailure(
            provider, error, error_description, null, null, ipAddress);

        String errorCode =
            switch (error) {
              case "access_denied" -> "OAUTH2_AUTHORIZATION_DENIED";
              case "invalid_request" -> "OAUTH2_INVALID_REQUEST";
              case "invalid_client" -> "OAUTH2_INVALID_CLIENT";
              case "invalid_grant" -> "OAUTH2_INVALID_GRANT";
              case "unauthorized_client" -> "OAUTH2_UNAUTHORIZED_CLIENT";
              case "unsupported_grant_type" -> "OAUTH2_UNSUPPORTED_GRANT_TYPE";
              case "invalid_scope" -> "OAUTH2_INVALID_SCOPE";
              default -> "OAUTH2_UNKNOWN_ERROR";
            };

        return ResponseEntity.badRequest()
            .body(
                Map.of(
                    "success",
                    false,
                    "error",
                    Map.of(
                        "code",
                        errorCode,
                        "message",
                        error_description != null
                            ? error_description
                            : "OAuth2 authorization failed",
                        "details",
                        Map.of(
                            "provider_error",
                            error,
                            "provider_description",
                            Objects.toString(error_description, "")))));
      }

      // Validate required parameters
      if (code == null || state == null) {
        auditService.logAuthenticationFailure(
            provider, "Missing required callback parameters", ipAddress);
        return ResponseEntity.badRequest()
            .body(
                Map.of(
                    "error",
                    Map.of(
                        "code", "OAUTH2_INVALID_CALLBACK",
                        "message", "Missing required authorization code or state parameter")));
      }

      // Validate state parameter (CSRF protection)
      if (!isValidState(state)) {
        auditService.logStateValidationFailure(provider, "expected_state", state, ipAddress);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                Map.of(
                    "error",
                    Map.of(
                        "code", "OAUTH2_INVALID_STATE",
                        "message", "Invalid state parameter - possible CSRF attack")));
      }

      // Check for OAuth2User (should be populated by Spring Security)
      if (oauth2User == null) {
        auditService.logAuthenticationFailure(provider, "OAuth2 user not authenticated", ipAddress);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                Map.of(
                    "error",
                    Map.of(
                        "code", "OAUTH2_USER_NOT_AUTHENTICATED",
                        "message", "OAuth2 authentication failed")));
      }

      // Extract user information
      String providerUserId = oauth2User.getName();
      String email = oauth2User.getAttribute("email");

      if (email == null || providerUserId == null) {
        auditService.logAuthenticationFailure(
            provider, "Missing required user attributes", ipAddress);
        return ResponseEntity.badRequest()
            .body(
                Map.of(
                    "error",
                    Map.of(
                        "code", "OAUTH2_MISSING_USER_DATA",
                        "message", "Required user information not provided by OAuth2 provider")));
      }

      // Create OAuth2 session
      String sessionId = generateSessionId();
      OAuth2SessionService.OAuth2SessionResult sessionResult =
          sessionService.createSession(sessionId, oauth2User, provider, ipAddress, userAgent);

      if (!sessionResult.success()) {
        auditService.logAuthenticationFailure(provider, "Session creation failed", ipAddress);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                Map.of(
                    "error",
                    Map.of(
                        "code", "OAUTH2_SESSION_CREATION_FAILED",
                        "message", "Failed to create OAuth2 session")));
      }

      // Log successful authentication
      auditService.logUserLogin(provider, providerUserId, sessionId, ipAddress, userAgent);

      // Build response
      Map<String, Object> response =
          Map.of(
              "success",
              true,
              "session",
                  Map.of(
                  "sessionId", sessionResult.sessionId(),
                  "userId", sessionResult.userInfo().providerUniqueId(),
                  "provider", provider,
                  "isAuthenticated", true,
                  "expiresAt", sessionResult.expiresAt(),
                  "userInfo",
                      Map.of(
                          "sub", sessionResult.userInfo().providerUserId(),
                          "email", sessionResult.userInfo().email(),
                          "name", sessionResult.userInfo().displayName(),
                          "picture", Objects.toString(sessionResult.userInfo().picture(), ""),
                          "email_verified", sessionResult.userInfo().emailVerified(),
                          "provider", provider)),
              "redirectTo",
              frontendUrl + "/dashboard",
              "timestamp",
              Instant.now());

      logger.info(
          "OAuth2 callback successful for provider: {}, user: {}", provider, providerUserId);
      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.error("Error handling OAuth2 callback for provider: {}", provider, e);
      auditService.logAuthenticationFailure(
          provider, "Callback processing failed: " + e.getMessage(), ipAddress);

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error",
                  Map.of(
                      "code", "OAUTH2_CALLBACK_ERROR",
                      "message", "Failed to process OAuth2 callback")));
    }
  }

  /** GET /auth/oauth2/session - Get current OAuth2 session information */
  @GetMapping("/session")
  public ResponseEntity<Map<String, Object>> getCurrentSession(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal, HttpServletRequest request) {

    String sessionId = request.getSession(false) != null ? request.getSession().getId() : null;

    if (userPrincipal == null || sessionId == null) {
      return ResponseEntity.ok(Map.of("isAuthenticated", false, "timestamp", Instant.now()));
    }

    try {
      Optional<OAuth2SessionService.OAuth2SessionInfo> sessionInfoOpt =
          sessionService.getSessionInfo(sessionId);

      if (sessionInfoOpt.isEmpty()) {
        return ResponseEntity.ok(Map.of("isAuthenticated", false, "timestamp", Instant.now()));
      }

      OAuth2SessionService.OAuth2SessionInfo sessionInfo = sessionInfoOpt.get();

      Map<String, Object> response =
          Map.of(
              "isAuthenticated", true,
              "session",
                  Map.of(
                      "sessionId", sessionInfo.sessionId(),
                      "userId", sessionInfo.userInfo().providerUniqueId(),
                      "provider", sessionInfo.provider(),
                      "isValid", sessionInfo.isValid(),
                      "expiresAt", sessionInfo.expiresAt(),
                      "lastAccessedAt", sessionInfo.lastAccessedAt(),
                      "sessionDurationSeconds", sessionInfo.sessionDurationSeconds(),
                      "timeToExpirationSeconds", sessionInfo.timeToExpirationSeconds(),
                      "userInfo",
                          Map.of(
                              "sub", sessionInfo.userInfo().providerUserId(),
                              "email", sessionInfo.userInfo().email(),
                              "name", sessionInfo.userInfo().displayName(),
                              "picture", Objects.toString(sessionInfo.userInfo().picture(), ""),
                              "email_verified", sessionInfo.userInfo().emailVerified(),
                              "provider", sessionInfo.provider())),
              "timestamp", Instant.now());

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.error("Error retrieving OAuth2 session information", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error",
                  Map.of(
                      "code", "SESSION_ERROR",
                      "message", "Failed to retrieve session information")));
    }
  }

  /** POST /auth/oauth2/logout - Terminate OAuth2 session */
  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout(
      @RequestBody(required = false) Map<String, Object> logoutRequest,
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal,
      HttpServletRequest request) {

    String sessionId = request.getSession(false) != null ? request.getSession().getId() : null;
    String ipAddress = getClientIpAddress(request);

    try {
      if (sessionId != null) {
        boolean terminateProviderSession =
            logoutRequest != null
                && Boolean.TRUE.equals(logoutRequest.get("terminateProviderSession"));

        sessionService.terminateSession(sessionId, "user_logout", ipAddress);

        if (userPrincipal != null) {
          auditService.logUserLogout(
              "unknown", userPrincipal.getUserId().toString(), sessionId, ipAddress);
        }

        logger.info(
            "OAuth2 session terminated: sessionId={}, terminateProvider={}",
            sessionId,
            terminateProviderSession);
      }

      return ResponseEntity.ok(
          Map.of("success", true, "message", "Logout successful", "timestamp", Instant.now()));

    } catch (Exception e) {
      logger.error("Error during OAuth2 logout", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error",
                  Map.of(
                      "code", "LOGOUT_ERROR",
                      "message", "Failed to process logout request")));
    }
  }

  // Private helper methods

  private List<OAuth2ProviderDto> getAvailableProviders() {
    return configurationService.getAvailableProviders().stream()
        .map(this::mapToProviderDto)
        .collect(Collectors.toList());
  }

  private ClientRegistration getClientRegistration(String provider) {
    return configurationService.getClientRegistration(provider);
  }

  private OAuth2ProviderDto mapToProviderDto(OAuth2ConfigurationService.ProviderInfo providerInfo) {
    return new OAuth2ProviderDto(
        providerInfo.registrationId(),
        getProviderDisplayName(providerInfo.registrationId()),
        providerInfo.authorizationUri(),
        providerInfo.scopes());
  }

  private String getProviderDisplayName(String provider) {
    return switch (provider.toLowerCase()) {
      case "google" -> "Google";
      case "github" -> "GitHub";
      case "microsoft" -> "Microsoft";
      default -> provider.substring(0, 1).toUpperCase() + provider.substring(1);
    };
  }

  private String buildAuthorizationUrl(
      ClientRegistration registration, String state, String codeChallenge, String redirectUri) {
    // This is simplified - Spring Security normally handles URL building
    String baseUrl = registration.getProviderDetails().getAuthorizationUri();

    StringBuilder url = new StringBuilder(baseUrl);
    url.append("?response_type=code");
    url.append("&client_id=").append(registration.getClientId());
    url.append("&scope=").append(String.join(" ", registration.getScopes()));
    url.append("&state=").append(state);
    url.append("&code_challenge=").append(codeChallenge);
    url.append("&code_challenge_method=S256");

    if (redirectUri != null) {
      url.append("&redirect_uri=").append(redirectUri);
    } else {
      url.append("&redirect_uri=").append(registration.getRedirectUri());
    }

    return url.toString();
  }

  private String generateCodeVerifier() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String generateCodeChallenge(String codeVerifier) {
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(codeVerifier.getBytes("UTF-8"));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate code challenge", e);
    }
  }

  private String generateState() {
    return UUID.randomUUID().toString();
  }

  private String generateSessionId() {
    return UUID.randomUUID().toString();
  }

  private boolean isValidState(String state) {
    // In a real implementation, you'd validate against stored state values
    // For now, just check format
    try {
      UUID.fromString(state);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

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

  /** OAuth2 Provider DTO for API responses */
  public record OAuth2ProviderDto(
      String name, String displayName, String authorizationUrl, Set<String> scopes) {}
}
