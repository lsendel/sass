package com.platform.subscription.api;

import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.subscription.api.SubscriptionDto.CancelSubscriptionRequest;
import com.platform.subscription.api.SubscriptionDto.ChangePlanRequest;
import com.platform.subscription.api.SubscriptionDto.CreateSubscriptionRequest;
import com.platform.subscription.api.SubscriptionDto.InvoiceResponse;
import com.platform.subscription.api.SubscriptionDto.ReactivateSubscriptionRequest;
import com.platform.subscription.api.SubscriptionDto.SubscriptionResponse;
import com.platform.subscription.api.SubscriptionDto.SubscriptionStatisticsResponse;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the lifecycle of subscriptions.
 *
 * <p>This controller provides endpoints for creating, retrieving, updating, and canceling
 * subscriptions. It also offers endpoints for fetching related data such as invoices and usage
 * statistics. All endpoints are secured and require appropriate authorization.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscription management and billing endpoints")
@SecurityRequirement(name = "sessionAuth")
public class SubscriptionController {

  private final SubscriptionManagementService subscriptionManagementService;

  /**
   * Constructs the controller with the required subscription management service.
   *
   * @param subscriptionManagementService The service for handling subscription logic.
   */
  public SubscriptionController(SubscriptionManagementService subscriptionManagementService) {
    this.subscriptionManagementService = subscriptionManagementService;
  }

  /**
   * Creates a new subscription for an organization.
   *
   * @param userPrincipal The authenticated user principal.
   * @param request The request body containing the details for the new subscription.
   * @return A {@link ResponseEntity} with the created {@link SubscriptionResponse} on success.
   */
  @PostMapping
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #request.organizationId)")
  @Operation(
      summary = "Create subscription",
      description = "Create a new subscription for an organization")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Subscription created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Not authorized for organization")
      })
  public ResponseEntity<SubscriptionResponse> createSubscription(
      @Parameter(hidden = true) @AuthenticationPrincipal @P("principal")
          PlatformUserPrincipal userPrincipal,
      @Parameter(description = "Subscription creation data", required = true)
          @Valid
          @RequestBody
          @P("request")
          CreateSubscriptionRequest request) {
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

  /**
   * Retrieves the current subscription for a specific organization.
   *
   * @param userPrincipal The authenticated user principal.
   * @param organizationId The ID of the organization whose subscription is to be retrieved.
   * @return A {@link ResponseEntity} containing the {@link SubscriptionResponse} if found,
   *     otherwise a 404 Not Found response.
   */
  @GetMapping("/organizations/{organizationId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  @Operation(
      summary = "Get organization subscription",
      description = "Retrieve the current subscription for an organization")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Subscription found"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
      })
  public ResponseEntity<SubscriptionResponse> getOrganizationSubscription(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    Optional<SubscriptionResponse> subscription =
        subscriptionManagementService.findByOrganizationId(organizationId);
    return subscription.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  /**
   * Changes the plan for an existing subscription.
   *
   * @param userPrincipal The authenticated user principal.
   * @param subscriptionId The ID of the subscription to change.
   * @param request The request body containing the new plan details.
   * @return A {@link ResponseEntity} with the updated {@link SubscriptionResponse}.
   */
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

  /**
   * Cancels an active subscription.
   *
   * @param userPrincipal The authenticated user principal.
   * @param subscriptionId The ID of the subscription to cancel.
   * @param request The request body specifying cancellation options.
   * @return A {@link ResponseEntity} with the updated {@link SubscriptionResponse}.
   */
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

  /**
   * Reactivates a canceled subscription.
   *
   * @param userPrincipal The authenticated user principal.
   * @param subscriptionId The ID of the subscription to reactivate.
   * @param request The request body containing the organization ID.
   * @return A {@link ResponseEntity} with the updated {@link SubscriptionResponse}.
   */
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

  /**
   * Retrieves usage and billing statistics for an organization's subscription.
   *
   * @param userPrincipal The authenticated user principal.
   * @param organizationId The ID of the organization.
   * @return A {@link ResponseEntity} with the {@link SubscriptionStatisticsResponse}.
   */
  @GetMapping("/organizations/{organizationId}/statistics")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<SubscriptionStatisticsResponse> getSubscriptionStatistics(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    SubscriptionStatisticsResponse stats =
        subscriptionManagementService.getSubscriptionStatistics(organizationId);
    return ResponseEntity.ok(stats);
  }

  /**
   * Retrieves a list of invoices for an organization.
   *
   * @param userPrincipal The authenticated user principal.
   * @param organizationId The ID of the organization.
   * @return A {@link ResponseEntity} with a list of {@link InvoiceResponse}.
   */
  @GetMapping("/organizations/{organizationId}/invoices")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<InvoiceResponse>> getOrganizationInvoices(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    List<InvoiceResponse> invoices =
        subscriptionManagementService.getOrganizationInvoices(organizationId);
    return ResponseEntity.ok(invoices);
  }
}
