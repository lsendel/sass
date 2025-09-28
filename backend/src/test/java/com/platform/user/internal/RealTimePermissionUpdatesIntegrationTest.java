package com.platform.user.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
 * Integration test for real-time permission updates and cache invalidation.
 * Tests immediate permission changes and cache coherence including:
 * - Role assignment and permission cache invalidation
 * - Role modification and immediate permission updates
 * - User role expiration and access revocation
 * - Permission caching with Redis and proper TTL
 * - Cross-session permission consistency
 *
 * Based on real-time scenarios from quickstart.md:
 * "Immediate Permission Updates and Cache Management"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class RealTimePermissionUpdatesIntegrationTest {

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

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    private Organization testOrganization;
    private User testUser;
    private User adminUser;
    private Role customRole;
    private Role viewerRole;
    private List<Permission> systemPermissions;

    @BeforeEach
    void setUp() {
        // Create test organization
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setCode("TEST_ORG");
        testOrganization.setCreatedAt(Instant.now());
        testOrganization = organizationRepository.save(testOrganization);

        // Load system permissions
        systemPermissions = permissionRepository.findAllActive();
        assertTrue(systemPermissions.size() >= 20, "System permissions should be populated");

        // Create test users
        testUser = createUser("testuser@test.com", "Test", "User");
        adminUser = createUser("admin@test.com", "Admin", "User");

        // Get predefined viewer role
        viewerRole = roleRepository.findByOrganizationIdAndNameAndRoleType(
            testOrganization.getId(), "viewer", RoleType.PREDEFINED)
            .orElseThrow(() -> new RuntimeException("Viewer role not found"));

        // Create custom role for testing
        customRole = createCustomRole("test-role", "Test Role for Permission Updates");

        // Clear Redis cache before tests
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
    @DisplayName("Should immediately reflect role assignment in permission checks")
    void shouldImmediatelyReflectRoleAssignment() throws Exception {
        // Initially user has no permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("USERS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false))
                .andExpect(jsonPath("$.results[1].hasPermission").value(false));

        // Assign custom role to user
        String assignRoleRequest = objectMapper.writeValueAsString(
            AssignRoleRequest.builder()
                .roleId(customRole.getId())
                .expiresAt(null) // No expiration
                .build()
        );

        mockMvc.perform(post("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignRoleRequest))
                .andExpect(status().isCreated());

        // Immediately check permissions - should be updated
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("USERS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true))
                .andExpect(jsonPath("$.results[1].hasPermission").value(true));

        // Verify cache invalidation occurred
        String cacheKey = "user_permissions:" + testUser.getId() + ":" + testOrganization.getId();
        Object cachedPermissions = redisTemplate.opsForValue().get(cacheKey);
        // Cache should either be null (invalidated) or contain the new permissions
        if (cachedPermissions != null) {
            // If cached, verify it contains the new permissions
            assertTrue(cachedPermissions.toString().contains("PAYMENTS:READ"));
            assertTrue(cachedPermissions.toString().contains("USERS:READ"));
        }
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
    @DisplayName("Should immediately reflect role removal in permission checks")
    void shouldImmediatelyReflectRoleRemoval() throws Exception {
        // First assign a role
        assignUserRole(testUser, customRole);

        // Verify user has permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("USERS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true))
                .andExpect(jsonPath("$.results[1].hasPermission").value(true));

        // Remove role assignment
        mockMvc.perform(delete("/api/organizations/{organizationId}/users/{userId}/roles/{roleId}",
                testOrganization.getId(), testUser.getId(), customRole.getId()))
                .andExpect(status().isNoContent());

        // Immediately check permissions - should be revoked
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("USERS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false))
                .andExpect(jsonPath("$.results[1].hasPermission").value(false));
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
    @DisplayName("Should immediately reflect role modification in permission checks")
    void shouldImmediatelyReflectRoleModification() throws Exception {
        // Assign initial role
        assignUserRole(testUser, customRole);

        // Verify initial permissions (PAYMENTS:READ, USERS:READ)
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("PAYMENTS", "WRITE")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true))
                .andExpect(jsonPath("$.results[1].hasPermission").value(false)); // No WRITE initially

        // Add PAYMENTS:WRITE permission to the role
        Permission paymentsWrite = systemPermissions.stream()
            .filter(p -> "PAYMENTS".equals(p.getResource()) && "WRITE".equals(p.getAction()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("PAYMENTS:WRITE not found"));

        RolePermission newRolePermission = new RolePermission();
        newRolePermission.setRoleId(customRole.getId());
        newRolePermission.setPermissionId(paymentsWrite.getId());
        newRolePermission.setCreatedAt(Instant.now());
        rolePermissionRepository.save(newRolePermission);

        // Immediately check permissions - should include new WRITE permission
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("PAYMENTS", "WRITE")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true))
                .andExpect(jsonPath("$.results[1].hasPermission").value(true)); // Now has WRITE
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
    @DisplayName("Should handle role expiration and immediate access revocation")
    void shouldHandleRoleExpirationImmediately() throws Exception {
        // Assign role with future expiration
        UserRole userRole = new UserRole();
        userRole.setUserId(testUser.getId());
        userRole.setRoleId(customRole.getId());
        userRole.setAssignedAt(Instant.now());
        userRole.setAssignedBy(adminUser.getId());
        userRole.setExpiresAt(LocalDateTime.now().plusMinutes(1)); // Expires in 1 minute
        userRoleRepository.save(userRole);

        // Verify user has permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true));

        // Manually expire the role (simulate time passing)
        userRole.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Now expired
        userRoleRepository.save(userRole);

        // Immediately check permissions - should be revoked due to expiration
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false));
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
    @DisplayName("Should maintain cache consistency across multiple role changes")
    void shouldMaintainCacheConsistencyAcrossChanges() throws Exception {
        String cacheKey = "user_permissions:" + testUser.getId() + ":" + testOrganization.getId();

        // Initially no cache entry
        Object initialCache = redisTemplate.opsForValue().get(cacheKey);
        assertNull(initialCache, "Cache should be empty initially");

        // Assign role and check permissions (populates cache)
        assignUserRole(testUser, customRole);

        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true));

        // Cache should now have permissions
        Object populatedCache = redisTemplate.opsForValue().get(cacheKey);
        assertNotNull(populatedCache, "Cache should be populated after permission check");

        // Assign additional role
        assignUserRole(testUser, viewerRole);

        // Check permissions again - should aggregate from both roles
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),
                    new PermissionCheck("SUBSCRIPTIONS", "READ") // From viewer role
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true))
                .andExpect(jsonPath("$.results[1].hasPermission").value(true));

        // Remove one role
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(
            testUser.getId(), customRole.getId())
            .orElseThrow(() -> new RuntimeException("UserRole not found"));
        userRoleRepository.delete(userRole);

        // Check permissions - should only have viewer permissions
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ"),    // Should be false (was from custom role)
                    new PermissionCheck("SUBSCRIPTIONS", "READ") // Should be true (from viewer role)
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(false))
                .andExpect(jsonPath("$.results[1].hasPermission").value(true));
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
    @DisplayName("Should verify cache TTL and expiration behavior")
    void shouldVerifyCacheTTLBehavior() throws Exception {
        String cacheKey = "user_permissions:" + testUser.getId() + ":" + testOrganization.getId();

        // Assign role
        assignUserRole(testUser, customRole);

        // Perform permission check to populate cache
        mockMvc.perform(post("/api/organizations/{organizationId}/permissions/check", testOrganization.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new PermissionCheck("PAYMENTS", "READ")
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].hasPermission").value(true));

        // Verify cache entry exists and has proper TTL
        Boolean exists = redisTemplate.hasKey(cacheKey);
        assertTrue(exists, "Cache key should exist");

        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertTrue(ttl > 0 && ttl <= 900, "Cache TTL should be set (15 minutes = 900 seconds)");

        // Verify cache expiration clears the entry (simulate by manually expiring)
        redisTemplate.expire(cacheKey, 0, TimeUnit.SECONDS);

        // Wait a moment for expiration
        Thread.sleep(100);

        Boolean existsAfterExpiry = redisTemplate.hasKey(cacheKey);
        assertFalse(existsAfterExpiry, "Cache key should be expired and removed");
    }

    private User createUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private Role createCustomRole(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setOrganizationId(testOrganization.getId());
        role.setRoleType(RoleType.CUSTOM);
        role.setCreatedAt(Instant.now());
        role.setCreatedBy(adminUser.getId());
        role = roleRepository.save(role);

        // Assign PAYMENTS:READ and USERS:READ permissions
        List<Permission> permissions = systemPermissions.stream()
            .filter(p ->
                ("PAYMENTS".equals(p.getResource()) && "READ".equals(p.getAction())) ||
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
        private LocalDateTime expiresAt;

        public static AssignRoleRequestBuilder builder() {
            return new AssignRoleRequestBuilder();
        }

        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

        public static class AssignRoleRequestBuilder {
            private Long roleId;
            private LocalDateTime expiresAt;

            public AssignRoleRequestBuilder roleId(Long roleId) {
                this.roleId = roleId;
                return this;
            }

            public AssignRoleRequestBuilder expiresAt(LocalDateTime expiresAt) {
                this.expiresAt = expiresAt;
                return this;
            }

            public AssignRoleRequest build() {
                AssignRoleRequest request = new AssignRoleRequest();
                request.roleId = this.roleId;
                request.expiresAt = this.expiresAt;
                return request;
            }
        }
    }
}