package com.platform.auth.api;

import com.platform.auth.api.dto.LoginRequest;
import com.platform.auth.api.dto.LoginResponse;
import com.platform.auth.internal.AuthenticationService;
import com.platform.shared.exceptions.ValidationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Provides login and logout functionality with opaque token authentication.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "auth_token";
    private static final int COOKIE_MAX_AGE = 24 * 60 * 60; // 24 hours in seconds

    private final AuthenticationService authenticationService;

    /**
     * Constructor with dependency injection.
     *
     * @param authenticationService the authentication service
     */
    public AuthController(final AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Authenticates a user and returns an httpOnly cookie with opaque token.
     *
     * @param request the login request containing email and password
     * @return response with success message
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {
        try {
            final String token = authenticationService.authenticate(
                request.email(),
                request.password()
            );

            final ResponseCookie cookie = createAuthCookie(token, COOKIE_MAX_AGE);

            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse("Login successful"));

        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new LoginResponse(e.getMessage()));
        }
    }

    /**
     * Logs out a user by revoking their token and clearing the cookie.
     *
     * @param token the authentication token from cookie
     * @return response with success message
     */
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(
            @CookieValue(value = AUTH_COOKIE_NAME, required = false) final String token) {

        if (token != null) {
            authenticationService.revokeToken(token);
        }

        final ResponseCookie cookie = createAuthCookie("", 0);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(new LoginResponse("Logout successful"));
    }

    /**
     * Creates an httpOnly authentication cookie.
     *
     * @param token the token value
     * @param maxAge the cookie max age in seconds
     * @return the response cookie
     */
    private ResponseCookie createAuthCookie(final String token, final int maxAge) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(true) // HTTPS only in production
            .sameSite("Strict")
            .path("/")
            .maxAge(maxAge)
            .build();
    }
}
