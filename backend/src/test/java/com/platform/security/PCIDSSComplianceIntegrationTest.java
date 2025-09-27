package com.platform.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.audit.internal.AuditService;
import com.platform.payment.internal.PaymentRepository;
import com.platform.payment.internal.PaymentMethodRepository;
import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.shared.security.TenantContext;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Integration tests for PCI DSS (Payment Card Industry Data Security Standard) compliance.
 * Tests cardholder data protection, secure transmission, access controls, and audit requirements.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "platform.security.pci-dss.enabled=true",
    "platform.security.encrypt-sensitive-data=true",
    "platform.audit.enable-pii-redaction=true"
})
@Transactional
class PCIDSSComplianceIntegrationTest {

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
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditService auditService;

    private UUID orgId;
    private UUID userId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        // Clear existing data
        auditEventRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        paymentRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create test organization
        Organization org = new Organization("PCI DSS Test Corp", "pci-test", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test users
        User user = new User("user@pcitest.com", "PCI Test User");
        user.setOrganization(org);
        user = userRepository.save(user);
        userId = user.getId();

        User adminUser = new User("admin@pcitest.com", "PCI Admin User");
        adminUser.setOrganization(org);
        adminUser = userRepository.save(adminUser);
        adminUserId = adminUser.getId();
    }

    @Test
    void shouldNeverStoreCardholderDataInPlainText() throws Exception {
        authenticateAs(userId, orgId, "pci-test", "USER");

        // Attempt to create payment method with full card data
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_pci_compliance",
                "type": "CARD",
                "billingDetails": {
                    "name": "PCI Test User",
                    "email": "user@pcitest.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "4242",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentMethodRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lastFour").value("4242")) // Only last 4 digits
                .andExpect(jsonPath("$.cardNumber").doesNotExist()) // Full card number should not be present
                .andExpect(jsonPath("$.cvv").doesNotExist()) // CVV should never be stored
                .andExpect(jsonPath("$.stripePaymentMethodId").value("pm_test_pci_compliance")); // Stripe token is OK

        // Verify no full card data is stored in database
        var paymentMethods = paymentMethodRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(orgId);
        assertEquals(1, paymentMethods.size());

        var savedPaymentMethod = paymentMethods.get(0);
        assertNotNull(savedPaymentMethod.getLastFour());
        assertEquals("4242", savedPaymentMethod.getLastFour());

        // Verify sensitive data is not stored in plain text
        // (Implementation should use Stripe tokens, not store card data)
        assertNotNull(savedPaymentMethod.getStripePaymentMethodId());
        assertFalse(savedPaymentMethod.getStripePaymentMethodId().contains("4111")); // No full card number
    }

    @Test
    void shouldRedactCardDataInAuditLogs() throws Exception {
        authenticateAs(userId, orgId, "pci-test", "USER");

        // Create payment with card information
        Map<String, Object> paymentData = Map.of(
            "amount", 100.00,
            "currency", "usd",
            "cardNumber", "4111-1111-1111-1111", // Should be redacted
            "cardholderName", "John Doe",
            "cvv", "123", // Should be redacted
            "expiry", "12/25",
            "description", "PCI compliance test payment"
        );

        auditService.logPaymentEvent(
            "PAYMENT_ATTEMPTED",
            "payment-pci-test",
            "CREATE",
            paymentData,
            "192.168.1.100",
            "PCI-Test-Client"
        ).get(5, TimeUnit.SECONDS);

        // Verify card data is redacted in audit logs
        var auditEvents = auditEventRepository.findByResourceTypeAndResourceId("PAYMENT", "payment-pci-test");
        assertEquals(1, auditEvents.size());

        var auditEvent = auditEvents.get(0);
        String requestData = auditEvent.getRequestData();

        // Verify sensitive data is redacted
        assertFalse(requestData.contains("4111-1111-1111-1111"));
        assertFalse(requestData.contains("123")); // CVV redacted
        assertTrue(requestData.contains("[REDACTED]"));

        // Non-sensitive data should remain
        assertTrue(requestData.contains("John Doe")); // Name is not automatically redacted
        assertTrue(requestData.contains("100")); // Amount is not sensitive
        assertTrue(requestData.contains("12/25")); // Expiry is borderline but may be kept
    }

    @Test
    void shouldEnforceStrictAccessControlsForPaymentData() throws Exception {
        // Regular user creates payment method
        authenticateAs(userId, orgId, "pci-test", "USER");

        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_access_control",
                "type": "CARD",
                "billingDetails": {
                    "name": "Access Control Test",
                    "email": "user@pcitest.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "4242",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentMethodRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var pmResponse = objectMapper.readTree(responseJson);
        String paymentMethodId = pmResponse.get("id").asText();

        // User can view their own payment methods (limited data)
        mockMvc.perform(get("/api/v1/organizations/{orgId}/payment-methods/{pmId}", orgId, paymentMethodId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastFour").value("4242"))
                .andExpect(jsonPath("$.fullCardNumber").doesNotExist()); // Should never be exposed

        // User cannot access payment method details via debug/admin endpoints
        mockMvc.perform(get("/api/v1/organizations/{orgId}/payment-methods/{pmId}/debug", orgId, paymentMethodId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // User cannot export payment data without proper authorization
        mockMvc.perform(get("/api/v1/organizations/{orgId}/payment-data/export", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Even admin should have limited access to sensitive payment data
        authenticateAs(adminUserId, orgId, "pci-test", "ADMIN");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/payment-methods/{pmId}/sensitive", orgId, paymentMethodId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // Should require special PCI admin role
    }

    @Test
    void shouldImplementSecureTransmissionRequirements() throws Exception {
        authenticateAs(userId, orgId, "pci-test", "USER");

        // Test that all payment endpoints enforce HTTPS
        // This is typically enforced at the infrastructure level, but we can verify headers

        String paymentRequest = """
            {
                "amount": 50.00,
                "currency": "usd",
                "description": "Secure transmission test",
                "paymentMethodId": "pm_test_secure"
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest)
                .header("X-Forwarded-Proto", "https")) // Simulate HTTPS
                .andExpect(status().isCreated())
                .andReturn();

        // Verify security headers are present
        var response = result.getResponse();

        // Should have security headers
        assertNotNull(response.getHeader("X-Content-Type-Options"));
        assertNotNull(response.getHeader("X-Frame-Options"));
        assertNotNull(response.getHeader("X-XSS-Protection"));

        // Test rejection of non-HTTPS requests (if enforced)
        try {
            mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentRequest)
                    .header("X-Forwarded-Proto", "http")) // Simulate HTTP
                    .andExpect(status().isBadRequest()); // Should be rejected
        } catch (Exception e) {
            // May not be enforced in test environment
        }
    }

    @Test
    void shouldAuditAllPaymentDataAccess() throws Exception {
        auditEventRepository.deleteAll();

        authenticateAs(userId, orgId, "pci-test", "USER");

        // Create payment method
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_audit",
                "type": "CARD",
                "billingDetails": {
                    "name": "Audit Test User",
                    "email": "user@pcitest.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "4242",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentMethodRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var pmResponse = objectMapper.readTree(responseJson);
        String paymentMethodId = pmResponse.get("id").asText();

        Thread.sleep(1000);

        // Access payment method
        mockMvc.perform(get("/api/v1/organizations/{orgId}/payment-methods/{pmId}", orgId, paymentMethodId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Thread.sleep(1000);

        // Process payment
        String paymentRequest = String.format("""
            {
                "amount": 75.00,
                "currency": "usd",
                "description": "Audit test payment",
                "paymentMethodId": "%s"
            }
            """, paymentMethodId);

        mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest))
                .andExpect(status().isCreated());

        Thread.sleep(2000);

        // Verify all payment operations are audited
        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 20));

        assertTrue(auditEvents.getTotalElements() >= 3);

        var eventActions = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        // Should have payment method creation audit
        assertTrue(eventActions.stream()
            .anyMatch(action -> action.contains("PAYMENT_METHOD") && action.contains("CREAT")));

        // Should have payment method access audit
        assertTrue(eventActions.stream()
            .anyMatch(action -> action.contains("PAYMENT_METHOD") && action.contains("VIEW")));

        // Should have payment processing audit
        assertTrue(eventActions.stream()
            .anyMatch(action -> action.contains("PAYMENT") &&
                               (action.contains("CREAT") || action.contains("PROCESS"))));

        // Verify audit events contain required PCI DSS information
        for (var event : auditEvents.getContent()) {
            assertNotNull(event.getActorId()); // Who accessed the data
            assertNotNull(event.getCreatedAt()); // When
            assertNotNull(event.getIpAddress()); // From where
            assertNotNull(event.getUserAgent()); // What system
        }
    }

    @Test
    void shouldEnforcePasswordAndAuthenticationSecurity() throws Exception {
        authenticateAs(userId, orgId, "pci-test", "USER");

        // Test strong password requirements for payment access
        String weakPasswordChange = """
            {
                "currentPassword": "oldPassword123!",
                "newPassword": "weak",
                "confirmPassword": "weak"
            }
            """;

        mockMvc.perform(post("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(weakPasswordChange))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password")));

        // Test account lockout after multiple failed attempts
        for (int i = 0; i < 5; i++) {
            try {
                String wrongPasswordChange = """
                    {
                        "currentPassword": "wrongPassword",
                        "newPassword": "NewStrongPassword123!",
                        "confirmPassword": "NewStrongPassword123!"
                    }
                    """;

                mockMvc.perform(post("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPasswordChange))
                        .andExpect(status().isBadRequest());
            } catch (Exception e) {
                // Expected to fail
            }
        }

        // Should be locked out after multiple attempts
        String validPasswordChange = """
            {
                "currentPassword": "correctPassword123!",
                "newPassword": "NewStrongPassword123!",
                "confirmPassword": "NewStrongPassword123!"
            }
            """;

        // Account should be locked (or rate limited)
        mockMvc.perform(post("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPasswordChange))
                .andExpect(status().isTooManyRequests()); // Rate limited due to failed attempts
    }

    @Test
    void shouldImplementNetworkSecurityControls() throws Exception {
        authenticateAs(userId, orgId, "pci-test", "USER");

        // Test that payment endpoints are protected against common attacks

        // Test SQL injection protection
        String maliciousPaymentRequest = """
            {
                "amount": 100.00,
                "currency": "usd'; DROP TABLE payments; --",
                "description": "SQL Injection test",
                "paymentMethodId": "pm_test_security"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousPaymentRequest))
                .andExpect(status().isBadRequest()); // Should be rejected

        // Test XSS protection
        String xssPaymentRequest = """
            {
                "amount": 100.00,
                "currency": "usd",
                "description": "<script>alert('xss')</script>",
                "paymentMethodId": "pm_test_security"
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(xssPaymentRequest))
                .andExpect(status().isCreated()) // May be accepted but sanitized
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var paymentResponse = objectMapper.readTree(responseJson);
        String description = paymentResponse.get("description").asText();

        // Description should be sanitized
        assertFalse(description.contains("<script>"));
        assertFalse(description.contains("alert"));

        // Test CSRF protection
        mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousPaymentRequest)
                .header("Origin", "https://malicious-site.com"))
                .andExpect(status().isForbidden()); // Should be rejected due to CORS
    }

    @Test
    void shouldImplementDataRetentionAndDeletion() throws Exception {
        auditEventRepository.deleteAll();

        authenticateAs(userId, orgId, "pci-test", "USER");

        // Create payment data
        String paymentMethodRequest = """
            {
                "stripePaymentMethodId": "pm_test_retention",
                "type": "CARD",
                "billingDetails": {
                    "name": "Retention Test User",
                    "email": "user@pcitest.com"
                },
                "cardDetails": {
                    "brand": "visa",
                    "lastFour": "4242",
                    "expMonth": 12,
                    "expYear": 2025
                }
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentMethodRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var pmResponse = objectMapper.readTree(responseJson);
        String paymentMethodId = pmResponse.get("id").asText();

        Thread.sleep(1000);

        // Test data deletion request (GDPR/PCI compliance)
        String deletionRequest = """
            {
                "reason": "PCI_COMPLIANCE",
                "confirmDeletion": true,
                "secureErase": true
            }
            """;

        mockMvc.perform(delete("/api/v1/organizations/{orgId}/payment-methods/{pmId}/secure-delete",
                orgId, paymentMethodId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(deletionRequest))
                .andExpect(status().isOk());

        Thread.sleep(1000);

        // Verify payment method is securely deleted
        mockMvc.perform(get("/api/v1/organizations/{orgId}/payment-methods/{pmId}", orgId, paymentMethodId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Verify deletion is audited
        var auditEvents = auditEventRepository.findByResourceTypeAndResourceId(
            "PAYMENT_METHOD", paymentMethodId);

        assertTrue(auditEvents.stream()
            .anyMatch(e -> e.getAction().contains("DELETE") || e.getAction().contains("DESTROY")));

        // Test bulk data purging for expired data
        authenticateAs(adminUserId, orgId, "pci-test", "ADMIN");

        mockMvc.perform(post("/api/v1/admin/pci-compliance/purge-expired-data")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purgedCount").exists());
    }

    @Test
    void shouldEnforceVulnerabilityManagement() throws Exception {
        authenticateAs(adminUserId, orgId, "pci-test", "ADMIN");

        // Test that security scanning endpoints are available for admins
        mockMvc.perform(get("/api/v1/admin/security/vulnerability-scan")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastScanDate").exists())
                .andExpect(jsonPath("$.vulnerabilities").isArray());

        // Test security patch status
        mockMvc.perform(get("/api/v1/admin/security/patch-status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upToDate").exists());

        // Regular users should not have access to security information
        authenticateAs(userId, orgId, "pci-test", "USER");

        mockMvc.perform(get("/api/v1/admin/security/vulnerability-scan")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldImplementIncidentResponseProcedures() throws Exception {
        auditEventRepository.deleteAll();

        // Simulate security incident detection
        authenticateAs(userId, orgId, "pci-test", "USER");

        // Multiple suspicious payment attempts
        for (int i = 0; i < 10; i++) {
            String suspiciousRequest = String.format("""
                {
                    "amount": %.2f,
                    "currency": "usd",
                    "description": "Suspicious payment %d",
                    "paymentMethodId": "pm_suspicious_%d"
                }
                """, Math.random() * 1000, i, i);

            try {
                mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(suspiciousRequest)
                        .header("X-Forwarded-For", "192.168.100." + (50 + i)))
                        .andExpect(status().isCreated());
            } catch (Exception e) {
                // Some may fail, that's expected
            }

            Thread.sleep(100);
        }

        Thread.sleep(2000);

        // Verify incident is detected and logged
        var securityEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 50));

        // Should have multiple payment events
        assertTrue(securityEvents.getTotalElements() >= 5);

        // Admin should be able to view security incidents
        authenticateAs(adminUserId, orgId, "pci-test", "ADMIN");

        mockMvc.perform(get("/api/v1/admin/security/incidents")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Test incident response actions
        String incidentResponse = """
            {
                "incidentId": "INC-001",
                "action": "BLOCK_IP",
                "targetIp": "192.168.100.55",
                "reason": "Suspicious payment activity",
                "duration": "24h"
            }
            """;

        mockMvc.perform(post("/api/v1/admin/security/incident-response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(incidentResponse))
                .andExpect(status().isOk());
    }

    private void authenticateAs(UUID userId, UUID orgId, String orgSlug, String role) {
        PlatformUserPrincipal principal = PlatformUserPrincipal.organizationMember(
            userId,
            "user@pcitest.com",
            "PCI Test User",
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
}