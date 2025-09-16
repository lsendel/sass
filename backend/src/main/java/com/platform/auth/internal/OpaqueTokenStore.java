package com.platform.auth.internal;

import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.user.internal.User;
import com.platform.user.internal.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Opaque token storage implementation using database persistence.
 * Implements constitutional requirement for opaque tokens (no JWT).
 */
@Service
@Transactional
public class OpaqueTokenStore {

    private static final Logger logger = LoggerFactory.getLogger(OpaqueTokenStore.class);
    private static final int TOKEN_LENGTH = 32;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TokenMetadataRepository tokenMetadataRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public OpaqueTokenStore(TokenMetadataRepository tokenMetadataRepository,
                          PasswordEncoder passwordEncoder,
                          UserService userService) {
        this.tokenMetadataRepository = tokenMetadataRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    /**
     * Create a new opaque token for a user
     */
    public String createToken(UUID userId, String ipAddress, String userAgent, String oauthProvider) {
        // Generate secure random token
        String token = generateSecureToken();
        String salt = generateSalt();
        String tokenHash = hashToken(token, salt);

        // Set expiration (30 days for web sessions)
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);

        // Create token metadata
        TokenMetadata tokenMetadata = TokenMetadata.createOAuthSession(
            userId, tokenHash, salt, expiresAt, oauthProvider, ipAddress);

        if (userAgent != null) {
            tokenMetadata.addMetadata("userAgent", userAgent);
        }

        // Save to database
        tokenMetadataRepository.save(tokenMetadata);

        logger.info("Created new token for user: {} with provider: {}", userId, oauthProvider);
        return token;
    }

    /**
     * Create an API token with custom expiration
     */
    public String createApiToken(UUID userId, String apiKeyName, Instant expiresAt) {
        String token = generateSecureToken();
        String salt = generateSalt();
        String tokenHash = hashToken(token, salt);

        TokenMetadata tokenMetadata = TokenMetadata.createApiToken(
            userId, tokenHash, salt, expiresAt, apiKeyName);

        tokenMetadataRepository.save(tokenMetadata);

        logger.info("Created API token for user: {} with name: {}", userId, apiKeyName);
        return token;
    }

    /**
     * Validate a token and return user principal if valid
     */
    @Transactional(readOnly = true)
    public Optional<PlatformUserPrincipal> validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // Find token metadata by attempting to hash with all possible salts
            // This is secure because we're using the hash to find the record
            return tokenMetadataRepository.findValidTokens(Instant.now())
                .stream()
                .filter(metadata -> {
                    String expectedHash = hashToken(token, metadata.getSalt());
                    return metadata.getTokenHash().equals(expectedHash);
                })
                .findFirst()
                .map(this::updateLastUsedAndCreatePrincipal);

        } catch (Exception e) {
            logger.warn("Error validating token", e);
            return Optional.empty();
        }
    }

    /**
     * Revoke a specific token
     */
    public void revokeToken(String token) {
        validateToken(token).ifPresent(principal -> {
            TokenMetadata metadata = findTokenMetadata(token);
            if (metadata != null) {
                metadata.revoke();
                tokenMetadataRepository.save(metadata);
                logger.info("Revoked token for user: {}", principal.getUserId());
            }
        });
    }

    /**
     * Revoke all tokens for a user
     */
    public void revokeAllUserTokens(UUID userId) {
        int revokedCount = tokenMetadataRepository.revokeAllUserTokens(userId);
        logger.info("Revoked {} tokens for user: {}", revokedCount, userId);
    }

    /**
     * Revoke all expired tokens (cleanup job)
     */
    public void revokeExpiredTokens() {
        int revokedCount = tokenMetadataRepository.deleteExpiredTokens(Instant.now());
        if (revokedCount > 0) {
            logger.info("Cleaned up {} expired tokens", revokedCount);
        }
    }

    /**
     * Extend token expiration (for active sessions)
     */
    public void extendTokenExpiration(String token, Instant newExpiresAt) {
        TokenMetadata metadata = findTokenMetadata(token);
        if (metadata != null && metadata.isValid()) {
            metadata.extendExpiration(newExpiresAt);
            tokenMetadataRepository.save(metadata);
            logger.debug("Extended token expiration for user: {}", metadata.getUserId());
        }
    }

    /**
     * Get token information without validating
     */
    @Transactional(readOnly = true)
    public Optional<TokenMetadata> getTokenInfo(String token) {
        TokenMetadata metadata = findTokenMetadata(token);
        return Optional.ofNullable(metadata);
    }

    /**
     * Count active sessions for a user
     */
    @Transactional(readOnly = true)
    public long countActiveUserSessions(UUID userId) {
        return tokenMetadataRepository.countValidUserTokens(userId, Instant.now());
    }

    // Private helper methods

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String generateSalt() {
        byte[] saltBytes = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    private String hashToken(String token, String salt) {
        return passwordEncoder.encode(token + salt);
    }

    private TokenMetadata findTokenMetadata(String token) {
        return tokenMetadataRepository.findValidTokens(Instant.now())
            .stream()
            .filter(metadata -> {
                String expectedHash = hashToken(token, metadata.getSalt());
                return metadata.getTokenHash().equals(expectedHash);
            })
            .findFirst()
            .orElse(null);
    }

    private PlatformUserPrincipal updateLastUsedAndCreatePrincipal(TokenMetadata metadata) {
        // Update last used timestamp
        metadata.updateLastUsed();
        tokenMetadataRepository.save(metadata);

        // Fetch user details from UserService
        Optional<User> userOpt = userService.findById(metadata.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return PlatformUserPrincipal.systemUser(
                metadata.getUserId(),
                user.getEmail().getValue(), // Get email from User entity
                user.getName() // Get name from User entity
            );
        } else {
            // User not found - this should be rare but handle gracefully
            logger.warn("User not found for token metadata: {}", metadata.getUserId());
            return PlatformUserPrincipal.systemUser(
                metadata.getUserId(),
                "unknown@platform.com", // Fallback email
                "Unknown User" // Fallback name
            );
        }
    }
}