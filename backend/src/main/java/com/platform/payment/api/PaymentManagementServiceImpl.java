package com.platform.payment.api;

import com.platform.payment.api.PaymentDto.BillingAddress;
import com.platform.payment.api.PaymentDto.BillingDetails;
import com.platform.payment.api.PaymentDto.CardDetails;
import com.platform.payment.api.PaymentDto.ConfirmPaymentIntentRequest;
import com.platform.payment.api.PaymentDto.CreatePaymentIntentRequest;
import com.platform.payment.api.PaymentDto.PaymentIntentResponse;
import com.platform.payment.api.PaymentDto.PaymentMethodResponse;
import com.platform.payment.api.PaymentDto.PaymentResponse;
import com.platform.payment.api.PaymentDto.PaymentStatisticsResponse;
import com.platform.payment.api.PaymentDto.PaymentStatus;
import com.platform.payment.internal.Payment;
import com.platform.payment.internal.PaymentMethod;
import com.platform.payment.internal.PaymentMethodView;
import com.platform.payment.internal.PaymentService;
import com.platform.payment.internal.PaymentView;
import com.stripe.exception.StripeException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Implements the {@link PaymentManagementService} interface, acting as a bridge between the API
 * layer and the internal payment service.
 *
 * <p>This service is responsible for translating API-level Data Transfer Objects (DTOs) into
 * internal domain objects and vice versa. It orchestrates calls to the {@link PaymentService} to
 * perform the core business logic.
 * </p>
 */
@Service
public class PaymentManagementServiceImpl implements PaymentManagementService {

  private final PaymentService paymentService;

  /**
   * Constructs the service with the internal {@link PaymentService}.
   *
   * @param paymentService The core service for handling payment logic.
   */
  public PaymentManagementServiceImpl(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @Override
  public PaymentResponse createPayment(
      UUID organizationId,
      BigDecimal amount,
      String currency,
      String paymentMethodId,
      String description,
      boolean confirm)
      throws StripeException {
    var money = new com.platform.shared.types.Money(amount, currency);
    var paymentIntent =
        paymentService.createPaymentIntent(organizationId, money, currency, description, null);

    if (confirm) {
      if (paymentMethodId == null || paymentMethodId.isBlank()) {
        throw new IllegalArgumentException("paymentMethodId is required when confirm is true");
      }
      paymentIntent =
          paymentService.confirmPaymentIntent(paymentIntent.getId(), paymentMethodId);
    }

    return paymentService
        .findByStripePaymentIntentId(paymentIntent.getId())
        .map(PaymentDtoMapper::toView)
        .map(this::mapToPaymentResponse)
        .orElseThrow(() -> new IllegalStateException("Payment record missing for intent"));
  }

  @Override
  public PaymentResponse confirmPayment(UUID organizationId, String paymentIntentId)
      throws StripeException {
    var defaultMethod =
        paymentService.getOrganizationPaymentMethods(organizationId).stream()
            .filter(PaymentMethodView::isDefault)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException("No default payment method configured for organization"));

    paymentService.confirmPaymentIntent(paymentIntentId, defaultMethod.stripePaymentMethodId());

    return paymentService
        .findByStripePaymentIntentId(paymentIntentId)
        .map(PaymentDtoMapper::toView)
        .map(this::mapToPaymentResponse)
        .orElseThrow(() -> new IllegalStateException("Payment record missing for intent"));
  }

  @Override
  public PaymentResponse cancelPayment(UUID organizationId, String paymentIntentId)
      throws StripeException {
    paymentService.cancelPaymentIntent(paymentIntentId);
    return paymentService
        .findByStripePaymentIntentId(paymentIntentId)
        .map(PaymentDtoMapper::toView)
        .map(this::mapToPaymentResponse)
        .orElseThrow(() -> new IllegalStateException("Payment record missing for intent"));
  }

  @Override
  public List<PaymentResponse> getOrganizationPayments(UUID organizationId) {
    return paymentService.getOrganizationPayments(organizationId).stream()
        .map(this::mapToPaymentResponse)
        .toList();
  }

  @Override
  public List<PaymentResponse> getOrganizationPaymentsByStatus(
      UUID organizationId, String status) {
    return paymentService.getOrganizationPaymentsByStatus(organizationId, status).stream()
        .map(this::mapToPaymentResponse)
        .toList();
  }

  @Override
  public PaymentMethodResponse attachPaymentMethod(
      UUID organizationId, String stripePaymentMethodId) throws StripeException {
    PaymentMethod paymentMethod =
        paymentService.attachPaymentMethod(organizationId, stripePaymentMethodId);
    return mapToPaymentMethodResponse(paymentMethod);
  }

  @Override
  public PaymentMethodResponse setDefaultPaymentMethod(UUID organizationId, UUID paymentMethodId) {
    PaymentMethod paymentMethod =
        paymentService.setDefaultPaymentMethod(organizationId, paymentMethodId);
    return mapToPaymentMethodResponse(paymentMethod);
  }

  @Override
  public void detachPaymentMethod(UUID organizationId, UUID paymentMethodId)
      throws StripeException {
    paymentService.detachPaymentMethod(organizationId, paymentMethodId);
  }

  @Override
  public PaymentMethodResponse updatePaymentMethod(
      UUID organizationId,
      UUID paymentMethodId,
      String displayName,
      BillingDetails billingDetails) {
    PaymentMethod.BillingAddress internalAddress =
        billingDetails != null ? mapToBillingAddress(billingDetails) : null;

    PaymentMethod paymentMethod =
        paymentService.updatePaymentMethod(
            organizationId,
            paymentMethodId,
            displayName,
            billingDetails != null ? billingDetails.name() : null,
            billingDetails != null ? billingDetails.email() : null,
            internalAddress);

    return mapToPaymentMethodResponse(paymentMethod);
  }

  @Override
  public List<PaymentMethodResponse> getOrganizationPaymentMethods(UUID organizationId) {
    return paymentService.getOrganizationPaymentMethods(organizationId).stream()
        .map(this::mapToPaymentMethodViewResponse)
        .toList();
  }

  @Override
  public PaymentStatisticsResponse getPaymentStatistics(UUID organizationId) {
    PaymentService.PaymentStatistics stats = paymentService.getPaymentStatistics(organizationId);
    return new PaymentStatisticsResponse(
        stats.totalSuccessfulPayments(),
        stats.totalAmount().getAmount(),
        stats.recentAmount().getAmount());
  }

  @Override
  public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request)
      throws StripeException {
    var money = new com.platform.shared.types.Money(request.amount(), request.currency());
    var intent =
        paymentService.createPaymentIntent(
            request.organizationId(),
            money,
            request.currency(),
            request.description(),
            request.metadata());
    return PaymentIntentResponse.fromStripePaymentIntent(intent);
  }

  @Override
  public PaymentIntentResponse confirmPaymentIntent(
      UUID organizationId, String paymentIntentId, ConfirmPaymentIntentRequest request)
      throws StripeException {
    if (request.paymentMethodId() == null || request.paymentMethodId().isBlank()) {
      throw new IllegalArgumentException("paymentMethodId is required");
    }
    var intent = paymentService.confirmPaymentIntent(paymentIntentId, request.paymentMethodId());
    return PaymentIntentResponse.fromStripePaymentIntent(intent);
  }

  @Override
  public PaymentIntentResponse cancelPaymentIntent(UUID organizationId, String paymentIntentId)
      throws StripeException {
    var intent = paymentService.cancelPaymentIntent(paymentIntentId);
    return PaymentIntentResponse.fromStripePaymentIntent(intent);
  }

  /**
   * Maps an internal {@link PaymentView} to an API-level {@link PaymentResponse} DTO.
   *
   * @param payment The internal payment view object.
   * @return The corresponding payment response DTO.
   */
  private PaymentResponse mapToPaymentResponse(PaymentView payment) {
    return new PaymentResponse(
        payment.id(),
        payment.organizationId(),
        payment.stripePaymentIntentId(),
        payment.amount(),
        payment.currency(),
        mapToApiPaymentStatus(payment.status()),
        null, // PaymentView doesn't have paymentMethodId field
        payment.description(),
        payment.createdAt());
  }

  /**
   * Maps an internal {@link PaymentMethod} entity to an API-level {@link PaymentMethodResponse}
   * DTO.
   *
   * @param paymentMethod The internal payment method entity.
   * @return The corresponding payment method response DTO.
   */
  private PaymentMethodResponse mapToPaymentMethodResponse(PaymentMethod paymentMethod) {
    return mapToPaymentMethodViewResponse(PaymentMethodView.fromEntity(paymentMethod));
  }

  /**
   * Maps an internal {@link PaymentMethodView} to an API-level {@link PaymentMethodResponse} DTO.
   *
   * @param paymentMethod The internal payment method view object.
   * @return The corresponding payment method response DTO.
   */
  private PaymentMethodResponse mapToPaymentMethodViewResponse(PaymentMethodView paymentMethod) {
    CardDetails cardDetails = null;
    if (paymentMethod.type() == PaymentMethod.Type.CARD) {
      cardDetails =
          new CardDetails(
              paymentMethod.lastFour(),
              paymentMethod.brand(),
              paymentMethod.expMonth(),
              paymentMethod.expYear());
    }

    BillingDetails billingDetails = null;
    if (paymentMethod.billingName() != null
        || paymentMethod.billingEmail() != null
        || paymentMethod.billingAddress() != null) {
      BillingAddress address = null;
      if (paymentMethod.billingAddress() != null) {
        var addr = paymentMethod.billingAddress();
        address =
            new BillingAddress(
                addr.getAddressLine1(),
                addr.getAddressLine2(),
                addr.getCity(),
                addr.getState(),
                addr.getPostalCode(),
                addr.getCountry());
      }
      billingDetails =
          new BillingDetails(paymentMethod.billingName(), paymentMethod.billingEmail(), address);
    }

    return new PaymentMethodResponse(
        paymentMethod.id(),
        paymentMethod.organizationId(),
        paymentMethod.stripePaymentMethodId(),
        paymentMethod.type().name(),
        paymentMethod.isDefault(),
        paymentMethod.displayName(),
        cardDetails,
        billingDetails,
        paymentMethod.createdAt());
  }

  /**
   * Maps an API-level {@link BillingDetails} DTO to an internal {@link
   * PaymentMethod.BillingAddress} object.
   *
   * @param billingDetails The API DTO for billing details.
   * @return The internal billing address object, or null if the input address is null.
   */
  private PaymentMethod.BillingAddress mapToBillingAddress(BillingDetails billingDetails) {
    if (billingDetails.address() == null) {
      return null;
    }
    var addr = billingDetails.address();
    return new PaymentMethod.BillingAddress(
        addr.addressLine1(), addr.city(), addr.state(), addr.postalCode(), addr.country());
  }

  /**
   * Maps an internal payment status string to the public API {@link PaymentStatus} enum.
   *
   * @param internalStatus The status string from the internal domain.
   * @return The corresponding public API {@link PaymentStatus}.
   */
  private PaymentStatus mapToApiPaymentStatus(String internalStatus) {
    try {
      Payment.Status status = Payment.Status.valueOf(internalStatus);
      return switch (status) {
        case SUCCEEDED -> PaymentStatus.SUCCEEDED;
        case FAILED -> PaymentStatus.FAILED;
        case CANCELED -> PaymentStatus.CANCELED;
        case PROCESSING, REQUIRES_CAPTURE -> PaymentStatus.PROCESSING;
        case PENDING -> PaymentStatus.PENDING;
        case REQUIRES_PAYMENT_METHOD, REQUIRES_CONFIRMATION, REQUIRES_ACTION ->
            PaymentStatus.REQUIRES_ACTION;
      };
    } catch (IllegalArgumentException e) {
      return PaymentStatus.PENDING;
    }
  }

  /** A private static helper class for mapping Payment entities to views. */
  private static final class PaymentDtoMapper {

    private PaymentDtoMapper() {}

    static PaymentView toView(Payment payment) {
      return PaymentView.fromEntity(payment);
    }
  }
}
