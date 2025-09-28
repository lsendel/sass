package com.platform.subscription.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents an organization's subscription to a plan.
 *
 * <p>This entity links an organization to a specific {@link Plan} and tracks the subscription's
 * lifecycle, including its status, billing period, and trial information. It is integrated with
 * Stripe via the {@code stripeSubscriptionId}.
 *
 * <p>The status of a subscription is managed by the {@link SubscriptionService} and is updated
 * based on events from Stripe.
 *
 * @see Plan
 * @see SubscriptionService
 */
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

  /**
   * Enumerates the possible statuses of a subscription, mirroring Stripe's subscription states.
   *
   * <p>Each status represents a specific state in the subscription lifecycle.
   */
  public enum Status {
    /** The subscription has not been fully set up and requires further action. */
    INCOMPLETE("incomplete"),
    /** The subscription was incomplete and has now expired. */
    INCOMPLETE_EXPIRED("incomplete_expired"),
    /** The subscription is in a trial period. */
    TRIALING("trialing"),
    /** The subscription is active and paid. */
    ACTIVE("active"),
    /** Payment for the subscription is past due. */
    PAST_DUE("past_due"),
    /** The subscription has been canceled. */
    CANCELED("canceled"),
    /** The subscription is unpaid. */
    UNPAID("unpaid");

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
      throw new IllegalArgumentException("Invalid subscription status: " + status);
    }

    /**
     * Checks if the subscription is in an active state (either active or trialing).
     *
     * @return {@code true} if the subscription is active, {@code false} otherwise
     */
    public boolean isActive() {
      return this == ACTIVE || this == TRIALING;
    }

    /**
     * Checks if the subscription requires payment.
     *
     * @return {@code true} if the subscription requires payment, {@code false} otherwise
     */
    public boolean requiresPayment() {
      return this == INCOMPLETE || this == PAST_DUE || this == UNPAID;
    }

    /**
     * Checks if the subscription has ended (either canceled or expired).
     *
     * @return {@code true} if the subscription has ended, {@code false} otherwise
     */
    public boolean isEnded() {
      return this == CANCELED || this == INCOMPLETE_EXPIRED;
    }
  }

  /** The unique identifier for the subscription. */
  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The ID of the organization that owns this subscription. */
  @NotNull
  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  /** The ID of the plan to which the organization is subscribed. */
  @NotNull
  @Column(name = "plan_id", nullable = false)
  private UUID planId;

  /** The ID of the subscription in Stripe. */
  @Column(name = "stripe_subscription_id", unique = true)
  private String stripeSubscriptionId;

  /** The current status of the subscription. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private Status status;

  /** The start date of the current billing period. */
  @Column(name = "current_period_start")
  private LocalDate currentPeriodStart;

  /** The end date of the current billing period. */
  @Column(name = "current_period_end")
  private LocalDate currentPeriodEnd;

  /** The end date of the trial period, if applicable. */
  @Column(name = "trial_end")
  private LocalDate trialEnd;

  /** The time at which the subscription is scheduled to be canceled. */
  @Column(name = "cancel_at")
  private Instant cancelAt;

  /** The timestamp of when the subscription was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** The timestamp of the last update to the subscription. */
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
  protected Subscription() {
    // JPA constructor
  }

  /**
   * Constructs a new Subscription.
   *
   * @param organizationId the ID of the organization
   * @param planId the ID of the subscribed plan
   * @param status the initial status of the subscription
   */
  public Subscription(UUID organizationId, UUID planId, Status status) {
    this.organizationId = organizationId;
    this.planId = planId;
    this.status = status;
  }

  /**
   * Factory method to create a new subscription in a trial state.
   *
   * @param organizationId the ID of the organization
   * @param planId the ID of the plan
   * @param trialEnd the date when the trial period ends
   * @return a new {@link Subscription} instance with TRIALING status
   */
  public static Subscription createTrial(UUID organizationId, UUID planId, LocalDate trialEnd) {
    var subscription = new Subscription(organizationId, planId, Status.TRIALING);
    subscription.trialEnd = trialEnd;
    return subscription;
  }

  /**
   * Factory method to create a new, active subscription.
   *
   * @param organizationId the ID of the organization
   * @param planId the ID of the plan
   * @param periodStart the start date of the current billing period
   * @param periodEnd the end date of the current billing period
   * @return a new {@link Subscription} instance with ACTIVE status
   */
  public static Subscription createActive(
      UUID organizationId, UUID planId, LocalDate periodStart, LocalDate periodEnd) {
    var subscription = new Subscription(organizationId, planId, Status.ACTIVE);
    subscription.currentPeriodStart = periodStart;
    subscription.currentPeriodEnd = periodEnd;
    return subscription;
  }

  /**
   * Checks if the subscription is active.
   *
   * @return {@code true} if the subscription is active, {@code false} otherwise
   */
  public boolean isActive() {
    return status.isActive();
  }

  /**
   * Checks if the subscription requires payment.
   *
   * @return {@code true} if the subscription requires payment, {@code false} otherwise
   */
  public boolean requiresPayment() {
    return status.requiresPayment();
  }

  /**
   * Checks if the subscription has ended.
   *
   * @return {@code true} if the subscription has ended, {@code false} otherwise
   */
  public boolean isEnded() {
    return status.isEnded();
  }

  /**
   * Checks if the subscription is currently in a trial period.
   *
   * @return {@code true} if the subscription is in trial, {@code false} otherwise
   */
  public boolean isInTrial() {
    return status == Status.TRIALING && trialEnd != null && trialEnd.isAfter(LocalDate.now());
  }

  /**
   * Checks if the trial period for the subscription has expired.
   *
   * @return {@code true} if the trial has expired, {@code false} otherwise
   */
  public boolean isTrialExpired() {
    return status == Status.TRIALING && trialEnd != null && trialEnd.isBefore(LocalDate.now());
  }

  /**
   * Checks if the current billing period has expired.
   *
   * @return {@code true} if the current period has expired, {@code false} otherwise
   */
  public boolean isPeriodExpired() {
    return currentPeriodEnd != null && currentPeriodEnd.isBefore(LocalDate.now());
  }

  /**
   * Updates the status of the subscription.
   *
   * @param newStatus the new status
   */
  public void updateStatus(Status newStatus) {
    this.status = newStatus;
  }

  /**
   * Updates the current billing period of the subscription.
   *
   * @param periodStart the start date of the new period
   * @param periodEnd the end date of the new period
   */
  public void updatePeriod(LocalDate periodStart, LocalDate periodEnd) {
    this.currentPeriodStart = periodStart;
    this.currentPeriodEnd = periodEnd;
  }

  /** Cancels the subscription, setting its status to CANCELED. */
  public void cancel() {
    this.status = Status.CANCELED;
  }

  /** Marks the subscription as past due. */
  public void markPastDue() {
    this.status = Status.PAST_DUE;
  }

  /** Marks the subscription as unpaid. */
  public void markUnpaid() {
    this.status = Status.UNPAID;
  }

  /** Activates the subscription if it was previously in a recoverable state (e.g., unpaid). */
  public void activate() {
    if (status == Status.INCOMPLETE || status == Status.PAST_DUE || status == Status.UNPAID) {
      this.status = Status.ACTIVE;
    }
  }

  /**
   * Changes the plan associated with this subscription.
   *
   * @param newPlanId the ID of the new plan
   */
  public void changePlan(UUID newPlanId) {
    this.planId = newPlanId;
  }

  /**
   * Updates the Stripe subscription ID for this subscription.
   *
   * @param stripeSubscriptionId the new ID from Stripe
   */
  public void updateStripeSubscriptionId(String stripeSubscriptionId) {
    this.stripeSubscriptionId = stripeSubscriptionId;
  }

  /**
   * Calculates the number of days remaining in the current billing period.
   *
   * @return the number of days remaining, or 0 if not applicable
   */
  public int getDaysRemainingInPeriod() {
    if (currentPeriodEnd == null) {
      return 0;
    }
    LocalDate today = LocalDate.now();
    return currentPeriodEnd.isAfter(today) ? (int) today.datesUntil(currentPeriodEnd).count() : 0;
  }

  /**
   * Calculates the number of days remaining in the trial period.
   *
   * @return the number of trial days remaining, or 0 if not in trial
   */
  public int getDaysRemainingInTrial() {
    if (trialEnd == null || status != Status.TRIALING) {
      return 0;
    }
    LocalDate today = LocalDate.now();
    return trialEnd.isAfter(today) ? (int) today.datesUntil(trialEnd).count() : 0;
  }

  /**
   * Checks if the subscription is active and will renew within 7 days.
   *
   * @return {@code true} if the subscription is renewing soon, {@code false} otherwise
   */
  public boolean willRenewSoon() {
    return isActive() && getDaysRemainingInPeriod() <= 7;
  }

  // Getters

  /**
   * Gets the unique identifier for the subscription.
   *
   * @return the ID of the subscription
   */
  public UUID getId() {
    return id;
  }

  /**
   * Gets the ID of the organization that owns this subscription.
   *
   * @return the organization ID
   */
  public UUID getOrganizationId() {
    return organizationId;
  }

  /**
   * Gets the ID of the plan to which the organization is subscribed.
   *
   * @return the plan ID
   */
  public UUID getPlanId() {
    return planId;
  }

  /**
   * Gets the ID of the subscription in Stripe.
   *
   * @return the Stripe subscription ID
   */
  public String getStripeSubscriptionId() {
    return stripeSubscriptionId;
  }

  /**
   * Gets the current status of the subscription.
   *
   * @return the subscription status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Gets the start date of the current billing period.
   *
   * @return the start date of the current period
   */
  public LocalDate getCurrentPeriodStart() {
    return currentPeriodStart;
  }

  /**
   * Gets the end date of the current billing period.
   *
   * @return the end date of the current period
   */
  public LocalDate getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  /**
   * Gets the end date of the trial period, if applicable.
   *
   * @return the trial end date
   */
  public LocalDate getTrialEnd() {
    return trialEnd;
  }

  /**
   * Gets the timestamp of when the subscription was created.
   *
   * @return the creation timestamp
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Gets the timestamp of the last update to the subscription.
   *
   * @return the last update timestamp
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Gets the version number for optimistic locking.
   *
   * @return the version number
   */
  public Long getVersion() {
    return version;
  }

  /**
   * Gets the time at which the subscription is scheduled to be canceled.
   *
   * @return the scheduled cancellation time
   */
  public Instant getCancelAt() {
    return cancelAt;
  }

  /**
   * Updates the subscription's state from a Stripe event.
   *
   * @param status the new status
   * @param currentPeriodStart the new period start time
   * @param currentPeriodEnd the new period end time
   * @param trialEnd the new trial end time
   * @param cancelAt the new scheduled cancellation time
   */
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

  /**
   * Schedules a subscription for cancellation at a specific time.
   *
   * @param cancelAt the time when the subscription should be canceled
   */
  public void scheduleCancellation(Instant cancelAt) {
    this.cancelAt = cancelAt;
    this.status = Status.ACTIVE; // Keep active until cancellation date
  }

  /** Reactivates a canceled subscription. */
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
