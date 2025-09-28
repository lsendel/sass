package com.platform.subscription.api;

import com.platform.payment.internal.Invoice;
import com.platform.subscription.internal.InvoiceRepository;
import com.platform.subscription.internal.Plan;
import com.platform.subscription.internal.PlanRepository;
import com.platform.subscription.internal.Subscription;
import com.platform.subscription.internal.SubscriptionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A test-only REST controller to provide mock endpoints for subscription management.
 *
 * <p>This controller is active in all profiles except "prod" for security reasons. It provides a
 * simplified, stateful implementation of the subscription API by interacting directly with
 * repositories. This allows integration tests to simulate a complete subscription lifecycle without
 * making external calls to a payment provider.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/organizations/{orgId}/subscription")
@Profile("!prod")
public class TestSubscriptionController {

  private final SubscriptionRepository subscriptionRepository;
  private final PlanRepository planRepository;
  private final InvoiceRepository invoiceRepository;

  /**
   * Constructs the controller with the necessary repositories.
   *
   * @param subscriptionRepository Repository for {@link Subscription} entities.
   * @param planRepository Repository for {@link Plan} entities.
   * @param invoiceRepository Repository for {@link Invoice} entities.
   */
  public TestSubscriptionController(
      SubscriptionRepository subscriptionRepository,
      PlanRepository planRepository,
      InvoiceRepository invoiceRepository) {
    this.subscriptionRepository = subscriptionRepository;
    this.planRepository = planRepository;
    this.invoiceRepository = invoiceRepository;
  }

  /**
   * DTO for creating a subscription in a test context.
   *
   * @param planId The ID of the plan to subscribe to.
   * @param trialDays The number of trial days for the subscription.
   * @param paymentMethodId A mock payment method ID.
   */
  public record CreateSubscriptionRequest(
      @NotNull UUID planId, Integer trialDays, String paymentMethodId) {}

  /**
   * Simulates the creation of a new subscription for an organization.
   *
   * @param orgId The ID of the organization.
   * @param req The request details for creating the subscription.
   * @return A {@link ResponseEntity} with the details of the created subscription.
   */
  @PostMapping
  public ResponseEntity<?> create(
      @PathVariable UUID orgId, @Valid @RequestBody CreateSubscriptionRequest req) {
    var plan = planRepository.findById(req.planId()).orElseThrow();
    Integer days = req.trialDays() != null ? req.trialDays() : 0;
    LocalDate trialEnd = days > 0 ? LocalDate.now().plusDays(days) : null;
    Subscription sub =
        Subscription.createActive(orgId, plan.getId(), LocalDate.now(), LocalDate.now().plusMonths(1));
    sub = subscriptionRepository.save(sub);
    Map<String, Object> response =
        Map.of("status", sub.getStatus().name(), "planId", plan.getId().toString(), "trialEnd", trialEnd);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  /**
   * DTO for changing a subscription's plan in a test context.
   *
   * @param newPlanId The ID of the new plan.
   * @param prorationBehavior A string indicating how to handle proration.
   */
  public record UpdatePlanRequest(@NotNull UUID newPlanId, @NotBlank String prorationBehavior) {}

  /**
   * Simulates changing the plan for an existing subscription.
   *
   * @param orgId The ID of the organization.
   * @param req The request details for updating the plan.
   * @return A {@link ResponseEntity} with the updated plan ID.
   */
  @PutMapping("/plan")
  public ResponseEntity<?> changePlan(
      @PathVariable UUID orgId, @Valid @RequestBody UpdatePlanRequest req) {
    Subscription sub = subscriptionRepository.findByOrganizationId(orgId).orElseThrow();
    Plan plan = planRepository.findById(req.newPlanId()).orElseThrow();
    sub.changePlan(plan.getId());
    subscriptionRepository.save(sub);
    return ResponseEntity.ok(Map.of("planId", plan.getId().toString()));
  }

  /**
   * DTO for canceling a subscription in a test context.
   *
   * @param cancelAtPeriodEnd Whether to cancel at the end of the billing period.
   * @param cancellationReason A reason for the cancellation.
   */
  public record CancelRequest(boolean cancelAtPeriodEnd, String cancellationReason) {}

  /**
   * Simulates the cancellation of a subscription.
   *
   * @param orgId The ID of the organization.
   * @param req The request details for the cancellation.
   * @return A {@link ResponseEntity} with the updated subscription status.
   */
  @DeleteMapping
  public ResponseEntity<?> cancel(@PathVariable UUID orgId, @Valid @RequestBody CancelRequest req) {
    Subscription sub = subscriptionRepository.findByOrganizationId(orgId).orElseThrow();
    sub.scheduleCancellation(
        LocalDate.now().plusMonths(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
    sub = subscriptionRepository.save(sub);
    return ResponseEntity.ok(
        Map.of("status", sub.getStatus().name(), "cancelAt", sub.getCancelAt()));
  }

  /**
   * Simulates the reactivation of a canceled subscription.
   *
   * @param orgId The ID of the organization.
   * @return A {@link ResponseEntity} with the updated subscription status.
   */
  @PostMapping("/reactivate")
  public ResponseEntity<?> reactivate(@PathVariable UUID orgId) {
    Subscription sub = subscriptionRepository.findByOrganizationId(orgId).orElseThrow();
    sub.reactivate();
    sub = subscriptionRepository.save(sub);
    return ResponseEntity.ok(Map.of("status", sub.getStatus().name()));
  }

  /**
   * DTO for creating an invoice in a test context.
   *
   * @param paymentMethodId A mock payment method ID.
   */
  public record InvoiceRequest(String paymentMethodId) {}

  /**
   * Simulates the creation of a new invoice for a subscription.
   *
   * @param orgId The ID of the organization.
   * @param req The invoice request details.
   * @return A {@link ResponseEntity} with the details of the created invoice.
   */
  @PostMapping("/invoice")
  public ResponseEntity<?> invoice(
      @PathVariable UUID orgId, @RequestBody(required = false) InvoiceRequest req) {
    Subscription sub = subscriptionRepository.findByOrganizationId(orgId).orElseThrow();
    Plan plan = planRepository.findById(sub.getPlanId()).orElseThrow();
    Invoice inv =
        new Invoice(sub.getId(), "in_test_" + orgId, plan.getPrice(), Invoice.Status.OPEN);
    inv = invoiceRepository.save(inv);
    return new ResponseEntity<>(
        Map.of(
            "amount",
            plan.getPrice().getAmount(),
            "currency",
            plan.getPrice().getCurrency().toLowerCase(),
            "status",
            inv.getStatus().name()),
        HttpStatus.CREATED);
  }
}

