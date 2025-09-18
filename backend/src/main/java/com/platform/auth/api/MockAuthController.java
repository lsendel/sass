package com.platform.auth.api;

import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Mock authentication controller for testing when PasswordAuthService is not available.
 * This bypasses the service layer and returns a successful response for demo credentials.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Profile("test")
public class MockAuthController {

    @PostMapping("/mock-login")
    public ResponseEntity<Map<String, Object>> mockLogin(@Valid @RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String password = (String) request.get("password");

        // Check for demo credentials
        if ("demo@example.com".equals(email) && "DemoPassword123!".equals(password)) {
            // Generate a mock token
            String mockToken = "mock-token-" + UUID.randomUUID().toString();

            return ResponseEntity.ok(
                Map.of(
                    "success", true,
                    "message", "Authentication successful",
                    "token", mockToken,
                    "user", Map.of(
                        "id", UUID.randomUUID().toString(),
                        "email", email,
                        "displayName", "Demo User",
                        "name", "Demo User",
                        "organization", Map.of(
                            "id", "b48e719b-3116-423e-b114-c9791e296a8d",
                            "name", "Demo Organization"
                        )
                    )
                )
            );
        } else {
            return ResponseEntity.status(401).body(
                Map.of(
                    "success", false,
                    "error", "INVALID_CREDENTIALS",
                    "message", "Invalid email or password"
                )
            );
        }
    }
}