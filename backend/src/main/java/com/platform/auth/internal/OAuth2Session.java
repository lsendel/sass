package com.platform.auth.internal;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * OAuth2 Session entity representing active user authentication sessions Stores session metadata
 * and links to user information from OAuth2 providers
 *
 * <p>This entity manages the lifecycle of authenticated sessions and provides secure session
 * management with proper expiration and validation.
 */
@Entity
@Table(
    name = "oauth2_sessions",
    uniqueConstraints = @UniqueConstraint(columnNames = "session_id"),
    indexes = {
      @Index(name = "idx_oauth2_session_id", columnList = "session_id"),
      @Index(name = "idx_oauth2_session_user", columnList = "user_info_id"),
      @Index(name = "idx_oauth2_session_expires", columnList = "expires_at"),
      @Index(name = "idx_oauth2_session_provider", columnList = "provider"),
      @Index(name = "idx_oauth2_session_active", columnList = "is_active")
    })
@EntityListeners(AuditingEntityListener.class)
public class OAuth2Session {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Unique session identifier (corresponds to HTTP session ID) This is the session ID stored in the
   * user's session cookie
   */
  @NotBlank
  @Column(name = "session_id", nullable = false, unique = true, length = 255)
  private String sessionId;

  /** Reference to the OAuth2 user information */
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_info_id", nullable = false)
  private OAuth2UserInfo userInfo;

  /** OAuth2 provider used for this session */
  @NotBlank
  @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "Provider must be lowercase alphanumeric")
  @Column(name = "provider", nullable = false, length = 50)
  private String provider;

  /** When this session expires */
  @NotNull
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** Whether this session is currently active */
  @NotNull
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  /** When this session was last accessed */
  @NotNull
  @Column(name = "last_accessed_at", nullable = false)
  private Instant lastAccessedAt;

  /** IP address from which this session was created */
  @Column(name = "created_from_ip", length = 45) // IPv6 max length
  private String createdFromIp;

  /** User agent string from session creation */
  @Column(name = "created_from_user_agent", length = 500)
  private String createdFromUserAgent;

  /** IP address of the last request in this session */
  @Column(name = "last_accessed_from_ip", length = 45)
  private String lastAccessedFromIp;

  /**
   * Original OAuth2 authorization code used (for audit purposes) Stored in hashed form for security
   */
  @Column(name = "authorization_code_hash", length = 255)
  private String authorizationCodeHash;

  /** PKCE code verifier hash (for audit and security validation) */
  @Column(name = "pkce_code_verifier_hash", length = 255)
  private String pkceCodeVerifierHash;

  /** OAuth2 state parameter used during authorization (hashed) */
  @Column(name = "oauth2_state_hash", length = 255)
  private String oauth2StateHash;

  /** When this session was terminated (null if still active) */
  @Column(name = "terminated_at")
  private Instant terminatedAt;

  /** Reason for session termination (logout, timeout, security, etc.) */
  @Column(name = "termination_reason", length = 50)
  private String terminationReason;

  /** Additional session metadata as JSON */
  @Column(name = "metadata")
  private String metadata;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** Version field for optimistic locking */
  @Version
  @Column(name = "version")
  private Long version;

  // Default constructor for JPA
  protected OAuth2Session() {}

  /** Constructor for creating a new OAuth2 session */
  public OAuth2Session(
      String sessionId, OAuth2UserInfo userInfo, String provider, Instant expiresAt) {
    this.sessionId = sessionId;
    this.userInfo = userInfo;
    this.provider = provider;
    this.expiresAt = expiresAt;
    this.lastAccessedAt = Instant.now();
  }

  // Getters and setters
  public Long getId() {
    return id;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public OAuth2UserInfo getUserInfo() {
    return userInfo;
  }

  public void setUserInfo(OAuth2UserInfo userInfo) {
    this.userInfo = userInfo;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public Instant getLastAccessedAt() {
    return lastAccessedAt;
  }

  public void setLastAccessedAt(Instant lastAccessedAt) {
    this.lastAccessedAt = lastAccessedAt;
  }

  public String getCreatedFromIp() {
    return createdFromIp;
  }

  public void setCreatedFromIp(String createdFromIp) {
    this.createdFromIp = createdFromIp;
  }

  public String getCreatedFromUserAgent() {
    return createdFromUserAgent;
  }

  public void setCreatedFromUserAgent(String createdFromUserAgent) {
    this.createdFromUserAgent = createdFromUserAgent;
  }

  public String getLastAccessedFromIp() {
    return lastAccessedFromIp;
  }

  public void setLastAccessedFromIp(String lastAccessedFromIp) {
    this.lastAccessedFromIp = lastAccessedFromIp;
  }

  public String getAuthorizationCodeHash() {
    return authorizationCodeHash;
  }

  public void setAuthorizationCodeHash(String authorizationCodeHash) {
    this.authorizationCodeHash = authorizationCodeHash;
  }

  public String getPkceCodeVerifierHash() {
    return pkceCodeVerifierHash;
  }

  public void setPkceCodeVerifierHash(String pkceCodeVerifierHash) {
    this.pkceCodeVerifierHash = pkceCodeVerifierHash;
  }

  public String getOauth2StateHash() {
    return oauth2StateHash;
  }

  public void setOauth2StateHash(String oauth2StateHash) {
    this.oauth2StateHash = oauth2StateHash;
  }

  public Instant getTerminatedAt() {
    return terminatedAt;
  }

  public void setTerminatedAt(Instant terminatedAt) {
    this.terminatedAt = terminatedAt;
  }

  public String getTerminationReason() {
    return terminationReason;
  }

  public void setTerminationReason(String terminationReason) {
    this.terminationReason = terminationReason;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
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

  /** Checks if this session is currently valid and active */
  public boolean isValid() {
    return isActive != null
        && isActive
        && expiresAt != null
        && expiresAt.isAfter(Instant.now())
        && terminatedAt == null;
  }

  /** Checks if this session has expired */
  public boolean isExpired() {
    return expiresAt != null && expiresAt.isBefore(Instant.now());
  }

  /** Updates the last accessed timestamp for session activity tracking */
  public void updateLastAccessed() {
    updateLastAccessed(null);
  }

  /** Updates the last accessed timestamp and IP address */
  public void updateLastAccessed(String ipAddress) {
    this.lastAccessedAt = Instant.now();
    if (ipAddress != null) {
      this.lastAccessedFromIp = ipAddress;
    }
  }

  /** Terminates this session with a reason */
  public void terminate(String reason) {
    this.isActive = false;
    this.terminatedAt = Instant.now();
    this.terminationReason = reason;
  }

  /** Extends the session expiration time */
  public void extendExpiration(Instant newExpiresAt) {
    if (newExpiresAt.isAfter(this.expiresAt)) {
      this.expiresAt = newExpiresAt;
    }
  }

  /** Gets the session duration in seconds */
  public long getSessionDurationSeconds() {
    if (createdAt == null) {
      return 0;
    }

    Instant endTime = terminatedAt != null ? terminatedAt : Instant.now();
    return endTime.getEpochSecond() - createdAt.getEpochSecond();
  }

  /** Gets the time until session expiration in seconds */
  public long getTimeToExpirationSeconds() {
    if (expiresAt == null) {
      return 0;
    }

    long seconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
    return Math.max(0, seconds);
  }

  /** Checks if the session needs to be refreshed based on activity */
  public boolean needsRefresh() {
    if (!isValid()) {
      return false;
    }

    // Refresh if session will expire within next hour
    return getTimeToExpirationSeconds() < 3600;
  }

  /** Sets security-related hashes for audit purposes */
  public void setSecurityHashes(String authCodeHash, String pkceVerifierHash, String stateHash) {
    this.authorizationCodeHash = authCodeHash;
    this.pkceCodeVerifierHash = pkceVerifierHash;
    this.oauth2StateHash = stateHash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OAuth2Session that)) return false;
    return Objects.equals(id, that.id) && Objects.equals(sessionId, that.sessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sessionId);
  }

  @Override
  public String toString() {
    return "OAuth2Session{"
        + "id="
        + id
        + ", sessionId='"
        + sessionId
        + '\''
        + ", provider='"
        + provider
        + '\''
        + ", isActive="
        + isActive
        + ", expiresAt="
        + expiresAt
        + ", lastAccessedAt="
        + lastAccessedAt
        + '}';
  }
}
