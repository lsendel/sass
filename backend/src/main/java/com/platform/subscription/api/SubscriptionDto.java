package com.platform.subscription.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A container for all Data Transfer Objects (DTOs) related to subscription operations.
 *
 * <p>This class uses nested records and enums to define the data structures for API requests and
 * responses. This approach encapsulates subscription-related data contracts, preventing the direct
 * exposure of internal domain entities.
 * </p>
 */
public class SubscriptionDto {

  /** Defines the possible statuses of a subscription at the API layer. */
  public enum SubscriptionStatus {
    ACTIVE,
    INACTIVE,
    CANCELED,
    PAST_DUE,
    TRIALING
  }

  /** Defines the possible statuses of an invoice at the API layer. */
  public enum InvoiceStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELLED
  }

  /**
   * Represents the data for a subscription sent in API responses.
   *
   * @param id The unique internal identifier for the subscription.
   * @param organizationId The ID of the organization that owns the subscription.
   * @param planId The ID of the associated subscription plan.
   * @param stripeSubscriptionId The corresponding ID from the Stripe API.
   * @param status The current status of the subscription.
   * @param currentPeriodStart The start date of the current billing period.
   * @param currentPeriodEnd The end date of the current billing period.
   * @param trialStart The start date of the trial period, if applicable.
   * @param trialEnd The end date of the trial period, if applicable.
   * @param cancelAt The timestamp when the subscription is scheduled to be canceled.
   * @param canceledAt The timestamp when the subscription was actually canceled.
   * @param createdAt The timestamp when the subscription was created.
   * @param updatedAt The timestamp when the subscription was last updated.
   */
  public record SubscriptionResponse(
      UUID id,
      UUID organizationId,
      UUID planId,
      String stripeSubscriptionId,
      SubscriptionStatus status,
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      Instant trialStart,
      Instant trialEnd,
      Instant cancelAt,
      Instant canceledAt,
      Instant createdAt,
      Instant updatedAt) {}

  /**
   * Represents the data for a billing invoice sent in API responses.
   *
   * @param id The unique internal identifier for the invoice.
   * @param organizationId The ID of the organization the invoice belongs to.
   * @param subscriptionId The ID of the associated subscription.
   * @param stripeInvoiceId The corresponding ID from the Stripe API.
   * @param amount The total amount of the invoice.
   * @param currency The currency of the invoice.
   * @param status The current status of the invoice.
   * @param dueDate The date the invoice is due.
   * @param paidAt The timestamp when the invoice was paid.
   * @param createdAt The timestamp when the invoice was created.
   */
  public record InvoiceResponse(
      UUID id,
      UUID organizationId,
      UUID subscriptionId,
      String stripeInvoiceId,
      BigDecimal amount,
      String currency,
      InvoiceStatus status,
      Instant dueDate,
      Instant paidAt,
      Instant createdAt) {}

  /**
   * Represents a summary of subscription-related statistics.
   *
   * @param status The current status of the subscription.
   * @param totalInvoices The total number of invoices generated for the subscription.
   * @param totalAmount The total amount paid over the lifetime of the subscription.
   * @param averageAmount The average amount per invoice.
   * @param lastPaymentDate The date of the last successful payment.
   */
  public record SubscriptionStatisticsResponse(
      SubscriptionStatus status,
      long totalInvoices,
      BigDecimal totalAmount,
      BigDecimal averageAmount,
      Instant lastPaymentDate) {}

  /**
   * Represents the data required to create a new subscription.
   *
   * @param organizationId The ID of the organization to subscribe.
   * @param planId The ID of the chosen subscription plan.
   * @param paymentMethodId The ID of the payment method to use (optional).
   * @param trialEligible Whether the organization is eligible for a trial period.
   */
  public record CreateSubscriptionRequest(
      @NotNull UUID organizationId,
      @NotNull UUID planId,
      String paymentMethodId,
      Boolean trialEligible) {}

  /**
   * Represents the data required to change a subscription's plan.
   *
   * @param organizationId The ID of the organization.
   * @param newPlanId The ID of the new plan to switch to.
   * @param prorationBehavior Whether to apply proration for the plan change.
   */
  public record ChangePlanRequest(
      @NotNull UUID organizationId, @NotNull UUID newPlanId, Boolean prorationBehavior) {}

  /**
   * Represents the data required to cancel a subscription.
   *
   * @param organizationId The ID of the organization.
   * @param immediate Whether to cancel the subscription immediately or at the end of the current
   *     billing period.
   * @param cancelAt A specific future timestamp to schedule the cancellation for.
   */
  public record CancelSubscriptionRequest(
      @NotNull UUID organizationId, Boolean immediate, Instant cancelAt) {}

  /**
   * Represents the data required to reactivate a canceled subscription.
   *
   * @param organizationId The ID of the organization whose subscription is to be reactivated.
   */
  public record ReactivateSubscriptionRequest(@NotNull UUID organizationId) {}
}