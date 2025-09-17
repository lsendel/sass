package com.platform.auth.api;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.platform.auth.internal.PasswordAuthService;
import com.platform.shared.security.PasswordProperties;

/**
 * REST API controller for password authentication endpoints. Only enabled when password
 * authentication is configured.
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "app.auth.password.enabled", havingValue = "true")
public class PasswordAuthController {

  private static final Logger log = LoggerFactory.getLogger(PasswordAuthController.class);

  private final PasswordAuthService passwordAuthService;
  private final PasswordProperties passwordProperties;

  @Autowired
  public PasswordAuthController(
      PasswordAuthService passwordAuthService, PasswordProperties passwordProperties) {
    this.passwordAuthService = passwordAuthService;
    this.passwordProperties = passwordProperties;
  }

  /** POST /auth/register - Register new user with password */
  @PostMapping("/register")
  public ResponseEntity<Map<String, Object>> register(
      @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
    String ipAddress = getClientIpAddress(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    PasswordAuthService.PasswordRegistrationResult result =
        passwordAuthService.registerUser(
            request.email(),
            request.password(),
            request.displayName(),
            request.organizationId(),
            ipAddress,
            userAgent);

    if (result.success()) {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(
              Map.of(
                  "success",
                  true,
                  "message",
                  "Registration successful. Please check your email for verification.",
                  "user",
                  Map.of(
                      "id", result.user().getId(),
                      "email", result.user().getEmail(),
                      "displayName", result.user().getName(),
                      "emailVerified", result.user().getEmailVerified())));
    } else {
      HttpStatus status =
          switch (result.errorType()) {
            case EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case INVALID_PASSWORD -> HttpStatus.BAD_REQUEST;
            case ORGANIZATION_NOT_FOUND -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
          };

      return ResponseEntity.status(status)
          .body(
              Map.of(
                  "success",
                  false,
                  "error",
                  result.errorType().name(),
                  "message",
                  result.errorMessage() != null ? result.errorMessage() : "Registration failed"));
    }
  }

  /** POST /auth/login - Authenticate user with email and password */
  @PostMapping("/login")
  public ResponseEntity<Map<String, Object>> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    String ipAddress = getClientIpAddress(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    PasswordAuthService.PasswordAuthenticationResult result =
        passwordAuthService.authenticateUser(
            request.email(), request.password(), request.organizationId(), ipAddress, userAgent);

    if (result.success()) {
      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "Authentication successful",
              "user",
              Map.of(
                  "id", result.user().getId(),
                  "email", result.user().getEmail(),
                  "displayName", result.user().getName(),
                  "organization",
                      Map.of(
                          "id", result.user().getOrganization().getId(),
                          "name", result.user().getOrganization().getName()))));
    } else {
      HttpStatus status =
          switch (result.errorType()) {
            case USER_NOT_FOUND, INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case ACCOUNT_LOCKED -> HttpStatus.LOCKED;
            case EMAIL_NOT_VERIFIED -> HttpStatus.FORBIDDEN;
            case INVALID_AUTHENTICATION_METHOD -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
          };

      return ResponseEntity.status(status)
          .body(
              Map.of(
                  "success", false,
                  "error", result.errorType().name(),
                  "message", getErrorMessage(result.errorType())));
    }
  }

  /** POST /auth/request-password-reset - Request password reset email */
  @PostMapping("/request-password-reset")
  public ResponseEntity<Map<String, Object>> requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
    String ipAddress = getClientIpAddress(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    PasswordAuthService.PasswordResetRequestResult result =
        passwordAuthService.requestPasswordReset(
            request.email(), request.organizationId(), ipAddress, userAgent);

    // Always return success for security (don't reveal if email exists)
    return ResponseEntity.ok(Map.of("success", true, "message", result.message()));
  }

  /** POST /auth/reset-password - Reset password using token */
  @PostMapping("/reset-password")
  public ResponseEntity<Map<String, Object>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
    String ipAddress = getClientIpAddress(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    PasswordAuthService.PasswordResetResult result =
        passwordAuthService.resetPassword(
            request.token(), request.newPassword(), ipAddress, userAgent);

    if (result.success()) {
      return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successful"));
    } else {
      HttpStatus status =
          switch (result.errorType()) {
            case INVALID_TOKEN -> HttpStatus.BAD_REQUEST;
            case INVALID_PASSWORD -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
          };

      return ResponseEntity.status(status)
          .body(
              Map.of(
                  "success", false,
                  "error", result.errorType().name(),
                  "message", getPasswordResetErrorMessage(result.errorType())));
    }
  }

  /** POST /auth/change-password - Change password for authenticated user */
  @PostMapping("/change-password")
  public ResponseEntity<Map<String, Object>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
    String ipAddress = getClientIpAddress(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    // TODO: Extract user ID from authentication context
    UUID userId = request.userId(); // This should come from security context

    PasswordAuthService.PasswordChangeResult result =
        passwordAuthService.changePassword(
            userId, request.currentPassword(), request.newPassword(), ipAddress, userAgent);

    if (result.success()) {
      return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));
    } else {
      HttpStatus status =
          switch (result.errorType()) {
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_CURRENT_PASSWORD -> HttpStatus.BAD_REQUEST;
            case INVALID_NEW_PASSWORD -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
          };

      return ResponseEntity.status(status)
          .body(
              Map.of(
                  "success", false,
                  "error", result.errorType().name(),
                  "message", getPasswordChangeErrorMessage(result.errorType())));
    }
  }

  /** POST /auth/verify-email - Verify email using verification token */
  @PostMapping("/verify-email")
  public ResponseEntity<Map<String, Object>> verifyEmail(
      @Valid @RequestBody EmailVerificationRequest request, HttpServletRequest httpRequest) {
    String ipAddress = getClientIpAddress(httpRequest);

    PasswordAuthService.EmailVerificationResult result =
        passwordAuthService.verifyEmail(request.token(), ipAddress);

    if (result.success()) {
      return ResponseEntity.ok(Map.of("success", true, "message", "Email verified successfully"));
    } else {
      HttpStatus status =
          switch (result.errorType()) {
            case INVALID_TOKEN -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
          };

      return ResponseEntity.status(status)
          .body(
              Map.of(
                  "success",
                  false,
                  "error",
                  result.errorType().name(),
                  "message",
                  "Invalid or expired verification token"));
    }
  }

  /** POST /auth/resend-verification - Resend email verification */
  @PostMapping("/resend-verification")
  public ResponseEntity<Map<String, Object>> resendVerification(
      @Valid @RequestBody ResendVerificationRequest request, HttpServletRequest httpRequest) {
    // TODO: Implement resend verification logic
    return ResponseEntity.ok(
        Map.of(
            "success",
            true,
            "message",
            "If the email exists and is unverified, a verification email has been sent."));
  }

  // Helper methods

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

  private String getErrorMessage(PasswordAuthService.AuthenticationError errorType) {
    return switch (errorType) {
      case USER_NOT_FOUND, INVALID_CREDENTIALS -> "Invalid email or password";
      case ACCOUNT_LOCKED -> "Account is temporarily locked due to too many failed login attempts";
      case EMAIL_NOT_VERIFIED -> "Please verify your email address before logging in";
      case INVALID_AUTHENTICATION_METHOD -> "Password authentication is not available for this account";
      case INTERNAL_ERROR -> "An internal error occurred. Please try again later.";
    };
  }

  private String getPasswordResetErrorMessage(PasswordAuthService.PasswordResetError errorType) {
    return switch (errorType) {
      case INVALID_TOKEN -> "Invalid or expired reset token";
      case INVALID_PASSWORD -> "Password does not meet security requirements";
      case INTERNAL_ERROR -> "An internal error occurred. Please try again later.";
    };
  }

  private String getPasswordChangeErrorMessage(PasswordAuthService.PasswordChangeError errorType) {
    return switch (errorType) {
      case USER_NOT_FOUND -> "User not found";
      case INVALID_CURRENT_PASSWORD -> "Current password is incorrect";
      case INVALID_NEW_PASSWORD -> "New password does not meet security requirements";
      case INTERNAL_ERROR -> "An internal error occurred. Please try again later.";
    };
  }

  // Request DTOs

  public record RegisterRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 128) String password,
      @NotBlank @Size(max = 255) String displayName,
      @NotBlank UUID organizationId) {}

  public record LoginRequest(
      @Email @NotBlank String email, @NotBlank String password, @NotBlank UUID organizationId) {}

  public record PasswordResetRequest(
      @Email @NotBlank String email, @NotBlank UUID organizationId) {}

  public record ResetPasswordRequest(
      @NotBlank String token, @NotBlank @Size(min = 8, max = 128) String newPassword) {}

  public record ChangePasswordRequest(
      @NotBlank UUID userId, // TODO: Extract from security context
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 128) String newPassword) {}

  public record EmailVerificationRequest(@NotBlank String token) {}

  public record ResendVerificationRequest(
      @Email @NotBlank String email, @NotBlank UUID organizationId) {}
}
