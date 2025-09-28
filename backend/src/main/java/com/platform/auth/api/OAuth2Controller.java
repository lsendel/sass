package com.platform.auth.api;

import com.platform.auth.internal.OAuth2AuditService;
import com.platform.auth.internal.OAuth2ConfigurationService;
import com.platform.auth.internal.OAuth2SessionService;
import com.platform.auth.internal.OAuth2UserService;
import com.platform.shared.security.PlatformUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manages OAuth2 authentication flows, including provider discovery, authorization initiation with
 * PKCE, callback handling, and session management.
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

  /**
   * Constructs the controller with necessary OAuth2 services.
   *
   * @param configurationService Service for accessing OAuth2 provider configurations.
   * @param sessionService Service for managing OAuth2 user sessions.
   * @param userService Service for handling user persistence.
   * @param auditService Service for logging audit events related to OAuth2 flows.
   */
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

  /**
   * Lists all available and configured OAuth2 identity providers.
   *
   * @return A {@link ResponseEntity} containing a list of available providers and their details.
   */
  @GetMapping("/providers")
  public ResponseEntity<Map<String, Object>> listProviders() {
    try {
      List<OAuth2ProviderDto> providers = getAvailableProviders();
      Map<String, Object> response =
          Map.of("providers", providers, "count", providers.size(), "timestamp", Instant.now());
      logger.debug("Listed {} OAuth2 providers", providers.size());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      logger.error("Error listing OAuth2 providers", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error",
                  Map.of("code", "PROVIDER_LIST_ERROR", "message", "Failed to retrieve OAuth2 providers")));
    }
  }

  /**
   * Initiates the OAuth2 authorization flow with Proof Key for Code Exchange (PKCE).
   *
   * @param provider The name of the OAuth2 provider (e.g., "google").
   * @param redirect_uri Optional override for the redirect URI.
   * @param request The incoming HTTP request.
   * @return A {@link ResponseEntity} containing the authorization URL and PKCE parameters.
   */
  @GetMapping("/authorize/{provider}")
  public ResponseEntity<Map<String, Object>> initiateAuthorization(
      @PathVariable String provider,
      @RequestParam(required = false) String redirect_uri,
      HttpServletRequest request) {
    String ipAddress = getClientIpAddress(request);
    try {
      ClientRegistration clientRegistration = getClientRegistration(provider);
      if (clientRegistration == null) {
        auditService.logAuthenticationFailure(provider, "Provider not configured", ipAddress);
        return ResponseEntity.badRequest()
            .body(
                Map.of(
                    "error",
                    Map.of(
                        "code", "INVALID_PROVIDER",
                        "message", "OAuth2 provider not supported or configured: " + provider)));
      }
      String codeVerifier = generateCodeVerifier();
      String codeChallenge = generateCodeChallenge(codeVerifier);
      String state = generateState();
      String authorizationUrl =
          buildAuthorizationUrl(clientRegistration, state, codeChallenge, redirect_uri);
      auditService.logAuthorizationStarted(
          provider, null, null, ipAddress, request.getHeader("User-Agent"));
      Map<String, Object> response =
          Map.of(
              "authorizationUrl",
              authorizationUrl,
              "state",
              state,
              "codeChallenge",
              codeChallenge,
              "codeChallengeMethod",
              "S256",
              "provider",
              provider,
              "timestamp",
              Instant.now());
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
                  Map.of("code", "AUTHORIZATION_ERROR", "message", "Failed to initiate OAuth2 authorization")));
    }
  }

  /**
   * Handles the callback from the OAuth2 provider after user authorization.
   *
   * @param provider The OAuth2 provider name.
   * @param code The authorization code granted by the provider.
   * @param state The state parameter for CSRF protection.
   * @param error An error code if authorization failed.
   * @param error_description A description of the error.
   * @param oauth2User The authenticated user principal from Spring Security.
   * @param request The incoming HTTP request.
   * @return A {@link ResponseEntity} with session information on success, or an error on failure.
   */
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
      if (error != null) {
        auditService.logAuthorizationFailure(provider, error, error_description, null, null, ipAddress);
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
                        error_description != null ? error_description : "OAuth2 authorization failed",
                        "details",
                        Map.of(
                            "provider_error",
                            error,
                            "provider_description",
                            Objects.toString(error_description, "")))));
      }
      if (code == null || state == null) {
        auditService.logAuthenticationFailure(provider, "Missing required callback parameters", ipAddress);
        return ResponseEntity.badRequest()
            .body(
                Map.of(
                    "error",
                    Map.of(
                        "code", "OAUTH2_INVALID_CALLBACK",
                        "message", "Missing required authorization code or state parameter")));
      }
      if (!isValidState(state)) {
        auditService.logStateValidationFailure(provider, "expected_state", state, ipAddress);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                Map.of(
                    "error",
                    Map.of("code", "OAUTH2_INVALID_STATE", "message", "Invalid state parameter - possible CSRF attack")));
      }
      if (oauth2User == null) {
        auditService.logAuthenticationFailure(provider, "OAuth2 user not authenticated", ipAddress);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                Map.of(
                    "error",
                    Map.of("code", "OAUTH2_USER_NOT_AUTHENTICATED", "message", "OAuth2 authentication failed")));
      }
      String providerUserId = oauth2User.getName();
      String email = oauth2User.getAttribute("email");
      if (email == null || providerUserId == null) {
        auditService.logAuthenticationFailure(provider, "Missing required user attributes", ipAddress);
        return ResponseEntity.badRequest()
            .body(
                Map.of(
                    "error",
                    Map.of(
                        "code", "OAUTH2_MISSING_USER_DATA",
                        "message", "Required user information not provided by OAuth2 provider")));
      }
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
                        "code", "OAUTH2_SESSION_CREATION_FAILED", "message", "Failed to create OAuth2 session")));
      }
      auditService.logUserLogin(provider, providerUserId, sessionId, ipAddress, userAgent);
      Map<String, Object> response =
          Map.of(
              "success",
              true,
              "session",
              Map.of(
                  "sessionId",
                  sessionResult.sessionId(),
                  "userId",
                  sessionResult.userInfo().providerUniqueId(),
                  "provider",
                  provider,
                  "isAuthenticated",
                  true,
                  "expiresAt",
                  sessionResult.expiresAt(),
                  "userInfo",
                  Map.of(
                      "sub",
                      sessionResult.userInfo().providerUserId(),
                      "email",
                      sessionResult.userInfo().email(),
                      "name",
                      sessionResult.userInfo().displayName(),
                      "picture",
                      Objects.toString(sessionResult.userInfo().picture(), ""),
                      "email_verified",
                      sessionResult.userInfo().emailVerified(),
                      "provider",
                      provider)),
              "redirectTo",
              frontendUrl + "/dashboard",
              "timestamp",
              Instant.now());
      logger.info("OAuth2 callback successful for provider: {}, user: {}", provider, providerUserId);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      logger.error("Error handling OAuth2 callback for provider: {}", provider, e);
      auditService.logAuthenticationFailure(
          provider, "Callback processing failed: " + e.getMessage(), ipAddress);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error",
                  Map.of("code", "OAUTH2_CALLBACK_ERROR", "message", "Failed to process OAuth2 callback")));
    }
  }

  /**
   * Retrieves information about the current OAuth2 session.
   *
   * @param userPrincipal The authenticated user principal.
   * @param request The incoming HTTP request.
   * @return A {@link ResponseEntity} with session details, or an unauthenticated status.
   */
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
              "isAuthenticated",
              true,
              "session",
              Map.of(
                  "sessionId",
                  sessionInfo.sessionId(),
                  "userId",
                  sessionInfo.userInfo().providerUniqueId(),
                  "provider",
                  sessionInfo.provider(),
                  "isValid",
                  sessionInfo.isValid(),
                  "expiresAt",
                  sessionInfo.expiresAt(),
                  "lastAccessedAt",
                  sessionInfo.lastAccessedAt(),
                  "sessionDurationSeconds",
                  sessionInfo.sessionDurationSeconds(),
                  "timeToExpirationSeconds",
                  sessionInfo.timeToExpirationSeconds(),
                  "userInfo",
                  Map.of(
                      "sub",
                      sessionInfo.userInfo().providerUserId(),
                      "email",
                      sessionInfo.userInfo().email(),
                      "name",
                      sessionInfo.userInfo().displayName(),
                      "picture",
                      Objects.toString(sessionInfo.userInfo().picture(), ""),
                      "email_verified",
                      sessionInfo.userInfo().emailVerified(),
                      "provider",
                      sessionInfo.provider())),
              "timestamp",
              Instant.now());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      logger.error("Error retrieving OAuth2 session information", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error", Map.of("code", "SESSION_ERROR", "message", "Failed to retrieve session information")));
    }
  }

  /**
   * Terminates the current OAuth2 session.
   *
   * @param logoutRequest Optional request body, can be used to specify additional logout options.
   * @param userPrincipal The authenticated user principal.
   * @param request The incoming HTTP request.
   * @return A {@link ResponseEntity} indicating the result of the logout operation.
   */
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
            logoutRequest != null && Boolean.TRUE.equals(logoutRequest.get("terminateProviderSession"));
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
              Map.of("error", Map.of("code", "LOGOUT_ERROR", "message", "Failed to process logout request")));
    }
  }

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

  /**
   * Data Transfer Object for exposing OAuth2 provider information to clients.
   *
   * @param name The registration ID of the provider (e.g., "google").
   * @param displayName A user-friendly name for the provider (e.g., "Google").
   * @param authorizationUrl The provider's authorization endpoint URL.
   * @param scopes The set of scopes requested from the provider.
   */
  public record OAuth2ProviderDto(
      String name, String displayName, String authorizationUrl, Set<String> scopes) {}
}
