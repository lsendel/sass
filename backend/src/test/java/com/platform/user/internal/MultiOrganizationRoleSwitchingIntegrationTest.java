package com.platform.user.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
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
 * Integration test for multi-organization role switching and tenant isolation.
 * Tests user context switching between organizations including:
 * - User with different roles in multiple organizations
 * - Organization context switching and permission isolation
 * - Cache isolation between organization contexts
 * - Cross-organization data access prevention
 * - Session management across organization switches
 *
 * Based on multi-tenant scenarios from quickstart.md:
 * "Multi-Organization User Access and Context Switching"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class MultiOrganizationRoleSwitchingIntegrationTest {

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
    private RedisTemplate<String, Object> redisTemplate;

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

    private Organization organization1;
    private Organization organization2;
    private Organization organization3;
    private User multiOrgUser;
    private User org1AdminUser;
    private User org2ViewerUser;
    private Role org1AdminRole;
    private Role org1PaymentRole;
    private Role org2AdminRole;
    private Role org2ViewerRole;
    private List<Permission> systemPermissions;

    @BeforeEach
    void setUp() {
        // Create multiple test organizations
        organization1 = createOrganization("Organization One", "ORG1");
        organization2 = createOrganization("Organization Two", "ORG2");
        organization3 = createOrganization("Organization Three", "ORG3");

        // Load system permissions
        systemPermissions = permissionRepository.findAllActive();
        assertTrue(systemPermissions.size() >= 20, "System permissions should be populated");

        // Create users with different org access patterns
        multiOrgUser = createUser("multiorg@test.com", "Multi", "OrgUser");
        org1AdminUser = createUser("org1admin@test.com", "Org1", "Admin");
        org2ViewerUser = createUser("org2viewer@test.com", "Org2", "Viewer");

        // Get predefined roles for each organization
        org1AdminRole = getRoleByName(organization1, "admin");
        org2AdminRole = getRoleByName(organization2, "admin");
        org2ViewerRole = getRoleByName(organization2, "viewer");

        // Create custom role in org1
        org1PaymentRole = createCustomRole(organization1, "payment-specialist",
            "Payment processing specialist for Org1");

        // Set up multi-organization user roles
        assignUserRole(multiOrgUser, org1AdminRole, organization1);
        assignUserRole(multiOrgUser, org2ViewerRole, organization2);
        // Note: User has NO role in organization3

        // Set up single-org users
        assignUserRole(org1AdminUser, org1AdminRole, organization1);
        assignUserRole(org2ViewerUser, org2ViewerRole, organization2);

        // Clear Redis cache before tests
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @WithMockUser(username = "multiorg@test.com")
    @DisplayName("Should have different permissions in different organizations")
    void shouldHaveDifferentPermissionsAcrossOrganizations() throws Exception {
        // In Organization 1: Should have admin permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("ORGANIZATIONS", "ADMIN"),
                    new PermissionCheck("USERS", "WRITE"),
                    new PermissionCheck("PAYMENTS", "WRITE")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true))  // Admin permissions
                .andExpect(jsonPath("$.results[1].hasPermission").value(true))
                .andExpect(jsonPath("$.results[2].hasPermission").value(true));

        // In Organization 2: Should have only viewer (read-only) permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization2.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("ORGANIZATIONS", "ADMIN"),  // Should be false
                    new PermissionCheck("USERS", "READ"),           // Should be true
                    new PermissionCheck("USERS", "WRITE"),          // Should be false
                    new PermissionCheck("PAYMENTS", "READ")         // Should be true
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false)) // No admin
                .andExpect(jsonPath("$.results[1].hasPermission").value(true))  // Can read
                .andExpect(jsonPath("$.results[2].hasPermission").value(false)) // Can't write
                .andExpect(jsonPath("$.results[3].hasPermission").value(true)); // Can read payments

        // In Organization 3: Should have NO permissions (not a member)
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization3.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("USERS", "READ"),
                    new PermissionCheck("PAYMENTS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false))
                .andExpect(jsonPath("$.results[1].hasPermission").value(false));
    }

    @Test
    @WithMockUser(username = "multiorg@test.com")
    @DisplayName("Should enforce organization isolation for data access")
    void shouldEnforceOrganizationIsolation() throws Exception {
        // Can access Organization 1 data (admin role)
        mockMvc.perform(get("/api/organizations/{organizationId}/users", organization1.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/organizations/{organizationId}/roles", organization1.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Can access Organization 2 data (viewer role - read only)
        mockMvc.perform(get("/api/organizations/{organizationId}/users", organization2.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Cannot modify Organization 2 data (viewer role - no write)
        mockMvc.perform(post("/api/organizations/{organizationId}/roles", organization2.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        // Cannot access Organization 3 data at all (no role)
        mockMvc.perform(get("/api/organizations/{organizationId}/users", organization3.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/organizations/{organizationId}/roles", organization3.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "multiorg@test.com")
    @DisplayName("Should maintain separate permission caches per organization")
    void shouldMaintainSeparatePermissionCaches() throws Exception {
        String org1CacheKey = "user_permissions:" + multiOrgUser.getId() + ":" + organization1.getId();
        String org2CacheKey = "user_permissions:" + multiOrgUser.getId() + ":" + organization2.getId();
        String org3CacheKey = "user_permissions:" + multiOrgUser.getId() + ":" + organization3.getId();

        // Access permissions in Organization 1 (should cache admin permissions)
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("ORGANIZATIONS", "ADMIN")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true));

        // Access permissions in Organization 2 (should cache viewer permissions)
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization2.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("USERS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true));

        // Verify separate cache entries exist
        Object org1Cache = redisTemplate.opsForValue().get(org1CacheKey);
        Object org2Cache = redisTemplate.opsForValue().get(org2CacheKey);
        Object org3Cache = redisTemplate.opsForValue().get(org3CacheKey);

        assertNotNull(org1Cache, "Organization 1 permissions should be cached");
        assertNotNull(org2Cache, "Organization 2 permissions should be cached");
        assertNull(org3Cache, "Organization 3 permissions should not be cached (no access)");

        // Verify cache contents are different
        assertNotEquals(org1Cache, org2Cache, "Cache contents should differ between organizations");
    }

    @Test
    @WithMockUser(username = "org1admin@test.com")
    @DisplayName("Should prevent cross-organization role assignments")
    void shouldPreventCrossOrganizationRoleAssignments() throws Exception {
        // Attempt to assign Organization 2 role to user in Organization 1 context
        // This should fail due to organization isolation

        String assignRoleRequest = objectMapper.writeValueAsString(
            AssignRoleRequest.builder()
                .roleId(org2AdminRole.getId()) // Org2 role
                .build()
        );

        // Should fail - cannot assign org2 role through org1 endpoint
        mockMvc.perform(post("/api/organizations/{organizationId}/users/{userId}/roles",
                organization1.getId(), multiOrgUser.getId()) // Org1 context
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignRoleRequest))
                .andExpect(status().isBadRequest()); // Or forbidden, depending on implementation

        // Verify the role was not assigned
        List<UserRole> userRoles = userRoleRepository.findByUserId(multiOrgUser.getId());
        boolean hasIllegalAssignment = userRoles.stream()
            .anyMatch(ur -> ur.getRoleId().equals(org2AdminRole.getId()) &&
                           !ur.getAssignedAt().isBefore(Instant.now().minusSeconds(60))); // Recent assignment

        assertFalse(hasIllegalAssignment, "Cross-organization role assignment should be prevented");
    }

    @Test
    @WithMockUser(username = "multiorg@test.com")
    @DisplayName("Should handle organization context switching efficiently")
    void shouldHandleOrganizationContextSwitching() throws Exception {
        // Rapid context switching between organizations
        for (int i = 0; i < 5; i++) {
            // Switch to Organization 1 - check admin permission
            mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization1.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(
                        new PermissionCheck("ORGANIZATIONS", "ADMIN")
                    ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[0].hasPermission").value(true));

            // Switch to Organization 2 - check viewer permission
            mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization2.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(
                        new PermissionCheck("USERS", "READ")
                    ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[0].hasPermission").value(true));

            // Verify Organization 2 doesn't have admin permissions
            mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization2.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(
                        new PermissionCheck("ORGANIZATIONS", "ADMIN")
                    ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[0].hasPermission").value(false));
        }

        // Verify cache keys for both organizations exist
        String org1CacheKey = "user_permissions:" + multiOrgUser.getId() + ":" + organization1.getId();
        String org2CacheKey = "user_permissions:" + multiOrgUser.getId() + ":" + organization2.getId();

        assertTrue(redisTemplate.hasKey(org1CacheKey), "Organization 1 cache should exist");
        assertTrue(redisTemplate.hasKey(org2CacheKey), "Organization 2 cache should exist");
    }

    @Test
    @WithMockUser(username = "multiorg@test.com")
    @DisplayName("Should validate role inheritance and organization boundaries")
    void shouldValidateRoleInheritanceAndBoundaries() throws Exception {
        // Assign additional role in Organization 1
        assignUserRole(multiOrgUser, org1PaymentRole, organization1);

        // In Organization 1: Should have permissions from both admin and payment roles
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("ORGANIZATIONS", "ADMIN"), // From admin role
                    new PermissionCheck("PAYMENTS", "WRITE"),      // From payment role
                    new PermissionCheck("USERS", "READ")           // From both roles
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true))
                .andExpect(jsonPath("$.results[1].hasPermission").value(true))
                .andExpect(jsonPath("$.results[2].hasPermission").value(true));

        // In Organization 2: Should still only have viewer permissions (roles don't cross over)
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", organization2.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("ORGANIZATIONS", "ADMIN"), // Should be false
                    new PermissionCheck("PAYMENTS", "WRITE"),      // Should be false
                    new PermissionCheck("USERS", "READ")           // Should be true (viewer)
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false))
                .andExpect(jsonPath("$.results[1].hasPermission").value(false))
                .andExpect(jsonPath("$.results[2].hasPermission").value(true));
    }

    private Organization createOrganization(String name, String code) {
        Organization org = new Organization();
        org.setName(name);
        org.setCode(code);
        org.setCreatedAt(Instant.now());
        return organizationRepository.save(org);
    }

    private User createUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private Role getRoleByName(Organization org, String roleName) {
        return roleRepository.findByOrganizationIdAndNameAndRoleType(
            org.getId(), roleName, RoleType.PREDEFINED)
            .orElseThrow(() -> new RuntimeException(roleName + " role not found for org " + org.getCode()));
    }

    private Role createCustomRole(Organization org, String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setOrganizationId(org.getId());
        role.setRoleType(RoleType.CUSTOM);
        role.setCreatedAt(Instant.now());
        role.setCreatedBy(1L); // System user
        return roleRepository.save(role);
    }

    private void assignUserRole(User user, Role role, Organization org) {
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRole.setAssignedAt(Instant.now());
        userRole.setAssignedBy(1L); // System user
        userRoleRepository.save(userRole);
    }

    /**
     * Helper classes for request DTOs
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

    private static class AssignRoleRequest {
        private Long roleId;

        public static AssignRoleRequestBuilder builder() {
            return new AssignRoleRequestBuilder();
        }

        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }

        public static class AssignRoleRequestBuilder {
            private Long roleId;

            public AssignRoleRequestBuilder roleId(Long roleId) {
                this.roleId = roleId;
                return this;
            }

            public AssignRoleRequest build() {
                AssignRoleRequest request = new AssignRoleRequest();
                request.roleId = this.roleId;
                return request;
            }
        }
    }
}