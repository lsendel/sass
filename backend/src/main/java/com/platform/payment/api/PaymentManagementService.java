package com.platform.payment.api;

import com.platform.payment.api.PaymentDto.BillingDetails;
import com.platform.payment.api.PaymentDto.ConfirmPaymentIntentRequest;
import com.platform.payment.api.PaymentDto.CreatePaymentIntentRequest;
import com.platform.payment.api.PaymentDto.PaymentIntentResponse;
import com.platform.payment.api.PaymentDto.PaymentMethodResponse;
import com.platform.payment.api.PaymentDto.PaymentResponse;
import com.platform.payment.api.PaymentDto.PaymentStatisticsResponse;
import com.stripe.exception.StripeException;
import java.util.List;
import java.util.UUID;

/**
 * Defines the contract for payment management operations.
 *
 * <p>This service interface provides a high-level API for handling all payment-related
 * functionality, such as creating payments and payment intents, managing payment methods, and
 * retrieving payment data. It serves as an abstraction layer between the controllers and the
 * internal implementation details of the payment module.
 * </p>
 */
public interface PaymentManagementService {

  /**
   * Creates a new payment.
   *
   * @param organizationId The ID of the organization making the payment.
   * @param amount The amount to be charged.
   * @param currency The currency of the payment.
   * @param paymentMethodId The ID of the payment method to use (optional).
   * @param description A description for the payment.
   * @param confirm Whether to confirm the payment immediately.
   * @return A {@link PaymentResponse} containing details of the created payment.
   * @throws StripeException if there is an error communicating with the Stripe API.
   */
  PaymentResponse createPayment(
      UUID organizationId,
      java.math.BigDecimal amount,
      String currency,
      String paymentMethodId,
      String description,
      boolean confirm)
      throws StripeException;

  /**
   * Creates a new Stripe PaymentIntent.
   *
   * @param request The request DTO containing the details for the payment intent.
   * @return A {@link PaymentIntentResponse} with details of the created intent.
   * @throws StripeException if there is an error communicating with the Stripe API.
   */
  PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) throws StripeException;

  /**
   * Confirms a Stripe PaymentIntent after client-side actions.
   *
   * @param organizationId The ID of the organization.
   * @param paymentIntentId The ID of the PaymentIntent to confirm.
   * @param request The request DTO containing confirmation details.
   * @return A {@link PaymentIntentResponse} with the updated status of the intent.
   * @throws StripeException if there is an error communicating with the Stripe API.
   */
  PaymentIntentResponse confirmPaymentIntent(
      UUID organizationId, String paymentIntentId, ConfirmPaymentIntentRequest request)
      throws StripeException;

  /**
   * Cancels a Stripe PaymentIntent.
   *
   * @param organizationId The ID of the organization.
   * @param paymentIntentId The ID of the PaymentIntent to cancel.
   * @return A {@link PaymentIntentResponse} with the updated status of the intent.
   * @throws StripeException if there is an error communicating with the Stripe API.
   */
  PaymentIntentResponse cancelPaymentIntent(UUID organizationId, String paymentIntentId)
      throws StripeException;

  /**
   * Confirms a payment.
   *
   * @param organizationId The ID of the organization.
   * @param paymentIntentId The ID of the payment intent to confirm.
   * @return A {@link PaymentResponse} for the confirmed payment.
   * @throws StripeException if there is an error communicating with the Stripe API.
   */
  PaymentResponse confirmPayment(UUID organizationId, String paymentIntentId) throws StripeException;

  /**
   * Cancels a payment.
   *
   * @param organizationId The ID of the organization.
   * @param paymentIntentId The ID of the payment intent to cancel.
   * @return A {@link PaymentResponse} for the canceled payment.
   * @throws StripeException if there is an error communicating with the Stripe API.
   */
  PaymentResponse cancelPayment(UUID organizationId, String paymentIntentId) throws StripeException;

  /**
   * Retrieves a list of all payments for an organization.
   *
   * @param organizationId The ID of the organization.
   * @return A list of {@link PaymentResponse} objects.
   */
  List<PaymentResponse> getOrganizationPayments(UUID organizationId);

  /**
   * Retrieves a list of payments for an organization, filtered by status.
   *
   * @param organizationId The ID of the organization.
   * @param status The payment status to filter by.
   * @return A list of matching {@link PaymentResponse} objects.
   */
  List<PaymentResponse> getOrganizationPaymentsByStatus(UUID organizationId, String status);

  /**
   * Attaches a new payment method to an organization's customer profile.
   *
   * @param organizationId The ID of the organization.
   * @param stripePaymentMethodId The Stripe ID of the payment method to attach.
   * @return A {@link PaymentMethodResponse} for the newly attached payment method.
   * @throws StripeException if there is an error communicating with the Stripe API.
   */
  PaymentMethodResponse attachPaymentMethod(UUID organizationId, String stripePaymentMethodId)
      throws StripeException;

  /**
   * Sets a specific payment method as the default for an organization.
   *
   * @param organizationId The ID of the organization.
   * @param paymentMethodId The internal ID of the payment method to set as default.
   * @return A {@link PaymentMethodResponse} for the updated payment method.
   */
  PaymentMethodResponse setDefaultPaymentMethod(UUID organizationId, UUID paymentMethodId);

  /**
   * Detaches a payment method from an organization's customer profile.
   *
   * @param organizationId The ID of the organization.
   * @param paymentMethodId The internal ID of the payment method to detach.
   * @throws StripeException if there is an error communicating with the Stripe API.
   */
  void detachPaymentMethod(UUID organizationId, UUID paymentMethodId) throws StripeException;

  /**
   * Updates the details of a stored payment method.
   *
   * @param organizationId The ID of the organization.
   * @param paymentMethodId The internal ID of the payment method to update.
   * @param displayName The new display name for the payment method.
   * @param billingDetails The new billing details.
   * @return A {@link PaymentMethodResponse} for the updated payment method.
   */
  PaymentMethodResponse updatePaymentMethod(
      UUID organizationId, UUID paymentMethodId, String displayName, BillingDetails billingDetails);

  /**
   * Retrieves all stored payment methods for an organization.
   *
   * @param organizationId The ID of the organization.
   * @return A list of {@link PaymentMethodResponse} objects.
   */
  List<PaymentMethodResponse> getOrganizationPaymentMethods(UUID organizationId);

  /**
   * Retrieves payment statistics for an organization.
   *
   * @param organizationId The ID of the organization.
   * @return A {@link PaymentStatisticsResponse} containing aggregated payment data.
   */
  PaymentStatisticsResponse getPaymentStatistics(UUID organizationId);
}
