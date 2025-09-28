package com.platform.auth.api;

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
 * Contract tests for Permission checking and catalog API endpoints.
 * Tests API contracts against OpenAPI specification for permission operations.
 *
 * Validates:
 * - Permission catalog endpoints and system permissions
 * - Permission checking batch operations
 * - Multi-resource permission validation
 * - Authorization context and organization isolation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PermissionControllerContractTest {

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
    private UserOrganizationRoleRepository userOrganizationRoleRepository;

    private User testUser;
    private Organization testOrganization;
    private Role memberRole;
    private List<Permission> systemPermissions;

    @BeforeEach
    void setUp() {
        // Create test organization
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setCode("TEST_ORG");
        testOrganization.setCreatedAt(Instant.now());
        testOrganization = organizationRepository.save(testOrganization);

        // Create test user
        testUser = new User();
        testUser.setEmail("user@test.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCreatedAt(Instant.now());
        testUser = userRepository.save(testUser);

        // Get member role and assign to user
        memberRole = roleRepository.findByOrganizationIdAndNameAndRoleType(
            testOrganization.getId(), "member", RoleType.PREDEFINED)
            .orElseThrow();

        // Assign member role to test user
        UserOrganizationRole assignment = new UserOrganizationRole();
        assignment.setUserId(testUser.getId());
        assignment.setOrganizationId(testOrganization.getId());
        assignment.setRoleId(memberRole.getId());
        assignment.setAssignedAt(Instant.now());
        assignment.setActive(true);
        userOrganizationRoleRepository.save(assignment);

        // Load system permissions
        systemPermissions = permissionRepository.findAllActive();
    }

    @Nested
    @DisplayName("GET /permissions - List System Permissions")
    class ListSystemPermissions {

        @Test
        @WithMockUser(username = "user@test.com", authorities = {"USERS:READ"})
        @DisplayName("Should return all system permissions with correct schema")
        void shouldReturnAllSystemPermissions() throws Exception {
            mockMvc.perform(get("/api/permissions")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.permissions").isArray())
                    .andExpect(jsonPath("$.permissions", hasSize(20))) // 5 resources × 4 actions = 20 permissions
                    .andExpect(jsonPath("$.permissions[*].id").exists())
                    .andExpect(jsonPath("$.permissions[*].resource").exists())
                    .andExpect(jsonPath("$.permissions[*].action").exists())
                    .andExpect(jsonPath("$.permissions[*].description").exists())
                    .andExpect(jsonPath("$.permissions[*].isActive").value(everyItem(equalTo(true))))
                    .andExpect(jsonPath("$.permissionsByResource").exists())
                    .andExpect(jsonPath("$.permissionsByResource.PAYMENTS").isArray())
                    .andExpect(jsonPath("$.permissionsByResource.ORGANIZATIONS").isArray())
                    .andExpect(jsonPath("$.permissionsByResource.USERS").isArray())
                    .andExpect(jsonPath("$.permissionsByResource.SUBSCRIPTIONS").isArray())
                    .andExpect(jsonPath("$.permissionsByResource.AUDIT").isArray());
        }

        @Test
        @WithMockUser(username = "user@test.com", authorities = {"USERS:READ"})
        @DisplayName("Should include all expected resource and action combinations")
        void shouldIncludeAllExpectedCombinations() throws Exception {
            mockMvc.perform(get("/api/permissions")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    // Verify specific permission combinations exist
                    .andExpect(jsonPath("$.permissions[?(@.resource == 'PAYMENTS' && @.action == 'READ')]").exists())
                    .andExpect(jsonPath("$.permissions[?(@.resource == 'PAYMENTS' && @.action == 'WRITE')]").exists())
                    .andExpect(jsonPath("$.permissions[?(@.resource == 'PAYMENTS' && @.action == 'DELETE')]").exists())
                    .andExpect(jsonPath("$.permissions[?(@.resource == 'PAYMENTS' && @.action == 'ADMIN')]").exists())
                    .andExpect(jsonPath("$.permissions[?(@.resource == 'ORGANIZATIONS' && @.action == 'ADMIN')]").exists())
                    .andExpect(jsonPath("$.permissions[?(@.resource == 'USERS' && @.action == 'READ')]").exists())
                    .andExpect(jsonPath("$.permissions[?(@.resource == 'SUBSCRIPTIONS' && @.action == 'WRITE')]").exists())
                    .andExpect(jsonPath("$.permissions[?(@.resource == 'AUDIT' && @.action == 'READ')]").exists());
        }

        @Test
        @WithMockUser(username = "user@test.com", authorities = {"USERS:READ"})
        @DisplayName("Should group permissions by resource correctly")
        void shouldGroupPermissionsByResource() throws Exception {
            mockMvc.perform(get("/api/permissions")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.permissionsByResource.PAYMENTS", hasSize(4))) // READ, WRITE, DELETE, ADMIN
                    .andExpect(jsonPath("$.permissionsByResource.ORGANIZATIONS", hasSize(4)))
                    .andExpect(jsonPath("$.permissionsByResource.USERS", hasSize(4)))
                    .andExpect(jsonPath("$.permissionsByResource.SUBSCRIPTIONS", hasSize(4)))
                    .andExpect(jsonPath("$.permissionsByResource.AUDIT", hasSize(4)));
        }

        @Test
        @DisplayName("Should return 403 for unauthenticated requests")
        void shouldReturn403ForUnauthenticatedRequests() throws Exception {
            mockMvc.perform(get("/api/permissions")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /auth/permissions/check - Check User Permissions")
    class CheckUserPermissions {

        @Test
        @WithMockUser(username = "user@test.com", authorities = {"PAYMENTS:READ", "USERS:READ"})
        @DisplayName("Should check single permission successfully")
        void shouldCheckSinglePermission() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
                PermissionCheckRequest.builder()
                    .organizationId(testOrganization.getId())
                    .checks(List.of(
                        PermissionCheck.builder()
                            .resource("PAYMENTS")
                            .action("READ")
                            .build()
                    ))
                    .build()
            );

            mockMvc.perform(post("/api/auth/permissions/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.results").isArray())
                    .andExpect(jsonPath("$.results", hasSize(1)))
                    .andExpect(jsonPath("$.results[0].resource").value("PAYMENTS"))
                    .andExpect(jsonPath("$.results[0].action").value("READ"))
                    .andExpect(jsonPath("$.results[0].granted").value(true))
                    .andExpect(jsonPath("$.results[0].reason").doesNotExist());
        }

        @Test
        @WithMockUser(username = "user@test.com", authorities = {"PAYMENTS:READ"})
        @DisplayName("Should check multiple permissions in batch")
        void shouldCheckMultiplePermissions() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
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
                            .action("ADMIN")
                            .build()
                    ))
                    .build()
            );

            mockMvc.perform(post("/api/auth/permissions/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
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
                    .andExpect(jsonPath("$.results[2].granted").value(false))
                    .andExpect(jsonPath("$.results[2].reason").value("Insufficient permissions"));
        }

        @Test
        @WithMockUser(username = "user@test.com", authorities = {"PAYMENTS:READ"})
        @DisplayName("Should support resource-specific permission checks")
        void shouldSupportResourceSpecificChecks() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
                PermissionCheckRequest.builder()
                    .organizationId(testOrganization.getId())
                    .checks(List.of(
                        PermissionCheck.builder()
                            .resource("PAYMENTS")
                            .action("READ")
                            .resourceId(123L) // Specific payment ID
                            .build()
                    ))
                    .build()
            );

            mockMvc.perform(post("/api/auth/permissions/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[0].resource").value("PAYMENTS"))
                    .andExpect(jsonPath("$.results[0].action").value("READ"))
                    .andExpect(jsonPath("$.results[0].resourceId").value(123))
                    .andExpect(jsonPath("$.results[0].granted").isBoolean());
        }

        @Test
        @WithMockUser(username = "user@test.com", authorities = {"PAYMENTS:READ"})
        @DisplayName("Should return 400 for invalid permission check request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
                PermissionCheckRequest.builder()
                    .organizationId(testOrganization.getId())
                    .checks(List.of(
                        PermissionCheck.builder()
                            .resource("INVALID_RESOURCE")
                            .action("READ")
                            .build()
                    ))
                    .build()
            );

            mockMvc.perform(post("/api/auth/permissions/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").value(containsString("Invalid resource")));
        }

        @Test
        @WithMockUser(username = "user@test.com", authorities = {"PAYMENTS:READ"})
        @DisplayName("Should return 400 for empty permission check list")
        void shouldReturn400ForEmptyCheckList() throws Exception {
            String requestBody = objectMapper.writeValueAsString(
                PermissionCheckRequest.builder()
                    .organizationId(testOrganization.getId())
                    .checks(List.of()) // Empty list
                    .build()
            );

            mockMvc.perform(post("/api/auth/permissions/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").value(containsString("at least one permission check")));
        }

        @Test
        @WithMockUser(username = "user@test.com", authorities = {"PAYMENTS:READ"})
        @DisplayName("Should handle organization context isolation")
        void shouldHandleOrganizationContextIsolation() throws Exception {
            // Create another organization
            Organization otherOrganization = new Organization();
            otherOrganization.setName("Other Organization");
            otherOrganization.setCode("OTHER_ORG");
            otherOrganization.setCreatedAt(Instant.now());
            otherOrganization = organizationRepository.save(otherOrganization);

            String requestBody = objectMapper.writeValueAsString(
                PermissionCheckRequest.builder()
                    .organizationId(otherOrganization.getId()) // Different organization
                    .checks(List.of(
                        PermissionCheck.builder()
                            .resource("PAYMENTS")
                            .action("READ")
                            .build()
                    ))
                    .build()
            );

            mockMvc.perform(post("/api/auth/permissions/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[0].granted").value(false))
                    .andExpect(jsonPath("$.results[0].reason").value(containsString("not a member")));
        }
    }

    /**
     * Data Transfer Objects for request payloads
     */
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
        private Long resourceId;

        public static PermissionCheckBuilder builder() {
            return new PermissionCheckBuilder();
        }

        public String getResource() { return resource; }
        public void setResource(String resource) { this.resource = resource; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public Long getResourceId() { return resourceId; }
        public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

        public static class PermissionCheckBuilder {
            private String resource;
            private String action;
            private Long resourceId;

            public PermissionCheckBuilder resource(String resource) {
                this.resource = resource;
                return this;
            }

            public PermissionCheckBuilder action(String action) {
                this.action = action;
                return this;
            }

            public PermissionCheckBuilder resourceId(Long resourceId) {
                this.resourceId = resourceId;
                return this;
            }

            public PermissionCheck build() {
                PermissionCheck check = new PermissionCheck();
                check.resource = this.resource;
                check.action = this.action;
                check.resourceId = this.resourceId;
                return check;
            }
        }
    }
}