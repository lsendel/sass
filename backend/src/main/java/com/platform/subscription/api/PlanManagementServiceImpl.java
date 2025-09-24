package com.platform.subscription.api;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.platform.subscription.api.PlanDto.BillingInterval;
import com.platform.subscription.api.PlanDto.PlanResponse;
import com.platform.subscription.internal.SubscriptionService;
import com.platform.subscription.internal.Plan;

/**
 * Implementation of PlanManagementService that bridges the API and internal layers.
 * This service converts between internal entities and API DTOs.
 */
@Service
public class PlanManagementServiceImpl implements PlanManagementService {

  private final SubscriptionService subscriptionService;

  public PlanManagementServiceImpl(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @Override
  public List<PlanResponse> getAvailablePlans() {
    return subscriptionService.getAvailablePlans()
        .stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  public List<PlanResponse> getPlansByInterval(BillingInterval interval) {
    Plan.BillingInterval internalInterval = mapToInternalInterval(interval);
    return subscriptionService.getPlansByInterval(internalInterval)
        .stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  public Optional<PlanResponse> findPlanBySlug(String slug) {
    return subscriptionService.findPlanBySlug(slug)
        .map(this::mapToResponse);
  }

  /**
   * Maps internal Plan entity to API PlanResponse DTO.
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
        plan.getUpdatedAt()
    );
  }

  /**
   * Maps API BillingInterval to internal BillingInterval.
   */
  private Plan.BillingInterval mapToInternalInterval(BillingInterval apiInterval) {
    return switch (apiInterval) {
      case MONTH -> Plan.BillingInterval.MONTH;
      case YEAR -> Plan.BillingInterval.YEAR;
    };
  }

  /**
   * Maps internal BillingInterval to API BillingInterval.
   */
  private BillingInterval mapToApiInterval(Plan.BillingInterval internalInterval) {
    return switch (internalInterval) {
      case MONTH -> BillingInterval.MONTH;
      case YEAR -> BillingInterval.YEAR;
    };
  }
}