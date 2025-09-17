package com.platform.subscription.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.platform.payment.internal.Invoice;
import com.platform.subscription.internal.*;
import com.stripe.exception.StripeException;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

  public SubscriptionController(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @PostMapping
  public ResponseEntity<SubscriptionResponse> createSubscription(
      @Valid @RequestBody CreateSubscriptionRequest request) {
    try {
      Subscription subscription =
          subscriptionService.createSubscription(
              request.organizationId(),
              request.planId(),
              request.paymentMethodId(),
              request.trialEligible() != null ? request.trialEligible() : false);

      SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping("/organizations/{organizationId}")
  public ResponseEntity<SubscriptionResponse> getOrganizationSubscription(
      @PathVariable UUID organizationId) {
    Optional<Subscription> subscription = subscriptionService.findByOrganizationId(organizationId);
    return subscription
        .map(sub -> ResponseEntity.ok(SubscriptionResponse.fromSubscription(sub)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{subscriptionId}/plan")
  public ResponseEntity<SubscriptionResponse> changeSubscriptionPlan(
      @PathVariable UUID subscriptionId, @Valid @RequestBody ChangePlanRequest request) {
    try {
      Subscription subscription =
          subscriptionService.changeSubscriptionPlan(
              request.organizationId(),
              request.newPlanId(),
              request.prorationBehavior() != null ? request.prorationBehavior() : true);

      SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
      return ResponseEntity.ok(response);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @PostMapping("/{subscriptionId}/cancel")
  public ResponseEntity<SubscriptionResponse> cancelSubscription(
      @PathVariable UUID subscriptionId, @Valid @RequestBody CancelSubscriptionRequest request) {
    try {
      Subscription subscription =
          subscriptionService.cancelSubscription(
              request.organizationId(),
              request.immediate() != null ? request.immediate() : false,
              request.cancelAt());

      SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
      return ResponseEntity.ok(response);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @PostMapping("/{subscriptionId}/reactivate")
  public ResponseEntity<SubscriptionResponse> reactivateSubscription(
      @PathVariable UUID subscriptionId,
      @Valid @RequestBody ReactivateSubscriptionRequest request) {
    try {
      Subscription subscription =
          subscriptionService.reactivateSubscription(request.organizationId());
      SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
      return ResponseEntity.ok(response);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping("/organizations/{organizationId}/statistics")
  public ResponseEntity<SubscriptionStatisticsResponse> getSubscriptionStatistics(
      @PathVariable UUID organizationId) {
    SubscriptionService.SubscriptionStatistics stats =
        subscriptionService.getSubscriptionStatistics(organizationId);
    SubscriptionStatisticsResponse response = SubscriptionStatisticsResponse.fromStatistics(stats);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/organizations/{organizationId}/invoices")
  public ResponseEntity<List<InvoiceResponse>> getOrganizationInvoices(
      @PathVariable UUID organizationId) {
    List<Invoice> invoices = subscriptionService.getOrganizationInvoices(organizationId);
    List<InvoiceResponse> responses = invoices.stream().map(InvoiceResponse::fromInvoice).toList();
    return ResponseEntity.ok(responses);
  }

  // Request DTOs
  public record CreateSubscriptionRequest(
      @NotNull UUID organizationId,
      @NotNull UUID planId,
      String paymentMethodId,
      Boolean trialEligible) {}

  public record ChangePlanRequest(
      @NotNull UUID organizationId, @NotNull UUID newPlanId, Boolean prorationBehavior) {}

  public record CancelSubscriptionRequest(
      @NotNull UUID organizationId, Boolean immediate, Instant cancelAt) {}

  public record ReactivateSubscriptionRequest(@NotNull UUID organizationId) {}

  // Response DTOs
  public record SubscriptionResponse(
      UUID id,
      UUID organizationId,
      UUID planId,
      String stripeSubscriptionId,
      Subscription.Status status,
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      Instant trialEnd,
      Instant cancelAt,
      Instant createdAt,
      Instant updatedAt) {
    public static SubscriptionResponse fromSubscription(Subscription subscription) {
      return new SubscriptionResponse(
          subscription.getId(),
          subscription.getOrganizationId(),
          subscription.getPlanId(),
          subscription.getStripeSubscriptionId(),
          subscription.getStatus(),
          subscription.getCurrentPeriodStart() != null
              ? subscription
                  .getCurrentPeriodStart()
                  .atStartOfDay(java.time.ZoneOffset.UTC)
                  .toInstant()
              : null,
          subscription.getCurrentPeriodEnd() != null
              ? subscription
                  .getCurrentPeriodEnd()
                  .atStartOfDay(java.time.ZoneOffset.UTC)
                  .toInstant()
              : null,
          subscription.getTrialEnd() != null
              ? subscription.getTrialEnd().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
              : null,
          subscription.getCancelAt(),
          subscription.getCreatedAt(),
          subscription.getUpdatedAt());
    }
  }

  public record InvoiceResponse(
      UUID id,
      UUID organizationId,
      UUID subscriptionId,
      String stripeInvoiceId,
      String invoiceNumber,
      Invoice.Status status,
      BigDecimal subtotalAmount,
      BigDecimal taxAmount,
      BigDecimal totalAmount,
      String currency,
      Instant dueDate,
      Instant paidAt,
      Instant createdAt) {
    public static InvoiceResponse fromInvoice(Invoice invoice) {
      return new InvoiceResponse(
          invoice.getId(),
          invoice.getOrganizationId(),
          invoice.getSubscriptionId(),
          invoice.getStripeInvoiceId(),
          invoice.getInvoiceNumber(),
          invoice.getStatus(),
          invoice.getSubtotalAmount().getAmount(),
          invoice.getTaxAmount().getAmount(),
          invoice.getTotalAmount().getAmount(),
          invoice.getCurrency(),
          invoice.getDueDate() != null
              ? invoice.getDueDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
              : null,
          invoice.getPaidAt(),
          invoice.getCreatedAt());
    }
  }

  public record SubscriptionStatisticsResponse(
      Subscription.Status status,
      long totalInvoices,
      BigDecimal totalAmount,
      BigDecimal recentAmount) {
    public static SubscriptionStatisticsResponse fromStatistics(
        SubscriptionService.SubscriptionStatistics stats) {
      return new SubscriptionStatisticsResponse(
          stats.status(),
          stats.totalInvoices(),
          stats.totalAmount().getAmount(),
          stats.recentAmount().getAmount());
    }
  }
}
