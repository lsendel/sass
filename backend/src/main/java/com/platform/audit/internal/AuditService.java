package com.platform.audit.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.shared.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
@Transactional
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final String REDACTED_PLACEHOLDER = "[REDACTED]";

    // PII patterns for automatic redaction
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;
    private final boolean enablePiiRedaction;
    private final int retentionDays;

    public AuditService(AuditEventRepository auditEventRepository,
                       ObjectMapper objectMapper,
                       @Value("${app.audit.enable-pii-redaction:true}") boolean enablePiiRedaction,
                       @Value("${app.audit.retention-days:2555}") int retentionDays) { // 7 years default
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
        this.enablePiiRedaction = enablePiiRedaction;
        this.retentionDays = retentionDays;
    }

    @Async
    public CompletableFuture<Void> logEvent(String eventType, String resourceType, String resourceId,
                                           String action, Map<String, Object> requestData,
                                           Map<String, Object> responseData, String ipAddress,
                                           String userAgent, Map<String, String> metadata) {
        try {
            UUID userId = TenantContext.getCurrentUserId();
            UUID organizationId = TenantContext.getCurrentOrganizationId();

            // Create audit event
            AuditEvent auditEvent = new AuditEvent(
                organizationId,
                userId,
                eventType,
                resourceType,
                resourceId,
                action,
                ipAddress,
                userAgent
            );

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

            logger.debug("Audit event logged: {} for resource: {}:{}", eventType, resourceType, resourceId);

        } catch (Exception e) {
            logger.error("Failed to log audit event: {} for resource: {}:{}", eventType, resourceType, resourceId, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    // Convenience methods for common audit events
    @Async
    public CompletableFuture<Void> logUserLogin(UUID userId, String ipAddress, String userAgent, boolean success) {
        String eventType = success ? "USER_LOGIN" : "USER_LOGIN_FAILED";
        Map<String, Object> metadata = Map.of(
            "success", success,
            "timestamp", Instant.now().toString()
        );

        return logEvent(eventType, "USER", userId.toString(), "LOGIN",
                       null, null, ipAddress, userAgent, Map.of("success", String.valueOf(success)));
    }

    @Async
    public CompletableFuture<Void> logUserLogout(UUID userId, String ipAddress, String userAgent) {
        return logEvent("USER_LOGOUT", "USER", userId.toString(), "LOGOUT",
                       null, null, ipAddress, userAgent, null);
    }

    @Async
    public CompletableFuture<Void> logResourceAccess(String resourceType, String resourceId, String action,
                                                    String ipAddress, String userAgent) {
        return logEvent("RESOURCE_ACCESS", resourceType, resourceId, action,
                       null, null, ipAddress, userAgent, null);
    }

    @Async
    public CompletableFuture<Void> logDataModification(String resourceType, String resourceId, String action,
                                                      Map<String, Object> oldData, Map<String, Object> newData,
                                                      String ipAddress, String userAgent) {
        return logEvent("DATA_MODIFICATION", resourceType, resourceId, action,
                       oldData, newData, ipAddress, userAgent, null);
    }

    @Async
    public CompletableFuture<Void> logSecurityEvent(String eventType, String description, String ipAddress,
                                                   String userAgent, Map<String, String> metadata) {
        return logEvent(eventType, "SECURITY", "SYSTEM", "SECURITY_EVENT",
                       Map.of("description", description), null, ipAddress, userAgent, metadata);
    }

    @Async
    public CompletableFuture<Void> logPaymentEvent(String eventType, String paymentId, String action,
                                                  Map<String, Object> paymentData, String ipAddress, String userAgent) {
        return logEvent(eventType, "PAYMENT", paymentId, action,
                       paymentData, null, ipAddress, userAgent, null);
    }

    // Query methods
    @Transactional(readOnly = true)
    public Page<AuditEvent> getOrganizationAuditEvents(UUID organizationId, int page, int size) {
        validateOrganizationAccess(organizationId);
        Pageable pageable = PageRequest.of(page, size);
        return auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getUserAuditEvents(UUID organizationId, UUID userId, int page, int size) {
        validateOrganizationAccess(organizationId);
        Pageable pageable = PageRequest.of(page, size);
        return auditEventRepository.findByOrganizationIdAndActorIdOrderByCreatedAtDesc(organizationId, userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getAuditEventsByType(UUID organizationId, String eventType, int page, int size) {
        validateOrganizationAccess(organizationId);
        Pageable pageable = PageRequest.of(page, size);
        return auditEventRepository.findByOrganizationIdAndActionOrderByCreatedAtDesc(organizationId, eventType, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> getAuditEventsByDateRange(UUID organizationId, Instant startDate, Instant endDate,
                                                     int page, int size) {
        validateOrganizationAccess(organizationId);
        Pageable pageable = PageRequest.of(page, size);
        return auditEventRepository.findByOrganizationIdAndCreatedAtBetween(organizationId, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getResourceAuditTrail(UUID organizationId, String resourceType, String resourceId) {
        validateOrganizationAccess(organizationId);
        return auditEventRepository.findByOrganizationIdAndResourceTypeAndResourceId(organizationId, resourceType, resourceId);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> searchAuditEvents(UUID organizationId, String searchTerm, int page, int size) {
        validateOrganizationAccess(organizationId);
        Pageable pageable = PageRequest.of(page, size);
        return auditEventRepository.searchAuditEvents(organizationId, searchTerm, pageable);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableEventTypes(UUID organizationId) {
        validateOrganizationAccess(organizationId);
        return auditEventRepository.findDistinctActionsByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public List<String> getAvailableResourceTypes(UUID organizationId) {
        validateOrganizationAccess(organizationId);
        return auditEventRepository.findDistinctResourceTypesByOrganizationId(organizationId);
    }

    // Security analysis methods
    @Transactional(readOnly = true)
    public List<AuditEvent> getSecurityEvents(UUID organizationId, Instant since) {
        validateOrganizationAccess(organizationId);
        return auditEventRepository.findSecurityEventsAfter(organizationId, since);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSuspiciousIpAddresses(UUID organizationId, Instant since, long failureThreshold) {
        validateOrganizationAccess(organizationId);
        return auditEventRepository.findSuspiciousIpAddresses(organizationId, since, failureThreshold);
    }

    @Transactional(readOnly = true)
    public AuditStatistics getAuditStatistics(UUID organizationId) {
        validateOrganizationAccess(organizationId);

        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);

        long totalEvents = auditEventRepository.countByOrganizationIdAndActionAndCreatedAtAfter(
            organizationId, "%", Instant.EPOCH);
        long recentEvents = auditEventRepository.countByOrganizationIdAndActionAndCreatedAtAfter(
            organizationId, "%", thirtyDaysAgo);
        long weeklyEvents = auditEventRepository.countByOrganizationIdAndActionAndCreatedAtAfter(
            organizationId, "%", sevenDaysAgo);
        long dailyEvents = auditEventRepository.countByOrganizationIdAndActionAndCreatedAtAfter(
            organizationId, "%", twentyFourHoursAgo);

        List<String> eventTypes = auditEventRepository.findDistinctActionsByOrganizationId(organizationId);
        List<String> resourceTypes = auditEventRepository.findDistinctResourceTypesByOrganizationId(organizationId);

        return new AuditStatistics(totalEvents, recentEvents, weeklyEvents, dailyEvents,
                                  eventTypes.size(), resourceTypes.size());
    }

    // GDPR compliance methods
    @Transactional
    public int deleteExpiredAuditEvents() {
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deletedCount = auditEventRepository.deleteEventsOlderThan(cutoffDate);
        logger.info("Deleted {} expired audit events older than {}", deletedCount, cutoffDate);
        return deletedCount;
    }

    @Transactional
    public int deleteOrganizationAuditEvents(UUID organizationId) {
        validateOrganizationAccess(organizationId);
        int deletedCount = auditEventRepository.deleteByOrganizationId(organizationId);
        logger.info("Deleted {} audit events for organization: {}", deletedCount, organizationId);
        return deletedCount;
    }

    @Transactional
    public int deleteUserAuditEvents(UUID userId) {
        int deletedCount = auditEventRepository.deleteByActorId(userId);
        logger.info("Deleted {} audit events for user: {}", deletedCount, userId);
        return deletedCount;
    }

    @Transactional
    public int redactUserAuditData(UUID userId) {
        int redactedCount = auditEventRepository.redactUserData(userId, REDACTED_PLACEHOLDER);
        logger.info("Redacted audit data for {} events for user: {}", redactedCount, userId);
        return redactedCount;
    }

    // Helper methods
    private String redactPii(String data) {
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

    private void validateOrganizationAccess(UUID organizationId) {
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
        int uniqueResourceTypes
    ) {}
}