package com.platform.contract;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests for Payment API endpoints.
 * These tests validate API contracts defined in specs/005-spring-boot-modulith/contracts/payment-api.yml
 *
 * CRITICAL: These tests MUST FAIL initially as no implementation exists yet.
 * This follows the TDD RED-GREEN-Refactor cycle.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Payment API Contract Tests")
class PaymentApiContractTest {

    @LocalServerPort
    private int port;

    private String validAuthToken = "Bearer test-valid-token";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("POST /payments/intent - should create payment intent")
    void shouldCreatePaymentIntent() {
        var paymentIntentRequest = Map.of(
            "amount", 2999,
            "currency", "USD",
            "paymentMethodId", "pm_card_visa",
            "organizationId", "550e8400-e29b-41d4-a716-446655440000"
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .body(paymentIntentRequest)
        .when()
            .post("/api/v1/payments/intent")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("id", notNullValue())
            .body("clientSecret", notNullValue())
            .body("status", equalTo("requires_confirmation"))
            .body("amount", equalTo(2999))
            .body("currency", equalTo("USD"));
    }

    @Test
    @DisplayName("POST /payments/intent - should reject invalid amount")
    void shouldRejectInvalidAmount() {
        var invalidRequest = Map.of(
            "amount", -100, // Negative amount
            "currency", "USD",
            "paymentMethodId", "pm_card_visa",
            "organizationId", "550e8400-e29b-41d4-a716-446655440000"
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .body(invalidRequest)
        .when()
            .post("/api/v1/payments/intent")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", equalTo("VALIDATION_ERROR"))
            .body("message", containsString("amount"))
            .body("details.amount", containsString("positive"));
    }

    @Test
    @DisplayName("GET /payments/{paymentId} - should return payment details")
    void shouldReturnPaymentDetails() {
        String paymentId = "pi_test_payment_intent_id";

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
        .when()
            .get("/api/v1/payments/{paymentId}", paymentId)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(paymentId))
            .body("amount", isA(Integer.class))
            .body("currency", notNullValue())
            .body("status", notNullValue())
            .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("GET /payments/{paymentId} - should return 404 for non-existent payment")
    void shouldReturn404ForNonExistentPayment() {
        String nonExistentId = "pi_nonexistent_payment";

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
        .when()
            .get("/api/v1/payments/{paymentId}", nonExistentId)
        .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .body("error", equalTo("PAYMENT_NOT_FOUND"))
            .body("message", containsString("payment"))
            .body("message", containsString(nonExistentId));
    }

    @Test
    @DisplayName("GET /payments - should return user's payment history")
    void shouldReturnPaymentHistory() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/v1/payments")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("content", isA(java.util.List.class))
            .body("page", isA(Integer.class))
            .body("size", isA(Integer.class))
            .body("totalElements", isA(Integer.class))
            .body("totalPages", isA(Integer.class));
    }

    @Test
    @DisplayName("POST /payments/{paymentId}/confirm - should confirm payment")
    void shouldConfirmPayment() {
        String paymentId = "pi_test_requires_confirmation";

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
        .when()
            .post("/api/v1/payments/{paymentId}/confirm", paymentId)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(paymentId))
            .body("status", equalTo("succeeded"))
            .body("confirmedAt", notNullValue());
    }

    @Test
    @DisplayName("POST /webhooks/stripe - should handle Stripe webhook")
    void shouldHandleStripeWebhook() {
        var webhookPayload = Map.of(
            "id", "evt_test_webhook",
            "object", "event",
            "type", "payment_intent.succeeded",
            "data", Map.of(
                "object", Map.of(
                    "id", "pi_test_payment_intent",
                    "status", "succeeded"
                )
            )
        );

        given()
            .contentType(ContentType.JSON)
            .header("Stripe-Signature", "t=1234567890,v1=test_signature")
            .body(webhookPayload)
        .when()
            .post("/api/v1/webhooks/stripe")
        .then()
            .statusCode(200)
            .body(equalTo("OK"));
    }

    @Test
    @DisplayName("POST /webhooks/stripe - should reject invalid signature")
    void shouldRejectInvalidWebhookSignature() {
        var webhookPayload = Map.of(
            "id", "evt_test_webhook",
            "type", "payment_intent.succeeded"
        );

        given()
            .contentType(ContentType.JSON)
            .header("Stripe-Signature", "invalid_signature")
            .body(webhookPayload)
        .when()
            .post("/api/v1/webhooks/stripe")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", equalTo("INVALID_SIGNATURE"))
            .body("message", containsString("signature"));
    }

    @Test
    @DisplayName("All payment endpoints should require authentication")
    void shouldRequireAuthentication() {
        given()
            .contentType(ContentType.JSON)
            // No Authorization header
        .when()
            .get("/api/v1/payments")
        .then()
            .statusCode(401)
            .contentType(ContentType.JSON)
            .body("error", equalTo("UNAUTHORIZED"))
            .body("message", containsString("authentication"));
    }

    @Test
    @DisplayName("Payment endpoints should validate organization access")
    void shouldValidateOrganizationAccess() {
        String unauthorizedOrgId = "550e8400-e29b-41d4-a716-446655440001";

        var paymentRequest = Map.of(
            "amount", 1000,
            "currency", "USD",
            "paymentMethodId", "pm_card_visa",
            "organizationId", unauthorizedOrgId
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .body(paymentRequest)
        .when()
            .post("/api/v1/payments/intent")
        .then()
            .statusCode(403)
            .contentType(ContentType.JSON)
            .body("error", equalTo("FORBIDDEN"))
            .body("message", containsString("organization"));
    }

    @Test
    @DisplayName("Should validate currency codes")
    void shouldValidateCurrencyCodes() {
        var invalidCurrencyRequest = Map.of(
            "amount", 1000,
            "currency", "INVALID",
            "paymentMethodId", "pm_card_visa",
            "organizationId", "550e8400-e29b-41d4-a716-446655440000"
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .body(invalidCurrencyRequest)
        .when()
            .post("/api/v1/payments/intent")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", equalTo("VALIDATION_ERROR"))
            .body("details.currency", containsString("valid currency"));
    }
}