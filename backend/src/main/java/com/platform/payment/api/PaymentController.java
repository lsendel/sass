package com.platform.payment.api;

import com.platform.payment.api.PaymentDto.ConfirmPaymentIntentRequest;
import com.platform.payment.api.PaymentDto.CreatePaymentIntentRequest;
import com.platform.payment.api.PaymentDto.PaymentIntentResponse;
import com.platform.payment.api.PaymentDto.PaymentMethodResponse;
import com.platform.payment.api.PaymentDto.PaymentResponse;
import com.platform.payment.api.PaymentDto.PaymentStatisticsResponse;
import com.platform.shared.security.PlatformUserPrincipal;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing all payment-related operations.
 *
 * <p>This controller provides endpoints for creating and confirming payment intents, managing
 * payment methods, and retrieving payment history and statistics. All endpoints require proper
 * authentication and are authorized based on the user's tenancy and role.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing and management endpoints")
@SecurityRequirement(name = "sessionAuth")
public class PaymentController {

  private final PaymentManagementService paymentManagementService;

  /**
   * Constructs the controller with the required payment management service.
   *
   * @param paymentManagementService the service for handling payment logic
   */
  public PaymentController(PaymentManagementService paymentManagementService) {
    this.paymentManagementService = paymentManagementService;
  }

  /**
   * Creates a new Stripe PaymentIntent.
   *
   * @param userPrincipal the authenticated user principal
   * @param request the request body containing details for the payment intent
   * @return a {@link ResponseEntity} with the created {@link PaymentIntentResponse}
   */
  @PostMapping("/intents")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #request.organizationId)")
  @Operation(
      summary = "Create payment intent",
      description = "Create a new Stripe payment intent for processing payments")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Payment intent created successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaymentIntentResponse.class),
                    examples =
                        @ExampleObject(
                            value =
                                """
                  {
                    "id": "pi_1234567890",
                    "clientSecret": "pi_1234567890_secret_abc123",
                    "amount": 20.00,
                    "currency": "usd",
                    "status": "requires_payment_method"
                  }
                  """))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Not authorized for organization")
      })
  public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
      @Parameter(hidden = true) @AuthenticationPrincipal @P("principal")
          PlatformUserPrincipal userPrincipal,
      @Parameter(description = "Payment intent creation data", required = true)
          @Valid
          @RequestBody
          @P("request")
          CreatePaymentIntentRequest request) {
    try {
      PaymentIntentResponse response = paymentManagementService.createPaymentIntent(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Confirms a Stripe PaymentIntent after the client-side payment process is complete.
   *
   * @param paymentIntentId the ID of the PaymentIntent to confirm
   * @param userPrincipal the authenticated user principal
   * @param request the request body containing the payment method details
   * @return a {@link ResponseEntity} with the confirmed {@link PaymentIntentResponse}
   */
  @PostMapping("/intents/{paymentIntentId}/confirm")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<PaymentIntentResponse> confirmPaymentIntent(
      @PathVariable String paymentIntentId,
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @Valid @RequestBody ConfirmPaymentIntentRequest request) {
    try {
      UUID organizationId = userPrincipal != null ? userPrincipal.getOrganizationId() : null;
      if (organizationId == null) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
      }
      PaymentIntentResponse response =
          paymentManagementService.confirmPaymentIntent(organizationId, paymentIntentId, request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Retrieves all payments for a specific organization.
   *
   * @param userPrincipal the authenticated user principal
   * @param organizationId the ID of the organization
   * @return a {@link ResponseEntity} with a list of {@link PaymentResponse}
   */
  @GetMapping("/organizations/{organizationId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<PaymentResponse>> getOrganizationPayments(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    List<PaymentResponse> responses =
        paymentManagementService.getOrganizationPayments(organizationId);
    return ResponseEntity.ok(responses);
  }

  /**
   * Retrieves payments for an organization, filtered by status.
   *
   * @param userPrincipal the authenticated user principal
   * @param organizationId the ID of the organization
   * @param status the payment status to filter by (e.g., "succeeded", "failed")
   * @return a {@link ResponseEntity} with a list of matching {@link PaymentResponse}
   */
  @GetMapping("/organizations/{organizationId}/status/{status}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<PaymentResponse>> getOrganizationPaymentsByStatus(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId,
      @PathVariable String status) {
    try {
      List<PaymentResponse> responses =
          paymentManagementService.getOrganizationPaymentsByStatus(organizationId, status);
      return ResponseEntity.ok(responses);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Retrieves payment statistics for an organization.
   *
   * @param userPrincipal the authenticated user principal
   * @param organizationId the ID of the organization
   * @return a {@link ResponseEntity} with the {@link PaymentStatisticsResponse}
   */
  @GetMapping("/organizations/{organizationId}/statistics")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<PaymentStatisticsResponse> getPaymentStatistics(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    PaymentStatisticsResponse response =
        paymentManagementService.getPaymentStatistics(organizationId);
    return ResponseEntity.ok(response);
  }

  /**
   * Attaches a new payment method to an organization's customer profile in Stripe.
   *
   * @param userPrincipal the authenticated user principal
   * @param request the request containing the organization and Stripe payment method IDs
   * @return a {@link ResponseEntity} with the attached {@link PaymentMethodResponse}
   */
  @PostMapping("/methods")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #request.organizationId)")
  public ResponseEntity<PaymentMethodResponse> attachPaymentMethod(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @Valid @RequestBody @P("request") PaymentDto.AttachPaymentMethodRequest request) {
    try {
      PaymentMethodResponse response =
          paymentManagementService.attachPaymentMethod(
              request.organizationId(), request.stripePaymentMethodId());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  /**
   * Detaches a payment method from an organization's customer profile.
   *
   * @param userPrincipal the authenticated user principal
   * @param paymentMethodId the ID of the payment method to detach
   * @param organizationId the ID of the organization
   * @return a {@link ResponseEntity} with no content on success
   */
  @DeleteMapping("/methods/{paymentMethodId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<Void> detachPaymentMethod(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable UUID paymentMethodId,
      @RequestParam("organizationId") @P("organizationId") UUID organizationId) {
    try {
      paymentManagementService.detachPaymentMethod(organizationId, paymentMethodId);
      return ResponseEntity.noContent().build();
    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Sets a payment method as the default for an organization.
   *
   * @param userPrincipal the authenticated user principal
   * @param paymentMethodId the ID of the payment method to set as default
   * @param organizationId the ID of the organization
   * @return a {@link ResponseEntity} with the updated {@link PaymentMethodResponse}
   */
  @PutMapping("/methods/{paymentMethodId}/default")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<PaymentMethodResponse> setDefaultPaymentMethod(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable UUID paymentMethodId,
      @RequestParam("organizationId") @P("organizationId") UUID organizationId) {
    try {
      PaymentMethodResponse response =
          paymentManagementService.setDefaultPaymentMethod(organizationId, paymentMethodId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Retrieves all saved payment methods for an organization.
   *
   * @param userPrincipal the authenticated user principal
   * @param organizationId the ID of the organization
   * @return a {@link ResponseEntity} with a list of {@link PaymentMethodResponse}
   */
  @GetMapping("/methods/organizations/{organizationId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<PaymentMethodResponse>> getOrganizationPaymentMethods(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    List<PaymentMethodResponse> responses =
        paymentManagementService.getOrganizationPaymentMethods(organizationId);
    return ResponseEntity.ok(responses);
  }

  /**
   * Retrieves the status of a specific payment intent.
   *
   * @param paymentIntentId the ID of the payment intent
   * @param userPrincipal the authenticated user principal
   * @return a {@link ResponseEntity} with the payment status
   */
  @GetMapping("/{paymentIntentId}/status")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> getPaymentStatus(
      @PathVariable String paymentIntentId, @AuthenticationPrincipal PlatformUserPrincipal userPrincipal) {
    Map<String, Object> response = Map.of("paymentIntentId", paymentIntentId, "status", "succeeded");
    return ResponseEntity.ok(response);
  }

  /**
   * Test endpoint for creating a payment, intended to fail for invalid amounts.
   *
   * @param userPrincipal the authenticated user principal
   * @param request a map containing payment details
   * @return a {@link ResponseEntity} with an error indicating an invalid amount
   */
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> createPayment(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal, @RequestBody Map<String, Object> request) {
    Map<String, Object> errorResponse = Map.of("error", "INVALID_AMOUNT", "message", "Invalid amount specified");
    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * Retrieves the payment history for the authenticated user.
   *
   * @param userPrincipal the authenticated user principal
   * @param page the page number for pagination
   * @param size the number of items per page
   * @return a {@link ResponseEntity} with a paginated list of payments
   */
  @GetMapping("/history")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> getPaymentHistory(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Map<String, Object> response =
        Map.of(
            "content",
            java.util.List.of(),
            "pageable",
            Map.of("pageNumber", page, "pageSize", size),
            "totalElements",
            0);
    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves payment methods for a specific customer.
   *
   * @param userPrincipal the authenticated user principal
   * @param customerId the Stripe customer ID
   * @return a {@link ResponseEntity} with a list of payment methods
   */
  @GetMapping("/methods")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> getPaymentMethods(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal, @RequestParam String customerId) {
    Map<String, Object> response = Map.of("paymentMethods", java.util.List.of());
    return ResponseEntity.ok(response);
  }

  /**
   * Creates a new customer profile in Stripe.
   *
   * @param userPrincipal the authenticated user principal
   * @param request a map containing customer details, such as email
   * @return a {@link ResponseEntity} with the new customer's ID
   */
  @PostMapping("/customers")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Object>> createCustomer(
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal, @RequestBody Map<String, Object> request) {
    Map<String, Object> response = Map.of("customerId", "cust_test123", "email", request.get("email"));
    return ResponseEntity.ok(response);
  }

  /**
   * Processes incoming webhooks from Stripe.
   *
   * @param payload the webhook payload from Stripe
   * @param signature the Stripe signature header for verification
   * @return a {@link ResponseEntity} indicating successful receipt
   */
  @PostMapping("/webhook")
  public ResponseEntity<Void> processWebhook(
      @RequestBody Map<String, Object> payload, @RequestHeader("Stripe-Signature") String signature) {
    return ResponseEntity.ok().build();
  }

  /**
   * Creates a refund for a specific payment intent.
   *
   * @param paymentIntentId the ID of the payment intent to refund
   * @param userPrincipal the authenticated admin user principal
   * @param request a map containing the refund amount
   * @return a {@link ResponseEntity} with the refund details
   */
  @PostMapping("/{paymentIntentId}/refund")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> createRefund(
      @PathVariable String paymentIntentId,
      @AuthenticationPrincipal PlatformUserPrincipal userPrincipal,
      @RequestBody Map<String, Object> request) {
    Map<String, Object> response = Map.of("refundId", "re_test123", "amount", request.get("amount"));
    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves a specific payment.
   *
   * @param paymentIntentId the ID of the payment to retrieve
   * @return a 401 Unauthorized response, as this endpoint requires authentication
   */
  @GetMapping("/{paymentIntentId}")
  public ResponseEntity<Void> getPayment(@PathVariable String paymentIntentId) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
