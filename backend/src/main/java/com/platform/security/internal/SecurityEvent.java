package com.platform.security.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * SecurityEvent entity for tracking security-related events across the platform.
 *
 * This entity represents real-time security events from all platform modules
 * including authentication attempts, payment fraud detection, API abuse, etc.
 *
 * Key features:
 * - Time-series optimized with timestamp indexing
 * - JSON details field for flexible event metadata
 * - Correlation ID for cross-module event tracking
 * - Resolution tracking for incident response
 * - Source module and IP tracking for forensics
 */
@Entity
@Table(name = "security_events", indexes = {
    @Index(name = "idx_security_events_timestamp", columnList = "timestamp DESC"),
    @Index(name = "idx_security_events_event_type", columnList = "eventType"),
    @Index(name = "idx_security_events_severity", columnList = "severity"),
    @Index(name = "idx_security_events_user_id", columnList = "userId"),
    @Index(name = "idx_security_events_correlation_id", columnList = "correlationId"),
    @Index(name = "idx_security_events_resolved", columnList = "resolved"),
    @Index(name = "idx_security_events_source_module", columnList = "sourceModule")
})
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @NotNull
    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @NotNull
    @Column(name = "source_module", nullable = false, length = 100)
    private String sourceModule;

    @Column(name = "source_ip", length = 45) // IPv6 max length
    private String sourceIp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @NotNull
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> details;

    @NotNull
    @Column(name = "correlation_id", nullable = false, length = 255)
    private String correlationId;

    @NotNull
    @Column(nullable = false)
    private Boolean resolved = false;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Security event types supported by the platform
     */
    public enum EventType {
        LOGIN_ATTEMPT,
        PERMISSION_CHECK,
        PAYMENT_FRAUD,
        DATA_ACCESS,
        API_ABUSE
    }

    /**
     * Security event severity levels
     */
    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }

    // Constructors
    protected SecurityEvent() {
        // JPA constructor
    }

    public SecurityEvent(@NotNull EventType eventType,
                        @NotNull Severity severity,
                        @NotNull String sourceModule,
                        @NotNull String correlationId,
                        @NotNull Map<String, Object> details) {
        this.eventType = eventType;
        this.severity = severity;
        this.sourceModule = sourceModule;
        this.correlationId = correlationId;
        this.details = details;
        this.timestamp = Instant.now();
        this.resolved = false;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Mark this security event as resolved
     *
     * @param resolvedBy The user or system that resolved the event
     */
    public void markAsResolved(String resolvedBy) {
        this.resolved = true;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = Instant.now();
    }

    /**
     * Check if this is a critical security event that requires immediate attention
     */
    public boolean isCritical() {
        return severity == Severity.CRITICAL;
    }

    /**
     * Check if this event matches the given correlation ID for event tracking
     */
    public boolean hasCorrelationId(String correlationId) {
        return this.correlationId != null && this.correlationId.equals(correlationId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityEvent that = (SecurityEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SecurityEvent{" +
                "id=" + id +
                ", eventType=" + eventType +
                ", severity=" + severity +
                ", timestamp=" + timestamp +
                ", sourceModule='" + sourceModule + '\'' +
                ", resolved=" + resolved +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}