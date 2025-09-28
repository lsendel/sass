package com.platform.payment.internal;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Manages payment status transitions using the Strategy pattern.
 *
 * <p>This class provides a clean and centralized interface for handling all payment status changes.
 * It delegates the logic for specific transitions to a collection of {@link PaymentStatusStrategy}
 * implementations, ensuring that status changes are valid and consistent.
 *
 * @see PaymentStatusStrategy
 * @see Payment
 */
@Service
public class PaymentStatusManager {

    private final List<PaymentStatusStrategy> strategies;
    private final PaymentRepository paymentRepository;

    /**
     * Constructs a new PaymentStatusManager.
     *
     * @param strategies a list of all available payment status strategies
     * @param paymentRepository the repository for managing payments
     */
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
     * @throws IllegalArgumentException if the payment or new status is null
     * @throws IllegalStateException if no strategy can be found for the requested transition
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
     * Updates a payment's status based on a Stripe webhook event.
     *
     * @param payment the payment to update
     * @param stripeStatus the status string from the Stripe webhook
     * @param webhookEventId the ID of the webhook event, for auditing purposes
     */
    public void updateFromStripeWebhook(Payment payment, String stripeStatus, String webhookEventId) {
        Payment.Status newStatus = mapStripeStatusToPaymentStatus(stripeStatus);
        PaymentStatusStrategy.StatusChangeContext context =
            PaymentStatusStrategy.StatusChangeContext.webhook(webhookEventId);

        updatePaymentStatus(payment, newStatus, context);
    }

    /**
     * Manually updates a payment's status, typically for administrative actions.
     *
     * @param payment the payment to update
     * @param newStatus the new status to set
     * @param userId the ID of the user performing the manual update
     * @param reason a description of why the status is being changed manually
     */
    public void updateManually(Payment payment, Payment.Status newStatus, String userId, String reason) {
        PaymentStatusStrategy.StatusChangeContext context =
            PaymentStatusStrategy.StatusChangeContext.manual(userId, reason);

        updatePaymentStatus(payment, newStatus, context);
    }

    /**
     * Checks if a transition between two statuses is valid.
     *
     * @param currentStatus the current status
     * @param newStatus the target status
     * @return {@code true} if a strategy exists for the transition, {@code false} otherwise
     */
    public boolean isValidTransition(Payment.Status currentStatus, Payment.Status newStatus) {
        return strategies.stream()
            .anyMatch(strategy -> strategy.canHandle(currentStatus, newStatus));
    }

    /**
     * Gets all possible next statuses from a given current status.
     *
     * @param currentStatus the current payment status
     * @return a list of all possible next statuses
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