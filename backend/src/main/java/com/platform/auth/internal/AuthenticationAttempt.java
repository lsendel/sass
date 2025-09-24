package com.platform.auth.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Entity for auditing authentication attempts. Used for security monitoring, rate limiting, and
 * GDPR compliance.
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

  @Column(name = "user_id")
  private UUID userId;

  @NotBlank
  @Column(name = "email", nullable = false)
  private String email;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false)
  private AuthenticationMethod method;

  @NotNull
  @Column(name = "success", nullable = false)
  private Boolean success;

  @Column(name = "failure_reason")
  private String failureReason;

  @NotBlank
  @Column(name = "ip_address", nullable = false)
  private String ipAddress;

  @Column(name = "user_agent")
  private String userAgent;

  @CreationTimestamp
  @Column(name = "attempt_time", nullable = false, updatable = false)
  private Instant attemptTime;

  @Column(name = "session_id")
  private String sessionId;

  // Additional context for security analysis
  @Column(name = "geolocation")
  private String geolocation;

  @Column(name = "device_fingerprint")
  private String deviceFingerprint;

  // Constructors
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

  /** Factory method for successful authentication attempts. */
  public static AuthenticationAttempt success(
      UUID userId,
      String email,
      AuthenticationMethod method,
      String ipAddress,
      String userAgent,
      String sessionId) {
    return new AuthenticationAttempt(userId, email, method, true, null, ipAddress, userAgent, sessionId);
  }

  /** Factory method for failed authentication attempts with a known user. */
  public static AuthenticationAttempt failure(
      UUID userId,
      String email,
      AuthenticationMethod method,
      String failureReason,
      String ipAddress,
      String userAgent) {
    return new AuthenticationAttempt(userId, email, method, false, failureReason, ipAddress, userAgent, null);
  }

  /** Factory method for failed authentication attempts when the user is unknown. */
  public static AuthenticationAttempt failureUnknownUser(
      String email,
      AuthenticationMethod method,
      String failureReason,
      String ipAddress,
      String userAgent) {
    return new AuthenticationAttempt(null, email, method, false, failureReason, ipAddress, userAgent, null);
  }

  // Business methods
  public boolean isSuccess() {
    return success != null && success;
  }

  public boolean isFailure() {
    return success != null && !success;
  }

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

  /** Authentication method enumeration */
  public enum AuthenticationMethod {
    PASSWORD,
    OAUTH2
  }
}
