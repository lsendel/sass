package com.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * UserProfile entity representing user information and preferences.
 * This is separate from the auth.User entity which handles authentication.
 *
 * <p>This entity contains the "profile" aspects of a user - names, contact info,
 * preferences, while auth.User contains authentication-specific data like
 * password hashes and login attempts.
 *
 * <p>Relationship: One UserProfile corresponds to one auth.User (same UUID).
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    private UUID id; // Same as auth.User.id

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String timezone;

    @Column(length = 10)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role = UserRole.MEMBER;

    @Column(name = "preferences", columnDefinition = "TEXT")
    private String preferences; // JSON string for user preferences

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    /**
     * Default constructor for JPA.
     */
    protected UserProfile() {
    }

    /**
     * Creates a new user profile.
     *
     * @param id the user ID (same as auth.User.id)
     * @param organization the organization this user belongs to
     * @param email the user's email address
     */
    public UserProfile(final UUID id, final Organization organization, final String email) {
        this.id = id;
        this.organization = organization;
        this.email = email;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.role = UserRole.MEMBER;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Gets the full name of the user.
     *
     * @return the full name, or email if names not set
     */
    public String getFullName() {
        if (this.displayName != null && !this.displayName.isBlank()) {
            return this.displayName;
        }
        if (this.firstName != null && this.lastName != null) {
            return this.firstName + " " + this.lastName;
        }
        if (this.firstName != null) {
            return this.firstName;
        }
        return this.email;
    }

    /**
     * Checks if the user is an admin or owner.
     *
     * @return true if user has admin or owner role
     */
    public boolean isAdmin() {
        return this.role == UserRole.OWNER || this.role == UserRole.ADMIN;
    }

    /**
     * Updates the last active timestamp.
     */
    public void updateLastActive() {
        this.lastActiveAt = Instant.now();
    }

    /**
     * Soft deletes the user profile.
     */
    public void delete() {
        this.deletedAt = Instant.now();
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(final String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(final String timezone) {
        this.timezone = timezone;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(final String locale) {
        this.locale = locale;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(final UserRole role) {
        this.role = role;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(final String preferences) {
        this.preferences = preferences;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    /**
     * User role within an organization.
     */
    public enum UserRole {
        /** Organization owner with full permissions */
        OWNER,
        /** Administrator with elevated permissions */
        ADMIN,
        /** Regular member with standard permissions */
        MEMBER,
        /** Read-only guest access */
        GUEST
    }
}
