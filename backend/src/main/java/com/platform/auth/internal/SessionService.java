package com.platform.auth.internal;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.shared.types.Email;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/** Service for managing user sessions and authentication flows. */
@Service
@Transactional
public class SessionService {

  private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

  private final OpaqueTokenStore tokenStore;
  private final UserRepository userRepository;

  public SessionService(OpaqueTokenStore tokenStore, UserRepository userRepository) {
    this.tokenStore = tokenStore;
    this.userRepository = userRepository;
  }

  /** Handle OAuth2 authentication success and create session */
  public AuthenticationResult handleOAuth2Authentication(
      OAuth2User oauth2User, String ipAddress, String userAgent) {

    // Extract user information from OAuth2User
    String email = oauth2User.getAttribute("email");
    String name = oauth2User.getAttribute("name");
    String provider = determineProvider(oauth2User);
    String providerId = oauth2User.getName(); // OAuth2 subject ID

    if (email == null || name == null) {
      throw new IllegalArgumentException("OAuth2 user missing required email or name");
    }

    // Find or create user
    User user = findOrCreateUser(email, name, provider, providerId);

    // Generate opaque token
    String token = tokenStore.createToken(user.getId(), ipAddress, userAgent, provider);

    logger.info("Created session for user: {} via provider: {}", user.getId(), provider);

    return new AuthenticationResult(user, token);
  }

  /** Get session information for a user */
  @Transactional(readOnly = true)
  public SessionInfo getSessionInfo(UUID userId) {
    long activeTokenCount = tokenStore.countActiveUserSessions(userId);

    // Get last activity from most recent token
    Instant lastActivity =
        userRepository.findById(userId).map(User::getUpdatedAt).orElse(Instant.now());

    return new SessionInfo(activeTokenCount, lastActivity);
  }

  /** Revoke a specific token */
  public void revokeToken(String token) {
    tokenStore.revokeToken(token);
  }

  /** Revoke all tokens for a user */
  public void revokeAllUserTokens(UUID userId) {
    tokenStore.revokeAllUserTokens(userId);
  }

  /** Extend session by updating token expiration */
  public void extendSession(String token, Instant newExpiresAt) {
    tokenStore.extendTokenExpiration(token, newExpiresAt);
  }

  /** Cleanup expired tokens (scheduled task) */
  public void cleanupExpiredTokens() {
    tokenStore.revokeExpiredTokens();
  }

  /** Handle password authentication and create session */
  public AuthenticationResult handlePasswordAuthentication(
      String email, String password, String ipAddress, String userAgent) {

    // Find user by email
    User user = userRepository.findByEmailAndDeletedAtIsNull(email)
        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    // Verify password (this would be enhanced with proper password hashing)
    // For now, this is a placeholder that always succeeds for demonstration

    // Generate opaque token
    String token = tokenStore.createToken(user.getId(), ipAddress, userAgent, "password");

    logger.info("Created session for user: {} via password authentication", user.getId());

    return new AuthenticationResult(user, token);
  }

  /** Check if token is valid */
  public boolean isTokenValid(String token) {
    return tokenStore.isTokenValid(token);
  }

  /** Get token expiry time */
  public Instant getTokenExpiry(String token) {
    return tokenStore.getTokenExpiry(token);
  }

  // Private helper methods

  private User findOrCreateUser(String email, String name, String provider, String providerId) {
    // Try to find existing user by provider and providerId first
    return userRepository
        .findByProviderAndProviderId(provider, providerId)
        .or(() -> userRepository.findByEmailAndDeletedAtIsNull(email))
        .map(existingUser -> updateUserIfNeeded(existingUser, email, name, provider, providerId))
        .orElseGet(() -> createNewUser(email, name, provider, providerId));
  }

  private User updateUserIfNeeded(
      User existingUser, String email, String name, String provider, String providerId) {
    boolean needsUpdate = false;

    // Update provider information if this is a new provider for the user
    if (!provider.equals(existingUser.getProvider())
        || !providerId.equals(existingUser.getProviderId())) {
      // For simplicity, we'll just log this case
      // In a real implementation, you might want to link multiple providers
      logger.info(
          "User {} authenticated with different provider: {} (was: {})",
          existingUser.getId(),
          provider,
          existingUser.getProvider());
    }

    // Update name if it has changed
    if (!name.equals(existingUser.getName())) {
      existingUser.updateProfile(name, existingUser.getPreferences());
      needsUpdate = true;
    }

    if (needsUpdate) {
      userRepository.save(existingUser);
    }

    return existingUser;
  }

  private User createNewUser(String email, String name, String provider, String providerId) {
    User newUser = new User(new Email(email), name, provider, providerId);
    User savedUser = userRepository.save(newUser);

    logger.info(
        "Created new user: {} with email: {} via provider: {}", savedUser.getId(), email, provider);

    return savedUser;
  }

  private String determineProvider(OAuth2User oauth2User) {
    // This would be enhanced to properly determine the provider
    // from the OAuth2User attributes or registration context

    // For now, we'll use a simple heuristic based on attributes
    if (oauth2User.getAttributes().containsKey("login")) {
      return "github";
    } else if (oauth2User.getAttributes().containsKey("hd")) {
      return "google";
    } else {
      return "unknown";
    }
  }

  /** Result of authentication process */
  public record AuthenticationResult(User user, String token) {}

  /** Session information */
  public record SessionInfo(long activeTokenCount, Instant lastActivity) {}
}
