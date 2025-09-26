package com.platform.payment.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.platform.payment.api.PaymentDto.BillingAddress;
import com.platform.payment.api.PaymentDto.BillingDetails;
import com.platform.payment.api.PaymentDto.CardDetails;
import com.platform.payment.api.PaymentDto.ConfirmPaymentIntentRequest;
import com.platform.payment.api.PaymentDto.CreatePaymentIntentRequest;
import com.platform.payment.api.PaymentDto.PaymentMethodResponse;
import com.platform.payment.api.PaymentDto.PaymentResponse;
import com.platform.payment.api.PaymentDto.PaymentStatisticsResponse;
import com.platform.payment.api.PaymentDto.PaymentStatus;
import com.platform.payment.api.PaymentDto.PaymentIntentResponse;
import com.platform.payment.internal.Payment;
import com.platform.payment.internal.PaymentService;
import com.platform.payment.internal.PaymentView;
import com.platform.payment.internal.PaymentMethod;
import com.platform.payment.internal.PaymentMethodView;
import com.stripe.exception.StripeException;

/**
 * Implementation of PaymentManagementService that bridges the API and internal layers.
 * This service converts between internal entities and API DTOs.
 */
@Service
public class PaymentManagementServiceImpl implements PaymentManagementService {

  private final PaymentService paymentService;

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
      boolean confirm) throws StripeException {
    var money = new com.platform.shared.types.Money(amount, currency);
    var paymentIntent = paymentService.createPaymentIntent(
        organizationId, money, currency, description, null);

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
  public PaymentResponse confirmPayment(UUID organizationId, String paymentIntentId) throws StripeException {
    var defaultMethod =
        paymentService.getOrganizationPaymentMethods(organizationId).stream()
            .filter(PaymentMethodView::isDefault)
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException(
                    "No default payment method configured for organization"));

    paymentService.confirmPaymentIntent(paymentIntentId, defaultMethod.stripePaymentMethodId());

    return paymentService
        .findByStripePaymentIntentId(paymentIntentId)
        .map(PaymentDtoMapper::toView)
        .map(this::mapToPaymentResponse)
        .orElseThrow(() -> new IllegalStateException("Payment record missing for intent"));
  }

  @Override
  public PaymentResponse cancelPayment(UUID organizationId, String paymentIntentId) throws StripeException {
    paymentService.cancelPaymentIntent(paymentIntentId);
    return paymentService
        .findByStripePaymentIntentId(paymentIntentId)
        .map(PaymentDtoMapper::toView)
        .map(this::mapToPaymentResponse)
        .orElseThrow(() -> new IllegalStateException("Payment record missing for intent"));
  }

  @Override
  public List<PaymentResponse> getOrganizationPayments(UUID organizationId) {
    return paymentService.getOrganizationPayments(organizationId)
        .stream()
        .map(this::mapToPaymentResponse)
        .toList();
  }

  @Override
  public List<PaymentResponse> getOrganizationPaymentsByStatus(UUID organizationId, String status) {
    return paymentService.getOrganizationPaymentsByStatus(organizationId, status).stream()
        .map(this::mapToPaymentResponse)
        .toList();
  }

  @Override
  public PaymentMethodResponse attachPaymentMethod(UUID organizationId, String stripePaymentMethodId) throws StripeException {
    PaymentMethod paymentMethod = paymentService.attachPaymentMethod(organizationId, stripePaymentMethodId);
    return mapToPaymentMethodResponse(paymentMethod);
  }

  @Override
  public PaymentMethodResponse setDefaultPaymentMethod(UUID organizationId, UUID paymentMethodId) {
    PaymentMethod paymentMethod = paymentService.setDefaultPaymentMethod(organizationId, paymentMethodId);
    return mapToPaymentMethodResponse(paymentMethod);
  }

  @Override
  public void detachPaymentMethod(UUID organizationId, UUID paymentMethodId) throws StripeException {
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
    return paymentService.getOrganizationPaymentMethods(organizationId)
        .stream()
        .map(this::mapToPaymentMethodViewResponse)
        .toList();
  }

  @Override
  public PaymentStatisticsResponse getPaymentStatistics(UUID organizationId) {
    PaymentService.PaymentStatistics stats = paymentService.getPaymentStatistics(organizationId);
    return new PaymentStatisticsResponse(
        stats.totalSuccessfulPayments(),
        stats.totalAmount().getAmount(),
        stats.recentAmount().getAmount()
    );
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
   * Maps internal PaymentView to API PaymentResponse DTO.
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
        payment.createdAt()
    );
  }

  /**
   * Maps internal PaymentMethod entity to API PaymentMethodResponse DTO.
   */
  private PaymentMethodResponse mapToPaymentMethodResponse(PaymentMethod paymentMethod) {
    return mapToPaymentMethodViewResponse(PaymentMethodView.fromEntity(paymentMethod));
  }

  /**
   * Maps internal PaymentMethodView to API PaymentMethodResponse DTO.
   */
  private PaymentMethodResponse mapToPaymentMethodViewResponse(PaymentMethodView paymentMethod) {
    CardDetails cardDetails = null;
    if (paymentMethod.type() == PaymentMethod.Type.CARD) {
      cardDetails = new CardDetails(
          paymentMethod.lastFour(),
          paymentMethod.brand(),
          paymentMethod.expMonth(),
          paymentMethod.expYear()
      );
    }

    BillingDetails billingDetails = null;
    if (paymentMethod.billingName() != null || paymentMethod.billingEmail() != null || paymentMethod.billingAddress() != null) {
      BillingAddress address = null;
      if (paymentMethod.billingAddress() != null) {
        var addr = paymentMethod.billingAddress();
        address = new BillingAddress(
            addr.getAddressLine1(),
            addr.getAddressLine2(),
            addr.getCity(),
            addr.getState(),
            addr.getPostalCode(),
            addr.getCountry()
        );
      }
      billingDetails = new BillingDetails(
          paymentMethod.billingName(),
          paymentMethod.billingEmail(),
          address
      );
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
        paymentMethod.createdAt()
    );
  }

  /**
   * Maps API BillingDetails to internal BillingAddress.
   */
  private PaymentMethod.BillingAddress mapToBillingAddress(BillingDetails billingDetails) {
    if (billingDetails.address() == null) {
      return null;
    }

    var addr = billingDetails.address();
    return new PaymentMethod.BillingAddress(
        addr.addressLine1(),
        addr.city(),
        addr.state(),
        addr.postalCode(),
        addr.country()
    );
  }

  /**
   * Maps internal Payment status string to API PaymentStatus.
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
        case REQUIRES_PAYMENT_METHOD,
            REQUIRES_CONFIRMATION,
            REQUIRES_ACTION -> PaymentStatus.REQUIRES_ACTION;
      };
    } catch (IllegalArgumentException e) {
      return PaymentStatus.PENDING;
    }
  }

  private static final class PaymentDtoMapper {

    private PaymentDtoMapper() {}

    static PaymentView toView(Payment payment) {
      return PaymentView.fromEntity(payment);
    }
  }
}
