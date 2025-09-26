package com.platform.auth.api;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import com.platform.auth.internal.OAuth2ProvidersService;
import com.platform.auth.internal.SessionService;
import com.platform.shared.security.PasswordProperties;
import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.shared.security.SecurityEventLogger;
import com.platform.shared.security.InputSanitizer;

/** REST controller for authentication endpoints including OAuth2 flows. */
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

  /** GET /auth/providers - List available OAuth2 providers */
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

  /** GET /auth/methods - Get available authentication methods */
  @GetMapping("/methods")
  public ResponseEntity<Map<String, Object>> getAuthMethods() {
    try {
      List<String> oauth2Providers =
          providersService.getAvailableProviders().stream()
              .map(provider -> provider.name())
              .toList();

      boolean passwordAuthEnabled = passwordProperties.isEnabled();
      List<String> methods;
      if (passwordAuthEnabled) {
        methods = List.of("PASSWORD", "OAUTH2"); // NOSONAR
      } else {
        methods = List.of("OAUTH2");
      }

      return ResponseEntity.ok(
          Map.of(
              "methods", methods,
              "passwordAuthEnabled", passwordAuthEnabled,
              "oauth2Providers", oauth2Providers));
    } catch (Exception e) {
      logger.error("Error retrieving authentication methods", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error", "INTERNAL_ERROR",
                  "message", "Failed to retrieve authentication methods"));
    }
  }

  /** GET /auth/authorize - Initiate OAuth2 authorization */
  @GetMapping("/authorize")
  public void initiateAuthorization(
      @RequestParam String provider,
      @RequestParam String redirect_uri,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {

    String clientIp = getClientIpAddress(request);
    String userAgent = request.getHeader("User-Agent");

    try {
      // Sanitize and validate inputs
      provider = inputSanitizer.sanitizeUserInput(provider);
      redirect_uri = inputSanitizer.sanitizeUrl(redirect_uri);

      // Validate provider
      if (!providersService.isProviderEnabled(provider)) {
        securityEventLogger.logSuspiciousActivity(
            "unknown", "invalid_oauth_provider", clientIp,
            "Attempted OAuth2 authorization with invalid provider: " + provider);

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response
            .getWriter()
            .write(
                """
                  {
                      "error": "INVALID_PROVIDER",
                      "message": "OAuth2 provider not supported or configured: %s"
                  }
                  """
                    .formatted(provider));
        return;
      }

      // Validate redirect URI (enhanced validation)
      if (!isValidRedirectUri(redirect_uri)) {
        securityEventLogger.logSuspiciousActivity(
            "unknown", "invalid_redirect_uri", clientIp,
            "Attempted OAuth2 authorization with invalid redirect URI: " + redirect_uri);

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response
            .getWriter()
            .write(
                """
                  {
                      "error": "INVALID_REDIRECT_URI",
                      "message": "Invalid redirect URI provided"
                  }
                  """);
        return;
      }

      // Check for security threats in parameters
      if (inputSanitizer.containsSecurityThreats(provider) ||
          inputSanitizer.containsSecurityThreats(redirect_uri)) {
        securityEventLogger.logSuspiciousActivity(
            "unknown", "security_threat_detected", clientIp,
            "Security threat detected in OAuth2 parameters");

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response
            .getWriter()
            .write(
                """
                  {
                      "error": "SECURITY_THREAT",
                      "message": "Security threat detected in request parameters"
                  }
                  """);
        return;
      }
    } catch (IllegalArgumentException e) {
      securityEventLogger.logSuspiciousActivity(
          "unknown", "input_validation_failed", clientIp,
          "Input validation failed during OAuth2 authorization: " + e.getMessage());

      response.setStatus(HttpStatus.BAD_REQUEST.value());
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              """
                {
                    "error": "VALIDATION_ERROR",
                    "message": "Invalid request parameters"
                }
                """);
      return;
    }

    try {
      // Store the original redirect URI in session for later use
      // This would be enhanced with proper state parameter handling
      String state = UUID.randomUUID().toString();

      // Build OAuth2 authorization URL
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
          .write(
              """
                {
                    "error": "AUTHORIZATION_ERROR",
                    "message": "Failed to initiate OAuth2 authorization"
                }
                """);
    }
  }

  /** POST /auth/callback - Handle OAuth2 callback */
  @PostMapping("/callback")
  public ResponseEntity<Map<String, Object>> handleCallback(
      @RequestParam String code,
      @RequestParam(required = false) String state,
      @AuthenticationPrincipal OAuth2User oauth2User,
      HttpServletRequest request) {

    String clientIp = getClientIpAddress(request);
    String userAgent = request.getHeader("User-Agent");

    try {
      // Sanitize input parameters
      code = inputSanitizer.sanitizeToken(code);
      if (state != null) {
        state = inputSanitizer.sanitizeUserInput(state);
      }

      if (oauth2User == null) {
        securityEventLogger.logAuthenticationFailure(
            "unknown", "OAUTH2", clientIp, "OAuth2 authentication failed - no user principal");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                Map.of(
                    "error", "AUTHENTICATION_FAILED",
                    "message", "OAuth2 authentication failed"));
      }

      // Create or update user and generate token
      SessionService.AuthenticationResult result =
          sessionService.handleOAuth2Authentication(
              oauth2User, clientIp, userAgent);

      // Log successful authentication
      securityEventLogger.logAuthenticationSuccess(
          result.user().getId().toString(), "OAUTH2", clientIp);

      logger.info("OAuth2 callback successful for user: {}", result.user().getId());

      // Create device fingerprint for security monitoring
      String deviceFingerprint = generateDeviceFingerprint(request);

      return ResponseEntity.ok(
          Map.of(
              "accessToken", result.token(),
              "user",
                  Map.of(
                      "id", result.user().getId(),
                      "email", result.user().getEmail().getValue(),
                      "name", result.user().getName()),
              "security",
                  Map.of(
                      "deviceFingerprint", deviceFingerprint,
                      "loginTime", System.currentTimeMillis())));

    } catch (IllegalArgumentException e) {
      securityEventLogger.logSuspiciousActivity(
          "unknown", "invalid_callback_parameters", clientIp,
          "Invalid callback parameters: " + e.getMessage());

      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(
              Map.of(
                  "error", "VALIDATION_ERROR",
                  "message", "Invalid callback parameters"));

    } catch (Exception e) {
      logger.error("Error handling OAuth2 callback", e);

      securityEventLogger.logAuthenticationFailure(
          "unknown", "OAUTH2", clientIp, "OAuth2 callback processing error: " + e.getMessage());

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error", "CALLBACK_ERROR",
                  "message", "Failed to process OAuth2 callback"));
    }
  }

  /** GET /auth/session - Get current user session */
  @GetMapping("/session")
  public ResponseEntity<Map<String, Object>> getCurrentSession(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal) {

    if (userPrincipal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              Map.of(
                  "error", "UNAUTHORIZED",
                  "message", "No valid authentication token provided"));
    }

    try {
      SessionService.SessionInfo sessionInfo =
          sessionService.getSessionInfo(userPrincipal.getUserId());

      return ResponseEntity.ok(
          Map.of(
              "user",
                  Map.of(
                      "id", userPrincipal.getUserId(),
                      "email", userPrincipal.getEmail(),
                      "name", userPrincipal.getName(),
                      "role", userPrincipal.getRole()),
              "organization",
                  userPrincipal.hasOrganizationContext()
                      ? Map.of(
                          "id", userPrincipal.getOrganizationId(),
                          "slug", userPrincipal.getOrganizationSlug())
                      : null,
              "session",
                  Map.of(
                      "activeTokens", sessionInfo.activeTokenCount(),
                      "lastActivity", sessionInfo.lastActivity())));

    } catch (Exception e) {
      logger.error("Error retrieving session information", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              Map.of(
                  "error", "SESSION_ERROR",
                  "message", "Failed to retrieve session information"));
    }
  }

  /** POST /auth/logout - Invalidate current session */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal, HttpServletRequest request) {

    String clientIp = getClientIpAddress(request);

    if (userPrincipal != null) {
      try {
        String token = extractTokenFromRequest(request);
        if (token != null) {
          sessionService.revokeToken(token);

          // Log successful logout
          securityEventLogger.logAuthenticationSuccess(
              userPrincipal.getUserId().toString(), "LOGOUT", clientIp);

          logger.info("User {} logged out successfully", userPrincipal.getUserId());
        }
      } catch (Exception e) {
        logger.error("Error during logout", e);

        // Log logout error but continue
        securityEventLogger.logSuspiciousActivity(
            userPrincipal.getUserId().toString(), "logout_error", clientIp,
            "Error during logout: " + e.getMessage());
      }
    } else {
      // Log logout attempt without valid session
      securityEventLogger.logSuspiciousActivity(
          "unknown", "invalid_logout_attempt", clientIp,
          "Logout attempted without valid session");
    }

    return ResponseEntity.noContent().build();
  }

  /** POST /auth/password/login - Password-based authentication */
  @PostMapping("/password/login")
  public ResponseEntity<Map<String, Object>> passwordLogin(
      @Valid @RequestBody PasswordLoginRequest request,
      HttpServletRequest httpRequest) {

    String clientIp = getClientIpAddress(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    try {
      // Sanitize inputs
      String email = inputSanitizer.sanitizeEmail(request.email());
      String password = request.password(); // Don't sanitize passwords as they may contain special chars

      // Validate password authentication is enabled
      if (!passwordProperties.isEnabled()) {
        securityEventLogger.logSuspiciousActivity(
            email, "password_auth_disabled", clientIp,
            "Attempted password login when disabled");

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "error", "PASSWORD_AUTH_DISABLED",
                "message", "Password authentication is not enabled"));
      }

      // Handle authentication through session service
      SessionService.AuthenticationResult result =
          sessionService.handlePasswordAuthentication(email, password, clientIp, userAgent);

      // Log successful authentication
      securityEventLogger.logAuthenticationSuccess(
          result.user().getId().toString(), "PASSWORD", clientIp);

      String deviceFingerprint = generateDeviceFingerprint(httpRequest);

      return ResponseEntity.ok(
          Map.of(
              "accessToken", result.token(),
              "user",
                  Map.of(
                      "id", result.user().getId(),
                      "email", result.user().getEmail().getValue(),
                      "name", result.user().getName()),
              "security",
                  Map.of(
                      "deviceFingerprint", deviceFingerprint,
                      "loginTime", System.currentTimeMillis())));

    } catch (IllegalArgumentException e) {
      securityEventLogger.logAuthenticationFailure(
          request.email(), "PASSWORD", clientIp, "Invalid input: " + e.getMessage());

      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of(
              "error", "VALIDATION_ERROR",
              "message", "Invalid login credentials"));

    } catch (Exception e) {
      logger.error("Error during password authentication", e);

      securityEventLogger.logAuthenticationFailure(
          request.email(), "PASSWORD", clientIp, "Authentication error: " + e.getMessage());

      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of(
              "error", "AUTHENTICATION_FAILED",
              "message", "Invalid credentials"));
    }
  }

  /** POST /auth/session/validate - Validate current session token */
  @PostMapping("/session/validate")
  public ResponseEntity<Map<String, Object>> validateSession(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal,
      HttpServletRequest request) {

    if (userPrincipal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of(
              "valid", false,
              "error", "INVALID_SESSION"));
    }

    try {
      String token = extractTokenFromRequest(request);
      boolean isValid = sessionService.isTokenValid(token);

      if (!isValid) {
        securityEventLogger.logSuspiciousActivity(
            userPrincipal.getUserId().toString(), "invalid_token_validation",
            getClientIpAddress(request), "Token validation failed");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "valid", false,
                "error", "TOKEN_INVALID"));
      }

      return ResponseEntity.ok(Map.of(
          "valid", true,
          "userId", userPrincipal.getUserId(),
          "expiresAt", sessionService.getTokenExpiry(token)));

    } catch (Exception e) {
      logger.error("Error validating session", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of(
              "valid", false,
              "error", "VALIDATION_ERROR"));
    }
  }

  // Helper methods

  private boolean isValidRedirectUri(String redirectUri) {
    if (redirectUri == null || redirectUri.trim().isEmpty()) {
      return false;
    }

    // Basic validation - should be enhanced based on security requirements
    return redirectUri.startsWith(frontendUrl)
        || redirectUri.startsWith("http://localhost:")
        || redirectUri.startsWith("https://localhost:");
  }

  private String buildAuthorizationUrl(String provider, String redirectUri, String state) {
    // This is a simplified version - Spring Security OAuth2 normally handles this
    // In a real implementation, you'd use OAuth2AuthorizationRequestResolver
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

    // Create a simple fingerprint hash (in production, use more sophisticated fingerprinting)
    String fingerprint = String.format("%s|%s|%s|%s",
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
      // Sanitize the URI first
      String sanitized = inputSanitizer.sanitizeUrl(redirectUri);

      // Enhanced validation
      return sanitized.startsWith(frontendUrl)
          || sanitized.startsWith("http://localhost:")
          || sanitized.startsWith("https://localhost:")
          || sanitized.matches("^https://[a-zA-Z0-9.-]+\\.(dev|test|local)(:[0-9]+)?(/.*)?$");
    } catch (Exception e) {
      return false;
    }
  }

  // DTO classes for request validation
  public record PasswordLoginRequest(
      @NotBlank @Email String email,
      @NotBlank String password
  ) {}
}
