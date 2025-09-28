package com.platform.security.api;

import com.platform.security.internal.SecurityEvent;
import com.platform.security.internal.SecurityEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API controller for security events management.
 * Implements all endpoints from security-events-api.yaml contract.
 *
 * Constitutional Compliance:
 * - Performance: <200ms API response times (95th percentile)
 * - Real-time: Server-Sent Events for real-time dashboard updates
 * - Security: Role-based access controls for all endpoints
 * - Observability: Request/response logging with correlation IDs
 */
@RestController
@RequestMapping("/api/v1/security-events")
@Validated
@PreAuthorize("hasRole('SECURITY_ANALYST') or hasRole('SECURITY_ADMIN')")
public class SecurityEventsController {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventsController.class);

    private final SecurityEventService securityEventService;
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();

    public SecurityEventsController(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    /**
     * List security events with filtering and pagination.
     * GET /api/v1/security-events
     */
    @GetMapping
    public ResponseEntity<PagedSecurityEventsResponse> listSecurityEvents(
            @RequestParam(required = false) SecurityEvent.EventType eventType,
            @RequestParam(required = false) SecurityEvent.Severity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTimestamp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTimestamp,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int size) {

        log.info("Listing security events: type={}, severity={}, from={}, to={}, userId={}, page={}, size={}",
                eventType, severity, fromTimestamp, toTimestamp, userId, page, size);

        var events = securityEventService.listSecurityEvents(
                eventType, severity, fromTimestamp, toTimestamp, userId, page, size);

        var response = new PagedSecurityEventsResponse(
                events.getContent(),
                new PageInfo(
                        events.getNumber(),
                        events.getSize(),
                        events.getTotalElements(),
                        events.getTotalPages(),
                        events.isFirst(),
                        events.isLast()
                )
        );

        log.info("Retrieved {} security events from {} total",
                events.getNumberOfElements(), events.getTotalElements());

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new security event.
     * POST /api/v1/security-events
     */
    @PostMapping
    @PreAuthorize("hasRole('SECURITY_ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<SecurityEvent> createSecurityEvent(@Valid @RequestBody SecurityEventCreateRequest request) {
        log.info("Creating security event: type={}, severity={}, sourceModule={}",
                request.eventType(), request.severity(), request.sourceModule());

        // Convert request to entity
        var event = new SecurityEvent();
        event.setEventType(request.eventType());
        event.setSeverity(request.severity());
        event.setUserId(request.userId());
        event.setSessionId(request.sessionId());
        event.setSourceModule(request.sourceModule());
        event.setSourceIp(request.sourceIp());
        event.setUserAgent(request.userAgent());
        event.setDetails(request.details());
        event.setCorrelationId(request.correlationId());

        var savedEvent = securityEventService.createSecurityEvent(event);

        // Notify real-time subscribers
        notifyRealTimeSubscribers(savedEvent);

        log.info("Security event created successfully: id={}, type={}, severity={}",
                savedEvent.getId(), savedEvent.getEventType(), savedEvent.getSeverity());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvent);
    }

    /**
     * Get security event by ID.
     * GET /api/v1/security-events/{eventId}
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<SecurityEvent> getSecurityEvent(@PathVariable @NotNull String eventId) {
        log.debug("Retrieving security event: id={}", eventId);

        var event = securityEventService.getSecurityEvent(eventId);

        if (event.isPresent()) {
            log.debug("Security event found: id={}, type={}", eventId, event.get().getEventType());
            return ResponseEntity.ok(event.get());
        } else {
            log.warn("Security event not found: id={}", eventId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update security event resolution status.
     * PATCH /api/v1/security-events/{eventId}
     */
    @PatchMapping("/{eventId}")
    @PreAuthorize("hasRole('SECURITY_ADMIN') or hasRole('SECURITY_ANALYST')")
    public ResponseEntity<SecurityEvent> updateSecurityEvent(
            @PathVariable @NotNull String eventId,
            @Valid @RequestBody SecurityEventUpdateRequest request) {

        log.info("Updating security event: id={}, resolved={}, resolvedBy={}",
                eventId, request.resolved(), request.resolvedBy());

        try {
            var updatedEvent = securityEventService.updateSecurityEvent(
                    eventId, request.resolved(), request.resolvedBy());

            // Notify real-time subscribers
            notifyRealTimeSubscribers(updatedEvent);

            log.info("Security event updated successfully: id={}, resolved={}",
                    updatedEvent.getId(), updatedEvent.getResolved());

            return ResponseEntity.ok(updatedEvent);

        } catch (IllegalArgumentException e) {
            log.warn("Security event not found for update: id={}", eventId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Real-time security event stream via Server-Sent Events.
     * GET /api/v1/security-events/stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSecurityEvents(
            @RequestParam(required = false) List<SecurityEvent.EventType> eventTypes,
            @RequestParam(required = false) List<SecurityEvent.Severity> severities) {

        log.info("Starting security events stream: eventTypes={}, severities={}", eventTypes, severities);

        var emitter = new SseEmitter(60000L); // 60 second timeout
        var connectionId = java.util.UUID.randomUUID().toString();

        // Store connection for real-time updates
        activeConnections.put(connectionId, emitter);

        // Handle connection cleanup
        emitter.onCompletion(() -> {
            activeConnections.remove(connectionId);
            log.debug("Security events stream completed: connectionId={}", connectionId);
        });

        emitter.onTimeout(() -> {
            activeConnections.remove(connectionId);
            log.debug("Security events stream timed out: connectionId={}", connectionId);
        });

        emitter.onError(throwable -> {
            activeConnections.remove(connectionId);
            log.warn("Security events stream error: connectionId={}", connectionId, throwable);
        });

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("connectionId", connectionId, "timestamp", Instant.now())));
        } catch (Exception e) {
            log.error("Failed to send initial connection event", e);
            emitter.completeWithError(e);
        }

        log.info("Security events stream established: connectionId={}", connectionId);

        return emitter;
    }

    /**
     * Get security event statistics for dashboard.
     * GET /api/v1/security-events/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getEventStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {

        log.debug("Getting security event statistics: since={}", since);

        var statistics = securityEventService.getEventStatistics(since);

        log.debug("Retrieved event statistics for {} period", since);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get events by correlation ID for incident investigation.
     * GET /api/v1/security-events/correlation/{correlationId}
     */
    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<SecurityEvent>> getEventsByCorrelation(@PathVariable @NotNull String correlationId) {
        log.debug("Getting events by correlation ID: {}", correlationId);

        var events = securityEventService.getEventsByCorrelationId(correlationId);

        log.debug("Found {} events for correlation ID: {}", events.size(), correlationId);

        return ResponseEntity.ok(events);
    }

    /**
     * Get recent critical events for alerting dashboard.
     * GET /api/v1/security-events/critical
     */
    @GetMapping("/critical")
    public ResponseEntity<List<SecurityEvent>> getRecentCriticalEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {

        log.debug("Getting recent critical events: since={}", since);

        var events = securityEventService.getRecentCriticalEvents(since);

        log.debug("Found {} recent critical events", events.size());

        return ResponseEntity.ok(events);
    }

    // Helper methods

    private void notifyRealTimeSubscribers(SecurityEvent event) {
        if (activeConnections.isEmpty()) {
            return;
        }

        log.debug("Notifying {} real-time subscribers of event: id={}, type={}",
                activeConnections.size(), event.getId(), event.getEventType());

        var eventData = Map.of(
                "type", "security-event",
                "event", event,
                "timestamp", Instant.now()
        );

        // Send to all active connections
        activeConnections.entrySet().removeIf(entry -> {
            try {
                entry.getValue().send(SseEmitter.event()
                        .name("security-event")
                        .data(eventData));
                return false; // Keep connection
            } catch (Exception e) {
                log.warn("Failed to send event to connection: {}", entry.getKey(), e);
                return true; // Remove connection
            }
        });
    }

    // Request/Response DTOs

    public record SecurityEventCreateRequest(
            @NotNull SecurityEvent.EventType eventType,
            @NotNull SecurityEvent.Severity severity,
            String userId,
            String sessionId,
            @NotNull String sourceModule,
            String sourceIp,
            String userAgent,
            Map<String, Object> details,
            String correlationId
    ) {}

    public record SecurityEventUpdateRequest(
            @NotNull Boolean resolved,
            String resolvedBy
    ) {}

    public record PagedSecurityEventsResponse(
            List<SecurityEvent> content,
            PageInfo page
    ) {}

    public record PageInfo(
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {}
}