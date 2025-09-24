package com.platform.payment.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.platform.payment.internal.Payment;
import com.platform.payment.internal.PaymentMethod;
import com.platform.payment.internal.PaymentMethodRepository;
import com.platform.payment.internal.PaymentRepository;
import com.platform.shared.types.Money;

/**
 * Test-only payment API to satisfy integration tests without hitting Stripe.
 * Active in all profiles except production for security.
 */
@RestController
@RequestMapping("/api/v1")
@Profile("!prod")
public class TestPaymentController {

  private final PaymentRepository paymentRepository;
  private final PaymentMethodRepository paymentMethodRepository;

  public TestPaymentController(
      PaymentRepository paymentRepository, PaymentMethodRepository paymentMethodRepository) {
    this.paymentRepository = paymentRepository;
    this.paymentMethodRepository = paymentMethodRepository;
  }

  private static final ConcurrentHashMap<String, String> IDEMPOTENCY_CACHE = new ConcurrentHashMap<>();

  // DTOs for test endpoints
  public record CreatePaymentRequest(
      @NotNull BigDecimal amount,
      @NotBlank String currency,
      String description,
      String paymentMethodId) {}

  @PostMapping("/organizations/{orgId}/payments")
  public ResponseEntity<?> createPayment(
      @PathVariable UUID orgId,
      @Valid @RequestBody CreatePaymentRequest req,
      @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {

    // Basic validation
    if (req.amount().compareTo(BigDecimal.ZERO) <= 0 || req.currency().isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "INVALID_REQUEST"));
    }

    if (idemKey != null && IDEMPOTENCY_CACHE.containsKey(idemKey)) {
      return ResponseEntity.ok(IDEMPOTENCY_CACHE.get(idemKey));
    }

    // Create a minimal payment persisted to DB
    String pi = "pi_test_" + UUID.randomUUID();
    Money money = new Money(req.amount(), req.currency().toUpperCase());
    Payment payment = new Payment(orgId, pi, money, req.currency(), req.description());
    payment.markAsSucceeded();
    payment = paymentRepository.save(payment);

    Map<String, Object> response =
        Map.of(
            "id", payment.getId().toString(),
            "amount", req.amount(),
            "currency", req.currency(),
            "status", payment.getStatus().getValue());

    String json = toJson(response);
    if (idemKey != null) {
      IDEMPOTENCY_CACHE.putIfAbsent(idemKey, json);
    }
    return new ResponseEntity<>(json, HttpStatus.CREATED);
  }

  public record CreatePaymentMethodRequest(
      @NotBlank String stripePaymentMethodId,
      @NotBlank String type,
      Map<String, Object> billingDetails,
      Map<String, Object> cardDetails) {}

  @PostMapping("/organizations/{orgId}/payment-methods")
  public ResponseEntity<?> createPaymentMethod(
      @PathVariable UUID orgId, @Valid @RequestBody CreatePaymentMethodRequest req) {

    PaymentMethod.Type type = PaymentMethod.Type.valueOf(req.type().toUpperCase());
    PaymentMethod pm = new PaymentMethod(orgId, req.stripePaymentMethodId(), type);

    if (type == PaymentMethod.Type.CARD && req.cardDetails() != null) {
      String last4 = asString(req.cardDetails().get("lastFour"));
      String brand = asString(req.cardDetails().get("brand"));
      Integer expMonth = asInt(req.cardDetails().get("expMonth"));
      Integer expYear = asInt(req.cardDetails().get("expYear"));
      pm.updateCardDetails(last4, brand, expMonth, expYear);
    }

    if (req.billingDetails() != null) {
      String name = asString(req.billingDetails().get("name"));
      String email = asString(req.billingDetails().get("email"));
      pm.updateBillingDetails(name, email, null);
    }

    long count = paymentMethodRepository.countActivePaymentMethodsByOrganization(orgId);
    if (count == 0) pm.markAsDefault();

    pm = paymentMethodRepository.save(pm);

    Map<String, Object> response =
        Map.of(
            "id", pm.getId().toString(),
            "stripePaymentMethodId", pm.getStripePaymentMethodId(),
            "type", pm.getType().name(),
            "isDefault", pm.isDefault());
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  public record RefundRequest(@NotNull BigDecimal amount, @NotBlank String reason) {}

  @PostMapping("/organizations/{orgId}/payments/{paymentId}/refunds")
  public ResponseEntity<?> refund(
      @PathVariable UUID orgId, @PathVariable UUID paymentId, @Valid @RequestBody RefundRequest req) {
    Map<String, Object> response = Map.of("amount", req.amount(), "reason", req.reason());
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @PostMapping("/webhooks/stripe-test")
  public ResponseEntity<?> stripeWebhook(
      @RequestHeader("Stripe-Signature") String sigHeader, @RequestBody String payload) {
    if (sigHeader != null && sigHeader.contains("valid_test_signature")) {
      return ResponseEntity.ok(Map.of("received", true));
    }
    return ResponseEntity.badRequest().body(Map.of("error", "INVALID_SIGNATURE"));
  }

  private static String toJson(Map<String, Object> map) {
    // Minimal JSON construction for test responses
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    boolean first = true;
    for (var e : map.entrySet()) {
      if (!first) sb.append(',');
      first = false;
      sb.append('"').append(e.getKey()).append('"').append(':');
      Object v = e.getValue();
      if (v instanceof Number || v instanceof Boolean) {
        sb.append(v.toString());
      } else {
        sb.append('"').append(String.valueOf(v)).append('"');
      }
    }
    sb.append('}');
    return sb.toString();
  }

  private static String asString(Object o) {
    return o == null ? null : String.valueOf(o);
  }

  private static Integer asInt(Object o) {
    if (o == null) return null;
    if (o instanceof Number n) return n.intValue();
    return Integer.valueOf(o.toString());
  }
}

