package com.platform.user.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.user.internal.*;

/**
 * Integration test for complete user role assignment workflows.
 * Tests the end-to-end scenarios from quickstart.md including:
 * - Role assignment to users
 * - Permission cache invalidation
 * - Effective permissions calculation
 * - Multi-role assignment and limits
 *
 * Based on Scenario 2 from quickstart.md:
 * "Assign Role to Organization User"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class UserRoleAssignmentIntegrationTest {

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
    private UserOrganizationRoleRepository userOrganizationRoleRepository;

    private User adminUser;
    private User targetUser;
    private Organization testOrganization;
    private Role paymentManagerRole;
    private Role memberRole;

    @BeforeEach
    void setUp() {
        // Create test organization
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setCode("TEST_ORG");
        testOrganization.setCreatedAt(Instant.now());
        testOrganization = organizationRepository.save(testOrganization);

        // Create admin user
        adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setCreatedAt(Instant.now());
        adminUser = userRepository.save(adminUser);

        // Create target user for role assignments
        targetUser = new User();
        targetUser.setEmail("employee@test.com");
        targetUser.setFirstName("Employee");
        targetUser.setLastName("User");
        targetUser.setCreatedAt(Instant.now());
        targetUser = userRepository.save(targetUser);

        // Create a custom payment-manager role for testing
        paymentManagerRole = new Role();
        paymentManagerRole.setName("payment-manager");
        paymentManagerRole.setDescription("Manages payment processing and customer billing");
        paymentManagerRole.setOrganizationId(testOrganization.getId());
        paymentManagerRole.setRoleType(RoleType.CUSTOM);
        paymentManagerRole.setActive(true);
        paymentManagerRole.setCreatedAt(Instant.now());
        paymentManagerRole.setCreatedBy(adminUser.getId());
        paymentManagerRole = roleRepository.save(paymentManagerRole);

        // Get predefined member role
        memberRole = roleRepository.findByOrganizationIdAndNameAndRoleType(
            testOrganization.getId(), "member", RoleType.PREDEFINED)
            .orElseThrow(() -> new IllegalStateException("Member role should be created by migration"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"USERS:ADMIN"})
    @DisplayName("Scenario 2: Assign Role to User - Complete Integration Flow")
    void shouldAssignPaymentManagerRoleCompleteFlow() throws Exception {
        // Step 1: Verify user has no roles initially
        mockMvc.perform(get("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments").isArray())
                .andExpect(jsonPath("$.assignments").isEmpty())
                .andExpect(jsonPath("$.effectivePermissions").isArray())
                .andExpect(jsonPath("$.effectivePermissions").isEmpty());

        // Step 2: Assign payment-manager role to user (from quickstart scenario)
        String assignRoleRequest = objectMapper.writeValueAsString(
            AssignRoleRequest.builder()
                .roleId(paymentManagerRole.getId())
                .expiresAt(null) // No expiration
                .build()
        );

        MvcResult assignResult = mockMvc.perform(post("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignRoleRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUser.getId().intValue()))
                .andExpect(jsonPath("$.organizationId").value(testOrganization.getId().intValue()))
                .andExpect(jsonPath("$.roleId").value(paymentManagerRole.getId().intValue()))
                .andExpect(jsonPath("$.role.name").value("payment-manager"))
                .andExpect(jsonPath("$.assignedAt").exists())
                .andExpect(jsonPath("$.assignedBy").value(adminUser.getId().intValue()))
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        JsonNode assignResponse = objectMapper.readTree(assignResult.getResponse().getContentAsString());
        Long assignmentId = assignResponse.get("id").asLong();

        // Step 3: Verify assignment in database
        Optional<UserOrganizationRole> assignment = userOrganizationRoleRepository.findById(assignmentId);
        assertTrue(assignment.isPresent(), "Role assignment should be persisted");

        UserOrganizationRole roleAssignment = assignment.get();
        assertEquals(targetUser.getId(), roleAssignment.getUserId());
        assertEquals(testOrganization.getId(), roleAssignment.getOrganizationId());
        assertEquals(paymentManagerRole.getId(), roleAssignment.getRoleId());
        assertEquals(adminUser.getId(), roleAssignment.getAssignedBy());
        assertTrue(roleAssignment.isActive());
        assertNull(roleAssignment.getExpiresAt());

        // Step 4: Verify user permissions endpoint shows assigned role and permissions
        MvcResult userRolesResult = mockMvc.perform(get("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments").isArray())
                .andExpect(jsonPath("$.assignments", hasSize(1)))
                .andExpect(jsonPath("$.assignments[0].userId").value(targetUser.getId().intValue()))
                .andExpect(jsonPath("$.assignments[0].role.name").value("payment-manager"))
                .andExpect(jsonPath("$.assignments[0].isActive").value(true))
                .andExpect(jsonPath("$.effectivePermissions").isArray())
                .andReturn();

        // Verify effective permissions are calculated correctly
        JsonNode userRolesResponse = objectMapper.readTree(userRolesResult.getResponse().getContentAsString());
        JsonNode effectivePermissions = userRolesResponse.get("effectivePermissions");
        assertTrue(effectivePermissions.size() > 0, "User should have effective permissions from assigned role");

        // Step 5: Test permission checking for the assigned user
        String permissionCheckRequest = objectMapper.writeValueAsString(
            PermissionCheckRequest.builder()
                .organizationId(testOrganization.getId())
                .checks(List.of(
                    PermissionCheck.builder()
                        .resource("PAYMENTS")
                        .action("READ")
                        .build(),
                    PermissionCheck.builder()
                        .resource("PAYMENTS")
                        .action("WRITE")
                        .build(),
                    PermissionCheck.builder()
                        .resource("ORGANIZATIONS")
                        .action("ADMIN") // Should be denied
                        .build()
                ))
                .build()
        );

        // Test as the assigned user
        mockMvc.perform(post("/api/auth/permissions/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(permissionCheckRequest)
                .with(user("employee@test.com").authorities("PAYMENTS:READ", "PAYMENTS:WRITE", "USERS:READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.results[0].resource").value("PAYMENTS"))
                .andExpect(jsonPath("$.results[0].action").value("READ"))
                .andExpect(jsonPath("$.results[0].granted").value(true))
                .andExpect(jsonPath("$.results[1].resource").value("PAYMENTS"))
                .andExpect(jsonPath("$.results[1].action").value("WRITE"))
                .andExpect(jsonPath("$.results[1].granted").value(true))
                .andExpect(jsonPath("$.results[2].resource").value("ORGANIZATIONS"))
                .andExpect(jsonPath("$.results[2].action").value("ADMIN"))
                .andExpect(jsonPath("$.results[2].granted").value(false));

        // Step 6: Test assigning additional role (member role)
        String assignMemberRequest = objectMapper.writeValueAsString(
            AssignRoleRequest.builder()
                .roleId(memberRole.getId())
                .expiresAt(null)
                .build()
        );

        mockMvc.perform(post("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignMemberRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role.name").value("member"));

        // Step 7: Verify user now has multiple roles
        mockMvc.perform(get("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments", hasSize(2)))
                .andExpect(jsonPath("$.assignments[?(@.role.name == 'payment-manager')]").exists())
                .andExpect(jsonPath("$.assignments[?(@.role.name == 'member')]").exists());

        // Step 8: Verify audit trail
        List<UserOrganizationRole> allAssignments = userOrganizationRoleRepository
            .findByUserIdAndOrganizationIdAndIsActive(targetUser.getId(), testOrganization.getId(), true);
        assertEquals(2, allAssignments.size(), "User should have 2 active role assignments");

        // Verify assignment metadata
        for (UserOrganizationRole assignment_item : allAssignments) {
            assertEquals(targetUser.getId(), assignment_item.getUserId());
            assertEquals(testOrganization.getId(), assignment_item.getOrganizationId());
            assertEquals(adminUser.getId(), assignment_item.getAssignedBy());
            assertNotNull(assignment_item.getAssignedAt());
            assertTrue(assignment_item.isActive());
        }
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"USERS:ADMIN"})
    @DisplayName("Should handle role assignment with expiration correctly")
    void shouldHandleRoleAssignmentWithExpiration() throws Exception {
        Instant expirationDate = Instant.now().plusSeconds(86400); // 24 hours from now

        String assignRoleRequest = objectMapper.writeValueAsString(
            AssignRoleRequest.builder()
                .roleId(paymentManagerRole.getId())
                .expiresAt(expirationDate)
                .build()
        );

        mockMvc.perform(post("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignRoleRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.isActive").value(true));

        // Verify expiration date in database
        UserOrganizationRole assignment = userOrganizationRoleRepository
            .findByUserIdAndOrganizationIdAndRoleId(targetUser.getId(), testOrganization.getId(), paymentManagerRole.getId())
            .orElseThrow();

        assertNotNull(assignment.getExpiresAt());
        assertEquals(expirationDate.getEpochSecond(), assignment.getExpiresAt().getEpochSecond());
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"USERS:ADMIN"})
    @DisplayName("Should prevent duplicate role assignments")
    void shouldPreventDuplicateRoleAssignments() throws Exception {
        // First assignment - should succeed
        String assignRoleRequest = objectMapper.writeValueAsString(
            AssignRoleRequest.builder()
                .roleId(paymentManagerRole.getId())
                .expiresAt(null)
                .build()
        );

        mockMvc.perform(post("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignRoleRequest))
                .andExpect(status().isCreated());

        // Second assignment of same role - should fail
        mockMvc.perform(post("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignRoleRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").value(containsString("already assigned")));

        // Verify only one assignment exists
        List<UserOrganizationRole> assignments = userOrganizationRoleRepository
            .findByUserIdAndOrganizationIdAndIsActive(targetUser.getId(), testOrganization.getId(), true);
        assertEquals(1, assignments.size(), "Should have only one role assignment");
    }

    @Test
    @WithMockUser(username = "admin@test.com", authorities = {"USERS:ADMIN"})
    @DisplayName("Should handle role removal correctly")
    void shouldHandleRoleRemovalCorrectly() throws Exception {
        // First assign a role
        assignRoleToUser(targetUser, paymentManagerRole);

        // Verify role is assigned
        mockMvc.perform(get("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments", hasSize(1)));

        // Remove the role
        mockMvc.perform(delete("/api/organizations/{organizationId}/users/{userId}/roles/{roleId}",
                testOrganization.getId(), targetUser.getId(), paymentManagerRole.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify role is no longer active
        mockMvc.perform(get("/api/organizations/{organizationId}/users/{userId}/roles",
                testOrganization.getId(), targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments").isEmpty())
                .andExpect(jsonPath("$.effectivePermissions").isEmpty());

        // Verify assignment still exists in database but is inactive (for audit)
        UserOrganizationRole assignment = userOrganizationRoleRepository
            .findByUserIdAndOrganizationIdAndRoleId(targetUser.getId(), testOrganization.getId(), paymentManagerRole.getId())
            .orElseThrow();
        assertFalse(assignment.isActive(), "Assignment should be marked as inactive for audit trail");
    }

    /**
     * Helper method to assign a role to a user directly in database
     */
    private UserOrganizationRole assignRoleToUser(User user, Role role) {
        UserOrganizationRole assignment = new UserOrganizationRole();
        assignment.setUserId(user.getId());
        assignment.setOrganizationId(testOrganization.getId());
        assignment.setRoleId(role.getId());
        assignment.setAssignedAt(Instant.now());
        assignment.setAssignedBy(adminUser.getId());
        assignment.setActive(true);
        return userOrganizationRoleRepository.save(assignment);
    }

    /**
     * Data Transfer Objects for requests
     */
    private static class AssignRoleRequest {
        private Long roleId;
        private Instant expiresAt;

        public static AssignRoleRequestBuilder builder() {
            return new AssignRoleRequestBuilder();
        }

        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public Instant getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

        public static class AssignRoleRequestBuilder {
            private Long roleId;
            private Instant expiresAt;

            public AssignRoleRequestBuilder roleId(Long roleId) {
                this.roleId = roleId;
                return this;
            }

            public AssignRoleRequestBuilder expiresAt(Instant expiresAt) {
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

    private static class PermissionCheckRequest {
        private Long organizationId;
        private List<PermissionCheck> checks;

        public static PermissionCheckRequestBuilder builder() {
            return new PermissionCheckRequestBuilder();
        }

        public Long getOrganizationId() { return organizationId; }
        public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
        public List<PermissionCheck> getChecks() { return checks; }
        public void setChecks(List<PermissionCheck> checks) { this.checks = checks; }

        public static class PermissionCheckRequestBuilder {
            private Long organizationId;
            private List<PermissionCheck> checks;

            public PermissionCheckRequestBuilder organizationId(Long organizationId) {
                this.organizationId = organizationId;
                return this;
            }

            public PermissionCheckRequestBuilder checks(List<PermissionCheck> checks) {
                this.checks = checks;
                return this;
            }

            public PermissionCheckRequest build() {
                PermissionCheckRequest request = new PermissionCheckRequest();
                request.organizationId = this.organizationId;
                request.checks = this.checks;
                return request;
            }
        }
    }

    private static class PermissionCheck {
        private String resource;
        private String action;

        public static PermissionCheckBuilder builder() {
            return new PermissionCheckBuilder();
        }

        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public static class PermissionCheckBuilder {
            private String resource;
            private String action;

            public PermissionCheckBuilder resource(String resource) {
                this.resource = resource;
                return this;
            }

            public PermissionCheckBuilder action(String action) {
                this.action = action;
                return this;
            }

            public PermissionCheck build() {
                PermissionCheck check = new PermissionCheck();
                check.resource = this.resource;
                check.action = this.action;
                return check;
            }
        }
    }
}