package com.platform.payment.internal;

import com.platform.audit.internal.AuditService;
import com.platform.shared.security.TenantContext;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerSearchResult;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * The core service for handling all payment-related business logic.
 *
 * <p>This service orchestrates operations related to payments, including creating and managing
 * Stripe PaymentIntents, handling payment methods, processing webhooks, and providing payment
 * analytics. It acts as the central point for all payment operations within the internal domain.
 */

import com.platform.audit.internal.AuditService;
import com.platform.shared.security.TenantContext;
import com.platform.shared.stripe.StripeCustomerService;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;


@Service
@Transactional
public class PaymentService {

  private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

  private final PaymentRepository paymentRepository;
  private final PaymentMethodRepository paymentMethodRepository;
  private final OrganizationRepository organizationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final AuditService auditService;
  private final StripeCustomerService stripeCustomerService;

  /**
   * Constructs the service with its dependencies and initializes the Stripe API key.
   *
   * @param paymentRepository repository for {@link Payment} entities
   * @param paymentMethodRepository repository for {@link PaymentMethod} entities
   * @param organizationRepository repository for {@link Organization} entities
   * @param eventPublisher publisher for application-level events
   * @param auditService service for logging audit events
   * @param stripeSecretKey the secret key for the Stripe API
   */
  public PaymentService(
      PaymentRepository paymentRepository,
      PaymentMethodRepository paymentMethodRepository,
      OrganizationRepository organizationRepository,
      ApplicationEventPublisher eventPublisher,
      AuditService auditService,
      @Value("${stripe.secret-key}") String stripeSecretKey,
      StripeCustomerService stripeCustomerService) {
    this.paymentRepository = paymentRepository;
    this.paymentMethodRepository = paymentMethodRepository;
    this.organizationRepository = organizationRepository;
    this.eventPublisher = eventPublisher;
    this.auditService = auditService;
    this.stripeCustomerService = stripeCustomerService;
    Stripe.apiKey = stripeSecretKey;
  }

  /**
   * Creates a new Stripe PaymentIntent and a corresponding local {@link Payment} record.
   *
   * @param organizationId the ID of the organization initiating the payment
   * @param amount the monetary amount of the payment
   * @param currency the currency of the payment
   * @param description a description for the payment
   * @param metadata additional metadata to associate with the payment
   * @return the created {@link PaymentIntent} from Stripe
   * @throws StripeException if an error occurs during communication with the Stripe API
   */
  public PaymentIntent createPaymentIntent(
      UUID organizationId,
      Money amount,
      String currency,
      String description,
      Map<String, String> metadata)
      throws StripeException {
    validateOrganizationAccess(organizationId);
    Organization organization =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Organization not found: " + organizationId));

    // Create or get Stripe customer
    String customerId = stripeCustomerService.getOrCreateCustomer(organization);

    // Create payment intent

    PaymentIntentCreateParams params =
        PaymentIntentCreateParams.builder()
            .setAmount(amount.getAmountInCents())
            .setCurrency(currency.toLowerCase())
            .setCustomer(customerId)
            .setDescription(description)
            .putAllMetadata(metadata != null ? metadata : Map.of())
            .putMetadata("organization_id", organizationId.toString())
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
            .build();
    PaymentIntent stripePaymentIntent = PaymentIntent.create(params);
    Payment payment =
        new Payment(organizationId, stripePaymentIntent.getId(), amount, currency, description);
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId != null) {
      payment.assignUser(currentUserId);
    }
    if (metadata != null) {
      payment.updateMetadata(metadata);
    }
    paymentRepository.save(payment);
    auditService.logPaymentEvent(
        "PAYMENT_INTENT_CREATED",
        stripePaymentIntent.getId(),
        "Payment intent created",
        Map.of(
            "amount",
            amount.getAmount().toString(),
            "currency",
            currency,
            "stripe_payment_intent_id",
            stripePaymentIntent.getId()),
        "system",
        "PaymentService");
    logger.info(
        "Created payment intent: {} for organization: {}",
        stripePaymentIntent.getId(),
        organizationId);
    return stripePaymentIntent;
  }

  /**
   * Confirms a Stripe PaymentIntent with a given payment method.
   *
   * @param paymentIntentId the ID of the PaymentIntent to confirm
   * @param paymentMethodId the ID of the payment method to use
   * @return the confirmed {@link PaymentIntent} from Stripe
   * @throws StripeException if an error occurs during communication with the Stripe API
   */
  public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId)
      throws StripeException {
    if (paymentMethodId == null || paymentMethodId.isBlank()) {
      throw new IllegalArgumentException("paymentMethodId cannot be null or blank");
    }
    Payment payment =
        paymentRepository
            .findByStripePaymentIntentId(paymentIntentId)
            .orElseThrow(
                () -> new IllegalArgumentException("Payment not found: " + paymentIntentId));
    validateOrganizationAccess(payment.getOrganizationId());
    PaymentIntentConfirmParams params =
        PaymentIntentConfirmParams.builder()
            .setPaymentMethod(paymentMethodId)
            .setReturnUrl("https://your-website.com/payment/return")
            .build();
    PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(paymentIntentId);
    PaymentIntent confirmedIntent = stripePaymentIntent.confirm(params);
    updatePaymentFromStripeIntent(payment, confirmedIntent);
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
        "PaymentService");
    logger.info(
        "Confirmed payment intent: {} with status: {}",
        paymentIntentId,
        confirmedIntent.getStatus());
    return confirmedIntent;
  }

  /**
   * Cancels a Stripe PaymentIntent.
   *
   * @param paymentIntentId the ID of the PaymentIntent to cancel
   * @return the canceled {@link PaymentIntent} from Stripe
   * @throws StripeException if an error occurs during communication with the Stripe API
   */
  public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
    Payment payment =
        paymentRepository
            .findByStripePaymentIntentId(paymentIntentId)
            .orElseThrow(
                () -> new IllegalArgumentException("Payment not found: " + paymentIntentId));
    validateOrganizationAccess(payment.getOrganizationId());
    PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(paymentIntentId);
    PaymentIntent canceledIntent = stripePaymentIntent.cancel();
    updatePaymentFromStripeIntent(payment, canceledIntent);
    auditService.logPaymentEvent(
        "PAYMENT_INTENT_CANCELED",
        paymentIntentId,
        "Payment intent canceled",
        Map.of("status", canceledIntent.getStatus()),
        "system",
        "PaymentService");
    logger.info("Canceled payment intent: {}", paymentIntentId);
    return canceledIntent;
  }

  @Transactional(readOnly = true)
  public Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId) {
    return paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId);
  }

  @Transactional(readOnly = true)
  public List<PaymentView> getOrganizationPayments(UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
        .map(PaymentView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PaymentView> getOrganizationPaymentsByStatus(
      UUID organizationId, Payment.Status status) {
    validateOrganizationAccess(organizationId);
    return paymentRepository
        .findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, status)
        .stream()
        .map(PaymentView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public PaymentStatistics getPaymentStatistics(UUID organizationId) {
    validateOrganizationAccess(organizationId);
    long totalPayments = paymentRepository.countSuccessfulPaymentsByOrganization(organizationId);
    Long totalAmountCents =
        paymentRepository.sumSuccessfulPaymentAmountsByOrganization(organizationId);
    Money totalAmount =
        totalAmountCents != null
            ? new Money(BigDecimal.valueOf(totalAmountCents, 2), "USD")
            : Money.ZERO;
    Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
    Long recentAmountCents =
        paymentRepository.sumSuccessfulPaymentAmountsByOrganizationAndDateRange(
            organizationId, thirtyDaysAgo, Instant.now());
    Money recentAmount =
        recentAmountCents != null
            ? new Money(BigDecimal.valueOf(recentAmountCents, 2), "USD")
            : Money.ZERO;
    return new PaymentStatistics(totalPayments, totalAmount, recentAmount);
  }

  /**
   * Attaches a new payment method to an organization's Stripe customer profile.
   *
   * @param organizationId the ID of the organization
   * @param stripePaymentMethodId the ID of the Stripe PaymentMethod to attach
   * @return the newly created local {@link PaymentMethod} record
   * @throws StripeException if an error occurs during communication with the Stripe API
   */
  public PaymentMethod attachPaymentMethod(UUID organizationId, String stripePaymentMethodId)
      throws StripeException {
    validateOrganizationAccess(organizationId);
    Organization organization =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Organization not found: " + organizationId));


    // Get or create Stripe customer
    String customerId = stripeCustomerService.getOrCreateCustomer(organization);

    // Attach payment method to customer

    com.stripe.model.PaymentMethod stripePaymentMethod =
        com.stripe.model.PaymentMethod.retrieve(stripePaymentMethodId);
    stripePaymentMethod.attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build());
    PaymentMethod.Type type = mapStripePaymentMethodType(stripePaymentMethod.getType());
    PaymentMethod paymentMethod = new PaymentMethod(organizationId, stripePaymentMethodId, type);
    if (type == PaymentMethod.Type.CARD && stripePaymentMethod.getCard() != null) {
      com.stripe.model.PaymentMethod.Card card = stripePaymentMethod.getCard();
      paymentMethod.updateCardDetails(
          card.getLast4(),
          card.getBrand(),
          Math.toIntExact(card.getExpMonth()),
          Math.toIntExact(card.getExpYear()));
    }
    if (stripePaymentMethod.getBillingDetails() != null) {
      com.stripe.model.PaymentMethod.BillingDetails billing =
          stripePaymentMethod.getBillingDetails();
      PaymentMethod.BillingAddress address = null;
      if (billing.getAddress() != null) {
        com.stripe.model.Address stripeAddress = billing.getAddress();
        address =
            new PaymentMethod.BillingAddress(
                stripeAddress.getLine1(),
                stripeAddress.getCity(),
                stripeAddress.getState(),
                stripeAddress.getPostalCode(),
                stripeAddress.getCountry());
        if (stripeAddress.getLine2() != null) {
          address.setAddressLine2(stripeAddress.getLine2());
        }
      }
      paymentMethod.updateBillingDetails(billing.getName(), billing.getEmail(), address);
    }
    long existingCount =
        paymentMethodRepository.countActivePaymentMethodsByOrganization(organizationId);
    if (existingCount == 0) {
      paymentMethod.markAsDefault();
    }
    PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
    auditService.logEvent(
        "PAYMENT_METHOD_ATTACHED",
        "PAYMENT_METHOD",
        stripePaymentMethodId,
        "Payment method attached to organization",
        Map.of(
            "payment_method_type",
            (Object) type.toString(),
            "is_default",
            (Object) String.valueOf(savedPaymentMethod.isDefault()),
            "last_four",
            (Object)
                (savedPaymentMethod.getLastFour() != null
                    ? savedPaymentMethod.getLastFour()
                    : "N/A")),
        null,
        "system",
        "PaymentService",
        null);
    logger.info(
        "Attached payment method: {} to organization: {}", stripePaymentMethodId, organizationId);
    return savedPaymentMethod;
  }

  /**
   * Detaches a payment method from a customer's profile in Stripe and marks it as deleted locally.
   *
   * @param organizationId the ID of the organization
   * @param paymentMethodId the internal ID of the payment method to detach
   * @throws StripeException if an error occurs during communication with the Stripe API
   */
  public void detachPaymentMethod(UUID organizationId, UUID paymentMethodId)
      throws StripeException {
    validateOrganizationAccess(organizationId);
    PaymentMethod paymentMethod =
        paymentMethodRepository
            .findById(paymentMethodId)
            .orElseThrow(
                () -> new IllegalArgumentException("Payment method not found: " + paymentMethodId));
    if (!paymentMethod.getOrganizationId().equals(organizationId)) {
      throw new SecurityException(
          "Access denied - payment method belongs to different organization");
    }
    com.stripe.model.PaymentMethod stripePaymentMethod =
        com.stripe.model.PaymentMethod.retrieve(paymentMethod.getStripePaymentMethodId());
    stripePaymentMethod.detach();
    paymentMethod.markAsDeleted();
    paymentMethodRepository.save(paymentMethod);
    if (paymentMethod.isDefault()) {
      setNewDefaultPaymentMethod(organizationId);
    }
    logger.info(
        "Detached payment method: {} from organization: {}", paymentMethodId, organizationId);
  }

  /**
   * Sets a payment method as the default for an organization.
   *
   * @param organizationId the ID of the organization
   * @param paymentMethodId the internal ID of the payment method to set as default
   * @return the updated {@link PaymentMethod} record
   */
  public PaymentMethod setDefaultPaymentMethod(UUID organizationId, UUID paymentMethodId) {
    validateOrganizationAccess(organizationId);
    PaymentMethod paymentMethod =
        paymentMethodRepository
            .findById(paymentMethodId)
            .orElseThrow(
                () -> new IllegalArgumentException("Payment method not found: " + paymentMethodId));
    if (!paymentMethod.getOrganizationId().equals(organizationId)) {
      throw new SecurityException(
          "Access denied - payment method belongs to different organization");
    }
    if (paymentMethod.isDeleted()) {
      throw new IllegalArgumentException("Cannot set deleted payment method as default");
    }
    paymentMethodRepository
        .findByOrganizationIdAndIsDefaultTrueAndDeletedAtIsNull(organizationId)
        .ifPresent(
            current -> {
              current.unmarkAsDefault();
              paymentMethodRepository.save(current);
            });
    paymentMethod.markAsDefault();
    PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
    logger.info(
        "Set payment method: {} as default for organization: {}", paymentMethodId, organizationId);
    return savedPaymentMethod;
  }

  /**
   * Updates the billing details of a payment method.
   *
   * @param organizationId the ID of the organization
   * @param paymentMethodId the internal ID of the payment method
   * @param displayName the new display name
   * @param billingName the new billing name
   * @param billingEmail the new billing email
   * @param billingAddress the new billing address
   * @return the updated {@link PaymentMethod} record
   */
  public PaymentMethod updatePaymentMethod(
      UUID organizationId,
      UUID paymentMethodId,
      String displayName,
      String billingName,
      String billingEmail,
      PaymentMethod.BillingAddress billingAddress) {
    validateOrganizationAccess(organizationId);
    PaymentMethod paymentMethod =
        paymentMethodRepository
            .findById(paymentMethodId)
            .orElseThrow(
                () -> new IllegalArgumentException("Payment method not found: " + paymentMethodId));
    if (!paymentMethod.getOrganizationId().equals(organizationId)) {
      throw new SecurityException(
          "Access denied - payment method belongs to different organization");
    }
    if (paymentMethod.isDeleted()) {
      throw new IllegalArgumentException("Cannot update deleted payment method");
    }
    if (billingName != null || billingEmail != null || billingAddress != null) {
      paymentMethod.updateBillingDetails(billingName, billingEmail, billingAddress);
    }
    return paymentMethodRepository.save(paymentMethod);
  }

  @Transactional(readOnly = true)
  public List<PaymentMethodView> getOrganizationPaymentMethods(UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return paymentMethodRepository
        .findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(organizationId)
        .stream()
        .map(PaymentMethodView::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PaymentView> getOrganizationPaymentsByStatus(UUID organizationId, String status) {
    Payment.Status s = Payment.Status.valueOf(status.toUpperCase());
    return getOrganizationPaymentsByStatus(organizationId, s);
  }

  /**
   * Processes an incoming webhook event from Stripe.
   *
   * @param stripeEventId the unique ID of the Stripe event
   * @param eventType the type of the event (e.g., "payment_intent.succeeded")
   * @param eventData a map containing the event payload
   */
  public void processWebhookEvent(
      String stripeEventId, String eventType, Map<String, Object> eventData) {
    logger.info("Processing Stripe webhook: {} of type: {}", stripeEventId, eventType);
    switch (eventType) {
      case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(eventData);
      case "payment_intent.payment_failed" -> handlePaymentIntentFailed(eventData);
      case "payment_intent.canceled" -> handlePaymentIntentCanceled(eventData);
      case "payment_method.attached" -> handlePaymentMethodAttached(eventData);
      case "payment_method.detached" -> handlePaymentMethodDetached(eventData);
      default -> logger.debug("Unhandled webhook event type: {}", eventType);
    }
  }

  private String getOrCreateStripeCustomer(Organization organization) throws StripeException {
    CustomerSearchParams searchParams =
        CustomerSearchParams.builder()
            .setQuery("metadata['organization_id']:'" + organization.getId() + "'")
            .build();
    CustomerSearchResult searchResult = Customer.search(searchParams);
    if (!searchResult.getData().isEmpty()) {
      return searchResult.getData().get(0).getId();
    }
    CustomerCreateParams params =
        CustomerCreateParams.builder()
            .setName(organization.getName())
            .setDescription("Customer for organization: " + organization.getName())
            .putMetadata("organization_id", organization.getId().toString())
            .build();
    Customer customer = Customer.create(params);
    return customer.getId();
  }

  // Helper methods


  private PaymentMethod.Type mapStripePaymentMethodType(String stripeType) {
    return switch (stripeType) {
      case "card" -> PaymentMethod.Type.CARD;
      case "us_bank_account" -> PaymentMethod.Type.BANK_ACCOUNT;
      case "sepa_debit" -> PaymentMethod.Type.SEPA_DEBIT;
      case "ach_debit" -> PaymentMethod.Type.ACH_DEBIT;
      default -> PaymentMethod.Type.CARD;
    };
  }

  private void updatePaymentFromStripeIntent(Payment payment, PaymentIntent stripeIntent) {
    Payment.Status newStatus =
        switch (stripeIntent.getStatus()) {
          case "succeeded" -> Payment.Status.SUCCEEDED;
          case "processing" -> Payment.Status.PROCESSING;
          case "requires_payment_method", "requires_confirmation", "requires_action" -> Payment
              .Status.PENDING;
          case "canceled" -> Payment.Status.CANCELED;
          default -> Payment.Status.FAILED;
        };
    payment.updateStatus(newStatus);
    paymentRepository.save(payment);
  }

  private void setNewDefaultPaymentMethod(UUID organizationId) {
    List<PaymentMethod> paymentMethods =
        paymentMethodRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            organizationId);
    if (!paymentMethods.isEmpty()) {
      PaymentMethod newDefault = paymentMethods.get(0);
      newDefault.markAsDefault();
      paymentMethodRepository.save(newDefault);
    }
  }

  private void handlePaymentIntentSucceeded(Map<String, Object> eventData) {
    String paymentIntentId = (String) ((Map<?, ?>) eventData.get("object")).get("id");
    paymentRepository
        .findByStripePaymentIntentId(paymentIntentId)
        .ifPresent(
            payment -> {
              payment.updateStatus(Payment.Status.SUCCEEDED);
              paymentRepository.save(payment);
              auditService.logPaymentEvent(
                  "PAYMENT_SUCCEEDED",
                  paymentIntentId,
                  "Payment succeeded via webhook",
                  Map.of(
                      "amount",
                      payment.getAmount().getAmount().toString(),
                      "currency",
                      payment.getCurrency()),
                  "webhook",
                  "StripeWebhook");
              logger.info("Updated payment status to SUCCEEDED: {}", paymentIntentId);
            });
  }

  private void handlePaymentIntentFailed(Map<String, Object> eventData) {
    String paymentIntentId = (String) ((Map<?, ?>) eventData.get("object")).get("id");
    paymentRepository
        .findByStripePaymentIntentId(paymentIntentId)
        .ifPresent(
            payment -> {
              payment.updateStatus(Payment.Status.FAILED);
              paymentRepository.save(payment);
              auditService.logPaymentEvent(
                  "PAYMENT_FAILED",
                  paymentIntentId,
                  "Payment failed via webhook",
                  Map.of(
                      "amount",
                      payment.getAmount().getAmount().toString(),
                      "currency",
                      payment.getCurrency()),
                  "webhook",
                  "StripeWebhook");
              logger.info("Updated payment status to FAILED: {}", paymentIntentId);
            });
  }

  private void handlePaymentIntentCanceled(Map<String, Object> eventData) {
    String paymentIntentId = (String) ((Map<?, ?>) eventData.get("object")).get("id");
    paymentRepository
        .findByStripePaymentIntentId(paymentIntentId)
        .ifPresent(
            payment -> {
              payment.updateStatus(Payment.Status.CANCELED);
              paymentRepository.save(payment);
              logger.info("Updated payment status to CANCELED: {}", paymentIntentId);
            });
  }

  private void handlePaymentMethodAttached(Map<String, Object> eventData) {
    logger.debug("Payment method attached webhook received");
  }

  private void handlePaymentMethodDetached(Map<String, Object> eventData) {
    logger.debug("Payment method detached webhook received");
  }

  private void validateOrganizationAccess(UUID organizationId) {
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId == null) {
      throw new SecurityException("Authentication required");
    }
    if (!TenantContext.belongsToOrganization(organizationId)) {
      throw new SecurityException("Access denied to organization: " + organizationId);
    }
  }

  /**
   * A record to hold aggregated payment statistics.
   *
   * @param totalSuccessfulPayments the total count of successful payments
   * @param totalAmount the total monetary value of successful payments
   * @param recentAmount the monetary value of successful payments in a recent time window
   */
  public record PaymentStatistics(
      long totalSuccessfulPayments, Money totalAmount, Money recentAmount) {}
}
