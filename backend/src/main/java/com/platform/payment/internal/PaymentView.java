package com.platform.payment.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A read-only projection of a {@link Payment} entity, designed for use in the API layer.
 *
 * <p>This record provides a safe way to expose payment data without revealing the full internal
 * entity. It includes all necessary information for displaying payment details to a user.
 *
 * @param id the unique identifier of the payment
 * @param organizationId the ID of the organization associated with the payment
 * @param stripePaymentIntentId the ID of the payment intent in Stripe
 * @param amount the monetary amount of the payment
 * @param currency the currency of the payment
 * @param description a description of the payment
 * @param status the current status of the payment
 * @param subscriptionId the ID of the subscription this payment is for, if applicable
 * @param invoiceId the ID of the invoice this payment is for, if applicable
 * @param metadata a map of custom metadata associated with the payment
 * @param createdAt the timestamp when the payment was created
 * @param updatedAt the timestamp when the payment was last updated
 */
public record PaymentView(
    UUID id,
    UUID organizationId,
    String stripePaymentIntentId,
    BigDecimal amount,
    String currency,
    String description,
    String status,
    UUID subscriptionId,
    UUID invoiceId,
    Map<String, String> metadata,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Creates a {@link PaymentView} from a {@link Payment} entity.
   *
   * @param payment the payment entity to convert
   * @return a new {@link PaymentView} instance
   */
  public static PaymentView fromEntity(Payment payment) {
    return new PaymentView(
        payment.getId(),
        payment.getOrganizationId(),
        payment.getStripePaymentIntentId(),
        payment.getAmount().getAmount(),
        payment.getCurrency(),
        payment.getDescription(),
        payment.getStatus().name(),
        payment.getSubscriptionId(),
        payment.getInvoiceId(),
        payment.getMetadata(),
        payment.getCreatedAt(),
        payment.getUpdatedAt());
  }
}

