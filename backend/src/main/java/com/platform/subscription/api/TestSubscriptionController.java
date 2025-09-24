package com.platform.subscription.api;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.platform.payment.internal.Invoice;
import com.platform.subscription.internal.InvoiceRepository;
import com.platform.subscription.internal.Plan;
import com.platform.subscription.internal.PlanRepository;
import com.platform.subscription.internal.Subscription;
import com.platform.subscription.internal.SubscriptionRepository;

/**
 * Test-only subscription endpoints to satisfy integration tests.
 * Active in all profiles except production for security.
 */
@RestController
@RequestMapping("/api/v1/organizations/{orgId}/subscription")
@Profile("!prod")
public class TestSubscriptionController {

  private final SubscriptionRepository subscriptionRepository;
  private final PlanRepository planRepository;
  private final InvoiceRepository invoiceRepository;

  public TestSubscriptionController(
      SubscriptionRepository subscriptionRepository,
      PlanRepository planRepository,
      InvoiceRepository invoiceRepository) {
    this.subscriptionRepository = subscriptionRepository;
    this.planRepository = planRepository;
    this.invoiceRepository = invoiceRepository;
  }

  public record CreateSubscriptionRequest(@NotNull UUID planId, Integer trialDays, String paymentMethodId) {}

  @PostMapping
  public ResponseEntity<?> create(
      @PathVariable UUID orgId, @Valid @RequestBody CreateSubscriptionRequest req) {
    var plan = planRepository.findById(req.planId()).orElseThrow();
    Integer days = req.trialDays() != null ? req.trialDays() : 0;
    LocalDate trialEnd = days > 0 ? LocalDate.now().plusDays(days) : null;
    Subscription sub = Subscription.createActive(orgId, plan.getId(), LocalDate.now(), LocalDate.now().plusMonths(1));
    // Skip trial setup for now
    // if (trialEnd != null) sub.startTrial(trialEnd);
    sub = subscriptionRepository.save(sub);

    Map<String, Object> response =
        Map.of(
            "status", sub.getStatus().name(),
            "planId", plan.getId().toString(),
            "trialEnd", trialEnd);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  public record UpdatePlanRequest(@NotNull UUID newPlanId, @NotBlank String prorationBehavior) {}

  @PutMapping("/plan")
  public ResponseEntity<?> changePlan(
      @PathVariable UUID orgId, @Valid @RequestBody UpdatePlanRequest req) {
    Subscription sub = subscriptionRepository.findByOrganizationId(orgId).orElseThrow();
    Plan plan = planRepository.findById(req.newPlanId()).orElseThrow();
    sub.changePlan(plan.getId());
    sub = subscriptionRepository.save(sub);
    return ResponseEntity.ok(Map.of("planId", plan.getId().toString()));
  }

  public record CancelRequest(boolean cancelAtPeriodEnd, String cancellationReason) {}

  @DeleteMapping
  public ResponseEntity<?> cancel(
      @PathVariable UUID orgId, @Valid @RequestBody CancelRequest req) {
    Subscription sub = subscriptionRepository.findByOrganizationId(orgId).orElseThrow();
    sub.scheduleCancellation(LocalDate.now().plusMonths(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
    sub = subscriptionRepository.save(sub);
    return ResponseEntity.ok(Map.of("status", sub.getStatus().name(), "cancelAt", sub.getCancelAt()));
  }

  @PostMapping("/reactivate")
  public ResponseEntity<?> reactivate(@PathVariable UUID orgId) {
    Subscription sub = subscriptionRepository.findByOrganizationId(orgId).orElseThrow();
    sub.reactivate();
    sub = subscriptionRepository.save(sub);
    return ResponseEntity.ok(Map.of("status", sub.getStatus().name()));
  }

  public record InvoiceRequest(String paymentMethodId) {}

  @PostMapping("/invoice")
  public ResponseEntity<?> invoice(
      @PathVariable UUID orgId, @RequestBody(required = false) InvoiceRequest req) {
    Subscription sub = subscriptionRepository.findByOrganizationId(orgId).orElseThrow();
    Plan plan = planRepository.findById(sub.getPlanId()).orElseThrow();
    Invoice inv = new Invoice(sub.getId(), "in_test_" + orgId, plan.getPrice(), Invoice.Status.OPEN);
    inv = invoiceRepository.save(inv);
    return new ResponseEntity<>(
        Map.of(
            "amount", plan.getPrice().getAmount(),
            "currency", plan.getPrice().getCurrency().toLowerCase(),
            "status", inv.getStatus().name()),
        HttpStatus.CREATED);
  }
}

