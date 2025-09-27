package com.platform.audit.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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
 * Integration tests for audit event persistence and retrieval.
 * Tests audit logging, querying, statistics, and cross-module integration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class AuditEventPersistenceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

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
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        // Clear existing audit events
        auditEventRepository.deleteAll();

        // Create test organization
        Organization org = new Organization("Audit Test Corp", "audit-test", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test users
        User user = new User("user@audittest.com", "Test User");
        user.setOrganization(org);
        user = userRepository.save(user);
        userId = user.getId();

        User adminUser = new User("admin@audittest.com", "Admin User");
        adminUser.setOrganization(org);
        adminUser = userRepository.save(adminUser);
        adminUserId = adminUser.getId();

        // Set tenant context for tests
        TenantContext.setTenantInfo(orgId, "audit-test", userId);
    }

    @Test
    void shouldPersistAuditEventSuccessfully() throws Exception {
        // Log an audit event
        auditService.logEvent(
            "USER_ACTION",
            "RESOURCE",
            "resource-123",
            "CREATE",
            Map.of("field1", "value1", "field2", "value2"),
            Map.of("result", "success", "id", "new-id-123"),
            "192.168.1.100",
            "Mozilla/5.0",
            Map.of("correlationId", UUID.randomUUID().toString())
        ).get(5, TimeUnit.SECONDS);

        // Verify event was persisted
        var events = auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(
            orgId, org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(1, events.getTotalElements());
        AuditEvent event = events.getContent().get(0);

        assertEquals("USER_ACTION", event.getAction());
        assertEquals("RESOURCE", event.getResourceType());
        assertEquals("resource-123", event.getResourceId());
        assertEquals(userId, event.getActorId());
        assertEquals(orgId, event.getOrganizationId());
        assertEquals("192.168.1.100", event.getIpAddress());
        assertEquals("Mozilla/5.0", event.getUserAgent());
        assertNotNull(event.getRequestData());
        assertNotNull(event.getResponseData());
        assertNotNull(event.getMetadata());
    }

    @Test
    void shouldLogUserLoginEventsCorrectly() throws Exception {
        // Log successful login
        auditService.logUserLogin(userId, "192.168.1.100", "Chrome/120.0", true)
            .get(5, TimeUnit.SECONDS);

        // Log failed login
        auditService.logUserLogin(userId, "192.168.1.101", "Firefox/121.0", false)
            .get(5, TimeUnit.SECONDS);

        // Verify events
        var events = auditEventRepository.findByActorIdOrderByCreatedAtDesc(userId);
        assertEquals(2, events.size());

        var successEvent = events.stream()
            .filter(e -> "USER_LOGIN".equals(e.getAction()))
            .findFirst()
            .orElseThrow();
        assertEquals("LOGIN", successEvent.getResourceType());
        assertTrue(successEvent.getMetadata().contains("\"success\":\"true\""));

        var failEvent = events.stream()
            .filter(e -> "USER_LOGIN_FAILED".equals(e.getAction()))
            .findFirst()
            .orElseThrow();
        assertEquals("LOGIN", failEvent.getResourceType());
        assertTrue(failEvent.getMetadata().contains("\"success\":\"false\""));
    }

    @Test
    void shouldLogPaymentEventsWithProperRedaction() throws Exception {
        // Log payment event with sensitive data
        Map<String, Object> paymentData = Map.of(
            "amount", 100.00,
            "currency", "usd",
            "cardNumber", "4111-1111-1111-1111", // Should be redacted
            "customerEmail", "customer@example.com" // Should be redacted
        );

        auditService.logPaymentEvent(
            "PAYMENT_PROCESSED",
            "payment-123",
            "CHARGE",
            paymentData,
            "192.168.1.100",
            "API/2.0"
        ).get(5, TimeUnit.SECONDS);

        // Verify event and PII redaction
        var events = auditEventRepository.findByResourceTypeAndResourceId("PAYMENT", "payment-123");
        assertEquals(1, events.size());

        AuditEvent event = events.get(0);
        assertEquals("PAYMENT_PROCESSED", event.getAction());

        // Check PII was redacted
        assertFalse(event.getRequestData().contains("4111-1111-1111-1111"));
        assertFalse(event.getRequestData().contains("customer@example.com"));
        assertTrue(event.getRequestData().contains("[REDACTED]"));
    }

    @Test
    void shouldTrackResourceModificationWithBeforeAfterSnapshots() throws Exception {
        Map<String, Object> oldData = Map.of(
            "name", "Old Name",
            "status", "ACTIVE",
            "email", "old@example.com"
        );

        Map<String, Object> newData = Map.of(
            "name", "New Name",
            "status", "INACTIVE",
            "email", "new@example.com"
        );

        auditService.logDataModification(
            "USER_PROFILE",
            userId.toString(),
            "UPDATE",
            oldData,
            newData,
            "192.168.1.100",
            "Web/1.0"
        ).get(5, TimeUnit.SECONDS);

        // Verify modification event
        var events = auditEventRepository.findByResourceTypeAndResourceId(
            "USER_PROFILE", userId.toString());
        assertEquals(1, events.size());

        AuditEvent event = events.get(0);
        assertEquals("DATA_MODIFICATION", event.getAction());
        assertNotNull(event.getRequestData()); // Contains old data
        assertNotNull(event.getResponseData()); // Contains new data

        // Verify emails are redacted
        assertFalse(event.getRequestData().contains("old@example.com"));
        assertFalse(event.getResponseData().contains("new@example.com"));
        assertTrue(event.getRequestData().contains("[REDACTED]"));
        assertTrue(event.getResponseData().contains("[REDACTED]"));
    }

    @Test
    void shouldLogSecurityEventsWithHighPriority() throws Exception {
        // Log various security events
        auditService.logSecurityEvent(
            "UNAUTHORIZED_ACCESS_ATTEMPT",
            "User attempted to access restricted resource",
            "192.168.1.200",
            "Unknown",
            Map.of("resource", "/api/v1/admin/users", "method", "DELETE")
        ).get(5, TimeUnit.SECONDS);

        auditService.logSecurityEvent(
            "SUSPICIOUS_ACTIVITY",
            "Multiple failed login attempts detected",
            "192.168.1.201",
            "Bot/1.0",
            Map.of("attempts", "15", "timeWindow", "60s")
        ).get(5, TimeUnit.SECONDS);

        // Verify security events
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        var securityEvents = auditEventRepository.findSecurityEventsAfter(orgId, since);

        assertTrue(securityEvents.size() >= 2);
        assertTrue(securityEvents.stream()
            .anyMatch(e -> "UNAUTHORIZED_ACCESS_ATTEMPT".equals(e.getAction())));
        assertTrue(securityEvents.stream()
            .anyMatch(e -> "SUSPICIOUS_ACTIVITY".equals(e.getAction())));
    }

    @Test
    void shouldRetrieveOrganizationAuditEventsWithPagination() throws Exception {
        // Create multiple audit events
        for (int i = 0; i < 25; i++) {
            auditService.logResourceAccess(
                "DOCUMENT",
                "doc-" + i,
                "VIEW",
                "192.168.1." + i,
                "Browser/1.0"
            ).get(5, TimeUnit.SECONDS);
        }

        // Query first page
        Page<AuditEvent> page1 = auditService.getOrganizationAuditEvents(orgId, 0, 10);
        assertEquals(25, page1.getTotalElements());
        assertEquals(3, page1.getTotalPages());
        assertEquals(10, page1.getContent().size());

        // Query second page
        Page<AuditEvent> page2 = auditService.getOrganizationAuditEvents(orgId, 1, 10);
        assertEquals(10, page2.getContent().size());

        // Verify ordering (newest first)
        assertTrue(page1.getContent().get(0).getCreatedAt()
            .isAfter(page1.getContent().get(9).getCreatedAt()) ||
            page1.getContent().get(0).getCreatedAt()
            .equals(page1.getContent().get(9).getCreatedAt()));
    }

    @Test
    void shouldRetrieveUserSpecificAuditEvents() throws Exception {
        // Create events for different users
        TenantContext.setTenantInfo(orgId, "audit-test", userId);
        auditService.logUserLogin(userId, "192.168.1.100", "Chrome", true)
            .get(5, TimeUnit.SECONDS);
        auditService.logResourceAccess("FILE", "file-1", "DOWNLOAD", "192.168.1.100", "Chrome")
            .get(5, TimeUnit.SECONDS);

        TenantContext.setTenantInfo(orgId, "audit-test", adminUserId);
        auditService.logUserLogin(adminUserId, "192.168.1.101", "Firefox", true)
            .get(5, TimeUnit.SECONDS);
        auditService.logResourceAccess("FILE", "file-2", "DELETE", "192.168.1.101", "Firefox")
            .get(5, TimeUnit.SECONDS);

        // Query user-specific events
        Page<AuditEvent> userEvents = auditService.getUserAuditEvents(orgId, userId, 0, 10);
        assertEquals(2, userEvents.getTotalElements());
        assertTrue(userEvents.getContent().stream()
            .allMatch(e -> userId.equals(e.getActorId())));

        Page<AuditEvent> adminEvents = auditService.getUserAuditEvents(orgId, adminUserId, 0, 10);
        assertEquals(2, adminEvents.getTotalElements());
        assertTrue(adminEvents.getContent().stream()
            .allMatch(e -> adminUserId.equals(e.getActorId())));
    }

    @Test
    void shouldFilterAuditEventsByDateRange() throws Exception {
        // Create events at different times
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);

        // Create test events with specific timestamps (would need repository access for this)
        AuditEvent event1 = new AuditEvent(orgId, userId, "ACTION_1", "RESOURCE", "res-1", "CREATE", "192.168.1.1", "Browser");
        event1.setCreatedAt(weekAgo);
        auditEventRepository.save(event1);

        AuditEvent event2 = new AuditEvent(orgId, userId, "ACTION_2", "RESOURCE", "res-2", "UPDATE", "192.168.1.2", "Browser");
        event2.setCreatedAt(twoDaysAgo);
        auditEventRepository.save(event2);

        AuditEvent event3 = new AuditEvent(orgId, userId, "ACTION_3", "RESOURCE", "res-3", "DELETE", "192.168.1.3", "Browser");
        event3.setCreatedAt(yesterday);
        auditEventRepository.save(event3);

        // Query events in date range
        Page<AuditEvent> recentEvents = auditService.getAuditEventsByDateRange(
            orgId, twoDaysAgo.minus(1, ChronoUnit.HOURS), now, 0, 10);
        assertEquals(2, recentEvents.getTotalElements());

        Page<AuditEvent> allEvents = auditService.getAuditEventsByDateRange(
            orgId, weekAgo.minus(1, ChronoUnit.HOURS), now, 0, 10);
        assertEquals(3, allEvents.getTotalElements());
    }

    @Test
    void shouldProvideResourceAuditTrail() throws Exception {
        String resourceId = "payment-" + UUID.randomUUID();

        // Simulate payment lifecycle
        auditService.logPaymentEvent("PAYMENT_INITIATED", resourceId, "CREATE",
            Map.of("amount", 100.00), "192.168.1.100", "API/1.0")
            .get(5, TimeUnit.SECONDS);

        Thread.sleep(100); // Ensure time difference

        auditService.logPaymentEvent("PAYMENT_AUTHORIZED", resourceId, "AUTHORIZE",
            Map.of("authCode", "AUTH123"), "192.168.1.100", "API/1.0")
            .get(5, TimeUnit.SECONDS);

        Thread.sleep(100);

        auditService.logPaymentEvent("PAYMENT_CAPTURED", resourceId, "CAPTURE",
            Map.of("transactionId", "TXN123"), "192.168.1.100", "API/1.0")
            .get(5, TimeUnit.SECONDS);

        // Get resource audit trail
        var auditTrail = auditService.getResourceAuditTrail(orgId, "PAYMENT", resourceId);
        assertEquals(3, auditTrail.size());

        // Verify chronological order
        assertEquals("PAYMENT_INITIATED", auditTrail.get(0).getAction());
        assertEquals("PAYMENT_AUTHORIZED", auditTrail.get(1).getAction());
        assertEquals("PAYMENT_CAPTURED", auditTrail.get(2).getAction());

        // Verify all events are for the same resource
        assertTrue(auditTrail.stream()
            .allMatch(e -> resourceId.equals(e.getResourceId())));
    }

    @Test
    void shouldCalculateAuditStatisticsCorrectly() throws Exception {
        // Create various events over time
        Instant now = Instant.now();

        // Events from different time periods
        for (int i = 0; i < 5; i++) {
            auditService.logUserLogin(userId, "192.168.1." + i, "Browser", true)
                .get(5, TimeUnit.SECONDS);
        }

        for (int i = 0; i < 3; i++) {
            auditService.logPaymentEvent("PAYMENT", "pay-" + i, "CREATE",
                Map.of("amount", 100 + i), "192.168.1." + i, "API")
                .get(5, TimeUnit.SECONDS);
        }

        for (int i = 0; i < 2; i++) {
            auditService.logSecurityEvent("SECURITY_CHECK", "Routine check " + i,
                "192.168.1." + i, "System", Map.of("level", "INFO"))
                .get(5, TimeUnit.SECONDS);
        }

        // Get statistics
        AuditService.AuditStatistics stats = auditService.getAuditStatistics(orgId);

        assertTrue(stats.totalEvents() >= 10);
        assertTrue(stats.recentEvents() >= 10);
        assertTrue(stats.weeklyEvents() >= 10);
        assertTrue(stats.dailyEvents() >= 10);
        assertTrue(stats.uniqueEventTypes() >= 3);
        assertTrue(stats.uniqueResourceTypes() >= 3);
    }

    @Test
    void shouldIdentifySuspiciousIpAddresses() throws Exception {
        String suspiciousIp = "192.168.100.50";
        String normalIp = "192.168.1.1";

        // Simulate suspicious activity from one IP
        for (int i = 0; i < 10; i++) {
            TenantContext.setTenantInfo(orgId, "audit-test", userId);
            auditService.logUserLogin(userId, suspiciousIp, "Bot/1.0", false)
                .get(5, TimeUnit.SECONDS);
        }

        // Normal activity from another IP
        auditService.logUserLogin(userId, normalIp, "Chrome", true)
            .get(5, TimeUnit.SECONDS);

        // Check suspicious IPs
        var suspiciousIps = auditService.getSuspiciousIpAddresses(
            orgId, Instant.now().minus(1, ChronoUnit.HOURS), 5L);

        assertTrue(suspiciousIps.size() >= 1);
        assertTrue(suspiciousIps.stream()
            .anyMatch(result -> suspiciousIp.equals(result[0])));
    }

    @Test
    void shouldSearchAuditEventsWithFreeText() throws Exception {
        // Create events with searchable content
        auditService.logEvent("USER_SEARCH", "SEARCH", "search-1", "EXECUTE",
            Map.of("query", "financial report Q4 2024"),
            Map.of("results", 15),
            "192.168.1.100", "Web", null)
            .get(5, TimeUnit.SECONDS);

        auditService.logEvent("DOCUMENT_ACCESS", "DOCUMENT", "doc-financial", "VIEW",
            Map.of("title", "Q4 2024 Financial Statement"),
            null,
            "192.168.1.101", "Web", null)
            .get(5, TimeUnit.SECONDS);

        auditService.logEvent("REPORT_GENERATION", "REPORT", "report-1", "GENERATE",
            Map.of("type", "quarterly financial"),
            Map.of("status", "completed"),
            "192.168.1.102", "API", null)
            .get(5, TimeUnit.SECONDS);

        // Search for "financial"
        Page<AuditEvent> searchResults = auditService.searchAuditEvents(
            orgId, "financial", 0, 10);

        assertTrue(searchResults.getTotalElements() >= 2);
        assertTrue(searchResults.getContent().stream()
            .allMatch(e -> e.getRequestData() != null || e.getResponseData() != null));
    }

    @Test
    void shouldGetAvailableEventAndResourceTypes() throws Exception {
        // Create events with different types
        auditService.logUserLogin(userId, "192.168.1.1", "Browser", true)
            .get(5, TimeUnit.SECONDS);
        auditService.logPaymentEvent("PAYMENT_CREATED", "pay-1", "CREATE",
            Map.of("amount", 100), "192.168.1.2", "API")
            .get(5, TimeUnit.SECONDS);
        auditService.logSecurityEvent("ACCESS_DENIED", "Unauthorized access",
            "192.168.1.3", "Browser", null)
            .get(5, TimeUnit.SECONDS);
        auditService.logResourceAccess("DOCUMENT", "doc-1", "READ",
            "192.168.1.4", "API")
            .get(5, TimeUnit.SECONDS);

        // Get available event types
        var eventTypes = auditService.getAvailableEventTypes(orgId);
        assertTrue(eventTypes.contains("USER_LOGIN"));
        assertTrue(eventTypes.contains("PAYMENT_CREATED"));
        assertTrue(eventTypes.contains("ACCESS_DENIED"));
        assertTrue(eventTypes.contains("RESOURCE_ACCESS"));

        // Get available resource types
        var resourceTypes = auditService.getAvailableResourceTypes(orgId);
        assertTrue(resourceTypes.contains("USER"));
        assertTrue(resourceTypes.contains("PAYMENT"));
        assertTrue(resourceTypes.contains("SECURITY"));
        assertTrue(resourceTypes.contains("DOCUMENT"));
    }

    @Test
    void shouldEnforceTenantIsolationInQueries() {
        // Create audit event for org1
        TenantContext.setTenantInfo(orgId, "audit-test", userId);
        auditService.logResourceAccess("FILE", "file-1", "READ", "192.168.1.1", "Browser");

        // Create another organization
        Organization org2 = new Organization("Other Corp", "other-corp", (UUID) null);
        org2 = organizationRepository.save(org2);
        UUID org2Id = org2.getId();

        // Try to access org1's audit events with org2 context
        TenantContext.setTenantInfo(org2Id, "other-corp", UUID.randomUUID());

        assertThrows(SecurityException.class, () -> {
            auditService.getOrganizationAuditEvents(orgId, 0, 10);
        });

        assertThrows(SecurityException.class, () -> {
            auditService.getResourceAuditTrail(orgId, "FILE", "file-1");
        });

        assertThrows(SecurityException.class, () -> {
            auditService.deleteOrganizationAuditEvents(orgId);
        });
    }

    @Test
    void shouldHandleConcurrentAuditEventLogging() throws Exception {
        int threadCount = 10;
        int eventsPerThread = 10;

        // Log events concurrently from multiple threads
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < eventsPerThread; i++) {
                    try {
                        TenantContext.setTenantInfo(orgId, "audit-test", userId);
                        auditService.logResourceAccess(
                            "RESOURCE",
                            String.format("res-%d-%d", threadId, i),
                            "ACCESS",
                            String.format("192.168.%d.%d", threadId, i),
                            "Thread-" + threadId
                        ).get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        fail("Failed to log audit event: " + e.getMessage());
                    }
                }
            });
            threads[t].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(10000);
        }

        // Verify all events were logged
        Thread.sleep(1000); // Allow async operations to complete
        Page<AuditEvent> allEvents = auditService.getOrganizationAuditEvents(orgId, 0, 200);
        assertEquals(threadCount * eventsPerThread, allEvents.getTotalElements());
    }
}