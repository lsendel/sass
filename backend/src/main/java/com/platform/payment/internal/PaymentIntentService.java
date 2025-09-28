package com.platform.payment.internal;

import com.platform.audit.internal.AuditService;
import com.platform.payment.api.PaymentRequest;
import com.platform.shared.security.TenantContext;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A service dedicated to managing the lifecycle of Stripe PaymentIntents.
 *
 * <p>This class adheres to the Single Responsibility Principle by isolating the logic for Stripe
 * PaymentIntent operations (creation, confirmation, cancellation) from other payment-related
 * concerns. It interacts with the Stripe API and persists corresponding local {@link Payment}
 * records.
 */
@Service
@Transactional
public class PaymentIntentService {

  private static final Logger logger = LoggerFactory.getLogger(PaymentIntentService.class);
  private static final String PAYMENT_RETURN_URL =
      "https://your-website.com/payment/return"; // TODO: Make configurable

  private final PaymentRepository paymentRepository;
  private final OrganizationRepository organizationRepository;
  private final AuditService auditService;
  private final PaymentValidator paymentValidator;
  private final StripeCustomerService stripeCustomerService;

  /**
   * Constructs the service with its dependencies.
   *
   * @param paymentRepository repository for {@link Payment} entities
   * @param organizationRepository repository for {@link Organization} entities
   * @param auditService service for logging audit events
   * @param paymentValidator validator for payment-related operations
   * @param stripeCustomerService service for managing Stripe customer objects
   */
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
   * Creates a new PaymentIntent in Stripe and saves a corresponding {@link Payment} record in the
   * local database.
   *
   * @param request the request containing the details for the payment intent
   * @return the created {@link PaymentIntent} from Stripe
   * @throws StripeException if an error occurs during communication with the Stripe API
   */
  public PaymentIntent createPaymentIntent(PaymentRequest request) throws StripeException {
    paymentValidator.validateOrganizationAccess(request.organizationId());
    Organization organization = findOrganization(request.organizationId());
    String customerId = stripeCustomerService.getOrCreateCustomer(organization);
    PaymentIntent stripeIntent = createStripePaymentIntent(request, customerId);
    createPaymentRecord(request, stripeIntent);
    logPaymentCreation(stripeIntent, request);
    return stripeIntent;
  }

  /**
   * Confirms a PaymentIntent with a given payment method.
   *
   * @param paymentIntentId the ID of the PaymentIntent to confirm
   * @param paymentMethodId the ID of the payment method to use for confirmation
   * @return the updated {@link PaymentIntent} from Stripe after confirmation
   * @throws StripeException if an error occurs during communication with the Stripe API
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
   *
   * @param paymentIntentId the ID of the PaymentIntent to cancel
   * @return the updated {@link PaymentIntent} from Stripe after cancellation
   * @throws StripeException if an error occurs during communication with the Stripe API
   */
  public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
    Payment payment = findPaymentByStripeId(paymentIntentId);
    paymentValidator.validateOrganizationAccess(payment.getOrganizationId());
    PaymentIntent canceledIntent = performStripeCancellation(paymentIntentId);
    updatePaymentStatus(payment, canceledIntent);
    logPaymentCancellation(paymentIntentId, canceledIntent);
    return canceledIntent;
  }

  /**
   * Finds an organization by its ID.
   *
   * @param organizationId the ID of the organization
   * @return the {@link Organization} entity
   * @throws IllegalArgumentException if the organization is not found
   */
  private Organization findOrganization(UUID organizationId) {
    return organizationRepository
        .findById(organizationId)
        .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
  }

  /**
   * Finds a payment record by its Stripe PaymentIntent ID.
   *
   * @param stripePaymentIntentId the Stripe PaymentIntent ID
   * @return the {@link Payment} entity
   * @throws IllegalArgumentException if the payment is not found
   */
  private Payment findPaymentByStripeId(String stripePaymentIntentId) {
    return paymentRepository
        .findByStripePaymentIntentId(stripePaymentIntentId)
        .orElseThrow(
            () -> new IllegalArgumentException("Payment not found: " + stripePaymentIntentId));
  }

  /**
   * Creates a new PaymentIntent in Stripe.
   *
   * @param request the payment request
   * @param customerId the Stripe customer ID
   * @return the created {@link PaymentIntent}
   * @throws StripeException if an error occurs during the API call
   */
  private PaymentIntent createStripePaymentIntent(PaymentRequest request, String customerId)
      throws StripeException {
    PaymentIntentCreateParams params =
        PaymentIntentCreateParams.builder()
            .setAmount(request.amount().getAmountInCents())
            .setCurrency(request.currency().toLowerCase())
            .setCustomer(customerId)
            .setDescription(request.description())
            .putAllMetadata(request.metadata() != null ? request.metadata() : Map.of())
            .putMetadata("organization_id", request.organizationId().toString())
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
            .build();
    return PaymentIntent.create(params);
  }

  /**
   * Creates and saves a local {@link Payment} record corresponding to a Stripe PaymentIntent.
   *
   * @param request the original payment request
   * @param stripeIntent the Stripe PaymentIntent
   */
  private void createPaymentRecord(PaymentRequest request, PaymentIntent stripeIntent) {
    Payment payment =
        new Payment(
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
    paymentRepository.save(payment);
  }

  /**
   * Confirms a PaymentIntent in Stripe.
   *
   * @param paymentIntentId the ID of the PaymentIntent to confirm
   * @param paymentMethodId the ID of the payment method to use
   * @return the confirmed {@link PaymentIntent}
   * @throws StripeException if an error occurs during the API call
   */
  private PaymentIntent performStripeConfirmation(String paymentIntentId, String paymentMethodId)
      throws StripeException {
    PaymentIntentConfirmParams params =
        PaymentIntentConfirmParams.builder()
            .setPaymentMethod(paymentMethodId)
            .setReturnUrl(PAYMENT_RETURN_URL)
            .build();
    PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(paymentIntentId);
    return stripePaymentIntent.confirm(params);
  }

  /**
   * Cancels a PaymentIntent in Stripe.
   *
   * @param paymentIntentId the ID of the PaymentIntent to cancel
   * @return the canceled {@link PaymentIntent}
   * @throws StripeException if an error occurs during the API call
   */
  private PaymentIntent performStripeCancellation(String paymentIntentId) throws StripeException {
    PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(paymentIntentId);
    return stripePaymentIntent.cancel();
  }

  /**
   * Updates the status of a local {@link Payment} record based on the status of a Stripe
   * PaymentIntent.
   *
   * @param payment the local payment record to update
   * @param stripeIntent the Stripe PaymentIntent
   */
  private void updatePaymentStatus(Payment payment, PaymentIntent stripeIntent) {
    Payment.Status newStatus = mapStripeStatusToPaymentStatus(stripeIntent.getStatus());
    payment.updateStatus(newStatus);
    paymentRepository.save(payment);
  }

  /**
   * Maps a Stripe PaymentIntent status string to a local {@link Payment.Status} enum.
   *
   * @param stripeStatus the status string from Stripe
   * @return the corresponding local status
   */
  private Payment.Status mapStripeStatusToPaymentStatus(String stripeStatus) {
    return switch (stripeStatus) {
      case "succeeded" -> Payment.Status.SUCCEEDED;
      case "processing" -> Payment.Status.PROCESSING;
      case "requires_payment_method", "requires_confirmation", "requires_action" ->
          Payment.Status.PENDING;
      case "canceled" -> Payment.Status.CANCELED;
      default -> Payment.Status.FAILED;
    };
  }

  /**
   * Logs the creation of a payment intent.
   *
   * @param stripeIntent the created Stripe PaymentIntent
   * @param request the original payment request
   */
  private void logPaymentCreation(PaymentIntent stripeIntent, PaymentRequest request) {
    auditService.logPaymentEvent(
        "PAYMENT_INTENT_CREATED",
        stripeIntent.getId(),
        "Payment intent created",
        Map.of(
            "amount",
            request.amount().getAmount().toString(),
            "currency",
            request.currency(),
            "stripe_payment_intent_id",
            stripeIntent.getId()),
        "system",
        "PaymentIntentService");
    logger.info(
        "Created payment intent: {} for organization: {}",
        stripeIntent.getId(),
        request.organizationId());
  }

  /**
   * Logs the confirmation of a payment intent.
   *
   * @param paymentIntentId the ID of the payment intent
   * @param paymentMethodId the ID of the payment method used
   * @param confirmedIntent the confirmed Stripe PaymentIntent
   * @param payment the local payment record
   */
  private void logPaymentConfirmation(
      String paymentIntentId,
      String paymentMethodId,
      PaymentIntent confirmedIntent,
      Payment payment) {
    auditService.logPaymentEvent(
        "PAYMENT_INTENT_CONFIRMED",
        paymentIntentId,
        "Payment intent confirmed",
        Map.of(
            "payment_method_id",
            paymentMethodId,
            "status",
            confirmedIntent.getStatus(),
            "amount",
            payment.getAmount().getAmount().toString()),
        "system",
        "PaymentIntentService");
    logger.info(
        "Confirmed payment intent: {} with status: {}",
        paymentIntentId,
        confirmedIntent.getStatus());
  }

  /**
   * Logs the cancellation of a payment intent.
   *
   * @param paymentIntentId the ID of the payment intent
   * @param canceledIntent the canceled Stripe PaymentIntent
   */
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