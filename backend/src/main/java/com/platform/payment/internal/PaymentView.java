package com.platform.payment.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only projection of a Payment for use by API layer without exposing JPA entity.
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

