package com.platform.payment.internal;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Manager class that coordinates payment status changes using the Strategy pattern.
 * Provides a clean interface for status transitions while delegating to appropriate strategies.
 */
@Service
public class PaymentStatusManager {

    private final List<PaymentStatusStrategy> strategies;
    private final PaymentRepository paymentRepository;

    public PaymentStatusManager(List<PaymentStatusStrategy> strategies, PaymentRepository paymentRepository) {
        this.strategies = strategies;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Updates a payment's status using the appropriate strategy.
     *
     * @param payment the payment to update
     * @param newStatus the new status
     * @param context the context for the status change
     */
    public void updatePaymentStatus(Payment payment, Payment.Status newStatus,
                                   PaymentStatusStrategy.StatusChangeContext context) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        if (payment.getStatus() == newStatus) {
            // No change needed
            return;
        }

        PaymentStatusStrategy strategy = findStrategy(payment.getStatus(), newStatus);
        strategy.handleStatusChange(payment, newStatus, context);

        // Save the updated payment
        paymentRepository.save(payment);
    }

    /**
     * Updates a payment status from Stripe webhook data.
     *
     * @param payment the payment to update
     * @param stripeStatus the Stripe status string
     * @param webhookEventId the webhook event ID for auditing
     */
    public void updateFromStripeWebhook(Payment payment, String stripeStatus, String webhookEventId) {
        Payment.Status newStatus = mapStripeStatusToPaymentStatus(stripeStatus);
        PaymentStatusStrategy.StatusChangeContext context =
            PaymentStatusStrategy.StatusChangeContext.webhook(webhookEventId);

        updatePaymentStatus(payment, newStatus, context);
    }

    /**
     * Manually updates a payment status (e.g., admin action).
     *
     * @param payment the payment to update
     * @param newStatus the new status
     * @param userId the user making the change
     * @param reason the reason for the status change
     */
    public void updateManually(Payment payment, Payment.Status newStatus, String userId, String reason) {
        PaymentStatusStrategy.StatusChangeContext context =
            PaymentStatusStrategy.StatusChangeContext.manual(userId, reason);

        updatePaymentStatus(payment, newStatus, context);
    }

    /**
     * Checks if a status transition is valid.
     *
     * @param currentStatus the current status
     * @param newStatus the target status
     * @return true if the transition is allowed
     */
    public boolean isValidTransition(Payment.Status currentStatus, Payment.Status newStatus) {
        return strategies.stream()
            .anyMatch(strategy -> strategy.canHandle(currentStatus, newStatus));
    }

    /**
     * Gets all possible next statuses from the current status.
     *
     * @param currentStatus the current payment status
     * @return list of possible next statuses
     */
    public List<Payment.Status> getPossibleTransitions(Payment.Status currentStatus) {
        return List.of(Payment.Status.values())
            .stream()
            .filter(status -> status != currentStatus)
            .filter(status -> isValidTransition(currentStatus, status))
            .toList();
    }

    private PaymentStatusStrategy findStrategy(Payment.Status currentStatus, Payment.Status newStatus) {
        return strategies.stream()
            .filter(strategy -> strategy.canHandle(currentStatus, newStatus))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No strategy found for transition from " + currentStatus + " to " + newStatus));
    }

    private Payment.Status mapStripeStatusToPaymentStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> Payment.Status.SUCCEEDED;
            case "processing" -> Payment.Status.PROCESSING;
            case "requires_payment_method", "requires_confirmation", "requires_action" -> Payment.Status.PENDING;
            case "canceled" -> Payment.Status.CANCELED;
            default -> Payment.Status.FAILED;
        };
    }
}