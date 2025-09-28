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
 * An enhanced implementation of {@link AuditService} with performance optimizations, caching, and
 * advanced search capabilities.
 *
 * <p>This service extends the base audit functionality with features such as performance
 * monitoring, caching for frequently accessed audit data, and scheduled tasks for data archival and
 * compression.
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

  /**
   * Constructs a new EnhancedAuditService.
   *
   * @param auditEventRepository the repository for managing audit events
   * @param performanceMonitoring the service for monitoring performance
   */
  @Autowired
  public EnhancedAuditService(
      AuditEventRepository auditEventRepository,
      PerformanceMonitoringService performanceMonitoring) {
    super(auditEventRepository);
    this.auditEventRepository = auditEventRepository;
    this.performanceMonitoring = performanceMonitoring;
  }

  /**
   * Retrieves a paginated list of audit events for a specific user, with performance monitoring.
   *
   * @param organizationId the ID of the organization
   * @param userId the ID of the user
   * @param page the page number to retrieve
   * @param size the number of events per page
   * @return a {@link Page} of audit events
   */
  @Override
  @Transactional(readOnly = true)
  public Page<AuditEvent> getUserAuditEvents(UUID organizationId, UUID userId, int page, int size) {

    Instant startTime = Instant.now();
    try {
      validateOrganizationAccess(organizationId);
      Pageable pageable = PageRequest.of(page, size);

      Page<AuditEvent> result =
          auditEventRepository.findByOrganizationIdAndActorIdOrderByCreatedAtDesc(
              organizationId, userId, pageable);

      return result;
    } finally {
      Duration duration = Duration.between(startTime, Instant.now());
      performanceMonitoring.recordDatabaseQuery(
          "audit_user_events", "getUserAuditEvents", duration);
    }
  }

  /**
   * Retrieves audit events for a user within a specific timeframe, with caching.
   *
   * @param userId the ID of the user
   * @param fromDate the start of the timeframe
   * @param toDate the end of the timeframe
   * @return a list of audit events
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
   * Retrieves recent audit events with performance monitoring and caching.
   *
   * @param since the timestamp from which to retrieve recent events
   * @return a list of recent audit events
   */
  @Cacheable(value = "audit-recent", key = "#since.toEpochMilli()")
  @Transactional(readOnly = true)
  public List<AuditEvent> getRecentEvents(Instant since) {
    Instant startTime = Instant.now();
    try {
      return auditEventRepository.findRecentEvents(since);
    } finally {
      Duration duration = Duration.between(startTime, Instant.now());
      performanceMonitoring.recordDatabaseQuery("audit_recent", "getRecentEvents", duration);
    }
  }

  /**
   * Performs an advanced search for audit events with multiple criteria and performance monitoring.
   *
   * @param userId the ID of the user to filter by
   * @param eventTypes a list of event types to filter by
   * @param severities a list of severities to filter by
   * @param fromDate the start of the date range to filter by
   * @param toDate the end of the date range to filter by
   * @param ipAddress the IP address to filter by
   * @param correlationId the correlation ID to filter by
   * @param pageable pagination information
   * @return a {@link Page} of matching audit events
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
          userId, eventTypes, severities, fromDate, toDate, ipAddress, correlationId, pageable);
    } finally {
      Duration duration = Duration.between(startTime, Instant.now());
      performanceMonitoring.recordDatabaseQuery(
          "audit_advanced_search", "searchAuditEvents", duration);

      // Log slow searches for optimization
      if (duration.toMillis() > 500) {
        logger.warn(
            "Slow audit search detected: {}ms - Criteria: userId={}, types={}, from={}, to={}",
            duration.toMillis(),
            userId,
            eventTypes,
            fromDate,
            toDate);
      }
    }
  }

  /**
   * Asynchronously archives old audit events to improve performance.
   *
   * <p>This scheduled task runs weekly on Sunday at 2 AM.
   *
   * @return a {@link CompletableFuture} containing the number of archived events
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
      performanceMonitoring.recordDatabaseQuery("audit_archival", "archiveOldEvents", duration);

      return CompletableFuture.completedFuture(archivedCount);
    } catch (Exception e) {
      logger.error("Failed to archive audit events", e);
      return CompletableFuture.failedFuture(e);
    }
  }

  /**
   * Asynchronously compresses old audit events for storage optimization.
   *
   * <p>This scheduled task runs weekly on Sunday at 3 AM.
   *
   * @return a {@link CompletableFuture} containing the number of compressed events
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
      performanceMonitoring.recordDatabaseQuery("audit_compression", "compressOldEvents", duration);

      return CompletableFuture.completedFuture(compressedCount);
    } catch (Exception e) {
      logger.error("Failed to compress audit events", e);
      return CompletableFuture.failedFuture(e);
    }
  }

  /**
   * Creates a batch of audit events for high-throughput scenarios.
   *
   * @param events a list of audit events to create
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
   * Performs a performance-optimized security analysis with caching.
   *
   * @param organizationId the ID of the organization to analyze
   * @param since the timestamp from which to start the analysis
   * @param lookbackHours the number of hours to look back for behavior anomalies
   * @return a {@link SecurityAnalysisResult} containing the analysis results
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
      List<Object[]> suspiciousIps =
          auditEventRepository.findSuspiciousIpAddresses(organizationId, since, 5);

      // Get user behavior anomalies
      List<Object[]> userAnomalies =
          auditEventRepository.detectUserBehaviorAnomalies(organizationId, since, 100, 5);

      return new SecurityAnalysisResult(
          securityEvents.size(),
          suspiciousIps.size(),
          userAnomalies.size(),
          calculateRiskScore(securityEvents, suspiciousIps, userAnomalies));

    } finally {
      Duration duration = Duration.between(startTime, Instant.now());
      performanceMonitoring.recordDatabaseQuery(
          "security_analysis", "performSecurityAnalysis", duration);
    }
  }

  /** Monitors the performance of audit operations. */
  @Scheduled(fixedRate = 60000) // Every minute
  public void monitorAuditPerformance() {
    try {
      // Check audit event count growth
      long recentEventCount =
          auditEventRepository
              .findRecentEvents(Instant.now().minus(5, ChronoUnit.MINUTES))
              .size();

      if (recentEventCount > 1000) { // High volume threshold
        logger.warn(
            "High audit event volume detected: {} events in last 5 minutes", recentEventCount);
      }

      // Check for slow queries in audit operations
      // This would integrate with actual query performance monitoring
      performanceMonitoring.recordCacheHit("audit-monitoring");

    } catch (Exception e) {
      logger.error("Failed to monitor audit performance", e);
    }
  }

  /**
   * Calculates a risk score based on security analysis results.
   *
   * @param securityEvents a list of security events
   * @param suspiciousIps a list of suspicious IP addresses
   * @param userAnomalies a list of user behavior anomalies
   * @return an integer risk score, capped at 100
   */
  private int calculateRiskScore(
      List<AuditEvent> securityEvents, List<Object[]> suspiciousIps, List<Object[]> userAnomalies) {
    int score = 0;

    // Weight different security factors
    score += securityEvents.size() * 2; // 2 points per security event
    score += suspiciousIps.size() * 5; // 5 points per suspicious IP
    score += userAnomalies.size() * 3; // 3 points per user anomaly

    return Math.min(100, score); // Cap at 100
  }

  /** A container for security analysis results. */
  public static class SecurityAnalysisResult {
    private final int securityEventCount;
    private final int suspiciousIpCount;
    private final int userAnomalyCount;
    private final int riskScore;

    public SecurityAnalysisResult(
        int securityEventCount, int suspiciousIpCount, int userAnomalyCount, int riskScore) {
      this.securityEventCount = securityEventCount;
      this.suspiciousIpCount = suspiciousIpCount;
      this.userAnomalyCount = userAnomalyCount;
      this.riskScore = riskScore;
    }

    // Getters
    public int getSecurityEventCount() {
      return securityEventCount;
    }

    public int getSuspiciousIpCount() {
      return suspiciousIpCount;
    }

    public int getUserAnomalyCount() {
      return userAnomalyCount;
    }

    public int getRiskScore() {
      return riskScore;
    }
  }
}