package com.platform.user.api;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Contract tests for Role management API endpoints.
 * Tests API contracts against OpenAPI specification for role CRUD operations.
 *
 * Validates:
 * - Request/response schema compliance with rbac-api.yaml
 * - HTTP status codes and error responses
 * - Security constraints and authorization
 * - Data validation and business rules
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class RoleControllerContractTest {

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

    private User testUser;
    private Organization testOrganization;
    private List<Permission> systemPermissions;

    @BeforeEach
    void setUp() {
        // Create test organization
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setCode("TEST_ORG");
        testOrganization.setCreatedAt(Instant.now());
        testOrganization = organizationRepository.save(testOrganization);

        // Create test user with admin privileges
        testUser = new User();
        testUser.setEmail("admin@test.com");
        testUser.setFirstName("Test");
        testUser.setLastName("Admin");
        testUser.setCreatedAt(Instant.now());
        testUser = userRepository.save(testUser);

        // Load system permissions (should be populated by migration V016)
        systemPermissions = permissionRepository.findAllActive();
    }

    @Nested
    @DisplayName("GET /organizations/{organizationId}/roles - List Organization Roles")
    class ListOrganizationRoles {

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return list of organization roles with correct schema")
        void shouldReturnListOfRoles() throws Exception {
            mockMvc.perform(get("/api/organizations/{organizationId}/roles", testOrganization.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.roles").isArray())
                    .andExpect(jsonPath("$.total").isNumber())
                    .andExpect(jsonPath("$.roles[*].id").exists())
                    .andExpect(jsonPath("$.roles[*].name").exists())
                    .andExpect(jsonPath("$.roles[*].roleType").exists())
                    .andExpect(jsonPath("$.roles[*].organizationId").value(everyItem(equalTo(testOrganization.getId().intValue()))))
                    .andExpect(jsonPath("$.roles[*].isActive").value(everyItem(equalTo(true))))
                    .andExpect(jsonPath("$.roles[*].createdAt").exists());
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should include predefined roles (owner, admin, member, viewer)")
        void shouldIncludePredefinedRoles() throws Exception {
            mockMvc.perform(get("/api/organizations/{organizationId}/roles", testOrganization.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles[?(@.name == 'owner' && @.roleType == 'PREDEFINED')]").exists())
                    .andExpect(jsonPath("$.roles[?(@.name == 'admin' && @.roleType == 'PREDEFINED')]").exists())
                    .andExpect(jsonPath("$.roles[?(@.name == 'member' && @.roleType == 'PREDEFINED')]").exists())
                    .andExpect(jsonPath("$.roles[?(@.name == 'viewer' && @.roleType == 'PREDEFINED')]").exists())
                    .andExpect(jsonPath("$.total").value(greaterThanOrEqualTo(4)));
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should support includeInactive query parameter")
        void shouldSupportIncludeInactiveParameter() throws Exception {
            // Test with includeInactive=false (default)
            mockMvc.perform(get("/api/organizations/{organizationId}/roles", testOrganization.getId())
                    .param("includeInactive", "false")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles[*].isActive").value(everyItem(equalTo(true))));

            // Test with includeInactive=true
            mockMvc.perform(get("/api/organizations/{organizationId}/roles", testOrganization.getId())
                    .param("includeInactive", "true")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "member@test.com", authorities = {"ORGANIZATIONS:READ"})
        @DisplayName("Should return 403 for insufficient permissions")
        void shouldReturn403ForInsufficientPermissions() throws Exception {
            mockMvc.perform(get("/api/organizations/{organizationId}/roles", testOrganization.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("RBAC_001"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.correlationId").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return 404 for non-existent organization")
        void shouldReturn404ForNonExistentOrganization() throws Exception {
            Long nonExistentOrgId = 99999L;
            mockMvc.perform(get("/api/organizations/{organizationId}/roles", nonExistentOrgId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.correlationId").exists());
        }
    }

    @Nested
    @DisplayName("POST /organizations/{organizationId}/roles - Create Custom Role")
    class CreateCustomRole {

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should create custom role with valid request")
        void shouldCreateCustomRoleWithValidRequest() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
                CreateRoleRequest.builder()
                    .name("payment-manager")
                    .description("Manages payment processing and customer billing")
                    .permissionIds(List.of(1L, 2L, 5L)) // PAYMENTS:READ, PAYMENTS:WRITE, USERS:READ
                    .build()
            );

            mockMvc.perform(post("/api/organizations/{organizationId}/roles", testOrganization.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("payment-manager"))
                    .andExpect(jsonPath("$.description").value("Manages payment processing and customer billing"))
                    .andExpect(jsonPath("$.organizationId").value(testOrganization.getId().intValue()))
                    .andExpect(jsonPath("$.roleType").value("CUSTOM"))
                    .andExpect(jsonPath("$.isActive").value(true))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.createdBy").exists());
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return 400 for invalid role name")
        void shouldReturn400ForInvalidRoleName() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
                CreateRoleRequest.builder()
                    .name("invalid role name!") // Contains invalid characters
                    .description("Valid description")
                    .permissionIds(List.of(1L))
                    .build()
            );

            mockMvc.perform(post("/api/organizations/{organizationId}/roles", testOrganization.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return 409 for duplicate role name")
        void shouldReturn409ForDuplicateRoleName() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
                CreateRoleRequest.builder()
                    .name("owner") // Conflicts with predefined role
                    .description("Duplicate role name")
                    .permissionIds(List.of(1L))
                    .build()
            );

            mockMvc.perform(post("/api/organizations/{organizationId}/roles", testOrganization.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").value(containsString("already exists")));
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return 400 for empty permission list")
        void shouldReturn400ForEmptyPermissionList() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
                CreateRoleRequest.builder()
                    .name("test-role")
                    .description("Valid description")
                    .permissionIds(List.of()) // Empty permissions
                    .build()
            );

            mockMvc.perform(post("/api/organizations/{organizationId}/roles", testOrganization.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").value(containsString("at least one permission")));
        }
    }

    @Nested
    @DisplayName("GET /organizations/{organizationId}/roles/{roleId} - Get Role Details")
    class GetRoleDetails {

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:READ"})
        @DisplayName("Should return role details with permissions")
        void shouldReturnRoleDetailsWithPermissions() throws Exception {
            // Get owner role ID (created by migration)
            Role ownerRole = roleRepository.findByOrganizationIdAndNameAndRoleType(
                testOrganization.getId(), "owner", RoleType.PREDEFINED)
                .orElseThrow();

            mockMvc.perform(get("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), ownerRole.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(ownerRole.getId().intValue()))
                    .andExpect(jsonPath("$.name").value("owner"))
                    .andExpect(jsonPath("$.roleType").value("PREDEFINED"))
                    .andExpect(jsonPath("$.permissions").isArray())
                    .andExpect(jsonPath("$.permissions[*].resource").exists())
                    .andExpect(jsonPath("$.permissions[*].action").exists())
                    .andExpect(jsonPath("$.assignedUserCount").isNumber());
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:READ"})
        @DisplayName("Should return 404 for non-existent role")
        void shouldReturn404ForNonExistentRole() throws Exception {
            Long nonExistentRoleId = 99999L;
            mockMvc.perform(get("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), nonExistentRoleId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @WithMockUser(username = "member@test.com", authorities = {"USERS:READ"})
        @DisplayName("Should return 403 for insufficient permissions")
        void shouldReturn403ForInsufficientPermissions() throws Exception {
            Role ownerRole = roleRepository.findByOrganizationIdAndNameAndRoleType(
                testOrganization.getId(), "owner", RoleType.PREDEFINED)
                .orElseThrow();

            mockMvc.perform(get("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), ownerRole.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("RBAC_001"));
        }
    }

    @Nested
    @DisplayName("PUT /organizations/{organizationId}/roles/{roleId} - Update Role")
    class UpdateRole {

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should update custom role successfully")
        void shouldUpdateCustomRole() throws Exception {
            // First create a custom role to update
            Role customRole = createCustomRole("test-role", "Original description", List.of(1L, 2L));

            String requestBody = objectMapper.writeValueAsString(
                UpdateRoleRequest.builder()
                    .name("updated-test-role")
                    .description("Updated description for test role")
                    .permissionIds(List.of(1L, 2L, 5L)) // Add USERS:READ permission
                    .build()
            );

            mockMvc.perform(put("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), customRole.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(customRole.getId().intValue()))
                    .andExpect(jsonPath("$.name").value("updated-test-role"))
                    .andExpect(jsonPath("$.description").value("Updated description for test role"))
                    .andExpect(jsonPath("$.organizationId").value(testOrganization.getId().intValue()))
                    .andExpect(jsonPath("$.roleType").value("CUSTOM"))
                    .andExpect(jsonPath("$.isActive").value(true))
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.updatedBy").exists());
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should update role permissions successfully")
        void shouldUpdateRolePermissions() throws Exception {
            Role customRole = createCustomRole("permission-test-role", "Test description", List.of(1L)); // Only PAYMENTS:READ

            String requestBody = objectMapper.writeValueAsString(
                UpdateRoleRequest.builder()
                    .name("permission-test-role") // Keep same name
                    .description("Test description") // Keep same description
                    .permissionIds(List.of(1L, 2L, 5L, 6L)) // Add more permissions
                    .build()
            );

            mockMvc.perform(put("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), customRole.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("permission-test-role"));
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return 400 for predefined role update attempt")
        void shouldReturn400ForPredefinedRoleUpdate() throws Exception {
            Role ownerRole = roleRepository.findByOrganizationIdAndNameAndRoleType(
                testOrganization.getId(), "owner", RoleType.PREDEFINED)
                .orElseThrow();

            String requestBody = objectMapper.writeValueAsString(
                UpdateRoleRequest.builder()
                    .name("modified-owner")
                    .description("Cannot modify predefined role")
                    .permissionIds(List.of(1L))
                    .build()
            );

            mockMvc.perform(put("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), ownerRole.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").value(containsString("predefined role")));
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return 409 for name conflict with existing role")
        void shouldReturn409ForNameConflict() throws Exception {
            Role customRole = createCustomRole("test-role", "Test description", List.of(1L));

            String requestBody = objectMapper.writeValueAsString(
                UpdateRoleRequest.builder()
                    .name("admin") // Conflicts with predefined admin role
                    .description("Test description")
                    .permissionIds(List.of(1L))
                    .build()
            );

            mockMvc.perform(put("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), customRole.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").value(containsString("already exists")));
        }

        @Test
        @WithMockUser(username = "member@test.com", authorities = {"ORGANIZATIONS:READ"})
        @DisplayName("Should return 403 for insufficient permissions")
        void shouldReturn403ForInsufficientPermissions() throws Exception {
            Role customRole = createCustomRole("test-role", "Test description", List.of(1L));

            String requestBody = objectMapper.writeValueAsString(
                UpdateRoleRequest.builder()
                    .name("test-role")
                    .description("Test description")
                    .permissionIds(List.of(1L))
                    .build()
            );

            mockMvc.perform(put("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), customRole.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("RBAC_001"));
        }
    }

    @Nested
    @DisplayName("DELETE /organizations/{organizationId}/roles/{roleId} - Delete Role")
    class DeleteRole {

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should delete custom role successfully when no users assigned")
        void shouldDeleteCustomRoleWhenNoUsersAssigned() throws Exception {
            Role customRole = createCustomRole("deletable-role", "Can be deleted", List.of(1L));

            mockMvc.perform(delete("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), customRole.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return 400 when role has active user assignments")
        void shouldReturn400WhenRoleHasActiveAssignments() throws Exception {
            Role customRole = createCustomRole("assigned-role", "Has users assigned", List.of(1L));

            // Assign role to a user (this would be done through the assignment service)
            // For this contract test, we assume the role has assignments

            mockMvc.perform(delete("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), customRole.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").value(containsString("active assignments")));
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return 400 for predefined role deletion attempt")
        void shouldReturn400ForPredefinedRoleDeletion() throws Exception {
            Role ownerRole = roleRepository.findByOrganizationIdAndNameAndRoleType(
                testOrganization.getId(), "owner", RoleType.PREDEFINED)
                .orElseThrow();

            mockMvc.perform(delete("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), ownerRole.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").value(containsString("predefined role")));
        }

        @Test
        @WithMockUser(username = "admin@test.com", authorities = {"ORGANIZATIONS:ADMIN"})
        @DisplayName("Should return 404 for non-existent role")
        void shouldReturn404ForNonExistentRole() throws Exception {
            Long nonExistentRoleId = 99999L;
            mockMvc.perform(delete("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), nonExistentRoleId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @WithMockUser(username = "member@test.com", authorities = {"ORGANIZATIONS:READ"})
        @DisplayName("Should return 403 for insufficient permissions")
        void shouldReturn403ForInsufficientPermissions() throws Exception {
            Role customRole = createCustomRole("test-role", "Test description", List.of(1L));

            mockMvc.perform(delete("/api/organizations/{organizationId}/roles/{roleId}",
                    testOrganization.getId(), customRole.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("RBAC_001"));
        }
    }

    /**
     * Helper method to create a custom role for testing
     */
    private Role createCustomRole(String name, String description, List<Long> permissionIds) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setOrganizationId(testOrganization.getId());
        role.setRoleType(RoleType.CUSTOM);
        role.setActive(true);
        role.setCreatedAt(Instant.now());
        role.setCreatedBy(testUser.getId());
        return roleRepository.save(role);
    }

    /**
     * Data Transfer Objects for request payloads
     */
    private static class CreateRoleRequest {
        private String name;
        private String description;
        private List<Long> permissionIds;

        public static CreateRoleRequestBuilder builder() {
            return new CreateRoleRequestBuilder();
        }

        // Standard getters, setters, and builder implementation
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Long> getPermissionIds() { return permissionIds; }
        public void setPermissionIds(List<Long> permissionIds) { this.permissionIds = permissionIds; }

        public static class CreateRoleRequestBuilder {
            private String name;
            private String description;
            private List<Long> permissionIds;

            public CreateRoleRequestBuilder name(String name) {
                this.name = name;
                return this;
            }

            public CreateRoleRequestBuilder description(String description) {
                this.description = description;
                return this;
            }

            public CreateRoleRequestBuilder permissionIds(List<Long> permissionIds) {
                this.permissionIds = permissionIds;
                return this;
            }

            public CreateRoleRequest build() {
                CreateRoleRequest request = new CreateRoleRequest();
                request.name = this.name;
                request.description = this.description;
                request.permissionIds = this.permissionIds;
                return request;
            }
        }
    }

    private static class UpdateRoleRequest {
        private String name;
        private String description;
        private List<Long> permissionIds;

        public static UpdateRoleRequestBuilder builder() {
            return new UpdateRoleRequestBuilder();
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Long> getPermissionIds() { return permissionIds; }
        public void setPermissionIds(List<Long> permissionIds) { this.permissionIds = permissionIds; }

        public static class UpdateRoleRequestBuilder {
            private String name;
            private String description;
            private List<Long> permissionIds;

            public UpdateRoleRequestBuilder name(String name) {
                this.name = name;
                return this;
            }

            public UpdateRoleRequestBuilder description(String description) {
                this.description = description;
                return this;
            }

            public UpdateRoleRequestBuilder permissionIds(List<Long> permissionIds) {
                this.permissionIds = permissionIds;
                return this;
            }

            public UpdateRoleRequest build() {
                UpdateRoleRequest request = new UpdateRoleRequest();
                request.name = this.name;
                request.description = this.description;
                request.permissionIds = this.permissionIds;
                return request;
            }
        }
    }
}