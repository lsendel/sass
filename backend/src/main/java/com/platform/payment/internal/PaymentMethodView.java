package com.platform.payment.internal;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of a PaymentMethod for API layer mapping.
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

