package com.platform.payment.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.payment.api.PaymentDto;
import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.shared.security.TenantGuard;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "platform.stripe.publishable-key=pk_test_fake",
    "platform.stripe.secret-key=sk_test_fake",
    "platform.stripe.webhook-secret=whsec_fake"
})
@Transactional
class PaymentControllerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PaymentControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentManagementService paymentManagementService;

    @MockBean
    private TenantGuard tenantGuard;

    @BeforeEach
    void setUp() {
        log.info("Setting up PaymentControllerIntegrationTest");

        // Mock tenant guard to allow organization access
        when(tenantGuard.canManageOrganization(any(), any())).thenReturn(true);
        when(tenantGuard.canAccessOrganization(any(), any())).thenReturn(true);

        // Set up tenant context for the test user and organization
        UUID testUserId = UUID.fromString("01847b7f-d037-4c51-a4af-0c7688a1ec18");
        UUID testOrgId = UUID.fromString("01847b7f-d037-4c51-a4af-0c7688a1ec17");
        com.platform.shared.security.TenantContext.setTenantInfo(testOrgId, "test-org", testUserId);

        // Mock PaymentManagementService methods to avoid actual Stripe API calls
        try {
            PaymentDto.PaymentIntentResponse mockPaymentIntentResponse = createMockPaymentIntentResponse();
            when(paymentManagementService.createPaymentIntent(any()))
                .thenReturn(mockPaymentIntentResponse);

            // Mock other PaymentManagementService methods used by other tests
            when(paymentManagementService.confirmPaymentIntent(any(), any(), any()))
                .thenReturn(mockPaymentIntentResponse);
            when(paymentManagementService.getOrganizationPayments(any()))
                .thenReturn(java.util.List.of());
            when(paymentManagementService.getOrganizationPaymentMethods(any()))
                .thenReturn(java.util.List.of());
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup PaymentManagementService mocks", e);
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up tenant context after each test
        com.platform.shared.security.TenantContext.clear();
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testCreatePayment_ShouldReturnPaymentIntent() throws Exception {
        log.info("Testing POST /api/v1/payments/intents");

        Map<String, Object> paymentRequest = Map.of(
            "amount", 2500,
            "currency", "usd",
            "organizationId", "01847b7f-d037-4c51-a4af-0c7688a1ec17", // Need organization ID for authorization
            "description", "Test payment"
        );

        String requestBody = objectMapper.writeValueAsString(paymentRequest);

        MvcResult result = mockMvc.perform(post("/api/v1/payments/intents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSecret").exists())
                .andExpect(jsonPath("$.amount").value(25.00))
                .andExpect(jsonPath("$.currency").value("usd"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> paymentResponse = objectMapper.readValue(response, Map.class);

        assertTrue(paymentResponse.containsKey("clientSecret"));
        assertEquals(25.00, paymentResponse.get("amount"));
        assertEquals("usd", paymentResponse.get("currency"));

        log.info("Payment creation successful with client secret: {}", paymentResponse.get("clientSecret"));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testCreatePayment_InvalidAmount_ShouldReturnBadRequest() throws Exception {
        log.info("Testing POST /api/v1/payments with invalid amount");

        Map<String, Object> paymentRequest = Map.of(
            "amount", -100,
            "currency", "usd",
            "customerId", "cust_test123"
        );

        String requestBody = objectMapper.writeValueAsString(paymentRequest);

        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = objectMapper.readValue(response, Map.class);

        assertTrue(errorResponse.containsKey("error"));
        assertTrue(errorResponse.get("message").toString().contains("amount"));

        log.info("Invalid amount correctly rejected: {}", errorResponse.get("message"));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testGetPaymentMethods_ShouldReturnCustomerPaymentMethods() throws Exception {
        log.info("Testing GET /api/v1/payments/methods");

        mockMvc.perform(get("/api/v1/payments/methods")
                .param("customerId", "cust_test123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethods").exists())
                .andReturn();

        log.info("Payment methods retrieved successfully");
    }

    @Test
    void testConfirmPayment_ShouldConfirmPaymentIntent() throws Exception {
        log.info("Testing POST /api/v1/payments/{paymentIntentId}/confirm");

        String paymentIntentId = "pi_test123";
        Map<String, Object> confirmRequest = Map.of(
            "paymentMethodId", "pm_card_visa"
        );

        String requestBody = objectMapper.writeValueAsString(confirmRequest);

        // Create a proper PlatformUserPrincipal with organization ID
        PlatformUserPrincipal principal = PlatformUserPrincipal.organizationMember(
            UUID.fromString("01847b7f-d037-4c51-a4af-0c7688a1ec18"),
            "test@example.com",
            "Test User",
            UUID.fromString("01847b7f-d037-4c51-a4af-0c7688a1ec17"),
            "test-org",
            "USER"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/payments/intents/{paymentIntentId}/confirm", paymentIntentId)
                .with(withPrincipal(principal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> confirmResponse = objectMapper.readValue(response, Map.class);

        assertTrue(confirmResponse.containsKey("status"));

        log.info("Payment confirmation successful with status: {}", confirmResponse.get("status"));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testGetPaymentHistory_ShouldReturnUserPayments() throws Exception {
        log.info("Testing GET /api/v1/payments/history");

        mockMvc.perform(get("/api/v1/payments/history")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andReturn();

        log.info("Payment history retrieved successfully");
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"ADMIN"})
    void testCreateRefund_ShouldProcessRefund() throws Exception {
        log.info("Testing POST /api/v1/payments/{paymentIntentId}/refund");

        String paymentIntentId = "pi_test123";
        Map<String, Object> refundRequest = Map.of(
            "amount", 1000,
            "reason", "requested_by_customer"
        );

        String requestBody = objectMapper.writeValueAsString(refundRequest);

        MvcResult result = mockMvc.perform(post("/api/v1/payments/{paymentIntentId}/refund", paymentIntentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").exists())
                .andExpect(jsonPath("$.amount").value(1000))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> refundResponse = objectMapper.readValue(response, Map.class);

        assertTrue(refundResponse.containsKey("refundId"));
        assertEquals(1000, refundResponse.get("amount"));

        log.info("Refund processed successfully with ID: {}", refundResponse.get("refundId"));
    }

    @Test
    void testStripeWebhook_ShouldProcessWebhookEvent() throws Exception {
        log.info("Testing POST /api/v1/payments/webhook");

        Map<String, Object> webhookPayload = Map.of(
            "id", "evt_test123",
            "type", "payment_intent.succeeded",
            "data", Map.of(
                "object", Map.of(
                    "id", "pi_test123",
                    "status", "succeeded",
                    "amount", 2500
                )
            )
        );

        String requestBody = objectMapper.writeValueAsString(webhookPayload);

        mockMvc.perform(post("/api/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("Stripe-Signature", "t=1234567890,v1=test_signature"))
                .andExpect(status().isOk())
                .andReturn();

        log.info("Webhook processed successfully");
    }

    @Test
    void testGetPayment_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        log.info("Testing GET /api/v1/payments/pi_test123 without authentication");

        mockMvc.perform(get("/api/v1/payments/{paymentIntentId}", "pi_test123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();

        log.info("Unauthorized access correctly rejected");
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testCreateCustomer_ShouldCreateStripeCustomer() throws Exception {
        log.info("Testing POST /api/v1/payments/customers");

        Map<String, Object> customerRequest = Map.of(
            "email", "test@example.com",
            "name", "Test User",
            "phone", "+1234567890"
        );

        String requestBody = objectMapper.writeValueAsString(customerRequest);

        MvcResult result = mockMvc.perform(post("/api/v1/payments/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> customerResponse = objectMapper.readValue(response, Map.class);

        assertTrue(customerResponse.containsKey("customerId"));
        assertEquals("test@example.com", customerResponse.get("email"));

        log.info("Customer created successfully with ID: {}", customerResponse.get("customerId"));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testGetPaymentStatus_ShouldReturnPaymentDetails() throws Exception {
        log.info("Testing GET /api/v1/payments/{paymentIntentId}/status");

        String paymentIntentId = "pi_test123";

        MvcResult result = mockMvc.perform(get("/api/v1/payments/{paymentIntentId}/status", paymentIntentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").value(paymentIntentId))
                .andExpect(jsonPath("$.status").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> statusResponse = objectMapper.readValue(response, Map.class);

        assertEquals(paymentIntentId, statusResponse.get("paymentIntentId"));
        assertTrue(statusResponse.containsKey("status"));

        log.info("Payment status retrieved: {}", statusResponse.get("status"));
    }

    private PaymentDto.PaymentIntentResponse createMockPaymentIntentResponse() {
        // Create a mock PaymentIntentResponse with minimal required fields
        return new PaymentDto.PaymentIntentResponse(
            "pi_test123456789",
            "pi_test123456789_secret_abc123",
            "requires_payment_method",
            java.math.BigDecimal.valueOf(25.00),
            "usd",
            "Test payment",
            java.util.Map.of()
        );
    }

    private RequestPostProcessor withPrincipal(PlatformUserPrincipal principal) {
        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());
        return authentication(authenticationToken);
    }
}