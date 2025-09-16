package com.platform.contract;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests for Subscription API endpoints.
 * These tests validate API contracts defined in specs/005-spring-boot-modulith/contracts/subscription-api.yml
 *
 * CRITICAL: These tests MUST FAIL initially as no implementation exists yet.
 * This follows the TDD RED-GREEN-Refactor cycle.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Subscription API Contract Tests")
class SubscriptionApiContractTest {

    @LocalServerPort
    private int port;

    private String validAuthToken = "Bearer test-valid-token";
    private String organizationId = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("GET /plans - should return available subscription plans")
    void shouldReturnAvailablePlans() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/plans")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", isA(java.util.List.class))
            .body("$", hasSize(greaterThan(0)))
            .body("[0].id", notNullValue())
            .body("[0].name", notNullValue())
            .body("[0].amount", isA(java.math.BigDecimal.class))
            .body("[0].currency", equalTo("USD"))
            .body("[0].interval", isIn(java.util.Arrays.asList("month", "year")))
            .body("[0].features", notNullValue())
            .body("[0].active", equalTo(true));
    }

    @Test
    @DisplayName("GET /plans/{planId} - should return specific plan details")
    void shouldReturnPlanDetails() {
        String planId = "550e8400-e29b-41d4-a716-446655440000";

        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/plans/{planId}", planId)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(planId))
            .body("name", notNullValue())
            .body("stripePriceId", notNullValue())
            .body("amount", isA(java.math.BigDecimal.class))
            .body("currency", notNullValue())
            .body("features.maxUsers", isA(Integer.class))
            .body("features.apiCallsPerMonth", isA(Integer.class));
    }

    @Test
    @DisplayName("POST /subscriptions - should create new subscription")
    void shouldCreateNewSubscription() {
        var subscriptionRequest = Map.of(
            "planId", "550e8400-e29b-41d4-a716-446655440000",
            "organizationId", organizationId,
            "paymentMethodId", "pm_card_visa"
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .body(subscriptionRequest)
        .when()
            .post("/api/v1/subscriptions")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("id", notNullValue())
            .body("organizationId", equalTo(organizationId))
            .body("planId", notNullValue())
            .body("status", isIn(java.util.Arrays.asList("trialing", "active", "incomplete")))
            .body("currentPeriodStart", notNullValue())
            .body("currentPeriodEnd", notNullValue())
            .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("GET /subscriptions - should return organization subscriptions")
    void shouldReturnOrganizationSubscriptions() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .queryParam("organizationId", organizationId)
        .when()
            .get("/api/v1/subscriptions")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", isA(java.util.List.class))
            .body("$", everyItem(hasKey("id")))
            .body("$", everyItem(hasKey("status")))
            .body("$", everyItem(hasKey("planId")))
            .body("$", everyItem(hasKey("organizationId")));
    }

    @Test
    @DisplayName("GET /subscriptions/{subscriptionId} - should return subscription details")
    void shouldReturnSubscriptionDetails() {
        String subscriptionId = "sub_test_subscription_id";

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
        .when()
            .get("/api/v1/subscriptions/{subscriptionId}", subscriptionId)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(subscriptionId))
            .body("organizationId", notNullValue())
            .body("plan.id", notNullValue())
            .body("plan.name", notNullValue())
            .body("status", notNullValue())
            .body("currentPeriodStart", notNullValue())
            .body("currentPeriodEnd", notNullValue());
    }

    @Test
    @DisplayName("PUT /subscriptions/{subscriptionId} - should update subscription plan")
    void shouldUpdateSubscriptionPlan() {
        String subscriptionId = "sub_test_subscription_id";
        var updateRequest = Map.of(
            "planId", "550e8400-e29b-41d4-a716-446655440001"
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .body(updateRequest)
        .when()
            .put("/api/v1/subscriptions/{subscriptionId}", subscriptionId)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(subscriptionId))
            .body("planId", equalTo("550e8400-e29b-41d4-a716-446655440001"))
            .body("updatedAt", notNullValue());
    }

    @Test
    @DisplayName("DELETE /subscriptions/{subscriptionId} - should cancel subscription")
    void shouldCancelSubscription() {
        String subscriptionId = "sub_test_subscription_id";

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
        .when()
            .delete("/api/v1/subscriptions/{subscriptionId}", subscriptionId)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(subscriptionId))
            .body("status", equalTo("canceled"))
            .body("canceledAt", notNullValue());
    }

    @Test
    @DisplayName("GET /subscriptions/{subscriptionId}/usage - should return usage metrics")
    void shouldReturnUsageMetrics() {
        String subscriptionId = "sub_test_subscription_id";

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .queryParam("startDate", "2024-01-01")
            .queryParam("endDate", "2024-01-31")
        .when()
            .get("/api/v1/subscriptions/{subscriptionId}/usage", subscriptionId)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("subscriptionId", equalTo(subscriptionId))
            .body("period.start", equalTo("2024-01-01"))
            .body("period.end", equalTo("2024-01-31"))
            .body("metrics", isA(java.util.List.class))
            .body("metrics", everyItem(hasKey("name")))
            .body("metrics", everyItem(hasKey("usage")))
            .body("metrics", everyItem(hasKey("limit")));
    }

    @Test
    @DisplayName("POST /subscriptions/{subscriptionId}/usage - should record usage")
    void shouldRecordUsage() {
        String subscriptionId = "sub_test_subscription_id";
        var usageRequest = Map.of(
            "metricName", "api_calls",
            "quantity", 150,
            "timestamp", "2024-01-15T10:30:00Z"
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .body(usageRequest)
        .when()
            .post("/api/v1/subscriptions/{subscriptionId}/usage", subscriptionId)
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("subscriptionId", equalTo(subscriptionId))
            .body("metricName", equalTo("api_calls"))
            .body("quantity", equalTo(150))
            .body("recordedAt", notNullValue());
    }

    @Test
    @DisplayName("Should reject subscription creation without authentication")
    void shouldRejectUnauthenticatedSubscriptionCreation() {
        var subscriptionRequest = Map.of(
            "planId", "550e8400-e29b-41d4-a716-446655440000",
            "organizationId", organizationId
        );

        given()
            .contentType(ContentType.JSON)
            .body(subscriptionRequest)
        .when()
            .post("/api/v1/subscriptions")
        .then()
            .statusCode(401)
            .contentType(ContentType.JSON)
            .body("error", equalTo("UNAUTHORIZED"))
            .body("message", containsString("authentication"));
    }

    @Test
    @DisplayName("Should validate organization ownership for subscription operations")
    void shouldValidateOrganizationOwnership() {
        String unauthorizedOrgId = "550e8400-e29b-41d4-a716-446655440999";
        var subscriptionRequest = Map.of(
            "planId", "550e8400-e29b-41d4-a716-446655440000",
            "organizationId", unauthorizedOrgId
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .body(subscriptionRequest)
        .when()
            .post("/api/v1/subscriptions")
        .then()
            .statusCode(403)
            .contentType(ContentType.JSON)
            .body("error", equalTo("FORBIDDEN"))
            .body("message", containsString("organization"));
    }

    @Test
    @DisplayName("Should reject invalid plan ID")
    void shouldRejectInvalidPlanId() {
        var invalidRequest = Map.of(
            "planId", "invalid-plan-id",
            "organizationId", organizationId
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
            .body(invalidRequest)
        .when()
            .post("/api/v1/subscriptions")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", equalTo("VALIDATION_ERROR"))
            .body("details.planId", containsString("valid UUID"));
    }

    @Test
    @DisplayName("Should handle subscription not found")
    void shouldHandleSubscriptionNotFound() {
        String nonExistentId = "sub_nonexistent_subscription";

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", validAuthToken)
        .when()
            .get("/api/v1/subscriptions/{subscriptionId}", nonExistentId)
        .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .body("error", equalTo("SUBSCRIPTION_NOT_FOUND"))
            .body("message", containsString(nonExistentId));
    }
}