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

/**
 * Spring Data JPA repository for {@link AuditEvent} entities.
 *
 * <p>This interface provides a rich set of methods for querying audit events, including standard
 * finders, custom JPQL queries, and complex native queries for security analytics and compliance
 * reporting. It also includes methods for data management, such as deletion and redaction, to
 * support GDPR requirements.
 * </p>
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  /**
   * Finds a paginated list of audit events for a specific organization, sorted by creation date in
   * descending order.
   *
   * @param organizationId the ID of the organization
   * @param pageable pagination information
   * @return a {@link Page} of {@link AuditEvent}s
   */
  Page<AuditEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

  /**
   * Finds a paginated list of audit events for a specific organization and actor, sorted by
   * creation date in descending order.
   *
   * @param organizationId the ID of the organization
   * @param actorId the ID of the actor
   * @param pageable pagination information
   * @return a {@link Page} of {@link AuditEvent}s
   */
  Page<AuditEvent> findByOrganizationIdAndActorIdOrderByCreatedAtDesc(
      UUID organizationId, UUID actorId, Pageable pageable);

  /**
   * Finds a list of audit events for a specific actor, sorted by creation date in descending order.
   *
   * @param actorId the ID of the actor
   * @return a {@link List} of {@link AuditEvent}s
   */
  List<AuditEvent> findByActorIdOrderByCreatedAtDesc(UUID actorId);

  /**
   * Finds a paginated list of audit events for a specific organization and action type, sorted by
   * creation date in descending order.
   *
   * @param organizationId the ID of the organization
   * @param action the action type (e.g., "user.login")
   * @param pageable pagination information
   * @return a {@link Page} of {@link AuditEvent}s
   */
  Page<AuditEvent> findByOrganizationIdAndActionOrderByCreatedAtDesc(
      UUID organizationId, String action, Pageable pageable);

  /**
   * Finds a paginated list of audit events for an organization within a specific date range.
   *
   * @param organizationId the ID of the organization
   * @param startDate the start of the date range (inclusive)
   * @param endDate the end of the date range (inclusive)
   * @param pageable pagination information
   * @return a {@link Page} of {@link AuditEvent}s
   */
  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
  Page<AuditEvent> findByOrganizationIdAndCreatedAtBetween(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      Pageable pageable);

  /**
   * Finds a paginated list of audit events for an organization with a specific action within a date
   * range.
   *
   * @param organizationId the ID of the organization
   * @param action the action type
   * @param startDate the start of the date range
   * @param endDate the end of the date range
   * @param pageable pagination information
   * @return a {@link Page} of {@link AuditEvent}s
   */
  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action = :action AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
  Page<AuditEvent> findByOrganizationIdAndActionAndCreatedAtBetween(
      @Param("organizationId") UUID organizationId,
      @Param("action") String action,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      Pageable pageable);

  /**
   * Finds all audit events related to a specific resource within an organization.
   *
   * @param organizationId the ID of the organization
   * @param resourceType the type of the resource
   * @param resourceId the ID of the resource
   * @return a list of matching {@link AuditEvent}s
   */
  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.resourceType = :resourceType AND ae.resourceId = :resourceId ORDER BY ae.createdAt DESC")
  List<AuditEvent> findByOrganizationIdAndResourceTypeAndResourceId(
      @Param("organizationId") UUID organizationId,
      @Param("resourceType") String resourceType,
      @Param("resourceId") String resourceId);

  /**
   * Finds all audit events from a specific IP address within a date range for an organization.
   *
   * @param organizationId the ID of the organization
   * @param ipAddress the source IP address
   * @param startDate the start of the date range
   * @param endDate the end of the date range
   * @return a list of matching {@link AuditEvent}s
   */
  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.resourceType = :resourceType AND ae.resourceId = :resourceId ORDER BY ae.createdAt DESC")
  List<AuditEvent> findByResourceTypeAndResourceId(
      @Param("resourceType") String resourceType,
      @Param("resourceId") String resourceId);

  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.ipAddress = :ipAddress AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
  List<AuditEvent> findByOrganizationIdAndIpAddressAndCreatedAtBetween(
      @Param("organizationId") UUID organizationId,
      @Param("ipAddress") String ipAddress,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * Counts the number of times a specific action has occurred for an organization since a given
   * timestamp.
   *
   * @param organizationId the ID of the organization
   * @param action the action type
   * @param since the timestamp to count from
   * @return the total count of matching events
   */
  @Query(
      "SELECT COUNT(ae) FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action = :action AND ae.createdAt >= :since")
  long countByOrganizationIdAndActionAndCreatedAtAfter(
      @Param("organizationId") UUID organizationId,
      @Param("action") String action,
      @Param("since") Instant since);

  /**
   * Counts the number of events initiated by a specific actor for an organization since a given
   * timestamp.
   *
   * @param organizationId the ID of the organization
   * @param actorId the ID of the actor
   * @param since the timestamp to count from
   * @return the total count of matching events
   */
  @Query(
      "SELECT COUNT(ae) FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.actorId = :actorId AND ae.createdAt >= :since")
  long countByOrganizationIdAndActorIdAndCreatedAtAfter(
      @Param("organizationId") UUID organizationId,
      @Param("actorId") UUID actorId,
      @Param("since") Instant since);

  /**
   * Finds all distinct action types recorded for a given organization.
   *
   * @param organizationId the ID of the organization
   * @return a sorted list of distinct action strings
   */
  @Query(
      "SELECT DISTINCT ae.action FROM AuditEvent ae WHERE ae.organizationId = :organizationId ORDER BY ae.action")
  List<String> findDistinctActionsByOrganizationId(@Param("organizationId") UUID organizationId);

  /**
   * Finds all distinct resource types recorded for a given organization.
   *
   * @param organizationId the ID of the organization
   * @return a sorted list of distinct resource type strings
   */
  @Query(
      "SELECT DISTINCT ae.resourceType FROM AuditEvent ae WHERE ae.organizationId = :organizationId ORDER BY ae.resourceType")
  List<String> findDistinctResourceTypesByOrganizationId(
      @Param("organizationId") UUID organizationId);

  // GDPR compliance - data retention and deletion

  /**
   * Deletes all audit events created before a specified cutoff date.
   *
   * @param cutoffDate the date before which events will be deleted
   * @return the number of events deleted
   */
  @Modifying
  @Query("DELETE FROM AuditEvent ae WHERE ae.createdAt < :cutoffDate")
  int deleteEventsOlderThan(@Param("cutoffDate") Instant cutoffDate);

  /**
   * Deletes all audit events associated with a specific organization.
   *
   * @param organizationId the ID of the organization whose events will be deleted
   * @return the number of events deleted
   */
  @Modifying
  @Query("DELETE FROM AuditEvent ae WHERE ae.organizationId = :organizationId")
  int deleteByOrganizationId(@Param("organizationId") UUID organizationId);

  /**
   * Deletes all audit events initiated by a specific actor.
   *
   * @param actorId the ID of the actor whose events will be deleted
   * @return the number of events deleted
   */
  @Modifying
  @Query("DELETE FROM AuditEvent ae WHERE ae.actorId = :actorId")
  int deleteByActorId(@Param("actorId") UUID actorId);

  /**
   * Redacts user data in the details field for a specific actor.
   *
   * @param actorId the ID of the actor whose data will be redacted
   * @param redactedData the JSON string representing the redacted data
   * @return the number of events updated
   */
  @Modifying
  @Query("UPDATE AuditEvent ae SET ae.details = :redactedData WHERE ae.actorId = :actorId")
  int redactUserData(@Param("actorId") UUID actorId, @Param("redactedData") String redactedData);

  /**
   * Performs a free-text search across action, resourceType, and correlationId fields.
   *
   * @param organizationId the ID of the organization
   * @param searchTerm the term to search for
   * @param pageable pagination information
   * @return a {@link Page} of matching {@link AuditEvent}s
   */
  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND (ae.action LIKE %:searchTerm% OR ae.resourceType LIKE %:searchTerm% OR ae.correlationId LIKE %:searchTerm%) ORDER BY ae.createdAt DESC")
  Page<AuditEvent> searchAuditEvents(
      @Param("organizationId") UUID organizationId,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

  // Security analysis queries

  /**
   * Finds critical security-related events that occurred after a given timestamp.
   *
   * @param organizationId the ID of the organization
   * @param since the timestamp to search from
   * @return a list of security-related {@link AuditEvent}s
   */
  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY') AND ae.createdAt >= :since ORDER BY ae.createdAt DESC")
  List<AuditEvent> findSecurityEventsAfter(
      @Param("organizationId") UUID organizationId, @Param("since") Instant since);

  /**
   * Finds IP addresses with a high number of failed login attempts.
   *
   * @param organizationId the ID of the organization
   * @param since the timestamp to search from
   * @param threshold the minimum number of failed attempts to be considered suspicious
   * @return a list of {@code Object[]} arrays, where each array contains the IP address (String)
   *     and the count of failed logins (Long)
   */
  @Query(
      "SELECT ae.ipAddress, COUNT(ae) as count FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action = 'LOGIN_FAILED' AND ae.createdAt >= :since GROUP BY ae.ipAddress HAVING COUNT(ae) > :threshold")
  List<Object[]> findSuspiciousIpAddresses(
      @Param("organizationId") UUID organizationId,
      @Param("since") Instant since,
      @Param("threshold") long threshold);

  // ===== ADVANCED SECURITY ANALYTICS =====

  /**
   * Analyzes trends of security incidents over a specified period.
   *
   * @param organizationId the ID of the organization
   * @param period the time unit for grouping (e.g., 'day', 'hour')
   * @param startDate the start of the analysis window
   * @param endDate the end of the analysis window
   * @return a list of {@code Object[]} arrays with the trend data. Each array contains:
   *     <ul>
   *       <li>[0]: Time period (Timestamp)
   *       <li>[1]: Action type (String)
   *       <li>[2]: Incident count (Long)
   *       <li>[3]: Unique actors count (Long)
   *       <li>[4]: Unique IPs count (Long)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getSecurityIncidentTrends(
      @Param("organizationId") UUID organizationId,
      @Param("period") String period,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * Detects anomalous user behavior based on activity volume and IP address diversity.
   *
   * @param organizationId the ID of the organization
   * @param lookbackDate the start date for the lookback period
   * @param activityThreshold the minimum number of actions to be considered anomalous
   * @param ipThreshold the minimum number of unique IPs to be considered anomalous
   * @return a list of {@code Object[]} arrays identifying users with anomalous behavior. Each array
   *     contains:
   *     <ul>
   *       <li>[0]: Actor ID (UUID)
   *       <li>[1]: Total actions (Long)
   *       <li>[2]: Unique actions (Long)
   *       <li>[3]: Unique IPs (Long)
   *       <li>[4]: First activity timestamp (Timestamp)
   *       <li>[5]: Last activity timestamp (Timestamp)
   *       <li>[6]: Activity span in hours (Double)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> detectUserBehaviorAnomalies(
      @Param("organizationId") UUID organizationId,
      @Param("lookbackDate") Instant lookbackDate,
      @Param("activityThreshold") int activityThreshold,
      @Param("ipThreshold") int ipThreshold);

  /**
   * Analyzes access patterns by geographic location (approximated by IP address).
   *
   * @param organizationId the ID of the organization
   * @param startDate the start date for the analysis
   * @param topN the number of top IP addresses to return
   * @return a list of {@code Object[]} arrays with access pattern data. Each array contains:
   *     <ul>
   *       <li>[0]: IP Address (String)
   *       <li>[1]: Access count (Long)
   *       <li>[2]: Unique users (Long)
   *       <li>[3]: Aggregated actions (String)
   *       <li>[4]: First seen timestamp (Timestamp)
   *       <li>[5]: Last seen timestamp (Timestamp)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getAccessPatternsByLocation(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("topN") int topN);

  /**
   * Generates a report on data access patterns for compliance purposes.
   *
   * @param organizationId the ID of the organization
   * @param startDate the start date for the report
   * @param endDate the end date for the report
   * @return a list of {@code Object[]} arrays with data access details. Each array contains:
   *     <ul>
   *       <li>[0]: Resource type (String)
   *       <li>[1]: Action type (String)
   *       <li>[2]: Access count (Long)
   *       <li>[3]: Unique accessors (Long)
   *       <li>[4]: Unique resources (Long)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getDataAccessComplianceReport(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * Calculates a risk score for each user based on their security-related activities.
   *
   * @param organizationId the ID of the organization
   * @param lookbackDate the start date for the risk assessment
   * @return a list of {@code Object[]} arrays with user risk scores. Each array contains:
   *     <ul>
   *       <li>[0]: User ID (UUID)
   *       <li>[1]: User email (String)
   *       <li>[2]: User name (String)
   *       <li>[3]: Calculated risk score (BigDecimal)
   *       <li>[4]: Total incident count (BigDecimal)
   *       <li>[5]: Unique IP count (BigDecimal)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getUserRiskScores(
      @Param("organizationId") UUID organizationId, @Param("lookbackDate") Instant lookbackDate);

  /**
   * Analyzes session-related activity patterns by day of the week and hour of the day.
   *
   * @param organizationId the ID of the organization
   * @param startDate the start date for the analysis
   * @return a list of {@code Object[]} arrays with session patterns. Each array contains:
   *     <ul>
   *       <li>[0]: Day of the week (Double)
   *       <li>[1]: Hour of the day (Double)
   *       <li>[2]: Activity count (Long)
   *       <li>[3]: Unique users (Long)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getSessionPatternAnalysis(
      @Param("organizationId") UUID organizationId, @Param("startDate") Instant startDate);

  /**
   * Analyzes failed authentication attempts, grouping by source IP and user agent.
   *
   * @param organizationId the ID of the organization
   * @param startDate the start date for the analysis
   * @param failureThreshold the minimum number of failures to include in the result
   * @return a list of {@code Object[]} arrays with failure details. Each array contains:
   *     <ul>
   *       <li>[0]: IP Address (String)
   *       <li>[1]: User Agent (String)
   *       <li>[2]: Failure count (Long)
   *       <li>[3]: Targeted users (Long)
   *       <li>[4]: First attempt timestamp (Timestamp)
   *       <li>[5]: Last attempt timestamp (Timestamp)
   *       <li>[6]: Aggregated failure reasons (String)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getFailedAuthenticationAnalysis(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("failureThreshold") int failureThreshold);

  /**
   * Tracks data export activities for privacy and compliance monitoring.
   *
   * @param organizationId the ID of the organization
   * @param startDate the start date for the report
   * @return a list of {@code Object[]} arrays summarizing export activities. Each array contains:
   *     <ul>
   *       <li>[0]: Actor ID (UUID)
   *       <li>[1]: Export count (Long)
   *       <li>[2]: Exported resources (String)
   *       <li>[3]: Resource IDs (String)
   *       <li>[4]: First export timestamp (Timestamp)
   *       <li>[5]: Last export timestamp (Timestamp)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getDataExportComplianceReport(
      @Param("organizationId") UUID organizationId, @Param("startDate") Instant startDate);

  /**
   * Monitors administrative actions within the system.
   *
   * @param organizationId the ID of the organization
   * @param startDate the start date for the report
   * @return a list of {@code Object[]} arrays detailing admin actions. Each array contains:
   *     <ul>
   *       <li>[0]: Actor ID (UUID)
   *       <li>[1]: Admin email (String)
   *       <li>[2]: Admin name (String)
   *       <li>[3]: Action type (String)
   *       <li>[4]: Resource type (String)
   *       <li>[5]: Action count (Long)
   *       <li>[6]: Last action timestamp (Timestamp)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getAdministrativeActionReport(
      @Param("organizationId") UUID organizationId, @Param("startDate") Instant startDate);

  /**
   * Finds and groups security events by correlation ID for forensic investigation.
   *
   * @param organizationId the ID of the organization
   * @param startDate the start date of the investigation window
   * @param endDate the end date of the investigation window
   * @return a list of {@code Object[]} arrays of correlated events. Each array contains:
   *     <ul>
   *       <li>[0]: Correlation ID (String)
   *       <li>[1]: Related events count (Long)
   *       <li>[2]: Aggregated actions (String)
   *       <li>[3]: Aggregated resources (String)
   *       <li>[4]: Event start timestamp (Timestamp)
   *       <li>[5]: Event end timestamp (Timestamp)
   *       <li>[6]: Involved users count (Long)
   *     </ul>
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getCorrelatedSecurityEvents(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  // Additional methods for comprehensive audit service

  /**
   * Finds audit events for a specific user within a given time frame.
   *
   * @param userId the ID of the user
   * @param fromDate the start of the time frame
   * @param toDate the end of the time frame
   * @return a list of {@link AuditEvent}s
   */
  @Query(
      "SELECT ae FROM AuditEvent ae WHERE ae.actorId = :userId "
          + "AND ae.createdAt BETWEEN :fromDate AND :toDate "
          + "ORDER BY ae.createdAt DESC")
  List<AuditEvent> findByUserIdAndTimeframe(
      @Param("userId") String userId,
      @Param("fromDate") Instant fromDate,
      @Param("toDate") Instant toDate);

  /**
   * Finds all recent audit events that occurred since a given timestamp.
   *
   * @param since the timestamp to find events from
   * @return a list of recent {@link AuditEvent}s
   */
  @Query("SELECT ae FROM AuditEvent ae WHERE ae.createdAt >= :since " + "ORDER BY ae.createdAt DESC")
  List<AuditEvent> findRecentEvents(@Param("since") Instant since);

  /**
   * Finds audit events using a comprehensive set of optional filtering criteria.
   *
   * @param userId optional user ID to filter by
   * @param eventTypes optional list of event types to filter by
   * @param severities optional list of severities to filter by
   * @param fromDate optional start date for the time range
   * @param toDate optional end date for the time range
   * @param ipAddress optional IP address to filter by
   * @param correlationId optional correlation ID to filter by
   * @param pageable pagination information
   * @return a {@link Page} of matching {@link AuditEvent}s
   */
  @Query(
      "SELECT ae FROM AuditEvent ae WHERE "
          + "(:userId IS NULL OR CAST(ae.actorId AS string) = :userId) AND "
          + "(:eventTypes IS NULL OR ae.action IN :eventTypes) AND "
          + "(:severities IS NULL OR ae.severity IN :severities) AND "
          + "(:fromDate IS NULL OR ae.createdAt >= :fromDate) AND "
          + "(:toDate IS NULL OR ae.createdAt <= :toDate) AND "
          + "(:ipAddress IS NULL OR ae.ipAddress = :ipAddress) AND "
          + "(:correlationId IS NULL OR ae.correlationId = :correlationId) "
          + "ORDER BY ae.createdAt DESC")
  Page<AuditEvent> findByAdvancedCriteria(
      @Param("userId") String userId,
      @Param("eventTypes") List<String> eventTypes,
      @Param("severities") List<String> severities,
      @Param("fromDate") Instant fromDate,
      @Param("toDate") Instant toDate,
      @Param("ipAddress") String ipAddress,
      @Param("correlationId") String correlationId,
      Pageable pageable);

  /**
   * Archives audit events older than a specified threshold by moving them to an archive table.
   *
   * @param threshold the timestamp before which events will be archived
   * @return the number of events archived
   */
  @Modifying
  @Query(
      value =
          "INSERT INTO audit_events_archive SELECT * FROM audit_events "
              + "WHERE created_at < :threshold",
      nativeQuery = true)
  long archiveEventsOlderThan(@Param("threshold") Instant threshold);

  /**
   * Marks old audit events as compressed. (Note: The actual compression mechanism is not handled
   * here).
   *
   * @param threshold the timestamp before which events will be marked
   * @return the number of events marked as compressed
   */
  @Modifying
  @Query(
      "UPDATE AuditEvent ae SET ae.compressed = true "
          + "WHERE ae.createdAt < :threshold AND ae.compressed = false")
  long compressEventsOlderThan(@Param("threshold") Instant threshold);
}
