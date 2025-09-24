package com.platform.subscription.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Data transfer objects for subscription-related operations.
 * These DTOs prevent direct access to internal entities from controllers.
 */
public class SubscriptionDto {

  /**
   * Subscription status enumeration for API layer.
   */
  public enum SubscriptionStatus {
    ACTIVE, INACTIVE, CANCELED, PAST_DUE, TRIALING
  }

  /**
   * Invoice status enumeration for API layer.
   */
  public enum InvoiceStatus {
    PENDING, PAID, FAILED, CANCELLED
  }

  /**
   * Response DTO for subscription information.
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
      Instant updatedAt) {
  }

  /**
   * Response DTO for invoice information.
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
      Instant createdAt) {
  }

  /**
   * Response DTO for subscription statistics.
   */
  public record SubscriptionStatisticsResponse(
      SubscriptionStatus status,
      long totalInvoices,
      BigDecimal totalAmount,
      BigDecimal averageAmount,
      Instant lastPaymentDate) {
  }

  /**
   * Request DTO for creating a subscription.
   */
  public record CreateSubscriptionRequest(
      @NotNull UUID organizationId,
      @NotNull UUID planId,
      String paymentMethodId,
      Boolean trialEligible) {
  }

  /**
   * Request DTO for changing subscription plan.
   */
  public record ChangePlanRequest(
      @NotNull UUID organizationId,
      @NotNull UUID newPlanId,
      Boolean prorationBehavior) {
  }

  /**
   * Request DTO for canceling subscription.
   */
  public record CancelSubscriptionRequest(
      @NotNull UUID organizationId,
      Boolean immediate,
      Instant cancelAt) {
  }

  /**
   * Request DTO for reactivating subscription.
   */
  public record ReactivateSubscriptionRequest(
      @NotNull UUID organizationId) {
  }
}