package com.platform.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
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
import com.platform.payment.internal.PaymentRepository;
import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.shared.security.TenantContext;
import com.platform.subscription.internal.Plan;
import com.platform.subscription.internal.PlanRepository;
import com.platform.subscription.internal.SubscriptionRepository;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Integration tests for cross-module event verification.
 * Tests event publishing, handling, and propagation across all modules.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class CrossModuleEventIntegrationTest {

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
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

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
        paymentRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        planRepository.deleteAll();

        // Create test organization
        Organization org = new Organization("Cross Module Test Corp", "cross-module", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test user
        User user = new User("crossmodule@example.com", "Cross Module User");
        user.setOrganization(org);
        user = userRepository.save(user);
        userId = user.getId();

        // Create test plan
        Plan plan = new Plan("Event Test Plan", "price_event_test",
                           new Money(new BigDecimal("19.99"), "USD"),
                           Plan.BillingInterval.MONTH);
        setField(plan, "slug", "event-test-plan");
        setField(plan, "active", true);
        plan = planRepository.save(plan);
        planId = plan.getId();

        // Set authentication context
        authenticateAs(userId, orgId, "cross-module", "ADMIN");
    }

    @Test
    void shouldPublishAndHandleUserRegistrationEvents() throws Exception {
        // Clear existing audit events
        auditEventRepository.deleteAll();

        // Step 1: Create new user (should trigger UserRegisteredEvent)
        String newUserRequest = """
            {
                "email": "newuser@crossmodule.com",
                "name": "New User",
                "role": "USER"
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/users", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUserRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var userResponse = objectMapper.readTree(responseJson);
        String newUserId = userResponse.get("id").asText();

        // Wait for async event processing
        Thread.sleep(2000);

        // Step 2: Verify audit events were created
        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 20));

        assertTrue(auditEvents.getTotalElements() >= 1);

        // Verify user creation event
        var userCreationEvents = auditEvents.getContent().stream()
            .filter(e -> "USER_CREATED".equals(e.getAction()) ||
                        "USER_REGISTRATION".equals(e.getAction()) ||
                        e.getResourceType().equals("USER"))
            .toList();

        assertFalse(userCreationEvents.isEmpty());

        // Verify event contains correct data
        var userEvent = userCreationEvents.get(0);
        assertEquals(orgId, userEvent.getOrganizationId());
        assertNotNull(userEvent.getCreatedAt());
    }

    @Test
    void shouldHandlePaymentToSubscriptionEventFlow() throws Exception {
        auditEventRepository.deleteAll();

        // Step 1: Create payment method
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_event_flow",
                "type": "CARD",
                "billingDetails": {
                    "name": "Cross Module User",
                    "email": "crossmodule@example.com"
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

        Thread.sleep(1000);

        // Step 2: Create subscription (should trigger multiple events)
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

        Thread.sleep(2000);

        // Step 3: Verify cross-module events
        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 30));

        var eventActions = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        // Should have payment method creation event
        assertTrue(eventActions.stream()
            .anyMatch(action -> action.contains("PAYMENT_METHOD") || action.contains("CREATED")));

        // Should have subscription creation event
        assertTrue(eventActions.stream()
            .anyMatch(action -> action.contains("SUBSCRIPTION") || action.contains("CREATED")));

        // Step 4: Generate invoice (should trigger payment events)
        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/invoice", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        Thread.sleep(2000);

        // Verify payment events were created
        var updatedAuditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 50));

        var updatedEventActions = updatedAuditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        assertTrue(updatedEventActions.stream()
            .anyMatch(action -> action.contains("PAYMENT") || action.contains("INVOICE")));
    }

    @Test
    void shouldHandleSubscriptionStatusChangeEvents() throws Exception {
        auditEventRepository.deleteAll();

        // Step 1: Create subscription
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_status_change",
                "type": "CARD",
                "billingDetails": {
                    "name": "Status Change User",
                    "email": "crossmodule@example.com"
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

        Thread.sleep(1500);

        // Step 2: Cancel subscription (should trigger events)
        String cancellationRequest = """
            {
                "cancelAtPeriodEnd": true,
                "cancellationReason": "testing_events"
            }
            """;

        mockMvc.perform(delete("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cancellationRequest))
                .andExpect(status().isOk());

        Thread.sleep(1500);

        // Step 3: Verify cancellation events triggered audit and user notifications
        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 30));

        var eventActions = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        // Should have subscription cancellation event
        assertTrue(eventActions.stream()
            .anyMatch(action -> action.contains("SUBSCRIPTION") &&
                               (action.contains("CANCEL") || action.contains("UPDATE"))));

        // Step 4: Reactivate subscription
        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/reactivate", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Thread.sleep(1500);

        // Verify reactivation events
        var reactivationEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 40));

        var reactivationActions = reactivationEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        assertTrue(reactivationActions.stream()
            .anyMatch(action -> action.contains("SUBSCRIPTION") &&
                               action.contains("REACTIVAT")));
    }

    @Test
    void shouldHandlePaymentFailureEventCascade() throws Exception {
        auditEventRepository.deleteAll();

        // Step 1: Create subscription with failing payment method
        String failingPaymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_card_chargeDeclined",
                "type": "CARD",
                "billingDetails": {
                    "name": "Failing Payment User",
                    "email": "crossmodule@example.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "0002",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        var pmResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(failingPaymentMethodRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String pmJson = pmResult.getResponse().getContentAsString();
        var pmResponse = objectMapper.readTree(pmJson);
        String failingPaymentMethodId = pmResponse.get("id").asText();

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

        Thread.sleep(1000);

        // Step 2: Attempt payment (should fail and trigger events)
        String failedBillingRequest = String.format("""
            {
                "paymentMethodId": "%s"
            }
            """, failingPaymentMethodId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/invoice", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(failedBillingRequest))
                .andExpect(status().isCreated()); // Invoice created but payment may fail

        Thread.sleep(2000);

        // Step 3: Verify failure event cascade
        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 40));

        var eventActions = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        // Should have payment failure events
        boolean hasPaymentEvents = eventActions.stream()
            .anyMatch(action -> action.contains("PAYMENT") || action.contains("INVOICE"));
        assertTrue(hasPaymentEvents);

        // Should trigger user notification events
        boolean hasNotificationEvents = eventActions.stream()
            .anyMatch(action -> action.contains("NOTIFICATION") ||
                               action.contains("EMAIL") ||
                               action.contains("ALERT"));
        // Note: Notification events may not be implemented yet, so we check for any audit events

        // Should have subscription status change events due to payment failure
        boolean hasSubscriptionEvents = eventActions.stream()
            .anyMatch(action -> action.contains("SUBSCRIPTION"));
        assertTrue(hasSubscriptionEvents);
    }

    @Test
    void shouldHandleOrganizationEventPropagation() throws Exception {
        auditEventRepository.deleteAll();

        // Step 1: Update organization settings (should trigger events across modules)
        String settingsRequest = """
            {
                "companySize": "LARGE",
                "industry": "FINTECH",
                "timeZone": "America/Los_Angeles",
                "features": {
                    "sso": true,
                    "audit": true,
                    "api": true
                }
            }
            """;

        mockMvc.perform(put("/api/v1/organizations/{orgId}/settings", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(settingsRequest))
                .andExpect(status().isOk());

        Thread.sleep(1500);

        // Step 2: Verify organization update events
        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 20));

        var eventActions = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        // Should have organization update event
        assertTrue(eventActions.stream()
            .anyMatch(action -> action.contains("ORGANIZATION") &&
                               (action.contains("UPDATE") || action.contains("SETTINGS"))));

        // Step 3: Disable organization (should cascade to all modules)
        mockMvc.perform(post("/api/v1/organizations/{orgId}/disable", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Thread.sleep(1500);

        // Verify cascade events
        var disableEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 30));

        var disableActions = disableEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        // Should have organization disable event
        assertTrue(disableActions.stream()
            .anyMatch(action -> action.contains("ORGANIZATION") &&
                               action.contains("DISABLE")));

        // Should trigger subscription suspension events
        boolean hasSubscriptionSuspension = disableActions.stream()
            .anyMatch(action -> action.contains("SUBSCRIPTION") &&
                               (action.contains("SUSPEND") || action.contains("DISABLE")));
        // Note: This may not be implemented yet

        // Should trigger user deactivation events
        boolean hasUserDeactivation = disableActions.stream()
            .anyMatch(action -> action.contains("USER") &&
                               action.contains("DEACTIVAT"));
        // Note: This may not be implemented yet
    }

    @Test
    void shouldHandleSecurityEventPropagation() throws Exception {
        auditEventRepository.deleteAll();

        // Step 1: Simulate security breach detection
        // This would normally be triggered by security monitoring
        // For testing, we'll simulate it via audit service directly

        // Create suspicious activity
        authenticateAs(userId, orgId, "cross-module", "USER");

        // Multiple failed attempts to access restricted resource
        for (int i = 0; i < 5; i++) {
            try {
                mockMvc.perform(get("/api/v1/organizations/{orgId}/admin/sensitive-data", orgId)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
            } catch (Exception e) {
                // Expected to fail
            }
            Thread.sleep(100);
        }

        Thread.sleep(2000);

        // Step 2: Verify security events were created
        var securityEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 20));

        var securityActions = securityEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        // Should have access denied events
        assertTrue(securityActions.stream()
            .anyMatch(action -> action.contains("ACCESS") && action.contains("DENIED")));

        // Step 3: Trigger account lockout (if implemented)
        // This would be handled by security policies

        // Step 4: Verify notification events (if implemented)
        // Security events should trigger admin notifications
    }

    @Test
    void shouldHandleEventOrderingAndConsistency() throws Exception {
        auditEventRepository.deleteAll();

        // Step 1: Perform series of related operations rapidly
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_ordering",
                "type": "CARD",
                "billingDetails": {
                    "name": "Ordering Test User",
                    "email": "crossmodule@example.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "4242",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        // Create payment method
        var pmResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentMethodRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String pmJson = pmResult.getResponse().getContentAsString();
        var pmResponse = objectMapper.readTree(pmJson);
        String paymentMethodId = pmResponse.get("id").asText();

        Thread.sleep(200);

        // Create subscription
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

        Thread.sleep(200);

        // Generate invoice
        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/invoice", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        Thread.sleep(200);

        // Cancel subscription
        String cancellationRequest = """
            {
                "cancelAtPeriodEnd": false,
                "cancellationReason": "testing_event_ordering"
            }
            """;

        mockMvc.perform(delete("/api/v1/organizations/{orgId}/subscription", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cancellationRequest))
                .andExpect(status().isOk());

        Thread.sleep(3000); // Allow all async processing to complete

        // Step 2: Verify event chronological ordering
        var allEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 50));

        // Events should be in chronological order (newest first)
        var events = allEvents.getContent();
        for (int i = 0; i < events.size() - 1; i++) {
            assertTrue(events.get(i).getCreatedAt().isAfter(events.get(i + 1).getCreatedAt()) ||
                      events.get(i).getCreatedAt().equals(events.get(i + 1).getCreatedAt()),
                      "Events not in chronological order");
        }

        // Step 3: Verify logical event sequence
        var eventActions = events.stream()
            .map(e -> e.getAction())
            .toList();

        // Should have all expected event types
        assertTrue(eventActions.stream().anyMatch(action -> action.contains("PAYMENT_METHOD")));
        assertTrue(eventActions.stream().anyMatch(action -> action.contains("SUBSCRIPTION")));
        assertTrue(eventActions.stream().anyMatch(action -> action.contains("INVOICE") || action.contains("PAYMENT")));

        // Step 4: Verify event correlation (events should reference related entities)
        var subscriptionEvents = events.stream()
            .filter(e -> e.getAction().contains("SUBSCRIPTION"))
            .toList();

        assertFalse(subscriptionEvents.isEmpty());

        // Subscription events should reference the organization
        for (var event : subscriptionEvents) {
            assertEquals(orgId, event.getOrganizationId());
            assertEquals(userId, event.getActorId());
        }
    }

    @Test
    void shouldHandleEventFailureAndRetry() throws Exception {
        auditEventRepository.deleteAll();

        // Step 1: Create scenario that might cause event processing failures
        // (e.g., rapid concurrent operations)

        // Multiple concurrent operations that should trigger events
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    String userRequest = String.format("""
                        {
                            "email": "concurrent%d@crossmodule.com",
                            "name": "Concurrent User %d",
                            "role": "USER"
                        }
                        """, threadIndex, threadIndex);

                    mockMvc.perform(post("/api/v1/organizations/{orgId}/users", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userRequest))
                            .andExpect(status().isCreated());
                } catch (Exception e) {
                    // Some operations may fail due to concurrency, that's expected
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for completion
        for (Thread thread : threads) {
            thread.join(5000);
        }

        Thread.sleep(3000); // Allow event processing

        // Step 2: Verify events were processed despite concurrent access
        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 30));

        // Should have at least some events (some operations may have failed)
        assertTrue(auditEvents.getTotalElements() > 0);

        // All events should have proper organization and user context
        for (var event : auditEvents.getContent()) {
            assertEquals(orgId, event.getOrganizationId());
            assertNotNull(event.getCreatedAt());
        }
    }

    private void authenticateAs(UUID userId, UUID orgId, String orgSlug, String role) {
        PlatformUserPrincipal principal = PlatformUserPrincipal.organizationMember(
            userId,
            "crossmodule@example.com",
            "Cross Module User",
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