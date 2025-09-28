package com.platform.user.internal;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.platform.shared.types.Email;

/**
 * Represents a user in the system.
 *
 * <p>This entity stores information about a user, including their personal details, authentication
 * credentials, and relationship with an {@link Organization}. It supports multiple authentication
 * methods, such as password-based and OAuth2.
 *
 * @see Organization
 * @see Email
 */
@Entity
@Table(
    name = "users",
    indexes = {
      @Index(name = "idx_users_email_org", columnList = "email, organization_id", unique = true),
      @Index(name = "idx_users_provider", columnList = "provider, provider_id"),
      @Index(name = "idx_users_deleted_at", columnList = "deleted_at"),
      @Index(name = "idx_users_password_reset_token", columnList = "password_reset_token"),
      @Index(name = "idx_users_email_verification_token", columnList = "email_verification_token")
    })
public class User {

  /** The unique identifier for the user. */
  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The user's email address, represented as a value object. */
  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false))
  private Email email;

  /** The user's full name. */
  @NotBlank
  @Column(name = "name", nullable = false)
  private String name;

  /** The organization the user belongs to. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  /** The hashed password for password-based authentication. */
  @Column(name = "password_hash")
  private String passwordHash;

  /** A token for resetting the user's password. */
  @Column(name = "password_reset_token")
  private String passwordResetToken;

  /** The expiration time for the password reset token. */
  @Column(name = "password_reset_expires_at")
  private Instant passwordResetExpiresAt;

  /** A flag indicating whether the user's email address has been verified. */
  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified = false;

  /** A token for verifying the user's email address. */
  @Column(name = "email_verification_token")
  private String emailVerificationToken;

  /** The expiration time for the email verification token. */
  @Column(name = "email_verification_expires_at")
  private Instant emailVerificationExpiresAt;

  /** The number of failed login attempts since the last successful login. */
  @Column(name = "failed_login_attempts", nullable = false)
  private Integer failedLoginAttempts = 0;

  /** The time until which the user's account is locked out. */
  @Column(name = "lockout_expires_at")
  private Instant lockoutExpiresAt;

  /** The set of authentication methods supported by the user. */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "user_authentication_methods",
      joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "method")
  @Enumerated(EnumType.STRING)
  private Set<AuthenticationMethod> authenticationMethods = new HashSet<>();

  /** The name of the OAuth2 provider (e.g., "google", "github"). */
  @Column(name = "provider", length = 50)
  private String provider;

  /** The user's unique ID from the OAuth2 provider. */
  @Column(name = "provider_id")
  private String providerId;

  /** A flexible map for storing user preferences. */
  @Column(name = "preferences")
  @Convert(converter = MapToJsonConverter.class)
  private Map<String, Object> preferences = Map.of();

  /** The timestamp of when the user was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** The timestamp of the last update to the user. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** The timestamp of the user's last activity. */
  @Column(name = "last_active_at")
  private Instant lastActiveAt;

  /** The timestamp of when the user was soft-deleted. */
  @Column(name = "deleted_at")
  private Instant deletedAt;

  /** The version number for optimistic locking. */
  @Version private Long version;

  /**
   * Protected no-argument constructor for JPA.
   *
   * <p>This constructor is required by JPA and should not be used directly.
   */
  protected User() {
    // JPA constructor
  }

  /**
   * Constructs a new User for password-based authentication.
   *
   * @param email the user's email address
   * @param name the user's full name
   */
  public User(String email, String name) {
    this.email = new Email(email);
    this.name = name;
    this.authenticationMethods = new HashSet<>();
  }

  /**
   * Constructs a new User for OAuth2-based authentication.
   *
   * @param email the user's email address
   * @param name the user's full name
   * @param provider the name of the OAuth2 provider
   * @param providerId the user's ID from the OAuth2 provider
   */
  public User(Email email, String name, String provider, String providerId) {
    this.email = email;
    this.name = name;
    this.provider = provider;
    this.providerId = providerId;
    this.authenticationMethods = Set.of(AuthenticationMethod.OAUTH2);
    this.emailVerified = true; // OAuth2 users are pre-verified
  }

  // Business methods

  /**
   * Checks if the user has been soft-deleted.
   *
   * @return {@code true} if the user is marked as deleted, {@code false} otherwise
   */
  public boolean isDeleted() {
    return deletedAt != null;
  }

  /** Marks the user as soft-deleted by setting the deleted timestamp. */
  public void markAsDeleted() {
    this.deletedAt = Instant.now();
  }

  /**
   * Updates the user's profile information.
   *
   * @param name the new name for the user
   * @param preferences a map of new preferences for the user
   */
  public void updateProfile(String name, Map<String, Object> preferences) {
    this.name = name;
    this.preferences = preferences != null ? Map.copyOf(preferences) : Map.of();
  }

  /**
   * Updates the user's preferences.
   *
   * @param preferences a map of new preferences for the user
   */
  public void updatePreferences(Map<String, Object> preferences) {
    this.preferences = preferences != null ? Map.copyOf(preferences) : Map.of();
  }

  // Password authentication methods

  /**
   * Sets the user's hashed password and enables password authentication.
   *
   * @param passwordHash the hashed password
   */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
    this.authenticationMethods.add(AuthenticationMethod.PASSWORD);
  }

  /**
   * Checks if the user has a password set.
   *
   * @return {@code true} if the user has a password, {@code false} otherwise
   */
  public boolean hasPassword() {
    return passwordHash != null && !passwordHash.isEmpty();
  }

  /**
   * Sets a password reset token for the user.
   *
   * @param token the reset token
   * @param expiresAt the expiration time for the token
   */
  public void setPasswordResetToken(String token, Instant expiresAt) {
    this.passwordResetToken = token;
    this.passwordResetExpiresAt = expiresAt;
  }

  /** Clears the password reset token. */
  public void clearPasswordResetToken() {
    this.passwordResetToken = null;
    this.passwordResetExpiresAt = null;
  }

  /**
   * Checks if a password reset token is valid.
   *
   * @param token the token to validate
   * @return {@code true} if the token is valid and not expired, {@code false} otherwise
   */
  public boolean isPasswordResetTokenValid(String token) {
    return passwordResetToken != null
        && passwordResetToken.equals(token)
        && passwordResetExpiresAt != null
        && passwordResetExpiresAt.isAfter(Instant.now());
  }

  /** Marks the user's email as verified and clears the verification token. */
  public void verifyEmail() {
    this.emailVerified = true;
    this.emailVerificationToken = null;
    this.emailVerificationExpiresAt = null;
  }

  /**
   * Sets an email verification token for the user.
   *
   * @param token the verification token
   * @param expiresAt the expiration time for the token
   */
  public void setEmailVerificationToken(String token, Instant expiresAt) {
    this.emailVerificationToken = token;
    this.emailVerificationExpiresAt = expiresAt;
  }

  /**
   * Checks if an email verification token is valid.
   *
   * @param token the token to validate
   * @return {@code true} if the token is valid and not expired, {@code false} otherwise
   */
  public boolean isEmailVerificationTokenValid(String token) {
    return emailVerificationToken != null
        && emailVerificationToken.equals(token)
        && emailVerificationExpiresAt != null
        && emailVerificationExpiresAt.isAfter(Instant.now());
  }

  /** Increments the count of failed login attempts. */
  public void incrementFailedLoginAttempts() {
    this.failedLoginAttempts++;
  }

  /** Resets the count of failed login attempts and clears any lockout. */
  public void resetFailedLoginAttempts() {
    this.failedLoginAttempts = 0;
    this.lockoutExpiresAt = null;
  }

  /**
   * Locks the user's account until a specified time.
   *
   * @param lockoutExpiresAt the time when the lockout should expire
   */
  public void lockAccount(Instant lockoutExpiresAt) {
    this.lockoutExpiresAt = lockoutExpiresAt;
  }

  /**
   * Checks if the user's account is currently locked.
   *
   * @return {@code true} if the account is locked, {@code false} otherwise
   */
  public boolean isAccountLocked() {
    return lockoutExpiresAt != null && lockoutExpiresAt.isAfter(Instant.now());
  }

  /**
   * Checks if the user supports a specific authentication method.
   *
   * @param method the authentication method to check for
   * @return {@code true} if the user supports the method, {@code false} otherwise
   */
  public boolean supportsAuthenticationMethod(AuthenticationMethod method) {
    return authenticationMethods.contains(method);
  }

  // Getters and Setters

  /**
   * Gets the unique identifier for the user.
   *
   * @return the ID of the user
   */
  public UUID getId() {
    return id;
  }

  /**
   * Gets the user's email address.
   *
   * @return the email address
   */
  public Email getEmail() {
    return email;
  }

  /**
   * Gets the user's full name.
   *
   * @return the name of the user
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the organization the user belongs to.
   *
   * @return the user's organization
   */
  public Organization getOrganization() {
    return organization;
  }

  /**
   * Sets the organization for the user.
   *
   * @param organization the organization to set
   */
  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  /**
   * Gets the user's hashed password.
   *
   * @return the hashed password
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * Gets the password reset token.
   *
   * @return the password reset token
   */
  public String getPasswordResetToken() {
    return passwordResetToken;
  }

  /**
   * Gets the expiration time for the password reset token.
   *
   * @return the expiration time
   */
  public Instant getPasswordResetExpiresAt() {
    return passwordResetExpiresAt;
  }

  /**
   * Gets the email verification status.
   *
   * @return {@code true} if the email is verified, {@code false} otherwise
   */
  public Boolean getEmailVerified() {
    return emailVerified;
  }

  /**
   * Sets the email verification status.
   *
   * @param emailVerified the new verification status
   */
  public void setEmailVerified(Boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  /**
   * Gets the email verification token.
   *
   * @return the email verification token
   */
  public String getEmailVerificationToken() {
    return emailVerificationToken;
  }

  /**
   * Gets the expiration time for the email verification token.
   *
   * @return the expiration time
   */
  public Instant getEmailVerificationExpiresAt() {
    return emailVerificationExpiresAt;
  }

  /**
   * Gets the number of failed login attempts.
   *
   * @return the number of failed attempts
   */
  public Integer getFailedLoginAttempts() {
    return failedLoginAttempts;
  }

  /**
   * Sets the number of failed login attempts.
   *
   * @param failedLoginAttempts the new number of failed attempts
   */
  public void setFailedLoginAttempts(Integer failedLoginAttempts) {
    this.failedLoginAttempts = failedLoginAttempts;
  }

  /**
   * Gets the lockout expiration time.
   *
   * @return the lockout expiration time
   */
  public Instant getLockoutExpiresAt() {
    return lockoutExpiresAt;
  }

  /**
   * Sets the lockout expiration time.
   *
   * @param lockoutExpiresAt the new lockout expiration time
   */
  public void setLockoutExpiresAt(Instant lockoutExpiresAt) {
    this.lockoutExpiresAt = lockoutExpiresAt;
  }

  /**
   * Gets the set of supported authentication methods.
   *
   * @return an immutable set of authentication methods
   */
  public Set<AuthenticationMethod> getAuthenticationMethods() {
    return Set.copyOf(authenticationMethods);
  }

  /**
   * Sets the authentication methods for the user.
   *
   * @param authenticationMethods the new set of authentication methods
   */
  public void setAuthenticationMethods(Set<AuthenticationMethod> authenticationMethods) {
    this.authenticationMethods = new HashSet<>(authenticationMethods);
  }

  /**
   * Gets the name of the OAuth2 provider.
   *
   * @return the OAuth2 provider name
   */
  public String getProvider() {
    return provider;
  }

  /**
   * Gets the user's ID from the OAuth2 provider.
   *
   * @return the provider-specific user ID
   */
  public String getProviderId() {
    return providerId;
  }

  /**
   * Gets the user's preferences.
   *
   * @return an immutable map of user preferences
   */
  public Map<String, Object> getPreferences() {
    return Map.copyOf(preferences);
  }

  /**
   * Gets the creation timestamp of the user.
   *
   * @return the creation time
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets the creation timestamp of the user.
   *
   * @param createdAt the new creation time
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Gets the last update timestamp of the user.
   *
   * @return the last update time
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Sets the last update timestamp of the user.
   *
   * @param updatedAt the new update time
   */
  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * Gets the timestamp of the user's last activity.
   *
   * @return the last active time
   */
  public Instant getLastActiveAt() {
    return lastActiveAt;
  }

  /**
   * Gets the soft-deletion timestamp of the user.
   *
   * @return the deletion time, or null if not deleted
   */
  public Instant getDeletedAt() {
    return deletedAt;
  }

  /**
   * Gets the version number for optimistic locking.
   *
   * @return the version number
   */
  public Long getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof User other)) return false;
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "User{"
        + "id="
        + id
        + ", email="
        + email
        + ", name='"
        + name
        + '\''
        + ", organization="
        + (organization != null ? organization.getId() : null)
        + ", authenticationMethods="
        + authenticationMethods
        + ", createdAt="
        + createdAt
        + '}';
  }

  /**
   * Enumerates the supported authentication methods for a user.
   */
  public enum AuthenticationMethod {
    /** Password-based authentication. */
    PASSWORD,
    /** OAuth2-based authentication. */
    OAUTH2
  }
}
