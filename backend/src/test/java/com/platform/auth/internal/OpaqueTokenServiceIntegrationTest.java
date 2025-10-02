package com.platform.auth.internal;

import com.platform.AbstractIntegrationTest;
import com.platform.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OpaqueTokenService using real PostgreSQL and Redis.
 *
 * <p><b>Testing Strategy:</b>
 * <ul>
 *   <li>Real Redis via TestContainers - no mocks</li>
 *   <li>Real PostgreSQL via TestContainers for user data</li>
 *   <li>Tests token generation, validation, revocation, and cleanup</li>
 *   <li>Verifies tokens are correctly stored with expiration</li>
 * </ul>
 *
 * <p><b>Constitutional Compliance:</b>
 * <ul>
 *   <li>Zero mocks - all dependencies are real</li>
 *   <li>TDD approach - tests written first</li>
 *   <li>Integration testing with real databases</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles({"integration-test", "default"}) // Enable auth services
@Transactional
@DisplayName("OpaqueTokenService Integration Tests")
class OpaqueTokenServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OpaqueTokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private User testUser;

    @BeforeEach
    void setUpTest() {
        // Create a test user in the real PostgreSQL database
        testUser = new User("test@example.com", "hashedPassword123");
        testUser = userRepository.save(testUser);
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        // Clean up Redis before each test
        final String pattern = "auth:token:*";
        final var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ========================================================================
    // Token Generation Tests
    // ========================================================================

    @Test
    @DisplayName("Should generate token and store in Redis")
    void shouldGenerateTokenAndStoreInRedis() {
        // WHEN: Generate token (no mocks!)
        final String token = tokenService.generateToken(testUser);

        // THEN: Verify token generated
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token).hasSize(43); // Base64 URL-encoded 32 bytes

        // AND: Verify stored in Redis with correct key
        final String redisKey = "auth:token:" + token;
        final String storedUserId = redisTemplate.opsForValue().get(redisKey);
        assertThat(storedUserId).isEqualTo(testUser.getId().toString());

        // AND: Verify expiration is set (TTL should be ~24 hours = 86400 seconds)
        final Long ttl = redisTemplate.getExpire(redisKey, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isBetween(86000L, 86400L); // Allow small time drift
    }

    @Test
    @DisplayName("Should generate unique tokens for different users")
    void shouldGenerateUniqueTokensForDifferentUsers() {
        // GIVEN: Another user
        final User anotherUser = new User("another@example.com", "hash456");
        userRepository.save(anotherUser);

        // WHEN: Generate tokens for both users
        final String token1 = tokenService.generateToken(testUser);
        final String token2 = tokenService.generateToken(anotherUser);

        // THEN: Tokens are different
        assertThat(token1).isNotEqualTo(token2);

        // AND: Both stored correctly in Redis
        final String value1 = redisTemplate.opsForValue().get("auth:token:" + token1);
        final String value2 = redisTemplate.opsForValue().get("auth:token:" + token2);

        assertThat(value1).isEqualTo(testUser.getId().toString());
        assertThat(value2).isEqualTo(anotherUser.getId().toString());
    }

    @Test
    @DisplayName("Should generate different tokens for same user on multiple calls")
    void shouldGenerateDifferentTokensForSameUser() {
        // WHEN: Generate multiple tokens for same user
        final String token1 = tokenService.generateToken(testUser);
        final String token2 = tokenService.generateToken(testUser);
        final String token3 = tokenService.generateToken(testUser);

        // THEN: All tokens are unique
        assertThat(token1).isNotEqualTo(token2);
        assertThat(token2).isNotEqualTo(token3);
        assertThat(token1).isNotEqualTo(token3);

        // AND: All tokens map to same user in Redis
        final String value1 = redisTemplate.opsForValue().get("auth:token:" + token1);
        final String value2 = redisTemplate.opsForValue().get("auth:token:" + token2);
        final String value3 = redisTemplate.opsForValue().get("auth:token:" + token3);

        assertThat(value1).isEqualTo(testUser.getId().toString());
        assertThat(value2).isEqualTo(testUser.getId().toString());
        assertThat(value3).isEqualTo(testUser.getId().toString());
    }

    // ========================================================================
    // Token Validation Tests
    // ========================================================================

    @Test
    @DisplayName("Should validate valid token successfully")
    void shouldValidateValidTokenSuccessfully() {
        // GIVEN: Generated token
        final String token = tokenService.generateToken(testUser);

        // WHEN: Validate token (no mocks!)
        final Optional<UUID> result = tokenService.validateToken(token);

        // THEN: Token is valid
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        // WHEN: Validate null token
        final Optional<UUID> result = tokenService.validateToken(null);

        // THEN: Token is invalid
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        // WHEN: Validate empty token
        final Optional<UUID> result = tokenService.validateToken("");

        // THEN: Token is invalid
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should reject non-existent token")
    void shouldRejectNonExistentToken() {
        // WHEN: Validate token that doesn't exist in Redis
        final Optional<UUID> result = tokenService.validateToken("nonexistent-token-12345");

        // THEN: Token is invalid
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should extend token expiry on validation (sliding expiration)")
    void shouldExtendTokenExpiryOnValidation() throws InterruptedException {
        // GIVEN: Generated token
        final String token = tokenService.generateToken(testUser);
        final String redisKey = "auth:token:" + token;

        // AND: Get initial TTL
        final Long initialTtl = redisTemplate.getExpire(redisKey, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(initialTtl).isNotNull();
        assertThat(initialTtl).isBetween(86000L, 86400L); // Initial TTL is 24 hours

        // AND: Wait 3 seconds for TTL to decrease
        Thread.sleep(3000);

        // AND: Get decreased TTL before validation
        final Long ttlBeforeValidation = redisTemplate.getExpire(redisKey,
                java.util.concurrent.TimeUnit.SECONDS);
        assertThat(ttlBeforeValidation).isNotNull();
        assertThat(ttlBeforeValidation).isLessThan(initialTtl); // TTL decreased

        // WHEN: Validate token (should extend TTL back to 24 hours)
        tokenService.validateToken(token);

        // THEN: TTL should be refreshed (close to 24 hours again)
        final Long ttlAfterValidation = redisTemplate.getExpire(redisKey,
                java.util.concurrent.TimeUnit.SECONDS);
        assertThat(ttlAfterValidation).isNotNull();
        assertThat(ttlAfterValidation).isGreaterThan(ttlBeforeValidation); // TTL extended
        assertThat(ttlAfterValidation).isBetween(86000L, 86400L); // Back to ~24 hours
    }

    @Test
    @DisplayName("Should handle corrupted UUID in Redis gracefully")
    void shouldHandleCorruptedUuidInRedis() {
        // GIVEN: Token with invalid UUID stored in Redis
        final String token = "test-token-with-invalid-uuid";
        final String redisKey = "auth:token:" + token;
        redisTemplate.opsForValue().set(redisKey, "not-a-valid-uuid");

        // WHEN: Validate token
        final Optional<UUID> result = tokenService.validateToken(token);

        // THEN: Token is invalid
        assertThat(result).isEmpty();

        // AND: Corrupted data should be removed from Redis
        final String value = redisTemplate.opsForValue().get(redisKey);
        assertThat(value).isNull();
    }

    // ========================================================================
    // Token Revocation Tests
    // ========================================================================

    @Test
    @DisplayName("Should revoke token and remove from Redis")
    void shouldRevokeTokenAndRemoveFromRedis() {
        // GIVEN: Generated token
        final String token = tokenService.generateToken(testUser);
        final String redisKey = "auth:token:" + token;

        // AND: Verify token exists
        assertThat(redisTemplate.hasKey(redisKey)).isTrue();

        // WHEN: Revoke token (no mocks!)
        tokenService.revokeToken(token);

        // THEN: Token removed from Redis
        assertThat(redisTemplate.hasKey(redisKey)).isFalse();

        // AND: Token no longer validates
        final Optional<UUID> result = tokenService.validateToken(token);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle revocation of non-existent token gracefully")
    void shouldHandleRevocationOfNonExistentToken() {
        // WHEN: Revoke token that doesn't exist
        tokenService.revokeToken("non-existent-token");

        // THEN: No exception thrown (graceful handling)
        // Test passes if no exception
    }

    @Test
    @DisplayName("Should handle revocation of null token gracefully")
    void shouldHandleRevocationOfNullToken() {
        // WHEN: Revoke null token
        tokenService.revokeToken(null);

        // THEN: No exception thrown
        // Test passes if no exception
    }

    @Test
    @DisplayName("Should handle revocation of empty token gracefully")
    void shouldHandleRevocationOfEmptyToken() {
        // WHEN: Revoke empty token
        tokenService.revokeToken("");

        // THEN: No exception thrown
        // Test passes if no exception
    }

    // ========================================================================
    // Revoke All User Tokens Tests
    // ========================================================================

    @Test
    @DisplayName("Should revoke all tokens for a user")
    void shouldRevokeAllTokensForUser() {
        // GIVEN: Multiple tokens for same user
        final String token1 = tokenService.generateToken(testUser);
        final String token2 = tokenService.generateToken(testUser);
        final String token3 = tokenService.generateToken(testUser);

        // AND: Token for another user (should not be affected)
        final User anotherUser = new User("other@example.com", "hash");
        userRepository.save(anotherUser);
        final String otherToken = tokenService.generateToken(anotherUser);

        // AND: Verify all tokens exist
        assertThat(tokenService.validateToken(token1)).isPresent();
        assertThat(tokenService.validateToken(token2)).isPresent();
        assertThat(tokenService.validateToken(token3)).isPresent();
        assertThat(tokenService.validateToken(otherToken)).isPresent();

        // WHEN: Revoke all tokens for testUser
        tokenService.revokeAllUserTokens(testUser.getId());

        // THEN: All testUser tokens are invalid
        assertThat(tokenService.validateToken(token1)).isEmpty();
        assertThat(tokenService.validateToken(token2)).isEmpty();
        assertThat(tokenService.validateToken(token3)).isEmpty();

        // AND: Other user's token is still valid
        assertThat(tokenService.validateToken(otherToken)).isPresent();
    }

    @Test
    @DisplayName("Should handle revoking all tokens when user has no tokens")
    void shouldHandleRevokingAllTokensWhenUserHasNoTokens() {
        // GIVEN: User with no tokens
        final User userWithNoTokens = new User("notokens@example.com", "hash");
        userRepository.save(userWithNoTokens);

        // WHEN: Revoke all tokens
        tokenService.revokeAllUserTokens(userWithNoTokens.getId());

        // THEN: No exception thrown
        // Test passes if no exception
    }

    // ========================================================================
    // Edge Cases and Security Tests
    // ========================================================================

    @Test
    @DisplayName("Should generate cryptographically random tokens")
    void shouldGenerateCryptographicallyRandomTokens() {
        // WHEN: Generate many tokens
        final int tokenCount = 1000;
        final java.util.Set<String> tokens = new java.util.HashSet<>();

        for (int i = 0; i < tokenCount; i++) {
            final String token = tokenService.generateToken(testUser);
            tokens.add(token);
        }

        // THEN: All tokens are unique (no collisions)
        assertThat(tokens).hasSize(tokenCount);

        // AND: Each token has correct format (Base64 URL-encoded, no padding)
        tokens.forEach(token -> {
            assertThat(token).matches("[A-Za-z0-9_-]+");
            assertThat(token).doesNotContain("="); // No padding
            assertThat(token).doesNotContain("+");
            assertThat(token).doesNotContain("/");
        });
    }

    @Test
    @DisplayName("Should handle concurrent token generation safely")
    void shouldHandleConcurrentTokenGenerationSafely() throws InterruptedException {
        // GIVEN: Multiple threads generating tokens concurrently
        final int threadCount = 10;
        final int tokensPerThread = 10;
        final java.util.concurrent.ConcurrentHashMap<String, Boolean> allTokens =
                new java.util.concurrent.ConcurrentHashMap<>();

        // WHEN: Generate tokens concurrently
        final java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < tokensPerThread; j++) {
                    final String token = tokenService.generateToken(testUser);
                    allTokens.put(token, Boolean.TRUE);
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        // THEN: All tokens are unique (no race conditions)
        assertThat(allTokens).hasSize(threadCount * tokensPerThread);

        // AND: All tokens are valid
        allTokens.keySet().forEach(token -> {
            final Optional<UUID> result = tokenService.validateToken(token);
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(testUser.getId());
        });
    }
}
