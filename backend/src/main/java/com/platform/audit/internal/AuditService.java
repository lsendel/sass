package com.platform.audit.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.shared.security.TenantContext;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * A transactional service for creating, querying, and managing audit events.
 *
 * <p>This service provides a comprehensive API for the audit module. It handles the logic for
 * logging events asynchronously, redacting Personally Identifiable Information (PII) based on
 * configuration, and querying audit data with tenancy-based security checks. It also includes
 * methods for GDPR compliance, such as data retention and deletion.
 *
 * @see AuditEvent
 * @see AuditEventRepository
 */
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

  /**
   * Constructs the AuditService with its dependencies and configuration.
   *
   * @param auditEventRepository the repository for audit event persistence
   * @param objectMapper the Jackson mapper for JSON serialization
   * @param enablePiiRedaction a flag to control automatic PII redaction
   * @param retentionDays the number of days to retain audit events
   */
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

  /**
   * Asynchronously logs a detailed audit event.
   *
   * <p>This is the core logging method that handles event creation, PII redaction, and persistence.
   * It is designed to be non-blocking.
   *
   * @param eventType the high-level type of the event (e.g., "USER_LOGIN")
   * @param resourceType the type of the resource being acted upon (e.g., "USER")
   * @param resourceId the ID of the resource
   * @param action the specific action performed (e.g., "LOGIN")
   * @param requestData a map representing the request payload or "before" state
   * @param responseData a map representing the response payload or "after" state
   * @param ipAddress the source IP address of the request
   * @param userAgent the user agent of the client
   * @param metadata a map of additional, unstructured metadata
   * @return a {@link CompletableFuture} that completes when the operation is finished
   */
  @Async
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

  /**
   * Asynchronously logs a user login attempt.
   *
   * @param userId the ID of the user attempting to log in
   * @param ipAddress the source IP address
   * @param userAgent the client's user agent
   * @param success {@code true} if the login was successful, {@code false} otherwise
   * @return a {@link CompletableFuture} that completes when the log operation is finished
   */
  @Async
  public CompletableFuture<Void> logUserLogin(
      final UUID userId, final String ipAddress, final String userAgent, final boolean success) {
    String eventType = success ? "USER_LOGIN" : "USER_LOGIN_FAILED";
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

  /**
   * Asynchronously logs a user logout event.
   *
   * @param userId the ID of the user logging out
   * @param ipAddress the source IP address
   * @param userAgent the client's user agent
   * @return a {@link CompletableFuture} that completes when the log operation is finished
   */
  @Async
  public CompletableFuture<Void> logUserLogout(
      final UUID userId, final String ipAddress, final String userAgent) {
    return logEvent(
        "USER_LOGOUT", "USER", userId.toString(), "LOGOUT", null, null, ipAddress, userAgent, null);
  }

  /**
   * Asynchronously logs a read-only resource access event.
   *
   * @param resourceType the type of the accessed resource
   * @param resourceId the ID of the accessed resource
   * @param action the specific access action (e.g., "READ", "LIST")
   * @param ipAddress the source IP address
   * @param userAgent the client's user agent
   * @return a {@link CompletableFuture} that completes when the log operation is finished
   */
  @Async
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

  /**
   * Asynchronously logs a data modification event, capturing the state before and after the change.
   *
   * @param resourceType the type of the modified resource
   * @param resourceId the ID of the modified resource
   * @param action the modification action (e.g., "UPDATE", "CREATE")
   * @param oldData a map representing the state of the data before modification
   * @param newData a map representing the state of the data after modification
   * @param ipAddress the source IP address
   * @param userAgent the client's user agent
   * @return a {@link CompletableFuture} that completes when the log operation is finished
   */
  @Async
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

  /**
   * Asynchronously logs a security-related event.
   *
   * @param eventType the specific type of security event (e.g., "CSRF_DETECTED")
   * @param description a description of the event
   * @param ipAddress the source IP address
   * @param userAgent the client's user agent
   * @param metadata additional metadata about the event
   * @return a {@link CompletableFuture} that completes when the log operation is finished
   */
  @Async
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

  /**
   * Asynchronously logs a payment-related event.
   *
   * @param eventType the specific type of payment event (e.g., "PAYMENT_SUCCESS")
   * @param paymentId the ID of the payment
   * @param action the action performed (e.g., "PROCESS")
   * @param paymentData data associated with the payment
   * @param ipAddress the source IP address
   * @param userAgent the client's user agent
   * @return a {@link CompletableFuture} that completes when the log operation is finished
   */
  @Async
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

  /**
   * Retrieves a paginated list of audit events for an organization.
   *
   * @param organizationId the ID of the organization
   * @param page the page number to retrieve
   * @param size the number of events per page
   * @return a {@link Page} of audit events
   */
  @Transactional(readOnly = true)
  public Page<AuditEvent> getOrganizationAuditEvents(
      final UUID organizationId, final int page, final int size) {
    validateOrganizationAccess(organizationId);
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);
  }

  /**
   * Retrieves a paginated list of audit events for a specific user within an organization.
   *
   * @param organizationId the ID of the organization
   * @param userId the ID of the user
   * @param page the page number
   * @param size the page size
   * @return a {@link Page} of audit events
   */
  @Transactional(readOnly = true)
  public Page<AuditEvent> getUserAuditEvents(
      final UUID organizationId, final UUID userId, final int page, final int size) {
    validateOrganizationAccess(organizationId);
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.findByOrganizationIdAndActorIdOrderByCreatedAtDesc(
        organizationId, userId, pageable);
  }

  /**
   * Retrieves a paginated list of audit events of a specific type for an organization.
   *
   * @param organizationId the ID of the organization
   * @param eventType the type of event to retrieve
   * @param page the page number
   * @param size the page size
   * @return a {@link Page} of audit events
   */
  @Transactional(readOnly = true)
  public Page<AuditEvent> getAuditEventsByType(
      final UUID organizationId, final String eventType, final int page, final int size) {
    validateOrganizationAccess(organizationId);
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.findByOrganizationIdAndActionOrderByCreatedAtDesc(
        organizationId, eventType, pageable);
  }

  /**
   * Retrieves a paginated list of audit events within a specific date range for an organization.
   *
   * @param organizationId the ID of the organization
   * @param startDate the start of the date range
   * @param endDate the end of the date range
   * @param page the page number
   * @param size the page size
   * @return a {@link Page} of audit events
   */
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

  /**
   * Retrieves the complete audit trail for a specific resource.
   *
   * @param organizationId the ID of the organization
   * @param resourceType the type of the resource
   * @param resourceId the ID of the resource
   * @return a list of all audit events for the specified resource
   */
  @Transactional(readOnly = true)
  public List<AuditEvent> getResourceAuditTrail(
      final UUID organizationId, final String resourceType, final String resourceId) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findByOrganizationIdAndResourceTypeAndResourceId(
        organizationId, resourceType, resourceId);
  }

  /**
   * Performs a free-text search on audit events for an organization.
   *
   * @param organizationId the ID of the organization
   * @param searchTerm the term to search for
   * @param page the page number
   * @param size the page size
   * @return a {@link Page} of audit events matching the search term
   */
  @Transactional(readOnly = true)
  public Page<AuditEvent> searchAuditEvents(
      final UUID organizationId, final String searchTerm, final int page, final int size) {
    validateOrganizationAccess(organizationId);
    Pageable pageable = PageRequest.of(page, size);
    return auditEventRepository.searchAuditEvents(organizationId, searchTerm, pageable);
  }

  /**
   * Retrieves a list of all distinct event types recorded for an organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of distinct event type strings
   */
  @Transactional(readOnly = true)
  public List<String> getAvailableEventTypes(final UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findDistinctActionsByOrganizationId(organizationId);
  }

  /**
   * Retrieves a list of all distinct resource types recorded for an organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of distinct resource type strings
   */
  @Transactional(readOnly = true)
  public List<String> getAvailableResourceTypes(final UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findDistinctResourceTypesByOrganizationId(organizationId);
  }

  /**
   * Retrieves critical security events for an organization that occurred since a given time.
   *
   * @param organizationId the ID of the organization
   * @param since the timestamp from which to retrieve events
   * @return a list of security-related audit events
   */
  @Transactional(readOnly = true)
  public List<AuditEvent> getSecurityEvents(final UUID organizationId, final Instant since) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findSecurityEventsAfter(organizationId, since);
  }

  /**
   * Identifies suspicious IP addresses based on the number of failed login attempts.
   *
   * @param organizationId the ID of the organization
   * @param since the timestamp from which to analyze failures
   * @param failureThreshold the minimum number of failures to be considered suspicious
   * @return a list of Object arrays, each containing an IP address and the count of failures
   */
  @Transactional(readOnly = true)
  public List<Object[]> getSuspiciousIpAddresses(
      final UUID organizationId, final Instant since, final long failureThreshold) {
    validateOrganizationAccess(organizationId);
    return auditEventRepository.findSuspiciousIpAddresses(organizationId, since, failureThreshold);
  }

  /**
   * Aggregates and returns key audit statistics for an organization.
   *
   * @param organizationId the ID of the organization
   * @return an {@link AuditStatistics} record containing the aggregated data
   */
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

  /**
   * Deletes audit events that are older than the configured retention period.
   *
   * @return the number of deleted events
   */
  @Transactional
  public int deleteExpiredAuditEvents() {
    Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
    int deletedCount = auditEventRepository.deleteEventsOlderThan(cutoffDate);
    logger.info("Deleted {} expired audit events older than {}", deletedCount, cutoffDate);
    return deletedCount;
  }

  /**
   * Deletes all audit events associated with a specific organization.
   *
   * @param organizationId the ID of the organization to delete events for
   * @return the number of deleted events
   */
  @Transactional
  public int deleteOrganizationAuditEvents(final UUID organizationId) {
    validateOrganizationAccess(organizationId);
    int deletedCount = auditEventRepository.deleteByOrganizationId(organizationId);
    logger.info("Deleted {} audit events for organization: {}", deletedCount, organizationId);
    return deletedCount;
  }

  /**
   * Deletes all audit events associated with a specific user.
   *
   * @param userId the ID of the user to delete events for
   * @return the number of deleted events
   */
  @Transactional
  public int deleteUserAuditEvents(final UUID userId) {
    int deletedCount = auditEventRepository.deleteByActorId(userId);
    logger.info("Deleted {} audit events for user: {}", deletedCount, userId);
    return deletedCount;
  }

  /**
   * Redacts PII from the details of all audit events associated with a specific user.
   *
   * @param userId the ID of the user whose data should be redacted
   * @return the number of updated events
   */
  @Transactional
  public int redactUserAuditData(final UUID userId) {
    int redactedCount = auditEventRepository.redactUserData(userId, REDACTED_PLACEHOLDER);
    logger.info("Redacted audit data for {} events for user: {}", redactedCount, userId);
    return redactedCount;
  }

  /**
   * Redacts common PII patterns from a given string.
   *
   * @param data the input string to sanitize
   * @return the string with PII patterns replaced by a placeholder
   */
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

  /**
   * Validates that the current authenticated principal has access to the given organization.
   *
   * @param organizationId the ID of the organization to check
   * @throws SecurityException if access is denied
   */
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

  /**
   * A record to hold aggregated audit statistics.
   *
   * @param totalEvents total number of events ever recorded
   * @param recentEvents number of events in the last 30 days
   * @param weeklyEvents number of events in the last 7 days
   * @param dailyEvents number of events in the last 24 hours
   * @param uniqueEventTypes the count of distinct event types
   * @param uniqueResourceTypes the count of distinct resource types
   */
  public record AuditStatistics(
      long totalEvents,
      long recentEvents,
      long weeklyEvents,
      long dailyEvents,
      int uniqueEventTypes,
      int uniqueResourceTypes) {}
}
