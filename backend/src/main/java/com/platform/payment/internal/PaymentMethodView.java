package com.platform.payment.internal;

import java.time.Instant;
import java.util.UUID;

/**
 * A read-only projection of a {@link PaymentMethod} entity.
 *
 * <p>This record is used to safely expose payment method data to other parts of the application,
 * such as the API layer, without exposing the mutable entity itself. It provides a flat, immutable
 * representation of a payment method's details.
 *
 * @param id The internal unique identifier of the payment method.
 * @param organizationId The ID of the organization this payment method belongs to.
 * @param stripePaymentMethodId The corresponding ID from the Stripe API.
 * @param type The type of the payment method (e.g., CARD).
 * @param isDefault Whether this is the default payment method for the organization.
 * @param displayName A user-friendly name for the payment method.
 * @param lastFour The last four digits of the card number, if applicable.
 * @param brand The card brand, if applicable.
 * @param expMonth The card's expiration month, if applicable.
 * @param expYear The card's expiration year, if applicable.
 * @param billingName The name associated with the billing details.
 * @param billingEmail The email associated with the billing details.
 * @param billingAddress The billing address.
 * @param createdAt The timestamp when the payment method was created.
 */
public record PaymentMethodView(
    UUID id,
    UUID organizationId,
    String stripePaymentMethodId,
    PaymentMethod.Type type,
    boolean isDefault,
    String displayName,
    String lastFour,
    String brand,
    Integer expMonth,
    Integer expYear,
    String billingName,
    String billingEmail,
    PaymentMethod.BillingAddress billingAddress,
    Instant createdAt) {

  /**
   * A factory method to create a {@code PaymentMethodView} from a {@link PaymentMethod} entity.
   *
   * @param paymentMethod The entity to create the view from.
   * @return A new {@link PaymentMethodView} instance.
   */
  public static PaymentMethodView fromEntity(PaymentMethod paymentMethod) {
    return new PaymentMethodView(
        paymentMethod.getId(),
        paymentMethod.getOrganizationId(),
        paymentMethod.getStripePaymentMethodId(),
        paymentMethod.getType(),
        paymentMethod.isDefault(),
        paymentMethod.getDisplayName(),
        paymentMethod.getLastFour(),
        paymentMethod.getBrand(),
        paymentMethod.getExpMonth(),
        paymentMethod.getExpYear(),
        paymentMethod.getBillingName(),
        paymentMethod.getBillingEmail(),
        paymentMethod.getBillingAddress(),
        paymentMethod.getCreatedAt());
  }
}

