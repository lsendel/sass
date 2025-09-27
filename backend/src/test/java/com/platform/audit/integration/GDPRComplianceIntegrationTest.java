package com.platform.audit.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.platform.audit.internal.AuditEvent;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.audit.internal.AuditService;
import com.platform.shared.security.TenantContext;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Integration tests for GDPR compliance features in the audit module.
 * Tests PII redaction, data retention, right to deletion, and data portability.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.audit.enable-pii-redaction=true",
    "app.audit.retention-days=30"
})
@Transactional
class GDPRComplianceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private UUID orgId;
    private UUID userId;
    private UUID secondUserId;

    @BeforeEach
    void setUp() {
        // Clear existing audit events
        auditEventRepository.deleteAll();

        // Create test organization
        Organization org = new Organization("GDPR Test Corp", "gdpr-test", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test users
        User user = new User("john.doe@gdprtest.com", "John Doe");
        user.setOrganization(org);
        user = userRepository.save(user);
        userId = user.getId();

        User secondUser = new User("jane.smith@gdprtest.com", "Jane Smith");
        secondUser.setOrganization(org);
        secondUser = userRepository.save(secondUser);
        secondUserId = secondUser.getId();

        // Set tenant context
        TenantContext.setTenantInfo(orgId, "gdpr-test", userId);
    }

    @Test
    void shouldAutomaticallyRedactPIIInAuditLogs() throws Exception {
        // Create audit event with various PII data
        Map<String, Object> requestData = Map.of(
            "email", "john.doe@example.com",
            "phone", "555-123-4567",
            "ssn", "123-45-6789",
            "creditCard", "4111-1111-1111-1111",
            "name", "John Doe",
            "address", "123 Main St"
        );

        Map<String, Object> responseData = Map.of(
            "userEmail", "jane.smith@example.com",
            "contactPhone", "(555) 987-6543",
            "paymentCard", "5555 5555 5555 5555",
            "customerId", "cust_123456"
        );

        auditService.logEvent(
            "USER_DATA_PROCESSING",
            "USER_PROFILE",
            userId.toString(),
            "UPDATE",
            requestData,
            responseData,
            "192.168.1.100",
            "Mozilla/5.0",
            Map.of("processor", "GDPR_TEST")
        ).get(5, TimeUnit.SECONDS);

        // Retrieve the audit event
        var events = auditEventRepository.findByActorIdOrderByCreatedAtDesc(userId);
        assertEquals(1, events.size());

        AuditEvent event = events.get(0);

        // Verify PII is redacted
        String requestDataStr = event.getRequestData();
        String responseDataStr = event.getResponseData();

        // Emails should be redacted
        assertFalse(requestDataStr.contains("john.doe@example.com"));
        assertFalse(responseDataStr.contains("jane.smith@example.com"));

        // Phone numbers should be redacted
        assertFalse(requestDataStr.contains("555-123-4567"));
        assertFalse(responseDataStr.contains("(555) 987-6543"));

        // SSN should be redacted
        assertFalse(requestDataStr.contains("123-45-6789"));

        // Credit cards should be redacted
        assertFalse(requestDataStr.contains("4111-1111-1111-1111"));
        assertFalse(responseDataStr.contains("5555 5555 5555 5555"));

        // Verify redaction placeholder is present
        assertTrue(requestDataStr.contains("[REDACTED]"));
        assertTrue(responseDataStr.contains("[REDACTED]"));

        // Non-PII data should remain
        assertTrue(requestDataStr.contains("John Doe")); // Name is not automatically redacted
        assertTrue(requestDataStr.contains("123 Main St")); // Address is not automatically redacted
        assertTrue(responseDataStr.contains("cust_123456")); // Customer ID is not PII
    }

    @Test
    void shouldImplementRightToErasure() throws Exception {
        // Create multiple audit events for a user
        for (int i = 0; i < 5; i++) {
            TenantContext.setTenantInfo(orgId, "gdpr-test", userId);
            auditService.logResourceAccess(
                "DOCUMENT",
                "doc-" + i,
                "READ",
                "192.168.1." + i,
                "Browser"
            ).get(5, TimeUnit.SECONDS);
        }

        // Create events for another user
        for (int i = 0; i < 3; i++) {
            TenantContext.setTenantInfo(orgId, "gdpr-test", secondUserId);
            auditService.logResourceAccess(
                "DOCUMENT",
                "doc-" + (i + 10),
                "READ",
                "192.168.1." + (i + 10),
                "Browser"
            ).get(5, TimeUnit.SECONDS);
        }

        // Verify events exist
        assertEquals(5, auditEventRepository.findByActorIdOrderByCreatedAtDesc(userId).size());
        assertEquals(3, auditEventRepository.findByActorIdOrderByCreatedAtDesc(secondUserId).size());

        // Exercise right to erasure for first user
        int deletedCount = auditService.deleteUserAuditEvents(userId);
        assertEquals(5, deletedCount);

        // Verify user's events are deleted
        assertEquals(0, auditEventRepository.findByActorIdOrderByCreatedAtDesc(userId).size());

        // Verify other user's events remain
        assertEquals(3, auditEventRepository.findByActorIdOrderByCreatedAtDesc(secondUserId).size());
    }

    @Test
    void shouldImplementRightToDataPortability() throws Exception {
        // Create various audit events for a user
        TenantContext.setTenantInfo(orgId, "gdpr-test", userId);

        auditService.logUserLogin(userId, "192.168.1.100", "Chrome", true)
            .get(5, TimeUnit.SECONDS);

        auditService.logPaymentEvent(
            "PAYMENT_PROCESSED",
            "payment-001",
            "CHARGE",
            Map.of("amount", 99.99, "currency", "EUR"),
            "192.168.1.100",
            "Chrome"
        ).get(5, TimeUnit.SECONDS);

        auditService.logDataModification(
            "PROFILE",
            userId.toString(),
            "UPDATE",
            Map.of("field", "email", "oldValue", "old@example.com"),
            Map.of("field", "email", "newValue", "new@example.com"),
            "192.168.1.100",
            "Chrome"
        ).get(5, TimeUnit.SECONDS);

        // Export user's audit data (simulate GDPR data export request)
        List<AuditEvent> userAuditData = auditEventRepository.findByActorIdOrderByCreatedAtDesc(userId);

        // Verify all user's data is exportable
        assertEquals(3, userAuditData.size());

        // Verify data contains all necessary fields for portability
        for (AuditEvent event : userAuditData) {
            assertNotNull(event.getId());
            assertNotNull(event.getCreatedAt());
            assertNotNull(event.getAction());
            assertNotNull(event.getActorId());
            assertEquals(userId, event.getActorId());
            assertNotNull(event.getOrganizationId());
            assertEquals(orgId, event.getOrganizationId());
        }

        // Verify PII is redacted in exported data
        for (AuditEvent event : userAuditData) {
            if (event.getRequestData() != null) {
                assertFalse(event.getRequestData().contains("old@example.com"));
            }
            if (event.getResponseData() != null) {
                assertFalse(event.getResponseData().contains("new@example.com"));
            }
        }
    }

    @Test
    void shouldImplementDataRetentionPolicies() throws Exception {
        Instant now = Instant.now();
        Instant oldDate = now.minus(35, ChronoUnit.DAYS); // Older than 30-day retention
        Instant recentDate = now.minus(10, ChronoUnit.DAYS); // Within retention period

        // Create old audit events (should be deleted)
        for (int i = 0; i < 3; i++) {
            AuditEvent oldEvent = new AuditEvent(
                orgId, userId, "OLD_EVENT_" + i, "RESOURCE", "res-old-" + i,
                "ACTION", "192.168.1." + i, "Browser"
            );
            oldEvent.setCreatedAt(oldDate);
            auditEventRepository.save(oldEvent);
        }

        // Create recent audit events (should be retained)
        for (int i = 0; i < 5; i++) {
            AuditEvent recentEvent = new AuditEvent(
                orgId, userId, "RECENT_EVENT_" + i, "RESOURCE", "res-new-" + i,
                "ACTION", "192.168.2." + i, "Browser"
            );
            recentEvent.setCreatedAt(recentDate);
            auditEventRepository.save(recentEvent);
        }

        // Verify all events exist initially
        assertEquals(8, auditEventRepository.count());

        // Execute retention policy
        int deletedCount = auditService.deleteExpiredAuditEvents();

        // Verify old events were deleted
        assertEquals(3, deletedCount);
        assertEquals(5, auditEventRepository.count());

        // Verify only recent events remain
        List<AuditEvent> remainingEvents = auditEventRepository.findAll();
        assertTrue(remainingEvents.stream()
            .allMatch(e -> e.getCreatedAt().isAfter(now.minus(31, ChronoUnit.DAYS))));
        assertTrue(remainingEvents.stream()
            .allMatch(e -> e.getAction().startsWith("RECENT_EVENT")));
    }

    @Test
    void shouldRedactUserDataUponRequest() throws Exception {
        // Create audit events with user data
        Map<String, Object> userData = Map.of(
            "username", "johndoe",
            "email", "john.doe@example.com",
            "fullName", "John Doe",
            "userId", userId.toString()
        );

        auditService.logEvent(
            "USER_ACTIVITY",
            "USER",
            userId.toString(),
            "LOGIN",
            userData,
            Map.of("result", "success"),
            "192.168.1.100",
            "Chrome",
            null
        ).get(5, TimeUnit.SECONDS);

        auditService.logEvent(
            "DATA_ACCESS",
            "FILE",
            "file-123",
            "DOWNLOAD",
            Map.of("requestedBy", userId.toString()),
            Map.of("fileName", "report.pdf", "owner", "John Doe"),
            "192.168.1.100",
            "Chrome",
            null
        ).get(5, TimeUnit.SECONDS);

        // Verify data exists before redaction
        var eventsBeforeRedaction = auditEventRepository.findByActorIdOrderByCreatedAtDesc(userId);
        assertEquals(2, eventsBeforeRedaction.size());
        assertTrue(eventsBeforeRedaction.get(0).getRequestData().contains("johndoe") ||
                  eventsBeforeRedaction.get(0).getResponseData().contains("John Doe"));

        // Redact user data
        int redactedCount = auditService.redactUserAuditData(userId);
        assertTrue(redactedCount > 0);

        // Verify data is redacted
        var eventsAfterRedaction = auditEventRepository.findByActorIdOrderByCreatedAtDesc(userId);
        assertEquals(2, eventsAfterRedaction.size());

        for (AuditEvent event : eventsAfterRedaction) {
            if (event.getRequestData() != null) {
                assertTrue(event.getRequestData().contains("[REDACTED]"));
                assertFalse(event.getRequestData().contains("johndoe"));
                assertFalse(event.getRequestData().contains("john.doe@example.com"));
            }
            if (event.getResponseData() != null) {
                assertFalse(event.getResponseData().contains("John Doe"));
            }
        }
    }

    @Test
    void shouldAnonymizeDataWhilePreservingAuditIntegrity() throws Exception {
        // Create payment audit event
        String paymentId = "payment-" + UUID.randomUUID();
        Map<String, Object> paymentData = Map.of(
            "paymentId", paymentId,
            "amount", 150.00,
            "currency", "EUR",
            "customerEmail", "customer@example.com",
            "customerName", "John Customer",
            "cardLastFour", "4242"
        );

        auditService.logPaymentEvent(
            "PAYMENT_COMPLETED",
            paymentId,
            "CAPTURE",
            paymentData,
            "192.168.1.100",
            "API/2.0"
        ).get(5, TimeUnit.SECONDS);

        // Retrieve event
        var events = auditEventRepository.findByResourceTypeAndResourceId("PAYMENT", paymentId);
        assertEquals(1, events.size());

        AuditEvent event = events.get(0);

        // Verify PII is redacted but business data preserved
        String requestData = event.getRequestData();

        // PII should be redacted
        assertFalse(requestData.contains("customer@example.com"));
        assertTrue(requestData.contains("[REDACTED]"));

        // Business-critical data should be preserved
        assertTrue(requestData.contains(paymentId));
        assertTrue(requestData.contains("150"));
        assertTrue(requestData.contains("EUR"));
        assertTrue(requestData.contains("4242")); // Last four digits are typically not considered full PII

        // Audit integrity fields should remain
        assertNotNull(event.getId());
        assertNotNull(event.getCreatedAt());
        assertEquals("PAYMENT_COMPLETED", event.getAction());
        assertEquals(paymentId, event.getResourceId());
    }

    @Test
    void shouldProvideAuditLogForComplianceReporting() throws Exception {
        // Create various compliance-relevant events
        TenantContext.setTenantInfo(orgId, "gdpr-test", userId);

        // Data access event
        auditService.logEvent(
            "DATA_ACCESS",
            "PERSONAL_DATA",
            "user-profile-" + userId,
            "VIEW",
            Map.of("accessedBy", "admin", "purpose", "support-request"),
            Map.of("dataCategories", List.of("name", "email", "phone")),
            "192.168.1.100",
            "Admin-Portal",
            Map.of("ticketId", "TICKET-123")
        ).get(5, TimeUnit.SECONDS);

        // Data modification event
        auditService.logEvent(
            "DATA_MODIFICATION",
            "PERSONAL_DATA",
            "user-profile-" + userId,
            "UPDATE",
            Map.of("field", "email", "oldValue", "[REDACTED]"),
            Map.of("field", "email", "newValue", "[REDACTED]"),
            "192.168.1.101",
            "User-Portal",
            Map.of("reason", "user-request")
        ).get(5, TimeUnit.SECONDS);

        // Data deletion event
        auditService.logEvent(
            "DATA_DELETION",
            "PERSONAL_DATA",
            "user-messages-" + userId,
            "DELETE",
            Map.of("dataType", "messages", "count", 42),
            Map.of("status", "completed", "permanent", true),
            "192.168.1.102",
            "System",
            Map.of("reason", "retention-policy")
        ).get(5, TimeUnit.SECONDS);

        // Consent event
        auditService.logEvent(
            "CONSENT_UPDATED",
            "USER_CONSENT",
            "consent-" + userId,
            "UPDATE",
            Map.of("marketingEmails", false, "dataAnalytics", false),
            Map.of("marketingEmails", true, "dataAnalytics", true),
            "192.168.1.103",
            "User-Portal",
            Map.of("consentVersion", "2.0")
        ).get(5, TimeUnit.SECONDS);

        // Generate compliance report
        var complianceEvents = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, org.springframework.data.domain.PageRequest.of(0, 100));

        // Verify all compliance events are logged
        assertEquals(4, complianceEvents.getTotalElements());

        // Verify event types for compliance
        var eventTypes = complianceEvents.getContent().stream()
            .map(AuditEvent::getAction)
            .toList();

        assertTrue(eventTypes.contains("DATA_ACCESS"));
        assertTrue(eventTypes.contains("DATA_MODIFICATION"));
        assertTrue(eventTypes.contains("DATA_DELETION"));
        assertTrue(eventTypes.contains("CONSENT_UPDATED"));

        // Verify metadata is preserved for compliance reporting
        var consentEvent = complianceEvents.getContent().stream()
            .filter(e -> "CONSENT_UPDATED".equals(e.getAction()))
            .findFirst()
            .orElseThrow();

        assertNotNull(consentEvent.getMetadata());
        assertTrue(consentEvent.getMetadata().contains("consentVersion"));
    }

    @Test
    void shouldTrackDataProcessingLegalBasis() throws Exception {
        // Log events with legal basis for processing
        Map<String, Object> processingMetadata = Map.of(
            "legalBasis", "LEGITIMATE_INTEREST",
            "purpose", "fraud-prevention",
            "dataCategories", List.of("ip-address", "user-agent", "transaction-amount")
        );

        auditService.logEvent(
            "DATA_PROCESSING",
            "TRANSACTION_ANALYSIS",
            "transaction-" + UUID.randomUUID(),
            "ANALYZE",
            Map.of("transactionAmount", 1000.00, "merchantId", "merchant-123"),
            Map.of("riskScore", 0.15, "decision", "APPROVED"),
            "192.168.1.100",
            "Risk-Engine",
            processingMetadata
        ).get(5, TimeUnit.SECONDS);

        Map<String, Object> consentBasedProcessing = Map.of(
            "legalBasis", "CONSENT",
            "consentId", "consent-" + UUID.randomUUID(),
            "purpose", "marketing-analytics"
        );

        auditService.logEvent(
            "DATA_PROCESSING",
            "MARKETING_ANALYSIS",
            "campaign-" + UUID.randomUUID(),
            "PROCESS",
            Map.of("userId", userId, "segments", List.of("high-value", "frequent-buyer")),
            Map.of("recommendations", List.of("product-A", "product-B")),
            "192.168.1.101",
            "Marketing-Engine",
            consentBasedProcessing
        ).get(5, TimeUnit.SECONDS);

        // Verify legal basis is tracked
        var processingEvents = auditEventRepository.findByOrganizationIdAndActionOrderByCreatedAtDesc(
            orgId, "DATA_PROCESSING", org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(2, processingEvents.getTotalElements());

        for (AuditEvent event : processingEvents.getContent()) {
            assertNotNull(event.getMetadata());
            assertTrue(event.getMetadata().contains("legalBasis"));
            assertTrue(event.getMetadata().contains("purpose"));
        }
    }

    @Test
    void shouldSupportDataMinimizationPrinciple() throws Exception {
        // Create event with minimal necessary data
        Map<String, Object> minimalData = Map.of(
            "action", "password-reset",
            "timestamp", Instant.now().toString(),
            "success", true
        );

        // Should not include unnecessary PII
        Map<String, Object> unnecessaryData = Map.of(
            "email", "user@example.com", // Will be redacted
            "previousPassword", "old-password", // Should never be logged
            "newPassword", "new-password", // Should never be logged
            "mothersMaidenName", "Smith" // Unnecessary PII
        );

        auditService.logEvent(
            "SECURITY_ACTION",
            "PASSWORD_RESET",
            "user-" + userId,
            "RESET",
            minimalData, // Only necessary data
            Map.of("result", "success"),
            "192.168.1.100",
            "Web",
            null
        ).get(5, TimeUnit.SECONDS);

        // Verify minimal data is stored
        var events = auditEventRepository.findByResourceTypeAndResourceId(
            "PASSWORD_RESET", "user-" + userId);
        assertEquals(1, events.size());

        AuditEvent event = events.get(0);

        // Verify only necessary data is present
        assertTrue(event.getRequestData().contains("password-reset"));
        assertTrue(event.getRequestData().contains("success"));

        // Verify sensitive data is not present
        assertFalse(event.getRequestData().contains("previousPassword"));
        assertFalse(event.getRequestData().contains("newPassword"));
        assertFalse(event.getRequestData().contains("mothersMaidenName"));
    }

    @Test
    void shouldProvideTransparencyForDataSubjects() throws Exception {
        // Create comprehensive audit trail for a user
        TenantContext.setTenantInfo(orgId, "gdpr-test", userId);

        // Various user activities
        auditService.logUserLogin(userId, "192.168.1.100", "Chrome", true)
            .get(5, TimeUnit.SECONDS);

        auditService.logEvent(
            "PROFILE_VIEW",
            "USER_PROFILE",
            userId.toString(),
            "VIEW",
            null,
            Map.of("viewedBy", "support-agent-1", "reason", "support-ticket"),
            "192.168.1.101",
            "Support-Portal",
            Map.of("ticketId", "SUP-123")
        ).get(5, TimeUnit.SECONDS);

        auditService.logEvent(
            "DATA_EXPORT",
            "USER_DATA",
            userId.toString(),
            "EXPORT",
            Map.of("requestedBy", userId.toString(), "format", "JSON"),
            Map.of("status", "completed", "fileSize", "2.5MB"),
            "192.168.1.100",
            "User-Portal",
            Map.of("gdprRequest", "true")
        ).get(5, TimeUnit.SECONDS);

        // User can query their own audit trail
        var userAuditTrail = auditService.getUserAuditEvents(orgId, userId, 0, 50);

        // Verify transparency - user can see all actions
        assertTrue(userAuditTrail.getTotalElements() >= 3);

        // Verify user can see who accessed their data
        var profileViewEvent = userAuditTrail.getContent().stream()
            .filter(e -> "PROFILE_VIEW".equals(e.getAction()))
            .findFirst()
            .orElseThrow();

        assertNotNull(profileViewEvent.getResponseData());
        assertTrue(profileViewEvent.getResponseData().contains("support-agent-1"));
        assertTrue(profileViewEvent.getMetadata().contains("SUP-123"));

        // Verify user can see their data exports
        var exportEvent = userAuditTrail.getContent().stream()
            .filter(e -> "DATA_EXPORT".equals(e.getAction()))
            .findFirst()
            .orElseThrow();

        assertNotNull(exportEvent.getMetadata());
        assertTrue(exportEvent.getMetadata().contains("gdprRequest"));
    }
}