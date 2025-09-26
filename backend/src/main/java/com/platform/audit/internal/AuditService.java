package com.platform.audit.internal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.shared.security.TenantContext;

@Transactional
public class AuditService implements com.platform.shared.audit.AuditService {

  private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
  private static final String REDACTED_PLACEHOLDER = "[REDACTED]";

  // PII patterns for automatic redaction
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
  private static final Pattern PHONE_PATTERN =
      Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b");
  private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
  private static final Pattern CREDIT_CARD_PATTERN =
      Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");

  private final AuditEventRepository auditEventRepository;
  private final ObjectMapper objectMapper;
  private final boolean enablePiiRedaction;
  private final int retentionDays;

  public AuditService(
      final AuditEventRepository auditEventRepository,
      final ObjectMapper objectMapper,
      @Value("${app.audit.enable-pii-redaction:true}") final boolean enablePiiRedaction,
      @Value("${app.audit.retention-days:2555}") final int retentionDays) { // 7 years default
    this.auditEventRepository = auditEventRepository;
    this.objectMapper = objectMapper;
    this.enablePiiRedaction = enablePiiRedaction;
    this.retentionDays = retentionDays;
  }

  @Async
  /**
   * Core logging method used by convenience helpers.
   * Not intended for overriding; wraps serialization and PII redaction.
   */
  public CompletableFuture<Void> logEvent(
      final String eventType,
      final String resourceType,
      final String resourceId,
      final String action,
      final Map<String, Object> requestData,
      final Map<String, Object> responseData,
      final String ipAddress,
      final String userAgent,
      final Map<String, String> metadata) {
    try {
      UUID userId = TenantContext.getCurrentUserId();
      UUID organizationId = TenantContext.getCurrentOrganizationId();

      // Create audit event
      AuditEvent auditEvent =
          new AuditEvent(
              organizationId,
              userId,
              eventType,
              resourceType,
              resourceId,
              action,
              ipAddress,
              userAgent);

      // Serialize and redact data if enabled
      if (requestData != null) {
        String requestJson = objectMapper.writeValueAsString(requestData);
        auditEvent.setRequestData(enablePiiRedaction ? redactPii(requestJson) : requestJson);
      }

      if (responseData != null) {
        String responseJson = objectMapper.writeValueAsString(responseData);
        auditEvent.setResponseData(enablePiiRedaction ? redactPii(responseJson) : responseJson);
      }

      if (metadata != null && !metadata.isEmpty()) {
        String metadataJson = objectMapper.writeValueAsString(metadata);
        auditEvent.setMetadata(enablePiiRedaction ? redactPii(metadataJson) : metadataJson);
      }

      // Save audit event
      auditEventRepository.save(auditEvent);

      logger.debug(
          "Audit event logged: {} for resource: {}:{}", eventType, resourceType, resourceId);

    } catch (Exception e) {
      logger.error(
          "Failed to log audit event: {} for resource: {}:{}",
          eventType,
          resourceType,
          resourceId,
          e);
    }

    return CompletableFuture.completedFuture(null);
  }

  // Convenience methods for common audit events
  @Async
  /** Logs a user login event (success/failure). Not intended for extension. */
  public CompletableFuture<Void> logUserLogin(
      final UUID userId, final String ipAddress, final String userAgent, final boolean success) {
    String eventType = success ? "USER_LOGIN" : "USER_LOGIN_FAILED";
    Map<String, Object> metadata =
        Map.of("success", success, "timestamp", Instant.now().toString());

    return logEvent(
        eventType,
        "USER",
        userId.toString(),
        "LOGIN",
        null,
        null,
        ipAddress,
        userAgent,
        Map.of("success", String.valueOf(success)));
  }

  @Async
  /** Logs a user logout event. Not intended for extension. */
  public CompletableFuture<Void> logUserLogout(
      final UUID userId, final String ipAddress, final String userAgent) {
    return logEvent(
        "USER_LOGOUT", "USER", userId.toString(), "LOGOUT", null, null, ipAddress, userAgent, null);
  }

  @Async
  /** Logs a resource access event (read-only). Not intended for extension. */
  public CompletableFuture<Void> logResourceAccess(
      String resourceType, String resourceId, String action, String ipAddress, String userAgent) {
    return logEvent(
        "RESOURCE_ACCESS",
        resourceType,
        resourceId,
        action,
        null,
        null,
        ipAddress,
        userAgent,
        null);
  }

  @Async
  /** Logs a data modification event with before/after snapshots. Not intended for extension. */
  public CompletableFuture<Void> logDataModification(
      String resourceType,
      String resourceId,
      String action,
      Map<String, Object> oldData,
      Map<String, Object> newData,
      String ipAddress,
      String userAgent) {
    return logEvent(
        "DATA_MODIFICATION",
        resourceType,
        resourceId,
        action,
        oldData,
        newData,
        ipAddress,
        userAgent,
        null);
  }

  @Async
  /** Logs a security-related event. Not intended for extension. */
  public CompletableFuture<Void> logSecurityEvent(
      final String eventType,
      final String description,
      final String ipAddress,
      final String userAgent,
      final Map<String, String> metadata) {
    return logEvent(
        eventType,
        "SECURITY",
        "SYSTEM",
        "SECURITY_EVENT",
        Map.of("description", description),
        null,
        ipAddress,
        userAgent,
        metadata);
  }

  @Async
  /** Logs a payment-related event. Not intended for extension. */
  public CompletableFuture<Void> logPaymentEvent(
      final String eventType,
      final String paymentId,
      final String action,
      final Map<String, Object> paymentData,
      final String ipAddress,
      final String userAgent) {
    return logEvent(
        eventType, "PAYMENT", paymentId, action, paymentData, null, ipAddress, userAgent, null);
  }

  // Query methods
  /** Retrieves paged organization audit events ordered by time. */
  @Transactional(readOnly = true)
  public Page<AuditEvent> getOrganizationAuditEvents(
      final UUID organizationId, final int page, final int size) {
    validateOrganizationAccess(organizationId);
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);
  }

  /** Retrieves paged user audit events ordered by time. */
  @Transactional(readOnly = true)
  public Page<AuditEvent> getUserAuditEvents(
      final UUID organizationId, final UUID userId, final int page, final int size) {
    validateOrganizationAccess(organizationId);
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.findByOrganizationIdAndActorIdOrderByCreatedAtDesc(
        organizationId, userId, pageable);
  }

  /** Retrieves events by event type. */
  @Transactional(readOnly = true)
  public Page<AuditEvent> getAuditEventsByType(
      final UUID organizationId, final String eventType, final int page, final int size) {
    validateOrganizationAccess(organizationId);
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.findByOrganizationIdAndActionOrderByCreatedAtDesc(
        organizationId, eventType, pageable);
  }

  /** Retrieves events within a date range. */
  @Transactional(readOnly = true)
  public Page<AuditEvent> getAuditEventsByDateRange(
      final UUID organizationId,
      final Instant startDate,
      final Instant endDate,
      final int page,
      final int size) {
    validateOrganizationAccess(organizationId);
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.findByOrganizationIdAndCreatedAtBetween(
        organizationId, startDate, endDate, pageable);
  }

  /** Retrieves resource audit trail for a specific resource. */
  @Transactional(readOnly = true)
  public List<AuditEvent> getResourceAuditTrail(
      final UUID organizationId, final String resourceType, final String resourceId) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findByOrganizationIdAndResourceTypeAndResourceId(
        organizationId, resourceType, resourceId);
  }

  /** Searches audit events using a free-text term. */
  @Transactional(readOnly = true)
  public Page<AuditEvent> searchAuditEvents(
      final UUID organizationId, final String searchTerm, final int page, final int size) {
    validateOrganizationAccess(organizationId);
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.searchAuditEvents(organizationId, searchTerm, pageable);
  }

  /**
   * Returns distinct event types available for an organization. Not intended for overriding; use
   * repository queries to extend.
   */
  @Transactional(readOnly = true)
  public List<String> getAvailableEventTypes(final UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findDistinctActionsByOrganizationId(organizationId);
  }

  /**
   * Returns distinct resource types available for an organization. Not intended for overriding.
   */
  @Transactional(readOnly = true)
  public List<String> getAvailableResourceTypes(final UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findDistinctResourceTypesByOrganizationId(organizationId);
  }

  // Security analysis methods
  /** Retrieves security events since a given timestamp for an organization. */
  @Transactional(readOnly = true)
  public List<AuditEvent> getSecurityEvents(final UUID organizationId, final Instant since) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findSecurityEventsAfter(organizationId, since);
  }

  /** Returns suspicious IP addresses with failed events count since a timestamp. */
  @Transactional(readOnly = true)
  public List<Object[]> getSuspiciousIpAddresses(
      final UUID organizationId, final Instant since, final long failureThreshold) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findSuspiciousIpAddresses(organizationId, since, failureThreshold);
  }

  /** Aggregates audit statistics for an organization. */
  @Transactional(readOnly = true)
  public AuditStatistics getAuditStatistics(final UUID organizationId) {
    validateOrganizationAccess(organizationId);

    Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
    Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
    Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);

    long totalEvents =
        auditEventRepository.countByOrganizationIdAndActionAndCreatedAtAfter(
            organizationId, "%", Instant.EPOCH);
    long recentEvents =
        auditEventRepository.countByOrganizationIdAndActionAndCreatedAtAfter(
            organizationId, "%", thirtyDaysAgo);
    long weeklyEvents =
        auditEventRepository.countByOrganizationIdAndActionAndCreatedAtAfter(
            organizationId, "%", sevenDaysAgo);
    long dailyEvents =
        auditEventRepository.countByOrganizationIdAndActionAndCreatedAtAfter(
            organizationId, "%", twentyFourHoursAgo);

    List<String> eventTypes =
        auditEventRepository.findDistinctActionsByOrganizationId(organizationId);
    List<String> resourceTypes =
        auditEventRepository.findDistinctResourceTypesByOrganizationId(organizationId);

    return new AuditStatistics(
        totalEvents,
        recentEvents,
        weeklyEvents,
        dailyEvents,
        eventTypes.size(),
        resourceTypes.size());
  }

  // GDPR compliance methods
  /**
   * Deletes audit events older than the configured retention period. Not intended for overriding;
   * behavior relies on service invariants.
   */
  @Transactional
  public int deleteExpiredAuditEvents() {
    Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
    int deletedCount = auditEventRepository.deleteEventsOlderThan(cutoffDate);
    logger.info("Deleted {} expired audit events older than {}", deletedCount, cutoffDate);
    return deletedCount;
  }

  /**
   * Deletes all audit events for the given organization. Not intended for overriding; use
   * repository extension points instead.
   */
  @Transactional
  public int deleteOrganizationAuditEvents(final UUID organizationId) {
    validateOrganizationAccess(organizationId);
    int deletedCount = auditEventRepository.deleteByOrganizationId(organizationId);
    logger.info("Deleted {} audit events for organization: {}", deletedCount, organizationId);
    return deletedCount;
  }

  /**
   * Deletes all audit events for the given user. Not intended for overriding; use repository
   * extension points instead.
   */
  @Transactional
  public int deleteUserAuditEvents(final UUID userId) {
    int deletedCount = auditEventRepository.deleteByActorId(userId);
    logger.info("Deleted {} audit events for user: {}", deletedCount, userId);
    return deletedCount;
  }

  /**
   * Redacts PII for the given user across all audit data. Not intended for overriding; behavior
   * must remain consistent.
   */
  @Transactional
  public int redactUserAuditData(final UUID userId) {
    int redactedCount = auditEventRepository.redactUserData(userId, REDACTED_PLACEHOLDER);
    logger.info("Redacted audit data for {} events for user: {}", redactedCount, userId);
    return redactedCount;
  }

    // Helper methods
    private String redactPii(final String data) {
        if (data == null || data.trim().isEmpty()) {
            return data;
        }

        String redacted = data;
        redacted = EMAIL_PATTERN.matcher(redacted).replaceAll(REDACTED_PLACEHOLDER);
        redacted = PHONE_PATTERN.matcher(redacted).replaceAll(REDACTED_PLACEHOLDER);
        redacted = SSN_PATTERN.matcher(redacted).replaceAll(REDACTED_PLACEHOLDER);
        redacted = CREDIT_CARD_PATTERN.matcher(redacted).replaceAll(REDACTED_PLACEHOLDER);

        return redacted;
    }

    private void validateOrganizationAccess(final UUID organizationId) {
        UUID currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new SecurityException("Authentication required");
        }

        UUID currentOrganizationId = TenantContext.getCurrentOrganizationId();
        if (currentOrganizationId != null && !currentOrganizationId.equals(organizationId)) {
            throw new SecurityException("Access denied - organization mismatch");
        }
    }

    public record AuditStatistics(
            long totalEvents,
            long recentEvents,
            long weeklyEvents,
            long dailyEvents,
            int uniqueEventTypes,
            int uniqueResourceTypes) { }
}
