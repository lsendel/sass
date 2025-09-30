package com.platform.audit.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents a single, immutable audit event record in the system.
 *
 * <p>This entity is designed for comprehensive compliance and security audit logging. It captures
 * who did what, when, and to which resource. The class includes features for GDPR compliance, such
 * as automatic PII (Personally Identifiable Information) redaction from the details map.
 *
 * <p>The associated database table {@code audit_events} is indexed for efficient querying on common
 * fields like actor, organization, and timestamp.
 *
 * @see AuditService
 * @see AuditDetailsConverter
 */
@Entity
@Table(
        name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_events_user", columnList = "user_id"),
                @Index(name = "idx_audit_events_org", columnList = "organization_id"),
                @Index(name = "idx_audit_events_correlation", columnList = "correlation_id"),
                @Index(name = "idx_audit_events_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_events_action", columnList = "action")
        })
public final class AuditEvent {

    private static final int DEFAULT_RETENTION_DAYS = 365;
    private static final int MAX_STRING_LENGTH = 1000;
    private static final int STRING_TRUNCATE_LENGTH = 997;
    private static final int MIN_CREDIT_CARD_LENGTH = 13;
    private static final int MAX_CREDIT_CARD_LENGTH = 19;

    /** The unique identifier for the audit event. */
    @Id
    @UuidGenerator
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID id;

    /** The type of event. */
    @NotBlank
    @Size(max = 100)
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /** The ID of the organization this event is associated with. Can be null. */
    @Column(name = "organization_id")
    private UUID organizationId;

    /** The ID of the user or system principal that performed the action. */
    @Column(name = "user_id")
    private UUID actorId;

    /** The email of the user who performed the action. */
    @Column(name = "user_email", length = 255)
    private String userEmail;

    /** The IP address from which the action was initiated. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** The user agent string. */
    @Column(name = "user_agent")
    private String userAgent;

    /** Description of the event. */
    @Column(name = "description")
    private String description;

    /**
     * A flexible map of additional details about the event, stored as JSON. Sensitive information in
     * this map is automatically redacted.
       */
    @Column(name = "event_data")
    @Convert(converter = AuditDetailsConverter.class)
    private Map<String, Object> details = Map.of();

    /** A unique ID to correlate multiple events within a single request or transaction. */
    @Size(max = 255)
    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    /** The severity level of the event. */
    @Size(max = 20)
    @Column(name = "severity", nullable = false, length = 20)
    private String severity = "LOW";

    /** The timestamp when the event was created. */
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant createdAt;

    /** The module that generated the event. */
    @Size(max = 50)
    @Column(name = "module", nullable = false, length = 50)
    private String module;

    /** A dot-separated string representing the action performed (e.g., "user.login"). */
    @NotBlank
    @Size(max = 100)
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** The type of resource that was affected (e.g., "payment", "subscription"). */
    @Size(max = 255)
    @Column(name = "resource", length = 255)
    private String resourceType;

    /** Whether this event contains sensitive data. */
    @Column(name = "sensitive_data", nullable = false)
    private Boolean sensitiveData = false;

    /** When this audit record expires for retention. */
    @Column(name = "retention_expiry", nullable = false)
    private Instant retentionExpiry;

    /**
     * Protected no-argument constructor required by JPA.
       */
    protected AuditEvent() {
        // JPA constructor
    }

    /**
     * Creates a new audit event with the essential actor and action.
     *
     * @param actorId the ID of the principal performing the action
     * @param action the action being performed
       */
    public AuditEvent(final UUID actorId, final String action) {
        this.actorId = actorId;
        this.action = action;
        this.eventType = action;
        this.module = "unknown";
        this.retentionExpiry = Instant.now().plus(DEFAULT_RETENTION_DAYS, java.time.temporal.ChronoUnit.DAYS);
    }

    /**
     * A comprehensive constructor for creating a detailed audit event.
     *
     * @param organizationId the associated organization ID
     * @param actorId the ID of the actor
     * @param eventType the type of event
     * @param resourceType the type of the affected resource
     * @param resourceId the ID of the affected resource, which can be a non-UUID string
     * @param action the action performed
     * @param ipAddress the source IP address
     * @param userAgent the user agent of the client
       */
    public AuditEvent(
            final UUID organizationId,
            final UUID actorId,
            final String eventType,
            final String resourceType,
            final String resourceId,
            final String action,
            final String ipAddress,
            final String userAgent) {
        this.organizationId = organizationId;
        this.actorId = actorId;
        this.eventType = eventType;
        this.action = action;
        this.resourceType = resourceType;
        this.module = "unknown";
        this.retentionExpiry = Instant.now().plus(DEFAULT_RETENTION_DAYS, java.time.temporal.ChronoUnit.DAYS);

        // Store resourceId in details if not a UUID
        if (resourceId != null && !resourceId.trim().isEmpty()) {
            var detailsMap = new java.util.HashMap<String, Object>();
            detailsMap.put("resourceId", resourceId);
            this.details = Map.copyOf(detailsMap);
        }
        if (ipAddress != null) {
            this.ipAddress = ipAddress;
        }
        if (userAgent != null) {
            this.userAgent = userAgent;
        }
    }

  // Factory methods for common audit events

    /**
     * Creates an audit event for a user login.
     *
     * @param userId the ID of the user logging in
     * @param ipAddress the source IP address
     * @param correlationId the correlation ID for the request
     * @return a new {@link AuditEvent} for the login action
       */
    public static AuditEvent userLogin(final UUID userId, final String ipAddress, final String correlationId) {
        var event = new AuditEvent(userId, "user.login");
        event.eventType = "user.login";
        event.module = "auth";
        event.ipAddress = ipAddress;
        event.correlationId = correlationId;
        return event;
    }

    /**
     * Creates an audit event for a user logout.
     *
     * @param userId the ID of the user logging out
     * @param correlationId the correlation ID for the request
     * @return a new {@link AuditEvent} for the logout action
       */
    public static AuditEvent userLogout(final UUID userId, final String correlationId) {
        var event = new AuditEvent(userId, "user.logout");
        event.eventType = "user.logout";
        event.module = "auth";
        event.correlationId = correlationId;
        return event;
    }

    /**
     * Creates an audit event for the creation of a new organization.
     *
     * @param actorId the ID of the user creating the organization
     * @param organizationId the ID of the newly created organization
     * @param details additional details about the organization
     * @return a new {@link AuditEvent} for the organization creation
       */
    public static AuditEvent organizationCreated(
            final UUID actorId, final UUID organizationId, final Map<String, Object> details) {
        var event = new AuditEvent(actorId, "organization.created");
        event.eventType = "organization.created";
        event.module = "user";
        event.organizationId = organizationId;
        event.resourceType = "organization";
        var detailsMap = new java.util.HashMap<>(sanitizeDetails(details));
        detailsMap.put("resourceId", organizationId.toString());
        event.details = Map.copyOf(detailsMap);
        return event;
    }

    /**
     * Creates an audit event for the creation of a new subscription.
     *
     * @param actorId the ID of the user creating the subscription
     * @param organizationId the ID of the organization the subscription belongs to
     * @param subscriptionId the ID of the newly created subscription
     * @param details additional details about the subscription
     * @return a new {@link AuditEvent} for the subscription creation
       */
    public static AuditEvent subscriptionCreated(
            final UUID actorId,
            final UUID organizationId,
            final UUID subscriptionId,
            final Map<String, Object> details) {
        var event = new AuditEvent(actorId, "subscription.created");
        event.eventType = "subscription.created";
        event.module = "subscription";
        event.organizationId = organizationId;
        event.resourceType = "subscription";
        var detailsMap = new java.util.HashMap<>(sanitizeDetails(details));
        detailsMap.put("resourceId", subscriptionId.toString());
        event.details = Map.copyOf(detailsMap);
        return event;
    }

    /**
     * Creates an audit event for a processed payment.
     *
     * @param actorId the ID of the user associated with the payment
     * @param organizationId the ID of the organization the payment belongs to
     * @param paymentId the ID of the payment record
     * @param details additional details about the payment
     * @return a new {@link AuditEvent} for the payment processing
       */
    public static AuditEvent paymentProcessed(
            final UUID actorId, final UUID organizationId, final UUID paymentId, final Map<String, Object> details) {
        var event = new AuditEvent(actorId, "payment.processed");
        event.eventType = "payment.processed";
        event.module = "payment";
        event.organizationId = organizationId;
        event.resourceType = "payment";
        var detailsMap = new java.util.HashMap<>(sanitizeDetails(details));
        detailsMap.put("resourceId", paymentId.toString());
        event.details = Map.copyOf(detailsMap);
        return event;
    }

    /**
     * Creates an audit event for a data export action.
     *
     * @param actorId the ID of the user exporting data
     * @param organizationId the ID of the organization from which data is exported
     * @param exportType the type of data being exported (e.g., "csv", "pdf")
     * @param details additional details about the export
     * @return a new {@link AuditEvent} for the data export
       */
    public static AuditEvent dataExported(
            final UUID actorId, final UUID organizationId, final String exportType, final Map<String, Object> details) {
        var event = new AuditEvent(actorId, "data.exported");
        event.eventType = "data.exported";
        event.module = "audit";
        event.organizationId = organizationId;
        event.resourceType = "export";
        var detailsMap = new java.util.HashMap<>(sanitizeDetails(details));
        detailsMap.put("exportType", exportType);
        event.details = Map.copyOf(detailsMap);
        return event;
    }

    /**
     * Creates an audit event for a data deletion action.
     *
     * @param actorId the ID of the user deleting the data
     * @param organizationId the ID of the organization the data belongs to
     * @param resourceType the type of the deleted resource
     * @param resourceId the ID of the deleted resource
     * @param details additional details about the deletion
     * @return a new {@link AuditEvent} for the data deletion
       */
    public static AuditEvent dataDeleted(
            final UUID actorId,
            final UUID organizationId,
            final String resourceType,
            final UUID resourceId,
            final Map<String, Object> details) {
        var event = new AuditEvent(actorId, "data.deleted");
        event.eventType = "data.deleted";
        event.module = "audit";
        event.organizationId = organizationId;
        event.resourceType = resourceType;
        var detailsMap = new java.util.HashMap<>(sanitizeDetails(details));
        detailsMap.put("resourceId", resourceId.toString());
        event.details = Map.copyOf(detailsMap);
        return event;
    }

  // Builder methods for fluent API

    /**
     * Sets the organization ID for this event.
     *
     * @param organizationId the organization ID
     * @return this {@link AuditEvent} instance for chaining
       */
    public AuditEvent withOrganization(final UUID orgId) {
        this.organizationId = orgId;
        return this;
    }

    /**
     * Sets the resource context for this event.
     *
     * @param resourceType the type of the resource
     * @param resourceId the ID of the resource
     * @return this {@link AuditEvent} instance for chaining
       */
    public AuditEvent withResource(final String resType, final UUID resId) {
        this.resourceType = resType;
        var detailsMap = new java.util.HashMap<>(this.details);
        detailsMap.put("resourceId", resId.toString());
        this.details = Map.copyOf(detailsMap);
        return this;
    }

    /**
     * Sets the correlation ID for this event.
     *
     * @param correlationId the correlation ID
     * @return this {@link AuditEvent} instance for chaining
       */
    public AuditEvent withCorrelationId(final String corrId) {
        this.correlationId = corrId;
        return this;
    }

    /**
     * Sets the IP address for this event.
     *
     * @param ipAddress the source IP address
     * @return this {@link AuditEvent} instance for chaining
       */
    public AuditEvent withIpAddress(final String ip) {
        this.ipAddress = ip;
        return this;
    }

    /**
     * Sets the details map for this event, ensuring it is sanitized.
     *
     * @param details a map of additional details
     * @return this {@link AuditEvent} instance for chaining
       */
    public AuditEvent withDetails(final Map<String, Object> detailsMap) {
        this.details = sanitizeDetails(detailsMap);
        return this;
    }

    /**
     * Adds a single key-value pair to the details map, ensuring the value is sanitized.
     *
     * @param key the key for the detail entry
     * @param value the value for the detail entry
     * @return this {@link AuditEvent} instance for chaining
       */
    public AuditEvent addDetail(final String key, final Object value) {
        var updatedDetails = new java.util.HashMap<>(this.details);
        updatedDetails.put(key, sanitizeValue(value));
        this.details = Map.copyOf(updatedDetails);
        return this;
    }

    /**
     * Sanitizes a map of details by redacting or cleaning potential PII from its values.
     *
     * @param details the map to sanitize
     * @return a new map with sanitized values
       */
    private static Map<String, Object> sanitizeDetails(final Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }

        var sanitized = new java.util.HashMap<String, Object>();
        for (var entry : details.entrySet()) {
            sanitized.put(entry.getKey(), sanitizeValue(entry.getValue()));
        }
        return Map.copyOf(sanitized);
    }

    /**
     * Sanitizes a single object by checking for and redacting common PII patterns.
     *
     * @param value the value to sanitize
     * @return the sanitized value, or the original value if no PII is detected
       */
    private static Object sanitizeValue(final Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString();

        // Redact potential PII patterns
        if (isPotentialEmail(stringValue)) {
            return redactEmail(stringValue);
        }

        if (isPotentialCreditCard(stringValue)) {
            return "[REDACTED_CC]";
        }

        if (isPotentialSSN(stringValue)) {
            return "[REDACTED_SSN]";
        }

        // Truncate very long values
        if (stringValue.length() > MAX_STRING_LENGTH) {
            return stringValue.substring(0, STRING_TRUNCATE_LENGTH) + "...";
        }

        return value;
    }

    private static boolean isPotentialEmail(final String value) {
        return value.contains("@") && value.contains(".");
    }

    private static String redactEmail(final String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "[REDACTED_EMAIL]";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return "**" + domain;
        }

        return localPart.charAt(0)
                                + "*".repeat(localPart.length() - 2)
                        + localPart.charAt(localPart.length() - 1)
                        + domain;
    }

    private static boolean isPotentialCreditCard(final String value) {
        String digitsOnly = value.replaceAll("\\D", "");
        return digitsOnly.length() >= MIN_CREDIT_CARD_LENGTH && digitsOnly.length() <= MAX_CREDIT_CARD_LENGTH;
    }

    private static boolean isPotentialSSN(final String value) {
        return value.matches("\\d{3}-\\d{2}-\\d{4}") || value.matches("\\d{9}");
    }

  // Business methods

    /**
     * Checks if the event is a user-related action.
     *
     * @return {@code true} if the action starts with "user.", {@code false} otherwise
       */
    public boolean isUserAction() {
        return action.startsWith("user.");
    }

    /**
     * Checks if the event is an organization-related action.
     *
     * @return {@code true} if the action starts with "organization.", {@code false} otherwise
       */
    public boolean isOrganizationAction() {
        return action.startsWith("organization.");
    }

    /**
     * Checks if the event is a payment-related action.
     *
     * @return {@code true} if the action starts with "payment.", {@code false} otherwise
       */
    public boolean isPaymentAction() {
        return action.startsWith("payment.");
    }

    /**
     * Checks if the event is a data-related action.
     *
     * @return {@code true} if the action starts with "data.", {@code false} otherwise
       */
    public boolean isDataAction() {
        return action.startsWith("data.");
    }

    /**
     * Checks if the event has an associated organization.
     *
     * @return {@code true} if {@code organizationId} is not null, {@code false} otherwise
       */
    public boolean hasOrganizationContext() {
        return organizationId != null;
    }

    /**
     * Checks if the event has an associated resource.
     *
     * @return {@code true} if both {@code resourceType} and resourceId in details are not null
       */
    public boolean hasResourceContext() {
        return resourceType != null && details.containsKey("resourceId");
    }

  // Getters

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public String getModule() {
        return module;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return details.get("resourceId") != null ? details.get("resourceId").toString() : null;
    }

    public Boolean getSensitiveData() {
        return sensitiveData;
    }

    public Instant getRetentionExpiry() {
        return retentionExpiry;
    }

    public Map<String, Object> getDetails() {
        return Map.copyOf(details);
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

  // Setters for additional data

    /**
     * Adds raw request data to the details map.
     *
     * @param requestData the request data string
       */
    public void setRequestData(final String requestData) {
        var detailsMap = new java.util.HashMap<>(this.details);
        detailsMap.put("requestData", requestData);
        this.details = Map.copyOf(detailsMap);
    }

    /**
     * Adds raw response data to the details map.
     *
     * @param responseData the response data string
       */
    public void setResponseData(final String responseData) {
        var detailsMap = new java.util.HashMap<>(this.details);
        detailsMap.put("responseData", responseData);
        this.details = Map.copyOf(detailsMap);
    }

    /**
     * Adds arbitrary metadata to the details map.
     *
     * @param metadata the metadata string
       */
    public void setMetadata(final String metadata) {
        var detailsMap = new java.util.HashMap<>(this.details);
        detailsMap.put("metadata", metadata);
        this.details = Map.copyOf(detailsMap);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AuditEvent other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AuditEvent{"
                        + "id="
                + id
                        + ", actorId="
                + actorId
                        + ", organizationId="
                + organizationId
                        + ", action='"
                + action
                + '\''
                        + ", resourceType='"
                + resourceType
                + '\''
                        + ", createdAt="
                + createdAt
                +'}';
    }
}
