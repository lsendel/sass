package com.platform.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import com.platform.payment.api.PaymentDto.PaymentMethodResponse;
import com.platform.payment.api.PaymentDto.PaymentResponse;
import com.platform.payment.api.PaymentDto.PaymentStatisticsResponse;
import com.platform.shared.types.Money;
import com.platform.shared.security.PlatformUserPrincipal;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

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
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing and management endpoints")
@SecurityRequirement(name = "sessionAuth")
public class PaymentController {

  private final PaymentManagementService paymentManagementService;

  public PaymentController(PaymentManagementService paymentManagementService) {
    this.paymentManagementService = paymentManagementService;
  }

  @PostMapping("/intents")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #request.organizationId)")
  @Operation(
      summary = "Create payment intent",
      description = "Create a new Stripe payment intent for processing payments"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Payment intent created successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PaymentIntentResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "pi_1234567890",
                    "clientSecret": "pi_1234567890_secret_abc123",
                    "amount": 2000,
                    "currency": "usd",
                    "status": "requires_payment_method"
                  }
                  """)
          )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "403", description = "Not authorized for organization")
  })
  public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
      @Parameter(hidden = true)
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @Parameter(description = "Payment intent creation data", required = true)
      @Valid @RequestBody @P("request") CreatePaymentIntentRequest request) {
    try {
      // PaymentIntent functionality needs to be moved to PaymentManagementService
      throw new UnsupportedOperationException("Method not yet implemented - needs PaymentIntent support in management service");

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @PostMapping("/intents/{paymentIntentId}/confirm")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<PaymentIntentResponse> confirmPaymentIntent(
      @PathVariable String paymentIntentId,
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @Valid @RequestBody ConfirmPaymentIntentRequest request) {

    try {
      // PaymentIntent functionality needs to be moved to PaymentManagementService
      throw new UnsupportedOperationException("Method not yet implemented - needs PaymentIntent support in management service");

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping("/organizations/{organizationId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<PaymentResponse>> getOrganizationPayments(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    List<PaymentResponse> responses = paymentManagementService.getOrganizationPayments(organizationId);
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/organizations/{organizationId}/status/{status}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<PaymentResponse>> getOrganizationPaymentsByStatus(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId,
      @PathVariable String status) {

    try {
      // This method needs to be implemented in PaymentManagementService
      throw new UnsupportedOperationException("Method not yet implemented");

    } catch (SecurityException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

  @GetMapping("/organizations/{organizationId}/statistics")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<PaymentStatisticsResponse> getPaymentStatistics(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    PaymentStatisticsResponse response = paymentManagementService.getPaymentStatistics(organizationId);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/methods")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #request.organizationId)")
  public ResponseEntity<PaymentMethodResponse> attachPaymentMethod(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @Valid @RequestBody @P("request") PaymentDto.AttachPaymentMethodRequest request) {
    try {
      PaymentMethodResponse response = paymentManagementService.attachPaymentMethod(
          request.organizationId(), request.stripePaymentMethodId());

      return ResponseEntity.status(HttpStatus.CREATED).body(response);

    } catch (StripeException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

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

  @PutMapping("/methods/{paymentMethodId}/default")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<PaymentMethodResponse> setDefaultPaymentMethod(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable UUID paymentMethodId,
      @RequestParam("organizationId") @P("organizationId") UUID organizationId) {

    try {
      PaymentMethodResponse response = paymentManagementService.setDefaultPaymentMethod(
          organizationId, paymentMethodId);
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/methods/organizations/{organizationId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<PaymentMethodResponse>> getOrganizationPaymentMethods(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal userPrincipal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    List<PaymentMethodResponse> responses =
        paymentManagementService.getOrganizationPaymentMethods(organizationId);
    return ResponseEntity.ok(responses);
  }

  // Request DTOs
  public record CreatePaymentIntentRequest(
      @NotNull UUID organizationId,
      @NotNull @Positive BigDecimal amount,
      @NotBlank String currency,
      String description,
      Map<String, String> metadata) {}

  public record ConfirmPaymentIntentRequest(@NotBlank String paymentMethodId) {}

  // Response DTOs
  public record PaymentIntentResponse(
      String id,
      String clientSecret,
      String status,
      BigDecimal amount,
      String currency,
      String description,
      Map<String, String> metadata) {
    public static PaymentIntentResponse fromStripePaymentIntent(PaymentIntent intent) {
      return new PaymentIntentResponse(
          intent.getId(),
          intent.getClientSecret(),
          intent.getStatus(),
          BigDecimal.valueOf(intent.getAmount(), 2),
          intent.getCurrency(),
          intent.getDescription(),
          intent.getMetadata());
    }
  }

}
