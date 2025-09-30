package com.platform.audit.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Specialized repository for compliance and reporting audit queries.
 * Extracted from AuditEventRepository to follow Single Responsibility Principle.
 */
@Repository
public interface ComplianceRepository extends JpaRepository<AuditEvent, UUID> {

    /**
     * Generate compliance report for data access activities.
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND ae.resourceType IN ('USER_DATA', 'PAYMENT_DATA', 'PERSONAL_INFO')
        AND ae.createdAt BETWEEN :startDate AND :endDate
        ORDER BY ae.createdAt DESC
            """)
    List<AuditEvent> findDataAccessEvents(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find all administrative actions for compliance reporting.
     */
    @Query("""
        SELECT ae FROM AuditEvent ae
        WHERE ae.organizationId = :organizationId
        AND ae.action IN ('CREATE_USER', 'DELETE_USER', 'MODIFY_PERMISSIONS', 'EXPORT_DATA')
        AND ae.createdAt BETWEEN :startDate AND :endDate
        ORDER BY ae.createdAt DESC
            """)
    List<AuditEvent> findAdministrativeActions(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Get data retention compliance statistics.
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total_records,
            COUNT(CASE WHEN created_at < :retentionCutoff THEN 1 END) as records_past_retention,
            COUNT(CASE WHEN action IN ('DELETE', 'PURGE') THEN 1 END) as deletion_events,
            COUNT(DISTINCT resource_id) as unique_resources
        FROM audit_events
        WHERE organization_id = :organizationId
        AND resource_type IN ('USER_DATA', 'PAYMENT_DATA', 'PERSONAL_INFO')
            """, nativeQuery = true)
    DataRetentionStatistics getDataRetentionStats(
            @Param("organizationId") UUID organizationId,
            @Param("retentionCutoff") Instant retentionCutoff);

    /**
     * Data retention statistics projection.
     */
    interface DataRetentionStatistics {
        long getTotalRecords();
        long getRecordsPastRetention();
        long getDeletionEvents();
        long getUniqueResources();
    }
}