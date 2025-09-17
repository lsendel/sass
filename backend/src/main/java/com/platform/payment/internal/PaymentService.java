package com.platform.payment.internal;

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

import com.platform.audit.internal.AuditService;
import com.platform.shared.security.TenantContext;
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

  public PaymentService(
      PaymentRepository paymentRepository,
      PaymentMethodRepository paymentMethodRepository,
      OrganizationRepository organizationRepository,
      ApplicationEventPublisher eventPublisher,
      AuditService auditService,
      @Value("${stripe.secret-key}") String stripeSecretKey) {
    this.paymentRepository = paymentRepository;
    this.paymentMethodRepository = paymentMethodRepository;
    this.organizationRepository = organizationRepository;
    this.eventPublisher = eventPublisher;
    this.auditService = auditService;

    Stripe.apiKey = stripeSecretKey;
  }

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
    String customerId = getOrCreateStripeCustomer(organization);

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
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build())
            .build();

    PaymentIntent stripePaymentIntent = PaymentIntent.create(params);

    // Create our payment record
    Payment payment =
        new Payment(organizationId, stripePaymentIntent.getId(), amount, currency, description);

    if (metadata != null) {
      payment.updateMetadata(metadata);
    }

    Payment savedPayment = paymentRepository.save(payment);

    // Audit log payment creation
    auditService.logPaymentEvent(
        "PAYMENT_INTENT_CREATED",
        stripePaymentIntent.getId(),
        "Payment intent created",
        Map.of(
            "amount", amount.getAmount().toString(),
            "currency", currency,
            "stripe_payment_intent_id", stripePaymentIntent.getId()),
        "system", // ipAddress - could be extracted from request context
        "PaymentService"); // userAgent - service identifier

    logger.info(
        "Created payment intent: {} for organization: {}",
        stripePaymentIntent.getId(),
        organizationId);

    return stripePaymentIntent;
  }

  public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId)
      throws StripeException {
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

    // Update payment status based on Stripe response
    updatePaymentFromStripeIntent(payment, confirmedIntent);

    // Audit log payment confirmation
    auditService.logPaymentEvent(
        "PAYMENT_INTENT_CONFIRMED",
        paymentIntentId,
        "Payment intent confirmed",
        Map.of(
            "payment_method_id", paymentMethodId,
            "status", confirmedIntent.getStatus(),
            "amount", payment.getAmount().getAmount().toString()),
        "system",
        "PaymentService");

    logger.info(
        "Confirmed payment intent: {} with status: {}",
        paymentIntentId,
        confirmedIntent.getStatus());
    return confirmedIntent;
  }

  @Transactional(readOnly = true)
  public Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId) {
    return paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId);
  }

  @Transactional(readOnly = true)
  public List<Payment> getOrganizationPayments(UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
  }

  @Transactional(readOnly = true)
  public List<Payment> getOrganizationPaymentsByStatus(UUID organizationId, Payment.Status status) {
    validateOrganizationAccess(organizationId);
    return paymentRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(
        organizationId, status);
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

  public PaymentMethod attachPaymentMethod(UUID organizationId, String stripePaymentMethodId)
      throws StripeException {
    validateOrganizationAccess(organizationId);

    Organization organization =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Organization not found: " + organizationId));

    // Get or create Stripe customer
    String customerId = getOrCreateStripeCustomer(organization);

    // Attach payment method to customer
    com.stripe.model.PaymentMethod stripePaymentMethod =
        com.stripe.model.PaymentMethod.retrieve(stripePaymentMethodId);
    stripePaymentMethod.attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build());

    // Create our payment method record
    PaymentMethod.Type type = mapStripePaymentMethodType(stripePaymentMethod.getType());
    PaymentMethod paymentMethod = new PaymentMethod(organizationId, stripePaymentMethodId, type);

    // Update card details if it's a card
    if (type == PaymentMethod.Type.CARD && stripePaymentMethod.getCard() != null) {
      com.stripe.model.PaymentMethod.Card card = stripePaymentMethod.getCard();
      paymentMethod.updateCardDetails(
          card.getLast4(),
          card.getBrand(),
          Math.toIntExact(card.getExpMonth()),
          Math.toIntExact(card.getExpYear()));
    }

    // Update billing details
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

    // Set as default if it's the first payment method
    long existingCount =
        paymentMethodRepository.countActivePaymentMethodsByOrganization(organizationId);
    if (existingCount == 0) {
      paymentMethod.markAsDefault();
    }

    PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);

    // Audit log payment method attachment
    auditService.logEvent(
        "PAYMENT_METHOD_ATTACHED",
        "PAYMENT_METHOD",
        stripePaymentMethodId,
        "Payment method attached to organization",
        Map.of(
            "payment_method_type", (Object) type.toString(),
            "is_default", (Object) String.valueOf(savedPaymentMethod.isDefault()),
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

    // Detach from Stripe
    com.stripe.model.PaymentMethod stripePaymentMethod =
        com.stripe.model.PaymentMethod.retrieve(paymentMethod.getStripePaymentMethodId());
    stripePaymentMethod.detach();

    // Mark as deleted
    paymentMethod.markAsDeleted();
    paymentMethodRepository.save(paymentMethod);

    // If this was the default, set another as default
    if (paymentMethod.isDefault()) {
      setNewDefaultPaymentMethod(organizationId);
    }

    logger.info(
        "Detached payment method: {} from organization: {}", paymentMethodId, organizationId);
  }

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

    // Unmark current default
    paymentMethodRepository
        .findByOrganizationIdAndIsDefaultTrueAndDeletedAtIsNull(organizationId)
        .ifPresent(
            current -> {
              current.unmarkAsDefault();
              paymentMethodRepository.save(current);
            });

    // Set new default
    paymentMethod.markAsDefault();
    PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);

    logger.info(
        "Set payment method: {} as default for organization: {}", paymentMethodId, organizationId);
    return savedPaymentMethod;
  }

  @Transactional(readOnly = true)
  public List<PaymentMethod> getOrganizationPaymentMethods(UUID organizationId) {
    validateOrganizationAccess(organizationId);
    return paymentMethodRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        organizationId);
  }

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

  // Helper methods

  private String getOrCreateStripeCustomer(Organization organization) throws StripeException {
    // In a real implementation, you might store the Stripe customer ID on the organization
    // For now, we'll create a new customer each time or search for existing
    CustomerSearchParams searchParams =
        CustomerSearchParams.builder()
            .setQuery("metadata['organization_id']:'" + organization.getId() + "'")
            .build();

    CustomerSearchResult searchResult = Customer.search(searchParams);

    if (!searchResult.getData().isEmpty()) {
      return searchResult.getData().get(0).getId();
    }

    // Create new customer
    CustomerCreateParams params =
        CustomerCreateParams.builder()
            .setName(organization.getName())
            .setDescription("Customer for organization: " + organization.getName())
            .putMetadata("organization_id", organization.getId().toString())
            .build();

    Customer customer = Customer.create(params);
    return customer.getId();
  }

  private PaymentMethod.Type mapStripePaymentMethodType(String stripeType) {
    return switch (stripeType) {
      case "card" -> PaymentMethod.Type.CARD;
      case "us_bank_account" -> PaymentMethod.Type.BANK_ACCOUNT;
      case "sepa_debit" -> PaymentMethod.Type.SEPA_DEBIT;
      case "ach_debit" -> PaymentMethod.Type.ACH_DEBIT;
      default -> PaymentMethod.Type.CARD; // Default fallback
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

              // Audit log successful payment
              auditService.logPaymentEvent(
                  "PAYMENT_SUCCEEDED",
                  paymentIntentId,
                  "Payment succeeded via webhook",
                  Map.of(
                      "amount", payment.getAmount().getAmount().toString(),
                      "currency", payment.getCurrency()),
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

              // Audit log failed payment - important for fraud detection
              auditService.logPaymentEvent(
                  "PAYMENT_FAILED",
                  paymentIntentId,
                  "Payment failed via webhook",
                  Map.of(
                      "amount", payment.getAmount().getAmount().toString(),
                      "currency", payment.getCurrency()),
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
    // Additional organization access validation would go here
    // For now, we assume if user is authenticated, they have access
  }

  public record PaymentStatistics(
      long totalSuccessfulPayments, Money totalAmount, Money recentAmount) {}
}
