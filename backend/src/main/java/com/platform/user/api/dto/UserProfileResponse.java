package com.platform.user.api.dto;

import com.platform.user.UserProfile;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for user profile information.
 *
 * @since 1.0.0
 */
public record UserProfileResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String displayName,
    String fullName,
    String avatarUrl,
    String role,
    UUID organizationId,
    String organizationName,
    Instant createdAt,
    Instant lastActiveAt
) {
    /**
     * Creates a response from a UserProfile entity.
     *
     * @param profile the user profile
     * @return the response DTO
     */
    public static UserProfileResponse fromEntity(final UserProfile profile) {
        return new UserProfileResponse(
            profile.getId(),
            profile.getEmail(),
            profile.getFirstName(),
            profile.getLastName(),
            profile.getDisplayName(),
            profile.getFullName(),
            profile.getAvatarUrl(),
            profile.getRole().name(),
            profile.getOrganization().getId(),
            profile.getOrganization().getName(),
            profile.getCreatedAt(),
            profile.getLastActiveAt()
        );
    }
}
