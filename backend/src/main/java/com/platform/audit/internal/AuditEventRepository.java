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
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<AuditEvent> findByOrganizationIdAndActorIdOrderByCreatedAtDesc(UUID organizationId, UUID actorId, Pageable pageable);

    Page<AuditEvent> findByOrganizationIdAndActionOrderByCreatedAtDesc(UUID organizationId, String action, Pageable pageable);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
    Page<AuditEvent> findByOrganizationIdAndCreatedAtBetween(@Param("organizationId") UUID organizationId,
                                                            @Param("startDate") Instant startDate,
                                                            @Param("endDate") Instant endDate,
                                                            Pageable pageable);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action = :action AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
    Page<AuditEvent> findByOrganizationIdAndActionAndCreatedAtBetween(@Param("organizationId") UUID organizationId,
                                                                         @Param("action") String action,
                                                                         @Param("startDate") Instant startDate,
                                                                         @Param("endDate") Instant endDate,
                                                                         Pageable pageable);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.resourceType = :resourceType AND ae.resourceId = :resourceId ORDER BY ae.createdAt DESC")
    List<AuditEvent> findByOrganizationIdAndResourceTypeAndResourceId(@Param("organizationId") UUID organizationId,
                                                                      @Param("resourceType") String resourceType,
                                                                      @Param("resourceId") String resourceId);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.ipAddress = :ipAddress AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
    List<AuditEvent> findByOrganizationIdAndIpAddressAndCreatedAtBetween(@Param("organizationId") UUID organizationId,
                                                                         @Param("ipAddress") String ipAddress,
                                                                         @Param("startDate") Instant startDate,
                                                                         @Param("endDate") Instant endDate);

    // Spring Data might be trying to create this method - add it explicitly
    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.ipAddress = :ipAddress AND ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
    List<AuditEvent> findByOrganizationIdAndIpAddressAndTimestampBetween(@Param("organizationId") UUID organizationId,
                                                                         @Param("ipAddress") String ipAddress,
                                                                         @Param("startDate") Instant startDate,
                                                                         @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(ae) FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action = :action AND ae.createdAt >= :since")
    long countByOrganizationIdAndActionAndCreatedAtAfter(@Param("organizationId") UUID organizationId,
                                                           @Param("action") String action,
                                                           @Param("since") Instant since);

    @Query("SELECT COUNT(ae) FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.actorId = :actorId AND ae.createdAt >= :since")
    long countByOrganizationIdAndActorIdAndCreatedAtAfter(@Param("organizationId") UUID organizationId,
                                                        @Param("actorId") UUID actorId,
                                                        @Param("since") Instant since);

    @Query("SELECT DISTINCT ae.action FROM AuditEvent ae WHERE ae.organizationId = :organizationId ORDER BY ae.action")
    List<String> findDistinctActionsByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT DISTINCT ae.resourceType FROM AuditEvent ae WHERE ae.organizationId = :organizationId ORDER BY ae.resourceType")
    List<String> findDistinctResourceTypesByOrganizationId(@Param("organizationId") UUID organizationId);

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

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND (ae.action LIKE %:searchTerm% OR ae.resourceType LIKE %:searchTerm% OR ae.correlationId LIKE %:searchTerm%) ORDER BY ae.createdAt DESC")
    Page<AuditEvent> searchAuditEvents(@Param("organizationId") UUID organizationId,
                                       @Param("searchTerm") String searchTerm,
                                       Pageable pageable);

    // Security analysis queries
    @Query("SELECT ae FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'SUSPICIOUS_ACTIVITY') AND ae.createdAt >= :since ORDER BY ae.createdAt DESC")
    List<AuditEvent> findSecurityEventsAfter(@Param("organizationId") UUID organizationId,
                                            @Param("since") Instant since);

    @Query("SELECT ae.ipAddress, COUNT(ae) as count FROM AuditEvent ae WHERE ae.organizationId = :organizationId AND ae.action = 'LOGIN_FAILED' AND ae.createdAt >= :since GROUP BY ae.ipAddress HAVING COUNT(ae) > :threshold")
    List<Object[]> findSuspiciousIpAddresses(@Param("organizationId") UUID organizationId,
                                            @Param("since") Instant since,
                                            @Param("threshold") long threshold);
}