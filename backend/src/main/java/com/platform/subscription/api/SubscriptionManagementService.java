package com.platform.subscription.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.platform.subscription.api.SubscriptionDto.SubscriptionResponse;
import com.platform.subscription.api.SubscriptionDto.InvoiceResponse;
import com.platform.subscription.api.SubscriptionDto.SubscriptionStatisticsResponse;
import com.stripe.exception.StripeException;

/**
 * Service interface for subscription management operations.
 * This interface provides the API layer with access to subscription functionality
 * without depending on internal implementation details.
 */
public interface SubscriptionManagementService {

  /**
   * Creates a new subscription for an organization.
   *
   * @param organizationId the organization ID
   * @param planId the plan ID
   * @param paymentMethodId the payment method ID (optional)
   * @param trialEligible whether the subscription is eligible for trial
   * @return the created subscription
   * @throws StripeException if Stripe operation fails
   */
  SubscriptionResponse createSubscription(
      UUID organizationId,
      UUID planId,
      String paymentMethodId,
      boolean trialEligible) throws StripeException;

  /**
   * Finds a subscription by organization ID.
   *
   * @param organizationId the organization ID
   * @return the subscription if found, empty otherwise
   */
  Optional<SubscriptionResponse> findByOrganizationId(UUID organizationId);

  /**
   * Changes the plan of an existing subscription.
   *
   * @param organizationId the organization ID
   * @param newPlanId the new plan ID
   * @param prorationBehavior whether to prorate the change
   * @return the updated subscription
   * @throws StripeException if Stripe operation fails
   */
  SubscriptionResponse changeSubscriptionPlan(
      UUID organizationId,
      UUID newPlanId,
      boolean prorationBehavior) throws StripeException;

  /**
   * Cancels a subscription.
   *
   * @param organizationId the organization ID
   * @param immediate whether to cancel immediately
   * @param cancelAt when to cancel (if not immediate)
   * @return the canceled subscription
   * @throws StripeException if Stripe operation fails
   */
  SubscriptionResponse cancelSubscription(
      UUID organizationId,
      boolean immediate,
      java.time.Instant cancelAt) throws StripeException;

  /**
   * Reactivates a canceled subscription.
   *
   * @param organizationId the organization ID
   * @return the reactivated subscription
   * @throws StripeException if Stripe operation fails
   */
  SubscriptionResponse reactivateSubscription(UUID organizationId) throws StripeException;

  /**
   * Gets invoices for an organization.
   *
   * @param organizationId the organization ID
   * @return list of invoices
   */
  List<InvoiceResponse> getOrganizationInvoices(UUID organizationId);

  /**
   * Gets subscription statistics for an organization.
   *
   * @param organizationId the organization ID
   * @return subscription statistics
   */
  SubscriptionStatisticsResponse getSubscriptionStatistics(UUID organizationId);



}