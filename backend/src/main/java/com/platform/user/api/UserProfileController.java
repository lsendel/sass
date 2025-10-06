package com.platform.user.api;

import com.platform.user.UserProfile;
import com.platform.user.api.dto.UpdateProfileRequest;
import com.platform.user.api.dto.UserProfileResponse;
import com.platform.user.internal.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for user profile operations.
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(final UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Gets the current user's profile.
     *
     * @param principal the authenticated user
     * @return the user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(final Principal principal) {
        // In production, extract user ID from authenticated principal
        // For now, using email from principal name
        final UserProfile profile = userProfileService.findByEmail(principal.getName());
        return ResponseEntity.ok(UserProfileResponse.fromEntity(profile));
    }

    /**
     * Gets a user profile by ID.
     *
     * @param userId the user ID
     * @return the user profile
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable final UUID userId) {
        final UserProfile profile = userProfileService.findById(userId);
        return ResponseEntity.ok(UserProfileResponse.fromEntity(profile));
    }

    /**
     * Gets all users in an organization.
     *
     * @param organizationId the organization ID
     * @return list of user profiles
     */
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getUsersByOrganization(
            @RequestParam final UUID organizationId) {
        final List<UserProfile> profiles =
            userProfileService.findByOrganization(organizationId);
        final List<UserProfileResponse> responses =
            profiles.stream()
                .map(UserProfileResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Updates the current user's profile.
     *
     * @param request the update request
     * @param principal the authenticated user
     * @return the updated profile
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
            @Valid @RequestBody final UpdateProfileRequest request,
            final Principal principal) {
        final UserProfile current = userProfileService.findByEmail(principal.getName());
        final UserProfile updated = userProfileService.updateProfile(
            current.getId(),
            request.firstName(),
            request.lastName(),
            request.displayName()
        );
        return ResponseEntity.ok(UserProfileResponse.fromEntity(updated));
    }

    /**
     * Deletes the current user's profile.
     *
     * @param principal the authenticated user
     * @return no content
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(final Principal principal) {
        final UserProfile current = userProfileService.findByEmail(principal.getName());
        userProfileService.deleteProfile(current.getId());
        return ResponseEntity.noContent().build();
    }
}
