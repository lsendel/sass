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
 * User entity representing authenticated users in the system. Supports both OAuth2 and password
 * authentication with multi-tenant organization isolation.
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

  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false))
  private Email email;

  @NotBlank
  @Column(name = "name", nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  // Password authentication fields
  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "password_reset_token")
  private String passwordResetToken;

  @Column(name = "password_reset_expires_at")
  private Instant passwordResetExpiresAt;

  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified = false;

  @Column(name = "email_verification_token")
  private String emailVerificationToken;

  @Column(name = "email_verification_expires_at")
  private Instant emailVerificationExpiresAt;

  @Column(name = "failed_login_attempts", nullable = false)
  private Integer failedLoginAttempts = 0;

  @Column(name = "lockout_expires_at")
  private Instant lockoutExpiresAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "user_authentication_methods",
      joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "method")
  @Enumerated(EnumType.STRING)
  private Set<AuthenticationMethod> authenticationMethods = new HashSet<>();

  // OAuth2 fields (optional, for OAuth2 users)
  @Column(name = "provider", length = 50)
  private String provider;

  @Column(name = "provider_id")
  private String providerId;

  @Column(name = "preferences")
  @Convert(converter = MapToJsonConverter.class)
  private Map<String, Object> preferences = Map.of();

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "last_active_at")
  private Instant lastActiveAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  // Note: Role management now handled through UserRole entity relationships

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Version private Long version;

  // Constructors
  protected User() {
    // JPA constructor
  }

  // Constructor for password authentication
  public User(String email, String name) {
    this.email = new Email(email);
    this.name = name;
    this.authenticationMethods = new HashSet<>();
  }

  // Constructor for OAuth2 authentication
  public User(Email email, String name, String provider, String providerId) {
    this.email = email;
    this.name = name;
    this.provider = provider;
    this.providerId = providerId;
    this.authenticationMethods = Set.of(AuthenticationMethod.OAUTH2);
    this.emailVerified = true; // OAuth2 users are pre-verified
  }

  // Business methods
  public boolean isDeleted() {
    return deletedAt != null;
  }

  public void markAsDeleted() {
    this.deletedAt = Instant.now();
  }

  public void updateProfile(String name, Map<String, Object> preferences) {
    this.name = name;
    this.preferences = preferences != null ? Map.copyOf(preferences) : Map.of();
  }

  public void updatePreferences(Map<String, Object> preferences) {
    this.preferences = preferences != null ? Map.copyOf(preferences) : Map.of();
  }

  // Password authentication methods
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
    this.authenticationMethods.add(AuthenticationMethod.PASSWORD);
  }

  public boolean hasPassword() {
    return passwordHash != null && !passwordHash.isEmpty();
  }

  public void setPasswordResetToken(String token, Instant expiresAt) {
    this.passwordResetToken = token;
    this.passwordResetExpiresAt = expiresAt;
  }

  public void clearPasswordResetToken() {
    this.passwordResetToken = null;
    this.passwordResetExpiresAt = null;
  }

  public boolean isPasswordResetTokenValid(String token) {
    return passwordResetToken != null
        && passwordResetToken.equals(token)
        && passwordResetExpiresAt != null
        && passwordResetExpiresAt.isAfter(Instant.now());
  }

  public void verifyEmail() {
    this.emailVerified = true;
    this.emailVerificationToken = null;
    this.emailVerificationExpiresAt = null;
  }

  public void setEmailVerificationToken(String token, Instant expiresAt) {
    this.emailVerificationToken = token;
    this.emailVerificationExpiresAt = expiresAt;
  }

  public boolean isEmailVerificationTokenValid(String token) {
    return emailVerificationToken != null
        && emailVerificationToken.equals(token)
        && emailVerificationExpiresAt != null
        && emailVerificationExpiresAt.isAfter(Instant.now());
  }

  public void incrementFailedLoginAttempts() {
    this.failedLoginAttempts++;
  }

  public void resetFailedLoginAttempts() {
    this.failedLoginAttempts = 0;
    this.lockoutExpiresAt = null;
  }

  public void lockAccount(Instant lockoutExpiresAt) {
    this.lockoutExpiresAt = lockoutExpiresAt;
  }

  public boolean isAccountLocked() {
    return lockoutExpiresAt != null && lockoutExpiresAt.isAfter(Instant.now());
  }

  public boolean supportsAuthenticationMethod(AuthenticationMethod method) {
    return authenticationMethods.contains(method);
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public Email getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getPasswordResetToken() {
    return passwordResetToken;
  }

  public Instant getPasswordResetExpiresAt() {
    return passwordResetExpiresAt;
  }

  public Boolean getEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(Boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public String getEmailVerificationToken() {
    return emailVerificationToken;
  }

  public Instant getEmailVerificationExpiresAt() {
    return emailVerificationExpiresAt;
  }

  public Integer getFailedLoginAttempts() {
    return failedLoginAttempts;
  }

  public void setFailedLoginAttempts(Integer failedLoginAttempts) {
    this.failedLoginAttempts = failedLoginAttempts;
  }

  public Instant getLockoutExpiresAt() {
    return lockoutExpiresAt;
  }

  public void setLockoutExpiresAt(Instant lockoutExpiresAt) {
    this.lockoutExpiresAt = lockoutExpiresAt;
  }

  public Set<AuthenticationMethod> getAuthenticationMethods() {
    return Set.copyOf(authenticationMethods);
  }

  public void setAuthenticationMethods(Set<AuthenticationMethod> authenticationMethods) {
    this.authenticationMethods = new HashSet<>(authenticationMethods);
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderId() {
    return providerId;
  }

  public Map<String, Object> getPreferences() {
    return Map.copyOf(preferences);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getLastActiveAt() {
    return lastActiveAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public Instant getDeactivatedAt() {
    return deletedAt; // In this system, deactivated is the same as deleted
  }

  public Long getVersion() {
    return version;
  }

  // Note: Role access is now handled through UserRole service methods

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public boolean isActive() {
    return Boolean.TRUE.equals(active) && !isDeleted() && !isAccountLocked();
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

  /** Authentication method enumeration */
  public enum AuthenticationMethod {
    PASSWORD,
    OAUTH2
  }
}
