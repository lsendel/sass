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
 * Repository for AlertRule entities with real-time alerting support.
 *
 * This repository provides specialized methods for alert rule management,
 * trigger tracking, and automated alerting operations.
 *
 * Key features:
 * - Active rule filtering for real-time monitoring
 * - Cooldown period management to prevent alert spam
 * - Severity-based prioritization and filtering
 * - Trigger statistics and performance tracking
 * - Owner-based access control and multi-tenancy
 * - Bulk operations for alert management
 * - Threshold monitoring and adjustment
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    // Active rule queries for real-time monitoring

    /**
     * Find all active alert rules that can trigger (enabled and not in cooldown)
     *
     * @return List of active alert rules
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.enabled = true " +
           "AND (ar.lastTriggered IS NULL OR ar.lastTriggered + ar.cooldownPeriod <= CURRENT_TIMESTAMP) " +
           "ORDER BY ar.severity DESC")
    List<AlertRule> findActiveTriggerableRules();

    /**
     * Find active rules by severity for prioritized monitoring
     *
     * @param severity Alert severity level
     * @return List of active rules with specified severity
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.enabled = true AND ar.severity = :severity " +
           "AND (ar.lastTriggered IS NULL OR ar.lastTriggered + ar.cooldownPeriod <= CURRENT_TIMESTAMP) " +
           "ORDER BY ar.threshold ASC")
    List<AlertRule> findActiveBySeverity(@Param("severity") AlertRule.Severity severity);

    /**
     * Find critical alert rules that can trigger immediately
     *
     * @return List of critical alert rules
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.enabled = true AND ar.severity = 'CRITICAL' " +
           "AND (ar.lastTriggered IS NULL OR ar.lastTriggered + ar.cooldownPeriod <= CURRENT_TIMESTAMP)")
    List<AlertRule> findActiveCriticalRules();

    /**
     * Check if any critical rules are ready to trigger
     *
     * @return True if critical rules are available for triggering
     */
    @Query("SELECT CASE WHEN COUNT(ar) > 0 THEN true ELSE false END FROM AlertRule ar " +
           "WHERE ar.enabled = true AND ar.severity = 'CRITICAL' " +
           "AND (ar.lastTriggered IS NULL OR ar.lastTriggered + ar.cooldownPeriod <= CURRENT_TIMESTAMP)")
    boolean hasCriticalRulesReady();

    // Owner-based queries (multi-tenancy)

    /**
     * Find all alert rules owned by a specific user
     *
     * @param createdBy Owner user ID
     * @param pageable Pagination parameters
     * @return Page of user-owned alert rules
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.createdBy = :createdBy ORDER BY ar.name ASC")
    Page<AlertRule> findByCreatedBy(@Param("createdBy") String createdBy, Pageable pageable);

    /**
     * Find enabled alert rules by owner
     *
     * @param createdBy Owner user ID
     * @return List of enabled rules owned by user
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.createdBy = :createdBy AND ar.enabled = true " +
           "ORDER BY ar.severity DESC")
    List<AlertRule> findEnabledByCreatedBy(@Param("createdBy") String createdBy);

    /**
     * Find alert rule by name and owner (ensures tenant isolation)
     *
     * @param name Alert rule name
     * @param createdBy Owner user ID
     * @return Optional alert rule if found and owned by user
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.name = :name AND ar.createdBy = :createdBy")
    Optional<AlertRule> findByNameAndCreatedBy(@Param("name") String name, @Param("createdBy") String createdBy);

    /**
     * Count alert rules by owner
     *
     * @param createdBy Owner user ID
     * @return Number of rules owned by user
     */
    @Query("SELECT COUNT(ar) FROM AlertRule ar WHERE ar.createdBy = :createdBy")
    long countByCreatedBy(@Param("createdBy") String createdBy);

    // Severity and threshold filtering

    /**
     * Find alert rules by severity level
     *
     * @param severity Severity level to filter by
     * @param pageable Pagination parameters
     * @return Page of rules with specified severity
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.severity = :severity ORDER BY ar.threshold ASC")
    Page<AlertRule> findBySeverity(@Param("severity") AlertRule.Severity severity, Pageable pageable);

    /**
     * Find rules with thresholds within specified range
     *
     * @param minThreshold Minimum threshold value
     * @param maxThreshold Maximum threshold value
     * @param pageable Pagination parameters
     * @return Page of rules within threshold range
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.threshold BETWEEN :minThreshold AND :maxThreshold " +
           "ORDER BY ar.threshold ASC")
    Page<AlertRule> findByThresholdRange(@Param("minThreshold") Double minThreshold,
                                        @Param("maxThreshold") Double maxThreshold,
                                        Pageable pageable);

    /**
     * Find high-threshold rules (above specified value)
     *
     * @param thresholdValue Threshold value to compare against
     * @return List of rules with high thresholds
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.threshold > :thresholdValue AND ar.enabled = true " +
           "ORDER BY ar.threshold DESC")
    List<AlertRule> findHighThresholdRules(@Param("thresholdValue") Double thresholdValue);

    // Cooldown and trigger management

    /**
     * Find rules currently in cooldown period
     *
     * @return List of rules in cooldown
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.lastTriggered IS NOT NULL " +
           "AND ar.lastTriggered + ar.cooldownPeriod > CURRENT_TIMESTAMP")
    List<AlertRule> findRulesInCooldown();

    /**
     * Find rules that recently triggered (within specified time)
     *
     * @param since Timestamp threshold for recent triggers
     * @param pageable Pagination parameters
     * @return Page of recently triggered rules
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.lastTriggered >= :since " +
           "ORDER BY ar.lastTriggered DESC")
    Page<AlertRule> findRecentlyTriggered(@Param("since") Instant since, Pageable pageable);

    /**
     * Find rules with high trigger counts
     *
     * @param minTriggerCount Minimum trigger count threshold
     * @param pageable Pagination parameters
     * @return Page of frequently triggered rules
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.triggerCount >= :minTriggerCount " +
           "ORDER BY ar.triggerCount DESC")
    Page<AlertRule> findByMinTriggerCount(@Param("minTriggerCount") Integer minTriggerCount,
                                         Pageable pageable);

    /**
     * Update trigger statistics when rule is triggered
     *
     * @param ruleId Alert rule ID
     * @param triggeredAt Trigger timestamp
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE AlertRule ar SET ar.lastTriggered = :triggeredAt, " +
           "ar.triggerCount = ar.triggerCount + 1 WHERE ar.id = :ruleId")
    int recordTrigger(@Param("ruleId") UUID ruleId, @Param("triggeredAt") Instant triggeredAt);

    /**
     * Reset trigger statistics for a rule
     *
     * @param ruleId Alert rule ID
     * @return Number of updated records
     */
    @Modifying
    @Query("UPDATE AlertRule ar SET ar.triggerCount = 0, ar.lastTriggered = null WHERE ar.id = :ruleId")
    int resetTriggerStats(@Param("ruleId") UUID ruleId);

    // Notification and escalation management

    /**
     * Find rules with specific notification channels
     *
     * @param channel Notification channel to search for
     * @return List of rules using the specified channel
     */
    @Query("SELECT ar FROM AlertRule ar JOIN ar.notificationChannels nc WHERE nc = :channel")
    List<AlertRule> findByNotificationChannel(@Param("channel") String channel);

    /**
     * Find rules with escalation rules configured
     *
     * @return List of rules with escalation configurations
     */
    @Query("SELECT ar FROM AlertRule ar WHERE SIZE(ar.escalationRules) > 0")
    List<AlertRule> findRulesWithEscalation();

    /**
     * Find rules without notification channels (potentially misconfigured)
     *
     * @param createdBy Optional owner filter
     * @return List of rules without notification setup
     */
    @Query("SELECT ar FROM AlertRule ar WHERE SIZE(ar.notificationChannels) = 0 " +
           "AND (:createdBy IS NULL OR ar.createdBy = :createdBy)")
    List<AlertRule> findRulesWithoutNotifications(@Param("createdBy") String createdBy);

    // Condition and time window analysis

    /**
     * Find rules with similar conditions (for deduplication analysis)
     *
     * @param condition Condition expression to match
     * @param excludeId Optional rule ID to exclude from results
     * @return List of rules with similar conditions
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.condition = :condition " +
           "AND (:excludeId IS NULL OR ar.id != :excludeId)")
    List<AlertRule> findBySimilarCondition(@Param("condition") String condition,
                                          @Param("excludeId") UUID excludeId);

    /**
     * Find rules with short time windows (potential noise generators)
     *
     * @param maxTimeWindowMinutes Maximum time window in minutes
     * @return List of rules with short time windows
     */
    @Query(value = "SELECT * FROM alert_rules WHERE " +
                   "EXTRACT(EPOCH FROM time_window) / 60 <= :maxTimeWindowMinutes",
           nativeQuery = true)
    List<AlertRule> findShortTimeWindowRules(@Param("maxTimeWindowMinutes") long maxTimeWindowMinutes);

    /**
     * Find rules with very short cooldown periods (potential spam generators)
     *
     * @param maxCooldownSeconds Maximum cooldown in seconds
     * @return List of rules with short cooldowns
     */
    @Query(value = "SELECT * FROM alert_rules WHERE " +
                   "EXTRACT(EPOCH FROM cooldown_period) <= :maxCooldownSeconds",
           nativeQuery = true)
    List<AlertRule> findShortCooldownRules(@Param("maxCooldownSeconds") long maxCooldownSeconds);

    // Statistical and performance analysis

    /**
     * Get alert rule statistics by severity
     *
     * @return List of [severity, count] pairs
     */
    @Query("SELECT ar.severity, COUNT(ar) FROM AlertRule ar GROUP BY ar.severity ORDER BY ar.severity")
    List<Object[]> getAlertRuleStatsBySeverity();

    /**
     * Get trigger frequency statistics
     *
     * @param startTime Start of analysis period
     * @param endTime End of analysis period
     * @return List of [rule_name, trigger_count, avg_frequency] triplets
     */
    @Query(value = "SELECT name, trigger_count, " +
                   "CASE WHEN created_at < :startTime THEN " +
                   "  trigger_count / GREATEST(1, EXTRACT(DAYS FROM (:endTime - :startTime))) " +
                   "ELSE " +
                   "  trigger_count / GREATEST(1, EXTRACT(DAYS FROM (:endTime - created_at))) " +
                   "END as avg_daily_frequency " +
                   "FROM alert_rules " +
                   "WHERE trigger_count > 0 " +
                   "ORDER BY avg_daily_frequency DESC",
           nativeQuery = true)
    List<Object[]> getTriggerFrequencyStats(@Param("startTime") Instant startTime,
                                           @Param("endTime") Instant endTime);

    /**
     * Find most and least triggered rules
     *
     * @param topN Number of top/bottom rules to return
     * @return List of rules ordered by trigger count
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.triggerCount > 0 ORDER BY ar.triggerCount DESC")
    List<AlertRule> findMostTriggeredRules(Pageable pageable);

    /**
     * Get cooldown effectiveness analysis
     *
     * @return List of rules with cooldown impact metrics
     */
    @Query(value = "SELECT name, trigger_count, " +
                   "EXTRACT(EPOCH FROM cooldown_period) / 60 as cooldown_minutes, " +
                   "CASE WHEN last_triggered IS NOT NULL THEN " +
                   "  EXTRACT(EPOCH FROM (NOW() - last_triggered)) / 3600 " +
                   "ELSE NULL END as hours_since_last_trigger " +
                   "FROM alert_rules WHERE enabled = true " +
                   "ORDER BY trigger_count DESC",
           nativeQuery = true)
    List<Object[]> getCooldownEffectivenessStats();

    // Bulk operations

    /**
     * Bulk enable/disable alert rules by owner
     *
     * @param createdBy Owner user ID
     * @param enabled New enabled status
     * @param ruleIds List of rule IDs to update
     * @return Number of updated rules
     */
    @Modifying
    @Query("UPDATE AlertRule ar SET ar.enabled = :enabled WHERE ar.createdBy = :createdBy " +
           "AND ar.id IN :ruleIds")
    int bulkUpdateEnabledStatus(@Param("createdBy") String createdBy,
                               @Param("enabled") Boolean enabled,
                               @Param("ruleIds") List<UUID> ruleIds);

    /**
     * Bulk update severity for rules
     *
     * @param createdBy Owner user ID for security
     * @param ruleIds List of rule IDs to update
     * @param severity New severity level
     * @return Number of updated rules
     */
    @Modifying
    @Query("UPDATE AlertRule ar SET ar.severity = :severity WHERE ar.createdBy = :createdBy " +
           "AND ar.id IN :ruleIds")
    int bulkUpdateSeverity(@Param("createdBy") String createdBy,
                          @Param("ruleIds") List<UUID> ruleIds,
                          @Param("severity") AlertRule.Severity severity);

    /**
     * Bulk delete alert rules by owner (with security check)
     *
     * @param createdBy Owner user ID
     * @param ruleIds List of rule IDs to delete
     * @return Number of deleted rules
     */
    @Modifying
    @Query("DELETE FROM AlertRule ar WHERE ar.createdBy = :createdBy AND ar.id IN :ruleIds")
    int bulkDeleteByCreatedBy(@Param("createdBy") String createdBy,
                             @Param("ruleIds") List<UUID> ruleIds);

    // Advanced search and filtering

    /**
     * Search alert rules by name pattern (case-insensitive)
     *
     * @param createdBy Owner user ID
     * @param namePattern Name pattern (supports % wildcards)
     * @param pageable Pagination parameters
     * @return Page of matching alert rules
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.createdBy = :createdBy AND " +
           "LOWER(ar.name) LIKE LOWER(:namePattern) ORDER BY ar.name ASC")
    Page<AlertRule> searchByNamePattern(@Param("createdBy") String createdBy,
                                       @Param("namePattern") String namePattern,
                                       Pageable pageable);

    /**
     * Find alert rules with complex filtering
     *
     * @param createdBy Owner user ID
     * @param enabled Optional enabled status filter
     * @param severity Optional severity filter
     * @param minThreshold Optional minimum threshold filter
     * @param maxThreshold Optional maximum threshold filter
     * @param pageable Pagination parameters
     * @return Page of filtered alert rules
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.createdBy = :createdBy AND " +
           "(:enabled IS NULL OR ar.enabled = :enabled) AND " +
           "(:severity IS NULL OR ar.severity = :severity) AND " +
           "(:minThreshold IS NULL OR ar.threshold >= :minThreshold) AND " +
           "(:maxThreshold IS NULL OR ar.threshold <= :maxThreshold) " +
           "ORDER BY ar.name ASC")
    Page<AlertRule> findWithFilters(@Param("createdBy") String createdBy,
                                   @Param("enabled") Boolean enabled,
                                   @Param("severity") AlertRule.Severity severity,
                                   @Param("minThreshold") Double minThreshold,
                                   @Param("maxThreshold") Double maxThreshold,
                                   Pageable pageable);

    /**
     * Find rules created after specific timestamp
     *
     * @param createdBy Owner user ID
     * @param createdAfter Creation timestamp threshold
     * @param pageable Pagination parameters
     * @return Page of recently created rules
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.createdBy = :createdBy " +
           "AND ar.createdAt > :createdAfter ORDER BY ar.createdAt DESC")
    Page<AlertRule> findCreatedAfter(@Param("createdBy") String createdBy,
                                    @Param("createdAfter") Instant createdAfter,
                                    Pageable pageable);

    /**
     * Check if rule name is unique for owner
     *
     * @param name Rule name
     * @param createdBy Owner user ID
     * @param excludeId Optional rule ID to exclude from check
     * @return True if name is unique
     */
    @Query("SELECT CASE WHEN COUNT(ar) = 0 THEN true ELSE false END FROM AlertRule ar WHERE " +
           "ar.name = :name AND ar.createdBy = :createdBy AND (:excludeId IS NULL OR ar.id != :excludeId)")
    boolean isNameUniqueForUser(@Param("name") String name,
                               @Param("createdBy") String createdBy,
                               @Param("excludeId") UUID excludeId);

    /**
     * Find rules that need maintenance (high trigger count, recent problems)
     *
     * @param minTriggerCount Threshold for considering rule as high-volume
     * @param recentDays Number of days to look back for recent activity
     * @return List of rules needing attention
     */
    @Query(value = "SELECT * FROM alert_rules WHERE " +
                   "trigger_count >= :minTriggerCount AND " +
                   "last_triggered >= NOW() - INTERVAL ':recentDays days' " +
                   "ORDER BY trigger_count DESC",
           nativeQuery = true)
    List<AlertRule> findRulesNeedingMaintenance(@Param("minTriggerCount") Integer minTriggerCount,
                                               @Param("recentDays") int recentDays);
}