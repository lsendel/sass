package com.platform.payment.api;

import com.platform.shared.types.Money;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a request to create a payment intent.
 *
 * <p>This record encapsulates all the necessary data for creating a payment, including validation
 * logic within its compact constructor to ensure data integrity.
 *
 * @param organizationId The unique identifier of the organization initiating the payment.
 * @param amount The monetary value of the payment, represented by the {@link Money} type.
 * @param currency The ISO 3-character currency code.
 * @param description A brief description of the payment.
 * @param metadata A map of custom key-value pairs for additional context.
 */
public record PaymentRequest(
    UUID organizationId,
    Money amount,
    String currency,
    String description,
    Map<String, String> metadata) {

  /**
   * Compact constructor that validates the request parameters upon instantiation.
   *
   * @throws IllegalArgumentException if any of the required fields are null, blank, or invalid.
   */
  public PaymentRequest {
    if (organizationId == null) {
      throw new IllegalArgumentException("Organization ID is required");
    }
    if (amount == null || amount.isNegative() || amount.isZero()) {
      throw new IllegalArgumentException("Valid positive amount is required");
    }
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency is required");
    }
    if (currency.length() != 3) {
      throw new IllegalArgumentException("Currency must be a valid 3-character ISO code");
    }
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Description is required");
    }
    if (description.length() > 500) {
      throw new IllegalArgumentException("Description cannot exceed 500 characters");
    }
  }

  /**
   * A factory method to create a {@code PaymentRequest} with the minimum required fields.
   *
   * @param organizationId The ID of the organization making the payment.
   * @param amount The payment amount.
   * @param currency The ISO 3-character currency code.
   * @param description A description of the payment.
   * @return A new {@link PaymentRequest} instance.
   */
  public static PaymentRequest of(
      UUID organizationId, Money amount, String currency, String description) {
    return new PaymentRequest(organizationId, amount, currency, description, null);
  }

  /**
   * A factory method to create a {@code PaymentRequest} that includes metadata.
   *
   * @param organizationId The ID of the organization making the payment.
   * @param amount The payment amount.
   * @param currency The ISO 3-character currency code.
   * @param description A description of the payment.
   * @param metadata Additional key-value metadata for the payment.
   * @return A new {@link PaymentRequest} instance.
   */
  public static PaymentRequest withMetadata(
      UUID organizationId,
      Money amount,
      String currency,
      String description,
      Map<String, String> metadata) {
    return new PaymentRequest(organizationId, amount, currency, description, metadata);
  }

  /**
   * Returns the currency code in a normalized, uppercase format.
   *
   * @return The uppercase currency code.
   */
  public String normalizedCurrency() {
    return currency.toUpperCase();
  }

  /**
   * Checks whether this payment request includes metadata.
   *
   * @return {@code true} if metadata is present and not empty, {@code false} otherwise.
   */
  public boolean hasMetadata() {
    return metadata != null && !metadata.isEmpty();
  }
}