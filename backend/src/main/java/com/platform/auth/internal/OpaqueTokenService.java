package com.platform.auth.internal;

import com.platform.auth.User;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing opaque authentication tokens.
 * Tokens are stored in Redis with expiration for security and performance.
 * Only active in non-test profiles.
 *
 * <p>Constitutional requirement: Opaque tokens only, no JWT.
 *
 * @since 1.0.0
 */
@Service
@Profile("!test & !integration-test") // Disable in test profiles
final class OpaqueTokenService {

        private static final String TOKEN_PREFIX = "auth:token:";
        private static final int TOKEN_LENGTH_BYTES = 32;
        private static final Duration TOKEN_EXPIRY = Duration.ofHours(24);

        private final RedisTemplate<String, String> redisTemplate;
        private final SecureRandom secureRandom;

        /**
         * Constructor with dependency injection.
         *
         * @param redisTemplate Redis template for token storage
         */
        OpaqueTokenService(final RedisTemplate<String, String> redisTemplate) {
                this.redisTemplate = redisTemplate;
                this.secureRandom = new SecureRandom();
        }

        /**
         * Generates a new opaque token for the user.
         * Token is stored in Redis with sliding expiration.
         *
         * @param user the authenticated user
         * @return the generated token
         */
        public String generateToken(final User user) {
                final String token = generateRandomToken();
                final String key = TOKEN_PREFIX + token;

                redisTemplate.opsForValue().set(
                        key,
                        user.getId().toString(),
                        TOKEN_EXPIRY
                );

                return token;
        }

        /**
         * Validates a token and returns the associated user ID.
         * Extends token expiry on successful validation (sliding expiration).
         *
         * @param token the token to validate
         * @return optional containing user ID if token is valid
         */
        public Optional<UUID> validateToken(final String token) {
                if (token == null || token.isEmpty()) {
                        return Optional.empty();
                }

                final String key = TOKEN_PREFIX + token;
                final String userId = redisTemplate.opsForValue().get(key);

                if (userId == null) {
                        return Optional.empty();
                }

                // Sliding expiration - extend token life on use
                redisTemplate.expire(key, TOKEN_EXPIRY);

                try {
                        return Optional.of(UUID.fromString(userId));
                } catch (IllegalArgumentException e) {
                        // Invalid UUID in Redis, remove corrupted data
                        redisTemplate.delete(key);
                        return Optional.empty();
                }
        }

        /**
         * Revokes a token by removing it from Redis.
         * Used during logout.
         *
         * @param token the token to revoke
         */
        public void revokeToken(final String token) {
                if (token != null && !token.isEmpty()) {
                        final String key = TOKEN_PREFIX + token;
                        redisTemplate.delete(key);
                }
        }

        /**
         * Revokes all tokens for a user.
         * Used when password changes or account is compromised.
         *
         * @param userId the user ID
         */
        public void revokeAllUserTokens(final UUID userId) {
                final String pattern = TOKEN_PREFIX + "*";
                final var keys = redisTemplate.keys(pattern);

                if (keys != null) {
                        keys.stream()
                                .filter(key -> {
                                        final String value = redisTemplate.opsForValue().get(key);
                                        return userId.toString().equals(value);
                                })
                                .forEach(redisTemplate::delete);
                }
        }

        /**
         * Generates a cryptographically secure random token.
         *
         * @return the generated token as Base64-encoded string
         */
        private String generateRandomToken() {
                final byte[] bytes = new byte[TOKEN_LENGTH_BYTES];
                secureRandom.nextBytes(bytes);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
}
