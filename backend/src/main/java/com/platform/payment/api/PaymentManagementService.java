package com.platform.payment.api;

import java.util.List;
import java.util.UUID;

import com.platform.payment.api.PaymentDto.PaymentResponse;
import com.platform.payment.api.PaymentDto.PaymentMethodResponse;
import com.platform.payment.api.PaymentDto.PaymentStatisticsResponse;
import com.platform.payment.api.PaymentDto.PaymentIntentResponse;
import com.platform.payment.api.PaymentDto.CreatePaymentIntentRequest;
import com.platform.payment.api.PaymentDto.ConfirmPaymentIntentRequest;
import com.platform.payment.api.PaymentDto.BillingDetails;
import com.stripe.exception.StripeException;

/**
 * Service interface for payment management operations.
 * This interface provides the API layer with access to payment functionality
 * without depending on internal implementation details.
 */
public interface PaymentManagementService {

  /**
   * Creates a payment intent.
   *
   * @param organizationId the organization ID
   * @param amount the payment amount
   * @param currency the payment currency
   * @param paymentMethodId the payment method ID (optional)
   * @param description the payment description
   * @param confirm whether to confirm the payment immediately
   * @return the created payment
   * @throws StripeException if Stripe operation fails
   */
  PaymentResponse createPayment(
      UUID organizationId,
      java.math.BigDecimal amount,
      String currency,
      String paymentMethodId,
      String description,
      boolean confirm) throws StripeException;

  /**
   * Creates a Stripe payment intent.
   *
   * @param request the payment intent request payload
   * @return payment intent details
   * @throws StripeException if Stripe operation fails
   */
  PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request)
      throws StripeException;

  /**
   * Confirms a Stripe payment intent.
   *
   * @param organizationId the organization ID
   * @param paymentIntentId the payment intent ID
   * @param request the confirm request payload
   * @return payment intent details after confirmation
   * @throws StripeException if Stripe operation fails
   */
  PaymentIntentResponse confirmPaymentIntent(
      UUID organizationId, String paymentIntentId, ConfirmPaymentIntentRequest request)
      throws StripeException;

  /**
   * Cancels a Stripe payment intent.
   *
   * @param organizationId the organization ID
   * @param paymentIntentId the payment intent ID
   * @return payment intent details after cancellation
   * @throws StripeException if Stripe operation fails
   */
  PaymentIntentResponse cancelPaymentIntent(UUID organizationId, String paymentIntentId)
      throws StripeException;

  /**
   * Confirms a payment intent.
   *
   * @param organizationId the organization ID
   * @param paymentIntentId the payment intent ID
   * @return the confirmed payment
   * @throws StripeException if Stripe operation fails
   */
  PaymentResponse confirmPayment(UUID organizationId, String paymentIntentId) throws StripeException;

  /**
   * Cancels a payment intent.
   *
   * @param organizationId the organization ID
   * @param paymentIntentId the payment intent ID
   * @return the canceled payment
   * @throws StripeException if Stripe operation fails
   */
  PaymentResponse cancelPayment(UUID organizationId, String paymentIntentId) throws StripeException;

  /**
   * Gets payments for an organization.
   *
   * @param organizationId the organization ID
   * @return list of payments
   */
  List<PaymentResponse> getOrganizationPayments(UUID organizationId);

  /**
   * Gets payments for an organization filtered by status.
   *
   * @param organizationId the organization ID
   * @param status the status filter
   * @return list of payments
   */
  List<PaymentResponse> getOrganizationPaymentsByStatus(UUID organizationId, String status);

  /**
   * Attaches a payment method to an organization.
   *
   * @param organizationId the organization ID
   * @param stripePaymentMethodId the Stripe payment method ID
   * @return the attached payment method
   * @throws StripeException if Stripe operation fails
   */
  PaymentMethodResponse attachPaymentMethod(UUID organizationId, String stripePaymentMethodId) throws StripeException;

  /**
   * Sets a payment method as default for an organization.
   *
   * @param organizationId the organization ID
   * @param paymentMethodId the payment method ID
   * @return the updated payment method
   */
  PaymentMethodResponse setDefaultPaymentMethod(UUID organizationId, UUID paymentMethodId);

  /**
   * Detaches a payment method from an organization.
   *
   * @param organizationId the organization ID
   * @param paymentMethodId the payment method ID
   * @throws StripeException if Stripe operation fails
   */
  void detachPaymentMethod(UUID organizationId, UUID paymentMethodId) throws StripeException;

  /**
   * Updates a payment method.
   *
   * @param organizationId the organization ID
   * @param paymentMethodId the payment method ID
   * @param displayName the new display name
   * @param billingDetails the new billing details
   * @return the updated payment method
   */
  PaymentMethodResponse updatePaymentMethod(
      UUID organizationId,
      UUID paymentMethodId,
      String displayName,
      BillingDetails billingDetails);

  /**
   * Gets payment methods for an organization.
   *
   * @param organizationId the organization ID
   * @return list of payment methods
   */
  List<PaymentMethodResponse> getOrganizationPaymentMethods(UUID organizationId);

  /**
   * Gets payment statistics for an organization.
   *
   * @param organizationId the organization ID
   * @return payment statistics
   */
  PaymentStatisticsResponse getPaymentStatistics(UUID organizationId);
}
