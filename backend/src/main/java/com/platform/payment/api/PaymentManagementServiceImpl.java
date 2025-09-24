package com.platform.payment.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.platform.payment.api.PaymentDto.BillingAddress;
import com.platform.payment.api.PaymentDto.BillingDetails;
import com.platform.payment.api.PaymentDto.CardDetails;
import com.platform.payment.api.PaymentDto.PaymentMethodResponse;
import com.platform.payment.api.PaymentDto.PaymentMethodType;
import com.platform.payment.api.PaymentDto.PaymentResponse;
import com.platform.payment.api.PaymentDto.PaymentStatisticsResponse;
import com.platform.payment.api.PaymentDto.PaymentStatus;
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

    // This method needs to be implemented using the existing PaymentService methods
    throw new UnsupportedOperationException("Method not yet implemented");
  }

  @Override
  public PaymentResponse confirmPayment(UUID organizationId, String paymentIntentId) throws StripeException {
    // This method needs to be implemented using the existing PaymentService methods
    throw new UnsupportedOperationException("Method not yet implemented");
  }

  @Override
  public PaymentResponse cancelPayment(UUID organizationId, String paymentIntentId) throws StripeException {
    // This method needs to be implemented using the existing PaymentService methods
    throw new UnsupportedOperationException("Method not yet implemented");
  }

  @Override
  public List<PaymentResponse> getOrganizationPayments(UUID organizationId) {
    return paymentService.getOrganizationPayments(organizationId)
        .stream()
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

    // This method needs to be implemented using the existing PaymentService methods
    throw new UnsupportedOperationException("Method not yet implemented");
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
    return switch (internalStatus) {
      case "PENDING" -> PaymentStatus.PENDING;
      case "PROCESSING" -> PaymentStatus.PROCESSING;
      case "SUCCEEDED" -> PaymentStatus.SUCCEEDED;
      case "FAILED" -> PaymentStatus.FAILED;
      case "CANCELED" -> PaymentStatus.CANCELED;
      case "REQUIRES_ACTION" -> PaymentStatus.REQUIRES_ACTION;
      default -> PaymentStatus.PENDING;
    };
  }
}