package com.platform.payment.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.platform.shared.types.Money;

/** Invoice entity for billing documents with Stripe integration. */
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

  /** Invoice status enumeration based on Stripe invoice states */
  public enum Status {
    DRAFT("draft"),
    OPEN("open"),
    PAID("paid"),
    UNCOLLECTIBLE("uncollectible"),
    VOID("void");

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
      throw new IllegalArgumentException("Invalid invoice status: " + status);
    }

    public boolean isPaid() {
      return this == PAID;
    }

    public boolean isUnpaid() {
      return this == OPEN;
    }

    public boolean isClosed() {
      return this == PAID || this == UNCOLLECTIBLE || this == VOID;
    }

    public boolean canBePaid() {
      return this == OPEN;
    }
  }

  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @Column(name = "subscription_id", nullable = false)
  private UUID subscriptionId;

  @Column(name = "organization_id")
  private UUID organizationId;

  @NotBlank
  @Column(name = "stripe_invoice_id", nullable = false, unique = true)
  private String stripeInvoiceId;

  @Column(name = "invoice_number")
  private String invoiceNumber;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "amount", column = @Column(name = "subtotal_amount")),
    @AttributeOverride(name = "currency", column = @Column(name = "subtotal_currency"))
  })
  private Money subtotalAmount;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "amount", column = @Column(name = "tax_amount")),
    @AttributeOverride(name = "currency", column = @Column(name = "tax_currency"))
  })
  private Money taxAmount;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
    @AttributeOverride(name = "currency", column = @Column(name = "total_currency"))
  })
  private Money totalAmount;

  @Embedded private Money amount;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private Status status;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Version private Long version;

  // Constructors
  protected Invoice() {
    // JPA constructor
  }

  public Invoice(UUID subscriptionId, String stripeInvoiceId, Money amount, Status status) {
    this.subscriptionId = subscriptionId;
    this.stripeInvoiceId = stripeInvoiceId;
    this.amount = amount;
    this.status = status;
  }

  // Factory methods
  public static Invoice createDraft(UUID subscriptionId, String stripeInvoiceId, Money amount) {
    return new Invoice(subscriptionId, stripeInvoiceId, amount, Status.DRAFT);
  }

  public static Invoice createOpen(
      UUID subscriptionId, String stripeInvoiceId, Money amount, LocalDate dueDate) {
    var invoice = new Invoice(subscriptionId, stripeInvoiceId, amount, Status.OPEN);
    invoice.dueDate = dueDate;
    return invoice;
  }

  // Business methods
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

  public boolean isOverdue() {
    return isUnpaid() && dueDate != null && dueDate.isBefore(LocalDate.now());
  }

  public boolean isDueSoon() {
    if (dueDate == null || !isUnpaid()) {
      return false;
    }
    LocalDate today = LocalDate.now();
    return dueDate.isAfter(today) && dueDate.isBefore(today.plusDays(7));
  }

  public void updateStatus(Status newStatus) {
    this.status = newStatus;
    if (newStatus == Status.PAID && paidAt == null) {
      this.paidAt = Instant.now();
    }
  }

  public void markAsPaid() {
    this.status = Status.PAID;
    this.paidAt = Instant.now();
  }

  public void markAsUncollectible() {
    this.status = Status.UNCOLLECTIBLE;
  }

  public void markAsVoid() {
    this.status = Status.VOID;
  }

  public void finalize(LocalDate dueDate) {
    if (status != Status.DRAFT) {
      throw new IllegalStateException("Only draft invoices can be finalized");
    }
    this.status = Status.OPEN;
    this.dueDate = dueDate;
  }

  public void updateDueDate(LocalDate newDueDate) {
    if (isClosed()) {
      throw new IllegalStateException("Cannot update due date for closed invoice");
    }
    this.dueDate = newDueDate;
  }

  // Helper methods
  public int getDaysUntilDue() {
    if (dueDate == null) {
      return 0;
    }
    LocalDate today = LocalDate.now();
    return dueDate.isAfter(today) ? (int) today.datesUntil(dueDate).count() : 0;
  }

  public int getDaysOverdue() {
    if (dueDate == null || !isOverdue()) {
      return 0;
    }
    return (int) dueDate.datesUntil(LocalDate.now()).count();
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

  // Add method required by SubscriptionService
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
