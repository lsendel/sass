package com.platform.auth.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for OAuth2AuditEvent entities.
 * Manages audit trail for OAuth2 authentication and authorization events.
 */
@Repository
public interface OAuth2AuditEventRepository extends JpaRepository<OAuth2AuditEvent, Long> {

    /**
     * Find audit events by user ID within a time range
     */
    List<OAuth2AuditEvent> findByUserIdAndEventTimestampBetweenOrderByEventTimestampDesc(
        String userId, Instant startTime, Instant endTime);

    /**
     * Find audit events by provider within a time range
     */
    List<OAuth2AuditEvent> findByProviderAndEventTimestampBetweenOrderByEventTimestampDesc(
        String provider, Instant startTime, Instant endTime);

    /**
     * Find audit events by severity within a time range
     */
    List<OAuth2AuditEvent> findBySeverityAndEventTimestampBetweenOrderByEventTimestampDesc(
        OAuth2AuditEvent.AuditSeverity severity, Instant startTime, Instant endTime);

    /**
     * Find failed authentication attempts from an IP address
     */
    List<OAuth2AuditEvent> findByIpAddressAndSuccessFalseAndEventTimestampBetweenOrderByEventTimestampDesc(
        String ipAddress, Instant startTime, Instant endTime);

    /**
     * Find audit events by session ID
     */
    List<OAuth2AuditEvent> findBySessionIdOrderByEventTimestampDesc(String sessionId);

    /**
     * Find audit events by event type
     */
    List<OAuth2AuditEvent> findByEventTypeOrderByEventTimestampDesc(OAuth2AuditEvent.OAuth2EventType eventType);

    /**
     * Find audit events by event type within a time range
     */
    List<OAuth2AuditEvent> findByEventTypeAndEventTimestampBetween(
        OAuth2AuditEvent.OAuth2EventType eventType, Instant startTime, Instant endTime);

    /**
     * Count events by type within a time range
     */
    long countByEventTypeAndEventTimestampBetween(
        OAuth2AuditEvent.OAuth2EventType eventType, Instant startTime, Instant endTime);

    /**
     * Find critical events (paginated)
     */
    Page<OAuth2AuditEvent> findBySeverity(OAuth2AuditEvent.AuditSeverity severity, Pageable pageable);

    /**
     * Find events with errors
     */
    List<OAuth2AuditEvent> findBySuccessFalseAndEventTimestampAfterOrderByEventTimestampDesc(Instant after);

    /**
     * Find events by correlation ID
     */
    List<OAuth2AuditEvent> findByCorrelationIdOrderByEventTimestampAsc(String correlationId);

    /**
     * Count failed login attempts for a user within a time window
     */
    @Query("SELECT COUNT(e) FROM OAuth2AuditEvent e WHERE e.userId = :userId AND " +
           "e.eventType = 'USER_LOGIN' AND e.success = false AND " +
           "e.eventTimestamp > :since")
    long countFailedLoginAttempts(@Param("userId") String userId, @Param("since") Instant since);

    /**
     * Count failed login attempts from an IP within a time window
     */
    @Query("SELECT COUNT(e) FROM OAuth2AuditEvent e WHERE e.ipAddress = :ipAddress AND " +
           "e.eventType IN ('AUTHORIZATION_FAILED', 'TOKEN_EXCHANGE_FAILED') AND " +
           "e.eventTimestamp > :since")
    long countFailedAttemptsFromIp(@Param("ipAddress") String ipAddress, @Param("since") Instant since);

    /**
     * Get event statistics by type
     */
    @Query("SELECT e.eventType, COUNT(e), " +
           "COUNT(CASE WHEN e.success = true THEN 1 END), " +
           "COUNT(CASE WHEN e.success = false THEN 1 END) " +
           "FROM OAuth2AuditEvent e " +
           "WHERE e.eventTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY e.eventType")
    List<Object[]> getEventStatistics(@Param("startTime") Instant startTime,
                                     @Param("endTime") Instant endTime);

    /**
     * Get provider statistics
     */
    @Query("SELECT e.provider, COUNT(e), " +
           "COUNT(CASE WHEN e.success = true THEN 1 END), " +
           "COUNT(CASE WHEN e.success = false THEN 1 END), " +
           "AVG(e.durationMs) " +
           "FROM OAuth2AuditEvent e " +
           "WHERE e.provider IS NOT NULL AND " +
           "e.eventTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY e.provider")
    List<Object[]> getProviderStatistics(@Param("startTime") Instant startTime,
                                        @Param("endTime") Instant endTime);

    /**
     * Find suspicious activity patterns
     */
    @Query("SELECT e.ipAddress, e.userId, COUNT(e) as attempts " +
           "FROM OAuth2AuditEvent e " +
           "WHERE e.eventType IN ('PKCE_VALIDATION_FAILED', 'STATE_VALIDATION_FAILED', " +
           "'SUSPICIOUS_ACTIVITY', 'RATE_LIMIT_EXCEEDED') AND " +
           "e.eventTimestamp > :since " +
           "GROUP BY e.ipAddress, e.userId " +
           "HAVING COUNT(e) > :threshold")
    List<Object[]> findSuspiciousActivityPatterns(@Param("since") Instant since,
                                                 @Param("threshold") long threshold);

    /**
     * Clean up old audit events (retention policy)
     */
    void deleteByEventTimestampBefore(Instant before);

    /**
     * Find GDPR-related events for a user
     */
    @Query("SELECT e FROM OAuth2AuditEvent e WHERE e.userId = :userId AND " +
           "e.eventType IN ('GDPR_DATA_EXPORT', 'GDPR_DATA_DELETION', " +
           "'CONSENT_GRANTED', 'CONSENT_REVOKED') " +
           "ORDER BY e.eventTimestamp DESC")
    List<OAuth2AuditEvent> findGdprEventsForUser(@Param("userId") String userId);

    /**
     * Get performance metrics
     */
    @Query("SELECT e.eventType, " +
           "MIN(e.durationMs), MAX(e.durationMs), AVG(e.durationMs), " +
           "PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY e.durationMs), " +
           "PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY e.durationMs), " +
           "PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY e.durationMs) " +
           "FROM OAuth2AuditEvent e " +
           "WHERE e.durationMs IS NOT NULL AND " +
           "e.eventTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY e.eventType")
    List<Object[]> getPerformanceMetrics(@Param("startTime") Instant startTime,
                                        @Param("endTime") Instant endTime);
}