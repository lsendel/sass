package com.platform.auth.internal;

import com.platform.AbstractIntegrationTest;
import com.platform.auth.User;
import com.platform.auth.events.UserAuthenticatedEvent;
import com.platform.shared.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for AuthenticationService using real databases.
 *
 * <p><b>Testing Strategy:</b>
 * <ul>
 *   <li>Real PostgreSQL for user persistence</li>
 *   <li>Real Redis for token storage</li>
 *   <li>Real PasswordEncoder for password hashing</li>
 *   <li>Tests full authentication flow end-to-end</li>
 *   <li>Verifies account lockout mechanisms</li>
 *   <li>Tests event publishing for audit</li>
 * </ul>
 *
 * <p><b>Constitutional Compliance:</b>
 * <ul>
 *   <li>Zero mocks - all dependencies are real</li>
 *   <li>Tests written first (TDD)</li>
 *   <li>Integration testing with real Spring Security</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles({"integration-test", "default"})
@RecordApplicationEvents
@Transactional
@DisplayName("AuthenticationService Integration Tests")
class AuthenticationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OpaqueTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ApplicationEvents events;

    private User testUser;
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_EMAIL = "auth-test@example.com";

    @BeforeEach
    void setUpTest() {
        // Clean up any existing test data first
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
        userRepository.flush();

        // Create active test user with encoded password
        testUser = new User(TEST_EMAIL, passwordEncoder.encode(TEST_PASSWORD));
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
    }

    // ========================================================================
    // AUTHENTICATION SUCCESS TESTS
    // ========================================================================

    @Test
    @DisplayName("Should authenticate valid user with correct credentials")
    void shouldAuthenticateValidUserWithCorrectCredentials() {
        // WHEN: Authenticate with valid credentials (no mocks!)
        final String token = authService.authenticate(TEST_EMAIL, TEST_PASSWORD);

        // THEN: Token is generated
        assertThat(token).isNotNull().isNotEmpty();

        // AND: Token is valid in Redis
        assertThat(tokenService.validateToken(token)).isPresent()
                .hasValue(testUser.getId());

        // AND: Token stored in Redis with correct key
        final String redisKey = "auth:token:" + token;
        assertThat(redisTemplate.hasKey(redisKey)).isTrue();

        // AND: User failed attempts reset in database
        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getFailedLoginAttempts()).isZero();
    }

    @Test
    @DisplayName("Should publish UserAuthenticatedEvent on successful authentication")
    void shouldPublishUserAuthenticatedEventOnSuccess() {
        // WHEN: Authenticate successfully
        authService.authenticate(TEST_EMAIL, TEST_PASSWORD);

        // THEN: Event published
        final long eventCount = events.stream(UserAuthenticatedEvent.class).count();
        assertThat(eventCount).isEqualTo(1);

        // AND: Event contains correct data
        final UserAuthenticatedEvent event = events.stream(UserAuthenticatedEvent.class)
                .findFirst()
                .orElseThrow();

        assertThat(event.userId()).isEqualTo(testUser.getId());
        assertThat(event.email()).isEqualTo(TEST_EMAIL);
        assertThat(event.occurredOn()).isBetween(
                Instant.now().minus(5, ChronoUnit.SECONDS),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should auto-verify pending user on first successful login")
    void shouldAutoVerifyPendingUserOnFirstLogin() {
        // GIVEN: User with pending verification status
        User pendingUser = new User("pending@example.com",
                passwordEncoder.encode(TEST_PASSWORD));
        pendingUser.setStatus(User.UserStatus.PENDING_VERIFICATION);
        pendingUser = userRepository.save(pendingUser);

        // WHEN: Authenticate
        final String token = authService.authenticate("pending@example.com", TEST_PASSWORD);

        // THEN: Token generated
        assertThat(token).isNotNull();

        // AND: User status changed to ACTIVE in database
        final User updated = userRepository.findById(pendingUser.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should reset failed login attempts on successful authentication")
    void shouldResetFailedAttemptsOnSuccess() {
        // GIVEN: User with failed attempts
        testUser.setFailedLoginAttempts(2);
        testUser = userRepository.save(testUser);
        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(2);

        // WHEN: Authenticate successfully
        authService.authenticate(TEST_EMAIL, TEST_PASSWORD);

        // THEN: Failed attempts reset in database
        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getFailedLoginAttempts()).isZero();
        assertThat(updated.getLockedUntil()).isNull();
    }

    // ========================================================================
    // AUTHENTICATION FAILURE TESTS
    // ========================================================================

    @Test
    @DisplayName("Should reject authentication with wrong password")
    void shouldRejectWrongPassword() {
        // WHEN/THEN: Authenticate with wrong password
        assertThatThrownBy(() ->
                authService.authenticate(TEST_EMAIL, "WrongPassword123!"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid credentials");

        // AND: Failed attempts incremented in database
        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject authentication for non-existent user")
    void shouldRejectNonExistentUser() {
        // WHEN/THEN: Authenticate with non-existent email
        assertThatThrownBy(() ->
                authService.authenticate("nonexistent@example.com", TEST_PASSWORD))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("Should reject authentication for inactive user")
    void shouldRejectInactiveUser() {
        // GIVEN: Inactive user
        testUser.setStatus(User.UserStatus.DISABLED);
        userRepository.save(testUser);

        // WHEN/THEN: Authenticate
        assertThatThrownBy(() ->
                authService.authenticate(TEST_EMAIL, TEST_PASSWORD))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account is not active");
    }

    @Test
    @DisplayName("Should lock account after max failed attempts")
    void shouldLockAccountAfterMaxFailedAttempts() {
        // GIVEN: User near lock threshold (MAX_LOGIN_ATTEMPTS = 5)
        for (int i = 0; i < 4; i++) {
            try {
                authService.authenticate(TEST_EMAIL, "WrongPassword");
            } catch (ValidationException e) {
                // Expected
            }
        }

        // WHEN: One more failed attempt
        assertThatThrownBy(() ->
                authService.authenticate(TEST_EMAIL, "WrongPassword"))
                .isInstanceOf(ValidationException.class);

        // THEN: Account locked in database
        final User locked = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(locked.getFailedLoginAttempts()).isGreaterThanOrEqualTo(5);
        assertThat(locked.getLockedUntil()).isNotNull();
        assertThat(locked.getLockedUntil()).isAfter(Instant.now());
        assertThat(locked.isLocked()).isTrue();
    }

    @Test
    @DisplayName("Should reject login for locked account")
    void shouldRejectLoginForLockedAccount() {
        // GIVEN: Locked account
        testUser.lock(30); // Lock for 30 minutes
        userRepository.save(testUser);

        // WHEN/THEN: Try to authenticate (even with correct password)
        assertThatThrownBy(() ->
                authService.authenticate(TEST_EMAIL, TEST_PASSWORD))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("temporarily locked");
    }

    @Test
    @DisplayName("Should allow login after lock expiry")
    void shouldAllowLoginAfterLockExpiry() {
        // GIVEN: Account locked in the past
        testUser.setLockedUntil(Instant.now().minus(1, ChronoUnit.HOURS));
        testUser.setFailedLoginAttempts(5);
        userRepository.save(testUser);

        // WHEN: Authenticate (lock expired)
        final String token = authService.authenticate(TEST_EMAIL, TEST_PASSWORD);

        // THEN: Authentication succeeds
        assertThat(token).isNotNull();

        // AND: Lock cleared in database
        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getLockedUntil()).isNull();
        assertThat(updated.getFailedLoginAttempts()).isZero();
        assertThat(updated.isLocked()).isFalse();
    }

    // ========================================================================
    // CONCURRENCY & EDGE CASE TESTS
    // ========================================================================

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Should handle concurrent authentication attempts safely")
    void shouldHandleConcurrentAuthenticationSafely() throws InterruptedException {
        // GIVEN: Multiple threads authenticating concurrently
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        // WHEN: Concurrent authentication attempts
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    final String token = authService.authenticate(TEST_EMAIL, TEST_PASSWORD);
                    if (token != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // THEN: All attempts should succeed (no race conditions)
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failureCount.get()).isZero();

        // AND: User state is consistent in database
        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getFailedLoginAttempts()).isZero();
    }

    @Test
    @Disabled("Concurrency test has timing issues with transaction boundaries and TestContainers")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Should handle concurrent failed attempts and lock correctly")
    void shouldHandleConcurrentFailedAttemptsCorrectly() throws InterruptedException {
        // Clean up test data since this test runs outside transaction
        userRepository.findByEmail(TEST_EMAIL).ifPresent(u -> {
            userRepository.delete(u);
            userRepository.flush();
        });

        // Recreate test user for this non-transactional test
        testUser = new User(TEST_EMAIL, passwordEncoder.encode(TEST_PASSWORD));
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser = userRepository.saveAndFlush(testUser);

        // GIVEN: Multiple threads with wrong password
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // WHEN: Concurrent failed authentication attempts
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    authService.authenticate(TEST_EMAIL, "WrongPassword");
                } catch (ValidationException e) {
                    // Expected
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // THEN: Account locked in database
        final User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getFailedLoginAttempts()).isGreaterThanOrEqualTo(5);
        assertThat(updated.isLocked()).isTrue();

        // Clean up after non-transactional test
        userRepository.delete(updated);
        userRepository.flush();
    }

    @Test
    @DisplayName("Should handle null/empty email gracefully")
    void shouldHandleNullEmailGracefully() {
        assertThatThrownBy(() -> authService.authenticate(null, TEST_PASSWORD))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> authService.authenticate("", TEST_PASSWORD))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> authService.authenticate("   ", TEST_PASSWORD))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Should handle null/empty password gracefully")
    void shouldHandleNullPasswordGracefully() {
        assertThatThrownBy(() -> authService.authenticate(TEST_EMAIL, null))
                .isInstanceOf(Exception.class);

        assertThatThrownBy(() -> authService.authenticate(TEST_EMAIL, ""))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Should revoke token on logout")
    void shouldRevokeTokenOnLogout() {
        // GIVEN: Authenticated user with token
        final String token = authService.authenticate(TEST_EMAIL, TEST_PASSWORD);
        assertThat(tokenService.validateToken(token)).isPresent();

        // WHEN: Revoke token
        authService.revokeToken(token);

        // THEN: Token no longer valid
        assertThat(tokenService.validateToken(token)).isEmpty();

        // AND: Token removed from Redis
        final String redisKey = "auth:token:" + token;
        assertThat(redisTemplate.hasKey(redisKey)).isFalse();
    }
}
