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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.platform.auth.internal.PasswordAuthService;
import com.platform.shared.security.PasswordProperties;
import com.platform.shared.security.PlatformUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API controller for password authentication endpoints. Only enabled when password
 * authentication is configured.
 */
@RestController
@Tag(name = "Authentication", description = "Password-based authentication endpoints")
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
  @com.platform.shared.security.RateLimitingAspect.RateLimited(
    requests = 3,
    window = 300,
    unit = java.util.concurrent.TimeUnit.SECONDS,
    keyPrefix = "auth-register"
  )
  @Operation(
      summary = "Register new user",
      description = "Create a new user account with email and password authentication"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "User registered successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Map.class),
              examples = @ExampleObject(value = """
                  {
                    "success": true,
                    "user": {
                      "id": "123e4567-e89b-12d3-a456-426614174000",
                      "email": "user@example.com",
                      "displayName": "John Doe",
                      "emailVerified": false
                    },
                    "organization": {
                      "id": "123e4567-e89b-12d3-a456-426614174001",
                      "name": "John's Organization"
                    }
                  }
                  """)
          )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "409", description = "Email already exists")
  })
  public ResponseEntity<Map<String, Object>> register(
      @Parameter(description = "User registration data", required = true)
      @Valid @RequestBody RegisterRequest request, 
      HttpServletRequest httpRequest) {
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
                  "id", result.user().id(),
                  "email", result.user().email(),
                  "displayName", result.user().name(),
                  "emailVerified", true)));
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
  @com.platform.shared.security.RateLimitingAspect.RateLimited(
    requests = 5,
    window = 60,
    unit = java.util.concurrent.TimeUnit.SECONDS,
    keyPrefix = "auth-login"
  )
  @Operation(
      summary = "Authenticate user",
      description = "Authenticate user with email and password, returns session cookie"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Authentication successful",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Map.class),
              examples = @ExampleObject(value = """
                  {
                    "success": true,
                    "user": {
                      "id": "123e4567-e89b-12d3-a456-426614174000",
                      "email": "user@example.com",
                      "displayName": "John Doe"
                    },
                    "organization": {
                      "id": "123e4567-e89b-12d3-a456-426614174001",
                      "name": "John's Organization"
                    }
                  }
                  """)
          )
      ),
      @ApiResponse(responseCode = "401", description = "Invalid credentials"),
      @ApiResponse(responseCode = "423", description = "Account locked")
  })
  public ResponseEntity<Map<String, Object>> login(
      @Parameter(description = "User login credentials", required = true)
      @Valid @RequestBody LoginRequest request, 
      HttpServletRequest httpRequest) {
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
                  "id", result.user().id(),
                  "email", result.user().email(),
                  "displayName", result.user().name(),
                  "organization",
                      Map.of(
                          "id", result.user().organizationId(),
                          "name", result.user().organizationName()))));
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
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> changePassword(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal,
      @Valid @RequestBody ChangePasswordRequest request,
      HttpServletRequest httpRequest) {

    if (userPrincipal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              Map.of(
                  "success", false,
                  "error", "UNAUTHORIZED",
                  "message", "Authentication required"));
    }

    String ipAddress = getClientIpAddress(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    PasswordAuthService.PasswordChangeResult result =
        passwordAuthService.changePassword(
            userPrincipal.getUserId(),
            request.currentPassword(),
            request.newPassword(),
            ipAddress,
            userAgent);

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
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 128) String newPassword) {}

  public record EmailVerificationRequest(@NotBlank String token) {}

  public record ResendVerificationRequest(
      @Email @NotBlank String email, @NotBlank UUID organizationId) {}
}
