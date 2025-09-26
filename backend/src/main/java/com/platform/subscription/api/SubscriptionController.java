package com.platform.subscription.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import com.platform.subscription.api.SubscriptionDto.CancelSubscriptionRequest;
import com.platform.subscription.api.SubscriptionDto.ChangePlanRequest;
import com.platform.subscription.api.SubscriptionDto.CreateSubscriptionRequest;
import com.platform.subscription.api.SubscriptionDto.InvoiceResponse;
import com.platform.subscription.api.SubscriptionDto.ReactivateSubscriptionRequest;
import com.platform.subscription.api.SubscriptionDto.SubscriptionResponse;
import com.platform.subscription.api.SubscriptionDto.SubscriptionStatisticsResponse;
import com.platform.shared.security.PlatformUserPrincipal;
import com.stripe.exception.StripeException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscription management and billing endpoints")
@SecurityRequirement(name = "sessionAuth")
public class SubscriptionController {

  private final SubscriptionManagementService subscriptionManagementService;

  public SubscriptionController(SubscriptionManagementService subscriptionManagementService) {
    this.subscriptionManagementService = subscriptionManagementService;
  }

  @PostMapping
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #request.organizationId)")
  @Operation(
      summary = "Create subscription",
      description = "Create a new subscription for an organization"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Subscription created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "403", description = "Not authorized for organization")
  })
  public ResponseEntity<SubscriptionResponse> createSubscription(
      @Parameter(hidden = true) @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @Parameter(description = "Subscription creation data", required = true)
      @Valid @RequestBody @P("request") CreateSubscriptionRequest request) {
    try {
        SubscriptionResponse subscription =
            subscriptionManagementService.createSubscription(
                request.organizationId(),
              request.planId(),
              request.paymentMethodId(),
              request.trialEligible() != null ? request.trialEligible() : false);

      return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping("/organizations/{organizationId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  @Operation(
      summary = "Get organization subscription",
      description = "Retrieve the current subscription for an organization"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Subscription found"),
      @ApiResponse(responseCode = "404", description = "Subscription not found")
  })
  public ResponseEntity<SubscriptionResponse> getOrganizationSubscription(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    Optional<SubscriptionResponse> subscription = subscriptionManagementService.findByOrganizationId(organizationId);
    return subscription
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{subscriptionId}/plan")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #request.organizationId)")
  public ResponseEntity<SubscriptionResponse> changeSubscriptionPlan(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable UUID subscriptionId,
      @Valid @RequestBody @P("request") ChangePlanRequest request) {
    try {
      SubscriptionResponse subscription =
          subscriptionManagementService.changeSubscriptionPlan(
              request.organizationId(),
              request.newPlanId(),
              request.prorationBehavior() != null ? request.prorationBehavior() : true);

      return ResponseEntity.ok(subscription);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @PostMapping("/{subscriptionId}/cancel")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #request.organizationId)")
  public ResponseEntity<SubscriptionResponse> cancelSubscription(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable UUID subscriptionId,
      @Valid @RequestBody @P("request") CancelSubscriptionRequest request) {
    try {
      SubscriptionResponse subscription =
          subscriptionManagementService.cancelSubscription(
              request.organizationId(),
              request.immediate() != null ? request.immediate() : false,
              request.cancelAt());

      return ResponseEntity.ok(subscription);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @PostMapping("/{subscriptionId}/reactivate")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #request.organizationId)")
  public ResponseEntity<SubscriptionResponse> reactivateSubscription(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable UUID subscriptionId,
      @Valid @RequestBody @P("request") ReactivateSubscriptionRequest request) {
    try {
      SubscriptionResponse subscription =
          subscriptionManagementService.reactivateSubscription(request.organizationId());
      return ResponseEntity.ok(subscription);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping("/organizations/{organizationId}/statistics")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<SubscriptionStatisticsResponse> getSubscriptionStatistics(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    SubscriptionStatisticsResponse stats =
        subscriptionManagementService.getSubscriptionStatistics(organizationId);
    return ResponseEntity.ok(stats);
  }

  @GetMapping("/organizations/{organizationId}/invoices")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<InvoiceResponse>> getOrganizationInvoices(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    List<InvoiceResponse> invoices = subscriptionManagementService.getOrganizationInvoices(organizationId);
    return ResponseEntity.ok(invoices);
  }

}
