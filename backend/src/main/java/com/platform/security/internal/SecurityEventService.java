package com.platform.security.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing security events with real-time processing capabilities.
 * Handles event ingestion, correlation, and real-time streaming to dashboard clients.
 *
 * Constitutional Compliance:
 * - Real-time processing: <1s latency for critical security events
 * - Observability: Structured logging with correlation IDs for all operations
 * - Performance: <200ms API response times with efficient database queries
 * - Security: All operations audited and logged for compliance
 */
@Service
@Validated
@Transactional
public class SecurityEventService {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventService.class);

    private static final String CACHE_NAME = "security-events";
    private static final String STATS_CACHE_NAME = "security-event-stats";

    private final SecurityEventRepository securityEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicLong eventCounter = new AtomicLong(0);

    // Real-time streaming destinations
    private static final String EVENTS_TOPIC = "/topic/security/events";
    private static final String CRITICAL_EVENTS_TOPIC = "/topic/security/events/critical";
    private static final String MODULE_EVENTS_TOPIC = "/topic/security/events/module/";

    public SecurityEventService(
            SecurityEventRepository securityEventRepository,
            ApplicationEventPublisher eventPublisher,
            SimpMessagingTemplate messagingTemplate) {
        this.securityEventRepository = securityEventRepository;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Create a new security event with real-time processing.
     * Publishes event for real-time dashboard updates and alerting.
     */
    @Transactional
    @CacheEvict(value = STATS_CACHE_NAME, allEntries = true)
    public SecurityEvent createSecurityEvent(@Valid @NotNull SecurityEvent event) {
        var correlationId = MDC.get("correlationId");
        log.info("Creating security event: type={}, severity={}, correlationId={}",
                event.getEventType(), event.getSeverity(), correlationId);

        // Set correlation ID if not provided
        if (event.getCorrelationId() == null && correlationId != null) {
            event.setCorrelationId(correlationId);
        }

        // Set timestamp if not provided
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        // Validate timestamp is not in future
        if (event.getTimestamp().isAfter(Instant.now().plusSeconds(60))) {
            throw new IllegalArgumentException("Event timestamp cannot be in the future");
        }

        // Save to database
        var savedEvent = securityEventRepository.save(event);
        long eventNumber = eventCounter.incrementAndGet();

        // Publish for real-time streaming and alerting
        publishSecurityEventCreated(savedEvent);

        // Stream to WebSocket subscribers (async)
        streamEventAsync(savedEvent);

        log.info("Security event created successfully: id={}, type={}, severity={}, eventNumber={}",
                savedEvent.getId(), savedEvent.getEventType(), savedEvent.getSeverity(), eventNumber);

        return savedEvent;
    }

    /**
     * Get security event by ID with audit logging.
     */
    @Transactional(readOnly = true)
    public Optional<SecurityEvent> getSecurityEvent(@NonNull UUID eventId) {
        log.debug("Retrieving security event: id={}", eventId);

        var event = securityEventRepository.findById(eventId);
        if (event.isPresent()) {
            log.debug("Security event found: id={}, type={}", eventId, event.get().getEventType());
        } else {
            log.warn("Security event not found: id={}", eventId);
        }

        return event;
    }

    /**
     * List security events with filtering and pagination.
     * Optimized for dashboard display with <200ms response time.
     */
    @Transactional(readOnly = true)
    public Page<SecurityEvent> listSecurityEvents(
            SecurityEvent.EventType eventType,
            SecurityEvent.Severity severity,
            Instant fromTimestamp,
            Instant toTimestamp,
            String userId,
            @Positive int page,
            @Positive int size) {

        log.debug("Listing security events: type={}, severity={}, from={}, to={}, userId={}, page={}, size={}",
                eventType, severity, fromTimestamp, toTimestamp, userId, page, size);

        // Create pageable with timestamp descending order
        Pageable pageable = PageRequest.of(page, Math.min(size, 1000),
                Sort.by(Sort.Direction.DESC, "timestamp"));

        // Set default time range if not provided (last 24 hours)
        if (fromTimestamp == null) {
            fromTimestamp = Instant.now().minusSeconds(24 * 60 * 60);
        }
        if (toTimestamp == null) {
            toTimestamp = Instant.now();
        }

        // Apply filters based on provided parameters
        Page<SecurityEvent> events;
        if (eventType != null && severity != null) {
            events = securityEventRepository.findByTypeAndSeverity(
                    eventType, severity, fromTimestamp, toTimestamp, pageable);
        } else if (userId != null) {
            events = securityEventRepository.findByUserId(userId, fromTimestamp, toTimestamp, pageable);
        } else if (eventType != null || severity != null) {
            // Use the general filter method for single criteria
            events = securityEventRepository.findWithFilters(
                    userId, null, severity, fromTimestamp, toTimestamp, pageable);
        } else {
            events = securityEventRepository.findByTimestampBetween(fromTimestamp, toTimestamp, pageable);
        }

        log.debug("Retrieved {} security events from {} total",
                events.getNumberOfElements(), events.getTotalElements());

        return events;
    }

    /**
     * Update security event resolution status.
     * Used for incident response workflow tracking.
     */
    @Transactional
    public SecurityEvent updateSecurityEvent(@NonNull UUID eventId,
                                           boolean resolved,
                                           String resolvedBy) {
        log.info("Updating security event resolution: id={}, resolved={}, resolvedBy={}",
                eventId, resolved, resolvedBy);

        var event = securityEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Security event not found: " + eventId));

        // Validate state transition for critical events
        if (event.getSeverity() == SecurityEvent.Severity.CRITICAL && resolved &&
            event.getResolved() != null && !event.getResolved()) {
            log.warn("Attempting to resolve critical event without investigation: id={}", eventId);
            // Could implement additional validation here
        }

        event.setResolved(resolved);
        event.setResolvedBy(resolvedBy);
        event.setResolvedAt(resolved ? Instant.now() : null);

        var savedEvent = securityEventRepository.save(event);

        // Publish update for real-time dashboard updates
        publishSecurityEventUpdated(savedEvent);

        log.info("Security event updated successfully: id={}, resolved={}",
                savedEvent.getId(), savedEvent.getResolved());

        return savedEvent;
    }

    /**
     * Get security events by correlation ID for tracing.
     * Critical for incident investigation and cross-module event correlation.
     */
    @Transactional(readOnly = true)
    public List<SecurityEvent> getEventsByCorrelationId(@NonNull String correlationId) {
        log.debug("Retrieving events by correlation ID: {}", correlationId);

        var events = securityEventRepository.findByCorrelationId(correlationId);
        log.debug("Found {} events for correlation ID: {}", events.size(), correlationId);

        return events;
    }

    /**
     * Get recent critical security events for dashboard alerting.
     * Returns events from the last 24 hours by default.
     */
    @Transactional(readOnly = true)
    public List<SecurityEvent> getRecentCriticalEvents(Instant since) {
        if (since == null) {
            since = Instant.now().minusSeconds(24 * 60 * 60); // Last 24 hours
        }

        log.debug("Retrieving recent critical events since: {}", since);

        var events = securityEventRepository.findHighSeverityEvents(
                since, Instant.now(), PageRequest.of(0, 100)).getContent();

        log.debug("Found {} critical events since {}", events.size(), since);

        return events;
    }

    /**
     * Get event statistics for dashboard summary widgets.
     * Provides aggregated counts by type and severity.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = STATS_CACHE_NAME, key = "#since")
    public Map<String, Object> getEventStatistics(Instant since) {
        if (since == null) {
            since = Instant.now().minusSeconds(24 * 60 * 60); // Last 24 hours
        }

        log.debug("Calculating event statistics since: {}", since);

        var endTime = Instant.now();
        var totalEvents = securityEventRepository.countEventsByTimeRange(since, endTime);
        var criticalEvents = securityEventRepository.countBySeverity(
                SecurityEvent.Severity.CRITICAL, since, endTime);
        var highEvents = securityEventRepository.countBySeverity(
                SecurityEvent.Severity.HIGH, since, endTime);
        var unresolvedEvents = securityEventRepository.countUnresolvedEvents(since, endTime);

        var hourlyTrends = securityEventRepository.getHourlyEventCounts(since, endTime);
        var severityTrends = securityEventRepository.getSeverityTrends(since, endTime);
        var topSourceIps = securityEventRepository.getTopSourceIps(since, endTime, 10);

        var statistics = Map.of(
                "period", since,
                "totalEvents", totalEvents,
                "criticalEvents", criticalEvents,
                "highEvents", highEvents,
                "unresolvedEvents", unresolvedEvents,
                "hourlyTrends", hourlyTrends,
                "severityTrends", severityTrends,
                "topSourceIps", topSourceIps,
                "lastUpdated", Instant.now()
        );

        log.debug("Calculated event statistics for {} total events", totalEvents);

        return statistics;
    }

    /**
     * Stream security events for real-time dashboard updates.
     * Returns events newer than the provided timestamp.
     */
    @Transactional(readOnly = true)
    public List<SecurityEvent> streamSecurityEvents(Instant since, int limit) {
        log.debug("Streaming security events since: {}, limit: {}", since, limit);

        var pageable = PageRequest.of(0, Math.min(limit, 100),
                Sort.by(Sort.Direction.DESC, "timestamp"));

        var events = securityEventRepository.findRecentEvents(since, pageable).getContent();

        log.debug("Streaming {} security events", events.size());

        return events;
    }

    /**
     * Delete old security events based on retention policy.
     * Used by cleanup jobs to maintain database performance.
     */
    @Transactional
    public int deleteEventsOlderThan(Instant cutoffTime) {
        log.info("Deleting security events older than: {}", cutoffTime);

        var deletedCount = securityEventRepository.deleteEventsByAge(cutoffTime);

        log.info("Deleted {} security events older than {}", deletedCount, cutoffTime);

        return deletedCount;
    }

    /**
     * Publish security event created for real-time processing.
     */
    private void publishSecurityEventCreated(SecurityEvent event) {
        try {
            // Create event for real-time streaming
            var streamingEvent = new SecurityEventCreatedEvent(
                    event.getId(),
                    event.getEventType(),
                    event.getSeverity(),
                    event.getTimestamp(),
                    event.getCorrelationId()
            );

            eventPublisher.publishEvent(streamingEvent);

            log.debug("Published SecurityEventCreated event: id={}", event.getId());
        } catch (Exception e) {
            log.error("Failed to publish SecurityEventCreated event: id={}", event.getId(), e);
            // Don't rethrow - event creation should succeed even if publishing fails
        }
    }

    /**
     * Publish security event updated for real-time processing.
     */
    private void publishSecurityEventUpdated(SecurityEvent event) {
        try {
            var updateEvent = new SecurityEventUpdatedEvent(
                    event.getId(),
                    event.getResolved(),
                    event.getResolvedBy(),
                    event.getResolvedAt()
            );

            eventPublisher.publishEvent(updateEvent);

            log.debug("Published SecurityEventUpdated event: id={}", event.getId());
        } catch (Exception e) {
            log.error("Failed to publish SecurityEventUpdated event: id={}", event.getId(), e);
        }
    }

    /**
     * Stream security event to real-time subscribers (async)
     */
    private void streamEventAsync(SecurityEvent event) {
        CompletableFuture.runAsync(() -> {
            try {
                // Stream to general events topic
                messagingTemplate.convertAndSend(EVENTS_TOPIC, createEventMessage(event));

                // Stream critical events to special topic
                if (event.getSeverity() == SecurityEvent.Severity.CRITICAL) {
                    messagingTemplate.convertAndSend(CRITICAL_EVENTS_TOPIC, createEventMessage(event));
                }

                // Stream to module-specific topic
                String moduleTopicDestination = MODULE_EVENTS_TOPIC + event.getSourceModule();
                messagingTemplate.convertAndSend(moduleTopicDestination, createEventMessage(event));

                log.debug("Event streamed: eventId={}, topics={}", event.getId(),
                         List.of(EVENTS_TOPIC, moduleTopicDestination));

            } catch (Exception e) {
                log.error("Failed to stream security event: eventId={}", event.getId(), e);
            }
        });
    }

    /**
     * Create event message for streaming
     */
    private Map<String, Object> createEventMessage(SecurityEvent event) {
        return Map.of(
                "id", event.getId(),
                "eventType", event.getEventType(),
                "severity", event.getSeverity(),
                "timestamp", event.getTimestamp(),
                "sourceModule", event.getSourceModule(),
                "correlationId", event.getCorrelationId(),
                "resolved", event.getResolved(),
                "details", event.getDetails()
        );
    }

    /**
     * Get current event counter value
     */
    public long getCurrentEventCount() {
        return eventCounter.get();
    }

    /**
     * Event for security event creation notifications.
     */
    public record SecurityEventCreatedEvent(
            UUID eventId,
            SecurityEvent.EventType eventType,
            SecurityEvent.Severity severity,
            Instant timestamp,
            String correlationId
    ) {}

    /**
     * Event for security event update notifications.
     */
    public record SecurityEventUpdatedEvent(
            UUID eventId,
            Boolean resolved,
            String resolvedBy,
            Instant resolvedAt
    ) {}
}