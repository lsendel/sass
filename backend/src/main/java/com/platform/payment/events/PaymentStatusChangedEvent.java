package com.platform.payment.events;

import java.time.Instant;
import java.util.UUID;

import com.platform.payment.internal.Payment;

/**
 * Domain event published when a payment status changes.
 * Used for decoupled communication between modules.
 */
public record PaymentStatusChangedEvent(
    UUID paymentId,
    UUID organizationId,
    Payment.Status previousStatus,
    Payment.Status newStatus,
    String reason,
    Instant occurredAt
) {

    /**
     * Constructor with automatic timestamp.
     */
    public PaymentStatusChangedEvent(UUID paymentId, UUID organizationId,
                                   Payment.Status previousStatus, Payment.Status newStatus,
                                   String reason) {
        this(paymentId, organizationId, previousStatus, newStatus, reason, Instant.now());
    }

    /**
     * Checks if this represents a successful payment.
     */
    public boolean isPaymentSucceeded() {
        return newStatus == Payment.Status.SUCCEEDED;
    }

    /**
     * Checks if this represents a failed payment.
     */
    public boolean isPaymentFailed() {
        return newStatus == Payment.Status.FAILED;
    }

    /**
     * Checks if this represents a canceled payment.
     */
    public boolean isPaymentCanceled() {
        return newStatus == Payment.Status.CANCELED;
    }

    /**
     * Gets a human-readable description of the status change.
     */
    public String getDescription() {
        return String.format("Payment %s changed from %s to %s",
                           paymentId, previousStatus, newStatus);
    }
}