package com.platform.subscription.api;

import com.platform.payment.internal.Invoice;
import com.platform.subscription.api.SubscriptionDto.InvoiceResponse;
import com.platform.subscription.api.SubscriptionDto.InvoiceStatus;
import com.platform.subscription.api.SubscriptionDto.SubscriptionResponse;
import com.platform.subscription.api.SubscriptionDto.SubscriptionStatisticsResponse;
import com.platform.subscription.api.SubscriptionDto.SubscriptionStatus;
import com.platform.subscription.internal.Subscription;
import com.platform.subscription.internal.SubscriptionService;
import com.stripe.exception.StripeException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Implements the {@link SubscriptionManagementService} interface, acting as a bridge between the
 * API layer and the internal subscription service.
 *
 * <p>This service translates requests from the API layer into calls to the internal {@link
 * SubscriptionService} and maps the resulting internal domain entities (like {@link Subscription}
 * and {@link Invoice}) to public DTOs.
 * </p>
 */
@Service
public class SubscriptionManagementServiceImpl implements SubscriptionManagementService {

  private final SubscriptionService subscriptionService;

  /**
   * Constructs the service with the required internal {@link SubscriptionService}.
   *
   * @param subscriptionService The core service for handling subscription logic.
   */
  public SubscriptionManagementServiceImpl(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @Override
  public SubscriptionResponse createSubscription(
      UUID organizationId, UUID planId, String paymentMethodId, boolean trialEligible)
      throws StripeException {
    Subscription subscription =
        subscriptionService.createSubscription(organizationId, planId, paymentMethodId, trialEligible);
    return mapToResponse(subscription);
  }

  @Override
  public Optional<SubscriptionResponse> findByOrganizationId(UUID organizationId) {
    return subscriptionService.findByOrganizationId(organizationId).map(this::mapToResponse);
  }

  @Override
  public SubscriptionResponse changeSubscriptionPlan(
      UUID organizationId, UUID newPlanId, boolean prorationBehavior) throws StripeException {
    Subscription subscription =
        subscriptionService.changeSubscriptionPlan(organizationId, newPlanId, prorationBehavior);
    return mapToResponse(subscription);
  }

  @Override
  public SubscriptionResponse cancelSubscription(
      UUID organizationId, boolean immediate, java.time.Instant cancelAt) throws StripeException {
    Subscription subscription =
        subscriptionService.cancelSubscription(organizationId, immediate, cancelAt);
    return mapToResponse(subscription);
  }

  @Override
  public SubscriptionResponse reactivateSubscription(UUID organizationId) throws StripeException {
    Subscription subscription = subscriptionService.reactivateSubscription(organizationId);
    return mapToResponse(subscription);
  }

  @Override
  public List<InvoiceResponse> getOrganizationInvoices(UUID organizationId) {
    return subscriptionService.getOrganizationInvoices(organizationId).stream()
        .map(this::mapToInvoiceResponse)
        .toList();
  }

  @Override
  public SubscriptionStatisticsResponse getSubscriptionStatistics(UUID organizationId) {
    SubscriptionService.SubscriptionStatistics stats =
        subscriptionService.getSubscriptionStatistics(organizationId);
    return new SubscriptionStatisticsResponse(
        mapToApiStatus(stats.status()),
        stats.totalInvoices(),
        stats.totalAmount().getAmount(),
        stats.recentAmount().getAmount(),
        null // lastPaymentDate not available in current stats
        );
  }

  /**
   * Maps an internal {@link Subscription} entity to a public {@link SubscriptionResponse} DTO.
   *
   * @param subscription The internal subscription entity.
   * @return The corresponding subscription response DTO.
   */
  private SubscriptionResponse mapToResponse(Subscription subscription) {
    return new SubscriptionResponse(
        subscription.getId(),
        subscription.getOrganizationId(),
        subscription.getPlanId(),
        subscription.getStripeSubscriptionId(),
        mapToApiStatus(subscription.getStatus()),
        subscription.getCurrentPeriodStart() != null
            ? subscription.getCurrentPeriodStart().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
            : null,
        subscription.getCurrentPeriodEnd() != null
            ? subscription.getCurrentPeriodEnd().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
            : null,
        null, // trialStart not available in entity
        subscription.getTrialEnd() != null
            ? subscription.getTrialEnd().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
            : null,
        subscription.getCancelAt(),
        null, // canceledAt not available in entity
        subscription.getCreatedAt(),
        subscription.getUpdatedAt());
  }

  /**
   * Maps an internal {@link Invoice} entity to a public {@link InvoiceResponse} DTO.
   *
   * @param invoice The internal invoice entity.
   * @return The corresponding invoice response DTO.
   */
  private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
    return new InvoiceResponse(
        invoice.getId(),
        invoice.getOrganizationId(),
        invoice.getSubscriptionId(),
        invoice.getStripeInvoiceId(),
        invoice.getAmount().getAmount(),
        invoice.getAmount().getCurrency(),
        mapToApiInvoiceStatus(invoice.getStatus()),
        invoice.getDueDate() != null
            ? invoice.getDueDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
            : null,
        invoice.getPaidAt(),
        invoice.getCreatedAt());
  }

  /**
   * Maps an internal {@link Subscription.Status} enum to the public API {@link
   * SubscriptionStatus} enum.
   *
   * @param internalStatus The status from the internal domain.
   * @return The corresponding public API status.
   */
  private SubscriptionStatus mapToApiStatus(Subscription.Status internalStatus) {
    return switch (internalStatus) {
      case ACTIVE -> SubscriptionStatus.ACTIVE;
      case CANCELED -> SubscriptionStatus.CANCELED;
      case PAST_DUE -> SubscriptionStatus.PAST_DUE;
      case TRIALING -> SubscriptionStatus.TRIALING;
      case INCOMPLETE, INCOMPLETE_EXPIRED, UNPAID -> SubscriptionStatus.INACTIVE;
    };
  }

  /**
   * Maps an internal {@link Invoice.Status} enum to the public API {@link InvoiceStatus} enum.
   *
   * @param internalStatus The status from the internal domain.
   * @return The corresponding public API status.
   */
  private InvoiceStatus mapToApiInvoiceStatus(Invoice.Status internalStatus) {
    return switch (internalStatus) {
      case DRAFT, OPEN -> InvoiceStatus.PENDING;
      case PAID -> InvoiceStatus.PAID;
      case UNCOLLECTIBLE, VOID -> InvoiceStatus.CANCELLED;
    };
  }
}