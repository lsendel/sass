package com.platform.user.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.audit.internal.AuditService;
import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.shared.security.TenantContext;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.OrganizationSettings;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;
import com.platform.user.internal.UserRole;

/**
 * Enhanced integration tests for user management functionality.
 * Tests organization management, role-based access, user invitations, and compliance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class EnhancedUserManagementIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditService auditService;

    private UUID orgId;
    private UUID adminUserId;
    private UUID regularUserId;
    private UUID managerUserId;

    @BeforeEach
    void setUp() {
        // Clear existing data
        auditEventRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create test organization
        Organization org = new Organization("Enhanced Test Corp", "enhanced-test", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create users with different roles
        User adminUser = new User("admin@enhanced.com", "Admin User");
        adminUser.setOrganization(org);
        adminUser.setRole(UserRole.ADMIN);
        adminUser = userRepository.save(adminUser);
        adminUserId = adminUser.getId();

        User regularUser = new User("user@enhanced.com", "Regular User");
        regularUser.setOrganization(org);
        regularUser.setRole(UserRole.USER);
        regularUser = userRepository.save(regularUser);
        regularUserId = regularUser.getId();

        User managerUser = new User("manager@enhanced.com", "Manager User");
        managerUser.setOrganization(org);
        managerUser.setRole(UserRole.MANAGER);
        managerUser = userRepository.save(managerUser);
        managerUserId = managerUser.getId();
    }

    @Test
    void shouldManageOrganizationMembersWithRoleHierarchy() throws Exception {
        // Admin can manage all users
        authenticateAs(adminUserId, orgId, "enhanced-test", "ADMIN");

        // Admin creates new user
        String newUserRequest = """
            {
                "email": "newuser@enhanced.com",
                "name": "New User",
                "role": "USER",
                "department": "Engineering",
                "title": "Software Developer"
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/users", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(newUserRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@enhanced.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var userResponse = objectMapper.readTree(responseJson);
        String newUserId = userResponse.get("id").asText();

        // Admin can promote user to manager
        String promotionRequest = """
            {
                "role": "MANAGER",
                "department": "Engineering"
            }
            """;

        mockMvc.perform(put("/api/v1/organizations/{orgId}/users/{userId}/role", orgId, newUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(promotionRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        // Manager cannot promote user to admin (role hierarchy enforcement)
        authenticateAs(managerUserId, orgId, "enhanced-test", "MANAGER");

        String invalidPromotionRequest = """
            {
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(put("/api/v1/organizations/{orgId}/users/{userId}/role", orgId, newUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPromotionRequest))
                .andExpect(status().isForbidden());

        // Regular user cannot manage other users
        authenticateAs(regularUserId, orgId, "enhanced-test", "USER");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/users", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldHandleUserInvitationWorkflow() throws Exception {
        authenticateAs(adminUserId, orgId, "enhanced-test", "ADMIN");

        // Step 1: Send invitation
        String invitationRequest = """
            {
                "email": "invite@enhanced.com",
                "role": "USER",
                "department": "Marketing",
                "customMessage": "Welcome to our team!"
            }
            """;

        var inviteResult = mockMvc.perform(post("/api/v1/organizations/{orgId}/users/invite", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invitationRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("invite@enhanced.com"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invitationToken").exists())
                .andReturn();

        String inviteJson = inviteResult.getResponse().getContentAsString();
        var inviteResponse = objectMapper.readTree(inviteJson);
        String invitationToken = inviteResponse.get("invitationToken").asText();

        // Step 2: List pending invitations
        mockMvc.perform(get("/api/v1/organizations/{orgId}/invitations", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("invite@enhanced.com"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        // Step 3: Accept invitation (simulate invitee)
        String acceptanceRequest = """
            {
                "name": "Invited User",
                "password": "SecurePassword123!",
                "acceptedTerms": true
            }
            """;

        mockMvc.perform(post("/api/v1/invitations/{token}/accept", invitationToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(acceptanceRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("invite@enhanced.com"))
                .andExpect(jsonPath("$.name").value("Invited User"));

        // Step 4: Verify user was created
        var invitedUser = userRepository.findByEmailAndDeletedAtIsNull("invite@enhanced.com");
        assertTrue(invitedUser.isPresent());
        assertEquals(orgId, invitedUser.get().getOrganization().getId());
        assertEquals(UserRole.USER, invitedUser.get().getRole());

        // Step 5: Verify audit trail
        Thread.sleep(1000);

        var auditEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, PageRequest.of(0, 10));

        var eventActions = auditEvents.getContent().stream()
            .map(e -> e.getAction())
            .toList();

        assertTrue(eventActions.stream().anyMatch(action -> action.contains("INVITATION")));
        assertTrue(eventActions.stream().anyMatch(action -> action.contains("USER_CREATED")));
    }

    @Test
    void shouldManageOrganizationSettings() throws Exception {
        authenticateAs(adminUserId, orgId, "enhanced-test", "ADMIN");

        // Update organization settings
        String settingsRequest = """
            {
                "companySize": "MEDIUM",
                "industry": "TECHNOLOGY",
                "timeZone": "America/New_York",
                "locale": "en_US",
                "dateFormat": "MM/dd/yyyy",
                "features": {
                    "sso": true,
                    "audit": true,
                    "api": false
                },
                "billing": {
                    "address": {
                        "street": "123 Business St",
                        "city": "New York",
                        "state": "NY",
                        "postalCode": "10001",
                        "country": "US"
                    },
                    "taxId": "12-3456789"
                }
            }
            """;

        mockMvc.perform(put("/api/v1/organizations/{orgId}/settings", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(settingsRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companySize").value("MEDIUM"))
                .andExpect(jsonPath("$.industry").value("TECHNOLOGY"))
                .andExpect(jsonPath("$.features.sso").value(true));

        // Regular user cannot modify settings
        authenticateAs(regularUserId, orgId, "enhanced-test", "USER");

        mockMvc.perform(put("/api/v1/organizations/{orgId}/settings", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(settingsRequest))
                .andExpect(status().isForbidden());

        // But can view settings
        mockMvc.perform(get("/api/v1/organizations/{orgId}/settings", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companySize").value("MEDIUM"));
    }

    @Test
    void shouldEnforcePasswordPolicies() throws Exception {
        authenticateAs(regularUserId, orgId, "enhanced-test", "USER");

        // Test weak password rejection
        String weakPasswordRequest = """
            {
                "currentPassword": "currentPass123!",
                "newPassword": "weak",
                "confirmPassword": "weak"
            }
            """;

        mockMvc.perform(post("/api/v1/users/me/password", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(weakPasswordRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password")));

        // Test password reuse prevention
        String reusedPasswordRequest = """
            {
                "currentPassword": "currentPass123!",
                "newPassword": "previouslyUsedPass123!",
                "confirmPassword": "previouslyUsedPass123!"
            }
            """;

        mockMvc.perform(post("/api/v1/users/me/password", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reusedPasswordRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("recently")));

        // Test strong password acceptance
        String strongPasswordRequest = """
            {
                "currentPassword": "currentPass123!",
                "newPassword": "NewSecurePassword456!",
                "confirmPassword": "NewSecurePassword456!"
            }
            """;

        mockMvc.perform(post("/api/v1/users/me/password", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(strongPasswordRequest))
                .andExpect(status().isNoContent());

        // Verify password change audit
        Thread.sleep(1000);

        var auditEvents = auditEventRepository.findByActorIdOrderByCreatedAtDesc(regularUserId);
        assertTrue(auditEvents.stream()
            .anyMatch(e -> "PASSWORD_CHANGED".equals(e.getAction())));
    }

    @Test
    void shouldHandleUserDeactivationAndDataRetention() throws Exception {
        authenticateAs(adminUserId, orgId, "enhanced-test", "ADMIN");

        // Create user with data
        User userToDeactivate = new User("deactivate@enhanced.com", "User To Deactivate");
        userToDeactivate.setOrganization(organizationRepository.findById(orgId).orElseThrow());
        userToDeactivate = userRepository.save(userToDeactivate);

        // Create some audit data for the user
        TenantContext.setTenantInfo(orgId, "enhanced-test", userToDeactivate.getId());
        auditService.logUserLogin(userToDeactivate.getId(), "192.168.1.100", "Chrome", true)
            .get(5, TimeUnit.SECONDS);

        // Deactivate user
        String deactivationRequest = """
            {
                "reason": "employee_departure",
                "retainData": true,
                "transferDataTo": null
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/users/{userId}/deactivate",
                orgId, userToDeactivate.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(deactivationRequest))
                .andExpect(status().isNoContent());

        // Verify user is deactivated
        var deactivatedUser = userRepository.findById(userToDeactivate.getId());
        assertTrue(deactivatedUser.isPresent());
        assertFalse(deactivatedUser.get().isActive());
        assertNotNull(deactivatedUser.get().getDeactivatedAt());

        // Verify user cannot log in
        authenticateAs(userToDeactivate.getId(), orgId, "enhanced-test", "USER");

        mockMvc.perform(get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Test data transfer option
        User transferUser = new User("transfer@enhanced.com", "Transfer User");
        transferUser.setOrganization(organizationRepository.findById(orgId).orElseThrow());
        transferUser = userRepository.save(transferUser);

        User secondUserToDeactivate = new User("deactivate2@enhanced.com", "User To Deactivate 2");
        secondUserToDeactivate.setOrganization(organizationRepository.findById(orgId).orElseThrow());
        secondUserToDeactivate = userRepository.save(secondUserToDeactivate);

        String transferDeactivationRequest = String.format("""
            {
                "reason": "role_change",
                "retainData": false,
                "transferDataTo": "%s"
            }
            """, transferUser.getId());

        authenticateAs(adminUserId, orgId, "enhanced-test", "ADMIN");

        mockMvc.perform(post("/api/v1/organizations/{orgId}/users/{userId}/deactivate",
                orgId, secondUserToDeactivate.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferDeactivationRequest))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldImplementGDPRDataSubjectRights() throws Exception {
        authenticateAs(regularUserId, orgId, "enhanced-test", "USER");

        // Request data export (Right to portability)
        mockMvc.perform(post("/api/v1/users/me/export", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.estimatedCompletion").exists());

        // Check export status
        mockMvc.perform(get("/api/v1/users/me/export/status", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        // Request data deletion (Right to erasure)
        String deletionRequest = """
            {
                "reason": "GDPR_REQUEST",
                "confirmDeletion": true,
                "acknowledgeConsequences": true
            }
            """;

        mockMvc.perform(post("/api/v1/users/me/delete", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(deletionRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists());

        // View processing activities (Right to transparency)
        mockMvc.perform(get("/api/v1/users/me/processing-activities", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activities").isArray());

        // Update consent preferences
        String consentRequest = """
            {
                "marketing": false,
                "analytics": true,
                "essential": true
            }
            """;

        mockMvc.perform(put("/api/v1/users/me/consent", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(consentRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketing").value(false))
                .andExpect(jsonPath("$.analytics").value(true));
    }

    @Test
    void shouldManageUserSessionsAndSecurity() throws Exception {
        authenticateAs(regularUserId, orgId, "enhanced-test", "USER");

        // View active sessions
        mockMvc.perform(get("/api/v1/users/me/sessions", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions").isArray());

        // Enable two-factor authentication
        String twoFactorRequest = """
            {
                "method": "TOTP",
                "phoneNumber": "+1234567890"
            }
            """;

        mockMvc.perform(post("/api/v1/users/me/two-factor/enable", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(twoFactorRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCode").exists())
                .andExpect(jsonPath("$.backupCodes").isArray());

        // Verify two-factor setup
        String verificationRequest = """
            {
                "code": "123456"
            }
            """;

        mockMvc.perform(post("/api/v1/users/me/two-factor/verify", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(verificationRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        // View security events
        mockMvc.perform(get("/api/v1/users/me/security-events", orgId)
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Revoke sessions (except current)
        mockMvc.perform(post("/api/v1/users/me/sessions/revoke-all", orgId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revokedSessions").exists());
    }

    @Test
    void shouldEnforceDataClassificationAndAccess() throws Exception {
        authenticateAs(adminUserId, orgId, "enhanced-test", "ADMIN");

        // Create user with specific clearance level
        String sensitiveUserRequest = """
            {
                "email": "sensitive@enhanced.com",
                "name": "Sensitive User",
                "role": "USER",
                "clearanceLevel": "CONFIDENTIAL",
                "department": "Security"
            }
            """;

        var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/users", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sensitiveUserRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        var userResponse = objectMapper.readTree(responseJson);
        String sensitiveUserId = userResponse.get("id").asText();

        // Regular user cannot access sensitive user data
        authenticateAs(regularUserId, orgId, "enhanced-test", "USER");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/users/{userId}", orgId, sensitiveUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Admin can access but action is audited
        authenticateAs(adminUserId, orgId, "enhanced-test", "ADMIN");

        mockMvc.perform(get("/api/v1/organizations/{orgId}/users/{userId}", orgId, sensitiveUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clearanceLevel").value("CONFIDENTIAL"));

        // Verify sensitive data access is audited
        Thread.sleep(1000);

        var auditEvents = auditEventRepository.findByResourceTypeAndResourceId("USER", sensitiveUserId);
        assertTrue(auditEvents.stream()
            .anyMatch(e -> "SENSITIVE_DATA_ACCESS".equals(e.getAction()) ||
                          "USER_VIEW".equals(e.getAction())));
    }

    @Test
    void shouldHandleBulkUserOperations() throws Exception {
        authenticateAs(adminUserId, orgId, "enhanced-test", "ADMIN");

        // Create multiple users for bulk operations
        for (int i = 0; i < 5; i++) {
            User bulkUser = new User("bulk" + i + "@enhanced.com", "Bulk User " + i);
            bulkUser.setOrganization(organizationRepository.findById(orgId).orElseThrow());
            bulkUser.setRole(UserRole.USER);
            userRepository.save(bulkUser);
        }

        // Bulk role update
        String bulkRoleRequest = """
            {
                "action": "UPDATE_ROLE",
                "userIds": ["user1", "user2", "user3"],
                "targetRole": "MANAGER",
                "reason": "promotion_cycle"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/users/bulk", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkRoleRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").exists())
                .andExpect(jsonPath("$.failedCount").exists());

        // Bulk deactivation
        String bulkDeactivationRequest = """
            {
                "action": "DEACTIVATE",
                "userIds": ["user4", "user5"],
                "reason": "organizational_restructure"
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/users/bulk", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkDeactivationRequest))
                .andExpect(status().isOk());

        // Bulk export for compliance
        String bulkExportRequest = """
            {
                "action": "EXPORT_DATA",
                "userIds": ["user1", "user2"],
                "format": "JSON",
                "includeAuditTrail": true
            }
            """;

        mockMvc.perform(post("/api/v1/organizations/{orgId}/users/bulk", orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkExportRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists());

        // Check bulk operation status
        mockMvc.perform(get("/api/v1/organizations/{orgId}/users/bulk-operations", orgId)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
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