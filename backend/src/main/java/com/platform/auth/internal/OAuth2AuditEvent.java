package com.platform.auth.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * OAuth2 Audit Event entity for compliance logging and security monitoring
 * Records all OAuth2-related activities for audit trails and forensic analysis
 *
 * This entity supports GDPR compliance by tracking user consent and data processing
 * activities while providing comprehensive audit trails for security monitoring.
 */
@Entity
@Table(name = "oauth2_audit_events",
       indexes = {
           @Index(name = "idx_oauth2_audit_event_type", columnList = "event_type"),
           @Index(name = "idx_oauth2_audit_user", columnList = "user_id"),
           @Index(name = "idx_oauth2_audit_session", columnList = "session_id"),
           @Index(name = "idx_oauth2_audit_provider", columnList = "provider"),
           @Index(name = "idx_oauth2_audit_timestamp", columnList = "event_timestamp"),
           @Index(name = "idx_oauth2_audit_ip", columnList = "ip_address"),
           @Index(name = "idx_oauth2_audit_severity", columnList = "severity")
       })
@EntityListeners(AuditingEntityListener.class)
public class OAuth2AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of OAuth2 event being audited
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private OAuth2EventType eventType;

    /**
     * Severity level of the audit event
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AuditSeverity severity = AuditSeverity.INFO;

    /**
     * User ID associated with this event (if applicable)
     */
    @Column(name = "user_id", length = 255)
    private String userId;

    /**
     * Session ID associated with this event (if applicable)
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    /**
     * OAuth2 provider involved in this event
     */
    @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "Provider must be lowercase alphanumeric")
    @Column(name = "provider", length = 50)
    private String provider;

    /**
     * Human-readable description of the event
     */
    @NotBlank
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    /**
     * IP address from which the event originated
     */
    @Column(name = "ip_address", length = 45) // IPv6 max length
    private String ipAddress;

    /**
     * User agent string from the request
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Additional event details as JSON
     */
    @Column(name = "event_details")
    private String eventDetails;

    /**
     * Error code if this is an error event
     */
    @Column(name = "error_code", length = 100)
    private String errorCode;

    /**
     * Error message if this is an error event
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * Request ID or correlation ID for tracing
     */
    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    /**
     * OAuth2 authorization code hash (for flow tracking)
     */
    @Column(name = "authorization_code_hash", length = 255)
    private String authorizationCodeHash;

    /**
     * OAuth2 state parameter hash (for CSRF tracking)
     */
    @Column(name = "state_hash", length = 255)
    private String stateHash;

    /**
     * Duration of the operation in milliseconds (for performance monitoring)
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * Whether this event represents a successful operation
     */
    @NotNull
    @Column(name = "success", nullable = false)
    private Boolean success = true;

    /**
     * Timestamp when the event occurred
     */
    @NotNull
    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor for JPA
    protected OAuth2AuditEvent() {}

    /**
     * Constructor for creating a new audit event
     */
    public OAuth2AuditEvent(OAuth2EventType eventType, String description) {
        this.eventType = eventType;
        this.description = description;
        this.eventTimestamp = Instant.now();
        this.severity = determineSeverity(eventType);
    }

    /**
     * OAuth2 Event Types for audit logging
     */
    public enum OAuth2EventType {
        // Authentication flow events
        AUTHORIZATION_STARTED,
        AUTHORIZATION_COMPLETED,
        AUTHORIZATION_FAILED,
        AUTHORIZATION_DENIED,

        // Token events
        TOKEN_EXCHANGE_STARTED,
        TOKEN_EXCHANGE_COMPLETED,
        TOKEN_EXCHANGE_FAILED,
        TOKEN_VALIDATION_FAILED,

        // Session events
        SESSION_CREATED,
        SESSION_RENEWED,
        SESSION_EXPIRED,
        SESSION_TERMINATED,

        // User events
        USER_LOGIN,
        USER_LOGOUT,
        USER_INFO_RETRIEVED,
        USER_INFO_UPDATED,
        USER_INFO_DELETED,

        // Security events
        PKCE_VALIDATION_FAILED,
        STATE_VALIDATION_FAILED,
        SUSPICIOUS_ACTIVITY,
        RATE_LIMIT_EXCEEDED,

        // Configuration events
        PROVIDER_CONFIGURED,
        PROVIDER_DISABLED,

        // Compliance events
        GDPR_DATA_EXPORT,
        GDPR_DATA_DELETION,
        CONSENT_GRANTED,
        CONSENT_REVOKED
    }

    /**
     * Audit severity levels
     */
    public enum AuditSeverity {
        DEBUG,   // Detailed flow information
        INFO,    // Normal operations
        WARN,    // Potential issues
        ERROR,   // Errors that were handled
        CRITICAL // Security issues or system failures
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public OAuth2EventType getEventType() {
        return eventType;
    }

    public void setEventType(OAuth2EventType eventType) {
        this.eventType = eventType;
    }

    public AuditSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AuditSeverity severity) {
        this.severity = severity;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    public void setEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getAuthorizationCodeHash() {
        return authorizationCodeHash;
    }

    public void setAuthorizationCodeHash(String authorizationCodeHash) {
        this.authorizationCodeHash = authorizationCodeHash;
    }

    public String getStateHash() {
        return stateHash;
    }

    public void setStateHash(String stateHash) {
        this.stateHash = stateHash;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Business methods

    /**
     * Marks this event as an error with details
     */
    public void markAsError(String errorCode, String errorMessage) {
        this.success = false;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.severity = AuditSeverity.ERROR;
    }

    /**
     * Marks this event as critical security issue
     */
    public void markAsCritical() {
        this.severity = AuditSeverity.CRITICAL;
    }

    /**
     * Sets security-related hashes for flow tracking
     */
    public void setSecurityHashes(String authCodeHash, String stateHash) {
        this.authorizationCodeHash = authCodeHash;
        this.stateHash = stateHash;
    }

    /**
     * Sets request context information
     */
    public void setRequestContext(String ipAddress, String userAgent, String correlationId) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.correlationId = correlationId;
    }

    /**
     * Determines appropriate severity based on event type
     */
    private static AuditSeverity determineSeverity(OAuth2EventType eventType) {
        return switch (eventType) {
            case AUTHORIZATION_FAILED, TOKEN_EXCHANGE_FAILED, TOKEN_VALIDATION_FAILED,
                 PKCE_VALIDATION_FAILED, STATE_VALIDATION_FAILED -> AuditSeverity.ERROR;
            case SUSPICIOUS_ACTIVITY, RATE_LIMIT_EXCEEDED -> AuditSeverity.CRITICAL;
            case SESSION_EXPIRED, AUTHORIZATION_DENIED, USER_INFO_DELETED -> AuditSeverity.WARN;
            default -> AuditSeverity.INFO;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OAuth2AuditEvent that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OAuth2AuditEvent{" +
                "id=" + id +
                ", eventType=" + eventType +
                ", severity=" + severity +
                ", userId='" + userId + '\'' +
                ", provider='" + provider + '\'' +
                ", success=" + success +
                ", eventTimestamp=" + eventTimestamp +
                '}';
    }
}