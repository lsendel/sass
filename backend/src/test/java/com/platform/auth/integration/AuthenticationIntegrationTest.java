package com.platform.auth.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.auth.internal.*;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Integration tests for authentication and authorization workflows.
 * Tests OAuth2 flows, session management, security policies, and cross-module integration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {

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
    private OAuth2SessionRepository sessionRepository;

    @Autowired
    private OAuth2UserInfoRepository userInfoRepository;

    @Autowired
    private OAuth2ProviderRepository providerRepository;

    @Autowired
    private OAuth2AuditEventRepository auditEventRepository;

    @Autowired
    private AuthenticationAttemptRepository attemptRepository;

    @Autowired
    private TokenMetadataRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private UUID orgId;
    private OAuth2Provider testProvider;

    @BeforeEach
    void setUp() {
        // Create test organization
        Organization org = new Organization("Auth Corp", "auth-corp", null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test OAuth2 provider
        testProvider = new OAuth2Provider(
            "test-provider",
            "Test Provider",
            "https://test.com/auth",
            "https://test.com/token",
            "https://test.com/userinfo",
            "test-client-id",
            "sub"
        );
        testProvider = providerRepository.save(testProvider);
    }

    @Test
    void shouldListAvailableAuthProviders() throws Exception {
        mockMvc.perform(get("/api/v1/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(jsonPath("$.providers[0].name").value("test-provider"))
                .andExpect(jsonPath("$.providers[0].displayName").value("Test Provider"))
                .andExpect(jsonPath("$.providers[0].supported").value(true));
    }

    @Test
    void shouldInitiateOAuth2AuthorizationFlow() throws Exception {
        String state = "test-state-" + UUID.randomUUID();
        String codeVerifier = "test-code-verifier-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/auth/authorize")
                .param("provider", "test-provider")
                .param("state", state)
                .param("code_challenge", "test-challenge")
                .param("code_challenge_method", "S256"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                    org.hamcrest.Matchers.containsString("https://test.com/auth")));

        // Verify OAuth2 session was created
        var sessions = sessionRepository.findByProvider("test-provider");
        assertTrue(sessions.stream().anyMatch(s -> state.equals(s.getOauth2StateHash())));
    }

    @Test
    void shouldHandleOAuth2CallbackWithValidCode() throws Exception {
        // Create test session
        OAuth2UserInfo userInfo = new OAuth2UserInfo(
            "test-provider",
            "test-user-id",
            "test@example.com",
            "Test User"
        );
        userInfo = userInfoRepository.save(userInfo);

        OAuth2Session session = new OAuth2Session(
            "session-123",
            "test-provider",
            userInfo,
            Instant.now().plus(1, ChronoUnit.HOURS)
        );
        session = sessionRepository.save(session);

        String callbackRequest = """
            {
                "code": "auth-code-123",
                "state": "test-state",
                "sessionId": "session-123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.authenticated").value(true));

        // Verify session is now active
        var updatedSession = sessionRepository.findById(session.getId());
        assertTrue(updatedSession.isPresent());
        assertTrue(updatedSession.get().isActive());
    }

    @Test
    void shouldRejectInvalidAuthorizationCodes() throws Exception {
        String invalidCallbackRequest = """
            {
                "code": "invalid-code",
                "state": "invalid-state",
                "sessionId": "non-existent"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidCallbackRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_AUTHORIZATION_CODE"));

        // Verify audit event was logged
        var auditEvents = auditEventRepository.findByEventType(OAuth2AuditEvent.EventType.AUTHORIZATION_FAILED);
        assertFalse(auditEvents.isEmpty());
    }

    @Test
    void shouldManageSessionLifecycle() throws Exception {
        // Create user and session
        User user = new User("session@example.com", "Session User", orgId);
        user = userRepository.save(user);

        OAuth2UserInfo userInfo = new OAuth2UserInfo(
            "test-provider",
            "session-user-id",
            "session@example.com",
            "Session User"
        );
        userInfo = userInfoRepository.save(userInfo);

        OAuth2Session session = new OAuth2Session(
            "session-lifecycle-123",
            "test-provider",
            userInfo,
            Instant.now().plus(1, ChronoUnit.HOURS)
        );
        session.activate();
        session = sessionRepository.save(session);

        // Check session status
        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer session-lifecycle-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value("session@example.com"));

        // Logout and terminate session
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer session-lifecycle-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify session is terminated
        var terminatedSession = sessionRepository.findById(session.getId());
        assertTrue(terminatedSession.isPresent());
        assertFalse(terminatedSession.get().isActive());
        assertNotNull(terminatedSession.get().getTerminatedAt());
    }

    @Test
    void shouldEnforceSecurityPolicies() throws Exception {
        String maliciousRequest = """
            {
                "code": "auth-code-123",
                "state": "../../../etc/passwd",
                "sessionId": "<script>alert('xss')</script>"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        // Verify security audit event
        var auditEvents = auditEventRepository.findByEventType(OAuth2AuditEvent.EventType.SUSPICIOUS_ACTIVITY);
        assertFalse(auditEvents.isEmpty());
    }

    @Test
    void shouldHandleTokenValidationAndExpiry() throws Exception {
        // Create expired session
        OAuth2UserInfo userInfo = new OAuth2UserInfo(
            "test-provider",
            "expired-user",
            "expired@example.com",
            "Expired User"
        );
        userInfo = userInfoRepository.save(userInfo);

        OAuth2Session expiredSession = new OAuth2Session(
            "expired-session-123",
            "test-provider",
            userInfo,
            Instant.now().minus(1, ChronoUnit.HOURS) // Already expired
        );
        expiredSession = sessionRepository.save(expiredSession);

        // Try to use expired session
        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer expired-session-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SESSION_EXPIRED"));

        // Verify audit event for expired session
        var auditEvents = auditEventRepository.findByEventType(OAuth2AuditEvent.EventType.SESSION_EXPIRED);
        assertFalse(auditEvents.isEmpty());
    }

    @Test
    void shouldTrackAuthenticationAttempts() throws Exception {
        String failedAttemptRequest = """
            {
                "code": "invalid-code",
                "state": "test-state",
                "sessionId": "non-existent"
            }
            """;

        // Make multiple failed attempts
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/callback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(failedAttemptRequest)
                    .header("X-Forwarded-For", "192.168.1.100"))
                    .andExpect(status().isUnauthorized());
        }

        // Verify attempts were tracked
        var attempts = attemptRepository.findByIpAddressAndAttemptTimeAfter(
            "192.168.1.100",
            Instant.now().minus(5, ChronoUnit.MINUTES)
        );
        assertEquals(3, attempts.size());
        assertTrue(attempts.stream().allMatch(a -> !a.isSuccess()));
    }

    @Test
    void shouldImplementRateLimiting() throws Exception {
        String rateLimitRequest = """
            {
                "code": "rate-limit-test",
                "state": "test-state",
                "sessionId": "rate-limit-session"
            }
            """;

        // Make rapid requests to trigger rate limiting
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/callback")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rateLimitRequest)
                    .header("X-Forwarded-For", "192.168.1.200"));
        }

        // Final request should be rate limited
        mockMvc.perform(post("/api/v1/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rateLimitRequest)
                .header("X-Forwarded-For", "192.168.1.200"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"));

        // Verify rate limit audit event
        var auditEvents = auditEventRepository.findByEventType(OAuth2AuditEvent.EventType.RATE_LIMIT_EXCEEDED);
        assertFalse(auditEvents.isEmpty());
    }

    @Test
    void shouldValidatePKCECodeChallenge() throws Exception {
        String invalidPKCERequest = """
            {
                "code": "auth-code-123",
                "state": "test-state",
                "sessionId": "session-123",
                "codeVerifier": "invalid-verifier"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPKCERequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("PKCE_VALIDATION_FAILED"));

        // Verify PKCE validation audit event
        var auditEvents = auditEventRepository.findByEventType(OAuth2AuditEvent.EventType.PKCE_VALIDATION_FAILED);
        assertFalse(auditEvents.isEmpty());
    }

    @Test
    void shouldHandleUserInfoSynchronization() throws Exception {
        // Create session with outdated user info
        OAuth2UserInfo userInfo = new OAuth2UserInfo(
            "test-provider",
            "sync-user-id",
            "sync@example.com",
            "Old Name"
        );
        userInfo.setLastUpdatedFromProvider(Instant.now().minus(30, ChronoUnit.DAYS));
        userInfo = userInfoRepository.save(userInfo);

        OAuth2Session session = new OAuth2Session(
            "sync-session-123",
            "test-provider",
            userInfo,
            Instant.now().plus(1, ChronoUnit.HOURS)
        );
        session.activate();
        session = sessionRepository.save(session);

        // Request user info update
        mockMvc.perform(post("/api/v1/auth/sync")
                .header("Authorization", "Bearer sync-session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true));

        // Verify user info sync audit event
        var auditEvents = auditEventRepository.findByEventType(OAuth2AuditEvent.EventType.USER_INFO_UPDATED);
        assertFalse(auditEvents.isEmpty());
    }
}