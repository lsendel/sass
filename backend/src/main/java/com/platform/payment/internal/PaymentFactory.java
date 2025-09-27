package com.platform.payment.internal;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.platform.payment.api.PaymentRequest;
import com.platform.shared.security.TenantContext;
import com.platform.shared.types.Money;

/**
 * Factory for creating Payment entities with proper validation and defaults.
 * Implements the Factory pattern to encapsulate complex Payment creation logic.
 */
@Component
public class PaymentFactory {

    private final PaymentValidator paymentValidator;

    public PaymentFactory(PaymentValidator paymentValidator) {
        this.paymentValidator = paymentValidator;
    }

    /**
     * Creates a Payment entity from a PaymentRequest.
     *
     * @param request the payment request containing payment details
     * @param stripePaymentIntentId the Stripe payment intent ID
     * @return a new Payment entity
     */
    public Payment createPayment(PaymentRequest request, String stripePaymentIntentId) {
        paymentValidator.validatePaymentRequest(request);

        if (stripePaymentIntentId == null || stripePaymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Stripe payment intent ID is required");
        }

        Payment payment = new Payment(
            request.organizationId(),
            stripePaymentIntentId,
            request.amount(),
            request.currency(),
            request.description()
        );

        // Auto-assign current user if available
        UUID currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId != null) {
            payment.assignUser(currentUserId);
        }

        // Add metadata if provided
        if (request.hasMetadata()) {
            payment.updateMetadata(request.metadata());
        }

        // Set initial status
        payment.updateStatus(Payment.Status.PENDING);

        return payment;
    }

    /**
     * Creates a Payment entity for refund operations.
     *
     * @param originalPayment the original payment being refunded
     * @param refundAmount the amount to refund
     * @param reason the reason for the refund
     * @param stripeRefundId the Stripe refund ID
     * @return a new Payment entity representing the refund
     */
    public Payment createRefundPayment(Payment originalPayment, Money refundAmount,
                                     String reason, String stripeRefundId) {
        if (originalPayment == null) {
            throw new IllegalArgumentException("Original payment is required for refund");
        }

        if (refundAmount == null || refundAmount.isNegative() || refundAmount.isZero()) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        if (refundAmount.isGreaterThan(originalPayment.getAmount())) {
            throw new IllegalArgumentException("Refund amount cannot exceed original payment amount");
        }

        Payment refundPayment = new Payment(
            originalPayment.getOrganizationId(),
            stripeRefundId,
            refundAmount.negate(), // Negative amount for refunds
            originalPayment.getCurrency(),
            "Refund: " + (reason != null ? reason : "Customer request")
        );

        // Link to original payment
        Map<String, String> metadata = Map.of(
            "refund_reason", reason != null ? reason : "Customer request",
            "original_payment_id", originalPayment.getId().toString(),
            "refund_type", "partial"
        );
        refundPayment.updateMetadata(metadata);

        // Auto-assign current user
        UUID currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId != null) {
            refundPayment.assignUser(currentUserId);
        }

        refundPayment.updateStatus(Payment.Status.PROCESSING);

        return refundPayment;
    }

    /**
     * Creates a Payment entity for subscription billing.
     *
     * @param organizationId the organization being billed
     * @param subscriptionId the subscription ID
     * @param amount the billing amount
     * @param billingPeriod the billing period description
     * @param stripeInvoiceId the Stripe invoice ID
     * @return a new Payment entity for subscription billing
     */
    public Payment createSubscriptionPayment(UUID organizationId, String subscriptionId,
                                            Money amount, String billingPeriod,
                                            String stripeInvoiceId) {
        paymentValidator.validateOrganizationAccess(organizationId);

        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("Subscription ID is required");
        }

        if (amount == null || amount.isNegative() || amount.isZero()) {
            throw new IllegalArgumentException("Billing amount must be positive");
        }

        Payment payment = new Payment(
            organizationId,
            stripeInvoiceId,
            amount,
            "USD", // Default currency for subscriptions
            "Subscription billing: " + billingPeriod
        );

        Map<String, String> metadata = Map.of(
            "payment_type", "subscription",
            "subscription_id", subscriptionId,
            "billing_period", billingPeriod != null ? billingPeriod : "monthly"
        );
        payment.updateMetadata(metadata);

        payment.updateStatus(Payment.Status.PENDING);

        return payment;
    }

    /**
     * Creates a builder for complex Payment creation scenarios.
     *
     * @return a new PaymentBuilder instance
     */
    public PaymentBuilder builder() {
        return new PaymentBuilder(this);
    }

    /**
     * Builder pattern implementation for complex Payment creation.
     */
    public static class PaymentBuilder {
        private final PaymentFactory factory;
        private UUID organizationId;
        private String stripePaymentIntentId;
        private Money amount;
        private String currency = "USD";
        private String description;
        private Map<String, String> metadata;
        private Payment.Status initialStatus = Payment.Status.PENDING;
        private UUID assignedUserId;

        private PaymentBuilder(PaymentFactory factory) {
            this.factory = factory;
        }

        public PaymentBuilder organizationId(UUID organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public PaymentBuilder stripePaymentIntentId(String stripePaymentIntentId) {
            this.stripePaymentIntentId = stripePaymentIntentId;
            return this;
        }

        public PaymentBuilder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public PaymentBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PaymentBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public PaymentBuilder initialStatus(Payment.Status status) {
            this.initialStatus = status;
            return this;
        }

        public PaymentBuilder assignedUserId(UUID userId) {
            this.assignedUserId = userId;
            return this;
        }

        public Payment build() {
            PaymentRequest request = new PaymentRequest(
                organizationId, amount, currency, description, metadata
            );

            Payment payment = factory.createPayment(request, stripePaymentIntentId);

            if (initialStatus != Payment.Status.PENDING) {
                payment.updateStatus(initialStatus);
            }

            if (assignedUserId != null) {
                payment.assignUser(assignedUserId);
            }

            return payment;
        }
    }
}