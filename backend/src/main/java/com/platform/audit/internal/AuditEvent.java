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
      @Index(name = "idx_audit_events_actor", columnList = "actor_id"),
      @Index(name = "idx_audit_events_org", columnList = "organization_id"),
      @Index(name = "idx_audit_events_correlation", columnList = "correlation_id"),
      @Index(name = "idx_audit_events_created", columnList = "created_at"),
      @Index(name = "idx_audit_events_action", columnList = "action")
    })
public class AuditEvent {

  /** The unique identifier for the audit event. */
  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The ID of the user or system principal that performed the action. */
  @Column(name = "actor_id")
  private UUID actorId;

  /** The ID of the organization this event is associated with. Can be null. */
  @Column(name = "organization_id")
  private UUID organizationId;

  /** A dot-separated string representing the action performed (e.g., "user.login"). */
  @NotBlank
  @Size(max = 100)
  @Column(name = "action", nullable = false, length = 100)
  private String action;

  /** The type of resource that was affected (e.g., "payment", "subscription"). */
  @Size(max = 100)
  @Column(name = "resource_type", length = 100)
  private String resourceType;

  /** The unique ID of the resource that was affected. */
  @Column(name = "resource_id")
  private UUID resourceId;

  /**
   * A flexible map of additional details about the event, stored as JSON. Sensitive information in
   * this map is automatically redacted.
   */
  @Column(name = "details")
  @Convert(converter = AuditDetailsConverter.class)
  private Map<String, Object> details = Map.of();

  /** A unique ID to correlate multiple events within a single request or transaction. */
  @Size(max = 100)
  @Column(name = "correlation_id", length = 100)
  private String correlationId;

  /** The IP address from which the action was initiated. */
  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  /** The timestamp when the event was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

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
  public AuditEvent(UUID actorId, String action) {
    this.actorId = actorId;
    this.action = action;
  }

  /**
   * A comprehensive constructor for creating a detailed audit event.
   *
   * @param organizationId the associated organization ID
   * @param actorId the ID of the actor
   * @param eventType deprecated, use {@code action}
   * @param resourceType the type of the affected resource
   * @param resourceId the ID of the affected resource, which can be a non-UUID string
   * @param action the action performed
   * @param ipAddress the source IP address
   * @param userAgent the user agent of the client
   */
  public AuditEvent(
      UUID organizationId,
      UUID actorId,
      String eventType,
      String resourceType,
      String resourceId,
      String action,
      String ipAddress,
      String userAgent) {
    this.organizationId = organizationId;
    this.actorId = actorId;
    this.action = action;
    this.resourceType = resourceType;
    // Parse resourceId as UUID if it's a valid UUID string
    if (resourceId != null && !resourceId.trim().isEmpty()) {
      try {
        this.resourceId = UUID.fromString(resourceId);
      } catch (IllegalArgumentException e) {
        // If not a valid UUID, store as part of details
        this.details = Map.of("resourceId", resourceId);
      }
    }
    if (ipAddress != null) {
      this.ipAddress = ipAddress;
    }
    if (userAgent != null) {
      var detailsMap = new java.util.HashMap<>(this.details);
      detailsMap.put("userAgent", userAgent);
      this.details = Map.copyOf(detailsMap);
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
  public static AuditEvent userLogin(UUID userId, String ipAddress, String correlationId) {
    var event = new AuditEvent(userId, "user.login");
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
  public static AuditEvent userLogout(UUID userId, String correlationId) {
    var event = new AuditEvent(userId, "user.logout");
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
      UUID actorId, UUID organizationId, Map<String, Object> details) {
    var event = new AuditEvent(actorId, "organization.created");
    event.organizationId = organizationId;
    event.resourceType = "organization";
    event.resourceId = organizationId;
    event.details = sanitizeDetails(details);
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
      UUID actorId, UUID organizationId, UUID subscriptionId, Map<String, Object> details) {
    var event = new AuditEvent(actorId, "subscription.created");
    event.organizationId = organizationId;
    event.resourceType = "subscription";
    event.resourceId = subscriptionId;
    event.details = sanitizeDetails(details);
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
      UUID actorId, UUID organizationId, UUID paymentId, Map<String, Object> details) {
    var event = new AuditEvent(actorId, "payment.processed");
    event.organizationId = organizationId;
    event.resourceType = "payment";
    event.resourceId = paymentId;
    event.details = sanitizeDetails(details);
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
      UUID actorId, UUID organizationId, String exportType, Map<String, Object> details) {
    var event = new AuditEvent(actorId, "data.exported");
    event.organizationId = organizationId;
    event.resourceType = "export";
    event.details = sanitizeDetails(details);
    event.addDetail("exportType", exportType);
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
      UUID actorId,
      UUID organizationId,
      String resourceType,
      UUID resourceId,
      Map<String, Object> details) {
    var event = new AuditEvent(actorId, "data.deleted");
    event.organizationId = organizationId;
    event.resourceType = resourceType;
    event.resourceId = resourceId;
    event.details = sanitizeDetails(details);
    return event;
  }

  // Builder methods for fluent API

  /**
   * Sets the organization ID for this event.
   *
   * @param organizationId the organization ID
   * @return this {@link AuditEvent} instance for chaining
   */
  public AuditEvent withOrganization(UUID organizationId) {
    this.organizationId = organizationId;
    return this;
  }

  /**
   * Sets the resource context for this event.
   *
   * @param resourceType the type of the resource
   * @param resourceId the ID of the resource
   * @return this {@link AuditEvent} instance for chaining
   */
  public AuditEvent withResource(String resourceType, UUID resourceId) {
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    return this;
  }

  /**
   * Sets the correlation ID for this event.
   *
   * @param correlationId the correlation ID
   * @return this {@link AuditEvent} instance for chaining
   */
  public AuditEvent withCorrelationId(String correlationId) {
    this.correlationId = correlationId;
    return this;
  }

  /**
   * Sets the IP address for this event.
   *
   * @param ipAddress the source IP address
   * @return this {@link AuditEvent} instance for chaining
   */
  public AuditEvent withIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }

  /**
   * Sets the details map for this event, ensuring it is sanitized.
   *
   * @param details a map of additional details
   * @return this {@link AuditEvent} instance for chaining
   */
  public AuditEvent withDetails(Map<String, Object> details) {
    this.details = sanitizeDetails(details);
    return this;
  }

  /**
   * Adds a single key-value pair to the details map, ensuring the value is sanitized.
   *
   * @param key the key for the detail entry
   * @param value the value for the detail entry
   * @return this {@link AuditEvent} instance for chaining
   */
  public AuditEvent addDetail(String key, Object value) {
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
  private static Map<String, Object> sanitizeDetails(Map<String, Object> details) {
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
  private static Object sanitizeValue(Object value) {
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
    if (stringValue.length() > 1000) {
      return stringValue.substring(0, 997) + "...";
    }

    return value;
  }

  private static boolean isPotentialEmail(String value) {
    return value.contains("@") && value.contains(".");
  }

  private static String redactEmail(String email) {
    int atIndex = email.indexOf('@');
    if (atIndex <= 0) return "[REDACTED_EMAIL]";

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

  private static boolean isPotentialCreditCard(String value) {
    String digitsOnly = value.replaceAll("\\D", "");
    return digitsOnly.length() >= 13 && digitsOnly.length() <= 19;
  }

  private static boolean isPotentialSSN(String value) {
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
   * @return {@code true} if both {@code resourceType} and {@code resourceId} are not null
   */
  public boolean hasResourceContext() {
    return resourceType != null && resourceId != null;
  }

  // Getters

  public UUID getId() {
    return id;
  }

  public UUID getActorId() {
    return actorId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public UUID getResourceId() {
    return resourceId;
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
  public void setRequestData(String requestData) {
    var detailsMap = new java.util.HashMap<>(this.details);
    detailsMap.put("requestData", requestData);
    this.details = Map.copyOf(detailsMap);
  }

  /**
   * Adds raw response data to the details map.
   *
   * @param responseData the response data string
   */
  public void setResponseData(String responseData) {
    var detailsMap = new java.util.HashMap<>(this.details);
    detailsMap.put("responseData", responseData);
    this.details = Map.copyOf(detailsMap);
  }

  /**
   * Adds arbitrary metadata to the details map.
   *
   * @param metadata the metadata string
   */
  public void setMetadata(String metadata) {
    var detailsMap = new java.util.HashMap<>(this.details);
    detailsMap.put("metadata", metadata);
    this.details = Map.copyOf(detailsMap);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof AuditEvent other)) return false;
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
        + '}';
  }
}
