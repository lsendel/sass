package com.platform.auth.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * OAuth2 User Information entity storing user profile data from OAuth2 providers
 * Contains standardized user information retrieved from Google, GitHub, Microsoft, etc.
 *
 * This entity stores user information in a provider-agnostic format while preserving
 * the original provider-specific data for audit and debugging purposes.
 */
@Entity
@Table(name = "oauth2_user_info",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"provider_user_id", "provider"}),
           @UniqueConstraint(columnNames = {"email", "provider"})
       },
       indexes = {
           @Index(name = "idx_oauth2_user_provider_id", columnList = "provider_user_id"),
           @Index(name = "idx_oauth2_user_email", columnList = "email"),
           @Index(name = "idx_oauth2_user_provider", columnList = "provider"),
           @Index(name = "idx_oauth2_user_verified", columnList = "email_verified")
       })
@EntityListeners(AuditingEntityListener.class)
public class OAuth2UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User identifier from the OAuth2 provider (sub claim in OpenID Connect)
     * This is the stable, unique identifier for the user from the provider
     */
    @NotBlank
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    /**
     * OAuth2 provider name (google, github, microsoft)
     */
    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "Provider must be lowercase alphanumeric")
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    /**
     * User's email address from the OAuth2 provider
     */
    @NotBlank
    @Email
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Whether the email address has been verified by the OAuth2 provider
     */
    @Column(name = "email_verified")
    private Boolean emailVerified;

    /**
     * User's full name from the OAuth2 provider
     */
    @Column(name = "name", length = 255)
    private String name;

    /**
     * User's given (first) name
     */
    @Column(name = "given_name", length = 100)
    private String givenName;

    /**
     * User's family (last) name
     */
    @Column(name = "family_name", length = 100)
    private String familyName;

    /**
     * URL to user's profile picture
     */
    @Column(name = "picture", length = 500)
    private String picture;

    /**
     * User's locale preference (e.g., en-US, fr-FR)
     */
    @Column(name = "locale", length = 10)
    private String locale;

    /**
     * User's timezone identifier (e.g., America/New_York)
     */
    @Column(name = "timezone", length = 50)
    private String timezone;

    /**
     * Original raw user information from the OAuth2 provider as JSON
     * Preserved for audit purposes and provider-specific attributes
     */
    @Column(name = "raw_attributes")
    private String rawAttributes;

    /**
     * When this user information was last updated from the provider
     */
    @Column(name = "last_updated_from_provider")
    private Instant lastUpdatedFromProvider;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Version field for optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    // Default constructor for JPA
    protected OAuth2UserInfo() {}

    /**
     * Constructor for creating OAuth2 user info from provider data
     */
    public OAuth2UserInfo(String providerUserId, String provider, String email) {
        this.providerUserId = providerUserId;
        this.provider = provider;
        this.email = email;
        this.lastUpdatedFromProvider = Instant.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getRawAttributes() {
        return rawAttributes;
    }

    public void setRawAttributes(String rawAttributes) {
        this.rawAttributes = rawAttributes;
    }

    public Instant getLastUpdatedFromProvider() {
        return lastUpdatedFromProvider;
    }

    public void setLastUpdatedFromProvider(Instant lastUpdatedFromProvider) {
        this.lastUpdatedFromProvider = lastUpdatedFromProvider;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    // Business methods

    /**
     * Updates user information with fresh data from OAuth2 provider
     */
    public void updateFromProvider(String email, String name, String givenName, String familyName,
                                  String picture, String locale, Boolean emailVerified, String rawAttributes) {
        this.email = email;
        this.name = name;
        this.givenName = givenName;
        this.familyName = familyName;
        this.picture = picture;
        this.locale = locale;
        this.emailVerified = emailVerified;
        this.rawAttributes = rawAttributes;
        this.lastUpdatedFromProvider = Instant.now();
    }

    /**
     * Gets the user's display name, preferring full name over given name
     */
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (givenName != null && !givenName.trim().isEmpty()) {
            return givenName;
        }
        return email; // Fallback to email if no name available
    }

    /**
     * Checks if the user's email is verified by the OAuth2 provider
     */
    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }

    /**
     * Checks if this user info needs refresh from the provider
     * Based on last update time and provider policies
     */
    public boolean needsRefreshFromProvider() {
        if (lastUpdatedFromProvider == null) {
            return true;
        }

        // Refresh if older than 24 hours
        return lastUpdatedFromProvider.isBefore(Instant.now().minusSeconds(24 * 60 * 60));
    }

    /**
     * Gets a unique identifier combining provider and provider user ID
     */
    public String getProviderUniqueId() {
        return provider + ":" + providerUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OAuth2UserInfo that)) return false;
        return Objects.equals(id, that.id) &&
               Objects.equals(providerUserId, that.providerUserId) &&
               Objects.equals(provider, that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, providerUserId, provider);
    }

    @Override
    public String toString() {
        return "OAuth2UserInfo{" +
                "id=" + id +
                ", providerUserId='" + providerUserId + '\'' +
                ", provider='" + provider + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", emailVerified=" + emailVerified +
                '}';
    }
}