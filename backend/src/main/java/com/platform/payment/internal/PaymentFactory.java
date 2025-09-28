package com.platform.payment.internal;

import com.platform.payment.api.PaymentRequest;
import com.platform.shared.security.TenantContext;
import com.platform.shared.types.Money;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * A factory for creating {@link Payment} entities.
 *
 * <p>This class implements the Factory pattern to encapsulate the complex logic of creating {@code
 * Payment} objects. It ensures that all created payments are properly validated and initialized with
 * default values and contextual information, such as the current user from {@link TenantContext}.
 */
@Component
public class PaymentFactory {

  private final PaymentValidator paymentValidator;

  /**
   * Constructs the factory with a {@link PaymentValidator}.
   *
   * @param paymentValidator the validator to use for payment requests
   */
  public PaymentFactory(PaymentValidator paymentValidator) {
    this.paymentValidator = paymentValidator;
  }

  /**
   * Creates a new {@link Payment} entity from a {@link PaymentRequest}.
   *
   * @param request the payment request DTO containing payment details
   * @param stripePaymentIntentId the ID of the corresponding Stripe PaymentIntent
   * @return a new, initialized {@link Payment} entity
   * @throws IllegalArgumentException if the request or Stripe ID is invalid
   */
  public Payment createPayment(PaymentRequest request, String stripePaymentIntentId) {
    paymentValidator.validatePaymentRequest(request);
    if (stripePaymentIntentId == null || stripePaymentIntentId.isBlank()) {
      throw new IllegalArgumentException("Stripe payment intent ID is required");
    }

    Payment payment =
        new Payment(
            request.organizationId(),
            stripePaymentIntentId,
            request.amount(),
            request.currency(),
            request.description());

    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId != null) {
      payment.assignUser(currentUserId);
    }

    if (request.hasMetadata()) {
      payment.updateMetadata(request.metadata());
    }

    payment.updateStatus(Payment.Status.PENDING);
    return payment;
  }

  /**
   * Creates a new {@link Payment} entity to represent a refund.
   *
   * @param originalPayment the original payment that is being refunded
   * @param refundAmount the amount to be refunded
   * @param reason the reason for the refund
   * @param stripeRefundId the ID of the corresponding Stripe Refund object
   * @return a new {@link Payment} entity representing the refund transaction
   * @throws IllegalArgumentException if the original payment or refund amount is invalid
   */
  public Payment createRefundPayment(
      Payment originalPayment, Money refundAmount, String reason, String stripeRefundId) {
    if (originalPayment == null) {
      throw new IllegalArgumentException("Original payment is required for refund");
    }
    if (refundAmount == null || refundAmount.isNegative() || refundAmount.isZero()) {
      throw new IllegalArgumentException("Refund amount must be positive");
    }
    if (refundAmount.isGreaterThan(originalPayment.getAmount())) {
      throw new IllegalArgumentException("Refund amount cannot exceed original payment amount");
    }

    Payment refundPayment =
        new Payment(
            originalPayment.getOrganizationId(),
            stripeRefundId,
            refundAmount.negate(), // Negative amount for refunds
            originalPayment.getCurrency(),
            "Refund: " + (reason != null ? reason : "Customer request"));

    Map<String, String> metadata =
        Map.of(
            "refund_reason",
            reason != null ? reason : "Customer request",
            "original_payment_id",
            originalPayment.getId().toString(),
            "refund_type",
            "partial");
    refundPayment.updateMetadata(metadata);

    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId != null) {
      refundPayment.assignUser(currentUserId);
    }

    refundPayment.updateStatus(Payment.Status.PROCESSING);
    return refundPayment;
  }

  /**
   * Creates a new {@link Payment} entity for a subscription billing event.
   *
   * @param organizationId the ID of the organization being billed
   * @param subscriptionId the ID of the subscription this payment is for
   * @param amount the billing amount
   * @param billingPeriod a description of the billing period (e.g., "Jan 2023")
   * @param stripeInvoiceId the ID of the corresponding Stripe Invoice
   * @return a new {@link Payment} entity for the subscription payment
   * @throws IllegalArgumentException if the input parameters are invalid
   */
  public Payment createSubscriptionPayment(
      UUID organizationId,
      String subscriptionId,
      Money amount,
      String billingPeriod,
      String stripeInvoiceId) {
    paymentValidator.validateOrganizationAccess(organizationId);
    if (subscriptionId == null || subscriptionId.isBlank()) {
      throw new IllegalArgumentException("Subscription ID is required");
    }
    if (amount == null || amount.isNegative() || amount.isZero()) {
      throw new IllegalArgumentException("Billing amount must be positive");
    }

    Payment payment =
        new Payment(
            organizationId,
            stripeInvoiceId,
            amount,
            "USD", // Default currency for subscriptions
            "Subscription billing: " + billingPeriod);

    Map<String, String> metadata =
        Map.of(
            "payment_type",
            "subscription",
            "subscription_id",
            subscriptionId,
            "billing_period",
            billingPeriod != null ? billingPeriod : "monthly");
    payment.updateMetadata(metadata);

    payment.updateStatus(Payment.Status.PENDING);
    return payment;
  }

  /**
   * Creates a new {@link PaymentBuilder} instance for fluently constructing a {@code Payment}.
   *
   * @return a new instance of the builder
   */
  public PaymentBuilder builder() {
    return new PaymentBuilder(this);
  }

  /** A builder class for creating complex {@link Payment} instances with a fluent API. */
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

    /**
     * Builds and returns the final {@link Payment} object.
     *
     * @return the constructed {@link Payment} instance
     */
    public Payment build() {
      PaymentRequest request =
          new PaymentRequest(organizationId, amount, currency, description, metadata);
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