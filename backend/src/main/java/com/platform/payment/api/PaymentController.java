package com.platform.payment.api;

import com.platform.payment.internal.*;
import com.platform.shared.types.Money;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/intents")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(@Valid @RequestBody CreatePaymentIntentRequest request) {
        try {
            Money amount = new Money(request.amount(), request.currency());
            PaymentIntent stripeIntent = paymentService.createPaymentIntent(
                request.organizationId(),
                amount,
                request.currency(),
                request.description(),
                request.metadata()
            );

            PaymentIntentResponse response = PaymentIntentResponse.fromStripePaymentIntent(stripeIntent);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/intents/{paymentIntentId}/confirm")
    public ResponseEntity<PaymentIntentResponse> confirmPaymentIntent(
            @PathVariable String paymentIntentId,
            @Valid @RequestBody ConfirmPaymentIntentRequest request) {

        try {
            PaymentIntent stripeIntent = paymentService.confirmPaymentIntent(paymentIntentId, request.paymentMethodId());
            PaymentIntentResponse response = PaymentIntentResponse.fromStripePaymentIntent(stripeIntent);
            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/organizations/{organizationId}")
    public ResponseEntity<List<PaymentResponse>> getOrganizationPayments(@PathVariable UUID organizationId) {
        try {
            List<Payment> payments = paymentService.getOrganizationPayments(organizationId);
            List<PaymentResponse> responses = payments.stream()
                .map(PaymentResponse::fromPayment)
                .toList();
            return ResponseEntity.ok(responses);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/organizations/{organizationId}/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getOrganizationPaymentsByStatus(
            @PathVariable UUID organizationId,
            @PathVariable Payment.Status status) {

        try {
            List<Payment> payments = paymentService.getOrganizationPaymentsByStatus(organizationId, status);
            List<PaymentResponse> responses = payments.stream()
                .map(PaymentResponse::fromPayment)
                .toList();
            return ResponseEntity.ok(responses);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/organizations/{organizationId}/statistics")
    public ResponseEntity<PaymentStatisticsResponse> getPaymentStatistics(@PathVariable UUID organizationId) {
        try {
            PaymentService.PaymentStatistics stats = paymentService.getPaymentStatistics(organizationId);
            PaymentStatisticsResponse response = PaymentStatisticsResponse.fromStatistics(stats);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/methods")
    public ResponseEntity<PaymentMethodResponse> attachPaymentMethod(@Valid @RequestBody AttachPaymentMethodRequest request) {
        try {
            PaymentMethod paymentMethod = paymentService.attachPaymentMethod(
                request.organizationId(),
                request.stripePaymentMethodId()
            );

            PaymentMethodResponse response = PaymentMethodResponse.fromPaymentMethod(paymentMethod);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/methods/{paymentMethodId}")
    public ResponseEntity<Void> detachPaymentMethod(
            @PathVariable UUID paymentMethodId,
            @RequestParam UUID organizationId) {

        try {
            paymentService.detachPaymentMethod(organizationId, paymentMethodId);
            return ResponseEntity.noContent().build();

        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/methods/{paymentMethodId}/default")
    public ResponseEntity<PaymentMethodResponse> setDefaultPaymentMethod(
            @PathVariable UUID paymentMethodId,
            @RequestParam UUID organizationId) {

        try {
            PaymentMethod paymentMethod = paymentService.setDefaultPaymentMethod(organizationId, paymentMethodId);
            PaymentMethodResponse response = PaymentMethodResponse.fromPaymentMethod(paymentMethod);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/methods/organizations/{organizationId}")
    public ResponseEntity<List<PaymentMethodResponse>> getOrganizationPaymentMethods(@PathVariable UUID organizationId) {
        try {
            List<PaymentMethod> paymentMethods = paymentService.getOrganizationPaymentMethods(organizationId);
            List<PaymentMethodResponse> responses = paymentMethods.stream()
                .map(PaymentMethodResponse::fromPaymentMethod)
                .toList();
            return ResponseEntity.ok(responses);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // Request DTOs
    public record CreatePaymentIntentRequest(
        @NotNull UUID organizationId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        String description,
        Map<String, String> metadata
    ) {}

    public record ConfirmPaymentIntentRequest(
        @NotBlank String paymentMethodId
    ) {}

    public record AttachPaymentMethodRequest(
        @NotNull UUID organizationId,
        @NotBlank String stripePaymentMethodId
    ) {}

    // Response DTOs
    public record PaymentIntentResponse(
        String id,
        String clientSecret,
        String status,
        BigDecimal amount,
        String currency,
        String description,
        Map<String, String> metadata
    ) {
        public static PaymentIntentResponse fromStripePaymentIntent(PaymentIntent intent) {
            return new PaymentIntentResponse(
                intent.getId(),
                intent.getClientSecret(),
                intent.getStatus(),
                BigDecimal.valueOf(intent.getAmount(), 2),
                intent.getCurrency(),
                intent.getDescription(),
                intent.getMetadata()
            );
        }
    }

    public record PaymentResponse(
        UUID id,
        UUID organizationId,
        String stripePaymentIntentId,
        BigDecimal amount,
        String currency,
        String description,
        Payment.Status status,
        UUID subscriptionId,
        UUID invoiceId,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static PaymentResponse fromPayment(Payment payment) {
            return new PaymentResponse(
                payment.getId(),
                payment.getOrganizationId(),
                payment.getStripePaymentIntentId(),
                payment.getAmount().getAmount(),
                payment.getCurrency(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getSubscriptionId(),
                payment.getInvoiceId(),
                payment.getMetadata(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
            );
        }
    }

    public record PaymentMethodResponse(
        UUID id,
        UUID organizationId,
        String stripePaymentMethodId,
        PaymentMethod.Type type,
        boolean isDefault,
        String displayName,
        CardDetails cardDetails,
        BillingDetails billingDetails,
        Instant createdAt
    ) {
        public static PaymentMethodResponse fromPaymentMethod(PaymentMethod paymentMethod) {
            CardDetails cardDetails = null;
            if (paymentMethod.getType() == PaymentMethod.Type.CARD) {
                cardDetails = new CardDetails(
                    paymentMethod.getLastFour(),
                    paymentMethod.getBrand(),
                    paymentMethod.getExpMonth(),
                    paymentMethod.getExpYear()
                );
            }

            BillingDetails billingDetails = null;
            if (paymentMethod.getBillingName() != null || paymentMethod.getBillingEmail() != null || paymentMethod.getBillingAddress() != null) {
                BillingAddress address = null;
                if (paymentMethod.getBillingAddress() != null) {
                    PaymentMethod.BillingAddress addr = paymentMethod.getBillingAddress();
                    address = new BillingAddress(
                        addr.getAddressLine1(),
                        addr.getAddressLine2(),
                        addr.getCity(),
                        addr.getState(),
                        addr.getPostalCode(),
                        addr.getCountry()
                    );
                }

                billingDetails = new BillingDetails(
                    paymentMethod.getBillingName(),
                    paymentMethod.getBillingEmail(),
                    address
                );
            }

            return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getOrganizationId(),
                paymentMethod.getStripePaymentMethodId(),
                paymentMethod.getType(),
                paymentMethod.isDefault(),
                paymentMethod.getDisplayName(),
                cardDetails,
                billingDetails,
                paymentMethod.getCreatedAt()
            );
        }

        public record CardDetails(
            String lastFour,
            String brand,
            Integer expMonth,
            Integer expYear
        ) {}

        public record BillingDetails(
            String name,
            String email,
            BillingAddress address
        ) {}

        public record BillingAddress(
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country
        ) {}
    }

    public record PaymentStatisticsResponse(
        long totalSuccessfulPayments,
        BigDecimal totalAmount,
        BigDecimal recentAmount
    ) {
        public static PaymentStatisticsResponse fromStatistics(PaymentService.PaymentStatistics stats) {
            return new PaymentStatisticsResponse(
                stats.totalSuccessfulPayments(),
                stats.totalAmount().getAmount(),
                stats.recentAmount().getAmount()
            );
        }
    }
}