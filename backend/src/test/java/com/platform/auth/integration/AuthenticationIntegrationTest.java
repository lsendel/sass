package com.platform.auth.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc
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
        providerRepository.deleteAll();
        userInfoRepository.deleteAll();
        sessionRepository.deleteAll();
        auditEventRepository.deleteAll();
        attemptRepository.deleteAll();
        tokenRepository.deleteAll();

        // Create test organization
        Organization org = new Organization("Auth Corp", "auth-corp", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test OAuth2 provider
        testProvider = new OAuth2Provider(
            "test-provider",
            "Test Provider",
            "https://test.com/auth",
            "https://test.com/token",
            "https://test.com/userinfo",
            java.util.List.of("openid", "profile", "email"),
            "test-client-id"
        );
        testProvider.setEnabled(true);
        testProvider.setSortOrder(0);
        testProvider.setConfiguration(null);
        testProvider.setUserNameAttribute("sub");
        testProvider.setCreatedAt(java.time.Instant.now());
        testProvider.setUpdatedAt(java.time.Instant.now());
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
        // Create test session first to avoid 401 errors
        OAuth2UserInfo userInfo = new OAuth2UserInfo(
            "test-user-id",
            "test-provider",
            "test@example.com"
        );
        userInfo.setName("Test User");
        userInfo = userInfoRepository.save(userInfo);

        OAuth2Session session = new OAuth2Session(
            "session-123",
            userInfo,
            "test-provider",
            Instant.now().plus(1, ChronoUnit.HOURS)
        );
        // Set PKCE parameters to avoid validation errors
        session.setPkceCodeVerifierHash("test-code-verifier");
        session.setOauth2StateHash("test-state");
        session = sessionRepository.save(session);

        String callbackRequest = """
            {
                "code": "auth-code-123",
                "state": "test-state",
                "sessionId": "session-123",
                "codeVerifier": "test-code-verifier"
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
        assertTrue(updatedSession.get().isValid());
    }

    @Test
    void shouldRejectInvalidAuthorizationCodes() throws Exception {
        String invalidCallbackRequest = """
            {
                "code": "invalid-code",
                "state": "invalid-state",
                "sessionId": "non-existent",
                "codeVerifier": "invalid-verifier"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidCallbackRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_AUTHORIZATION_CODE"));

        // Verify audit event was logged - use repository query instead of direct findBy method
        var auditEvents = auditEventRepository.findAll().stream()
            .filter(event -> event.getEventType() == OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_FAILED)
            .toList();
        assertFalse(auditEvents.isEmpty());
    }

    @Test
    void shouldManageSessionLifecycle() throws Exception {
        // Create user and session
        User user = new User("session@example.com", "Session User");
        user.setOrganization(organizationRepository.findById(orgId).orElseThrow());
        user = userRepository.save(user);

        OAuth2UserInfo userInfo = new OAuth2UserInfo(
            "session-user-id",
            "test-provider",
            "session@example.com"
        );
        userInfo.setName("Session User");
        userInfo = userInfoRepository.save(userInfo);

        OAuth2Session session = new OAuth2Session(
            "session-lifecycle-123",
            userInfo,
            "test-provider",
            Instant.now().plus(1, ChronoUnit.HOURS)
        );
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
        assertFalse(terminatedSession.get().isValid());
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
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Malicious")));
    }

    @Test
    void shouldHandleTokenValidationAndExpiry() throws Exception {
        // Create expired session
        OAuth2UserInfo userInfo = new OAuth2UserInfo(
            "expired-user",
            "test-provider",
            "expired@example.com"
        );
        userInfo.setName("Expired User");
        userInfo = userInfoRepository.save(userInfo);

        OAuth2Session expiredSession = new OAuth2Session(
            "expired-session-123",
            userInfo,
            "test-provider",
            Instant.now().minus(1, ChronoUnit.HOURS) // Already expired
        );
        expiredSession = sessionRepository.save(expiredSession);

        // Try to use expired session - should return 200 with authenticated: false
        mockMvc.perform(get("/api/v1/auth/session")
                .header("Authorization", "Bearer expired-session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.user").doesNotExist());

        // Verify audit event for expired session - use repository query
        var auditEvents = auditEventRepository.findAll().stream()
            .filter(event -> event.getEventType() == OAuth2AuditEvent.OAuth2EventType.SESSION_EXPIRED)
            .toList();
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

        // Verify attempts were tracked - use a future end time to account for timing precision
        var attempts = attemptRepository.findByIpAddressAndTimeBetween(
            "192.168.1.100",
            Instant.now().minus(5, ChronoUnit.MINUTES),
            Instant.now().plus(1, ChronoUnit.MINUTES)
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
        Instant beforeLimitCheck = Instant.now().minus(1, ChronoUnit.MINUTES);
        mockMvc.perform(post("/api/v1/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rateLimitRequest)
                .header("X-Forwarded-For", "192.168.1.200"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("Too many authentication attempts. Please try again later."))
                .andExpect(header().string("X-RateLimit-Limit", "10"))
                .andExpect(header().string("Retry-After", String.valueOf(Duration.ofMinutes(5).toSeconds())));

        // Verify rate limit attempt logged for IP address
        var rateLimitAttempts = attemptRepository.findByIpAddressAndTimeBetween(
            "192.168.1.200",
            beforeLimitCheck,
            Instant.now().plus(1, ChronoUnit.MINUTES)
        );
        assertFalse(rateLimitAttempts.isEmpty());
        assertTrue(rateLimitAttempts.stream()
            .anyMatch(att -> att.getFailureReason() != null && att.getFailureReason().contains("RATE_LIMIT_EXCEEDED")));
    }

    @Test
    void shouldValidatePKCECodeChallenge() throws Exception {
        // Create session with PKCE challenge first to avoid 401 errors
        OAuth2UserInfo userInfo = new OAuth2UserInfo(
            "pkce-user-id",
            "test-provider",
            "pkce@example.com"
        );
        userInfo.setName("PKCE User");
        userInfo = userInfoRepository.save(userInfo);

        OAuth2Session session = new OAuth2Session(
            "session-123",
            userInfo,
            "test-provider",
            Instant.now().plus(1, ChronoUnit.HOURS)
        );
        session.setPkceCodeVerifierHash("expected-challenge");
        session.setOauth2StateHash("test-state");
        session = sessionRepository.save(session);

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PKCE_VALIDATION_FAILED"));

        // Verify PKCE validation audit event - use repository query
        var auditEvents = auditEventRepository.findAll().stream()
            .filter(event -> event.getEventType() == OAuth2AuditEvent.OAuth2EventType.PKCE_VALIDATION_FAILED)
            .toList();
        assertFalse(auditEvents.isEmpty());
    }

    @Test
    void shouldHandleUserInfoSynchronization() throws Exception {
        // Create session with outdated user info
        OAuth2UserInfo userInfo = new OAuth2UserInfo(
            "sync-user-id",
            "test-provider",
            "sync@example.com"
        );
        userInfo.setName("Old Name");
        userInfo.setLastUpdatedFromProvider(Instant.now().minus(30, ChronoUnit.DAYS));
        userInfo = userInfoRepository.save(userInfo);

        OAuth2Session session = new OAuth2Session(
            "sync-session-123",
            userInfo,
            "test-provider",
            Instant.now().plus(1, ChronoUnit.HOURS)
        );
        session = sessionRepository.save(session);

        // Request user info update
        mockMvc.perform(post("/api/v1/auth/sync")
                .header("Authorization", "Bearer sync-session-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true));

        // Verify user info sync audit event - use repository query
        var auditEvents = auditEventRepository.findAll().stream()
            .filter(event -> event.getEventType() == OAuth2AuditEvent.OAuth2EventType.USER_INFO_UPDATED)
            .toList();
        assertFalse(auditEvents.isEmpty());
    }
}
