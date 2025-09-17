package com.platform.subscription.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/** Subscription entity linking organizations to plans with Stripe integration. */
@Entity
@Table(
    name = "subscriptions",
    indexes = {
      @Index(name = "idx_subscriptions_org", columnList = "organization_id"),
      @Index(name = "idx_subscriptions_status", columnList = "status"),
      @Index(
          name = "idx_subscriptions_stripe",
          columnList = "stripe_subscription_id",
          unique = true),
      @Index(name = "idx_subscriptions_period", columnList = "current_period_end")
    })
public class Subscription {

  /** Subscription status enumeration based on Stripe subscription states */
  public enum Status {
    INCOMPLETE("incomplete"),
    INCOMPLETE_EXPIRED("incomplete_expired"),
    TRIALING("trialing"),
    ACTIVE("active"),
    PAST_DUE("past_due"),
    CANCELED("canceled"),
    UNPAID("unpaid");

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
      throw new IllegalArgumentException("Invalid subscription status: " + status);
    }

    public boolean isActive() {
      return this == ACTIVE || this == TRIALING;
    }

    public boolean requiresPayment() {
      return this == INCOMPLETE || this == PAST_DUE || this == UNPAID;
    }

    public boolean isEnded() {
      return this == CANCELED || this == INCOMPLETE_EXPIRED;
    }
  }

  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @NotNull
  @Column(name = "plan_id", nullable = false)
  private UUID planId;

  @Column(name = "stripe_subscription_id", unique = true)
  private String stripeSubscriptionId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private Status status;

  @Column(name = "current_period_start")
  private LocalDate currentPeriodStart;

  @Column(name = "current_period_end")
  private LocalDate currentPeriodEnd;

  @Column(name = "trial_end")
  private LocalDate trialEnd;

  @Column(name = "cancel_at")
  private Instant cancelAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private Long version;

  // Constructors
  protected Subscription() {
    // JPA constructor
  }

  public Subscription(UUID organizationId, UUID planId, Status status) {
    this.organizationId = organizationId;
    this.planId = planId;
    this.status = status;
  }

  // Factory methods
  public static Subscription createTrial(UUID organizationId, UUID planId, LocalDate trialEnd) {
    var subscription = new Subscription(organizationId, planId, Status.TRIALING);
    subscription.trialEnd = trialEnd;
    return subscription;
  }

  public static Subscription createActive(
      UUID organizationId, UUID planId, LocalDate periodStart, LocalDate periodEnd) {
    var subscription = new Subscription(organizationId, planId, Status.ACTIVE);
    subscription.currentPeriodStart = periodStart;
    subscription.currentPeriodEnd = periodEnd;
    return subscription;
  }

  // Business methods
  public boolean isActive() {
    return status.isActive();
  }

  public boolean requiresPayment() {
    return status.requiresPayment();
  }

  public boolean isEnded() {
    return status.isEnded();
  }

  public boolean isInTrial() {
    return status == Status.TRIALING && trialEnd != null && trialEnd.isAfter(LocalDate.now());
  }

  public boolean isTrialExpired() {
    return status == Status.TRIALING && trialEnd != null && trialEnd.isBefore(LocalDate.now());
  }

  public boolean isPeriodExpired() {
    return currentPeriodEnd != null && currentPeriodEnd.isBefore(LocalDate.now());
  }

  public void updateStatus(Status newStatus) {
    this.status = newStatus;
  }

  public void updatePeriod(LocalDate periodStart, LocalDate periodEnd) {
    this.currentPeriodStart = periodStart;
    this.currentPeriodEnd = periodEnd;
  }

  public void cancel() {
    this.status = Status.CANCELED;
  }

  public void markPastDue() {
    this.status = Status.PAST_DUE;
  }

  public void markUnpaid() {
    this.status = Status.UNPAID;
  }

  public void activate() {
    if (status == Status.INCOMPLETE || status == Status.PAST_DUE || status == Status.UNPAID) {
      this.status = Status.ACTIVE;
    }
  }

  public void changePlan(UUID newPlanId) {
    this.planId = newPlanId;
  }

  public void updateStripeSubscriptionId(String stripeSubscriptionId) {
    this.stripeSubscriptionId = stripeSubscriptionId;
  }

  // Helper methods for period calculations
  public int getDaysRemainingInPeriod() {
    if (currentPeriodEnd == null) {
      return 0;
    }
    LocalDate today = LocalDate.now();
    return currentPeriodEnd.isAfter(today) ? (int) today.datesUntil(currentPeriodEnd).count() : 0;
  }

  public int getDaysRemainingInTrial() {
    if (trialEnd == null || status != Status.TRIALING) {
      return 0;
    }
    LocalDate today = LocalDate.now();
    return trialEnd.isAfter(today) ? (int) today.datesUntil(trialEnd).count() : 0;
  }

  public boolean willRenewSoon() {
    return isActive() && getDaysRemainingInPeriod() <= 7;
  }

  // Getters
  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getPlanId() {
    return planId;
  }

  public String getStripeSubscriptionId() {
    return stripeSubscriptionId;
  }

  public Status getStatus() {
    return status;
  }

  public LocalDate getCurrentPeriodStart() {
    return currentPeriodStart;
  }

  public LocalDate getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  public LocalDate getTrialEnd() {
    return trialEnd;
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

  public Instant getCancelAt() {
    return cancelAt;
  }

  // Method required by SubscriptionService
  public void updateFromStripe(
      Status status,
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      Instant trialEnd,
      Instant cancelAt) {
    this.status = status;
    this.currentPeriodStart =
        currentPeriodStart != null
            ? java.time.LocalDate.ofInstant(currentPeriodStart, java.time.ZoneOffset.UTC)
            : null;
    this.currentPeriodEnd =
        currentPeriodEnd != null
            ? java.time.LocalDate.ofInstant(currentPeriodEnd, java.time.ZoneOffset.UTC)
            : null;
    this.trialEnd =
        trialEnd != null ? java.time.LocalDate.ofInstant(trialEnd, java.time.ZoneOffset.UTC) : null;
    this.cancelAt = cancelAt;
  }

  public void scheduleCancellation(Instant cancelAt) {
    this.cancelAt = cancelAt;
    this.status = Status.ACTIVE; // Keep active until cancellation date
  }

  public void reactivate() {
    this.status = Status.ACTIVE;
    this.cancelAt = null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Subscription other)) return false;
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Subscription{"
        + "id="
        + id
        + ", organizationId="
        + organizationId
        + ", planId="
        + planId
        + ", status="
        + status
        + ", currentPeriodEnd="
        + currentPeriodEnd
        + ", createdAt="
        + createdAt
        + '}';
  }
}
