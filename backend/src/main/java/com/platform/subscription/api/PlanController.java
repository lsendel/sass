package com.platform.subscription.api;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.platform.subscription.api.PlanDto.BillingInterval;
import com.platform.subscription.api.PlanDto.PlanResponse;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

  private final PlanManagementService planManagementService;

  public PlanController(PlanManagementService planManagementService) {
    this.planManagementService = planManagementService;
  }

  @GetMapping
  public ResponseEntity<List<PlanResponse>> getAvailablePlans() {
    List<PlanResponse> plans = planManagementService.getAvailablePlans();
    return ResponseEntity.ok(plans);
  }

  @GetMapping("/interval/{interval}")
  public ResponseEntity<List<PlanResponse>> getPlansByInterval(
      @PathVariable BillingInterval interval) {
    List<PlanResponse> plans = planManagementService.getPlansByInterval(interval);
    return ResponseEntity.ok(plans);
  }

  @GetMapping("/slug/{slug}")
  public ResponseEntity<PlanResponse> getPlanBySlug(@PathVariable String slug) {
    Optional<PlanResponse> plan = planManagementService.findPlanBySlug(slug);
    return plan.map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
