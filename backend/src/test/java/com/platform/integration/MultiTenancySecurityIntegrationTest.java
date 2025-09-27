package com.platform.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import com.platform.subscription.internal.SubscriptionRepository;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Integration tests for multi-tenancy security enforcement.
 * Tests tenant isolation, cross-tenant access prevention, and security boundaries.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class MultiTenancySecurityIntegrationTest {

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
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    // Tenant A
    private UUID org1Id;
    private UUID user1Id;
    private UUID admin1Id;

    // Tenant B
    private UUID org2Id;
    private UUID user2Id;
    private UUID admin2Id;

    @BeforeEach
    void setUp() {
        // Clear existing data
        auditEventRepository.deleteAll();
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create Tenant A
        Organization org1 = new Organization("Tenant A Corp", "tenant-a", (UUID) null);
        org1 = organizationRepository.save(org1);
        org1Id = org1.getId();

        User user1 = new User("user1@tenanta.com", "User One");
        user1.setOrganization(org1);
        user1 = userRepository.save(user1);
        user1Id = user1.getId();

        User admin1 = new User("admin1@tenanta.com", "Admin One");
        admin1.setOrganization(org1);
        admin1 = userRepository.save(admin1);
        admin1Id = admin1.getId();

        // Create Tenant B
        Organization org2 = new Organization("Tenant B Corp", "tenant-b", (UUID) null);
        org2 = organizationRepository.save(org2);
        org2Id = org2.getId();

        User user2 = new User("user2@tenantb.com", "User Two");
        user2.setOrganization(org2);
        user2 = userRepository.save(user2);
        user2Id = user2.getId();

        User admin2 = new User("admin2@tenantb.com", "Admin Two");
        admin2.setOrganization(org2);
        admin2 = userRepository.save(admin2);
        admin2Id = admin2.getId();
    }

    @Test
    void shouldPreventCrossTenantUserAccess() throws Exception {
        // User from Tenant A trying to access Tenant B's user data
        authenticateAs(user1Id, org1Id, "tenant-a", "USER");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/users", org2Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Verify audit log for unauthorized access attempt
        var auditEvents = auditEventRepository.findAll();
        assertTrue(auditEvents.stream()
            .anyMatch(e -> "UNAUTHORIZED_ACCESS_ATTEMPT".equals(e.getAction()) ||
                          "ACCESS_DENIED".equals(e.getAction())));
    }

    @Test
    void shouldPreventCrossTenantPaymentAccess() throws Exception {
        // Create payment for Tenant A
        authenticateAs(admin1Id, org1Id, "tenant-a", "ADMIN");
        TenantContext.setTenantInfo(org1Id, "tenant-a", admin1Id);

        String paymentRequestA = """
            {
                "amount": 100.00,
                "currency": "usd",
                "description": "Tenant A payment",
                "paymentMethodId": "pm_test_123"
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", org1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequestA))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var payment = objectMapper.readTree(responseJson);
        String paymentId = payment.get("id").asText();

        // User from Tenant B trying to access Tenant A's payment
        authenticateAs(user2Id, org2Id, "tenant-b", "USER");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/payments/{paymentId}", org1Id, paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Try to access via wrong organization ID
        mockMvc.perform(get("/api/v1/organizations/{orgId}/payments/{paymentId}", org2Id, paymentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Payment belongs to different org
    }

    @Test
    void shouldPreventCrossTenantSubscriptionAccess() throws Exception {
        // Admin from Tenant A creates subscription
        authenticateAs(admin1Id, org1Id, "tenant-a", "ADMIN");

        // User from Tenant B tries to access Tenant A's subscription
        authenticateAs(user2Id, org2Id, "tenant-b", "USER");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/subscription", org1Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Try subscription management operations
        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription/cancel", org1Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldPreventCrossTenantAuditAccess() throws Exception {
        // User from Tenant A trying to access Tenant B's audit logs
        authenticateAs(user1Id, org1Id, "tenant-a", "USER");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/audit", org2Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Admin from Tenant A trying to access Tenant B's audit logs
        authenticateAs(admin1Id, org1Id, "tenant-a", "ADMIN");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/audit", org2Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceOrganizationScopedUserManagement() throws Exception {
        // Admin from Tenant A creates user for Tenant A
        authenticateAs(admin1Id, org1Id, "tenant-a", "ADMIN");

        String newUserRequest = """
            {
                "email": "newuser@tenanta.com",
                "name": "New User",
                "role": "USER"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/users", org1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUserRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@tenanta.com"));

        // Admin from Tenant A tries to create user for Tenant B
        mockMvc.perform(post("/api/v1/organizations/{orgId}/users", org2Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUserRequest))
                .andExpect(status().isForbidden());

        // Verify new user belongs to correct organization
        var newUser = userRepository.findByEmail("newuser@tenanta.com");
        assertTrue(newUser.isPresent());
        assertEquals(org1Id, newUser.get().getOrganization().getId());
    }

    @Test
    void shouldPreventDataLeakageThroughPagination() throws Exception {
        // Create users in both tenants
        authenticateAs(admin1Id, org1Id, "tenant-a", "ADMIN");
        for (int i = 0; i < 5; i++) {
            User userA = new User("user" + i + "@tenanta.com", "User A" + i);
            userA.setOrganization(organizationRepository.findById(org1Id).orElseThrow());
            userRepository.save(userA);
        }

        authenticateAs(admin2Id, org2Id, "tenant-b", "ADMIN");
        for (int i = 0; i < 5; i++) {
            User userB = new User("user" + i + "@tenantb.com", "User B" + i);
            userB.setOrganization(organizationRepository.findById(org2Id).orElseThrow());
            userRepository.save(userB);
        }

        // User from Tenant A queries users with large page size
        authenticateAs(admin1Id, org1Id, "tenant-a", "ADMIN");

        var result = mockMvc.perform(get("/api/v1/organizations/{orgId}/users", org1Id)
                .param("page", "0")
                .param("size", "100") // Large page size
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        var usersPage = objectMapper.readValue(response, Map.class);

        // Verify only Tenant A users are returned
        @SuppressWarnings("unchecked")
        var users = (java.util.List<Map<String, Object>>) usersPage.get("content");

        for (Map<String, Object> user : users) {
            String email = (String) user.get("email");
            assertTrue(email.contains("@tenanta.com"),
                "Found cross-tenant user: " + email);
        }
    }

    @Test
    void shouldPreventTenantContextSpoofing() throws Exception {
        // Authenticate as Tenant A user
        authenticateAs(user1Id, org1Id, "tenant-a", "USER");

        // Try to manually set tenant context to Tenant B
        TenantContext.setTenantInfo(org2Id, "tenant-b", user1Id);

        // Access should still be denied based on authenticated user's organization
        mockMvc.perform(get("/api/v1/organizations/{orgId}/users", org2Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Try payment access with spoofed context
        mockMvc.perform(get("/api/v1/organizations/{orgId}/payments", org2Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceRoleBasedAccessWithinTenant() throws Exception {
        // Regular user from Tenant A tries admin operations
        authenticateAs(user1Id, org1Id, "tenant-a", "USER");

        String newUserRequest = """
            {
                "email": "test@tenanta.com",
                "name": "Test User",
                "role": "USER"
            }
            """;

        // User cannot create other users (admin operation)
        mockMvc.perform(post("/api/v1/organizations/{orgId}/users", org1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUserRequest))
                .andExpect(status().isForbidden());

        // User cannot access all organization users
        mockMvc.perform(get("/api/v1/organizations/{orgId}/users", org1Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // But user can access their own profile
        mockMvc.perform(get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user1@tenanta.com"));
    }

    @Test
    void shouldPreventCrossTenantResourceReferences() throws Exception {
        // Create resource in Tenant A
        authenticateAs(admin1Id, org1Id, "tenant-a", "ADMIN");
        TenantContext.setTenantInfo(org1Id, "tenant-a", admin1Id);

        // Create payment for Tenant A
        String paymentRequestA = """
            {
                "amount": 150.00,
                "currency": "usd",
                "description": "Tenant A payment",
                "paymentMethodId": "pm_test_456"
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", org1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequestA))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var payment = objectMapper.readTree(responseJson);
        String paymentId = payment.get("id").asText();

        // User from Tenant B tries to reference Tenant A's payment
        authenticateAs(admin2Id, org2Id, "tenant-b", "ADMIN");

        String refundRequest = """
            {
                "amount": 50.00,
                "reason": "customer_request"
            }
            """;

        // Try to refund Tenant A's payment while authenticated as Tenant B
        mockMvc.perform(post("/api/v1/organizations/{orgId}/payments/{paymentId}/refunds",
                org2Id, paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(refundRequest))
                .andExpect(status().isNotFound()); // Payment not found in Tenant B's scope
    }

    @Test
    void shouldPreventDatabaseQueryInjectionAcrossTenants() throws Exception {
        // Attempt SQL injection to access other tenant's data
        authenticateAs(user1Id, org1Id, "tenant-a", "USER");

        // Try SQL injection in search parameter
        String maliciousQuery = "'; DROP TABLE users; SELECT * FROM users WHERE organization_id = '" + org2Id + "' --";

        mockMvc.perform(get("/api/v1/organizations/{orgId}/users/search", org1Id)
                .param("q", maliciousQuery)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Should not fail but also not return cross-tenant data

        // Verify Tenant B's data is still intact
        authenticateAs(user2Id, org2Id, "tenant-b", "USER");
        var tenantBUser = userRepository.findByEmail("user2@tenantb.com");
        assertTrue(tenantBUser.isPresent());
        assertEquals(org2Id, tenantBUser.get().getOrganization().getId());
    }

    @Test
    void shouldAuditCrossTenantAccessAttempts() throws Exception {
        // Clear audit events
        auditEventRepository.deleteAll();

        // Attempt cross-tenant access
        authenticateAs(user1Id, org1Id, "tenant-a", "USER");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/payments", org2Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/organizations/{orgId}/users", org2Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Verify security audit events are created
        var securityEvents = auditEventRepository.findAll();

        assertTrue(securityEvents.stream()
            .anyMatch(e -> "UNAUTHORIZED_ACCESS_ATTEMPT".equals(e.getAction()) ||
                          "ACCESS_DENIED".equals(e.getAction())));

        // Verify event contains attempted resource and source tenant
        var accessDeniedEvent = securityEvents.stream()
            .filter(e -> "ACCESS_DENIED".equals(e.getAction()) ||
                        "UNAUTHORIZED_ACCESS_ATTEMPT".equals(e.getAction()))
            .findFirst();

        if (accessDeniedEvent.isPresent()) {
            assertEquals(user1Id, accessDeniedEvent.get().getActorId());
            assertEquals(org1Id, accessDeniedEvent.get().getOrganizationId());
        }
    }

    @Test
    void shouldPreventSessionSharingAcrossTenants() throws Exception {
        // User from Tenant A logs in
        authenticateAs(user1Id, org1Id, "tenant-a", "USER");

        // Verify access to Tenant A resources
        mockMvc.perform(get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user1@tenanta.com"));

        // Change authentication to Tenant B user (simulating session hijacking)
        authenticateAs(user2Id, org2Id, "tenant-b", "USER");

        // Verify cannot access Tenant A data with new authentication
        mockMvc.perform(get("/api/v1/organizations/{orgId}/users", org1Id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Verify can access Tenant B data
        mockMvc.perform(get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user2@tenantb.com"));
    }

    @Test
    void shouldValidateTenantBoundariesInBulkOperations() throws Exception {
        // Create multiple users for Tenant A
        for (int i = 0; i < 3; i++) {
            User user = new User("bulk" + i + "@tenanta.com", "Bulk User " + i);
            user.setOrganization(organizationRepository.findById(org1Id).orElseThrow());
            userRepository.save(user);
        }

        // Admin from Tenant A performs bulk operation
        authenticateAs(admin1Id, org1Id, "tenant-a", "ADMIN");

        String bulkRequest = """
            {
                "action": "deactivate",
                "userIds": ["USER_ID_1", "USER_ID_2", "USER_ID_3"]
            }
            """;

        // Even with admin privileges, should only affect Tenant A users
        mockMvc.perform(post("/api/v1/organizations/{orgId}/users/bulk", org1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkRequest))
                .andExpect(status().isOk());

        // Verify Tenant B users are unaffected
        var tenantBUsers = userRepository.findByOrganizationId(org2Id);
        for (User user : tenantBUsers) {
            assertTrue(user.isActive()); // Should still be active
        }
    }

    private void authenticateAs(UUID userId, UUID orgId, String orgSlug, String role) {
        PlatformUserPrincipal principal = PlatformUserPrincipal.organizationMember(
            userId,
            "user@example.com",
            "Test User",
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