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
import java.util.Set;
import java.util.UUID;

/**
 * Repository for ThreatIndicator entities with threat intelligence operations.
 *
 * This repository provides specialized methods for threat indicator management,
 * lookup operations, and threat intelligence analysis.
 *
 * Key features:
 * - Fast indicator value lookups for real-time threat detection
 * - Expiration and cleanup management for dynamic threat data
 * - Source-based threat feed management
 * - Confidence and severity-based filtering
 * - False positive tracking and adjustment
 * - Whitelist/blacklist management
 * - Tag-based categorization and search
 */
@Repository
public interface ThreatIndicatorRepository extends JpaRepository<ThreatIndicator, UUID> {

    // Core threat lookup operations

    /**
     * Find threat indicator by type and value for real-time lookup
     *
     * @param indicatorType Type of indicator
     * @param indicatorValue Indicator value to match
     * @return Optional threat indicator if found
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.indicatorType = :indicatorType " +
           "AND ti.indicatorValue = :indicatorValue AND ti.active = true " +
           "AND (ti.expiresAt IS NULL OR ti.expiresAt > CURRENT_TIMESTAMP)")
    Optional<ThreatIndicator> findActiveByTypeAndValue(@Param("indicatorType") ThreatIndicator.IndicatorType indicatorType,
                                                      @Param("indicatorValue") String indicatorValue);

    /**
     * Find all active threat indicators by value (any type)
     *
     * @param indicatorValue Indicator value to match
     * @return List of matching active threat indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.indicatorValue = :indicatorValue " +
           "AND ti.active = true AND (ti.expiresAt IS NULL OR ti.expiresAt > CURRENT_TIMESTAMP)")
    List<ThreatIndicator> findAllActiveByValue(@Param("indicatorValue") String indicatorValue);

    /**
     * Check if indicator should be blocked (active, not whitelisted, high severity)
     *
     * @param indicatorType Type of indicator
     * @param indicatorValue Indicator value to check
     * @return True if indicator should be blocked
     */
    @Query("SELECT CASE WHEN COUNT(ti) > 0 THEN true ELSE false END FROM ThreatIndicator ti WHERE " +
           "ti.indicatorType = :indicatorType AND ti.indicatorValue = :indicatorValue " +
           "AND ti.active = true AND ti.listStatus != 'WHITELIST' " +
           "AND (ti.listStatus = 'BLACKLIST' OR ti.severity IN ('CRITICAL', 'HIGH')) " +
           "AND (ti.expiresAt IS NULL OR ti.expiresAt > CURRENT_TIMESTAMP)")
    boolean shouldBlock(@Param("indicatorType") ThreatIndicator.IndicatorType indicatorType,
                       @Param("indicatorValue") String indicatorValue);

    // Severity and confidence filtering

    /**
     * Find high-severity threat indicators
     *
     * @param pageable Pagination parameters
     * @return Page of high-severity indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.severity IN ('CRITICAL', 'HIGH') " +
           "AND ti.active = true ORDER BY ti.severity DESC, ti.confidence DESC")
    Page<ThreatIndicator> findHighSeverityIndicators(Pageable pageable);

    /**
     * Find threat indicators by severity level
     *
     * @param severity Severity level to filter by
     * @param pageable Pagination parameters
     * @return Page of indicators with specified severity
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.severity = :severity " +
           "AND ti.active = true ORDER BY ti.confidence DESC")
    Page<ThreatIndicator> findBySeverity(@Param("severity") ThreatIndicator.Severity severity,
                                        Pageable pageable);

    /**
     * Find high-confidence threat indicators (confidence >= 80%)
     *
     * @param pageable Pagination parameters
     * @return Page of high-confidence indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.confidence >= 80.0 " +
           "AND ti.active = true ORDER BY ti.confidence DESC")
    Page<ThreatIndicator> findHighConfidenceIndicators(Pageable pageable);

    /**
     * Find threat indicators by confidence range
     *
     * @param minConfidence Minimum confidence score
     * @param maxConfidence Maximum confidence score
     * @param pageable Pagination parameters
     * @return Page of indicators within confidence range
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.confidence BETWEEN :minConfidence AND :maxConfidence " +
           "AND ti.active = true ORDER BY ti.confidence DESC")
    Page<ThreatIndicator> findByConfidenceRange(@Param("minConfidence") Double minConfidence,
                                               @Param("maxConfidence") Double maxConfidence,
                                               Pageable pageable);

    // Source and feed management

    /**
     * Find threat indicators by source
     *
     * @param source Source name
     * @param pageable Pagination parameters
     * @return Page of indicators from specific source
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.source = :source " +
           "ORDER BY ti.lastSeen DESC")
    Page<ThreatIndicator> findBySource(@Param("source") String source, Pageable pageable);

    /**
     * Get all unique threat sources
     *
     * @return Set of unique source names
     */
    @Query("SELECT DISTINCT ti.source FROM ThreatIndicator ti ORDER BY ti.source")
    Set<String> findAllSources();

    /**
     * Count indicators by source
     *
     * @param source Source name
     * @return Number of indicators from source
     */
    @Query("SELECT COUNT(ti) FROM ThreatIndicator ti WHERE ti.source = :source")
    long countBySource(@Param("source") String source);

    /**
     * Find most recently updated indicators by source
     *
     * @param source Source name
     * @param limit Maximum number of results
     * @return List of recent indicators from source
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.source = :source " +
           "ORDER BY ti.lastSeen DESC")
    List<ThreatIndicator> findRecentBySource(@Param("source") String source, Pageable pageable);

    // Expiration and cleanup management

    /**
     * Find expired threat indicators
     *
     * @return List of expired indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.expiresAt IS NOT NULL " +
           "AND ti.expiresAt <= CURRENT_TIMESTAMP")
    List<ThreatIndicator> findExpiredIndicators();

    /**
     * Find indicators expiring within specified time window
     *
     * @param expiryThreshold Expiration threshold timestamp
     * @return List of soon-to-expire indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.expiresAt IS NOT NULL " +
           "AND ti.expiresAt BETWEEN CURRENT_TIMESTAMP AND :expiryThreshold")
    List<ThreatIndicator> findExpiringBefore(@Param("expiryThreshold") Instant expiryThreshold);

    /**
     * Find stale indicators (not seen recently)
     *
     * @param staleThreshold Threshold for considering indicators stale
     * @return List of stale indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.lastSeen < :staleThreshold")
    List<ThreatIndicator> findStaleIndicators(@Param("staleThreshold") Instant staleThreshold);

    /**
     * Bulk deactivate expired indicators
     *
     * @return Number of deactivated indicators
     */
    @Modifying
    @Query("UPDATE ThreatIndicator ti SET ti.active = false WHERE ti.expiresAt IS NOT NULL " +
           "AND ti.expiresAt <= CURRENT_TIMESTAMP AND ti.active = true")
    int deactivateExpiredIndicators();

    /**
     * Bulk delete old inactive indicators
     *
     * @param cutoffTime Indicators older than this will be deleted
     * @return Number of deleted indicators
     */
    @Modifying
    @Query("DELETE FROM ThreatIndicator ti WHERE ti.active = false " +
           "AND ti.updatedAt < :cutoffTime")
    int deleteOldInactiveIndicators(@Param("cutoffTime") Instant cutoffTime);

    // List status management (whitelist/blacklist)

    /**
     * Find indicators by list status
     *
     * @param listStatus List status to filter by
     * @param pageable Pagination parameters
     * @return Page of indicators with specified list status
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.listStatus = :listStatus " +
           "ORDER BY ti.updatedAt DESC")
    Page<ThreatIndicator> findByListStatus(@Param("listStatus") ThreatIndicator.ListStatus listStatus,
                                          Pageable pageable);

    /**
     * Find all whitelisted indicators
     *
     * @return List of whitelisted indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.listStatus = 'WHITELIST'")
    List<ThreatIndicator> findWhitelistedIndicators();

    /**
     * Find all blacklisted indicators
     *
     * @return List of blacklisted indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.listStatus = 'BLACKLIST'")
    List<ThreatIndicator> findBlacklistedIndicators();

    /**
     * Bulk update list status for indicators
     *
     * @param indicatorIds List of indicator IDs to update
     * @param listStatus New list status
     * @return Number of updated indicators
     */
    @Modifying
    @Query("UPDATE ThreatIndicator ti SET ti.listStatus = :listStatus WHERE ti.id IN :indicatorIds")
    int bulkUpdateListStatus(@Param("indicatorIds") List<UUID> indicatorIds,
                            @Param("listStatus") ThreatIndicator.ListStatus listStatus);

    // Threat type and tag filtering

    /**
     * Find indicators by threat type
     *
     * @param threatType Type of threat
     * @param pageable Pagination parameters
     * @return Page of indicators of specified threat type
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.threatType = :threatType " +
           "AND ti.active = true ORDER BY ti.severity DESC")
    Page<ThreatIndicator> findByThreatType(@Param("threatType") ThreatIndicator.ThreatType threatType,
                                          Pageable pageable);

    /**
     * Find indicators by tags
     *
     * @param tags Set of tags to match
     * @param pageable Pagination parameters
     * @return Page of indicators with matching tags
     */
    @Query("SELECT DISTINCT ti FROM ThreatIndicator ti JOIN ti.tags t WHERE t IN :tags " +
           "ORDER BY ti.severity DESC")
    Page<ThreatIndicator> findByTags(@Param("tags") Set<String> tags, Pageable pageable);

    /**
     * Find all unique tags used in threat indicators
     *
     * @return Set of unique tags
     */
    @Query("SELECT DISTINCT t FROM ThreatIndicator ti JOIN ti.tags t ORDER BY t")
    Set<String> findAllTags();

    /**
     * Count indicators by threat type
     *
     * @param threatType Type of threat
     * @return Number of indicators of specified type
     */
    @Query("SELECT COUNT(ti) FROM ThreatIndicator ti WHERE ti.threatType = :threatType")
    long countByThreatType(@Param("threatType") ThreatIndicator.ThreatType threatType);

    // Detection and false positive tracking

    /**
     * Find indicators with high detection counts
     *
     * @param minDetectionCount Minimum number of detections
     * @param pageable Pagination parameters
     * @return Page of frequently detected indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.detectionCount >= :minDetectionCount " +
           "ORDER BY ti.detectionCount DESC")
    Page<ThreatIndicator> findByMinDetectionCount(@Param("minDetectionCount") Integer minDetectionCount,
                                                 Pageable pageable);

    /**
     * Find indicators with high false positive rates
     *
     * @param minFalsePositiveRate Minimum false positive rate percentage
     * @param pageable Pagination parameters
     * @return Page of indicators with high false positive rates
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE " +
           "(ti.falsePositiveCount * 100.0 / NULLIF(ti.detectionCount + ti.falsePositiveCount, 0)) >= :minFalsePositiveRate " +
           "ORDER BY (ti.falsePositiveCount * 100.0 / (ti.detectionCount + ti.falsePositiveCount)) DESC")
    Page<ThreatIndicator> findByMinFalsePositiveRate(@Param("minFalsePositiveRate") Double minFalsePositiveRate,
                                                    Pageable pageable);

    /**
     * Update detection count for an indicator
     *
     * @param indicatorId Indicator ID
     * @param lastSeen New last seen timestamp
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE ThreatIndicator ti SET ti.detectionCount = ti.detectionCount + 1, " +
           "ti.lastSeen = :lastSeen WHERE ti.id = :indicatorId")
    int incrementDetectionCount(@Param("indicatorId") UUID indicatorId,
                               @Param("lastSeen") Instant lastSeen);

    /**
     * Update false positive count for an indicator
     *
     * @param indicatorId Indicator ID
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE ThreatIndicator ti SET ti.falsePositiveCount = ti.falsePositiveCount + 1 " +
           "WHERE ti.id = :indicatorId")
    int incrementFalsePositiveCount(@Param("indicatorId") UUID indicatorId);

    // Statistical and analysis queries

    /**
     * Get threat type distribution statistics
     *
     * @return List of [threat_type, count] pairs
     */
    @Query("SELECT ti.threatType, COUNT(ti) FROM ThreatIndicator ti WHERE ti.active = true " +
           "GROUP BY ti.threatType ORDER BY COUNT(ti) DESC")
    List<Object[]> getThreatTypeDistribution();

    /**
     * Get severity distribution statistics
     *
     * @return List of [severity, count] pairs
     */
    @Query("SELECT ti.severity, COUNT(ti) FROM ThreatIndicator ti WHERE ti.active = true " +
           "GROUP BY ti.severity ORDER BY ti.severity")
    List<Object[]> getSeverityDistribution();

    /**
     * Get source reliability statistics
     *
     * @return List of [source, avg_confidence, indicator_count] triplets
     */
    @Query("SELECT ti.source, AVG(ti.confidence), COUNT(ti) FROM ThreatIndicator ti " +
           "WHERE ti.active = true GROUP BY ti.source ORDER BY AVG(ti.confidence) DESC")
    List<Object[]> getSourceReliabilityStats();

    /**
     * Find top detected threat indicators in time period
     *
     * @param startTime Start of time period
     * @param limit Maximum number of results
     * @return List of most detected indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE ti.lastSeen >= :startTime " +
           "ORDER BY ti.detectionCount DESC")
    List<ThreatIndicator> findTopDetectedSince(@Param("startTime") Instant startTime,
                                              Pageable pageable);

    // Search and advanced filtering

    /**
     * Search indicators by value pattern (case-insensitive)
     *
     * @param valuePattern Value pattern (supports % wildcards)
     * @param pageable Pagination parameters
     * @return Page of matching indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE " +
           "LOWER(ti.indicatorValue) LIKE LOWER(:valuePattern) " +
           "ORDER BY ti.severity DESC")
    Page<ThreatIndicator> searchByValuePattern(@Param("valuePattern") String valuePattern,
                                              Pageable pageable);

    /**
     * Find indicators with complex filtering
     *
     * @param indicatorType Optional indicator type filter
     * @param severity Optional severity filter
     * @param threatType Optional threat type filter
     * @param source Optional source filter
     * @param active Optional active status filter
     * @param pageable Pagination parameters
     * @return Page of filtered indicators
     */
    @Query("SELECT ti FROM ThreatIndicator ti WHERE " +
           "(:indicatorType IS NULL OR ti.indicatorType = :indicatorType) AND " +
           "(:severity IS NULL OR ti.severity = :severity) AND " +
           "(:threatType IS NULL OR ti.threatType = :threatType) AND " +
           "(:source IS NULL OR ti.source = :source) AND " +
           "(:active IS NULL OR ti.active = :active) " +
           "ORDER BY ti.severity DESC, ti.confidence DESC")
    Page<ThreatIndicator> findWithFilters(@Param("indicatorType") ThreatIndicator.IndicatorType indicatorType,
                                         @Param("severity") ThreatIndicator.Severity severity,
                                         @Param("threatType") ThreatIndicator.ThreatType threatType,
                                         @Param("source") String source,
                                         @Param("active") Boolean active,
                                         Pageable pageable);

    /**
     * Get threat score distribution for analysis
     *
     * @return List of threat score ranges and counts
     */
    @Query(value = "SELECT " +
                   "CASE " +
                   "  WHEN (severity_weight * 0.4 + confidence/100.0 * 0.4 + history_weight * 0.2) * 100 >= 80 THEN '80-100' " +
                   "  WHEN (severity_weight * 0.4 + confidence/100.0 * 0.4 + history_weight * 0.2) * 100 >= 60 THEN '60-79' " +
                   "  WHEN (severity_weight * 0.4 + confidence/100.0 * 0.4 + history_weight * 0.2) * 100 >= 40 THEN '40-59' " +
                   "  WHEN (severity_weight * 0.4 + confidence/100.0 * 0.4 + history_weight * 0.2) * 100 >= 20 THEN '20-39' " +
                   "  ELSE '0-19' " +
                   "END as score_range, " +
                   "COUNT(*) as count " +
                   "FROM ( " +
                   "  SELECT confidence, " +
                   "    CASE severity " +
                   "      WHEN 'CRITICAL' THEN 1.0 " +
                   "      WHEN 'HIGH' THEN 0.8 " +
                   "      WHEN 'MEDIUM' THEN 0.6 " +
                   "      WHEN 'LOW' THEN 0.4 " +
                   "      ELSE 0.2 " +
                   "    END as severity_weight, " +
                   "    CASE " +
                   "      WHEN detection_count = 0 THEN 0.5 " +
                   "      ELSE GREATEST(0.0, 1.0 - (false_positive_count * 100.0 / NULLIF(detection_count + false_positive_count, 0) / 100.0)) " +
                   "    END as history_weight " +
                   "  FROM threat_indicators WHERE active = true " +
                   ") t " +
                   "GROUP BY score_range ORDER BY score_range",
           nativeQuery = true)
    List<Object[]> getThreatScoreDistribution();
}