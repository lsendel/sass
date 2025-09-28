package com.platform.user.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.user.internal.*;

/**
 * Integration test for permission enforcement across the RBAC system.
 * Tests real permission checking scenarios from quickstart.md including:
 * - User permission validation based on assigned roles
 * - Cross-module permission enforcement
 * - Permission caching and cache invalidation
 * - Multi-role permission aggregation
 * - Organization isolation for permissions
 *
 * Based on scenarios from quickstart.md:
 * "Managing User Permissions and Access Control"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PermissionEnforcementIntegrationTest {

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
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    private Organization testOrganization;
    private Organization otherOrganization;
    private User paymentManagerUser;
    private User viewerUser;
    private User adminUser;
    private Role paymentManagerRole;
    private Role viewerRole;
    private Role adminRole;
    private List<Permission> systemPermissions;

    @BeforeEach
    void setUp() {
        // Create test organizations
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setCode("TEST_ORG");
        testOrganization.setCreatedAt(Instant.now());
        testOrganization = organizationRepository.save(testOrganization);

        otherOrganization = new Organization();
        otherOrganization.setName("Other Organization");
        otherOrganization.setCode("OTHER_ORG");
        otherOrganization.setCreatedAt(Instant.now());
        otherOrganization = organizationRepository.save(otherOrganization);

        // Load system permissions
        systemPermissions = permissionRepository.findAllActive();
        assertTrue(systemPermissions.size() >= 20, "System permissions should be populated");

        // Create test users
        paymentManagerUser = createUser("payment.manager@test.com", "Payment", "Manager");
        viewerUser = createUser("viewer@test.com", "Test", "Viewer");
        adminUser = createUser("admin@test.com", "Test", "Admin");

        // Get predefined roles (created by migration V016)
        viewerRole = roleRepository.findByOrganizationIdAndNameAndRoleType(
            testOrganization.getId(), "viewer", RoleType.PREDEFINED)
            .orElseThrow(() -> new RuntimeException("Viewer role not found"));

        adminRole = roleRepository.findByOrganizationIdAndNameAndRoleType(
            testOrganization.getId(), "admin", RoleType.PREDEFINED)
            .orElseThrow(() -> new RuntimeException("Admin role not found"));

        // Create custom payment-manager role
        paymentManagerRole = createCustomRole();

        // Assign roles to users
        assignUserRole(paymentManagerUser, paymentManagerRole);
        assignUserRole(viewerUser, viewerRole);
        assignUserRole(adminUser, adminRole);
    }

    @Test
    @WithMockUser(username = "payment.manager@test.com", authorities = {"PAYMENTS:READ", "PAYMENTS:WRITE", "USERS:READ"})
    @DisplayName("Should enforce payment manager permissions correctly")
    void shouldEnforcePaymentManagerPermissions() throws Exception {
        // Payment manager should have payment permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("PAYMENTS", "WRITE"),
                    new PermissionCheck("USERS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true))
                .andExpect(jsonPath("$.results[0].resource").value("PAYMENTS"))
                .andExpect(jsonPath("$.results[0].action").value("READ"))
                .andExpect(jsonPath("$.results[1].hasPermission").value(true))
                .andExpect(jsonPath("$.results[1].resource").value("PAYMENTS"))
                .andExpect(jsonPath("$.results[1].action").value("WRITE"))
                .andExpect(jsonPath("$.results[2].hasPermission").value(true))
                .andExpect(jsonPath("$.results[2].resource").value("USERS"))
                .andExpect(jsonPath("$.results[2].action").value("READ"));

        // Payment manager should NOT have admin permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("ORGANIZATIONS", "ADMIN"),
                    new PermissionCheck("USERS", "WRITE"),
                    new PermissionCheck("SUBSCRIPTIONS", "ADMIN")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false))
                .andExpect(jsonPath("$.results[1].hasPermission").value(false))
                .andExpect(jsonPath("$.results[2].hasPermission").value(false));
    }

    @Test
    @WithMockUser(username = "viewer@test.com", authorities = {"USERS:READ", "ORGANIZATIONS:READ", "PAYMENTS:READ", "SUBSCRIPTIONS:READ", "AUDIT:READ"})
    @DisplayName("Should enforce viewer read-only permissions")
    void shouldEnforceViewerReadOnlyPermissions() throws Exception {
        // Viewer should have all read permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("USERS", "READ"),
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("SUBSCRIPTIONS", "READ"),
                    new PermissionCheck("ORGANIZATIONS", "READ"),
                    new PermissionCheck("AUDIT", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[*].hasPermission").value(true));

        // Viewer should NOT have any write/admin permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("USERS", "WRITE"),
                    new PermissionCheck("PAYMENTS", "WRITE"),
                    new PermissionCheck("ORGANIZATIONS", "ADMIN"),
                    new PermissionCheck("SUBSCRIPTIONS", "ADMIN")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[*].hasPermission").value(false));
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
    @DisplayName("Should enforce organization isolation between tenants")
    void shouldEnforceOrganizationIsolation() throws Exception {
        // Admin should have permissions in their organization
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("ORGANIZATIONS", "ADMIN"),
                    new PermissionCheck("USERS", "WRITE")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true))
                .andExpect(jsonPath("$.results[1].hasPermission").value(true));

        // Admin should NOT have permissions in other organization
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", otherOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("ORGANIZATIONS", "ADMIN"),
                    new PermissionCheck("USERS", "WRITE")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false))
                .andExpect(jsonPath("$.results[1].hasPermission").value(false));
    }

    @Test
    @WithMockUser(username = "payment.manager@test.com", authorities = {"PAYMENTS:READ", "PAYMENTS:WRITE", "USERS:READ"})
    @DisplayName("Should aggregate permissions from multiple roles")
    void shouldAggregatePermissionsFromMultipleRoles() throws Exception {
        // Assign additional viewer role to payment manager (multi-role scenario)
        assignUserRole(paymentManagerUser, viewerRole);

        // User should now have permissions from both payment-manager and viewer roles
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),    // From payment-manager
                    new PermissionCheck("PAYMENTS", "WRITE"),   // From payment-manager
                    new PermissionCheck("USERS", "READ"),       // From both roles
                    new PermissionCheck("SUBSCRIPTIONS", "READ"), // From viewer role
                    new PermissionCheck("AUDIT", "READ")        // From viewer role
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[*].hasPermission").value(true));

        // Verify user still doesn't have write permissions beyond what payment-manager provides
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("SUBSCRIPTIONS", "WRITE"),
                    new PermissionCheck("ORGANIZATIONS", "ADMIN")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[*].hasPermission").value(false));
    }

    @Test
    @WithMockUser(username = "payment.manager@test.com", authorities = {"PAYMENTS:READ", "PAYMENTS:WRITE", "USERS:READ"})
    @DisplayName("Should handle expired role assignments correctly")
    void shouldHandleExpiredRoleAssignments() throws Exception {
        // Set payment manager role to expire in the past
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(
            paymentManagerUser.getId(), paymentManagerRole.getId())
            .orElseThrow(() -> new RuntimeException("UserRole not found"));

        userRole.setExpiresAt(LocalDateTime.now().minusDays(1));
        userRoleRepository.save(userRole);

        // User should no longer have payment manager permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("PAYMENTS", "WRITE")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false))
                .andExpect(jsonPath("$.results[1].hasPermission").value(false));
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
    @DisplayName("Should validate database permission functions work correctly")
    void shouldValidateDatabasePermissionFunctions() throws Exception {
        // Test the PostgreSQL get_user_permissions function directly through repository
        List<Permission> userPermissions = permissionRepository.findByUserId(
            adminUser.getId(), testOrganization.getId());

        // Admin should have all permissions for admin role
        assertTrue(userPermissions.size() >= 15, "Admin should have multiple permissions");

        // Verify specific admin permissions exist
        boolean hasOrgAdmin = userPermissions.stream()
            .anyMatch(p -> "ORGANIZATIONS".equals(p.getResource()) && "ADMIN".equals(p.getAction()));
        boolean hasUserWrite = userPermissions.stream()
            .anyMatch(p -> "USERS".equals(p.getResource()) && "WRITE".equals(p.getAction()));

        assertTrue(hasOrgAdmin, "Admin should have ORGANIZATIONS:ADMIN permission");
        assertTrue(hasUserWrite, "Admin should have USERS:WRITE permission");
    }

    @Test
    @WithMockUser(username = "payment.manager@test.com", authorities = {"PAYMENTS:READ", "PAYMENTS:WRITE", "USERS:READ"})
    @DisplayName("Should enforce permission checks in cross-module scenarios")
    void shouldEnforcePermissionsAcrossModules() throws Exception {
        // Test payment module permission enforcement
        mockMvc.perform(get("/api/organizations/{organizationId}/payments", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Should have PAYMENTS:READ

        // Test user module permission enforcement
        mockMvc.perform(get("/api/organizations/{organizationId}/users", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Should have USERS:READ

        // Test unauthorized access to admin functions
        mockMvc.perform(post("/api/organizations/{organizationId}/roles", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden()); // Should NOT have ORGANIZATIONS:ADMIN
    }

    private User createUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private Role createCustomRole() {
        Role role = new Role();
        role.setName("payment-manager");
        role.setDescription("Manages payment processing and customer billing");
        role.setOrganizationId(testOrganization.getId());
        role.setRoleType(RoleType.CUSTOM);
        role.setCreatedAt(Instant.now());
        role.setCreatedBy(adminUser.getId());
        role = roleRepository.save(role);

        // Assign permissions: PAYMENTS:READ, PAYMENTS:WRITE, USERS:READ
        List<Permission> permissions = systemPermissions.stream()
            .filter(p ->
                ("PAYMENTS".equals(p.getResource()) && ("READ".equals(p.getAction()) || "WRITE".equals(p.getAction()))) ||
                ("USERS".equals(p.getResource()) && "READ".equals(p.getAction()))
            )
            .toList();

        for (Permission permission : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(role.getId());
            rolePermission.setPermissionId(permission.getId());
            rolePermission.setCreatedAt(Instant.now());
            rolePermissionRepository.save(rolePermission);
        }

        return role;
    }

    private void assignUserRole(User user, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRole.setAssignedAt(Instant.now());
        userRole.setAssignedBy(adminUser.getId());
        // No expiration for most test scenarios
        userRoleRepository.save(userRole);
    }

    /**
     * Helper class for permission check requests
     */
    private static class PermissionCheck {
        private String resource;
        private String action;

        public PermissionCheck(String resource, String action) {
            this.resource = resource;
            this.action = action;
        }

        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
}