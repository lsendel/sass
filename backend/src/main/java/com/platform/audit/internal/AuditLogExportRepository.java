package com.platform.audit.internal;

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
 * Repository for audit log export request operations.
 */
@Repository
public interface AuditLogExportRepository extends JpaRepository<AuditLogExportRequest, UUID> {

    /**
     * Find export requests by user ID with pagination.
     */
    Page<AuditLogExportRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find export requests by user and organization.
     */
    Page<AuditLogExportRequest> findByUserIdAndOrganizationIdOrderByCreatedAtDesc(
        UUID userId, UUID organizationId, Pageable pageable);

    /**
     * Find export request by download token.
     */
    Optional<AuditLogExportRequest> findByDownloadToken(String downloadToken);

    /**
     * Find export requests by status.
     */
    List<AuditLogExportRequest> findByStatus(AuditLogExportRequest.ExportStatus status);

    /**
     * Find pending export requests for processing.
     */
    @Query("""
        SELECT e FROM AuditLogExportRequest e
        WHERE e.status = 'PENDING'
        ORDER BY e.createdAt ASC
        """)
    List<AuditLogExportRequest> findPendingExports();

    /**
     * Find exports that need cleanup (expired downloads).
     */
    @Query("""
        SELECT e FROM AuditLogExportRequest e
        WHERE e.status = 'COMPLETED'
        AND e.downloadExpiresAt < :now
        """)
    List<AuditLogExportRequest> findExpiredExports(@Param("now") Instant now);

    /**
     * Find stale processing exports (stuck in processing state).
     */
    @Query("""
        SELECT e FROM AuditLogExportRequest e
        WHERE e.status = 'PROCESSING'
        AND e.startedAt < :cutoffTime
        """)
    List<AuditLogExportRequest> findStaleProcessingExports(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Count active exports for a user.
     */
    @Query("""
        SELECT COUNT(e) FROM AuditLogExportRequest e
        WHERE e.userId = :userId
        AND e.status IN ('PENDING', 'PROCESSING')
        """)
    long countActiveExportsForUser(@Param("userId") UUID userId);

    /**
     * Count exports created in the last period.
     */
    @Query("""
        SELECT COUNT(e) FROM AuditLogExportRequest e
        WHERE e.userId = :userId
        AND e.createdAt > :since
        """)
    long countExportsForUserSince(@Param("userId") UUID userId, @Param("since") Instant since);

    /**
     * Delete old export requests.
     */
    @Modifying
    @Query("""
        DELETE FROM AuditLogExportRequest e
        WHERE e.createdAt < :cutoffTime
        AND e.status IN ('COMPLETED', 'FAILED', 'EXPIRED')
        """)
    int deleteOldExports(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Update export status to expired for completed downloads past expiry.
     */
    @Modifying
    @Query("""
        UPDATE AuditLogExportRequest e
        SET e.status = 'EXPIRED'
        WHERE e.status = 'COMPLETED'
        AND e.downloadExpiresAt < :now
        """)
    int markExpiredDownloads(@Param("now") Instant now);

    /**
     * Find export requests by organization (for admin purposes).
     */
    @Query("""
        SELECT e FROM AuditLogExportRequest e
        WHERE e.organizationId = :organizationId
        ORDER BY e.createdAt DESC
        """)
    Page<AuditLogExportRequest> findByOrganizationId(
        @Param("organizationId") UUID organizationId, Pageable pageable);

    /**
     * Get export statistics for an organization.
     */
    @Query("""
        SELECT
            COUNT(e) as totalExports,
            COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) as completedExports,
            COUNT(CASE WHEN e.status = 'FAILED' THEN 1 END) as failedExports,
            AVG(CASE WHEN e.completedAt IS NOT NULL AND e.startedAt IS NOT NULL
                THEN EXTRACT(EPOCH FROM (e.completedAt - e.startedAt)) END) as avgProcessingTimeSeconds
        FROM AuditLogExportRequest e
        WHERE e.organizationId = :organizationId
        AND e.createdAt >= :since
        """)
    ExportStatistics getExportStatistics(@Param("organizationId") UUID organizationId,
                                       @Param("since") Instant since);

    /**
     * Export statistics projection.
     */
    interface ExportStatistics {
        long getTotalExports();
        long getCompletedExports();
        long getFailedExports();
        Double getAvgProcessingTimeSeconds();
    }
}