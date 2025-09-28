package com.platform.subscription.api;

import com.platform.subscription.api.SubscriptionDto.InvoiceResponse;
import com.platform.subscription.api.SubscriptionDto.SubscriptionResponse;
import com.platform.subscription.api.SubscriptionDto.SubscriptionStatisticsResponse;
import com.stripe.exception.StripeException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines the contract for managing the lifecycle of subscriptions.
 *
 * <p>This service interface provides a high-level API for creating, modifying, canceling, and
 * retrieving subscription information. It serves as an abstraction layer that decouples the API
 * controllers from the internal implementation details of subscription management and billing provider
 * interactions.
 * </p>
 */
public interface SubscriptionManagementService {

  /**
   * Creates a new subscription for an organization.
   *
   * @param organizationId The ID of the organization to subscribe.
   * @param planId The ID of the subscription plan to use.
   * @param paymentMethodId The ID of the payment method to attach to the subscription (optional).
   * @param trialEligible Whether the subscription is eligible for a trial period.
   * @return A {@link SubscriptionResponse} representing the newly created subscription.
   * @throws StripeException if there is an error communicating with the payment provider.
   */
  SubscriptionResponse createSubscription(
      UUID organizationId, UUID planId, String paymentMethodId, boolean trialEligible)
      throws StripeException;

  /**
   * Finds the active subscription for a given organization.
   *
   * @param organizationId The ID of the organization.
   * @return An {@link Optional} containing the {@link SubscriptionResponse} if found, otherwise an
   *     empty {@link Optional}.
   */
  Optional<SubscriptionResponse> findByOrganizationId(UUID organizationId);

  /**
   * Changes the plan for an existing subscription.
   *
   * @param organizationId The ID of the organization.
   * @param newPlanId The ID of the new plan to switch to.
   * @param prorationBehavior Whether to apply proration for the plan change.
   * @return The updated {@link SubscriptionResponse}.
   * @throws StripeException if there is an error communicating with the payment provider.
   */
  SubscriptionResponse changeSubscriptionPlan(
      UUID organizationId, UUID newPlanId, boolean prorationBehavior) throws StripeException;

  /**
   * Cancels an active subscription.
   *
   * @param organizationId The ID of the organization whose subscription is to be canceled.
   * @param immediate If {@code true}, the subscription is canceled immediately. Otherwise, it is
   *     canceled at the end of the current billing period.
   * @param cancelAt A specific future timestamp to schedule the cancellation for (optional).
   * @return The updated {@link SubscriptionResponse} reflecting the cancellation.
   * @throws StripeException if there is an error communicating with the payment provider.
   */
  SubscriptionResponse cancelSubscription(
      UUID organizationId, boolean immediate, java.time.Instant cancelAt) throws StripeException;

  /**
   * Reactivates a previously canceled subscription.
   *
   * @param organizationId The ID of the organization whose subscription is to be reactivated.
   * @return The reactivated {@link SubscriptionResponse}.
   * @throws StripeException if there is an error communicating with the payment provider.
   */
  SubscriptionResponse reactivateSubscription(UUID organizationId) throws StripeException;

  /**
   * Retrieves a list of all invoices for a given organization.
   *
   * @param organizationId The ID of the organization.
   * @return A list of {@link InvoiceResponse} objects.
   */
  List<InvoiceResponse> getOrganizationInvoices(UUID organizationId);

  /**
   * Retrieves billing and usage statistics for an organization's subscription.
   *
   * @param organizationId The ID of the organization.
   * @return A {@link SubscriptionStatisticsResponse} object containing the statistics.
   */
  SubscriptionStatisticsResponse getSubscriptionStatistics(UUID organizationId);
}