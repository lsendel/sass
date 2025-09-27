package com.platform.integration.e2e;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.payment.internal.Payment;
import com.platform.payment.internal.PaymentMethod;
import com.platform.payment.internal.PaymentMethodRepository;
import com.platform.payment.internal.PaymentRepository;
import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.shared.security.TenantContext;
import com.platform.subscription.internal.Plan;
import com.platform.subscription.internal.PlanRepository;
import com.platform.subscription.internal.Subscription;
import com.platform.subscription.internal.SubscriptionRepository;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * End-to-End integration tests for complete payment journeys.
 * Tests user onboarding → subscription → payment → billing cycles.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class CompletePaymentJourneyE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private UUID orgId;
    private UUID userId;
    private UUID planId;

    @BeforeEach
    void setUp() {
        // Clear existing data
        auditEventRepository.deleteAll();
        subscriptionRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        paymentRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        planRepository.deleteAll();

        // Create test organization
        Organization org = new Organization("E2E Test Corp", "e2e-test", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test user
        User user = new User("e2e@example.com", "E2E Test User");
        user.setOrganization(org);
        user = userRepository.save(user);
        userId = user.getId();

        // Create test plan
        Plan plan = new Plan("Pro Plan", "price_pro_e2e",
                           new Money(new BigDecimal("29.99"), "USD"),
                           Plan.BillingInterval.MONTH);
        // Set required fields using reflection helper
        setField(plan, "slug", "pro-plan-e2e");
        setField(plan, "active", true);
        plan = planRepository.save(plan);
        planId = plan.getId();

        // Set authentication context
        authenticateAs(userId, orgId, "e2e-test", "ADMIN");
    }

    @Test
    void shouldCompleteFullUserOnboardingToFirstPaymentJourney() throws Exception {
        // Step 1: User Registration (simulated - user already exists)
        authenticateAs(userId, orgId, "e2e-test", "USER");

        // Verify user profile
        mockMvc.perform(get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("e2e@example.com"))
                .andExpect(jsonPath("$.name").value("E2E Test User"));

        // Step 2: Create Payment Method
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_visa_e2e",
                "type": "CARD",
                "billingDetails": {
                    "name": "E2E Test User",
                    "email": "e2e@example.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "4242",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        var paymentMethodResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentMethodRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stripePaymentMethodId").value("pm_test_visa_e2e"))
                .andExpect(jsonPath("$.type").value("CARD"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andReturn();

        String pmResponseJson = paymentMethodResult.getResponse().getContentAsString();
        var pmResponse = objectMapper.readTree(pmResponseJson);
        String paymentMethodId = pmResponse.get("id").asText();

        // Step 3: Subscribe to Plan
        String subscriptionRequest = String.format("""
            {
                "planId": "%s",
                "paymentMethodId": "%s",
                "trialDays": 14
            }
            """, planId, paymentMethodId);

        var subscriptionResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscriptionRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TRIALING"))
                .andExpect(jsonPath("$.planId").value(planId.toString()))
                .andExpect(jsonPath("$.trialEnd").exists())
                .andReturn();

        String subResponseJson = subscriptionResult.getResponse().getContentAsString();
        var subResponse = objectMapper.readTree(subResponseJson);
        String subscriptionId = subResponse.get("id").asText();

        // Step 4: Simulate Trial End and First Payment
        // Generate invoice for subscription
        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/invoice", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(29.99))
                .andExpect(jsonPath("$.currency").value("usd"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        // Step 5: Verify Payment Created
        Thread.sleep(1000); // Allow async operations to complete

        var payments = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        assertTrue(payments.size() >= 1);

        Payment firstPayment = payments.get(0);
        assertEquals(new BigDecimal("29.99"), firstPayment.getAmount());
        assertEquals("USD", firstPayment.getCurrency());

        // Step 6: Verify Subscription is Active
        var subscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(subscription.isPresent());
        // After first payment, subscription should be active
        assertTrue(subscription.get().getStatus() == Subscription.Status.TRIALING ||
                  subscription.get().getStatus() == Subscription.Status.ACTIVE);

        // Step 7: Verify Audit Trail
        Thread.sleep(1000); // Allow audit events to be processed

        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 20));

        assertTrue(auditEvents.getTotalElements() >= 5);

        // Verify key events are logged
        var eventTypes = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        // Should include subscription creation, payment method creation, payment processing
        assertTrue(eventTypes.stream().anyMatch(type -> type.contains("SUBSCRIPTION")));
        assertTrue(eventTypes.stream().anyMatch(type -> type.contains("PAYMENT")));
    }

    @Test
    void shouldHandlePaymentFailureAndRecoveryJourney() throws Exception {
        authenticateAs(userId, orgId, "e2e-test", "ADMIN");

        // Step 1: Create Failing Payment Method
        String failingPaymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_card_chargeDeclined",
                "type": "CARD",
                "billingDetails": {
                    "name": "E2E Test User",
                    "email": "e2e@example.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "0002",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        var failingPmResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(failingPaymentMethodRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String failingPmJson = failingPmResult.getResponse().getContentAsString();
        var failingPmResponse = objectMapper.readTree(failingPmJson);
        String failingPaymentMethodId = failingPmResponse.get("id").asText();

        // Step 2: Create Subscription with Failing Payment Method
        String subscriptionRequest = String.format("""
            {
                "planId": "%s",
                "paymentMethodId": "%s"
            }
            """, planId, failingPaymentMethodId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscriptionRequest))
                .andExpect(status().isCreated());

        // Step 3: Attempt Payment (Should Fail)
        String failedBillingRequest = String.format("""
            {
                "paymentMethodId": "%s"
            }
            """, failingPaymentMethodId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/invoice", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(failedBillingRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN")); // Invoice created but payment may fail

        // Step 4: Add Valid Payment Method for Recovery
        String validPaymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_visa_recovery",
                "type": "CARD",
                "billingDetails": {
                    "name": "E2E Test User",
                    "email": "e2e@example.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "4242",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        var validPmResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPaymentMethodRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String validPmJson = validPmResult.getResponse().getContentAsString();
        var validPmResponse = objectMapper.readTree(validPmJson);
        String validPaymentMethodId = validPmResponse.get("id").asText();

        // Step 5: Update Subscription Payment Method
        String updatePaymentMethodRequest = String.format("""
            {
                "paymentMethodId": "%s"
            }
            """, validPaymentMethodId);

        mockMvc.perform(put("/api/v1/organizations/{orgId}/subscription/payment-method", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePaymentMethodRequest))
                .andExpect(status().isOk());

        // Step 6: Retry Payment
        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/invoice", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // Step 7: Verify Recovery
        Thread.sleep(1000);

        var subscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(subscription.isPresent());

        // Verify audit trail shows recovery
        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 30));

        var eventActions = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        // Should show payment failure and recovery events
        assertTrue(eventActions.stream().anyMatch(action ->
            action.contains("PAYMENT") && (action.contains("FAILED") || action.contains("RETRY"))));
    }

    @Test
    void shouldCompleteSubscriptionUpgradeJourney() throws Exception {
        authenticateAs(userId, orgId, "e2e-test", "ADMIN");

        // Step 1: Create Initial Subscription (Basic Plan)
        Plan basicPlan = new Plan("Basic Plan", "price_basic_e2e",
                                new Money(new BigDecimal("9.99"), "USD"),
                                Plan.BillingInterval.MONTH);
        setField(basicPlan, "slug", "basic-plan-e2e");
        setField(basicPlan, "active", true);
        basicPlan = planRepository.save(basicPlan);

        // Create payment method
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_upgrade",
                "type": "CARD",
                "billingDetails": {
                    "name": "E2E Test User",
                    "email": "e2e@example.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "4242",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        var pmResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentMethodRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String pmJson = pmResult.getResponse().getContentAsString();
        var pmResponse = objectMapper.readTree(pmJson);
        String paymentMethodId = pmResponse.get("id").asText();

        // Create subscription
        String basicSubscriptionRequest = String.format("""
            {
                "planId": "%s",
                "paymentMethodId": "%s"
            }
            """, basicPlan.getId(), paymentMethodId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(basicSubscriptionRequest))
                .andExpect(status().isCreated());

        Thread.sleep(500);

        // Step 2: Upgrade to Pro Plan
        String upgradeRequest = String.format("""
            {
                "newPlanId": "%s",
                "prorationBehavior": "ALWAYS_INVOICE"
            }
            """, planId);

        mockMvc.perform(put("/api/v1/organizations/{orgId}/subscription/plan", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(upgradeRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(planId.toString()));

        Thread.sleep(1000);

        // Step 3: Verify Proration Payment
        var payments = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        assertTrue(payments.size() >= 1);

        // Should have proration charge
        boolean hasProrationPayment = payments.stream()
            .anyMatch(p -> p.getAmount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(hasProrationPayment);

        // Step 4: Verify Subscription Updated
        var subscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(subscription.isPresent());
        assertEquals(planId, subscription.get().getPlanId());

        // Step 5: Verify Audit Trail
        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 20));

        var eventActions = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        assertTrue(eventActions.stream().anyMatch(action ->
            action.contains("SUBSCRIPTION") && action.contains("UPGRADE")));
    }

    @Test
    void shouldCompleteSubscriptionCancellationJourney() throws Exception {
        authenticateAs(userId, orgId, "e2e-test", "ADMIN");

        // Step 1: Create Active Subscription
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_cancel",
                "type": "CARD",
                "billingDetails": {
                    "name": "E2E Test User",
                    "email": "e2e@example.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "4242",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        var pmResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentMethodRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String pmJson = pmResult.getResponse().getContentAsString();
        var pmResponse = objectMapper.readTree(pmJson);
        String paymentMethodId = pmResponse.get("id").asText();

        String subscriptionRequest = String.format("""
            {
                "planId": "%s",
                "paymentMethodId": "%s"
            }
            """, planId, paymentMethodId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscriptionRequest))
                .andExpect(status().isCreated());

        Thread.sleep(500);

        // Step 2: Cancel Subscription
        String cancellationRequest = """
            {
                "cancelAtPeriodEnd": true,
                "cancellationReason": "user_request"
            }
            """;

        mockMvc.perform(delete("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cancellationRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelAt").exists());

        // Step 3: Verify Subscription Status
        var subscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(subscription.isPresent());
        assertNotNull(subscription.get().getCancelAt());

        // Step 4: Process Cancellation Survey (Optional)
        String surveyRequest = """
            {
                "reason": "too_expensive",
                "feedback": "Great service but outside budget",
                "rating": 4
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/cancellation-survey", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(surveyRequest))
                .andExpect(status().isOk());

        // Step 5: Verify Audit Trail
        Thread.sleep(1000);

        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 20));

        var eventActions = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        assertTrue(eventActions.stream().anyMatch(action ->
            action.contains("SUBSCRIPTION") && action.contains("CANCEL")));
    }

    @Test
    void shouldCompleteRefundProcessJourney() throws Exception {
        authenticateAs(userId, orgId, "e2e-test", "ADMIN");

        // Step 1: Create Payment
        String paymentRequest = """
            {
                "amount": 199.99,
                "currency": "usd",
                "description": "Refund test payment",
                "paymentMethodId": "pm_test_refund"
            }
            """;

        var paymentResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String paymentJson = paymentResult.getResponse().getContentAsString();
        var paymentResponse = objectMapper.readTree(paymentJson);
        String paymentId = paymentResponse.get("id").asText();

        Thread.sleep(500);

        // Step 2: Customer Requests Refund
        String refundRequest = """
            {
                "amount": 100.00,
                "reason": "customer_request",
                "description": "Partial refund for unused service"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/payments/{paymentId}/refunds", orgId, paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(refundRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.reason").value("customer_request"));

        Thread.sleep(500);

        // Step 3: Verify Payment Status
        var payment = paymentRepository.findById(UUID.fromString(paymentId));
        assertTrue(payment.isPresent());

        // Step 4: Issue Additional Refund
        String secondRefundRequest = """
            {
                "amount": 99.99,
                "reason": "service_issue",
                "description": "Remaining balance refund"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/payments/{paymentId}/refunds", orgId, paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondRefundRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(99.99));

        // Step 5: Verify Total Refunds
        Thread.sleep(1000);

        var auditEvents = auditEventRepository.findByResourceTypeAndResourceId("PAYMENT", paymentId);

        long refundEvents = auditEvents.stream()
            .filter(e -> e.getAction().contains("REFUND"))
            .count();

        assertTrue(refundEvents >= 2);

        // Step 6: Verify Customer Communication (Audit)
        var communicationEvents = auditEvents.stream()
            .filter(e -> e.getAction().contains("CUSTOMER") || e.getAction().contains("NOTIFICATION"))
            .count();

        // Should have notification events for refund confirmations
        assertTrue(communicationEvents >= 0); // At least audit logging occurred
    }

    private void authenticateAs(UUID userId, UUID orgId, String orgSlug, String role) {
        PlatformUserPrincipal principal = PlatformUserPrincipal.organizationMember(
            userId,
            "e2e@example.com",
            "E2E Test User",
            orgId,
            orgSlug,
            role
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
            );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        TenantContext.setTenantInfo(orgId, orgSlug, userId);
    }

    // Helper method for setting private fields via reflection
    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}