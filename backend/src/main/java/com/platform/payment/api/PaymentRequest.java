package com.platform.payment.api;

import java.util.Map;
import java.util.UUID;

import com.platform.shared.types.Money;

/**
 * Request object for creating payment intents.
 * Encapsulates all required data for payment creation with validation.
 */
public record PaymentRequest(
        UUID organizationId,
        Money amount,
        String currency,
        String description,
        Map<String, String> metadata
) {
    /**
     * Compact constructor with validation.
     * Validates that all required fields are present and valid.
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
        // Metadata is optional, so no validation needed
    }

    /**
     * Creates a PaymentRequest with minimal required fields.
     *
     * @param organizationId the organization making the payment
     * @param amount the payment amount
     * @param currency the currency code (ISO 3-character)
     * @param description the payment description
     * @return a new PaymentRequest instance
     */
    public static PaymentRequest of(UUID organizationId, Money amount, String currency, String description) {
        return new PaymentRequest(organizationId, amount, currency, description, null);
    }

    /**
     * Creates a PaymentRequest with metadata.
     *
     * @param organizationId the organization making the payment
     * @param amount the payment amount
     * @param currency the currency code (ISO 3-character)
     * @param description the payment description
     * @param metadata additional metadata for the payment
     * @return a new PaymentRequest instance
     */
    public static PaymentRequest withMetadata(UUID organizationId, Money amount, String currency,
                                            String description, Map<String, String> metadata) {
        return new PaymentRequest(organizationId, amount, currency, description, metadata);
    }

    /**
     * Returns the normalized currency code (uppercase).
     *
     * @return the currency code in uppercase
     */
    public String normalizedCurrency() {
        return currency.toUpperCase();
    }

    /**
     * Checks if this payment request has metadata.
     *
     * @return true if metadata is present and not empty
     */
    public boolean hasMetadata() {
        return metadata != null && !metadata.isEmpty();
    }
}