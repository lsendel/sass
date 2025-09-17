package com.platform.payment.internal;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.platform.shared.types.Money;
import com.platform.user.internal.MapToJsonConverter;

/** Payment entity for tracking payment transactions with Stripe integration. */
@Entity
@Table(
    name = "payments",
    indexes = {
      @Index(name = "idx_payments_user", columnList = "user_id"),
      @Index(name = "idx_payments_invoice", columnList = "invoice_id"),
      @Index(name = "idx_payments_stripe", columnList = "stripe_payment_intent_id", unique = true),
      @Index(name = "idx_payments_status", columnList = "status"),
      @Index(name = "idx_payments_created", columnList = "created_at")
    })
public class Payment {

  /** Payment status enumeration based on Stripe PaymentIntent states */
  public enum Status {
    REQUIRES_PAYMENT_METHOD("requires_payment_method"),
    REQUIRES_CONFIRMATION("requires_confirmation"),
    REQUIRES_ACTION("requires_action"),
    PROCESSING("processing"),
    SUCCEEDED("succeeded"),
    REQUIRES_CAPTURE("requires_capture"),
    CANCELED("canceled"),
    FAILED("failed"),
    PENDING("pending");

    private final String value;

    Status(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static Status fromString(String status) {
      for (Status s : values()) {
        if (s.value.equals(status)) {
          return s;
        }
      }
      throw new IllegalArgumentException("Invalid payment status: " + status);
    }

    public boolean isSuccessful() {
      return this == SUCCEEDED;
    }

    public boolean isFailed() {
      return this == CANCELED || this == FAILED;
    }

    public boolean isPending() {
      return this == REQUIRES_PAYMENT_METHOD
          || this == REQUIRES_CONFIRMATION
          || this == REQUIRES_ACTION
          || this == PROCESSING
          || this == REQUIRES_CAPTURE
          || this == PENDING;
    }

    public boolean requiresAction() {
      return this == REQUIRES_PAYMENT_METHOD
          || this == REQUIRES_CONFIRMATION
          || this == REQUIRES_ACTION
          || this == PENDING;
    }
  }

  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "organization_id")
  private UUID organizationId;

  @Column(name = "invoice_id")
  private UUID invoiceId;

  @NotBlank
  @Column(name = "stripe_payment_intent_id", nullable = false, unique = true)
  private String stripePaymentIntentId;

  @Embedded private Money amount;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private Status status;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "metadata")
  private Map<String, String> metadata;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private Long version;

  // Constructors
  protected Payment() {
    // JPA constructor
  }

  public Payment(UUID userId, String stripePaymentIntentId, Money amount, Status status) {
    this.userId = userId;
    this.stripePaymentIntentId = stripePaymentIntentId;
    this.amount = amount;
    this.status = status;
  }

  public Payment(
      UUID organizationId,
      String stripePaymentIntentId,
      Money amount,
      String currency,
      String description) {
    this.organizationId = organizationId;
    this.stripePaymentIntentId = stripePaymentIntentId;
    this.amount = amount;
    this.status = Status.REQUIRES_PAYMENT_METHOD;
    // description can be stored in metadata
    this.metadata =
        description != null ? Map.of("description", description) : new java.util.HashMap<>();
  }

  // Factory methods
  public static Payment createForInvoice(
      UUID userId, UUID invoiceId, String stripePaymentIntentId, Money amount) {
    var payment =
        new Payment(userId, stripePaymentIntentId, amount, Status.REQUIRES_PAYMENT_METHOD);
    payment.invoiceId = invoiceId;
    return payment;
  }

  public static Payment createStandalone(UUID userId, String stripePaymentIntentId, Money amount) {
    return new Payment(userId, stripePaymentIntentId, amount, Status.REQUIRES_PAYMENT_METHOD);
  }

  // Business methods
  public boolean isSuccessful() {
    return status.isSuccessful();
  }

  public boolean isFailed() {
    return status.isFailed();
  }

  public boolean isPending() {
    return status.isPending();
  }

  public boolean requiresAction() {
    return status.requiresAction();
  }

  public boolean isCompleted() {
    return isSuccessful() || isFailed();
  }

  public void updateStatus(Status newStatus) {
    this.status = newStatus;
  }

  public void markAsSucceeded() {
    this.status = Status.SUCCEEDED;
  }

  public void markAsFailed() {
    this.status = Status.FAILED;
  }

  public void markAsCanceled() {
    this.status = Status.CANCELED;
  }

  public void requiresConfirmation() {
    this.status = Status.REQUIRES_CONFIRMATION;
  }

  public void markAsRequiresAction() {
    this.status = Status.REQUIRES_ACTION;
  }

  public void markAsProcessing() {
    this.status = Status.PROCESSING;
  }

  public void linkToInvoice(UUID invoiceId) {
    this.invoiceId = invoiceId;
  }

  public void unlinkFromInvoice() {
    this.invoiceId = null;
  }

  // Helper methods
  public boolean isLinkedToInvoice() {
    return invoiceId != null;
  }

  public String getAmountFormatted() {
    return amount != null ? amount.formatWithSymbol() : "N/A";
  }

  public int getAmountInCents() {
    return amount != null ? amount.getAmountInCentsAsInt() : 0;
  }

  // Getters
  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getInvoiceId() {
    return invoiceId;
  }

  public String getStripePaymentIntentId() {
    return stripePaymentIntentId;
  }

  public Money getAmount() {
    return amount;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Long getVersion() {
    return version;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public String getCurrency() {
    return amount != null ? amount.getCurrency() : "USD";
  }

  public String getDescription() {
    // Extract description from metadata if stored
    if (metadata != null) {
      return metadata.get("description");
    }
    return null;
  }

  public UUID getSubscriptionId() {
    // Payment may not be directly linked to subscription, return null
    return null;
  }

  // Additional methods required by service layer
  public void updateMetadata(java.util.Map<String, String> metadata) {
    this.metadata =
        metadata != null ? new java.util.HashMap<>(metadata) : new java.util.HashMap<>();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Payment other)) return false;
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Payment{"
        + "id="
        + id
        + ", userId="
        + userId
        + ", amount="
        + amount
        + ", status="
        + status
        + ", stripePaymentIntentId='"
        + stripePaymentIntentId
        + '\''
        + ", createdAt="
        + createdAt
        + '}';
  }
}
