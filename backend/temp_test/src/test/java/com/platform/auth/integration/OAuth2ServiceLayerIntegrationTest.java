package com.platform.auth.integration;

import com.platform.auth.internal.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for OAuth2 service layer with real database and Redis dependencies.
 * Tests the service layer in isolation with actual data persistence.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class OAuth2ServiceLayerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private OAuth2UserService userService;

    @Autowired
    private OAuth2SessionService sessionService;

    @Autowired
    private OAuth2AuditService auditService;

    @Autowired
    private OAuth2UserInfoRepository userInfoRepository;

    @Autowired
    private OAuth2SessionRepository sessionRepository;

    @Autowired
    private OAuth2ProviderRepository providerRepository;

    @Autowired
    private OAuth2AuditEventRepository auditEventRepository;

    private OAuth2Provider testProvider;

    @BeforeEach
    void setUp() {
        // Create test OAuth2 provider
        testProvider = new OAuth2Provider(
            "google",
            "Google",
            "https://accounts.google.com/o/oauth2/v2/auth",
            "https://oauth2.googleapis.com/token",
            "https://www.googleapis.com/oauth2/v2/userinfo",
            List.of("openid", "email", "profile"),
            "test-client-id"
        );
        testProvider = providerRepository.save(testProvider);
    }

    @Test
    void createUserInfo_WithValidData_ShouldPersistUserInfo() {
        // ARRANGE
        String providerUserId = "google-user-123";
        String provider = "google";
        String email = "test@example.com";
        String name = "Test User";
        String givenName = "Test";
        String familyName = "User";
        String picture = "https://example.com/avatar.jpg";
        String locale = "en";
        Boolean emailVerified = true;
        String rawAttributes = "{\"sub\":\"google-user-123\",\"email\":\"test@example.com\"}";

        // ACT
        OAuth2UserInfo userInfo = userService.findOrCreateUserInfo(
            providerUserId, provider, email, name, givenName, familyName,
            picture, locale, emailVerified, rawAttributes
        );

        // ASSERT
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getId()).isNotNull();
        assertThat(userInfo.getProviderUserId()).isEqualTo(providerUserId);
        assertThat(userInfo.getProvider()).isEqualTo(provider);
        assertThat(userInfo.getEmail()).isEqualTo(email);
        assertThat(userInfo.getName()).isEqualTo(name);
        assertThat(userInfo.getGivenName()).isEqualTo(givenName);
        assertThat(userInfo.getFamilyName()).isEqualTo(familyName);
        assertThat(userInfo.getPicture()).isEqualTo(picture);
        assertThat(userInfo.getLocale()).isEqualTo(locale);
        assertThat(userInfo.isEmailVerified()).isEqualTo(emailVerified);
        assertThat(userInfo.getRawAttributes()).isEqualTo(rawAttributes);
        assertThat(userInfo.getCreatedAt()).isNotNull();
        assertThat(userInfo.getUpdatedAt()).isNotNull();
        assertThat(userInfo.getLastUpdatedFromProvider()).isNotNull();

        // Verify persistence
        Optional<OAuth2UserInfo> persisted = userInfoRepository.findByProviderUserIdAndProvider(
            providerUserId, provider);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getEmail()).isEqualTo(email);
    }

    @Test
    void findOrCreateUserInfo_ExistingUser_ShouldUpdateUserInfo() {
        // ARRANGE
        String providerUserId = "google-user-456";
        String provider = "google";
        String email = "update@example.com";

        // Create initial user info
        OAuth2UserInfo initialUserInfo = userService.findOrCreateUserInfo(
            providerUserId, provider, email, "Old Name", "Old", "Name",
            null, "en", false, "{\"old\":\"data\"}"
        );

        // ACT - Update with new information
        OAuth2UserInfo updatedUserInfo = userService.findOrCreateUserInfo(
            providerUserId, provider, email, "New Name", "New", "Name",
            "https://new-avatar.com/pic.jpg", "es", true, "{\"new\":\"data\"}"
        );

        // ASSERT
        assertThat(updatedUserInfo.getId()).isEqualTo(initialUserInfo.getId());
        assertThat(updatedUserInfo.getName()).isEqualTo("New Name");
        assertThat(updatedUserInfo.getGivenName()).isEqualTo("New");
        assertThat(updatedUserInfo.getFamilyName()).isEqualTo("Name");
        assertThat(updatedUserInfo.getPicture()).isEqualTo("https://new-avatar.com/pic.jpg");
        assertThat(updatedUserInfo.getLocale()).isEqualTo("es");
        assertThat(updatedUserInfo.isEmailVerified()).isTrue();
        assertThat(updatedUserInfo.getRawAttributes()).isEqualTo("{\"new\":\"data\"}");

        // Verify only one record exists
        List<OAuth2UserInfo> allUsers = userInfoRepository.findByEmailOrderByCreatedAtDesc(email);
        assertThat(allUsers).hasSize(1);
    }

    @Test
    void createSession_WithValidOAuth2User_ShouldCreateSessionAndUserInfo() {
        // ARRANGE
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-session-user-789");
        attributes.put("email", "session@example.com");
        attributes.put("name", "Session User");
        attributes.put("given_name", "Session");
        attributes.put("family_name", "User");
        attributes.put("picture", "https://example.com/session-avatar.jpg");
        attributes.put("locale", "fr");
        attributes.put("email_verified", true);

        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(() -> "ROLE_USER"),
            attributes,
            "sub"
        );

        String sessionId = "test-session-123";
        String provider = "google";
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0 Test Browser";

        // ACT
        OAuth2SessionService.OAuth2SessionResult result = sessionService.createSession(
            sessionId, oauth2User, provider, ipAddress, userAgent
        );

        // ASSERT
        assertThat(result.success()).isTrue();
        assertThat(result.session()).isNotNull();
        assertThat(result.userInfo()).isNotNull();

        OAuth2Session session = result.session();
        assertThat(session.getSessionId()).isEqualTo(sessionId);
        assertThat(session.getProvider()).isEqualTo(provider);
        assertThat(session.getCreatedFromIp()).isEqualTo(ipAddress);
        assertThat(session.getCreatedFromUserAgent()).isEqualTo(userAgent);
        assertThat(session.isValid()).isTrue();
        assertThat(session.getExpiresAt()).isAfter(Instant.now());

        OAuth2UserInfo userInfo = result.userInfo();
        assertThat(userInfo.getProviderUserId()).isEqualTo("google-session-user-789");
        assertThat(userInfo.getEmail()).isEqualTo("session@example.com");
        assertThat(userInfo.getName()).isEqualTo("Session User");
        assertThat(userInfo.isEmailVerified()).isTrue();

        // Verify persistence
        Optional<OAuth2Session> persistedSession = sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);
        assertThat(persistedSession).isPresent();
        assertThat(persistedSession.get().getUserInfo().getEmail()).isEqualTo("session@example.com");
    }

    @Test
    void getSessionInfo_WithValidSessionId_ShouldReturnSessionInfo() {
        // ARRANGE
        // Create user info first
        OAuth2UserInfo userInfo = userService.findOrCreateUserInfo(
            "test-user-999", "google", "sessioninfo@example.com",
            "Session Info User", "Session", "Info", null, "en", true, "{}"
        );

        // Create session
        Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour
        OAuth2Session session = new OAuth2Session("session-info-test", userInfo, "google", expiresAt);
        session.setCreatedFromIp("10.0.0.1");
        session.setCreatedFromUserAgent("Test Agent");
        OAuth2Session savedSession = sessionRepository.save(session);

        // ACT
        Optional<OAuth2SessionService.OAuth2SessionInfo> sessionInfoOpt =
            sessionService.getSessionInfo(savedSession.getSessionId());

        // ASSERT
        assertThat(sessionInfoOpt).isPresent();

        OAuth2SessionService.OAuth2SessionInfo sessionInfo = sessionInfoOpt.get();
        assertThat(sessionInfo.sessionId()).isEqualTo(savedSession.getSessionId());
        assertThat(sessionInfo.provider()).isEqualTo("google");
        assertThat(sessionInfo.isValid()).isTrue();
        assertThat(sessionInfo.userInfo().getEmail()).isEqualTo("sessioninfo@example.com");
        assertThat(sessionInfo.timeToExpirationSeconds()).isGreaterThan(3500); // Should be close to 1 hour
    }

    @Test
    void terminateSession_WithValidSessionId_ShouldTerminateSession() {
        // ARRANGE
        OAuth2UserInfo userInfo = userService.findOrCreateUserInfo(
            "test-terminate-user", "google", "terminate@example.com",
            "Terminate User", null, null, null, null, null, "{}"
        );

        OAuth2Session session = new OAuth2Session("terminate-session", userInfo, "google",
            Instant.now().plusSeconds(3600));
        OAuth2Session savedSession = sessionRepository.save(session);

        assertThat(savedSession.isValid()).isTrue();

        // ACT
        sessionService.terminateSession(savedSession.getSessionId(), "test_termination", "10.0.0.2");

        // ASSERT
        Optional<OAuth2Session> terminatedSession = sessionRepository.findById(savedSession.getId());
        assertThat(terminatedSession).isPresent();
        assertThat(terminatedSession.get().getIsActive()).isFalse();
        assertThat(terminatedSession.get().getTerminationReason()).isEqualTo("test_termination");
        assertThat(terminatedSession.get().getTerminatedAt()).isNotNull();
        assertThat(terminatedSession.get().isValid()).isFalse();
    }

    @Test
    void auditService_ShouldLogEventsCorrectly() {
        // ARRANGE
        String provider = "google";
        String userId = "audit-test-user";
        String sessionId = "audit-session-123";
        String ipAddress = "192.168.1.200";
        String userAgent = "Audit Test Browser";

        // ACT
        auditService.logAuthorizationStarted(provider, userId, sessionId, ipAddress, userAgent);
        auditService.logUserLogin(provider, userId, sessionId, ipAddress, userAgent);
        auditService.logSessionCreated(provider, userId, sessionId, ipAddress);
        auditService.logAuthorizationCompleted(provider, userId, sessionId, "auth-hash", "state-hash", ipAddress, 250L);

        // ASSERT
        List<OAuth2AuditEvent> events = auditEventRepository.findByUserIdAndSessionIdOrderByEventTimestampDesc(
            userId, sessionId);

        assertThat(events).hasSize(4);

        // Verify event types are logged
        assertThat(events).extracting(OAuth2AuditEvent::getEventType)
            .contains(
                OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_STARTED,
                OAuth2AuditEvent.OAuth2EventType.USER_LOGIN,
                OAuth2AuditEvent.OAuth2EventType.SESSION_CREATED,
                OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_COMPLETED
            );

        // Verify event details
        OAuth2AuditEvent authStartedEvent = events.stream()
            .filter(e -> e.getEventType() == OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_STARTED)
            .findFirst()
            .orElseThrow();

        assertThat(authStartedEvent.getProvider()).isEqualTo(provider);
        assertThat(authStartedEvent.getUserId()).isEqualTo(userId);
        assertThat(authStartedEvent.getSessionId()).isEqualTo(sessionId);
        assertThat(authStartedEvent.getIpAddress()).isEqualTo(ipAddress);
        assertThat(authStartedEvent.getUserAgent()).isEqualTo(userAgent);
        assertThat(authStartedEvent.getSuccess()).isTrue();
    }

    @Test
    void userService_GetUserStats_ShouldReturnCorrectStats() {
        // ARRANGE
        // Create multiple users with different verification states
        userService.findOrCreateUserInfo("user1", "google", "user1@example.com",
            "User One", null, null, "https://pic1.com", null, true, "{}");
        userService.findOrCreateUserInfo("user2", "google", "user2@example.com",
            "User Two", null, null, null, null, false, "{}");
        userService.findOrCreateUserInfo("user3", "github", "user3@example.com",
            "User Three", null, null, "https://pic3.com", null, true, "{}");

        // ACT
        OAuth2UserService.OAuth2UserStats stats = userService.getUserStats();

        // ASSERT
        assertThat(stats.totalUsers()).isEqualTo(3);
        assertThat(stats.verifiedEmailUsers()).isEqualTo(2);
        assertThat(stats.usersWithPictures()).isEqualTo(2);
    }

    @Test
    void sessionService_CleanupExpiredSessions_ShouldRemoveExpiredSessions() {
        // ARRANGE
        OAuth2UserInfo userInfo = userService.findOrCreateUserInfo(
            "cleanup-user", "google", "cleanup@example.com",
            "Cleanup User", null, null, null, null, null, "{}"
        );

        // Create expired session
        OAuth2Session expiredSession = new OAuth2Session("expired-session", userInfo, "google",
            Instant.now().minusSeconds(3600)); // Expired 1 hour ago
        sessionRepository.save(expiredSession);

        // Create valid session
        OAuth2Session validSession = new OAuth2Session("valid-session", userInfo, "google",
            Instant.now().plusSeconds(3600)); // Expires in 1 hour
        sessionRepository.save(validSession);

        // ACT
        int cleanedUp = sessionService.cleanupExpiredSessions();

        // ASSERT
        assertThat(cleanedUp).isEqualTo(1);

        // Verify expired session is terminated
        Optional<OAuth2Session> expiredSessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue("expired-session");
        assertThat(expiredSessionOpt).isEmpty();

        // Verify valid session is still active
        Optional<OAuth2Session> validSessionOpt = sessionRepository.findBySessionIdAndIsActiveTrue("valid-session");
        assertThat(validSessionOpt).isPresent();
        assertThat(validSessionOpt.get().isValid()).isTrue();
    }

    @Test
    void userService_SearchUsers_ShouldFindUsersByNameOrEmail() {
        // ARRANGE
        userService.findOrCreateUserInfo("search1", "google", "john.doe@example.com",
            "John Doe", "John", "Doe", null, null, null, "{}");
        userService.findOrCreateUserInfo("search2", "google", "jane.smith@example.com",
            "Jane Smith", "Jane", "Smith", null, null, null, "{}");
        userService.findOrCreateUserInfo("search3", "github", "bob.johnson@example.com",
            "Bob Johnson", "Bob", "Johnson", null, null, null, "{}");

        // ACT & ASSERT
        List<OAuth2UserInfo> searchByName = userService.searchUsers("john", 10);
        assertThat(searchByName).hasSize(2); // John Doe and Bob Johnson (contains "john")

        List<OAuth2UserInfo> searchByEmail = userService.searchUsers("jane.smith", 10);
        assertThat(searchByEmail).hasSize(1);
        assertThat(searchByEmail.get(0).getEmail()).isEqualTo("jane.smith@example.com");

        List<OAuth2UserInfo> searchByPartialEmail = userService.searchUsers("@example.com", 10);
        assertThat(searchByPartialEmail).hasSize(3); // All users have @example.com
    }
}