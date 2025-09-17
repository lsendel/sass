package com.platform.subscription.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.platform.subscription.internal.Plan;
import com.platform.subscription.internal.SubscriptionService;

@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

  private final SubscriptionService subscriptionService;

  public PlanController(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @GetMapping
  public ResponseEntity<List<PlanResponse>> getAvailablePlans() {
    List<Plan> plans = subscriptionService.getAvailablePlans();
    List<PlanResponse> responses = plans.stream().map(PlanResponse::fromPlan).toList();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/interval/{interval}")
  public ResponseEntity<List<PlanResponse>> getPlansByInterval(
      @PathVariable Plan.BillingInterval interval) {
    List<Plan> plans = subscriptionService.getPlansByInterval(interval);
    List<PlanResponse> responses = plans.stream().map(PlanResponse::fromPlan).toList();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/slug/{slug}")
  public ResponseEntity<PlanResponse> getPlanBySlug(@PathVariable String slug) {
    Optional<Plan> plan = subscriptionService.findPlanBySlug(slug);
    return plan.map(p -> ResponseEntity.ok(PlanResponse.fromPlan(p)))
        .orElse(ResponseEntity.notFound().build());
  }

  // Response DTO
  public record PlanResponse(
      UUID id,
      String name,
      String slug,
      String description,
      BigDecimal amount,
      String currency,
      Plan.BillingInterval interval,
      Integer intervalCount,
      Integer trialDays,
      boolean active,
      int displayOrder,
      Map<String, Object> features,
      Instant createdAt,
      Instant updatedAt) {
    public static PlanResponse fromPlan(Plan plan) {
      return new PlanResponse(
          plan.getId(),
          plan.getName(),
          plan.getSlug(),
          plan.getDescription(),
          plan.getPrice().getAmount(),
          plan.getCurrency(),
          plan.getInterval(),
          plan.getIntervalCount(),
          plan.getTrialDays(),
          plan.isActive(),
          plan.getDisplayOrder(),
          plan.getFeatures(),
          plan.getCreatedAt(),
          plan.getUpdatedAt());
    }
  }
}
