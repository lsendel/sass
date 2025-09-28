package com.platform.payment.api;

import com.stripe.model.PaymentIntent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A container for all Data Transfer Objects (DTOs) used in payment-related operations.
 *
 * <p>This class uses nested records and enums to define the data structures for API requests and
 * responses. This approach encapsulates payment-related data contracts, preventing direct exposure
 * of internal domain entities and providing a clear, immutable structure for data exchange.
 * </p>
 */
public class PaymentDto {

  /** Defines the possible statuses of a payment at the API layer. */
  public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELED,
    REQUIRES_ACTION
  }

  /** Defines the types of payment methods available at the API layer. */
  public enum PaymentMethodType {
    CARD,
    BANK_ACCOUNT,
    OTHER
  }

  /**
   * Represents a payment response, providing detailed information about a payment.
   *
   * @param id The internal unique identifier for the payment.
   * @param organizationId The ID of the organization associated with the payment.
   * @param stripePaymentIntentId The corresponding ID from the Stripe PaymentIntent.
   * @param amount The payment amount.
   * @param currency The currency of the payment (e.g., "usd").
   * @param status The current status of the payment.
   * @param paymentMethodId The ID of the payment method used.
   * @param description A description of the payment.
   * @param createdAt The timestamp when the payment was created.
   */
  public record PaymentResponse(
      UUID id,
      UUID organizationId,
      String stripePaymentIntentId,
      BigDecimal amount,
      String currency,
      PaymentStatus status,
      String paymentMethodId,
      String description,
      Instant createdAt) {}

  /**
   * Represents a payment method response, providing details about a stored payment method.
   *
   * @param id The internal unique identifier for the payment method.
   * @param organizationId The ID of the organization that owns the payment method.
   * @param stripePaymentMethodId The corresponding ID from Stripe.
   * @param type The type of the payment method (e.g., "card").
   * @param isDefault Whether this is the default payment method for the organization.
   * @param displayName A user-friendly name for the payment method.
   * @param cardDetails Specific details if the payment method is a card.
   * @param billingDetails The billing information associated with the payment method.
   * @param createdAt The timestamp when the payment method was created.
   */
  public record PaymentMethodResponse(
      UUID id,
      UUID organizationId,
      String stripePaymentMethodId,
      String type,
      boolean isDefault,
      String displayName,
      CardDetails cardDetails,
      BillingDetails billingDetails,
      Instant createdAt) {}

  /**
   * Represents a summary of payment statistics for an organization.
   *
   * @param totalSuccessfulPayments The total count of successful payments.
   * @param totalAmount The total monetary value of all successful payments.
   * @param recentAmount The monetary value of payments in a recent time window (e.g., last 30
   *     days).
   */
  public record PaymentStatisticsResponse(
      long totalSuccessfulPayments, BigDecimal totalAmount, BigDecimal recentAmount) {}

  /**
   * Represents a request to create a new Stripe PaymentIntent.
   *
   * @param organizationId The ID of the organization initiating the payment.
   * @param amount The amount to be charged.
   * @param currency The currency for the payment.
   * @param description An optional description for the payment.
   * @param metadata A map of custom key-value pairs to store with the payment.
   */
  public record CreatePaymentIntentRequest(
      @NotNull UUID organizationId,
      @NotNull @Positive BigDecimal amount,
      @NotBlank String currency,
      String description,
      Map<String, String> metadata) {}

  /**
   * Represents a request to confirm a PaymentIntent, typically after gathering payment details on
   * the client-side.
   *
   * @param paymentMethodId The ID of the Stripe PaymentMethod to use for confirmation.
   */
  public record ConfirmPaymentIntentRequest(@NotBlank String paymentMethodId) {}

  /**
   * Represents the response from creating or confirming a Stripe PaymentIntent.
   *
   * @param id The ID of the PaymentIntent.
   * @param clientSecret The client secret needed by the frontend to complete the payment flow.
   * @param status The current status of the PaymentIntent.
   * @param amount The amount of the payment.
   * @param currency The currency of the payment.
   * @param description An optional description.
   * @param metadata Associated metadata.
   */
  public record PaymentIntentResponse(
      String id,
      String clientSecret,
      String status,
      BigDecimal amount,
      String currency,
      String description,
      Map<String, String> metadata) {

    /**
     * A factory method to create a {@link PaymentIntentResponse} from a Stripe {@link
     * PaymentIntent} object.
     *
     * @param intent The Stripe PaymentIntent object.
     * @return A new {@link PaymentIntentResponse} instance.
     */
    public static PaymentIntentResponse fromStripePaymentIntent(PaymentIntent intent) {
      return new PaymentIntentResponse(
          intent.getId(),
          intent.getClientSecret(),
          intent.getStatus(),
          BigDecimal.valueOf(intent.getAmount(), 2),
          intent.getCurrency(),
          intent.getDescription(),
          intent.getMetadata());
    }
  }

  /**
   * Represents a request to create a payment.
   *
   * @param organizationId The ID of the organization.
   * @param amount The amount to be charged.
   * @param currency The currency of the payment.
   * @param paymentMethodId The ID of the payment method to use.
   * @param description An optional description.
   * @param confirm Whether to confirm the payment immediately.
   */
  public record CreatePaymentRequest(
      @NotNull UUID organizationId,
      @Positive BigDecimal amount,
      @NotBlank String currency,
      String paymentMethodId,
      String description,
      boolean confirm) {}

  /**
   * Represents a request to attach a new payment method to a customer.
   *
   * @param organizationId The ID of the organization.
   * @param stripePaymentMethodId The Stripe ID of the payment method to attach.
   */
  public record AttachPaymentMethodRequest(
      @NotNull UUID organizationId, @NotBlank String stripePaymentMethodId) {}

  /**
   * Represents a request to update a payment method.
   *
   * @param displayName A user-friendly name for the payment method.
   * @param billingDetails The updated billing details.
   */
  public record UpdatePaymentMethodRequest(String displayName, BillingDetails billingDetails) {}

  /**
   * Contains details specific to a card payment method.
   *
   * @param lastFour The last four digits of the card number.
   * @param brand The card brand (e.g., "Visa", "Mastercard").
   * @param expMonth The card's expiration month.
   * @param expYear The card's expiration year.
   */
  public record CardDetails(String lastFour, String brand, Integer expMonth, Integer expYear) {}

  /**
   * Contains billing details associated with a payment method.
   *
   * @param name The cardholder's name.
   * @param email The cardholder's email address.
   * @param address The billing address.
   */
  public record BillingDetails(String name, String email, BillingAddress address) {}

  /**
   * Represents a billing address.
   *
   * @param addressLine1 The first line of the address.
   * @param addressLine2 The second line of the address.
   * @param city The city.
   * @param state The state or province.
   * @param postalCode The postal or ZIP code.
   * @param country The two-letter country code.
   */
  public record BillingAddress(
      String addressLine1,
      String addressLine2,
      String city,
      String state,
      String postalCode,
      String country) {}
}
