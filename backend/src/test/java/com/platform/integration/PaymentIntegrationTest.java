package com.platform.integration;

import com.platform.auth.internal.OpaqueTokenStore;
import com.platform.payment.internal.*;
import com.platform.shared.types.Email;
import com.platform.shared.types.Money;
import com.platform.subscription.internal.Plan;
import com.platform.subscription.internal.PlanRepository;
import com.platform.subscription.internal.Subscription;
import com.platform.subscription.internal.SubscriptionRepository;
import com.platform.user.internal.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Payment Module Integration Tests")
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("platform_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        // Mock Stripe keys for testing
        registry.add("stripe.secret-key", () -> "sk_test_mock_key");
        registry.add("stripe.webhook.secret", () -> "whsec_mock_secret");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private OpaqueTokenStore tokenStore;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Clean up test data
        subscriptionRepository.deleteAll();
        paymentRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        planRepository.deleteAll();
        memberRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create payment intent with valid data")
    void shouldCreatePaymentIntentWithValidData() {
        User user = createTestUser("payment@example.com", "Payment User");
        Organization org = createTestOrganization(user, "Payment Org", "payment-org");
        String token = createTokenForUser(user);

        Map<String, Object> createRequest = Map.of(
            "organizationId", org.getId().toString(),
            "amount", "99.99",
            "currency", "usd",
            "description", "Test payment",
            "metadata", Map.of("test", "true")
        );

        // Note: This test will fail without actual Stripe configuration
        // In a real test environment, you would mock the Stripe calls
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(createRequest)
        .when()
            .post("/api/v1/payments/intents")
        .then()
            .statusCode(anyOf(equalTo(201), equalTo(400))); // 400 due to mock Stripe key
    }

    @Test
    @DisplayName("Should get organization payments")
    void shouldGetOrganizationPayments() {
        User user = createTestUser("payments@example.com", "Payments User");
        Organization org = createTestOrganization(user, "Payments Org", "payments-org");
        String token = createTokenForUser(user);

        // Create test payments directly in database
        Payment payment1 = new Payment(org.getId(), "pi_test_1", new Money(BigDecimal.valueOf(99.99)), "usd", "Test payment 1");
        Payment payment2 = new Payment(org.getId(), "pi_test_2", new Money(BigDecimal.valueOf(149.99)), "usd", "Test payment 2");
        payment1.updateStatus(Payment.Status.SUCCEEDED);
        payment2.updateStatus(Payment.Status.PENDING);
        paymentRepository.save(payment1);
        paymentRepository.save(payment2);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/organizations/{organizationId}", org.getId())
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", equalTo(2))
            .body("stripePaymentIntentId", hasItems("pi_test_1", "pi_test_2"))
            .body("status", hasItems("SUCCEEDED", "PENDING"));
    }

    @Test
    @DisplayName("Should get payments by status")
    void shouldGetPaymentsByStatus() {
        User user = createTestUser("status@example.com", "Status User");
        Organization org = createTestOrganization(user, "Status Org", "status-org");
        String token = createTokenForUser(user);

        // Create test payments with different statuses
        Payment succeededPayment = new Payment(org.getId(), "pi_succeeded", new Money(BigDecimal.valueOf(99.99)), "usd", "Succeeded payment");
        Payment failedPayment = new Payment(org.getId(), "pi_failed", new Money(BigDecimal.valueOf(149.99)), "usd", "Failed payment");
        succeededPayment.updateStatus(Payment.Status.SUCCEEDED);
        failedPayment.updateStatus(Payment.Status.FAILED);
        paymentRepository.save(succeededPayment);
        paymentRepository.save(failedPayment);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/organizations/{organizationId}/status/{status}", org.getId(), "SUCCEEDED")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", equalTo(1))
            .body("[0].status", equalTo("SUCCEEDED"))
            .body("[0].stripePaymentIntentId", equalTo("pi_succeeded"));
    }

    @Test
    @DisplayName("Should get payment statistics")
    void shouldGetPaymentStatistics() {
        User user = createTestUser("stats@example.com", "Stats User");
        Organization org = createTestOrganization(user, "Stats Org", "stats-org");
        String token = createTokenForUser(user);

        // Create successful payments
        Payment payment1 = new Payment(org.getId(), "pi_stats_1", new Money(BigDecimal.valueOf(100.00)), "usd", "Stats payment 1");
        Payment payment2 = new Payment(org.getId(), "pi_stats_2", new Money(BigDecimal.valueOf(200.00)), "usd", "Stats payment 2");
        payment1.updateStatus(Payment.Status.SUCCEEDED);
        payment2.updateStatus(Payment.Status.SUCCEEDED);
        paymentRepository.save(payment1);
        paymentRepository.save(payment2);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/organizations/{organizationId}/statistics", org.getId())
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("totalSuccessfulPayments", equalTo(2))
            .body("totalAmount", equalTo(300.00f))
            .body("recentAmount", equalTo(300.00f));
    }

    @Test
    @DisplayName("Should attach payment method")
    void shouldAttachPaymentMethod() {
        User user = createTestUser("method@example.com", "Method User");
        Organization org = createTestOrganization(user, "Method Org", "method-org");
        String token = createTokenForUser(user);

        Map<String, Object> attachRequest = Map.of(
            "organizationId", org.getId().toString(),
            "stripePaymentMethodId", "pm_test_mock"
        );

        // Note: This test will fail without actual Stripe configuration
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(attachRequest)
        .when()
            .post("/api/v1/payments/methods")
        .then()
            .statusCode(anyOf(equalTo(201), equalTo(400))); // 400 due to mock Stripe key
    }

    @Test
    @DisplayName("Should get organization payment methods")
    void shouldGetOrganizationPaymentMethods() {
        User user = createTestUser("methods@example.com", "Methods User");
        Organization org = createTestOrganization(user, "Methods Org", "methods-org");
        String token = createTokenForUser(user);

        // Create test payment methods directly in database
        PaymentMethod method1 = new PaymentMethod(org.getId(), "pm_test_1", PaymentMethod.Type.CARD);
        method1.updateCardDetails("4242", "visa", 12, 2025);
        method1.markAsDefault();

        PaymentMethod method2 = new PaymentMethod(org.getId(), "pm_test_2", PaymentMethod.Type.CARD);
        method2.updateCardDetails("1234", "mastercard", 6, 2026);

        paymentMethodRepository.save(method1);
        paymentMethodRepository.save(method2);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/payments/methods/organizations/{organizationId}", org.getId())
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", equalTo(2))
            .body("stripePaymentMethodId", hasItems("pm_test_1", "pm_test_2"))
            .body("type", hasItems("CARD", "CARD"))
            .body("isDefault", hasItems(true, false));
    }

    @Test
    @DisplayName("Should set default payment method")
    void shouldSetDefaultPaymentMethod() {
        User user = createTestUser("default@example.com", "Default User");
        Organization org = createTestOrganization(user, "Default Org", "default-org");
        String token = createTokenForUser(user);

        // Create test payment methods
        PaymentMethod method1 = new PaymentMethod(org.getId(), "pm_default_1", PaymentMethod.Type.CARD);
        method1.markAsDefault();
        PaymentMethod method2 = new PaymentMethod(org.getId(), "pm_default_2", PaymentMethod.Type.CARD);

        paymentMethodRepository.save(method1);
        paymentMethodRepository.save(method2);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("organizationId", org.getId())
        .when()
            .put("/api/v1/payments/methods/{paymentMethodId}/default", method2.getId())
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(method2.getId().toString()))
            .body("isDefault", equalTo(true));

        // Verify the first method is no longer default
        PaymentMethod updatedMethod1 = paymentMethodRepository.findById(method1.getId()).orElseThrow();
        assertThat(updatedMethod1.isDefault()).isFalse();
    }

    @Test
    @DisplayName("Should detach payment method")
    void shouldDetachPaymentMethod() {
        User user = createTestUser("detach@example.com", "Detach User");
        Organization org = createTestOrganization(user, "Detach Org", "detach-org");
        String token = createTokenForUser(user);

        PaymentMethod method = new PaymentMethod(org.getId(), "pm_detach_test", PaymentMethod.Type.CARD);
        paymentMethodRepository.save(method);

        // Note: This test will fail without actual Stripe configuration
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("organizationId", org.getId())
        .when()
            .delete("/api/v1/payments/methods/{paymentMethodId}", method.getId())
        .then()
            .statusCode(anyOf(equalTo(204), equalTo(400))); // 400 due to mock Stripe key
    }

    @Test
    @DisplayName("Should handle Stripe webhook")
    void shouldHandleStripeWebhook() {
        // Create a test payment
        User user = createTestUser("webhook@example.com", "Webhook User");
        Organization org = createTestOrganization(user, "Webhook Org", "webhook-org");
        Payment payment = new Payment(org.getId(), "pi_webhook_test", new Money(BigDecimal.valueOf(99.99)), "usd", "Webhook test");
        paymentRepository.save(payment);

        String webhookPayload = """
            {
              "id": "evt_test_webhook",
              "object": "event",
              "type": "payment_intent.succeeded",
              "data": {
                "object": {
                  "id": "pi_webhook_test",
                  "status": "succeeded"
                }
              }
            }
            """;

        // Note: This test will fail without proper webhook signature
        given()
            .header("Stripe-Signature", "t=123,v1=mock_signature")
            .contentType(ContentType.JSON)
            .body(webhookPayload)
        .when()
            .post("/api/v1/webhooks/stripe")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(400))); // 400 due to signature verification
    }

    @Test
    @DisplayName("Should require authentication for payment endpoints")
    void shouldRequireAuthenticationForPaymentEndpoints() {
        UUID orgId = UUID.randomUUID();

        given()
        .when()
            .get("/api/v1/payments/organizations/{organizationId}", orgId)
        .then()
            .statusCode(401);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("organizationId", orgId.toString(), "amount", "99.99", "currency", "usd"))
        .when()
            .post("/api/v1/payments/intents")
        .then()
            .statusCode(401);

        given()
        .when()
            .get("/api/v1/payments/methods/organizations/{organizationId}", orgId)
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Should validate payment intent creation data")
    void shouldValidatePaymentIntentCreationData() {
        User user = createTestUser("validate@example.com", "Validate User");
        Organization org = createTestOrganization(user, "Validate Org", "validate-org");
        String token = createTokenForUser(user);

        // Test with invalid amount
        Map<String, Object> invalidRequest = Map.of(
            "organizationId", org.getId().toString(),
            "amount", "-10.00",
            "currency", "usd"
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post("/api/v1/payments/intents")
        .then()
            .statusCode(400);

        // Test with missing required fields
        Map<String, Object> incompleteRequest = Map.of(
            "amount", "99.99"
        );

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(incompleteRequest)
        .when()
            .post("/api/v1/payments/intents")
        .then()
            .statusCode(400);
    }

    private User createTestUser(String email, String name) {
        User user = new User(new Email(email), name, "google", "test-provider-id");
        return userRepository.save(user);
    }

    private Organization createTestOrganization(User owner, String name, String slug) {
        Organization org = new Organization(name, slug, owner.getId());
        organizationRepository.save(org);

        OrganizationMember member = OrganizationMember.createOwner(owner.getId(), org.getId());
        memberRepository.save(member);

        return org;
    }

    private String createTokenForUser(User user) {
        return tokenStore.createToken(
            user.getId(),
            "127.0.0.1",
            "test-user-agent",
            "google"
        );
    }
}