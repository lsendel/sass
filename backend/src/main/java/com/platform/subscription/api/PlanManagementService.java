package com.platform.subscription.api;

import com.platform.subscription.api.PlanDto.BillingInterval;
import com.platform.subscription.api.PlanDto.PlanResponse;
import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for managing subscription plans.
 *
 * <p>This service interface provides a high-level API for retrieving information about subscription
 * plans. It acts as an abstraction layer, decoupling the API controllers from the internal
 * implementation details of plan management.
 * </p>
 */
public interface PlanManagementService {

  /**
   * Retrieves a list of all currently available (active) subscription plans.
   *
   * @return A list of {@link PlanResponse} objects representing the available plans.
   */
  List<PlanResponse> getAvailablePlans();

  /**
   * Retrieves a list of available subscription plans filtered by a specific billing interval.
   *
   * @param interval The {@link BillingInterval} to filter by (e.g., MONTH, YEAR).
   * @return A list of {@link PlanResponse} objects that match the given billing interval.
   */
  List<PlanResponse> getPlansByInterval(BillingInterval interval);

  /**
   * Finds a specific subscription plan by its unique slug.
   *
   * @param slug The URL-friendly, unique identifier for the plan.
   * @return An {@link Optional} containing the {@link PlanResponse} if a plan with the given slug
   *     is found, otherwise an empty {@link Optional}.
   */
  Optional<PlanResponse> findPlanBySlug(String slug);
}