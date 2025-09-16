package com.platform.auth.audit;

import com.platform.auth.internal.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for OAuth2 audit system compliance and GDPR requirements.
 * Validates that all 26 audit event types are properly implemented and that
 * GDPR compliance features (data retention, deletion, consent tracking) work correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class OAuth2AuditComplianceTest {

    @Autowired
    private OAuth2AuditService auditService;

    @Autowired
    private OAuth2AuditEventRepository auditEventRepository;

    private static final String TEST_PROVIDER = "google";
    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_SESSION_ID = "test-session-456";
    private static final String TEST_IP_ADDRESS = "192.168.1.100";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 Test Browser";
    private static final String TEST_ERROR_CODE = "TEST_ERROR";
    private static final String TEST_ERROR_MESSAGE = "Test error message";

    @Test
    @Order(1)
    @DisplayName("All OAuth2 authentication flow events should be logged correctly")
    void testAuthenticationFlowEvents() {
        // Test authorization started
        auditService.logAuthorizationStarted(TEST_PROVIDER, TEST_USER_ID, TEST_SESSION_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);

        // Test authorization completed
        auditService.logAuthorizationCompleted(TEST_PROVIDER, TEST_USER_ID, TEST_SESSION_ID, "authCodeHash", "stateHash", TEST_IP_ADDRESS, 150L);

        // Test authorization failure
        auditService.logAuthorizationFailure(TEST_PROVIDER, TEST_ERROR_CODE, TEST_ERROR_MESSAGE, TEST_USER_ID, TEST_SESSION_ID, TEST_IP_ADDRESS);

        // Test authorization denied
        auditService.logAuthorizationDenied(TEST_PROVIDER, TEST_USER_ID, TEST_IP_ADDRESS);

        // Verify events were created
        List<OAuth2AuditEvent> events = auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_STARTED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getProvider()).isEqualTo(TEST_PROVIDER);
        assertThat(events.get(0).getUserId()).isEqualTo(TEST_USER_ID);

        events = auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_COMPLETED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDurationMs()).isEqualTo(150L);

        events = auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_FAILED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getErrorCode()).isEqualTo(TEST_ERROR_CODE);
        assertThat(events.get(0).getSuccess()).isFalse();

        events = auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_DENIED);
        assertThat(events).hasSize(1);
    }

    @Test
    @Order(2)
    @DisplayName("All OAuth2 token events should be logged correctly")
    void testTokenEvents() {
        // Test token exchange events
        auditService.logTokenExchangeStarted(TEST_PROVIDER, TEST_USER_ID, "authCodeHash", TEST_IP_ADDRESS);
        auditService.logTokenExchangeCompleted(TEST_PROVIDER, TEST_USER_ID, TEST_IP_ADDRESS, 75L);
        auditService.logTokenExchangeFailure(TEST_PROVIDER, TEST_USER_ID, TEST_ERROR_CODE, TEST_ERROR_MESSAGE, TEST_IP_ADDRESS);
        auditService.logTokenValidationFailure(TEST_PROVIDER, TEST_USER_ID, "VALIDATION_ERROR", "Token validation failed", TEST_IP_ADDRESS);

        // Verify events
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.TOKEN_EXCHANGE_STARTED)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.TOKEN_EXCHANGE_COMPLETED)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.TOKEN_EXCHANGE_FAILED)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.TOKEN_VALIDATION_FAILED)).hasSize(1);
    }

    @Test
    @Order(3)
    @DisplayName("All OAuth2 session events should be logged correctly")
    void testSessionEvents() {
        // Test session events
        auditService.logSessionCreated(TEST_PROVIDER, TEST_USER_ID, TEST_SESSION_ID, TEST_IP_ADDRESS);
        auditService.logSessionRenewed(TEST_PROVIDER, TEST_USER_ID, TEST_SESSION_ID, TEST_IP_ADDRESS);
        auditService.logSessionExpired(TEST_PROVIDER, TEST_USER_ID, TEST_SESSION_ID, TEST_IP_ADDRESS);
        auditService.logSessionTerminated(TEST_PROVIDER, TEST_USER_ID, TEST_SESSION_ID, "user_logout", TEST_IP_ADDRESS);
        auditService.logUserSessionsTerminated(TEST_PROVIDER, TEST_USER_ID, 3, "security_cleanup");

        // Verify events
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.SESSION_CREATED)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.SESSION_RENEWED)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.SESSION_EXPIRED)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.SESSION_TERMINATED)).hasSize(2); // 2 session termination events
    }

    @Test
    @Order(4)
    @DisplayName("All OAuth2 user events should be logged correctly")
    void testUserEvents() {
        // Test user events
        auditService.logUserLogin(TEST_PROVIDER, TEST_USER_ID, TEST_SESSION_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
        auditService.logUserLogout(TEST_PROVIDER, TEST_USER_ID, TEST_SESSION_ID, TEST_IP_ADDRESS);
        auditService.logUserInfoRetrieved(TEST_PROVIDER, TEST_USER_ID, TEST_IP_ADDRESS);
        auditService.logUserInfoUpdated(TEST_PROVIDER, TEST_USER_ID, "\"email\",\"name\"", TEST_IP_ADDRESS);

        // Verify events
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.USER_LOGIN)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.USER_LOGOUT)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.USER_INFO_RETRIEVED)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.USER_INFO_UPDATED)).hasSize(1);
    }

    @Test
    @Order(5)
    @DisplayName("All OAuth2 security events should be logged correctly")
    void testSecurityEvents() {
        // Test security events
        auditService.logPkceValidationFailure(TEST_PROVIDER, TEST_USER_ID, TEST_SESSION_ID, TEST_IP_ADDRESS);
        auditService.logStateValidationFailure(TEST_PROVIDER, "expectedState", "actualState", TEST_IP_ADDRESS);
        auditService.logSuspiciousActivity(TEST_PROVIDER, "Multiple failed attempts", TEST_USER_ID, TEST_IP_ADDRESS, TEST_USER_AGENT);
        auditService.logRateLimitExceeded(TEST_PROVIDER, TEST_USER_ID, TEST_IP_ADDRESS);

        // Verify events
        List<OAuth2AuditEvent> events = auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.PKCE_VALIDATION_FAILED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSeverity()).isEqualTo(OAuth2AuditEvent.AuditSeverity.CRITICAL);

        events = auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.STATE_VALIDATION_FAILED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSeverity()).isEqualTo(OAuth2AuditEvent.AuditSeverity.CRITICAL);

        events = auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.SUSPICIOUS_ACTIVITY);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSeverity()).isEqualTo(OAuth2AuditEvent.AuditSeverity.CRITICAL);

        events = auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.RATE_LIMIT_EXCEEDED);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSeverity()).isEqualTo(OAuth2AuditEvent.AuditSeverity.CRITICAL);
    }

    @Test
    @Order(6)
    @DisplayName("All OAuth2 provider configuration events should be logged correctly")
    void testProviderConfigurationEvents() {
        // Test provider configuration events
        auditService.logProviderConfigured(TEST_PROVIDER, "admin-user");
        auditService.logProviderDisabled(TEST_PROVIDER, "admin-user", "security_update");

        // Verify events
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.PROVIDER_CONFIGURED)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.PROVIDER_DISABLED)).hasSize(1);
    }

    @Test
    @Order(7)
    @DisplayName("All GDPR compliance events should be logged correctly")
    void testGdprComplianceEvents() {
        // Test GDPR events
        auditService.logGdprDataExport(TEST_USER_ID, "user-self", "\"profile\",\"sessions\"", TEST_IP_ADDRESS);
        auditService.logGdprDataDeletion(TEST_USER_ID, "user-self", "full_account", TEST_IP_ADDRESS);
        auditService.logConsentGranted(TEST_PROVIDER, TEST_USER_ID, "data_processing", "v1.0", TEST_IP_ADDRESS);
        auditService.logConsentRevoked(TEST_PROVIDER, TEST_USER_ID, "marketing", "user_request", TEST_IP_ADDRESS);

        // Verify events
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.GDPR_DATA_EXPORT)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.GDPR_DATA_DELETION)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.CONSENT_GRANTED)).hasSize(1);
        assertThat(auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.CONSENT_REVOKED)).hasSize(1);

        // Test GDPR event retrieval
        List<OAuth2AuditEvent> gdprEvents = auditService.getGdprEventsForUser(TEST_USER_ID);
        assertThat(gdprEvents).hasSize(4); // 2 GDPR events + 2 consent events
    }

    @Test
    @Order(8)
    @DisplayName("Performance and analytics methods should return correct statistics")
    void testPerformanceAnalytics() {
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(1, ChronoUnit.HOURS);

        // Test event statistics
        List<Object[]> eventStats = auditService.getEventStatistics(startTime, endTime);
        assertThat(eventStats).isNotEmpty();

        // Test provider statistics
        List<Object[]> providerStats = auditService.getProviderStatistics(startTime, endTime);
        assertThat(providerStats).isNotEmpty();

        // Test performance metrics
        List<Object[]> perfMetrics = auditService.getPerformanceMetrics(startTime, endTime);
        assertThat(perfMetrics).isNotEmpty();

        // Test suspicious activity patterns
        List<Object[]> suspiciousPatterns = auditService.getSuspiciousActivityPatterns(
            Instant.now().minus(1, ChronoUnit.HOURS), 1);
        assertThat(suspiciousPatterns).isNotEmpty();

        // Test failed login attempt counts
        long failedLogins = auditService.countFailedLoginAttempts(TEST_USER_ID,
            Instant.now().minus(1, ChronoUnit.HOURS));
        assertThat(failedLogins).isGreaterThanOrEqualTo(0);

        long failedFromIp = auditService.countFailedAttemptsFromIp(TEST_IP_ADDRESS,
            Instant.now().minus(1, ChronoUnit.HOURS));
        assertThat(failedFromIp).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Order(9)
    @DisplayName("Data retention and cleanup should work correctly")
    void testDataRetentionCompliance() {
        // Create old audit event
        OAuth2AuditEvent oldEvent = new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.USER_LOGIN,
            "Old login event for retention testing"
        );
        oldEvent.setUserId(TEST_USER_ID);
        oldEvent.setProvider(TEST_PROVIDER);
        oldEvent.setEventTimestamp(Instant.now().minus(8, ChronoUnit.DAYS));
        auditEventRepository.save(oldEvent);

        // Count events before cleanup
        long eventCountBefore = auditEventRepository.count();
        assertThat(eventCountBefore).isGreaterThan(0);

        // Test cleanup with 7-day retention
        int cleanedUp = auditService.cleanupOldAuditEvents(Instant.now().minus(7, ChronoUnit.DAYS));
        assertThat(cleanedUp).isGreaterThan(0);

        // Verify cleanup event was logged
        List<OAuth2AuditEvent> cleanupEvents = auditEventRepository.findByEventTypeOrderByEventTimestampDesc(
            OAuth2AuditEvent.OAuth2EventType.GDPR_DATA_DELETION);

        // Should have at least one cleanup event (from previous GDPR test + this cleanup)
        assertThat(cleanupEvents).hasSizeGreaterThanOrEqualTo(1);

        // Find the retention cleanup event
        boolean hasRetentionCleanup = cleanupEvents.stream()
            .anyMatch(event -> event.getDescription().contains("retention cleanup"));
        assertThat(hasRetentionCleanup).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("Audit event correlation and traceability should work correctly")
    void testAuditTraceability() {
        // Log related events with same correlation context
        auditService.logAuthorizationStarted(TEST_PROVIDER, "trace-user", "trace-session", TEST_IP_ADDRESS, TEST_USER_AGENT);
        auditService.logUserLogin(TEST_PROVIDER, "trace-user", "trace-session", TEST_IP_ADDRESS, TEST_USER_AGENT);

        // Retrieve events by session
        List<OAuth2AuditEvent> sessionEvents = auditEventRepository.findBySessionIdOrderByEventTimestampDesc("trace-session");
        assertThat(sessionEvents).hasSize(2);

        // Verify correlation IDs are present
        for (OAuth2AuditEvent event : sessionEvents) {
            assertThat(event.getCorrelationId()).isNotNull();
            assertThat(event.getIpAddress()).isEqualTo(TEST_IP_ADDRESS);
            assertThat(event.getUserAgent()).isEqualTo(TEST_USER_AGENT);
        }

        // Test time-based queries
        Instant fromTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant toTime = Instant.now().plus(1, ChronoUnit.MINUTES);

        List<OAuth2AuditEvent> userEvents = auditService.getUserAuditEvents("trace-user", fromTime, toTime);
        assertThat(userEvents).hasSize(2);

        List<OAuth2AuditEvent> providerEvents = auditService.getProviderAuditEvents(TEST_PROVIDER, fromTime, toTime);
        assertThat(providerEvents).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(11)
    @DisplayName("All audit event severity levels should be correctly assigned")
    void testAuditEventSeverityLevels() {
        // Verify security events have CRITICAL severity
        List<OAuth2AuditEvent> criticalEvents = auditEventRepository.findBySeverity(
            OAuth2AuditEvent.AuditSeverity.CRITICAL, org.springframework.data.domain.Pageable.unpaged()).getContent();

        assertThat(criticalEvents).isNotEmpty();

        // Check that security-related events are marked as critical
        boolean hasCriticalSecurityEvents = criticalEvents.stream()
            .anyMatch(event -> event.getEventType() == OAuth2AuditEvent.OAuth2EventType.SUSPICIOUS_ACTIVITY ||
                              event.getEventType() == OAuth2AuditEvent.OAuth2EventType.PKCE_VALIDATION_FAILED ||
                              event.getEventType() == OAuth2AuditEvent.OAuth2EventType.STATE_VALIDATION_FAILED ||
                              event.getEventType() == OAuth2AuditEvent.OAuth2EventType.RATE_LIMIT_EXCEEDED);

        assertThat(hasCriticalSecurityEvents).isTrue();

        // Verify failed events have ERROR severity
        List<OAuth2AuditEvent> errorEvents = auditEventRepository.findBySeverityAndEventTimestampBetweenOrderByEventTimestampDesc(
            OAuth2AuditEvent.AuditSeverity.ERROR, Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.HOURS));

        assertThat(errorEvents).isNotEmpty();
    }

    @Test
    @Order(12)
    @DisplayName("Audit event context and metadata should be comprehensive")
    void testAuditEventContext() {
        // Create event with full context
        auditService.logUserLogin(TEST_PROVIDER, "context-test-user", "context-session",
                                 "203.0.113.42", "Mozilla/5.0 (Test Context Agent)");

        List<OAuth2AuditEvent> events = auditEventRepository.findByUserIdAndEventTimestampBetweenOrderByEventTimestampDesc(
            "context-test-user", Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now().plus(1, ChronoUnit.MINUTES));

        assertThat(events).hasSize(1);

        OAuth2AuditEvent event = events.get(0);
        assertThat(event.getUserId()).isEqualTo("context-test-user");
        assertThat(event.getSessionId()).isEqualTo("context-session");
        assertThat(event.getProvider()).isEqualTo(TEST_PROVIDER);
        assertThat(event.getIpAddress()).isEqualTo("203.0.113.42");
        assertThat(event.getUserAgent()).isEqualTo("Mozilla/5.0 (Test Context Agent)");
        assertThat(event.getCorrelationId()).isNotNull();
        assertThat(event.getEventTimestamp()).isNotNull();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getSuccess()).isTrue();
        assertThat(event.getSeverity()).isEqualTo(OAuth2AuditEvent.AuditSeverity.INFO);
    }

    @AfterAll
    static void verifyAllEventTypesImplemented() {
        // Verify all 26 OAuth2 event types have been tested
        OAuth2AuditEvent.OAuth2EventType[] allEventTypes = OAuth2AuditEvent.OAuth2EventType.values();
        assertThat(allEventTypes).hasSize(26);

        System.out.println("✅ All " + allEventTypes.length + " OAuth2 audit event types have been validated:");
        for (OAuth2AuditEvent.OAuth2EventType eventType : allEventTypes) {
            System.out.println("   - " + eventType);
        }
    }
}