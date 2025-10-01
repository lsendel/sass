package com.platform.user.internal;

import com.platform.shared.exceptions.ResourceNotFoundException;
import com.platform.shared.exceptions.ValidationException;
import com.platform.user.Organization;
import com.platform.user.UserProfile;
import com.platform.user.events.UserProfileCreatedEvent;
import com.platform.user.events.UserProfileUpdatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing user profiles.
 *
 * @since 1.0.0
 */
@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final OrganizationRepository organizationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserProfileService(
            final UserProfileRepository userProfileRepository,
            final OrganizationRepository organizationRepository,
            final ApplicationEventPublisher eventPublisher) {
        this.userProfileRepository = userProfileRepository;
        this.organizationRepository = organizationRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new user profile.
     * This is typically called after a user successfully registers via the auth module.
     *
     * @param userId the user ID (from auth.User)
     * @param organizationId the organization ID
     * @param email the user's email
     * @return the created user profile
     */
    @Transactional
    public UserProfile createUserProfile(
            final UUID userId,
            final UUID organizationId,
            final String email) {

        // Check if profile already exists
        if (userProfileRepository.existsById(userId)) {
            throw new ValidationException("User profile already exists for ID: " + userId);
        }

        // Verify organization exists and is active
        final Organization organization = organizationRepository.findById(organizationId)
            .filter(org -> org.isActive())
            .orElseThrow(() -> new ValidationException(
                "Organization not found or not active: " + organizationId
            ));

        // Check if user already exists in this organization
        if (userProfileRepository.existsByEmailAndOrganizationId(email, organizationId)) {
            throw new ValidationException(
                "User with email " + email + " already exists in this organization"
            );
        }

        final UserProfile profile = new UserProfile(userId, organization, email);
        final UserProfile saved = userProfileRepository.save(profile);

        // Publish event
        eventPublisher.publishEvent(new UserProfileCreatedEvent(
            saved.getId(),
            saved.getEmail(),
            saved.getOrganization().getId(),
            saved.getCreatedAt()
        ));

        return saved;
    }

    /**
     * Finds a user profile by ID.
     *
     * @param userId the user ID
     * @return the user profile
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public UserProfile findById(final UUID userId) {
        return userProfileRepository.findById(userId)
            .filter(profile -> profile.getDeletedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + userId));
    }

    /**
     * Finds a user profile by email.
     *
     * @param email the email address
     * @return the user profile
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public UserProfile findByEmail(final String email) {
        return userProfileRepository.findActiveByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    /**
     * Finds all users in an organization.
     *
     * @param organizationId the organization ID
     * @return list of user profiles
     */
    @Transactional(readOnly = true)
    public List<UserProfile> findByOrganization(final UUID organizationId) {
        return userProfileRepository.findByOrganizationId(organizationId);
    }

    /**
     * Updates a user profile.
     *
     * @param userId the user ID
     * @param firstName the first name
     * @param lastName the last name
     * @param displayName the display name
     * @return the updated profile
     */
    @Transactional
    public UserProfile updateProfile(
            final UUID userId,
            final String firstName,
            final String lastName,
            final String displayName) {

        final UserProfile profile = findById(userId);
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setDisplayName(displayName);

        final UserProfile updated = userProfileRepository.save(profile);

        // Publish event
        eventPublisher.publishEvent(new UserProfileUpdatedEvent(
            updated.getId(),
            updated.getEmail(),
            updated.getUpdatedAt()
        ));

        return updated;
    }

    /**
     * Updates user's last active timestamp.
     *
     * @param userId the user ID
     */
    @Transactional
    public void updateLastActive(final UUID userId) {
        userProfileRepository.findById(userId)
            .ifPresent(profile -> {
                profile.updateLastActive();
                userProfileRepository.save(profile);
            });
    }

    /**
     * Deletes a user profile (soft delete).
     *
     * @param userId the user ID
     */
    @Transactional
    public void deleteProfile(final UUID userId) {
        final UserProfile profile = findById(userId);
        profile.delete();
        userProfileRepository.save(profile);
    }

    /**
     * Counts users in an organization.
     *
     * @param organizationId the organization ID
     * @return the user count
     */
    @Transactional(readOnly = true)
    public long countUsersInOrganization(final UUID organizationId) {
        return userProfileRepository.countByOrganizationId(organizationId);
    }
}
