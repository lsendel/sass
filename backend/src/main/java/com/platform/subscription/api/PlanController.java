package com.platform.subscription.api;

import com.platform.subscription.api.PlanDto.BillingInterval;
import com.platform.subscription.api.PlanDto.PlanResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for retrieving information about available subscription plans.
 *
 * <p>This controller provides public endpoints to list all plans, filter them by billing interval,
 * or find a specific plan by its unique slug.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

  private final PlanManagementService planManagementService;

  /**
   * Constructs the controller with the required plan management service.
   *
   * @param planManagementService The service for managing subscription plan data.
   */
  public PlanController(PlanManagementService planManagementService) {
    this.planManagementService = planManagementService;
  }

  /**
   * Retrieves a list of all available subscription plans.
   *
   * @return A {@link ResponseEntity} containing a list of {@link PlanResponse} objects.
   */
  @GetMapping
  public ResponseEntity<List<PlanResponse>> getAvailablePlans() {
    List<PlanResponse> plans = planManagementService.getAvailablePlans();
    return ResponseEntity.ok(plans);
  }

  /**
   * Retrieves a list of subscription plans filtered by their billing interval.
   *
   * @param interval The billing interval to filter by (e.g., MONTHLY, YEARLY).
   * @return A {@link ResponseEntity} containing a list of matching {@link PlanResponse} objects.
   */
  @GetMapping("/interval/{interval}")
  public ResponseEntity<List<PlanResponse>> getPlansByInterval(
      @PathVariable BillingInterval interval) {
    List<PlanResponse> plans = planManagementService.getPlansByInterval(interval);
    return ResponseEntity.ok(plans);
  }

  /**
   * Retrieves a single subscription plan by its unique slug.
   *
   * @param slug The unique slug identifier for the plan.
   * @return A {@link ResponseEntity} containing the found {@link PlanResponse}, or a 404 Not Found
   *     response if no plan with the given slug exists.
   */
  @GetMapping("/slug/{slug}")
  public ResponseEntity<PlanResponse> getPlanBySlug(@PathVariable String slug) {
    Optional<PlanResponse> plan = planManagementService.findPlanBySlug(slug);
    return plan.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }
}
