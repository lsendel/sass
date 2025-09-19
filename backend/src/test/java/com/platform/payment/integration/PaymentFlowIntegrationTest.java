package com.platform.payment.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
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
import com.platform.payment.internal.PaymentRepository;
import com.platform.payment.internal.PaymentMethodRepository;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Integration tests for payment processing workflows.
 * Tests real database interactions, Stripe integration, and cross-module events.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PaymentFlowIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private UUID orgId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        // Create test organization
        Organization org = new Organization("Test Corp", "test-corp", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test user
        User user = new User("test@example.com", "Test User");
        user.setOrganization(org);
        user = userRepository.save(user);
        userId = user.getId();
    }

    @Test
    void shouldCreatePaymentIntentSuccessfully() throws Exception {
        String paymentRequest = """
            {
                "amount": 100.00,
                "currency": "usd",
                "description": "Test payment",
                "paymentMethodId": "pm_test_123"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("usd"))
                .andExpect(jsonPath("$.status").exists());

        // Verify payment was persisted
        var payments = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        assertEquals(1, payments.size());
        assertEquals(new BigDecimal("100.00"), payments.get(0).getAmount());
    }

    @Test
    void shouldProcessPaymentMethodManagement() throws Exception {
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_visa",
                "type": "CARD",
                "billingDetails": {
                    "name": "John Doe",
                    "email": "john@example.com"
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
        mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentMethodRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stripePaymentMethodId").value("pm_test_visa"))
                .andExpect(jsonPath("$.type").value("CARD"))
                .andExpect(jsonPath("$.isDefault").value(true));

        // Verify payment method was persisted
        var paymentMethods = paymentMethodRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(orgId);
        assertEquals(1, paymentMethods.size());
        assertTrue(paymentMethods.get(0).isDefault());
    }

    @Test
    void shouldHandlePaymentFailuresGracefully() throws Exception {
        String invalidPaymentRequest = """
            {
                "amount": -50.00,
                "currency": "invalid",
                "description": "Invalid payment"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPaymentRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // Verify no payment was created
        var payments = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        assertEquals(0, payments.size());
    }

    @Test
    void shouldProcessRefundsCorrectly() throws Exception {
        // First create a successful payment
        String paymentRequest = """
            {
                "amount": 200.00,
                "currency": "usd",
                "description": "Refund test payment",
                "paymentMethodId": "pm_test_123"
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var payment = objectMapper.readTree(responseJson);
        String paymentId = payment.get("id").asText();

        // Request refund
        String refundRequest = """
            {
                "amount": 100.00,
                "reason": "customer_request"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/payments/{paymentId}/refunds", orgId, paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(refundRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.reason").value("customer_request"));
    }

    @Test
    void shouldValidateStripeWebhookSignatures() throws Exception {
        String webhookPayload = """
            {
                "id": "evt_test_webhook",
                "object": "event",
                "type": "payment_intent.succeeded",
                "data": {
                    "object": {
                        "id": "pi_test_123",
                        "amount": 5000,
                        "currency": "usd",
                        "status": "succeeded"
                    }
                }
            }
            """;

        // Test with invalid signature
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "invalid_signature")
                .content(webhookPayload))
                .andExpect(status().isBadRequest());

        // Test with valid signature (mocked in test environment)
        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "t=1234567890,v1=valid_test_signature")
                .content(webhookPayload))
                .andExpect(status().isOk());
    }

    @Test
    void shouldEnforceIdempotencyForPayments() throws Exception {
        String paymentRequest = """
            {
                "amount": 150.00,
                "currency": "usd",
                "description": "Idempotency test",
                "paymentMethodId": "pm_test_123"
            }
            """;

        String idempotencyKey = "test-idempotency-" + UUID.randomUUID();

        // First request
        var result1 = mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(paymentRequest))
                .andExpect(status().isCreated())
                .andReturn();

        // Second request with same idempotency key
        var result2 = mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(paymentRequest))
                .andExpect(status().isOk()) // Should return existing payment
                .andReturn();

        // Responses should be identical
        assertEquals(result1.getResponse().getContentAsString(),
                    result2.getResponse().getContentAsString());

        // Only one payment should exist
        var payments = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        assertEquals(1, payments.size());
    }

    @Test
    void shouldHandleMultiCurrencyPayments() throws Exception {
        String[] currencies = {"usd", "eur", "gbp", "cad"};

        for (String currency : currencies) {
            String paymentRequest = String.format("""
                {
                    "amount": 100.00,
                    "currency": "%s",
                    "description": "Multi-currency test - %s",
                    "paymentMethodId": "pm_test_123"
                }
                """, currency, currency.toUpperCase());

            mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currency").value(currency));
        }

        // Verify all currencies were processed
        var payments = paymentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        assertEquals(currencies.length, payments.size());
    }
}