package com.platform.subscription.api;

import java.util.List;
import java.util.Optional;

import com.platform.subscription.api.PlanDto.BillingInterval;
import com.platform.subscription.api.PlanDto.PlanResponse;

/**
 * Service interface for plan management operations.
 * This interface provides the API layer with access to subscription functionality
 * without depending on internal implementation details.
 */
public interface PlanManagementService {

  /**
   * Retrieves all available plans.
   *
   * @return list of available plans
   */
  List<PlanResponse> getAvailablePlans();

  /**
   * Retrieves plans filtered by billing interval.
   *
   * @param interval the billing interval to filter by
   * @return list of plans matching the interval
   */
  List<PlanResponse> getPlansByInterval(BillingInterval interval);

  /**
   * Finds a plan by its slug.
   *
   * @param slug the plan slug
   * @return the plan if found, empty otherwise
   */
  Optional<PlanResponse> findPlanBySlug(String slug);
}