package com.platform.audit.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Specialized repository for security-related audit analytics.
 * Extracted from AuditEventRepository to follow Single Responsibility Principle.
 */
@Repository
public interface SecurityAnalyticsRepository extends JpaRepository<AuditEvent, UUID> {

    /**
     * Finds security-related events after a specific timestamp.
     */
    @Query("""
        SELECT ae FROM AuditEvent ae 
        WHERE ae.organizationId = :organizationId 
        AND ae.action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY') 
        AND ae.createdAt >= :since 
        ORDER BY ae.createdAt DESC
            """)
    List<AuditEvent> findSecurityEventsAfter(
                @Param("organizationId") UUID organizationId,
                @Param("since") Instant since);

    /**
     * Finds IP addresses with high number of failed login attempts.
     */
    @Query("""
        SELECT ae.ipAddress, COUNT(ae) as count 
        FROM AuditEvent ae 
        WHERE ae.organizationId = :organizationId 
        AND ae.action = 'LOGIN_FAILED' 
        AND ae.createdAt >= :since 
        GROUP BY ae.ipAddress 
        HAVING COUNT(ae) > :threshold
            """)
    List<Object[]> findSuspiciousIpAddresses(
                @Param("organizationId") UUID organizationId,
                @Param("since") Instant since,
                @Param("threshold") long threshold);

    /**
     * Find failed login attempts grouped by user.
     */
    @Query("""
        SELECT ae.actorId, COUNT(ae) as attemptCount
        FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND ae.action = 'LOGIN_FAILED'
        AND ae.createdAt >= :since
        GROUP BY ae.actorId
        HAVING COUNT(ae) >= :threshold
        ORDER BY attemptCount DESC
            """)
    List<Object[]> findFailedLoginsByUser(
                @Param("organizationId") UUID organizationId,
            @Param("since") Instant since,
            @Param("threshold") long threshold);

    /**
     * Get security event statistics for a time period.
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_events,
            COUNT(CASE WHEN action = 'LOGIN_FAILED' THEN 1 END) as failed_logins,
            COUNT(CASE WHEN action = 'UNAUTHORIZED_ACCESS' THEN 1 END) as unauthorized_attempts,
            COUNT(DISTINCT ip_address) as unique_ips,
            COUNT(DISTINCT actor_id) as unique_users
        FROM audit_events
        WHERE organization_id = :organizationId
        AND created_at BETWEEN :startDate AND :endDate
        AND action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY')
        """, nativeQuery = true)
    SecurityEventStatistics getSecurityStatistics(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Security event statistics projection.
     */
    interface SecurityEventStatistics {
        long getTotalEvents();
        long getFailedLogins();
        long getUnauthorizedAttempts();
        long getUniqueIps();
        long getUniqueUsers();
    }
}