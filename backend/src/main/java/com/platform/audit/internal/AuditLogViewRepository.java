package com.platform.audit.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for user-facing audit log viewing operations.
 *
 * This repository provides secure, filtered access to audit logs with proper
 * tenant isolation and user permission constraints. It's designed specifically
 * for the user-facing audit log viewer feature.
 */
@Repository
public interface AuditLogViewRepository extends JpaRepository<AuditEvent, UUID> {

    /**
     * Find audit logs for a specific user with pagination and organization isolation.
     * Only returns logs the user has permission to view.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access (for permission checking)
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        ORDER BY ae.createdAt DESC
        """)
    Page<AuditEvent> findUserAccessibleLogs(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions,
        Pageable pageable);

    /**
     * Find audit logs with date range filtering.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @param startDate start of date range (inclusive)
     * @param endDate end of date range (inclusive)
     * @param pageable pagination parameters
     * @return page of audit events within date range
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        AND ae.createdAt BETWEEN :startDate AND :endDate
        ORDER BY ae.createdAt DESC
        """)
    Page<AuditEvent> findUserAccessibleLogsInDateRange(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable);

    /**
     * Find audit logs with action type filtering.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @param actionTypes list of action types to include
     * @param pageable pagination parameters
     * @return page of audit events matching action types
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        AND ae.action IN :actionTypes
        ORDER BY ae.createdAt DESC
        """)
    Page<AuditEvent> findUserAccessibleLogsByActions(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions,
        @Param("actionTypes") List<String> actionTypes,
        Pageable pageable);

    /**
     * Find audit logs with resource type filtering.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @param resourceTypes list of resource types to include
     * @param pageable pagination parameters
     * @return page of audit events matching resource types
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        AND ae.resourceType IN :resourceTypes
        ORDER BY ae.createdAt DESC
        """)
    Page<AuditEvent> findUserAccessibleLogsByResourceTypes(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions,
        @Param("resourceTypes") List<String> resourceTypes,
        Pageable pageable);

    /**
     * Comprehensive search with multiple filters.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @param searchText optional text search across action and resource type
     * @param actionTypes optional list of action types
     * @param resourceTypes optional list of resource types
     * @param startDate optional start date
     * @param endDate optional end date
     * @param pageable pagination parameters
     * @return page of audit events matching all filters
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        AND (:searchText IS NULL OR
             LOWER(ae.action) LIKE LOWER(CONCAT('%', :searchText, '%')) OR
             LOWER(ae.resourceType) LIKE LOWER(CONCAT('%', :searchText, '%')) OR
             LOWER(ae.correlationId) LIKE LOWER(CONCAT('%', :searchText, '%')))
        AND (:actionTypes IS NULL OR ae.action IN :actionTypes)
        AND (:resourceTypes IS NULL OR ae.resourceType IN :resourceTypes)
        AND (:startDate IS NULL OR ae.createdAt >= :startDate)
        AND (:endDate IS NULL OR ae.createdAt <= :endDate)
        ORDER BY ae.createdAt DESC
        """)
    Page<AuditEvent> findUserAccessibleLogsWithFilters(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions,
        @Param("searchText") String searchText,
        @Param("actionTypes") List<String> actionTypes,
        @Param("resourceTypes") List<String> resourceTypes,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable);

    /**
     * Find a specific audit log entry that the user has permission to view.
     *
     * @param id the audit event ID
     * @param organizationId the organization to verify access
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @return the audit event if accessible, empty otherwise
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.id = :id
        AND ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        """)
    Optional<AuditEvent> findUserAccessibleLogById(
        @Param("id") UUID id,
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions);

    /**
     * Count total accessible logs for a user.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @return total count of accessible logs
     */
    @Query("""
        SELECT COUNT(ae) FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        """)
    long countUserAccessibleLogs(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions);

    /**
     * Count accessible logs with filters applied.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @param searchText optional text search
     * @param actionTypes optional action type filter
     * @param resourceTypes optional resource type filter
     * @param startDate optional start date
     * @param endDate optional end date
     * @return count of logs matching filters
     */
    @Query("""
        SELECT COUNT(ae) FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        AND (:searchText IS NULL OR
             LOWER(ae.action) LIKE LOWER(CONCAT('%', :searchText, '%')) OR
             LOWER(ae.resourceType) LIKE LOWER(CONCAT('%', :searchText, '%')) OR
             LOWER(ae.correlationId) LIKE LOWER(CONCAT('%', :searchText, '%')))
        AND (:actionTypes IS NULL OR ae.action IN :actionTypes)
        AND (:resourceTypes IS NULL OR ae.resourceType IN :resourceTypes)
        AND (:startDate IS NULL OR ae.createdAt >= :startDate)
        AND (:endDate IS NULL OR ae.createdAt <= :endDate)
        """)
    long countUserAccessibleLogsWithFilters(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions,
        @Param("searchText") String searchText,
        @Param("actionTypes") List<String> actionTypes,
        @Param("resourceTypes") List<String> resourceTypes,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate);

    /**
     * Get distinct action types available to the user.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @return list of distinct action types
     */
    @Query("""
        SELECT DISTINCT ae.action FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        ORDER BY ae.action
        """)
    List<String> findDistinctActionTypesForUser(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions);

    /**
     * Get distinct resource types available to the user.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @return list of distinct resource types
     */
    @Query("""
        SELECT DISTINCT ae.resourceType FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        AND ae.resourceType IS NOT NULL
        ORDER BY ae.resourceType
        """)
    List<String> findDistinctResourceTypesForUser(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions);

    /**
     * Find recent activity for the user (last 24 hours).
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @param since the timestamp to search from (typically 24 hours ago)
     * @param pageable pagination parameters
     * @return recent audit events
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        AND ae.createdAt >= :since
        ORDER BY ae.createdAt DESC
        """)
    Page<AuditEvent> findRecentActivityForUser(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions,
        @Param("since") Instant since,
        Pageable pageable);

    /**
     * Find logs by correlation ID that the user can access.
     * Useful for viewing related events in a transaction.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param includeSystemActions whether to include system-generated actions
     * @param correlationId the correlation ID to search for
     * @return list of related audit events
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND (ae.actorId = :userId OR :includeSystemActions = true)
        AND ae.correlationId = :correlationId
        ORDER BY ae.createdAt ASC
        """)
    List<AuditEvent> findUserAccessibleLogsByCorrelationId(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("includeSystemActions") boolean includeSystemActions,
        @Param("correlationId") String correlationId);

    /**
     * Security-focused query to find login/logout events for the user.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param since the timestamp to search from
     * @param pageable pagination parameters
     * @return authentication-related audit events
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND ae.actorId = :userId
        AND ae.action IN ('user.login', 'user.logout', 'auth.failed', 'auth.locked')
        AND ae.createdAt >= :since
        ORDER BY ae.createdAt DESC
        """)
    Page<AuditEvent> findAuthenticationEventsForUser(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("since") Instant since,
        Pageable pageable);

    /**
     * Find data access events for compliance reporting.
     * Shows what data the user has accessed or modified.
     *
     * @param organizationId the organization to filter by
     * @param userId the user requesting access
     * @param startDate start of reporting period
     * @param endDate end of reporting period
     * @param pageable pagination parameters
     * @return data access audit events
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND ae.actorId = :userId
        AND ae.action IN ('data.read', 'data.created', 'data.updated', 'data.deleted', 'data.exported')
        AND ae.createdAt BETWEEN :startDate AND :endDate
        ORDER BY ae.createdAt DESC
        """)
    Page<AuditEvent> findDataAccessEventsForUser(
        @Param("organizationId") UUID organizationId,
        @Param("userId") UUID userId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable);
}