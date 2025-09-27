package com.platform.audit.internal;

import com.platform.shared.monitoring.PerformanceMonitoringService;
import com.platform.shared.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced audit service with performance optimizations, caching,
 * and advanced search capabilities.
 */
@Service
@Transactional
public class EnhancedAuditService extends AuditService {

    private final PerformanceMonitoringService performanceMonitoring;
    private final AuditEventRepository auditEventRepository;

    // Performance settings
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int ARCHIVE_THRESHOLD_DAYS = 90;
    private static final int COMPRESSION_THRESHOLD_DAYS = 30;

    @Autowired
    public EnhancedAuditService(
            AuditEventRepository auditEventRepository,
            PerformanceMonitoringService performanceMonitoring) {
        super(auditEventRepository);
        this.auditEventRepository = auditEventRepository;
        this.performanceMonitoring = performanceMonitoring;
    }

    /**
     * Enhanced user audit events with performance monitoring
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> getUserAuditEvents(
            UUID organizationId, UUID userId, int page, int size) {

        Instant startTime = Instant.now();
        try {
            validateOrganizationAccess(organizationId);
            Pageable pageable = PageRequest.of(page, size);

            Page<AuditEvent> result = auditEventRepository
                    .findByOrganizationIdAndActorIdOrderByCreatedAtDesc(
                            organizationId, userId, pageable);

            return result;
        } finally {
            Duration duration = Duration.between(startTime, Instant.now());
            performanceMonitoring.recordDatabaseQuery(
                    "audit_user_events", "getUserAuditEvents", duration);
        }
    }

    /**
     * Get user audit events by timeframe with caching
     */
    @Cacheable(value = "audit-user-timeframe", keyGenerator = "tenantAwareKeyGenerator")
    @Transactional(readOnly = true)
    public List<AuditEvent> getUserAuditEventsByTimeframe(
            String userId, Instant fromDate, Instant toDate) {

        Instant startTime = Instant.now();
        try {
            return auditEventRepository.findByUserIdAndTimeframe(userId, fromDate, toDate);
        } finally {
            Duration duration = Duration.between(startTime, Instant.now());
            performanceMonitoring.recordDatabaseQuery(
                    "audit_user_timeframe", "getUserAuditEventsByTimeframe", duration);
        }
    }

    /**
     * Get recent events with performance monitoring
     */
    @Cacheable(value = "audit-recent", key = "#since.toEpochMilli()")
    @Transactional(readOnly = true)
    public List<AuditEvent> getRecentEvents(Instant since) {
        Instant startTime = Instant.now();
        try {
            return auditEventRepository.findRecentEvents(since);
        } finally {
            Duration duration = Duration.between(startTime, Instant.now());
            performanceMonitoring.recordDatabaseQuery(
                    "audit_recent", "getRecentEvents", duration);
        }
    }

    /**
     * Advanced search with multiple criteria
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> searchAuditEvents(
            String userId,
            List<String> eventTypes,
            List<String> severities,
            Instant fromDate,
            Instant toDate,
            String ipAddress,
            String correlationId,
            Pageable pageable) {

        Instant startTime = Instant.now();
        try {
            return auditEventRepository.findByAdvancedCriteria(
                    userId, eventTypes, severities, fromDate, toDate,
                    ipAddress, correlationId, pageable);
        } finally {
            Duration duration = Duration.between(startTime, Instant.now());
            performanceMonitoring.recordDatabaseQuery(
                    "audit_advanced_search", "searchAuditEvents", duration);

            // Log slow searches for optimization
            if (duration.toMillis() > 500) {
                logger.warn("Slow audit search detected: {}ms - Criteria: userId={}, types={}, from={}, to={}",
                        duration.toMillis(), userId, eventTypes, fromDate, toDate);
            }
        }
    }

    /**
     * Asynchronous event archival for performance
     */
    @Async
    @Scheduled(cron = "0 2 * * 0") // Weekly on Sunday at 2 AM
    public CompletableFuture<Long> archiveOldEvents() {
        logger.info("Starting audit events archival process");

        Instant threshold = Instant.now().minus(ARCHIVE_THRESHOLD_DAYS, ChronoUnit.DAYS);
        Instant startTime = Instant.now();

        try {
            long archivedCount = auditEventRepository.archiveEventsOlderThan(threshold);
            logger.info("Archived {} audit events older than {}", archivedCount, threshold);

            // Record performance metrics
            Duration duration = Duration.between(startTime, Instant.now());
            performanceMonitoring.recordDatabaseQuery(
                    "audit_archival", "archiveOldEvents", duration);

            return CompletableFuture.completedFuture(archivedCount);
        } catch (Exception e) {
            logger.error("Failed to archive audit events", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Asynchronous event compression for storage optimization
     */
    @Async
    @Scheduled(cron = "0 3 * * 0") // Weekly on Sunday at 3 AM
    public CompletableFuture<Long> compressOldEvents() {
        logger.info("Starting audit events compression process");

        Instant threshold = Instant.now().minus(COMPRESSION_THRESHOLD_DAYS, ChronoUnit.DAYS);
        Instant startTime = Instant.now();

        try {
            long compressedCount = auditEventRepository.compressEventsOlderThan(threshold);
            logger.info("Compressed {} audit events older than {}", compressedCount, threshold);

            // Record performance metrics
            Duration duration = Duration.between(startTime, Instant.now());
            performanceMonitoring.recordDatabaseQuery(
                    "audit_compression", "compressOldEvents", duration);

            return CompletableFuture.completedFuture(compressedCount);
        } catch (Exception e) {
            logger.error("Failed to compress audit events", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Batch audit event creation for high-throughput scenarios
     */
    @Transactional
    public void createAuditEventsBatch(List<AuditEvent> events) {
        if (events.isEmpty()) return;

        Instant startTime = Instant.now();
        try {
            // Process in batches for optimal performance
            for (int i = 0; i < events.size(); i += DEFAULT_BATCH_SIZE) {
                int endIndex = Math.min(i + DEFAULT_BATCH_SIZE, events.size());
                List<AuditEvent> batch = events.subList(i, endIndex);

                auditEventRepository.saveAll(batch);

                // Flush and clear to prevent memory issues
                if (i % DEFAULT_BATCH_SIZE == 0) {
                    auditEventRepository.flush();
                }
            }

            logger.info("Successfully created {} audit events in batch", events.size());
        } finally {
            Duration duration = Duration.between(startTime, Instant.now());
            performanceMonitoring.recordDatabaseQuery(
                    "audit_batch_create", "createAuditEventsBatch", duration);
        }
    }

    /**
     * Performance-optimized security analysis
     */
    @Cacheable(value = "security-analysis", keyGenerator = "tenantAwareKeyGenerator")
    @Transactional(readOnly = true)
    public SecurityAnalysisResult performSecurityAnalysis(
            UUID organizationId, Instant since, int lookbackHours) {

        Instant startTime = Instant.now();
        try {
            validateOrganizationAccess(organizationId);

            // Get security incidents
            List<AuditEvent> securityEvents = getSecurityEvents(organizationId, since);

            // Analyze suspicious IPs
            List<Object[]> suspiciousIps = auditEventRepository
                    .findSuspiciousIpAddresses(organizationId, since, 5);

            // Get user behavior anomalies
            List<Object[]> userAnomalies = auditEventRepository
                    .detectUserBehaviorAnomalies(organizationId, since, 100, 5);

            return new SecurityAnalysisResult(
                    securityEvents.size(),
                    suspiciousIps.size(),
                    userAnomalies.size(),
                    calculateRiskScore(securityEvents, suspiciousIps, userAnomalies)
            );

        } finally {
            Duration duration = Duration.between(startTime, Instant.now());
            performanceMonitoring.recordDatabaseQuery(
                    "security_analysis", "performSecurityAnalysis", duration);
        }
    }

    /**
     * Performance monitoring for audit operations
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorAuditPerformance() {
        try {
            // Check audit event count growth
            long recentEventCount = auditEventRepository
                    .findRecentEvents(Instant.now().minus(5, ChronoUnit.MINUTES))
                    .size();

            if (recentEventCount > 1000) { // High volume threshold
                logger.warn("High audit event volume detected: {} events in last 5 minutes",
                        recentEventCount);
            }

            // Check for slow queries in audit operations
            // This would integrate with actual query performance monitoring
            performanceMonitoring.recordCacheHit("audit-monitoring");

        } catch (Exception e) {
            logger.error("Failed to monitor audit performance", e);
        }
    }

    /**
     * Calculate risk score based on security analysis
     */
    private int calculateRiskScore(List<AuditEvent> securityEvents,
                                   List<Object[]> suspiciousIps,
                                   List<Object[]> userAnomalies) {
        int score = 0;

        // Weight different security factors
        score += securityEvents.size() * 2;  // 2 points per security event
        score += suspiciousIps.size() * 5;   // 5 points per suspicious IP
        score += userAnomalies.size() * 3;   // 3 points per user anomaly

        return Math.min(100, score); // Cap at 100
    }

    /**
     * Security analysis result container
     */
    public static class SecurityAnalysisResult {
        private final int securityEventCount;
        private final int suspiciousIpCount;
        private final int userAnomalyCount;
        private final int riskScore;

        public SecurityAnalysisResult(int securityEventCount, int suspiciousIpCount,
                                      int userAnomalyCount, int riskScore) {
            this.securityEventCount = securityEventCount;
            this.suspiciousIpCount = suspiciousIpCount;
            this.userAnomalyCount = userAnomalyCount;
            this.riskScore = riskScore;
        }

        // Getters
        public int getSecurityEventCount() { return securityEventCount; }
        public int getSuspiciousIpCount() { return suspiciousIpCount; }
        public int getUserAnomalyCount() { return userAnomalyCount; }
        public int getRiskScore() { return riskScore; }
    }
}