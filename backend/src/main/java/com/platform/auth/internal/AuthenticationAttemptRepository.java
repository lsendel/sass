package com.platform.auth.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for authentication attempt audit records. Supports security monitoring, rate limiting,
 * and GDPR compliance queries.
 */
@Repository
public interface AuthenticationAttemptRepository
    extends JpaRepository<AuthenticationAttempt, UUID> {

  /** Find all authentication attempts for a specific user, ordered by most recent first. */
  List<AuthenticationAttempt> findByUserIdOrderByAttemptTimeDesc(UUID userId);

  /** Find authentication attempts for a user within a time range. */
  @Query(
      "SELECT a FROM AuthenticationAttempt a WHERE a.userId = :userId "
          + "AND a.attemptTime BETWEEN :startTime AND :endTime "
          + "ORDER BY a.attemptTime DESC")
  List<AuthenticationAttempt> findByUserIdAndTimeBetween(
      @Param("userId") UUID userId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Find failed authentication attempts for a specific user within a time range. Used for account
   * lockout logic.
   */
  @Query(
      "SELECT a FROM AuthenticationAttempt a WHERE a.userId = :userId "
          + "AND a.success = false "
          + "AND a.attemptTime BETWEEN :startTime AND :endTime "
          + "ORDER BY a.attemptTime DESC")
  List<AuthenticationAttempt> findFailedAttemptsByUserIdAndTimeBetween(
      @Param("userId") UUID userId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Count failed authentication attempts from an IP address within a time range. Used for IP-based
   * rate limiting.
   */
  @Query(
      "SELECT COUNT(a) FROM AuthenticationAttempt a WHERE a.ipAddress = :ipAddress "
          + "AND a.success = false "
          + "AND a.attemptTime BETWEEN :startTime AND :endTime")
  long countFailedAttemptsByIpAndTimeBetween(
      @Param("ipAddress") String ipAddress,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Find authentication attempts by email address within a time range. Used for security monitoring
   * across different user accounts.
   */
  @Query(
      "SELECT a FROM AuthenticationAttempt a WHERE a.email = :email "
          + "AND a.attemptTime BETWEEN :startTime AND :endTime "
          + "ORDER BY a.attemptTime DESC")
  List<AuthenticationAttempt> findByEmailAndTimeBetween(
      @Param("email") String email,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Find authentication attempts by IP address within a time range. Used for security analysis and
   * threat detection.
   */
  @Query(
      "SELECT a FROM AuthenticationAttempt a WHERE a.ipAddress = :ipAddress "
          + "AND a.attemptTime BETWEEN :startTime AND :endTime "
          + "ORDER BY a.attemptTime DESC")
  List<AuthenticationAttempt> findByIpAddressAndTimeBetween(
      @Param("ipAddress") String ipAddress,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /**
   * Find all authentication attempts by method and success status within a time range. Used for
   * analytics and monitoring.
   */
  @Query(
      "SELECT a FROM AuthenticationAttempt a WHERE a.method = :method "
          + "AND a.success = :success "
          + "AND a.attemptTime BETWEEN :startTime AND :endTime "
          + "ORDER BY a.attemptTime DESC")
  List<AuthenticationAttempt> findByMethodAndSuccessAndTimeBetween(
      @Param("method") AuthenticationAttempt.AuthenticationMethod method,
      @Param("success") Boolean success,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /** Count successful authentication attempts for a user within a time range. */
  @Query(
      "SELECT COUNT(a) FROM AuthenticationAttempt a WHERE a.userId = :userId "
          + "AND a.success = true "
          + "AND a.attemptTime BETWEEN :startTime AND :endTime")
  long countSuccessfulAttemptsByUserIdAndTimeBetween(
      @Param("userId") UUID userId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /** Count failed authentication attempts for a user within a time range. */
  @Query(
      "SELECT COUNT(a) FROM AuthenticationAttempt a WHERE a.userId = :userId "
          + "AND a.success = false "
          + "AND a.attemptTime BETWEEN :startTime AND :endTime")
  long countFailedAttemptsByUserIdAndTimeBetween(
      @Param("userId") UUID userId,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime);

  /** Find recent authentication attempts for security monitoring. */
  @Query(
      "SELECT a FROM AuthenticationAttempt a WHERE a.attemptTime >= :cutoffTime "
          + "ORDER BY a.attemptTime DESC")
  List<AuthenticationAttempt> findRecentAttempts(@Param("cutoffTime") Instant cutoffTime);

  /**
   * Delete old authentication attempts for GDPR compliance. Should be called periodically to clean
   * up old audit data.
   */
  @Query("DELETE FROM AuthenticationAttempt a WHERE a.attemptTime < :cutoffTime")
  void deleteOldAttempts(@Param("cutoffTime") Instant cutoffTime);

  /** Find suspicious patterns: multiple failed attempts from same IP for different emails. */
  @Query(
      "SELECT a FROM AuthenticationAttempt a WHERE a.ipAddress = :ipAddress "
          + "AND a.success = false "
          + "AND a.attemptTime BETWEEN :startTime AND :endTime "
          + "GROUP BY a.email "
          + "HAVING COUNT(a) >= :threshold")
  List<AuthenticationAttempt> findSuspiciousActivityByIp(
      @Param("ipAddress") String ipAddress,
      @Param("startTime") Instant startTime,
      @Param("endTime") Instant endTime,
      @Param("threshold") int threshold);

  /** Get authentication statistics for monitoring dashboard. */
  @Query(
      "SELECT "
          + "a.method, "
          + "a.success, "
          + "COUNT(a) as attemptCount "
          + "FROM AuthenticationAttempt a "
          + "WHERE a.attemptTime BETWEEN :startTime AND :endTime "
          + "GROUP BY a.method, a.success")
  List<Object[]> getAuthenticationStatistics(
      @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
