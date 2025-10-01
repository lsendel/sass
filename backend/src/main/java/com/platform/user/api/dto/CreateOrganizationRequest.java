package com.platform.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating an organization.
 *
 * @param name the organization name
 * @param slug the URL-friendly slug
 * @since 1.0.0
 */
public record CreateOrganizationRequest(
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    String name,

    @NotBlank(message = "Organization slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(min = 2, max = 100, message = "Slug must be between 2 and 100 characters")
    String slug
) {
}
