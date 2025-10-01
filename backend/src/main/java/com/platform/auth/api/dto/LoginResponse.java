package com.platform.auth.api.dto;

/**
 * Response DTO for successful login.
 * Token is sent in httpOnly cookie, not in response body.
 *
 * @param message success message
 * @since 1.0.0
 */
public record LoginResponse(
    String message
) {
}
