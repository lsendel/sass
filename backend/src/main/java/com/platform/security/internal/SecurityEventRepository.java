package com.platform.security.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SecurityEvent entities with optimized time-series queries.
 *
 * This repository provides specialized methods for querying security events
 * with focus on time-based operations, aggregations, and real-time monitoring.
 *
 * Key features:
 * - Time-range queries with optimal index usage
 * - Severity-based filtering for alert systems
 * - Correlation ID tracking for incident investigation
 * - Bulk operations for high-volume event processing
 * - Statistical aggregations for dashboard metrics
 * - Resolution tracking for incident management
 */
@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    // Time-based queries (optimized with timestamp index)

    /**
     * Find all events within a time range, ordered by timestamp descending
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY se.timestamp DESC")
    Page<SecurityEvent> findByTimestampBetween(@Param("startTime") Instant startTime,
                                              @Param("endTime") Instant endTime,
                                              Pageable pageable);

    /**
     * Find recent events within the last specified duration
     *
     * @param since Timestamp threshold
     * @param pageable Pagination parameters
     * @return Page of recent security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.timestamp >= :since " +
           "ORDER BY se.timestamp DESC")
    Page<SecurityEvent> findRecentEvents(@Param("since") Instant since, Pageable pageable);

    // Severity and event type filtering

    /**
     * Find critical and high severity events within time range
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of high-priority security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.timestamp BETWEEN :startTime AND :endTime " +
           "AND se.severity IN ('CRITICAL', 'HIGH') ORDER BY se.timestamp DESC")
    Page<SecurityEvent> findHighSeverityEvents(@Param("startTime") Instant startTime,
                                              @Param("endTime") Instant endTime,
                                              Pageable pageable);

    /**
     * Find events by type and severity within time range
     *
     * @param eventType Type of security event
     * @param severity Event severity level
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of filtered security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.eventType = :eventType " +
           "AND se.severity = :severity AND se.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY se.timestamp DESC")
    Page<SecurityEvent> findByTypeAndSeverity(@Param("eventType") SecurityEvent.EventType eventType,
                                             @Param("severity") SecurityEvent.Severity severity,
                                             @Param("startTime") Instant startTime,
                                             @Param("endTime") Instant endTime,
                                             Pageable pageable);

    /**
     * Find events by source module within time range
     *
     * @param sourceModule Source module name
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of module-specific events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.sourceModule = :sourceModule " +
           "AND se.timestamp BETWEEN :startTime AND :endTime ORDER BY se.timestamp DESC")
    Page<SecurityEvent> findBySourceModule(@Param("sourceModule") String sourceModule,
                                          @Param("startTime") Instant startTime,
                                          @Param("endTime") Instant endTime,
                                          Pageable pageable);

    // User and session tracking

    /**
     * Find events associated with a specific user
     *
     * @param userId User identifier
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of user-related security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.userId = :userId " +
           "AND se.timestamp BETWEEN :startTime AND :endTime ORDER BY se.timestamp DESC")
    Page<SecurityEvent> findByUserId(@Param("userId") String userId,
                                    @Param("startTime") Instant startTime,
                                    @Param("endTime") Instant endTime,
                                    Pageable pageable);

    /**
     * Find events associated with a specific session
     *
     * @param sessionId Session identifier
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of session-related security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.sessionId = :sessionId " +
           "AND se.timestamp BETWEEN :startTime AND :endTime ORDER BY se.timestamp DESC")
    List<SecurityEvent> findBySessionId(@Param("sessionId") String sessionId,
                                       @Param("startTime") Instant startTime,
                                       @Param("endTime") Instant endTime);

    /**
     * Find events from a specific source IP within time range
     *
     * @param sourceIp Source IP address
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of IP-related security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.sourceIp = :sourceIp " +
           "AND se.timestamp BETWEEN :startTime AND :endTime ORDER BY se.timestamp DESC")
    Page<SecurityEvent> findBySourceIp(@Param("sourceIp") String sourceIp,
                                      @Param("startTime") Instant startTime,
                                      @Param("endTime") Instant endTime,
                                      Pageable pageable);

    // Correlation and incident tracking

    /**
     * Find all events with the same correlation ID
     *
     * @param correlationId Correlation identifier
     * @return List of correlated security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.correlationId = :correlationId " +
           "ORDER BY se.timestamp ASC")
    List<SecurityEvent> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Find unresolved events within time range
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of unresolved security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.resolved = false " +
           "AND se.timestamp BETWEEN :startTime AND :endTime ORDER BY se.timestamp DESC")
    Page<SecurityEvent> findUnresolvedEvents(@Param("startTime") Instant startTime,
                                            @Param("endTime") Instant endTime,
                                            Pageable pageable);

    /**
     * Find events resolved by a specific user or system
     *
     * @param resolvedBy Resolver identifier
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of resolved security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.resolvedBy = :resolvedBy " +
           "AND se.resolvedAt BETWEEN :startTime AND :endTime ORDER BY se.resolvedAt DESC")
    Page<SecurityEvent> findByResolvedBy(@Param("resolvedBy") String resolvedBy,
                                        @Param("startTime") Instant startTime,
                                        @Param("endTime") Instant endTime,
                                        Pageable pageable);

    // Statistical and aggregation queries

    /**
     * Count events by severity within time range
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Number of events in time range
     */
    @Query("SELECT COUNT(se) FROM SecurityEvent se WHERE se.timestamp BETWEEN :startTime AND :endTime")
    long countEventsByTimeRange(@Param("startTime") Instant startTime,
                               @Param("endTime") Instant endTime);

    /**
     * Count events by severity level within time range
     *
     * @param severity Event severity level
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Number of events with specified severity
     */
    @Query("SELECT COUNT(se) FROM SecurityEvent se WHERE se.severity = :severity " +
           "AND se.timestamp BETWEEN :startTime AND :endTime")
    long countBySeverity(@Param("severity") SecurityEvent.Severity severity,
                        @Param("startTime") Instant startTime,
                        @Param("endTime") Instant endTime);

    /**
     * Count events by event type within time range
     *
     * @param eventType Type of security event
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Number of events of specified type
     */
    @Query("SELECT COUNT(se) FROM SecurityEvent se WHERE se.eventType = :eventType " +
           "AND se.timestamp BETWEEN :startTime AND :endTime")
    long countByEventType(@Param("eventType") SecurityEvent.EventType eventType,
                         @Param("startTime") Instant startTime,
                         @Param("endTime") Instant endTime);

    /**
     * Count unresolved events within time range
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Number of unresolved events
     */
    @Query("SELECT COUNT(se) FROM SecurityEvent se WHERE se.resolved = false " +
           "AND se.timestamp BETWEEN :startTime AND :endTime")
    long countUnresolvedEvents(@Param("startTime") Instant startTime,
                              @Param("endTime") Instant endTime);

    /**
     * Get event count statistics grouped by hour for time-series charts
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of hourly event counts
     */
    @Query(value = "SELECT DATE_TRUNC('hour', timestamp) as hour, COUNT(*) as count " +
                   "FROM security_events WHERE timestamp BETWEEN :startTime AND :endTime " +
                   "GROUP BY DATE_TRUNC('hour', timestamp) ORDER BY hour",
           nativeQuery = true)
    List<Object[]> getHourlyEventCounts(@Param("startTime") Instant startTime,
                                       @Param("endTime") Instant endTime);

    /**
     * Get top source IPs by event count within time range
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param limit Maximum number of results
     * @return List of [source_ip, count] pairs
     */
    @Query(value = "SELECT source_ip, COUNT(*) as count FROM security_events " +
                   "WHERE timestamp BETWEEN :startTime AND :endTime AND source_ip IS NOT NULL " +
                   "GROUP BY source_ip ORDER BY count DESC LIMIT :limit",
           nativeQuery = true)
    List<Object[]> getTopSourceIps(@Param("startTime") Instant startTime,
                                  @Param("endTime") Instant endTime,
                                  @Param("limit") int limit);

    /**
     * Get security event trends by severity over time periods
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of [time_period, severity, count] triplets
     */
    @Query(value = "SELECT DATE_TRUNC('hour', timestamp) as period, severity, COUNT(*) as count " +
                   "FROM security_events WHERE timestamp BETWEEN :startTime AND :endTime " +
                   "GROUP BY DATE_TRUNC('hour', timestamp), severity ORDER BY period, severity",
           nativeQuery = true)
    List<Object[]> getSeverityTrends(@Param("startTime") Instant startTime,
                                    @Param("endTime") Instant endTime);

    // Bulk operations for high-volume processing

    /**
     * Bulk mark events as resolved by correlation ID
     *
     * @param correlationId Correlation identifier
     * @param resolvedBy Resolver identifier
     * @param resolvedAt Resolution timestamp
     * @return Number of updated events
     */
    @Modifying
    @Query("UPDATE SecurityEvent se SET se.resolved = true, se.resolvedBy = :resolvedBy, " +
           "se.resolvedAt = :resolvedAt WHERE se.correlationId = :correlationId AND se.resolved = false")
    int bulkResolveByCorrelationId(@Param("correlationId") String correlationId,
                                  @Param("resolvedBy") String resolvedBy,
                                  @Param("resolvedAt") Instant resolvedAt);

    /**
     * Bulk delete old resolved events for cleanup
     *
     * @param cutoffTime Events older than this will be deleted
     * @return Number of deleted events
     */
    @Modifying
    @Query("DELETE FROM SecurityEvent se WHERE se.resolved = true AND se.resolvedAt < :cutoffTime")
    int deleteOldResolvedEvents(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Bulk delete events older than specified time for retention cleanup
     *
     * @param cutoffTime Events older than this will be deleted
     * @return Number of deleted events
     */
    @Modifying
    @Query("DELETE FROM SecurityEvent se WHERE se.timestamp < :cutoffTime")
    int deleteEventsByAge(@Param("cutoffTime") Instant cutoffTime);

    // Advanced search queries

    /**
     * Find events with custom criteria using native query
     *
     * @param userId Optional user ID filter
     * @param sourceModule Optional source module filter
     * @param severity Optional severity filter
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of filtered security events
     */
    @Query("SELECT se FROM SecurityEvent se WHERE " +
           "(:userId IS NULL OR se.userId = :userId) AND " +
           "(:sourceModule IS NULL OR se.sourceModule = :sourceModule) AND " +
           "(:severity IS NULL OR se.severity = :severity) AND " +
           "se.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY se.timestamp DESC")
    Page<SecurityEvent> findWithFilters(@Param("userId") String userId,
                                       @Param("sourceModule") String sourceModule,
                                       @Param("severity") SecurityEvent.Severity severity,
                                       @Param("startTime") Instant startTime,
                                       @Param("endTime") Instant endTime,
                                       Pageable pageable);

    /**
     * Find the most recent event for a user
     *
     * @param userId User identifier
     * @return Optional most recent security event
     */
    @Query("SELECT se FROM SecurityEvent se WHERE se.userId = :userId " +
           "ORDER BY se.timestamp DESC")
    Optional<SecurityEvent> findMostRecentByUserId(@Param("userId") String userId);

    /**
     * Find events containing specific text in details (JSONB search)
     *
     * @param searchTerm Text to search for in event details
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination parameters
     * @return Page of matching security events
     */
    @Query(value = "SELECT * FROM security_events WHERE " +
                   "details::text ILIKE %:searchTerm% AND " +
                   "timestamp BETWEEN :startTime AND :endTime " +
                   "ORDER BY timestamp DESC",
           nativeQuery = true)
    Page<SecurityEvent> searchInDetails(@Param("searchTerm") String searchTerm,
                                       @Param("startTime") Instant startTime,
                                       @Param("endTime") Instant endTime,
                                       Pageable pageable);

    // Real-time monitoring queries

    /**
     * Find events that occurred in the last N minutes for real-time monitoring
     *
     * @param minutes Number of minutes to look back
     * @return List of recent security events
     */
    @Query(value = "SELECT * FROM security_events WHERE " +
                   "timestamp >= NOW() - INTERVAL ':minutes minutes' " +
                   "ORDER BY timestamp DESC",
           nativeQuery = true)
    List<SecurityEvent> findEventsInLastMinutes(@Param("minutes") int minutes);

    /**
     * Find suspicious activity patterns by detecting multiple events from same IP
     *
     * @param minEventCount Minimum number of events to be considered suspicious
     * @param timeWindowMinutes Time window in minutes
     * @return List of [source_ip, event_count] pairs for suspicious IPs
     */
    @Query(value = "SELECT source_ip, COUNT(*) as event_count FROM security_events " +
                   "WHERE timestamp >= NOW() - INTERVAL ':timeWindowMinutes minutes' " +
                   "AND source_ip IS NOT NULL " +
                   "GROUP BY source_ip " +
                   "HAVING COUNT(*) >= :minEventCount " +
                   "ORDER BY event_count DESC",
           nativeQuery = true)
    List<Object[]> findSuspiciousActivityByIp(@Param("minEventCount") int minEventCount,
                                             @Param("timeWindowMinutes") int timeWindowMinutes);

    /**
     * Check if system is under potential attack (high volume of critical events)
     *
     * @param criticalThreshold Number of critical events that indicates attack
     * @param timeWindowMinutes Time window to check
     * @return True if potential attack detected
     */
    @Query("SELECT CASE WHEN COUNT(se) >= :criticalThreshold THEN true ELSE false END " +
           "FROM SecurityEvent se WHERE se.severity = 'CRITICAL' " +
           "AND se.timestamp >= :since")
    boolean isPotentialAttackDetected(@Param("criticalThreshold") long criticalThreshold,
                                     @Param("since") Instant since);

    /**
     * Get the latest event timestamp for real-time polling
     *
     * @return Most recent event timestamp
     */
    @Query("SELECT MAX(se.timestamp) FROM SecurityEvent se")
    Optional<Instant> getLatestEventTimestamp();
}