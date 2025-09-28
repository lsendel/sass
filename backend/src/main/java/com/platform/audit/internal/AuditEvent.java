package com.platform.audit.internal;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * AuditEvent entity for compliance and security audit logging with GDPR support. Uses table
 * partitioning for performance and retention management.
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

  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "actor_id")
  private UUID actorId;

  @Column(name = "organization_id")
  private UUID organizationId;

  @NotBlank
  @Size(max = 100)
  @Column(name = "action", nullable = false, length = 100)
  private String action;

  @Size(max = 100)
  @Column(name = "resource_type", length = 100)
  private String resourceType;

  @Column(name = "resource_id")
  private UUID resourceId;

  @Column(name = "details")
  @Convert(converter = AuditDetailsConverter.class)
  private Map<String, Object> details = Map.of();

  @Size(max = 100)
  @Column(name = "correlation_id", length = 100)
  private String correlationId;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // Constructors
  public AuditEvent() {
    // JPA constructor - made public for tests
  }

  public AuditEvent(UUID actorId, String action) {
    this.actorId = actorId;
    this.action = action;
  }

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
  public static AuditEvent.Builder builder() {
    return new AuditEvent.Builder();
  }

  public static AuditEvent userLogin(UUID userId, String ipAddress, String correlationId) {
    var event = new AuditEvent(userId, "user.login");
    event.ipAddress = ipAddress;
    event.correlationId = correlationId;
    return event;
  }

  public static AuditEvent userLogout(UUID userId, String correlationId) {
    var event = new AuditEvent(userId, "user.logout");
    event.correlationId = correlationId;
    return event;
  }

  public static AuditEvent organizationCreated(
      UUID actorId, UUID organizationId, Map<String, Object> details) {
    var event = new AuditEvent(actorId, "organization.created");
    event.organizationId = organizationId;
    event.resourceType = "organization";
    event.resourceId = organizationId;
    event.details = sanitizeDetails(details);
    return event;
  }

  public static AuditEvent subscriptionCreated(
      UUID actorId, UUID organizationId, UUID subscriptionId, Map<String, Object> details) {
    var event = new AuditEvent(actorId, "subscription.created");
    event.organizationId = organizationId;
    event.resourceType = "subscription";
    event.resourceId = subscriptionId;
    event.details = sanitizeDetails(details);
    return event;
  }

  public static AuditEvent paymentProcessed(
      UUID actorId, UUID organizationId, UUID paymentId, Map<String, Object> details) {
    var event = new AuditEvent(actorId, "payment.processed");
    event.organizationId = organizationId;
    event.resourceType = "payment";
    event.resourceId = paymentId;
    event.details = sanitizeDetails(details);
    return event;
  }

  public static AuditEvent dataExported(
      UUID actorId, UUID organizationId, String exportType, Map<String, Object> details) {
    var event = new AuditEvent(actorId, "data.exported");
    event.organizationId = organizationId;
    event.resourceType = "export";
    event.details = sanitizeDetails(details);
    event.addDetail("exportType", exportType);
    return event;
  }

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
  public AuditEvent withOrganization(UUID organizationId) {
    this.organizationId = organizationId;
    return this;
  }

  public AuditEvent withResource(String resourceType, UUID resourceId) {
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    return this;
  }

  public AuditEvent withCorrelationId(String correlationId) {
    this.correlationId = correlationId;
    return this;
  }

  public AuditEvent withIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }

  public AuditEvent withDetails(Map<String, Object> details) {
    this.details = sanitizeDetails(details);
    return this;
  }

  public AuditEvent addDetail(String key, Object value) {
    var updatedDetails = new java.util.HashMap<>(this.details);
    updatedDetails.put(key, sanitizeValue(value));
    this.details = Map.copyOf(updatedDetails);
    return this;
  }

  // GDPR compliance methods
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
  public boolean isUserAction() {
    return action.startsWith("user.");
  }

  public boolean isOrganizationAction() {
    return action.startsWith("organization.");
  }

  public boolean isPaymentAction() {
    return action.startsWith("payment.");
  }

  public boolean isDataAction() {
    return action.startsWith("data.");
  }

  public boolean hasOrganizationContext() {
    return organizationId != null;
  }

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

  public String getUserAgent() {
    return (String) details.get("userAgent");
  }

  public String getRequestData() {
    return (String) details.get("requestData");
  }

  public String getResponseData() {
    return (String) details.get("responseData");
  }

  public String getMetadata() {
    return (String) details.get("metadata");
  }

  // Setters for additional data
  public void setId(UUID id) {
    this.id = id;
  }

  public void setOrganizationId(UUID organizationId) {
    this.organizationId = organizationId;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setActorId(UUID actorId) {
    this.actorId = actorId;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public void setResourceId(String resourceId) {
    if (resourceId != null && !resourceId.trim().isEmpty()) {
      try {
        this.resourceId = UUID.fromString(resourceId);
      } catch (IllegalArgumentException e) {
        // If not a valid UUID, store as part of details
        var detailsMap = new java.util.HashMap<>(this.details);
        detailsMap.put("resourceId", resourceId);
        this.details = Map.copyOf(detailsMap);
      }
    }
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public void setDetails(String details) {
    var detailsMap = new java.util.HashMap<>(this.details);
    detailsMap.put("details", details);
    this.details = Map.copyOf(detailsMap);
  }

  public void setRequestData(String requestData) {
    var detailsMap = new java.util.HashMap<>(this.details);
    detailsMap.put("requestData", requestData);
    this.details = Map.copyOf(detailsMap);
  }

  public void setResponseData(String responseData) {
    var detailsMap = new java.util.HashMap<>(this.details);
    detailsMap.put("responseData", responseData);
    this.details = Map.copyOf(detailsMap);
  }

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

  /**
   * Builder class for AuditEvent
   */
  public static class Builder {
    private UUID actorId;
    private String action;
    private UUID organizationId;
    private String resourceType;
    private UUID resourceId;
    private Map<String, Object> details = new java.util.HashMap<>();
    private String correlationId;
    private String ipAddress;

    public Builder actorId(UUID actorId) {
      this.actorId = actorId;
      return this;
    }

    public Builder action(String action) {
      this.action = action;
      return this;
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder resourceType(String resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public Builder resourceId(UUID resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public Builder details(Map<String, Object> details) {
      this.details = new java.util.HashMap<>(details);
      return this;
    }

    public Builder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder ipAddress(String ipAddress) {
      this.ipAddress = ipAddress;
      return this;
    }

    public Builder id(UUID id) {
      // AuditEvent ID is auto-generated, so this is a no-op for compatibility
      return this;
    }

    public AuditEvent build() {
      var event = new AuditEvent(actorId, action);
      event.organizationId = organizationId;
      event.resourceType = resourceType;
      event.resourceId = resourceId;
      event.details = sanitizeDetails(details);
      event.correlationId = correlationId;
      event.ipAddress = ipAddress;
      return event;
    }
  }
}
