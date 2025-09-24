package com.platform.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Data transfer objects for payment-related operations.
 * These DTOs prevent direct access to internal entities from controllers.
 */
public class PaymentDto {

  /**
   * Payment status enumeration for API layer.
   */
  public enum PaymentStatus {
    PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELED, REQUIRES_ACTION
  }

  /**
   * Payment method type enumeration for API layer.
   */
  public enum PaymentMethodType {
    CARD, BANK_ACCOUNT, OTHER
  }

  /**
   * Response DTO for payment information.
   */
  public record PaymentResponse(
      UUID id,
      UUID organizationId,
      String stripePaymentIntentId,
      BigDecimal amount,
      String currency,
      PaymentStatus status,
      String paymentMethodId,
      String description,
      Instant createdAt) {
  }

  /**
   * Response DTO for payment method information.
   */
  public record PaymentMethodResponse(
      UUID id,
      UUID organizationId,
      String stripePaymentMethodId,
      String type,
      boolean isDefault,
      String displayName,
      CardDetails cardDetails,
      BillingDetails billingDetails,
      Instant createdAt) {
  }

  /**
   * Response DTO for payment statistics.
   */
  public record PaymentStatisticsResponse(
      long totalSuccessfulPayments,
      BigDecimal totalAmount,
      BigDecimal recentAmount) {
  }

  /**
   * Request DTO for creating a payment.
   */
  public record CreatePaymentRequest(
      @NotNull UUID organizationId,
      @Positive BigDecimal amount,
      @NotBlank String currency,
      String paymentMethodId,
      String description,
      boolean confirm) {
  }

  /**
   * Request DTO for attaching a payment method.
   */
  public record AttachPaymentMethodRequest(
      @NotNull UUID organizationId,
      @NotBlank String stripePaymentMethodId) {
  }

  /**
   * Request DTO for updating payment method.
   */
  public record UpdatePaymentMethodRequest(
      String displayName,
      BillingDetails billingDetails) {
  }

  /**
   * DTO for card details.
   */
  public record CardDetails(
      String lastFour,
      String brand,
      Integer expMonth,
      Integer expYear) {
  }

  /**
   * DTO for billing details.
   */
  public record BillingDetails(
      String name,
      String email,
      BillingAddress address) {
  }

  /**
   * DTO for billing address.
   */
  public record BillingAddress(
      String addressLine1,
      String addressLine2,
      String city,
      String state,
      String postalCode,
      String country) {
  }
}