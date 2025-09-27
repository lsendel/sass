package com.platform.payment.internal;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.audit.internal.AuditService;
import com.platform.payment.api.PaymentRequest;
import com.platform.shared.security.TenantContext;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;

/**
 * Service responsible for managing Stripe PaymentIntents.
 * Separated from PaymentService for better single responsibility principle.
 */
@Service
@Transactional
public class PaymentIntentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentIntentService.class);
    private static final String PAYMENT_RETURN_URL = "https://your-website.com/payment/return"; // TODO: Make configurable

    private final PaymentRepository paymentRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final PaymentValidator paymentValidator;
    private final StripeCustomerService stripeCustomerService;

    public PaymentIntentService(
            PaymentRepository paymentRepository,
            OrganizationRepository organizationRepository,
            AuditService auditService,
            PaymentValidator paymentValidator,
            StripeCustomerService stripeCustomerService) {
        this.paymentRepository = paymentRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
        this.paymentValidator = paymentValidator;
        this.stripeCustomerService = stripeCustomerService;
    }

    /**
     * Creates a new PaymentIntent with Stripe and saves corresponding Payment record.
     */
    public PaymentIntent createPaymentIntent(PaymentRequest request) throws StripeException {
        paymentValidator.validateOrganizationAccess(request.organizationId());

        Organization organization = findOrganization(request.organizationId());
        String customerId = stripeCustomerService.getOrCreateCustomer(organization);

        PaymentIntent stripeIntent = createStripePaymentIntent(request, customerId);
        Payment payment = createPaymentRecord(request, stripeIntent);

        logPaymentCreation(stripeIntent, request);

        return stripeIntent;
    }

    /**
     * Confirms a PaymentIntent with the provided payment method.
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId)
            throws StripeException {
        paymentValidator.validatePaymentMethodId(paymentMethodId);

        Payment payment = findPaymentByStripeId(paymentIntentId);
        paymentValidator.validateOrganizationAccess(payment.getOrganizationId());

        PaymentIntent confirmedIntent = performStripeConfirmation(paymentIntentId, paymentMethodId);
        updatePaymentStatus(payment, confirmedIntent);

        logPaymentConfirmation(paymentIntentId, paymentMethodId, confirmedIntent, payment);

        return confirmedIntent;
    }

    /**
     * Cancels a PaymentIntent.
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        Payment payment = findPaymentByStripeId(paymentIntentId);
        paymentValidator.validateOrganizationAccess(payment.getOrganizationId());

        PaymentIntent canceledIntent = performStripeCancellation(paymentIntentId);
        updatePaymentStatus(payment, canceledIntent);

        logPaymentCancellation(paymentIntentId, canceledIntent);

        return canceledIntent;
    }

    // Private helper methods for better organization

    private Organization findOrganization(UUID organizationId) {
        return organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
    }

    private Payment findPaymentByStripeId(String stripePaymentIntentId) {
        return paymentRepository
                .findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + stripePaymentIntentId));
    }

    private PaymentIntent createStripePaymentIntent(PaymentRequest request, String customerId)
            throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.amount().getAmountInCents())
                .setCurrency(request.currency().toLowerCase())
                .setCustomer(customerId)
                .setDescription(request.description())
                .putAllMetadata(request.metadata() != null ? request.metadata() : Map.of())
                .putMetadata("organization_id", request.organizationId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .build();

        return PaymentIntent.create(params);
    }

    private Payment createPaymentRecord(PaymentRequest request, PaymentIntent stripeIntent) {
        Payment payment = new Payment(
                request.organizationId(),
                stripeIntent.getId(),
                request.amount(),
                request.currency(),
                request.description());

        UUID currentUserId = TenantContext.getCurrentUserId();
        if (currentUserId != null) {
            payment.assignUser(currentUserId);
        }

        if (request.metadata() != null) {
            payment.updateMetadata(request.metadata());
        }

        return paymentRepository.save(payment);
    }

    private PaymentIntent performStripeConfirmation(String paymentIntentId, String paymentMethodId)
            throws StripeException {
        PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                .setPaymentMethod(paymentMethodId)
                .setReturnUrl(PAYMENT_RETURN_URL)
                .build();

        PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return stripePaymentIntent.confirm(params);
    }

    private PaymentIntent performStripeCancellation(String paymentIntentId) throws StripeException {
        PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return stripePaymentIntent.cancel();
    }

    private void updatePaymentStatus(Payment payment, PaymentIntent stripeIntent) {
        Payment.Status newStatus = mapStripeStatusToPaymentStatus(stripeIntent.getStatus());
        payment.updateStatus(newStatus);
        paymentRepository.save(payment);
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

    private void logPaymentCreation(PaymentIntent stripeIntent, PaymentRequest request) {
        auditService.logPaymentEvent(
                "PAYMENT_INTENT_CREATED",
                stripeIntent.getId(),
                "Payment intent created",
                Map.of(
                        "amount", request.amount().getAmount().toString(),
                        "currency", request.currency(),
                        "stripe_payment_intent_id", stripeIntent.getId()),
                "system",
                "PaymentIntentService");

        logger.info("Created payment intent: {} for organization: {}",
                stripeIntent.getId(), request.organizationId());
    }

    private void logPaymentConfirmation(String paymentIntentId, String paymentMethodId,
                                      PaymentIntent confirmedIntent, Payment payment) {
        auditService.logPaymentEvent(
                "PAYMENT_INTENT_CONFIRMED",
                paymentIntentId,
                "Payment intent confirmed",
                Map.of(
                        "payment_method_id", paymentMethodId,
                        "status", confirmedIntent.getStatus(),
                        "amount", payment.getAmount().getAmount().toString()),
                "system",
                "PaymentIntentService");

        logger.info("Confirmed payment intent: {} with status: {}",
                paymentIntentId, confirmedIntent.getStatus());
    }

    private void logPaymentCancellation(String paymentIntentId, PaymentIntent canceledIntent) {
        auditService.logPaymentEvent(
                "PAYMENT_INTENT_CANCELED",
                paymentIntentId,
                "Payment intent canceled",
                Map.of("status", canceledIntent.getStatus()),
                "system",
                "PaymentIntentService");

        logger.info("Canceled payment intent: {}", paymentIntentId);
    }
}