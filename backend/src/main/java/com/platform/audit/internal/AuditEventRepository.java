package com.platform.audit.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  Page<AuditEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

  Page<AuditEvent> findByOrganizationIdAndActorIdOrderByCreatedAtDesc(
      UUID organizationId, UUID actorId, Pageable pageable);

  Page<AuditEvent> findByOrganizationIdAndActionOrderByCreatedAtDesc(
      UUID organizationId, String action, Pageable pageable);

  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
  Page<AuditEvent> findByOrganizationIdAndCreatedAtBetween(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      Pageable pageable);

  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action = :action AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
  Page<AuditEvent> findByOrganizationIdAndActionAndCreatedAtBetween(
      @Param("organizationId") UUID organizationId,
      @Param("action") String action,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      Pageable pageable);

  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.resourceType = :resourceType AND ae.resourceId = :resourceId ORDER BY ae.createdAt DESC")
  List<AuditEvent> findByOrganizationIdAndResourceTypeAndResourceId(
      @Param("organizationId") UUID organizationId,
      @Param("resourceType") String resourceType,
      @Param("resourceId") String resourceId);

  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.ipAddress = :ipAddress AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
  List<AuditEvent> findByOrganizationIdAndIpAddressAndCreatedAtBetween(
      @Param("organizationId") UUID organizationId,
      @Param("ipAddress") String ipAddress,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  // This method name was causing issues - removing duplicate
  // Use findByOrganizationIdAndIpAddressAndCreatedAtBetween instead

  @Query(
      "SELECT COUNT(ae) FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action = :action AND ae.createdAt >= :since")
  long countByOrganizationIdAndActionAndCreatedAtAfter(
      @Param("organizationId") UUID organizationId,
      @Param("action") String action,
      @Param("since") Instant since);

  @Query(
      "SELECT COUNT(ae) FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.actorId = :actorId AND ae.createdAt >= :since")
  long countByOrganizationIdAndActorIdAndCreatedAtAfter(
      @Param("organizationId") UUID organizationId,
      @Param("actorId") UUID actorId,
      @Param("since") Instant since);

  @Query(
      "SELECT DISTINCT ae.action FROM AuditEvent ae WHERE ae.organizationId = :organizationId ORDER BY ae.action")
  List<String> findDistinctActionsByOrganizationId(@Param("organizationId") UUID organizationId);

  @Query(
      "SELECT DISTINCT ae.resourceType FROM AuditEvent ae WHERE ae.organizationId = :organizationId ORDER BY ae.resourceType")
  List<String> findDistinctResourceTypesByOrganizationId(
      @Param("organizationId") UUID organizationId);

  // GDPR compliance - data retention and deletion
  @Modifying
  @Query("DELETE FROM AuditEvent ae WHERE ae.createdAt < :cutoffDate")
  int deleteEventsOlderThan(@Param("cutoffDate") Instant cutoffDate);

  @Modifying
  @Query("DELETE FROM AuditEvent ae WHERE ae.organizationId = :organizationId")
  int deleteByOrganizationId(@Param("organizationId") UUID organizationId);

  @Modifying
  @Query("DELETE FROM AuditEvent ae WHERE ae.actorId = :actorId")
  int deleteByActorId(@Param("actorId") UUID actorId);

  // PII redaction for GDPR
  @Modifying
  @Query("UPDATE AuditEvent ae SET ae.details = :redactedData WHERE ae.actorId = :actorId")
  int redactUserData(@Param("actorId") UUID actorId, @Param("redactedData") String redactedData);

  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND (ae.action LIKE %:searchTerm% OR ae.resourceType LIKE %:searchTerm% OR ae.correlationId LIKE %:searchTerm%) ORDER BY ae.createdAt DESC")
  Page<AuditEvent> searchAuditEvents(
      @Param("organizationId") UUID organizationId,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

  // Security analysis queries
  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY') AND ae.createdAt >= :since ORDER BY ae.createdAt DESC")
  List<AuditEvent> findSecurityEventsAfter(
      @Param("organizationId") UUID organizationId, @Param("since") Instant since);

  @Query(
      "SELECT ae.ipAddress, COUNT(ae) as count FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action = 'LOGIN_FAILED' AND ae.createdAt >= :since GROUP BY ae.ipAddress HAVING COUNT(ae) > :threshold")
  List<Object[]> findSuspiciousIpAddresses(
      @Param("organizationId") UUID organizationId,
      @Param("since") Instant since,
      @Param("threshold") long threshold);

  // ===== ADVANCED SECURITY ANALYTICS =====

  /** Security incident trend analysis */
  @Query(value = """
      SELECT
          DATE_TRUNC(:period, created_at) as time_period,
          action,
          COUNT(*) as incident_count,
          COUNT(DISTINCT actor_id) as unique_actors,
          COUNT(DISTINCT ip_address) as unique_ips
      FROM audit_events
      WHERE organization_id = :organizationId
          AND action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY', 'ACCOUNT_LOCKOUT')
          AND created_at BETWEEN :startDate AND :endDate
      GROUP BY DATE_TRUNC(:period, created_at), action
      ORDER BY time_period, action
      """, nativeQuery = true)
  List<Object[]> getSecurityIncidentTrends(
      @Param("organizationId") UUID organizationId,
      @Param("period") String period,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /** User behavior anomaly detection */
  @Query(value = """
      SELECT
          actor_id,
          COUNT(*) as total_actions,
          COUNT(DISTINCT action) as unique_actions,
          COUNT(DISTINCT ip_address) as unique_ips,
          MIN(created_at) as first_activity,
          MAX(created_at) as last_activity,
          EXTRACT(EPOCH FROM (MAX(created_at) - MIN(created_at))) / 3600 as activity_span_hours
      FROM audit_events
      WHERE organization_id = :organizationId
          AND created_at >= :lookbackDate
          AND actor_id IS NOT NULL
      GROUP BY actor_id
      HAVING COUNT(*) > :activityThreshold
          OR COUNT(DISTINCT ip_address) > :ipThreshold
      ORDER BY total_actions DESC
      """, nativeQuery = true)
  List<Object[]> detectUserBehaviorAnomalies(
      @Param("organizationId") UUID organizationId,
      @Param("lookbackDate") Instant lookbackDate,
      @Param("activityThreshold") int activityThreshold,
      @Param("ipThreshold") int ipThreshold);

  /** Geographic access pattern analysis */
  @Query(value = """
      SELECT
          ip_address,
          COUNT(*) as access_count,
          COUNT(DISTINCT actor_id) as unique_users,
          STRING_AGG(DISTINCT action, ', ') as actions,
          MIN(created_at) as first_seen,
          MAX(created_at) as last_seen
      FROM audit_events
      WHERE organization_id = :organizationId
          AND created_at >= :startDate
          AND ip_address IS NOT NULL
      GROUP BY ip_address
      ORDER BY access_count DESC
      LIMIT :topN
      """, nativeQuery = true)
  List<Object[]> getAccessPatternsByLocation(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("topN") int topN);

  /** Compliance reporting - data access patterns */
  @Query(value = """
      SELECT
          resource_type,
          action,
          COUNT(*) as access_count,
          COUNT(DISTINCT actor_id) as unique_accessors,
          COUNT(DISTINCT resource_id) as unique_resources
      FROM audit_events
      WHERE organization_id = :organizationId
          AND action IN ('READ', 'create', 'update', 'delete', 'export')
          AND created_at BETWEEN :startDate AND :endDate
      GROUP BY resource_type, action
      ORDER BY resource_type, access_count DESC
      """, nativeQuery = true)
  List<Object[]> getDataAccessComplianceReport(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /** Risk score calculation for users */
  @Query(value = """
      SELECT
          u.id as user_id,
          u.email,
          u.name,
          COALESCE(security_events.risk_score, 0) as risk_score,
          COALESCE(security_events.incident_count, 0) as incident_count,
          COALESCE(security_events.unique_ips, 0) as unique_ip_count
      FROM users u
      LEFT JOIN (
          SELECT
              actor_id,
              COUNT(*) as incident_count,
              COUNT(DISTINCT ip_address) as unique_ips,
              SUM(CASE
                  WHEN action = 'LOGIN_FAILED' THEN 1
                  WHEN action = 'UNAUTHORIZED_ACCESS' THEN 3
                  WHEN action = 'SUSPICIOUS_ACTIVITY' THEN 5
                  WHEN action = 'ACCOUNT_LOCKOUT' THEN 2
                  ELSE 0
              END) as risk_score
          FROM audit_events
          WHERE created_at >= :lookbackDate
              AND action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY', 'ACCOUNT_LOCKOUT')
          GROUP BY actor_id
      ) security_events ON u.id = security_events.actor_id
      WHERE u.organization_id = :organizationId
          AND u.deleted_at IS NULL
      ORDER BY risk_score DESC, incident_count DESC
      """, nativeQuery = true)
  List<Object[]> getUserRiskScores(
      @Param("organizationId") UUID organizationId,
      @Param("lookbackDate") Instant lookbackDate);

  /** Session pattern analysis */
  @Query(value = """
      SELECT
          EXTRACT(DOW FROM created_at) as day_of_week,
          EXTRACT(HOUR FROM created_at) as hour_of_day,
          COUNT(*) as activity_count,
          COUNT(DISTINCT actor_id) as unique_users
      FROM audit_events
      WHERE organization_id = :organizationId
          AND action IN ('login', 'logout', 'session_refresh')
          AND created_at >= :startDate
      GROUP BY EXTRACT(DOW FROM created_at), EXTRACT(HOUR FROM created_at)
      ORDER BY day_of_week, hour_of_day
      """, nativeQuery = true)
  List<Object[]> getSessionPatternAnalysis(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate);

  /** Failed authentication analysis by source */
  @Query(value = """
      SELECT
          ip_address,
          user_agent,
          COUNT(*) as failure_count,
          COUNT(DISTINCT actor_id) as targeted_users,
          MIN(created_at) as first_attempt,
          MAX(created_at) as last_attempt,
          STRING_AGG(DISTINCT details, '; ') as failure_reasons
      FROM audit_events
      WHERE organization_id = :organizationId
          AND action = 'LOGIN_FAILED'
          AND created_at >= :startDate
      GROUP BY ip_address, user_agent
      HAVING COUNT(*) >= :failureThreshold
      ORDER BY failure_count DESC
      """, nativeQuery = true)
  List<Object[]> getFailedAuthenticationAnalysis(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("failureThreshold") int failureThreshold);

  /** Data export and privacy compliance tracking */
  @Query(value = """
      SELECT
          actor_id,
          COUNT(*) as export_count,
          STRING_AGG(DISTINCT resource_type, ', ') as exported_resources,
          STRING_AGG(DISTINCT resource_id, ', ') as resource_ids,
          MIN(created_at) as first_export,
          MAX(created_at) as last_export
      FROM audit_events
      WHERE organization_id = :organizationId
          AND action = 'export'
          AND created_at >= :startDate
      GROUP BY actor_id
      ORDER BY export_count DESC
      """, nativeQuery = true)
  List<Object[]> getDataExportComplianceReport(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate);

  /** Administrative action monitoring */
  @Query(value = """
      SELECT
          ae.actor_id,
          u.email as admin_email,
          u.name as admin_name,
          ae.action,
          ae.resource_type,
          COUNT(*) as action_count,
          MAX(ae.created_at) as last_action
      FROM audit_events ae
      JOIN users u ON ae.actor_id = u.id
      WHERE ae.organization_id = :organizationId
          AND ae.action IN ('create', 'update', 'delete', 'grant_permission', 'revoke_permission')
          AND ae.created_at >= :startDate
      GROUP BY ae.actor_id, u.email, u.name, ae.action, ae.resource_type
      ORDER BY action_count DESC
      """, nativeQuery = true)
  List<Object[]> getAdministrativeActionReport(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate);

  /** Forensic investigation - correlated events */
  @Query(value = """
      SELECT
          correlation_id,
          COUNT(*) as related_events,
          STRING_AGG(DISTINCT action, ', ') as actions,
          STRING_AGG(DISTINCT resource_type, ', ') as resources,
          MIN(created_at) as event_start,
          MAX(created_at) as event_end,
          COUNT(DISTINCT actor_id) as involved_users
      FROM audit_events
      WHERE organization_id = :organizationId
          AND correlation_id IS NOT NULL
          AND created_at BETWEEN :startDate AND :endDate
      GROUP BY correlation_id
      HAVING COUNT(*) > 1
      ORDER BY related_events DESC, event_start DESC
      """, nativeQuery = true)
  List<Object[]> getCorrelatedSecurityEvents(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);
}
