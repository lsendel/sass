package com.platform.payment.internal;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.platform.audit.internal.AuditService;
import com.platform.payment.events.PaymentStatusChangedEvent;

/**
 * Strategy pattern implementation for handling different payment status transitions.
 * Encapsulates the complex logic of status changes and their side effects.
 */
public abstract class PaymentStatusStrategy {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final AuditService auditService;
    protected final ApplicationEventPublisher eventPublisher;

    protected PaymentStatusStrategy(AuditService auditService, ApplicationEventPublisher eventPublisher) {
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handles the status transition for a payment.
     *
     * @param payment the payment to update
     * @param newStatus the new status
     * @param context additional context for the status change
     */
    public abstract void handleStatusChange(Payment payment, Payment.Status newStatus,
                                          StatusChangeContext context);

    /**
     * Determines if this strategy can handle the given status transition.
     *
     * @param currentStatus the current payment status
     * @param newStatus the target status
     * @return true if this strategy can handle the transition
     */
    public abstract boolean canHandle(Payment.Status currentStatus, Payment.Status newStatus);

    /**
     * Context object containing additional information about status changes.
     */
    public record StatusChangeContext(
        String reason,
        String source,
        Map<String, Object> additionalData
    ) {
        public static StatusChangeContext of(String reason, String source) {
            return new StatusChangeContext(reason, source, Map.of());
        }

        public static StatusChangeContext webhook(String webhookEventId) {
            return new StatusChangeContext(
                "Stripe webhook notification",
                "stripe_webhook",
                Map.of("webhook_event_id", webhookEventId)
            );
        }

        public static StatusChangeContext manual(String userId, String reason) {
            return new StatusChangeContext(
                reason,
                "manual",
                Map.of("user_id", userId)
            );
        }
    }
}

/**
 * Strategy for handling successful payment transitions.
 */
@Component
class PaymentSucceededStrategy extends PaymentStatusStrategy {

    public PaymentSucceededStrategy(AuditService auditService, ApplicationEventPublisher eventPublisher) {
        super(auditService, eventPublisher);
    }

    @Override
    public void handleStatusChange(Payment payment, Payment.Status newStatus, StatusChangeContext context) {
        // Update payment status
        payment.updateStatus(newStatus);

        // Log successful payment
        auditService.logPaymentEvent(
            "PAYMENT_SUCCEEDED",
            payment.getStripePaymentIntentId(),
            "Payment completed successfully",
            Map.of(
                "amount", payment.getAmount().getAmount().toString(),
                "currency", payment.getCurrency(),
                "organization_id", payment.getOrganizationId().toString(),
                "source", context.source()
            ),
            context.source().equals("stripe_webhook") ? "webhook" : "system",
            "PaymentService"
        );

        // Publish domain event
        PaymentStatusChangedEvent event = new PaymentStatusChangedEvent(
            payment.getId(),
            payment.getOrganizationId(),
            Payment.Status.PROCESSING, // Assume previous status
            newStatus,
            context.reason()
        );
        eventPublisher.publishEvent(event);

        logger.info("Payment {} succeeded for organization {}",
                   payment.getStripePaymentIntentId(), payment.getOrganizationId());
    }

    @Override
    public boolean canHandle(Payment.Status currentStatus, Payment.Status newStatus) {
        return newStatus == Payment.Status.SUCCEEDED &&
               (currentStatus == Payment.Status.PENDING || currentStatus == Payment.Status.PROCESSING);
    }
}

/**
 * Strategy for handling failed payment transitions.
 */
@Component
class PaymentFailedStrategy extends PaymentStatusStrategy {

    public PaymentFailedStrategy(AuditService auditService, ApplicationEventPublisher eventPublisher) {
        super(auditService, eventPublisher);
    }

    @Override
    public void handleStatusChange(Payment payment, Payment.Status newStatus, StatusChangeContext context) {
        // Update payment status
        payment.updateStatus(newStatus);

        // Log failed payment with higher severity
        auditService.logPaymentEvent(
            "PAYMENT_FAILED",
            payment.getStripePaymentIntentId(),
            "Payment failed: " + context.reason(),
            Map.of(
                "amount", payment.getAmount().getAmount().toString(),
                "currency", payment.getCurrency(),
                "organization_id", payment.getOrganizationId().toString(),
                "failure_reason", context.reason(),
                "source", context.source()
            ),
            context.source().equals("stripe_webhook") ? "webhook" : "system",
            "PaymentService"
        );

        // Publish failure event for potential retry logic
        PaymentStatusChangedEvent event = new PaymentStatusChangedEvent(
            payment.getId(),
            payment.getOrganizationId(),
            payment.getStatus(), // Previous status
            newStatus,
            context.reason()
        );
        eventPublisher.publishEvent(event);

        logger.warn("Payment {} failed for organization {}: {}",
                   payment.getStripePaymentIntentId(), payment.getOrganizationId(), context.reason());
    }

    @Override
    public boolean canHandle(Payment.Status currentStatus, Payment.Status newStatus) {
        return newStatus == Payment.Status.FAILED;
    }
}

/**
 * Strategy for handling canceled payment transitions.
 */
@Component
class PaymentCanceledStrategy extends PaymentStatusStrategy {

    public PaymentCanceledStrategy(AuditService auditService, ApplicationEventPublisher eventPublisher) {
        super(auditService, eventPublisher);
    }

    @Override
    public void handleStatusChange(Payment payment, Payment.Status newStatus, StatusChangeContext context) {
        // Update payment status
        payment.updateStatus(newStatus);

        // Log cancellation
        auditService.logPaymentEvent(
            "PAYMENT_CANCELED",
            payment.getStripePaymentIntentId(),
            "Payment was canceled: " + context.reason(),
            Map.of(
                "amount", payment.getAmount().getAmount().toString(),
                "currency", payment.getCurrency(),
                "organization_id", payment.getOrganizationId().toString(),
                "cancellation_reason", context.reason(),
                "source", context.source()
            ),
            context.source().equals("stripe_webhook") ? "webhook" : "system",
            "PaymentService"
        );

        // Publish cancellation event
        PaymentStatusChangedEvent event = new PaymentStatusChangedEvent(
            payment.getId(),
            payment.getOrganizationId(),
            payment.getStatus(),
            newStatus,
            context.reason()
        );
        eventPublisher.publishEvent(event);

        logger.info("Payment {} canceled for organization {}: {}",
                   payment.getStripePaymentIntentId(), payment.getOrganizationId(), context.reason());
    }

    @Override
    public boolean canHandle(Payment.Status currentStatus, Payment.Status newStatus) {
        return newStatus == Payment.Status.CANCELED &&
               (currentStatus == Payment.Status.PENDING || currentStatus == Payment.Status.PROCESSING);
    }
}

/**
 * Default strategy for handling other status transitions.
 */
@Component
class DefaultPaymentStatusStrategy extends PaymentStatusStrategy {

    public DefaultPaymentStatusStrategy(AuditService auditService, ApplicationEventPublisher eventPublisher) {
        super(auditService, eventPublisher);
    }

    @Override
    public void handleStatusChange(Payment payment, Payment.Status newStatus, StatusChangeContext context) {
        // Simple status update without special handling
        payment.updateStatus(newStatus);

        // Basic audit log
        auditService.logPaymentEvent(
            "PAYMENT_STATUS_CHANGED",
            payment.getStripePaymentIntentId(),
            "Payment status changed to " + newStatus,
            Map.of(
                "new_status", newStatus.toString(),
                "organization_id", payment.getOrganizationId().toString(),
                "source", context.source()
            ),
            context.source().equals("stripe_webhook") ? "webhook" : "system",
            "PaymentService"
        );

        logger.debug("Payment {} status changed to {} for organization {}",
                    payment.getStripePaymentIntentId(), newStatus, payment.getOrganizationId());
    }

    @Override
    public boolean canHandle(Payment.Status currentStatus, Payment.Status newStatus) {
        // Default strategy handles all other cases
        return true;
    }
}