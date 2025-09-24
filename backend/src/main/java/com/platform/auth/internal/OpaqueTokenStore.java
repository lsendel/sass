package com.platform.auth.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationMember;
import com.platform.user.internal.OrganizationMemberRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserService;

/**
 * Opaque token storage implementation using database persistence. Implements constitutional
 * requirement for opaque tokens (no JWT).
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
  private final OrganizationMemberRepository memberRepository;

  public OpaqueTokenStore(
      TokenMetadataRepository tokenMetadataRepository,
      PasswordEncoder passwordEncoder,
      UserService userService,
      OrganizationMemberRepository memberRepository) {
    this.tokenMetadataRepository = tokenMetadataRepository;
    this.passwordEncoder = passwordEncoder;
    this.userService = userService;
    this.memberRepository = memberRepository;
  }

  /** Create a new opaque token for a user */
  public String createToken(UUID userId, String ipAddress, String userAgent, String oauthProvider) {
    // Generate secure random token
    String token = generateSecureToken();
    String salt = generateSalt();
    String tokenHash = hashToken(token, salt);
    String lookupHash = computeLookupHash(token);

    // Set expiration (30 days for web sessions)
    Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);

    // Create token metadata
    TokenMetadata tokenMetadata =
        TokenMetadata.createOAuthSession(
            userId, tokenHash, salt, expiresAt, oauthProvider, ipAddress);

    if (userAgent != null) {
      tokenMetadata.addMetadata("userAgent", userAgent);
    }

    tokenMetadata.setTokenLookupHash(lookupHash);

    // Save to database
    tokenMetadataRepository.save(tokenMetadata);

    logger.info("Created new token for user: {} with provider: {}", userId, oauthProvider);
    return token;
  }

  /** Create an API token with custom expiration */
  public String createApiToken(UUID userId, String apiKeyName, Instant expiresAt) {
    String token = generateSecureToken();
    String salt = generateSalt();
    String tokenHash = hashToken(token, salt);
    String lookupHash = computeLookupHash(token);

    TokenMetadata tokenMetadata =
        TokenMetadata.createApiToken(userId, tokenHash, salt, expiresAt, apiKeyName);

    tokenMetadata.setTokenLookupHash(lookupHash);

    tokenMetadataRepository.save(tokenMetadata);

    logger.info("Created API token for user: {} with name: {}", userId, apiKeyName);
    return token;
  }

  /** Validate a token and return user principal if valid */
  @Transactional
  public Optional<PlatformUserPrincipal> validateToken(String token) {
    return resolveTokenMetadata(token).map(this::updateLastUsedAndCreatePrincipal);
  }

  /** Revoke a specific token */
  public void revokeToken(String token) {
    resolveTokenMetadata(token)
        .ifPresent(
            metadata -> {
              metadata.revoke();
              tokenMetadataRepository.save(metadata);
              logger.info("Revoked token for user: {}", metadata.getUserId());
            });
  }

  /** Revoke all tokens for a user */
  public void revokeAllUserTokens(UUID userId) {
    int revokedCount = tokenMetadataRepository.revokeAllUserTokens(userId);
    logger.info("Revoked {} tokens for user: {}", revokedCount, userId);
  }

  /** Revoke all expired tokens (cleanup job) */
  public void revokeExpiredTokens() {
    int revokedCount = tokenMetadataRepository.deleteExpiredTokens(Instant.now());
    if (revokedCount > 0) {
      logger.info("Cleaned up {} expired tokens", revokedCount);
    }
  }

  /** Extend token expiration (for active sessions) */
  public void extendTokenExpiration(String token, Instant newExpiresAt) {
    resolveTokenMetadata(token)
        .ifPresent(
            metadata -> {
              if (metadata.isValid()) {
                metadata.extendExpiration(newExpiresAt);
                tokenMetadataRepository.save(metadata);
                logger.debug("Extended token expiration for user: {}", metadata.getUserId());
              }
            });
  }

  /** Get token information without validating */
  @Transactional
  public Optional<TokenMetadata> getTokenInfo(String token) {
    return resolveTokenMetadata(token);
  }

  /** Count active sessions for a user */
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

  private Optional<TokenMetadata> resolveTokenMetadata(String token) {
    if (token == null || token.trim().isEmpty()) {
      return Optional.empty();
    }

    try {
      Instant now = Instant.now();
      String lookupHash = computeLookupHash(token);

      Optional<TokenMetadata> directMatch =
          tokenMetadataRepository
              .findFirstByTokenLookupHashAndExpiresAtAfter(lookupHash, now)
              .filter(metadata -> matchesToken(token, metadata));

      if (directMatch.isPresent()) {
        return directMatch;
      }

      return tokenMetadataRepository.findValidTokens(now).stream()
          .filter(metadata -> matchesToken(token, metadata))
          .findFirst()
          .map(
              metadata -> {
                if (metadata.getTokenLookupHash() == null) {
                  metadata.setTokenLookupHash(lookupHash);
                  tokenMetadataRepository.save(metadata);
                }
                return metadata;
              });

    } catch (Exception e) {
      logger.warn("Error resolving token", e);
      return Optional.empty();
    }
  }

  private boolean matchesToken(String token, TokenMetadata metadata) {
    return passwordEncoder.matches(token + metadata.getSalt(), metadata.getTokenHash());
  }

  private String computeLookupHash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private PlatformUserPrincipal updateLastUsedAndCreatePrincipal(TokenMetadata metadata) {
    // Update last used timestamp
    metadata.updateLastUsed();
    tokenMetadataRepository.save(metadata);

    // Fetch user details from UserService
    Optional<User> userOpt = userService.findById(metadata.getUserId());
    if (userOpt.isEmpty()) {
      logger.warn("User not found for token metadata: {}", metadata.getUserId());
      return PlatformUserPrincipal.systemUser(
          metadata.getUserId(), "unknown@platform.com", "Unknown User");
    }

    User user = userOpt.get();
    Organization organization = user.getOrganization();

    if (organization == null) {
      return PlatformUserPrincipal.systemUser(
          metadata.getUserId(), user.getEmail().getValue(), user.getName());
    }

    String role = resolveMemberRole(metadata.getUserId(), organization.getId());
    return PlatformUserPrincipal.organizationMember(
        metadata.getUserId(),
        user.getEmail().getValue(),
        user.getName(),
        organization.getId(),
        organization.getSlug(),
        role);
  }

  private String resolveMemberRole(UUID userId, UUID organizationId) {
    if (organizationId == null) {
      return "MEMBER";
    }

    return memberRepository
        .findByOrganizationIdAndUserId(organizationId, userId)
        .map(OrganizationMember::getRole)
        .map(Enum::name)
        .orElse("MEMBER");
  }
}
