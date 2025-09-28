package com.platform.subscription.api;

import com.platform.subscription.api.PlanDto.BillingInterval;
import com.platform.subscription.api.PlanDto.PlanResponse;
import com.platform.subscription.internal.Plan;
import com.platform.subscription.internal.SubscriptionService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Implements the {@link PlanManagementService} interface, acting as a bridge between the API layer
 * and the internal subscription service.
 *
 * <p>This service translates requests from the API layer into calls to the internal {@link
 * SubscriptionService} and maps the resulting internal {@link Plan} entities to public {@link
 * PlanResponse} DTOs.
 * </p>
 */
@Service
public class PlanManagementServiceImpl implements PlanManagementService {

  private final SubscriptionService subscriptionService;

  /**
   * Constructs the service with the required internal {@link SubscriptionService}.
   *
   * @param subscriptionService The core service for subscription and plan logic.
   */
  public PlanManagementServiceImpl(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @Override
  public List<PlanResponse> getAvailablePlans() {
    return subscriptionService.getAvailablePlans().stream().map(this::mapToResponse).toList();
  }

  @Override
  public List<PlanResponse> getPlansByInterval(BillingInterval interval) {
    Plan.BillingInterval internalInterval = mapToInternalInterval(interval);
    return subscriptionService.getPlansByInterval(internalInterval).stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  public Optional<PlanResponse> findPlanBySlug(String slug) {
    return subscriptionService.findPlanBySlug(slug).map(this::mapToResponse);
  }

  /**
   * Maps an internal {@link Plan} entity to a public {@link PlanResponse} DTO.
   *
   * @param plan The internal Plan entity.
   * @return The corresponding PlanResponse DTO.
   */
  private PlanResponse mapToResponse(Plan plan) {
    return new PlanResponse(
        plan.getId(),
        plan.getName(),
        plan.getSlug(),
        plan.getDescription(),
        plan.getPrice().getAmount(),
        plan.getCurrency(),
        mapToApiInterval(plan.getInterval()),
        plan.getIntervalCount(),
        plan.getTrialDays(),
        plan.isActive(),
        plan.getDisplayOrder(),
        plan.getFeatures(),
        plan.getCreatedAt(),
        plan.getUpdatedAt());
  }

  /**
   * Maps a public API {@link BillingInterval} enum to the internal {@link Plan.BillingInterval}
   * enum.
   *
   * @param apiInterval The BillingInterval from the API layer.
   * @return The corresponding internal BillingInterval.
   */
  private Plan.BillingInterval mapToInternalInterval(BillingInterval apiInterval) {
    return switch (apiInterval) {
      case MONTH -> Plan.BillingInterval.MONTH;
      case YEAR -> Plan.BillingInterval.YEAR;
    };
  }

  /**
   * Maps an internal {@link Plan.BillingInterval} enum to the public API {@link BillingInterval}
   * enum.
   *
   * @param internalInterval The BillingInterval from the internal domain.
   * @return The corresponding public API BillingInterval.
   */
  private BillingInterval mapToApiInterval(Plan.BillingInterval internalInterval) {
    return switch (internalInterval) {
      case MONTH -> BillingInterval.MONTH;
      case YEAR -> BillingInterval.YEAR;
    };
  }
}