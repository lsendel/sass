package com.platform.user.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a user profile.
 *
 * @param firstName the first name
 * @param lastName the last name
 * @param displayName the display name
 * @since 1.0.0
 */
public record UpdateProfileRequest(
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName,

    @Size(max = 255, message = "Display name must not exceed 255 characters")
    String displayName
) {
}
