package com.platform.audit.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Simplified test repository for H2 compatibility.
 * Contains only basic queries without PostgreSQL-specific functions.
 * This replaces AuditEventRepository during testing to avoid H2 compatibility issues.
 */
@Repository
@Profile("test")
public interface TestAuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<AuditEvent> findByOrganizationIdAndActorIdOrderByCreatedAtDesc(
            UUID organizationId, UUID actorId, Pageable pageable);

    List<AuditEvent> findByActorIdOrderByCreatedAtDesc(UUID actorId);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId "
            + "AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
    Page<AuditEvent> findByOrganizationIdAndCreatedAtBetween(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query("SELECT COUNT(ae) FROM AuditEvent ae WHERE ae.organizationId = :organizationId "
            + "AND ae.action = :action AND ae.createdAt >= :since")
    long countByOrganizationIdAndActionAndCreatedAtAfter(
            @Param("organizationId") UUID organizationId,
            @Param("action") String action,
            @Param("since") Instant since);

    @Query("SELECT DISTINCT ae.action FROM AuditEvent ae WHERE ae.organizationId = :organizationId "
            + "ORDER BY ae.action")
    List<String> findDistinctActionsByOrganizationId(@Param("organizationId") UUID organizationId);
}