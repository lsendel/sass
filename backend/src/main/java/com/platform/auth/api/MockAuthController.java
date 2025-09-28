package com.platform.auth.api;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A mock authentication controller activated only during tests.
 *
 * <p>This controller provides a {@code /mock-login} endpoint that bypasses the actual
 * authentication service layer. It is designed to return a successful, predictable response for a
 * hardcoded set of "demo" credentials, which is useful for integration tests or UI tests where a
 * valid session is required without going through the full authentication flow.
 *
 * <p>This controller is only active when the "test" Spring profile is enabled.
 *
 * @see org.springframework.context.annotation.Profile
 */
@RestController
@RequestMapping("/api/v1/auth")
@Profile("test")
public class MockAuthController {

  /**
   * Simulates a user login for testing purposes.
   *
   * <p>This endpoint checks for the hardcoded credentials "demo@example.com" and
   * "DemoPassword123!". If they match, it returns a successful response with a mock access token
   * and user data. Otherwise, it returns a 401 Unauthorized error.
   *
   * @param request A map containing the user's "email" and "password".
   * @return A {@link ResponseEntity} containing a mock authentication token and user data on
   *     success, or an error message on failure.
   */
  @PostMapping("/mock-login")
  public ResponseEntity<Map<String, Object>> mockLogin(
      @Valid @RequestBody Map<String, Object> request) {
    String email = (String) request.get("email");
    String password = (String) request.get("password");

    if ("demo@example.com".equals(email) && "DemoPassword123!".equals(password)) {
      String mockToken = "mock-token-" + UUID.randomUUID();
      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "Authentication successful",
              "token",
              mockToken,
              "user",
              Map.of(
                  "id",
                  UUID.randomUUID().toString(),
                  "email",
                  email,
                  "displayName",
                  "Demo User",
                  "name",
                  "Demo User",
                  "organization",
                  Map.of(
                      "id", "b48e719b-3116-423e-b114-c9791e296a8d", "name", "Demo Organization"))));
    } else {
      return ResponseEntity.status(401)
          .body(
              Map.of(
                  "success", false, "error", "INVALID_CREDENTIALS", "message", "Invalid email or password"));
    }
  }
}