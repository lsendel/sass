package com.platform.auth.api;

import com.platform.auth.internal.OAuth2ProvidersService;
import com.platform.auth.internal.SessionService;
import com.platform.shared.security.InputSanitizer;
import com.platform.shared.security.PasswordProperties;
import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.shared.security.SecurityEventLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling all authentication-related endpoints, including OAuth2 flows,
 * password-based login, session management, and logout.
 */
@RestController
@org.springframework.context.annotation.Profile({"!test", "controller-test"})
@RequestMapping("/api/v1/auth")
@CrossOrigin(
    origins = {"${app.frontend-url}", "http://localhost:3000"},
    allowCredentials = "true")
public class AuthController {

  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

  @Value("${app.frontend-url}")
  private String frontendUrl;

  private final OAuth2ProvidersService providersService;
  private final SessionService sessionService;
  private final PasswordProperties passwordProperties;
  private final SecurityEventLogger securityEventLogger;
  private final InputSanitizer inputSanitizer;

  /**
   * Constructs the AuthController with required services.
   *
   * @param providersService Service for managing OAuth2 providers.
   * @param sessionService Service for managing user sessions and authentication.
   * @param passwordProperties Configuration properties for password-based authentication.
   * @param securityEventLogger Logger for security-related events.
   * @param inputSanitizer Utility for sanitizing user input.
   */
  public AuthController(
      OAuth2ProvidersService providersService,
      SessionService sessionService,
      PasswordProperties passwordProperties,
      SecurityEventLogger securityEventLogger,
      InputSanitizer inputSanitizer) {
    this.providersService = providersService;
    this.sessionService = sessionService;
    this.passwordProperties = passwordProperties;
    this.securityEventLogger = securityEventLogger;
    this.inputSanitizer = inputSanitizer;
  }

  /**
   * Lists the available and configured OAuth2 identity providers.
   *
   * @return A {@link ResponseEntity} containing a list of provider information.
   */
  @GetMapping("/providers")
  public ResponseEntity<Map<String, Object>> listProviders() {
    try {
      List<OAuth2ProvidersService.OAuth2ProviderInfo> providers =
          providersService.getAvailableProviders();
      return ResponseEntity.ok(
          Map.of(
              "providers",
              providers.stream().map(OAuth2ProvidersService.OAuth2ProviderInfo::toMap).toList()));
    } catch (Exception e) {
      logger.error("Error listing OAuth2 providers", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error", "INTERNAL_ERROR",
                  "message", "Failed to retrieve OAuth2 providers"));
    }
  }

  /**
   * Retrieves the authentication methods enabled for the application.
   *
   * @return A {@link ResponseEntity} with a list of methods (e.g., "PASSWORD", "OAUTH2") and
   *     details about the enabled providers.
   */
  @GetMapping("/methods")
  public ResponseEntity<Map<String, Object>> getAuthMethods() {
    try {
      List<String> oauth2Providers =
          providersService.getAvailableProviders().stream().map(provider -> provider.name()).toList();
      boolean passwordAuthEnabled = passwordProperties.isEnabled();
      List<String> methods = passwordAuthEnabled ? List.of("PASSWORD", "OAUTH2") : List.of("OAUTH2");
      return ResponseEntity.ok(
          Map.of(
              "methods",
              methods,
              "passwordAuthEnabled",
              passwordAuthEnabled,
              "oauth2Providers",
              oauth2Providers));
    } catch (Exception e) {
      logger.error("Error retrieving authentication methods", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error", "INTERNAL_ERROR",
                  "message", "Failed to retrieve authentication methods"));
    }
  }

  /**
   * Initiates the OAuth2 authorization flow by redirecting the user to the selected provider's
   * authorization page.
   *
   * @param provider The OAuth2 provider to use (e.g., "google", "github").
   * @param redirect_uri The URI to redirect back to after authorization.
   * @param request The incoming HTTP request.
   * @param response The outgoing HTTP response.
   * @throws IOException if a redirection error occurs.
   */
  @GetMapping("/authorize")
  public void initiateAuthorization(
      @RequestParam String provider,
      @RequestParam String redirect_uri,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    String clientIp = getClientIpAddress(request);
    try {
      provider = inputSanitizer.sanitizeUserInput(provider);
      redirect_uri = inputSanitizer.sanitizeUrl(redirect_uri);
      if (!providersService.isProviderEnabled(provider)) {
        securityEventLogger.logSuspiciousActivity(
            "unknown",
            "invalid_oauth_provider",
            clientIp,
            "Attempted OAuth2 authorization with invalid provider: " + provider);
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response
            .getWriter()
            .write(
                "{\"error\":\"INVALID_PROVIDER\",\"message\":\"OAuth2 provider not supported or configured: %s\"}"
                    .formatted(provider));
        return;
      }
      if (!isValidRedirectUri(redirect_uri)) {
        securityEventLogger.logSuspiciousActivity(
            "unknown",
            "invalid_redirect_uri",
            clientIp,
            "Attempted OAuth2 authorization with invalid redirect URI: " + redirect_uri);
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response
            .getWriter()
            .write("{\"error\":\"INVALID_REDIRECT_URI\",\"message\":\"Invalid redirect URI provided\"}");
        return;
      }
      if (inputSanitizer.containsSecurityThreats(provider)
          || inputSanitizer.containsSecurityThreats(redirect_uri)) {
        securityEventLogger.logSuspiciousActivity(
            "unknown", "security_threat_detected", clientIp, "Security threat detected in OAuth2 parameters");
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response
            .getWriter()
            .write(
                "{\"error\":\"SECURITY_THREAT\",\"message\":\"Security threat detected in request parameters\"}");
        return;
      }
    } catch (IllegalArgumentException e) {
      securityEventLogger.logSuspiciousActivity(
          "unknown",
          "input_validation_failed",
          clientIp,
          "Input validation failed during OAuth2 authorization: " + e.getMessage());
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setContentType("application/json");
      response
          .getWriter()
          .write("{\"error\":\"VALIDATION_ERROR\",\"message\":\"Invalid request parameters\"}");
      return;
    }
    try {
      String state = UUID.randomUUID().toString();
      String authorizationUrl = buildAuthorizationUrl(provider, redirect_uri, state);
      logger.info(
          "Initiating OAuth2 authorization for provider: {} with redirect: {}",
          provider,
          redirect_uri);
      response.sendRedirect(authorizationUrl);
    } catch (Exception e) {
      logger.error("Error initiating OAuth2 authorization", e);
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setContentType("application/json");
      response
          .getWriter()
          .write("{\"error\":\"AUTHORIZATION_ERROR\",\"message\":\"Failed to initiate OAuth2 authorization\"}");
    }
  }

  /**
   * Handles the callback from the OAuth2 provider after the user has granted authorization.
   *
   * @param code The authorization code from the provider.
   * @param state The state parameter for CSRF protection.
   * @param oauth2User The authenticated user principal provided by Spring Security.
   * @param request The incoming HTTP request.
   * @return A {@link ResponseEntity} containing an access token and user information.
   */
  @PostMapping("/callback")
  public ResponseEntity<Map<String, Object>> handleCallback(
      @RequestParam String code,
      @RequestParam(required = false) String state,
      @AuthenticationPrincipal OAuth2User oauth2User,
      HttpServletRequest request) {
    String clientIp = getClientIpAddress(request);
    String userAgent = request.getHeader("User-Agent");
    try {
      code = inputSanitizer.sanitizeToken(code);
      if (state != null) {
        state = inputSanitizer.sanitizeUserInput(state);
      }
      if (oauth2User == null) {
        securityEventLogger.logAuthenticationFailure(
            "unknown", "OAUTH2", clientIp, "OAuth2 authentication failed - no user principal");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "AUTHENTICATION_FAILED", "message", "OAuth2 authentication failed"));
      }
      SessionService.AuthenticationResult result =
          sessionService.handleOAuth2Authentication(oauth2User, clientIp, userAgent);
      securityEventLogger.logAuthenticationSuccess(result.user().getId().toString(), "OAUTH2", clientIp);
      logger.info("OAuth2 callback successful for user: {}", result.user().getId());
      String deviceFingerprint = generateDeviceFingerprint(request);
      return ResponseEntity.ok(
          Map.of(
              "accessToken",
              result.token(),
              "user",
              Map.of(
                  "id", result.user().getId(),
                  "email", result.user().getEmail().getValue(),
                  "name", result.user().getName()),
              "security",
              Map.of(
                  "deviceFingerprint", deviceFingerprint, "loginTime", System.currentTimeMillis())));
    } catch (IllegalArgumentException e) {
      securityEventLogger.logSuspiciousActivity(
          "unknown", "invalid_callback_parameters", clientIp, "Invalid callback parameters: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "VALIDATION_ERROR", "message", "Invalid callback parameters"));
    } catch (Exception e) {
      logger.error("Error handling OAuth2 callback", e);
      securityEventLogger.logAuthenticationFailure(
          "unknown", "OAUTH2", clientIp, "OAuth2 callback processing error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "CALLBACK_ERROR", "message", "Failed to process OAuth2 callback"));
    }
  }

  /**
   * Retrieves information about the current authenticated user's session.
   *
   * @param userPrincipal The authenticated user principal injected by Spring Security.
   * @return A {@link ResponseEntity} with user and session details.
   */
  @GetMapping("/session")
  public ResponseEntity<Map<String, Object>> getCurrentSession(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal) {
    if (userPrincipal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "UNAUTHORIZED", "message", "No valid authentication token provided"));
    }
    try {
      SessionService.SessionInfo sessionInfo = sessionService.getSessionInfo(userPrincipal.getUserId());
      return ResponseEntity.ok(
          Map.of(
              "user",
              Map.of(
                  "id",
                  userPrincipal.getUserId(),
                  "email",
                  userPrincipal.getEmail(),
                  "name",
                  userPrincipal.getName(),
                  "role",
                  userPrincipal.getRole()),
              "organization",
              userPrincipal.hasOrganizationContext()
                  ? Map.of("id", userPrincipal.getOrganizationId(), "slug", userPrincipal.getOrganizationSlug())
                  : null,
              "session",
              Map.of("activeTokens", sessionInfo.activeTokenCount(), "lastActivity", sessionInfo.lastActivity())));
    } catch (Exception e) {
      logger.error("Error retrieving session information", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "SESSION_ERROR", "message", "Failed to retrieve session information"));
    }
  }

  /**
   * Invalidates the current session token, effectively logging the user out.
   *
   * @param userPrincipal The authenticated user principal.
   * @param request The incoming HTTP request.
   * @return A {@link ResponseEntity} with no content.
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal, HttpServletRequest request) {
    String clientIp = getClientIpAddress(request);
    if (userPrincipal != null) {
      try {
        String token = extractTokenFromRequest(request);
        if (token != null) {
          sessionService.revokeToken(token);
          securityEventLogger.logAuthenticationSuccess(
              userPrincipal.getUserId().toString(), "LOGOUT", clientIp);
          logger.info("User {} logged out successfully", userPrincipal.getUserId());
        }
      } catch (Exception e) {
        logger.error("Error during logout", e);
        securityEventLogger.logSuspiciousActivity(
            userPrincipal.getUserId().toString(),
            "logout_error",
            clientIp,
            "Error during logout: " + e.getMessage());
      }
    } else {
      securityEventLogger.logSuspiciousActivity(
          "unknown", "invalid_logout_attempt", clientIp, "Logout attempted without valid session");
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * Handles password-based authentication.
   *
   * @param request The request body containing the user's email and password.
   * @param httpRequest The incoming HTTP request.
   * @return A {@link ResponseEntity} with an access token and user information upon success.
   */
  @PostMapping("/password/login")
  public ResponseEntity<Map<String, Object>> passwordLogin(
      @Valid @RequestBody PasswordLoginRequest request, HttpServletRequest httpRequest) {
    String clientIp = getClientIpAddress(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");
    try {
      String email = inputSanitizer.sanitizeEmail(request.email());
      String password = request.password();
      if (!passwordProperties.isEnabled()) {
        securityEventLogger.logSuspiciousActivity(
            email, "password_auth_disabled", clientIp, "Attempted password login when disabled");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                Map.of(
                    "error", "PASSWORD_AUTH_DISABLED", "message", "Password authentication is not enabled"));
      }
      SessionService.AuthenticationResult result =
          sessionService.handlePasswordAuthentication(email, password, clientIp, userAgent);
      securityEventLogger.logAuthenticationSuccess(result.user().getId().toString(), "PASSWORD", clientIp);
      String deviceFingerprint = generateDeviceFingerprint(httpRequest);
      return ResponseEntity.ok(
          Map.of(
              "accessToken",
              result.token(),
              "user",
              Map.of(
                  "id", result.user().getId(),
                  "email", result.user().getEmail().getValue(),
                  "name", result.user().getName()),
              "security",
              Map.of(
                  "deviceFingerprint", deviceFingerprint, "loginTime", System.currentTimeMillis())));
    } catch (IllegalArgumentException e) {
      securityEventLogger.logAuthenticationFailure(
          request.email(), "PASSWORD", clientIp, "Invalid input: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "VALIDATION_ERROR", "message", "Invalid login credentials"));
    } catch (Exception e) {
      logger.error("Error during password authentication", e);
      securityEventLogger.logAuthenticationFailure(
          request.email(), "PASSWORD", clientIp, "Authentication error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "AUTHENTICATION_FAILED", "message", "Invalid credentials"));
    }
  }

  /**
   * Validates the current session token provided in the Authorization header.
   *
   * @param userPrincipal The authenticated user principal.
   * @param request The incoming HTTP request.
   * @return A {@link ResponseEntity} indicating whether the session is valid and providing its expiry
   *     time.
   */
  @PostMapping("/session/validate")
  public ResponseEntity<Map<String, Object>> validateSession(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal, HttpServletRequest request) {
    if (userPrincipal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("valid", false, "error", "INVALID_SESSION"));
    }
    try {
      String token = extractTokenFromRequest(request);
      boolean isValid = sessionService.isTokenValid(token);
      if (!isValid) {
        securityEventLogger.logSuspiciousActivity(
            userPrincipal.getUserId().toString(),
            "invalid_token_validation",
            getClientIpAddress(request),
            "Token validation failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("valid", false, "error", "TOKEN_INVALID"));
      }
      return ResponseEntity.ok(
          Map.of("valid", true, "userId", userPrincipal.getUserId(), "expiresAt", sessionService.getTokenExpiry(token)));
    } catch (Exception e) {
      logger.error("Error validating session", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("valid", false, "error", "VALIDATION_ERROR"));
    }
  }

  private boolean isValidRedirectUri(String redirectUri) {
    if (redirectUri == null || redirectUri.trim().isEmpty()) {
      return false;
    }
    return redirectUri.startsWith(frontendUrl)
        || redirectUri.startsWith("http://localhost:")
        || redirectUri.startsWith("https://localhost:");
  }

  private String buildAuthorizationUrl(String provider, String redirectUri, String state) {
    String baseUrl = "/oauth2/authorization/" + provider;
    return baseUrl
        + "?redirect_uri="
        + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
        + "&state="
        + URLEncoder.encode(state, StandardCharsets.UTF_8);
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

  private String extractTokenFromRequest(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    return null;
  }

  private String generateDeviceFingerprint(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    String acceptLanguage = request.getHeader("Accept-Language");
    String acceptEncoding = request.getHeader("Accept-Encoding");
    String clientIp = getClientIpAddress(request);
    String fingerprint =
        String.format(
            "%s|%s|%s|%s",
            userAgent != null ? userAgent : "unknown",
            acceptLanguage != null ? acceptLanguage : "unknown",
            acceptEncoding != null ? acceptEncoding : "unknown",
            clientIp);
    return String.valueOf(fingerprint.hashCode());
  }

  private boolean isValidRedirectUriEnhanced(String redirectUri) {
    if (redirectUri == null || redirectUri.trim().isEmpty()) {
      return false;
    }
    try {
      String sanitized = inputSanitizer.sanitizeUrl(redirectUri);
      return sanitized.startsWith(frontendUrl)
          || sanitized.startsWith("http://localhost:")
          || sanitized.startsWith("https://localhost:")
          || sanitized.matches("^https://[a-zA-Z0-9.-]+\\.(dev|test|local)(:[0-9]+)?(/.*)?$");
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * DTO for the password-based login request.
   *
   * @param email The user's email address.
   * @param password The user's raw password.
   */
  public record PasswordLoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
}
