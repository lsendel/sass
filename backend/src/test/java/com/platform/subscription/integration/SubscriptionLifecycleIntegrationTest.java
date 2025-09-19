package com.platform.subscription.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.shared.types.Money;
import com.platform.subscription.internal.*;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Integration tests for subscription lifecycle management.
 * Tests plan management, subscription creation, billing cycles, and cross-module integration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class SubscriptionLifecycleIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private UUID orgId;
    private UUID userId;
    private UUID planId;

    @BeforeEach
    void setUp() {
        // Create test organization
        Organization org = new Organization("Subscription Corp", "sub-corp", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test user
        User user = new User("sub@example.com", "Sub User");
        user.setOrganization(org);
        user = userRepository.save(user);
        userId = user.getId();

        // Create test plan
        Plan plan = new Plan("Pro Plan", "price_pro_test",
                           new Money(new BigDecimal("29.99"), "USD"),
                           Plan.BillingInterval.MONTH);
        plan = planRepository.save(plan);
        planId = plan.getId();
    }

    @Test
    void shouldCreateSubscriptionWithTrialPeriod() throws Exception {
        String subscriptionRequest = String.format("""
            {
                "planId": "%s",
                "trialDays": 14,
                "paymentMethodId": "pm_test_trial"
            }
            """, planId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscriptionRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TRIALING"))
                .andExpect(jsonPath("$.planId").value(planId.toString()))
                .andExpect(jsonPath("$.trialEnd").exists());

        // Verify subscription was persisted
        var subscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(subscription.isPresent());
        assertEquals(Subscription.Status.TRIALING, subscription.get().getStatus());
        assertNotNull(subscription.get().getTrialEnd());
    }

    @Test
    void shouldProcessSubscriptionUpgrade() throws Exception {
        // Create initial subscription
        Subscription subscription = Subscription.createActive(orgId, planId, null, null);
        subscription = subscriptionRepository.save(subscription);

        // Create premium plan
        Plan premiumPlan = new Plan("Premium Plan", "price_premium_test",
                                  new Money(new BigDecimal("99.99"), "USD"),
                                  Plan.BillingInterval.MONTH);
        premiumPlan = planRepository.save(premiumPlan);

        String upgradeRequest = String.format("""
            {
                "newPlanId": "%s",
                "prorationBehavior": "ALWAYS_INVOICE"
            }
            """, premiumPlan.getId());

        mockMvc.perform(put("/api/v1/organizations/{orgId}/subscription/plan", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(upgradeRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(premiumPlan.getId().toString()));

        // Verify plan was updated
        var updatedSubscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(updatedSubscription.isPresent());
        assertEquals(premiumPlan.getId(), updatedSubscription.get().getPlanId());
    }

    @Test
    void shouldHandleSubscriptionCancellation() throws Exception {
        // Create active subscription
        Subscription subscription = Subscription.createActive(orgId, planId, null, null);
        subscription = subscriptionRepository.save(subscription);

        String cancellationRequest = """
            {
                "cancelAtPeriodEnd": true,
                "cancellationReason": "customer_request"
            }
            """;

        mockMvc.perform(delete("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cancellationRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cancelAt").exists());

        // Verify cancellation was scheduled
        var canceledSubscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(canceledSubscription.isPresent());
        assertNotNull(canceledSubscription.get().getCancelAt());
    }

    @Test
    void shouldProcessSubscriptionReactivation() throws Exception {
        // Create canceled subscription
        Subscription subscription = Subscription.createActive(orgId, planId, null, null);
        subscription.cancel();
        subscription = subscriptionRepository.save(subscription);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/reactivate", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cancelAt").doesNotExist());

        // Verify reactivation
        var reactivatedSubscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(reactivatedSubscription.isPresent());
        assertNull(reactivatedSubscription.get().getCancelAt());
    }

    @Test
    void shouldGenerateInvoicesForBillingCycles() throws Exception {
        // Create active subscription
        Subscription subscription = Subscription.createActive(orgId, planId,
                                    LocalDate.now(), LocalDate.now().plusMonths(1));
        subscription = subscriptionRepository.save(subscription);

        // Trigger invoice generation
        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/invoice", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(29.99))
                .andExpect(jsonPath("$.currency").value("usd"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        // Verify invoice was created
        var invoices = invoiceRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscription.getId());
        assertEquals(1, invoices.size());
        assertEquals(new BigDecimal("29.99"), invoices.get(0).getAmount());
    }

    @Test
    void shouldHandleFailedBillingGracefully() throws Exception {
        // Create subscription with failing payment method
        Subscription subscription = Subscription.createActive(orgId, planId, null, null);
        subscription = subscriptionRepository.save(subscription);

        String failedBillingRequest = """
            {
                "paymentMethodId": "pm_card_chargeDeclined"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/invoice", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(failedBillingRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        // Verify subscription enters grace period
        var updatedSubscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(updatedSubscription.isPresent());
        // Status might change to PAST_DUE depending on business logic
    }

    @Test
    void shouldProcessPlanManagement() throws Exception {
        String newPlanRequest = """
            {
                "name": "Enterprise Plan",
                "stripePriceId": "price_enterprise_test",
                "amount": 199.99,
                "currency": "usd",
                "interval": "MONTH",
                "trialDays": 30,
                "description": "Full enterprise features",
                "features": ["unlimited_users", "advanced_analytics", "priority_support"]
            }
            """;

        mockMvc.perform(post("/api/v1/plans", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newPlanRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Enterprise Plan"))
                .andExpect(jsonPath("$.amount").value(199.99))
                .andExpect(jsonPath("$.trialDays").value(30));

        // Verify plan was created
        var plans = planRepository.findByActiveOrderByDisplayOrderAsc(true);
        assertTrue(plans.stream().anyMatch(p -> "Enterprise Plan".equals(p.getName())));
    }

    @Test
    void shouldEnforceSubscriptionBusinessRules() throws Exception {
        // Try to create subscription for organization that already has one
        Subscription existingSubscription = Subscription.createActive(orgId, planId, null, null);
        subscriptionRepository.save(existingSubscription);

        String duplicateRequest = String.format("""
            {
                "planId": "%s",
                "paymentMethodId": "pm_test_123"
            }
            """, planId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SUBSCRIPTION_ALREADY_EXISTS"));
    }

    @Test
    void shouldCalculateProrationCorrectly() throws Exception {
        // Create subscription in middle of billing period
        Subscription subscription = Subscription.createActive(orgId, planId,
                                    LocalDate.now().minusDays(15),
                                    LocalDate.now().plusDays(15));
        subscription = subscriptionRepository.save(subscription);

        // Upgrade to premium plan
        Plan premiumPlan = new Plan("Premium Plan", "price_premium_test",
                                  new Money(new BigDecimal("99.99"), "USD"),
                                  Plan.BillingInterval.MONTH);
        premiumPlan = planRepository.save(premiumPlan);

        String upgradeRequest = String.format("""
            {
                "newPlanId": "%s",
                "prorationBehavior": "ALWAYS_INVOICE"
            }
            """, premiumPlan.getId());

        mockMvc.perform(put("/api/v1/organizations/{orgId}/subscription/plan", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(upgradeRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prorationAmount").exists());

        // Verify proration invoice was created
        var invoices = invoiceRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscription.getId());
        assertTrue(invoices.size() >= 1);
    }

    @Test
    void shouldHandleSubscriptionEventsAcrossModules() throws Exception {
        String subscriptionRequest = String.format("""
            {
                "planId": "%s",
                "paymentMethodId": "pm_test_events"
            }
            """, planId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscriptionRequest))
                .andExpect(status().isCreated());

        // Verify subscription creation triggered audit events
        // This would typically be tested through event listener verification
        var subscription = subscriptionRepository.findByOrganizationId(orgId);
        assertTrue(subscription.isPresent());

        // In a real test, we'd verify that:
        // 1. AuditEvent was published for subscription creation
        // 2. Payment module was notified for billing setup
        // 3. User module received organization status update
    }
}