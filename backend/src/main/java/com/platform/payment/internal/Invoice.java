package com.platform.payment.internal;

import com.platform.shared.types.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
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
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents a billing invoice, typically associated with a subscription.
 *
 * <p>This entity stores details about an invoice, including its corresponding Stripe ID, amounts,
 * status, and relevant dates. It is designed to integrate with Stripe's invoicing system and track
 * the lifecycle of each billing event.
 *
 * @see com.platform.subscription.internal.Subscription
 * @see Payment
 */
@Entity
@Table(
    name = "invoices",
    indexes = {
      @Index(name = "idx_invoices_subscription", columnList = "subscription_id"),
      @Index(name = "idx_invoices_stripe", columnList = "stripe_invoice_id", unique = true),
      @Index(name = "idx_invoices_status", columnList = "status"),
      @Index(name = "idx_invoices_due_date", columnList = "due_date")
    })
public class Invoice {

  /**
   * Enumerates the possible statuses of an invoice, mirroring Stripe's invoice states.
   */
  public enum Status {
    /** The invoice has been created but is not yet finalized. */
    DRAFT("draft"),
    /** The invoice is open and awaiting payment. */
    OPEN("open"),
    /** The invoice has been successfully paid. */
    PAID("paid"),
    /** The invoice is unlikely to be paid and has been marked as uncollectible. */
    UNCOLLECTIBLE("uncollectible"),
    /** The invoice has been voided and is no longer payable. */
    VOID("void");

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
      throw new IllegalArgumentException("Invalid invoice status: " + status);
    }

    /**
     * Checks if the invoice has been paid.
     *
     * @return {@code true} if the status is PAID, {@code false} otherwise
     */
    public boolean isPaid() {
      return this == PAID;
    }

    /**
     * Checks if the invoice is currently unpaid.
     *
     * @return {@code true} if the status is OPEN, {@code false} otherwise
     */
    public boolean isUnpaid() {
      return this == OPEN;
    }

    /**
     * Checks if the invoice is in a closed state (paid, uncollectible, or void).
     *
     * @return {@code true} if the invoice is closed, {@code false} otherwise
     */
    public boolean isClosed() {
      return this == PAID || this == UNCOLLECTIBLE || this == VOID;
    }

    /**
     * Checks if the invoice can be paid.
     *
     * @return {@code true} if the status is OPEN, {@code false} otherwise
     */
    public boolean canBePaid() {
      return this == OPEN;
    }
  }

  /** The unique identifier for the invoice. */
  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The ID of the subscription this invoice belongs to. */
  @NotNull
  @Column(name = "subscription_id", nullable = false)
  private UUID subscriptionId;

  /** The ID of the organization this invoice belongs to. */
  @Column(name = "organization_id")
  private UUID organizationId;

  /** The ID of the invoice in Stripe. */
  @NotBlank
  @Column(name = "stripe_invoice_id", nullable = false, unique = true)
  private String stripeInvoiceId;

  /** The user-friendly number of the invoice. */
  @Column(name = "invoice_number")
  private String invoiceNumber;

  /** The subtotal amount of the invoice, before taxes. */
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "amount", column = @Column(name = "subtotal_amount")),
    @AttributeOverride(name = "currency", column = @Column(name = "subtotal_currency"))
  })
  private Money subtotalAmount;

  /** The tax amount of the invoice. */
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "amount", column = @Column(name = "tax_amount")),
    @AttributeOverride(name = "currency", column = @Column(name = "tax_currency"))
  })
  private Money taxAmount;

  /** The total amount of the invoice, including taxes. */
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
    @AttributeOverride(name = "currency", column = @Column(name = "total_currency"))
  })
  private Money totalAmount;

  /** The amount of the invoice. */
  @Embedded private Money amount;

  /** The current status of the invoice. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private Status status;

  /** The date when the invoice is due. */
  @Column(name = "due_date")
  private LocalDate dueDate;

  /** The timestamp of when the invoice was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** The timestamp of when the invoice was paid. */
  @Column(name = "paid_at")
  private Instant paidAt;

  /** The version number for optimistic locking. */
  @Version private Long version;

  /**
   * Protected no-argument constructor for JPA.
   *
   * <p>This constructor is required by JPA and should not be used directly.
   */
  protected Invoice() {
    // JPA constructor
  }

  /**
   * Constructs a new Invoice.
   *
   * @param subscriptionId the ID of the associated subscription
   * @param stripeInvoiceId the corresponding invoice ID from Stripe
   * @param amount the total amount of the invoice
   * @param status the initial status of the invoice
   */
  public Invoice(UUID subscriptionId, String stripeInvoiceId, Money amount, Status status) {
    this.subscriptionId = subscriptionId;
    this.stripeInvoiceId = stripeInvoiceId;
    this.amount = amount;
    this.status = status;
  }

  /**
   * Factory method to create a new invoice in the DRAFT state.
   *
   * @param subscriptionId the subscription ID
   * @param stripeInvoiceId the Stripe invoice ID
   * @param amount the invoice amount
   * @return a new {@link Invoice} instance with DRAFT status
   */
  public static Invoice createDraft(UUID subscriptionId, String stripeInvoiceId, Money amount) {
    return new Invoice(subscriptionId, stripeInvoiceId, amount, Status.DRAFT);
  }

  /**
   * Factory method to create a new invoice in the OPEN state.
   *
   * @param subscriptionId the subscription ID
   * @param stripeInvoiceId the Stripe invoice ID
   * @param amount the invoice amount
   * @param dueDate the date the invoice is due
   * @return a new {@link Invoice} instance with OPEN status
   */
  public static Invoice createOpen(
      UUID subscriptionId, String stripeInvoiceId, Money amount, LocalDate dueDate) {
    var invoice = new Invoice(subscriptionId, stripeInvoiceId, amount, Status.OPEN);
    invoice.dueDate = dueDate;
    return invoice;
  }

  public boolean isPaid() {
    return status.isPaid();
  }

  public boolean isUnpaid() {
    return status.isUnpaid();
  }

  public boolean isClosed() {
    return status.isClosed();
  }

  public boolean canBePaid() {
    return status.canBePaid();
  }

  /**
   * Checks if the invoice is overdue.
   *
   * @return {@code true} if the invoice is unpaid and the due date is in the past
   */
  public boolean isOverdue() {
    return isUnpaid() && dueDate != null && dueDate.isBefore(LocalDate.now());
  }

  /**
   * Checks if the invoice is due within the next 7 days.
   *
   * @return {@code true} if the invoice is unpaid and due within 7 days
   */
  public boolean isDueSoon() {
    if (dueDate == null || !isUnpaid()) {
      return false;
    }
    LocalDate today = LocalDate.now();
    return dueDate.isAfter(today) && dueDate.isBefore(today.plusDays(7));
  }

  /**
   * Updates the status of the invoice. If the new status is PAID, the paidAt timestamp is set.
   *
   * @param newStatus the new status to set
   */
  public void updateStatus(Status newStatus) {
    this.status = newStatus;
    if (newStatus == Status.PAID && paidAt == null) {
      this.paidAt = Instant.now();
    }
  }

  /** Marks the invoice as paid and sets the paid-at timestamp. */
  public void markAsPaid() {
    this.status = Status.PAID;
    this.paidAt = Instant.now();
  }

  /** Marks the invoice as uncollectible. */
  public void markAsUncollectible() {
    this.status = Status.UNCOLLECTIBLE;
  }

  /** Marks the invoice as void. */
  public void markAsVoid() {
    this.status = Status.VOID;
  }

  /**
   * Finalizes a draft invoice, moving it to the OPEN state.
   *
   * @param dueDate the due date for the finalized invoice
   * @throws IllegalStateException if the invoice is not in DRAFT status
   */
  public void finalize(LocalDate dueDate) {
    if (status != Status.DRAFT) {
      throw new IllegalStateException("Only draft invoices can be finalized");
    }
    this.status = Status.OPEN;
    this.dueDate = dueDate;
  }

  /**
   * Updates the due date of the invoice.
   *
   * @param newDueDate the new due date
   * @throws IllegalStateException if the invoice is already closed
   */
  public void updateDueDate(LocalDate newDueDate) {
    if (isClosed()) {
      throw new IllegalStateException("Cannot update due date for closed invoice");
    }
    this.dueDate = newDueDate;
  }

  /**
   * Calculates the number of days until the invoice is due.
   *
   * @return the number of days until due, or 0 if not applicable
   */
  public int getDaysUntilDue() {
    if (dueDate == null) {
      return 0;
    }
    LocalDate today = LocalDate.now();
    return dueDate.isAfter(today) ? (int) today.datesUntil(dueDate).count() : 0;
  }

  /**
   * Calculates the number of days the invoice is overdue.
   *
   * @return the number of days overdue, or 0 if not overdue
   */
  public int getDaysOverdue() {
    if (dueDate == null || !isOverdue()) {
      return 0;
    }
    return (int) dueDate.datesUntil(LocalDate.now()).count();
  }

  /**
   * Returns the invoice amount formatted as a currency string with a symbol.
   *
   * @return the formatted amount string, or "N/A" if the amount is null
   */
  public String getAmountFormatted() {
    return amount != null ? amount.formatWithSymbol() : "N/A";
  }

  /**
   * Returns the invoice amount in the smallest currency unit (e.g., cents).
   *
   * @return the amount in cents, or 0 if the amount is null
   */
  public int getAmountInCents() {
    return amount != null ? amount.getAmountInCentsAsInt() : 0;
  }

  // Getters
  public UUID getId() {
    return id;
  }

  public UUID getSubscriptionId() {
    return subscriptionId;
  }

  public String getStripeInvoiceId() {
    return stripeInvoiceId;
  }

  public Money getAmount() {
    return amount;
  }

  public Status getStatus() {
    return status;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public Long getVersion() {
    return version;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  public Money getSubtotalAmount() {
    return subtotalAmount;
  }

  public Money getTaxAmount() {
    return taxAmount;
  }

  public Money getTotalAmount() {
    return totalAmount;
  }

  public String getCurrency() {
    return totalAmount != null ? totalAmount.getCurrency() : "USD";
  }

  /** Marks the invoice status as payment failed (uncollectible). */
  public void markAsPaymentFailed() {
    this.status = Status.UNCOLLECTIBLE;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Invoice other)) return false;
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Invoice{"
        + "id="
        + id
        + ", subscriptionId="
        + subscriptionId
        + ", amount="
        + amount
        + ", status="
        + status
        + ", dueDate="
        + dueDate
        + ", createdAt="
        + createdAt
        + '}';
  }
}
