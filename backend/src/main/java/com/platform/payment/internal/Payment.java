package com.platform.payment.internal;

import com.platform.shared.types.Money;
import com.platform.user.internal.MapToJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents a payment transaction within the system.
 *
 * <p>This entity tracks the state and details of a single payment, linking it to a user,
 * organization, and optionally an invoice. It is designed to integrate with Stripe, using the
 * {@code stripePaymentIntentId} to correlate with Stripe's PaymentIntent object.
 *
 * @see Invoice
 * @see PaymentService
 */
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

  /**
   * Enumerates the possible statuses of a payment, mirroring the states of a Stripe PaymentIntent.
   */
  public enum Status {
    /** The payment requires a payment method to be attached. */
    REQUIRES_PAYMENT_METHOD("requires_payment_method"),
    /** The payment requires confirmation from the user. */
    REQUIRES_CONFIRMATION("requires_confirmation"),
    /** The payment requires additional action from the user (e.g., 3D Secure). */
    REQUIRES_ACTION("requires_action"),
    /** The payment is being processed. */
    PROCESSING("processing"),
    /** The payment was successful. */
    SUCCEEDED("succeeded"),
    /** The payment has been authorized but not yet captured. */
    REQUIRES_CAPTURE("requires_capture"),
    /** The payment was canceled. */
    CANCELED("canceled"),
    /** The payment failed. */
    FAILED("failed"),
    /** The payment is in a pending state. */
    PENDING("pending");

    private final String value;

    Status(String value) {
      this.value = value;
    }

    /**
     * Gets the string value of the status, which corresponds to the status value in Stripe.
     *
     * @return the string value of the status
     */
    public String getValue() {
      return value;
    }

    /**
     * Creates a {@link Status} enum from a string value.
     *
     * @param status the string value of the status
     * @return the corresponding {@link Status} enum
     * @throws IllegalArgumentException if the status string is invalid
     */
    public static Status fromString(String status) {
      for (Status s : values()) {
        if (s.value.equals(status)) {
          return s;
        }
      }
      throw new IllegalArgumentException("Invalid payment status: " + status);
    }

    /**
     * Checks if the payment was successful.
     *
     * @return {@code true} if the status is SUCCEEDED, {@code false} otherwise
     */
    public boolean isSuccessful() {
      return this == SUCCEEDED;
    }

    /**
     * Checks if the payment has failed or was canceled.
     *
     * @return {@code true} if the status is CANCELED or FAILED, {@code false} otherwise
     */
    public boolean isFailed() {
      return this == CANCELED || this == FAILED;
    }

    /**
     * Checks if the payment is in a pending state.
     *
     * @return {@code true} if the payment is not successful or failed, {@code false} otherwise
     */
    public boolean isPending() {
      return this == REQUIRES_PAYMENT_METHOD
          || this == REQUIRES_CONFIRMATION
          || this == REQUIRES_ACTION
          || this == PROCESSING
          || this == REQUIRES_CAPTURE
          || this == PENDING;
    }

    /**
     * Checks if the payment requires action from the user.
     *
     * @return {@code true} if the payment requires user action, {@code false} otherwise
     */
    public boolean requiresAction() {
      return this == REQUIRES_PAYMENT_METHOD
          || this == REQUIRES_CONFIRMATION
          || this == REQUIRES_ACTION
          || this == PENDING;
    }
  }

  /** The unique identifier for the payment. */
  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The ID of the user who initiated the payment. */
  @Column(name = "user_id")
  private UUID userId;

  /** The ID of the organization associated with the payment. */
  @Column(name = "organization_id")
  private UUID organizationId;

  /** The ID of the invoice this payment is for, if applicable. */
  @Column(name = "invoice_id")
  private UUID invoiceId;

  /** The ID of the payment intent in Stripe. */
  @NotBlank
  @Column(name = "stripe_payment_intent_id", nullable = false, unique = true)
  private String stripePaymentIntentId;

  /** The monetary amount of the payment. */
  @Embedded private Money amount;

  /** The current status of the payment. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private Status status;

  /** A map of custom metadata associated with the payment. */
  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "metadata")
  private Map<String, String> metadata;

  /** The timestamp of when the payment was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** The timestamp of the last update to the payment. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** The version number for optimistic locking. */
  @Version private Long version;

  /**
   * Protected no-argument constructor for JPA.
   *
   * <p>This constructor is required by JPA and should not be used directly.
   */
  protected Payment() {
    // JPA constructor
  }

  /**
   * Constructs a new Payment.
   *
   * @param userId the ID of the user initiating the payment
   * @param stripePaymentIntentId the corresponding ID from Stripe
   * @param amount the monetary value of the payment
   * @param status the initial status of the payment
   */
  public Payment(UUID userId, String stripePaymentIntentId, Money amount, Status status) {
    this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
    this.stripePaymentIntentId = stripePaymentIntentId;
    this.amount = amount;
    this.status = status;
  }

  /**
   * Constructs a new Payment for an organization.
   *
   * @param organizationId the ID of the organization
   * @param stripePaymentIntentId the Stripe PaymentIntent ID
   * @param amount the payment amount
   * @param currency the payment currency
   * @param description a description of the payment
   */
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
    this.metadata =
        description != null ? Map.of("description", description) : new java.util.HashMap<>();
  }

  /**
   * Factory method to create a new payment linked to an invoice.
   *
   * @param userId the ID of the user
   * @param invoiceId the ID of the invoice
   * @param stripePaymentIntentId the Stripe PaymentIntent ID
   * @param amount the payment amount
   * @return a new {@link Payment} instance
   */
  public static Payment createForInvoice(
      UUID userId, UUID invoiceId, String stripePaymentIntentId, Money amount) {
    var payment = new Payment(userId, stripePaymentIntentId, amount, Status.REQUIRES_PAYMENT_METHOD);
    payment.invoiceId = invoiceId;
    return payment;
  }

  /**
   * Factory method to create a new standalone payment not linked to an invoice.
   *
   * @param userId the ID of the user
   * @param stripePaymentIntentId the Stripe PaymentIntent ID
   * @param amount the payment amount
   * @return a new {@link Payment} instance
   */
  public static Payment createStandalone(UUID userId, String stripePaymentIntentId, Money amount) {
    return new Payment(userId, stripePaymentIntentId, amount, Status.REQUIRES_PAYMENT_METHOD);
  }

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

  /**
   * Checks if the payment has reached a terminal state (succeeded or failed).
   *
   * @return {@code true} if the payment is completed, {@code false} otherwise
   */
  public boolean isCompleted() {
    return isSuccessful() || isFailed();
  }

  /**
   * Updates the status of the payment.
   *
   * @param newStatus the new status to set
   */
  public void updateStatus(Status newStatus) {
    this.status = newStatus;
  }

  /** Marks the payment as succeeded. */
  public void markAsSucceeded() {
    this.status = Status.SUCCEEDED;
  }

  /** Marks the payment as failed. */
  public void markAsFailed() {
    this.status = Status.FAILED;
  }

  /** Marks the payment as canceled. */
  public void markAsCanceled() {
    this.status = Status.CANCELED;
  }

  /** Sets the payment status to requires confirmation. */
  public void requiresConfirmation() {
    this.status = Status.REQUIRES_CONFIRMATION;
  }

  /** Sets the payment status to requires action. */
  public void markAsRequiresAction() {
    this.status = Status.REQUIRES_ACTION;
  }

  /** Sets the payment status to processing. */
  public void markAsProcessing() {
    this.status = Status.PROCESSING;
  }

  /**
   * Links this payment to an invoice.
   *
   * @param invoiceId the ID of the invoice to link
   */
  public void linkToInvoice(UUID invoiceId) {
    this.invoiceId = invoiceId;
  }

  /** Removes the link to an invoice. */
  public void unlinkFromInvoice() {
    this.invoiceId = null;
  }

  /**
   * Checks if this payment is linked to an invoice.
   *
   * @return {@code true} if an invoice ID is present, {@code false} otherwise
   */
  public boolean isLinkedToInvoice() {
    return invoiceId != null;
  }

  /**
   * Returns the payment amount formatted as a currency string.
   *
   * @return the formatted amount string, or "N/A"
   */
  public String getAmountFormatted() {
    return amount != null ? amount.formatWithSymbol() : "N/A";
  }

  /**
   * Returns the payment amount in the smallest currency unit (e.g., cents).
   *
   * @return the amount in cents
   */
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

  /**
   * Assigns a user to this payment.
   *
   * @param userId the ID of the user to assign
   */
  public void assignUser(UUID userId) {
    this.userId = userId;
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
    if (metadata != null) {
      return metadata.get("description");
    }
    return null;
  }

  public UUID getSubscriptionId() {
    return null;
  }

  /**
   * Updates the metadata for this payment.
   *
   * @param metadata the new metadata map
   */
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
