package com.platform.auth.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents a single attempt to authenticate into the system.
 *
 * <p>This entity is used for auditing and security monitoring purposes. It records key information
 * about each login attempt, such as the user involved, the method used, the outcome, and contextual
 * details like IP address and user agent. This data is critical for rate limiting, brute-force
 * detection, and forensic analysis.
 * </p>
 */
@Entity
@Table(
    name = "authentication_attempts",
    indexes = {
      @Index(name = "idx_auth_attempts_user_id", columnList = "user_id"),
      @Index(name = "idx_auth_attempts_ip_address", columnList = "ip_address"),
      @Index(name = "idx_auth_attempts_time_range", columnList = "attempt_time, ip_address"),
      @Index(name = "idx_auth_attempts_method", columnList = "method, success"),
      @Index(name = "idx_auth_attempts_cleanup", columnList = "attempt_time")
    })
public class AuthenticationAttempt {

  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The ID of the user attempting to authenticate. Can be null if the user is unknown. */
  @Column(name = "user_id")
  private UUID userId;

  /** The email address used for the authentication attempt. */
  @NotBlank
  @Column(name = "email", nullable = false)
  private String email;

  /** The authentication method used (e.g., PASSWORD, OAUTH2). */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false)
  private AuthenticationMethod method;

  /** Whether the authentication attempt was successful. */
  @NotNull
  @Column(name = "success", nullable = false)
  private Boolean success;

  /** The reason for failure, if applicable. */
  @Column(name = "failure_reason")
  private String failureReason;

  /** The IP address from which the attempt originated. */
  @NotBlank
  @Column(name = "ip_address", nullable = false)
  private String ipAddress;

  /** The user agent string of the client making the attempt. */
  @Column(name = "user_agent")
  private String userAgent;

  /** The timestamp when the attempt occurred. */
  @CreationTimestamp
  @Column(name = "attempt_time", nullable = false, updatable = false)
  private Instant attemptTime;

  /** The session ID associated with the attempt, if any. */
  @Column(name = "session_id")
  private String sessionId;

  /** Geolocation data derived from the IP address for security analysis. */
  @Column(name = "geolocation")
  private String geolocation;

  /** A unique fingerprint identifying the client device. */
  @Column(name = "device_fingerprint")
  private String deviceFingerprint;

  /** Protected no-argument constructor for JPA. */
  protected AuthenticationAttempt() {
    // JPA constructor
  }

  private AuthenticationAttempt(
      UUID userId,
      String email,
      AuthenticationMethod method,
      boolean success,
      String failureReason,
      String ipAddress,
      String userAgent,
      String sessionId) {
    this.userId = userId;
    this.email = email;
    this.method = method;
    this.success = success;
    this.failureReason = failureReason;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
    this.sessionId = sessionId;
  }

  /**
   * Factory method to create a record for a successful authentication attempt.
   *
   * @param userId The ID of the authenticated user.
   * @param email The user's email.
   * @param method The authentication method used.
   * @param ipAddress The source IP address.
   * @param userAgent The client's user agent.
   * @param sessionId The resulting session ID.
   * @return A new {@link AuthenticationAttempt} instance for a successful attempt.
   */
  public static AuthenticationAttempt success(
      UUID userId,
      String email,
      AuthenticationMethod method,
      String ipAddress,
      String userAgent,
      String sessionId) {
    return new AuthenticationAttempt(userId, email, method, true, null, ipAddress, userAgent, sessionId);
  }

  /**
   * Factory method to create a record for a failed authentication attempt where the user is known.
   *
   * @param userId The ID of the user who failed to authenticate.
   * @param email The user's email.
   * @param method The authentication method used.
   * @param failureReason The reason for the failure.
   * @param ipAddress The source IP address.
   * @param userAgent The client's user agent.
   * @return A new {@link AuthenticationAttempt} instance for a failed attempt.
   */
  public static AuthenticationAttempt failure(
      UUID userId,
      String email,
      AuthenticationMethod method,
      String failureReason,
      String ipAddress,
      String userAgent) {
    return new AuthenticationAttempt(userId, email, method, false, failureReason, ipAddress, userAgent, null);
  }

  /**
   * Factory method for a failed authentication attempt where the user could not be identified.
   *
   * @param email The email address provided in the attempt.
   * @param method The authentication method used.
   * @param failureReason The reason for the failure.
   * @param ipAddress The source IP address.
   * @param userAgent The client's user agent.
   * @return A new {@link AuthenticationAttempt} instance for a failed attempt by an unknown user.
   */
  public static AuthenticationAttempt failureUnknownUser(
      String email,
      AuthenticationMethod method,
      String failureReason,
      String ipAddress,
      String userAgent) {
    return new AuthenticationAttempt(null, email, method, false, failureReason, ipAddress, userAgent, null);
  }

  /**
   * Checks if the authentication attempt was successful.
   *
   * @return {@code true} if successful, {@code false} otherwise.
   */
  public boolean isSuccess() {
    return success != null && success;
  }

  /**
   * Checks if the authentication attempt failed.
   *
   * @return {@code true} if failed, {@code false} otherwise.
   */
  public boolean isFailure() {
    return success != null && !success;
  }

  /**
   * Checks if the attempt occurred within a specified time range.
   *
   * @param startTime The start of the time range (exclusive).
   * @param endTime The end of the time range (exclusive).
   * @return {@code true} if the attempt is within the range, {@code false} otherwise.
   */
  public boolean isWithinTimeRange(Instant startTime, Instant endTime) {
    return attemptTime.isAfter(startTime) && attemptTime.isBefore(endTime);
  }

  // Getters and setters
  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public AuthenticationMethod getMethod() {
    return method;
  }

  public void setMethod(AuthenticationMethod method) {
    this.method = method;
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public Instant getAttemptTime() {
    return attemptTime;
  }

  public void setAttemptTime(Instant attemptTime) {
    this.attemptTime = attemptTime;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getGeolocation() {
    return geolocation;
  }

  public void setGeolocation(String geolocation) {
    this.geolocation = geolocation;
  }

  public String getDeviceFingerprint() {
    return deviceFingerprint;
  }

  public void setDeviceFingerprint(String deviceFingerprint) {
    this.deviceFingerprint = deviceFingerprint;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AuthenticationAttempt other)) return false;
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "AuthenticationAttempt{"
        + "id="
        + id
        + ", userId="
        + userId
        + ", email='"
        + email
        + '\''
        + ", method="
        + method
        + ", success="
        + success
        + ", ipAddress='"
        + ipAddress
        + '\''
        + ", attemptTime="
        + attemptTime
        + '}';
  }

  /** Enumerates the supported methods of authentication. */
  public enum AuthenticationMethod {
    /** Authentication using a password. */
    PASSWORD,
    /** Authentication using an external OAuth2 provider. */
    OAUTH2
  }
}
