package com.platform.auth.api;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

/** REST controller for authentication endpoints including OAuth2 flows. */
@RestController
@org.springframework.context.annotation.Profile("!test")
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

  public AuthController(
      OAuth2ProvidersService providersService,
      SessionService sessionService,
      PasswordProperties passwordProperties) {
    this.providersService = providersService;
    this.sessionService = sessionService;
    this.passwordProperties = passwordProperties;
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
      HttpServletResponse response)
      throws IOException {

    // Validate provider
    if (!providersService.isProviderEnabled(provider)) {
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

    // Validate redirect URI (basic validation)
    if (!isValidRedirectUri(redirect_uri)) {
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

    try {
      if (oauth2User == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                Map.of(
                    "error", "AUTHENTICATION_FAILED",
                    "message", "OAuth2 authentication failed"));
      }

      // Create or update user and generate token
      SessionService.AuthenticationResult result =
          sessionService.handleOAuth2Authentication(
              oauth2User, getClientIpAddress(request), request.getHeader("User-Agent"));

      logger.info("OAuth2 callback successful for user: {}", result.user().getId());

      return ResponseEntity.ok(
          Map.of(
              "accessToken", result.token(),
              "user",
                  Map.of(
                      "id", result.user().getId(),
                      "email", result.user().getEmail().getValue(),
                      "name", result.user().getName())));

    } catch (Exception e) {
      logger.error("Error handling OAuth2 callback", e);
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

    if (userPrincipal != null) {
      try {
        String token = extractTokenFromRequest(request);
        if (token != null) {
          sessionService.revokeToken(token);
          logger.info("User {} logged out successfully", userPrincipal.getUserId());
        }
      } catch (Exception e) {
        logger.error("Error during logout", e);
        // Continue with logout even if token revocation fails
      }
    }

    return ResponseEntity.noContent().build();
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
}
