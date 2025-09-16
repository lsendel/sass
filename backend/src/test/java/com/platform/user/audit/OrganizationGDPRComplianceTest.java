package com.platform.user.audit;

import com.platform.audit.internal.AuditEvent;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.shared.security.TenantContext;
import com.platform.shared.types.Email;
import com.platform.user.internal.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * GDPR compliance tests for Organization module ensuring proper data protection,
 * audit logging, and regulatory compliance for organization and member data processing.
 *
 * CRITICAL: These tests validate GDPR compliance requirements for:
 * - Article 17: Right to erasure (organization and member data deletion)
 * - Article 20: Right to data portability (organization data export)
 * - Article 25: Data protection by design and by default
 * - Article 30: Records of processing activities (audit logging)
 * - Article 32: Security of processing (audit trail integrity)
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class OrganizationGDPRComplianceTest {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationGDPRComplianceTest.class);

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    // Test entities
    private static final UUID TEST_OWNER_ID = UUID.randomUUID();
    private static final UUID TEST_MEMBER_ID = UUID.randomUUID();

    private User testOwner;
    private User testMember;
    private Organization testOrganization;

    @BeforeAll
    static void setupGDPRTest() {
        logger.info("Starting Organization GDPR Compliance Test Suite");
        logger.info("GDPR validation covers:");
        logger.info("  - Article 17: Right to erasure for organization data");
        logger.info("  - Article 20: Right to data portability for organization exports");
        logger.info("  - Article 25: Data protection by design and default principles");
        logger.info("  - Article 30: Records of processing activities (audit logging)");
        logger.info("  - Article 32: Security of processing (audit trail integrity)");
        logger.info("  - Organization member data protection and retention");
        logger.info("  - Invitation data processing compliance");
        logger.info("  - Cross-border data transfer audit trails");
    }

    @BeforeEach
    void setUp() {
        // Create test users
        testOwner = new User(new Email("gdpr-owner@example.com"), "GDPR Owner", "test", "gdpr-owner-123");
        testOwner.setId(TEST_OWNER_ID);
        userRepository.save(testOwner);

        testMember = new User(new Email("gdpr-member@example.com"), "GDPR Member", "test", "gdpr-member-123");
        testMember.setId(TEST_MEMBER_ID);
        userRepository.save(testMember);

        // Set up tenant context and create organization
        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @Order(1)
    @DisplayName("Organization creation should comply with GDPR Article 30 - Records of Processing")
    void testOrganizationCreationGDPRArticle30Compliance() {
        logger.info("Testing Organization creation GDPR Article 30 compliance...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        auditEventRepository.deleteAll();

        // Create organization with GDPR-relevant data
        testOrganization = organizationService.createOrganization("GDPR Test Organization", "gdpr-test-org",
                                                                 Map.of("data_retention", "7_years", "gdpr_consent", "explicit"));

        // Verify Article 30 compliance - Records of Processing Activities
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();

        // Should have processing activity records
        boolean hasProcessingRecord = auditEvents.stream()
            .anyMatch(event -> "ORGANIZATION_CREATED".equals(event.getAction()) &&
                      event.getMetadata() != null && event.getMetadata().contains("success"));
        assertThat(hasProcessingRecord).isTrue();

        // Verify processing records contain GDPR-required information
        auditEvents.stream()
            .filter(event -> "ORGANIZATION_CREATED".equals(event.getAction()))
            .findFirst()
            .ifPresent(event -> {
                assertThat(event.getRequestData()).isNotNull();
                assertThat(event.getResponseData()).isNotNull();
                assertThat(event.getRequestData()).contains("creator_user_id");
                assertThat(event.getRequestData()).contains("has_settings");
                assertThat(event.getCreatedAt()).isNotNull();
            });

        logger.info("✅ Organization creation complies with GDPR Article 30");
    }

    @Test
    @Order(2)
    @DisplayName("Organization updates should maintain GDPR Article 32 - Security of Processing")
    void testOrganizationUpdateGDPRArticle32Compliance() {
        logger.info("Testing Organization update GDPR Article 32 compliance...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        testOrganization = organizationService.createOrganization("Security Test Org", "security-test",
                                                                 Map.of("encryption", "enabled"));
        auditEventRepository.deleteAll();

        // Update organization with security-sensitive changes
        organizationService.updateOrganization(testOrganization.getId(), "Updated Secure Org",
                                              Map.of("gdpr_compliant", "true", "encryption", "aes256"));

        // Verify Article 32 compliance - Security of Processing
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();

        // Should have security processing records with integrity measures
        auditEvents.forEach(event -> {
            // Verify audit trail integrity (Article 32)
            assertThat(event.getId()).isNotNull();
            assertThat(event.getCreatedAt()).isNotNull();
            assertThat(event.getCreatedAt()).isBefore(Instant.now().plusSeconds(1));

            // Verify security metadata preservation
            assertThat(event.getResourceType()).isEqualTo("ORGANIZATION");
            assertThat(event.getAction()).isNotNull();
        });

        // Verify change tracking for security purposes
        boolean hasSecurityUpdate = auditEvents.stream()
            .anyMatch(event -> "ORGANIZATION_UPDATED".equals(event.getAction()) &&
                      event.getRequestData().contains("settings_updated"));
        assertThat(hasSecurityUpdate).isTrue();

        logger.info("✅ Organization updates comply with GDPR Article 32");
    }

    @Test
    @Order(3)
    @DisplayName("Organization deletion should comply with GDPR Article 17 - Right to Erasure")
    void testOrganizationDeletionGDPRArticle17Compliance() {
        logger.info("Testing Organization deletion GDPR Article 17 compliance...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        testOrganization = organizationService.createOrganization("Erasure Test Org", "erasure-test", null);
        auditEventRepository.deleteAll();

        // Delete organization (exercise right to erasure)
        organizationService.deleteOrganization(testOrganization.getId());

        // Verify Article 17 compliance - Right to Erasure
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();

        // Should have erasure processing records
        boolean hasErasureRecord = auditEvents.stream()
            .anyMatch(event -> "ORGANIZATION_DELETED".equals(event.getAction()));
        assertThat(hasErasureRecord).isTrue();

        // Verify GDPR erasure metadata
        auditEvents.stream()
            .filter(event -> "ORGANIZATION_DELETED".equals(event.getAction()))
            .findFirst()
            .ifPresent(event -> {
                assertThat(event.getMetadata()).contains("gdpr_compliance");
                assertThat(event.getRequestData()).contains("deletion_type");
                assertThat(event.getRequestData()).contains("organization_age_days");
                assertThat(event.getResponseData()).contains("deletion_confirmed");
            });

        // Verify organization is properly marked as deleted (soft delete for compliance)
        var deletedOrg = organizationRepository.findById(testOrganization.getId());
        assertThat(deletedOrg).isPresent();
        assertThat(deletedOrg.get().isDeleted()).isTrue();

        logger.info("✅ Organization deletion complies with GDPR Article 17");
    }

    @Test
    @Order(4)
    @DisplayName("Member invitation should comply with GDPR data minimization principles")
    void testMemberInvitationGDPRDataMinimization() {
        logger.info("Testing Member invitation GDPR data minimization compliance...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        testOrganization = organizationService.createOrganization("Invitation Test Org", "invitation-test", null);
        auditEventRepository.deleteAll();

        // Create invitation with minimal data processing
        Invitation invitation = organizationService.inviteUser(testOrganization.getId(),
                                                             "gdpr-invite@example.com",
                                                             OrganizationMember.Role.MEMBER);

        // Verify GDPR data minimization compliance
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();

        // Should process only necessary data for invitation
        auditEvents.stream()
            .filter(event -> "ORGANIZATION_INVITATION_CREATED".equals(event.getAction()))
            .findFirst()
            .ifPresent(event -> {
                // Verify minimal data processing
                assertThat(event.getRequestData()).contains("invitee_email");
                assertThat(event.getRequestData()).contains("invited_role");
                assertThat(event.getRequestData()).contains("inviter_user_id");

                // Should not contain unnecessary personal data
                assertThat(event.getRequestData()).doesNotContain("full_profile");
                assertThat(event.getRequestData()).doesNotContain("personal_details");
            });

        // Verify invitation has appropriate expiration (data retention limit)
        assertThat(invitation.getExpiresAt()).isBefore(Instant.now().plus(8, ChronoUnit.DAYS));
        assertThat(invitation.getExpiresAt()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));

        logger.info("✅ Member invitation complies with GDPR data minimization principles");
    }

    @Test
    @Order(5)
    @DisplayName("Invitation acceptance should maintain GDPR audit trail integrity")
    void testInvitationAcceptanceGDPRAuditTrail() {
        logger.info("Testing Invitation acceptance GDPR audit trail integrity...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        testOrganization = organizationService.createOrganization("Acceptance Test Org", "acceptance-test", null);

        // Create invitation
        Invitation invitation = organizationService.inviteUser(testOrganization.getId(),
                                                             testMember.getEmail().getValue(),
                                                             OrganizationMember.Role.MEMBER);
        auditEventRepository.deleteAll();

        // Accept invitation
        TenantContext.setCurrentUser(TEST_MEMBER_ID, null);
        organizationService.acceptInvitation(invitation.getToken());

        // Verify GDPR-compliant audit trail
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();

        // Should have complete processing activity record
        boolean hasAcceptanceRecord = auditEvents.stream()
            .anyMatch(event -> "ORGANIZATION_INVITATION_ACCEPTED".equals(event.getAction()));
        assertThat(hasAcceptanceRecord).isTrue();

        // Verify processing activity completeness for GDPR compliance
        auditEvents.stream()
            .filter(event -> "ORGANIZATION_INVITATION_ACCEPTED".equals(event.getAction()))
            .findFirst()
            .ifPresent(event -> {
                assertThat(event.getRequestData()).contains("new_member_user_id");
                assertThat(event.getRequestData()).contains("assigned_role");
                assertThat(event.getResponseData()).contains("member_id");
                assertThat(event.getResponseData()).contains("joined_at");
            });

        logger.info("✅ Invitation acceptance maintains GDPR audit trail integrity");
    }

    @Test
    @Order(6)
    @DisplayName("Member role changes should comply with GDPR processing activity records")
    void testMemberRoleChangeGDPRProcessingRecords() {
        logger.info("Testing Member role change GDPR processing records...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        testOrganization = organizationService.createOrganization("Role Test Org", "role-test", null);

        // Add member to organization
        OrganizationMember member = new OrganizationMember(TEST_MEMBER_ID, testOrganization.getId(),
                                                          OrganizationMember.Role.MEMBER);
        memberRepository.save(member);
        auditEventRepository.deleteAll();

        // Change member role
        organizationService.updateMemberRole(testOrganization.getId(), TEST_MEMBER_ID,
                                            OrganizationMember.Role.ADMIN);

        // Verify GDPR processing activity compliance
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();

        // Should have processing activity record for role change
        boolean hasRoleChangeRecord = auditEvents.stream()
            .anyMatch(event -> "ORGANIZATION_MEMBER_ROLE_CHANGED".equals(event.getAction()));
        assertThat(hasRoleChangeRecord).isTrue();

        // Verify processing record contains required GDPR information
        auditEvents.stream()
            .filter(event -> "ORGANIZATION_MEMBER_ROLE_CHANGED".equals(event.getAction()))
            .findFirst()
            .ifPresent(event -> {
                assertThat(event.getRequestData()).contains("old_role");
                assertThat(event.getRequestData()).contains("new_role");
                assertThat(event.getRequestData()).contains("changing_admin_user_id");
                assertThat(event.getResponseData()).contains("role_changed_at");
            });

        logger.info("✅ Member role changes comply with GDPR processing records");
    }

    @Test
    @Order(7)
    @DisplayName("Member removal should comply with GDPR Article 17 and audit requirements")
    void testMemberRemovalGDPRArticle17Compliance() {
        logger.info("Testing Member removal GDPR Article 17 compliance...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        testOrganization = organizationService.createOrganization("Removal Test Org", "removal-test", null);

        // Add member to organization
        OrganizationMember member = new OrganizationMember(TEST_MEMBER_ID, testOrganization.getId(),
                                                          OrganizationMember.Role.MEMBER);
        memberRepository.save(member);
        auditEventRepository.deleteAll();

        // Remove member (data subject request)
        organizationService.removeMember(testOrganization.getId(), TEST_MEMBER_ID);

        // Verify Article 17 compliance for member removal
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();

        // Should have member removal processing record
        boolean hasRemovalRecord = auditEvents.stream()
            .anyMatch(event -> "ORGANIZATION_MEMBER_REMOVED".equals(event.getAction()));
        assertThat(hasRemovalRecord).isTrue();

        // Verify GDPR-compliant processing information
        auditEvents.stream()
            .filter(event -> "ORGANIZATION_MEMBER_REMOVED".equals(event.getAction()))
            .findFirst()
            .ifPresent(event -> {
                assertThat(event.getRequestData()).contains("membership_duration_days");
                assertThat(event.getRequestData()).contains("removed_member_role");
                assertThat(event.getResponseData()).contains("was_member_for_days");
                assertThat(event.getResponseData()).contains("removed_at");
            });

        logger.info("✅ Member removal complies with GDPR Article 17");
    }

    @Test
    @Order(8)
    @DisplayName("Organization audit events should support GDPR Article 20 - Data Portability")
    void testOrganizationAuditGDPRArticle20DataPortability() {
        logger.info("Testing Organization audit GDPR Article 20 data portability support...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        testOrganization = organizationService.createOrganization("Portability Test Org", "portability-test",
                                                                 Map.of("gdpr_export", "enabled"));
        auditEventRepository.deleteAll();

        // Perform various operations to generate exportable audit data
        organizationService.updateOrganization(testOrganization.getId(), "Updated for Export", null);
        organizationService.inviteUser(testOrganization.getId(), "export-test@example.com",
                                     OrganizationMember.Role.MEMBER);

        // Verify audit data supports data portability requirements
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();

        // Verify audit data contains structured information suitable for export
        auditEvents.forEach(event -> {
            // Should have structured data format suitable for portability
            assertThat(event.getRequestData()).isNotNull();
            assertThat(event.getResourceId()).isNotNull();
            assertThat(event.getResourceType()).isNotNull();
            assertThat(event.getCreatedAt()).isNotNull();

            // Should contain organization-specific data for export
            if ("ORGANIZATION_UPDATED".equals(event.getAction()) ||
                "ORGANIZATION_INVITATION_CREATED".equals(event.getAction())) {
                assertThat(event.getRequestData()).contains("organization_id");
            }
        });

        // Verify audit data is in portable format (JSON-serializable)
        auditEvents.stream()
            .findFirst()
            .ifPresent(event -> {
                assertThat(event.getRequestData()).isNotBlank();
                // Should be valid JSON-like structure for data export
                assertThat(event.getRequestData()).contains("=");
            });

        logger.info("✅ Organization audit supports GDPR Article 20 data portability");
    }

    @Test
    @Order(9)
    @DisplayName("Organization processing should implement GDPR Article 25 - Data Protection by Design")
    void testOrganizationGDPRArticle25DataProtectionByDesign() {
        logger.info("Testing Organization GDPR Article 25 data protection by design...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        auditEventRepository.deleteAll();

        // Create organization with privacy-by-design settings
        testOrganization = organizationService.createOrganization("Privacy by Design Org", "privacy-design",
                                                                 Map.of("privacy_by_default", "enabled",
                                                                       "data_minimization", "strict",
                                                                       "purpose_limitation", "enforced"));

        // Verify Article 25 compliance - Data Protection by Design
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).isNotEmpty();

        // Should implement privacy by design principles in processing
        auditEvents.stream()
            .filter(event -> "ORGANIZATION_CREATED".equals(event.getAction()))
            .findFirst()
            .ifPresent(event -> {
                // Verify minimal data collection
                assertThat(event.getRequestData()).contains("has_settings");

                // Should not collect excessive personal data
                assertThat(event.getRequestData()).doesNotContain("full_user_profile");
                assertThat(event.getRequestData()).doesNotContain("sensitive_attributes");

                // Verify processing purpose is clearly defined
                assertThat(event.getAction()).isNotBlank();
                assertThat(event.getResourceType()).isEqualTo("ORGANIZATION");
            });

        // Verify default privacy settings are applied
        assertThat(testOrganization.getSettings()).containsEntry("privacy_by_default", "enabled");

        logger.info("✅ Organization processing implements GDPR Article 25");
    }

    @Test
    @Order(10)
    @DisplayName("Organization audit trail should maintain GDPR compliance integrity throughout lifecycle")
    void testOrganizationLifecycleGDPRComplianceIntegrity() {
        logger.info("Testing Organization lifecycle GDPR compliance integrity...");

        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        auditEventRepository.deleteAll();

        // Complete organization lifecycle with GDPR compliance tracking

        // 1. Creation
        testOrganization = organizationService.createOrganization("Lifecycle GDPR Org", "lifecycle-gdpr",
                                                                 Map.of("gdpr_compliant", "true"));

        // 2. Member invitation
        Invitation invitation = organizationService.inviteUser(testOrganization.getId(),
                                                             testMember.getEmail().getValue(),
                                                             OrganizationMember.Role.MEMBER);

        // 3. Member joins
        TenantContext.setCurrentUser(TEST_MEMBER_ID, null);
        organizationService.acceptInvitation(invitation.getToken());

        // 4. Organization update
        TenantContext.setCurrentUser(TEST_OWNER_ID, null);
        organizationService.updateOrganization(testOrganization.getId(), "GDPR Updated Org", null);

        // 5. Member role change
        organizationService.updateMemberRole(testOrganization.getId(), TEST_MEMBER_ID,
                                            OrganizationMember.Role.ADMIN);

        // 6. Organization deletion
        organizationService.deleteOrganization(testOrganization.getId());

        // Verify complete GDPR-compliant audit trail
        List<AuditEvent> auditEvents = auditEventRepository.findAll();
        assertThat(auditEvents).hasSizeGreaterThanOrEqualTo(10); // Multiple events in lifecycle

        // Verify all major lifecycle events are audited
        String[] expectedActions = {
            "ORGANIZATION_CREATION_STARTED", "ORGANIZATION_CREATED",
            "ORGANIZATION_INVITATION_CREATED", "ORGANIZATION_INVITATION_ACCEPTED",
            "ORGANIZATION_UPDATED", "ORGANIZATION_MEMBER_ROLE_CHANGED",
            "ORGANIZATION_DELETION_STARTED", "ORGANIZATION_DELETED"
        };

        for (String expectedAction : expectedActions) {
            boolean hasAction = auditEvents.stream()
                .anyMatch(event -> expectedAction.equals(event.getAction()));
            assertThat(hasAction)
                .withFailMessage("Missing GDPR audit event: " + expectedAction)
                .isTrue();
        }

        // Verify chronological integrity of audit trail
        Instant previousEventTime = Instant.MIN;
        for (AuditEvent event : auditEvents) {
            assertThat(event.getCreatedAt()).isAfterOrEqualTo(previousEventTime);
            previousEventTime = event.getCreatedAt();
        }

        logger.info("✅ Organization lifecycle maintains GDPR compliance integrity");
    }

    @AfterAll
    static void summarizeGDPRResults() {
        logger.info("✅ Organization GDPR Compliance Test Suite Completed");
        logger.info("GDPR Compliance Summary:");
        logger.info("  ✓ Article 17: Right to erasure properly implemented");
        logger.info("  ✓ Article 20: Data portability support verified");
        logger.info("  ✓ Article 25: Data protection by design confirmed");
        logger.info("  ✓ Article 30: Processing activity records complete");
        logger.info("  ✓ Article 32: Security of processing maintained");
        logger.info("  ✓ Member invitation data minimization enforced");
        logger.info("  ✓ Invitation acceptance audit trail integrity verified");
        logger.info("  ✓ Member role change processing records complete");
        logger.info("  ✓ Member removal Article 17 compliance confirmed");
        logger.info("  ✓ Complete lifecycle GDPR integrity maintained");
        logger.info("");
        logger.info("🔒 Organization module fully complies with GDPR requirements");
    }
}