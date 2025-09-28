package com.platform.payment.internal;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.platform.payment.api.PaymentRequest;
import com.platform.shared.security.TenantContext;
import com.platform.shared.types.Money;

/**
 * A component for validating payment-related operations.
 *
 * <p>This class centralizes all validation logic for payments, payment methods, and related
 * entities, ensuring that all operations are consistent and secure.
 */
@Component
public class PaymentValidator {

    /**
     * Validates that the current user has access to the specified organization.
     *
     * @param organizationId the organization ID to validate access for
     * @throws SecurityException if authentication is missing or access is denied
     */
    public void validateOrganizationAccess(UUID organizationId) {
        UUID currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new SecurityException("Authentication required");
        }

        if (!TenantContext.belongsToOrganization(organizationId)) {
            throw new SecurityException("Access denied to organization: " + organizationId);
        }
    }

    /**
     * Validates that a payment method ID is not null or blank.
     *
     * @param paymentMethodId the payment method ID to validate
     * @throws IllegalArgumentException if the payment method ID is invalid
     */
    public void validatePaymentMethodId(String paymentMethodId) {
        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            throw new IllegalArgumentException("Payment method ID cannot be null or blank");
        }
    }

    /**
     * Validates a complete payment request for correctness.
     *
     * @param request the payment request to validate
     * @throws IllegalArgumentException if any required field is invalid
     */
    public void validatePaymentRequest(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }

        validateOrganizationId(request.organizationId());
        validateAmount(request.amount());
        validateCurrency(request.currency());
        validateDescription(request.description());
    }

    /**
     * Validates that a payment method belongs to the specified organization.
     *
     * @param paymentMethod the payment method to validate
     * @param organizationId the expected organization ID
     * @throws SecurityException if the payment method belongs to a different organization
     */
    public void validatePaymentMethodOwnership(PaymentMethod paymentMethod, UUID organizationId) {
        if (!paymentMethod.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("Access denied - payment method belongs to different organization");
        }
    }

    /**
     * Validates that a payment method is not deleted.
     *
     * @param paymentMethod the payment method to validate
     * @throws IllegalArgumentException if the payment method is deleted
     */
    public void validatePaymentMethodNotDeleted(PaymentMethod paymentMethod) {
        if (paymentMethod.isDeleted()) {
            throw new IllegalArgumentException("Cannot operate on deleted payment method");
        }
    }

    // Private validation helper methods

    private void validateOrganizationId(UUID organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization ID is required");
        }
    }

    private void validateAmount(Money amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount.isZero()) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a valid 3-character ISO code");
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (description.length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters");
        }
    }
}