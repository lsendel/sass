package com.platform.integration;

import com.platform.auth.internal.OpaqueTokenStore;
import com.platform.auth.internal.SessionService;
import com.platform.auth.internal.TokenMetadata;
import com.platform.auth.internal.TokenMetadataRepository;
import com.platform.shared.types.Email;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Auth module using TestContainers.
 * Tests the complete authentication flow with real database and Redis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@DisplayName("Auth Module Integration Tests")
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("platform_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private OpaqueTokenStore tokenStore;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenMetadataRepository tokenMetadataRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Clean up test data
        tokenMetadataRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create and validate opaque token")
    void shouldCreateAndValidateOpaqueToken() {
        // Create a test user
        User user = new User(new Email("test@example.com"), "Test User", "google", "google123");
        User savedUser = userRepository.save(user);

        // Create token
        String token = tokenStore.createToken(
            savedUser.getId(),
            "192.168.1.1",
            "Mozilla/5.0",
            "google"
        );

        assertThat(token).isNotNull().isNotEmpty();

        // Validate token
        Optional<com.platform.shared.security.PlatformUserPrincipal> principal =
            tokenStore.validateToken(token);

        assertThat(principal).isPresent();
        assertThat(principal.get().getUserId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("Should handle OAuth2 authentication flow")
    void shouldHandleOAuth2AuthenticationFlow() {
        // Create mock OAuth2User
        OAuth2User oauth2User = new DefaultOAuth2User(
            List.of(),
            Map.of(
                "email", "oauth@example.com",
                "name", "OAuth User",
                "sub", "oauth123"
            ),
            "email"
        );

        // Handle OAuth2 authentication
        SessionService.AuthenticationResult result = sessionService.handleOAuth2Authentication(
            oauth2User,
            "192.168.1.1",
            "Mozilla/5.0"
        );

        assertThat(result.user()).isNotNull();
        assertThat(result.user().getEmail().getValue()).isEqualTo("oauth@example.com");
        assertThat(result.user().getName()).isEqualTo("OAuth User");
        assertThat(result.token()).isNotNull().isNotEmpty();

        // Verify user was created
        Optional<User> createdUser = userRepository.findByEmailAndDeletedAtIsNull("oauth@example.com");
        assertThat(createdUser).isPresent();
        assertThat(createdUser.get().getProvider()).isEqualTo("unknown"); // Default for mock
    }

    @Test
    @DisplayName("Should return available OAuth2 providers")
    void shouldReturnAvailableOAuth2Providers() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/auth/providers")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("providers", isA(List.class));
    }

    @Test
    @DisplayName("Should reject invalid OAuth2 provider")
    void shouldRejectInvalidOAuth2Provider() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "invalid-provider")
            .queryParam("redirect_uri", "http://localhost:3000/callback")
        .when()
            .get("/api/v1/auth/authorize")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("error", equalTo("INVALID_PROVIDER"));
    }

    @Test
    @DisplayName("Should require authentication for session endpoint")
    void shouldRequireAuthenticationForSessionEndpoint() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/v1/auth/session")
        .then()
            .statusCode(401)
            .contentType(ContentType.JSON)
            .body("error", equalTo("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Should return session info for authenticated user")
    void shouldReturnSessionInfoForAuthenticatedUser() {
        // Create user and token
        User user = new User(new Email("session@example.com"), "Session User", "google", "session123");
        User savedUser = userRepository.save(user);

        String token = tokenStore.createToken(
            savedUser.getId(),
            "192.168.1.1",
            "Mozilla/5.0",
            "google"
        );

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/auth/session")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("user", notNullValue())
            .body("user.id", notNullValue())
            .body("session", notNullValue())
            .body("session.activeTokens", greaterThan(0));
    }

    @Test
    @DisplayName("Should revoke token on logout")
    void shouldRevokeTokenOnLogout() {
        // Create user and token
        User user = new User(new Email("logout@example.com"), "Logout User", "google", "logout123");
        User savedUser = userRepository.save(user);

        String token = tokenStore.createToken(
            savedUser.getId(),
            "192.168.1.1",
            "Mozilla/5.0",
            "google"
        );

        // Verify token is valid
        assertThat(tokenStore.validateToken(token)).isPresent();

        // Logout
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
        .when()
            .post("/api/v1/auth/logout")
        .then()
            .statusCode(204);

        // Verify token is no longer valid
        assertThat(tokenStore.validateToken(token)).isEmpty();
    }

    @Test
    @DisplayName("Should clean up expired tokens")
    void shouldCleanupExpiredTokens() {
        // Create user
        User user = new User(new Email("cleanup@example.com"), "Cleanup User", "google", "cleanup123");
        User savedUser = userRepository.save(user);

        // Create expired token manually
        TokenMetadata expiredToken = TokenMetadata.createWebSession(
            savedUser.getId(),
            "expired_hash",
            "salt",
            Instant.now().minus(1, ChronoUnit.HOURS), // Expired 1 hour ago
            "192.168.1.1",
            "Mozilla/5.0"
        );
        tokenMetadataRepository.save(expiredToken);

        // Create valid token
        String validToken = tokenStore.createToken(
            savedUser.getId(),
            "192.168.1.1",
            "Mozilla/5.0",
            "google"
        );

        // Cleanup expired tokens
        sessionService.cleanupExpiredTokens();

        // Verify expired token is removed and valid token remains
        long tokenCount = tokenMetadataRepository.count();
        assertThat(tokenCount).isEqualTo(1); // Only the valid token should remain
        assertThat(tokenStore.validateToken(validToken)).isPresent();
    }

    @Test
    @DisplayName("Should count active user sessions")
    void shouldCountActiveUserSessions() {
        // Create user
        User user = new User(new Email("sessions@example.com"), "Sessions User", "google", "sessions123");
        User savedUser = userRepository.save(user);

        // Create multiple tokens
        String token1 = tokenStore.createToken(savedUser.getId(), "192.168.1.1", "Mozilla/5.0", "google");
        String token2 = tokenStore.createToken(savedUser.getId(), "192.168.1.2", "Chrome", "google");

        // Count sessions
        long sessionCount = tokenStore.countActiveUserSessions(savedUser.getId());
        assertThat(sessionCount).isEqualTo(2);

        // Revoke one token
        tokenStore.revokeToken(token1);

        // Count again
        sessionCount = tokenStore.countActiveUserSessions(savedUser.getId());
        assertThat(sessionCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle duplicate user registration")
    void shouldHandleDuplicateUserRegistration() {
        // Create first OAuth2User
        OAuth2User oauth2User1 = new DefaultOAuth2User(
            List.of(),
            Map.of(
                "email", "duplicate@example.com",
                "name", "First User",
                "sub", "duplicate123"
            ),
            "email"
        );

        // First authentication
        SessionService.AuthenticationResult result1 = sessionService.handleOAuth2Authentication(
            oauth2User1,
            "192.168.1.1",
            "Mozilla/5.0"
        );

        // Second OAuth2User with same email but different name
        OAuth2User oauth2User2 = new DefaultOAuth2User(
            List.of(),
            Map.of(
                "email", "duplicate@example.com",
                "name", "Updated User",
                "sub", "duplicate123"
            ),
            "email"
        );

        // Second authentication (should update existing user)
        SessionService.AuthenticationResult result2 = sessionService.handleOAuth2Authentication(
            oauth2User2,
            "192.168.1.1",
            "Mozilla/5.0"
        );

        // Should be same user with updated name
        assertThat(result1.user().getId()).isEqualTo(result2.user().getId());
        assertThat(result2.user().getName()).isEqualTo("Updated User");

        // Should only have one user in database
        long userCount = userRepository.count();
        assertThat(userCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should validate token security")
    void shouldValidateTokenSecurity() {
        // Create user and token
        User user = new User(new Email("security@example.com"), "Security User", "google", "security123");
        User savedUser = userRepository.save(user);

        String token = tokenStore.createToken(
            savedUser.getId(),
            "192.168.1.1",
            "Mozilla/5.0",
            "google"
        );

        // Token should be validated successfully
        assertThat(tokenStore.validateToken(token)).isPresent();

        // Invalid tokens should fail
        assertThat(tokenStore.validateToken("invalid-token")).isEmpty();
        assertThat(tokenStore.validateToken(token + "modified")).isEmpty();
        assertThat(tokenStore.validateToken("")).isEmpty();
        assertThat(tokenStore.validateToken(null)).isEmpty();
    }
}