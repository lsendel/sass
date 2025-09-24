package com.platform.auth.internal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing OAuth2 sessions using OAuth2-specific entities. Handles session lifecycle,
 * user information sync, and audit logging.
 *
 * <p>This service specifically manages OAuth2Session entities which are separate from the general
 * User/Session management in the platform.
 */
@Service
@Transactional
public class OAuth2SessionService {

  private static final Logger logger = LoggerFactory.getLogger(OAuth2SessionService.class);
  private static final long DEFAULT_SESSION_DURATION_HOURS = 24;

  private final OAuth2SessionRepository sessionRepository;
  private final OAuth2UserInfoRepository userInfoRepository;
  private final OAuth2ProviderRepository providerRepository;
  private final OAuth2AuditService auditService;

  public OAuth2SessionService(
      OAuth2SessionRepository sessionRepository,
      OAuth2UserInfoRepository userInfoRepository,
      OAuth2ProviderRepository providerRepository,
      OAuth2AuditService auditService) {
    this.sessionRepository = sessionRepository;
    this.userInfoRepository = userInfoRepository;
    this.providerRepository = providerRepository;
    this.auditService = auditService;
  }

  /** Create a new OAuth2 session from successful authentication */
  public OAuth2SessionResult createSession(
      String sessionId,
      OAuth2User oauth2User,
      String provider,
      String ipAddress,
      String userAgent) {

    // Validate provider is configured
    OAuth2Provider oauthProvider =
        providerRepository
            .findByNameAndEnabledTrue(provider)
            .orElseThrow(
                () -> new IllegalArgumentException("Provider not configured: " + provider));

    // Extract user information from OAuth2User
    String providerUserId = oauth2User.getName(); // OAuth2 subject ID
    String email = oauth2User.getAttribute("email");

    if (email == null || providerUserId == null) {
      auditService.logAuthenticationFailure(
          provider, "Missing required OAuth2 attributes", ipAddress);
      throw new IllegalArgumentException("OAuth2 user missing required email or subject ID");
    }

    try {
      // Find or create OAuth2UserInfo
      OAuth2UserInfo userInfo = findOrCreateUserInfo(oauth2User, provider, providerUserId, email);

      // Calculate session expiration
      Instant expiresAt = Instant.now().plus(DEFAULT_SESSION_DURATION_HOURS, ChronoUnit.HOURS);

      // Create OAuth2Session
      OAuth2Session session = new OAuth2Session(sessionId, userInfo, provider, expiresAt);
      session.setCreatedFromIp(ipAddress);
      session.setCreatedFromUserAgent(userAgent);
      session.setLastAccessedFromIp(ipAddress);

      // Set security hashes for audit purposes
      String authCodeHash = generateSecurityHash("auth_code", sessionId);
      String stateHash = generateSecurityHash("state", sessionId);
      session.setSecurityHashes(authCodeHash, null, stateHash);

      // Save session
      OAuth2Session savedSession = sessionRepository.save(session);

      // Log successful session creation
      auditService.logSessionCreated(provider, userInfo.getProviderUserId(), sessionId, ipAddress);

      logger.info(
          "Created OAuth2 session {} for user {} via provider {}",
          sessionId,
          userInfo.getProviderUserId(),
          provider);

      return new OAuth2SessionResult(
          savedSession.getSessionId(),
          savedSession.getExpiresAt(),
          OAuth2UserInfoView.fromEntity(userInfo),
          true);

    } catch (Exception e) {
      auditService.logAuthenticationFailure(
          provider, "Session creation failed: " + e.getMessage(), ipAddress);
      throw new OAuth2SessionException("Failed to create OAuth2 session", e);
    }
  }

  /** Validate and retrieve session information */
  @Transactional(readOnly = true)
  public Optional<OAuth2SessionInfo> getSessionInfo(String sessionId) {
    return sessionRepository
        .findBySessionIdAndIsActiveTrue(sessionId)
        .filter(OAuth2Session::isValid)
        .map(
            session -> {
              // Update last accessed timestamp
              session.updateLastAccessed();
              sessionRepository.save(session);

              return new OAuth2SessionInfo(
                  session.getSessionId(),
                  OAuth2UserInfoView.fromEntity(session.getUserInfo()),
                  session.getProvider(),
                  session.isValid(),
                  session.getExpiresAt(),
                  session.getLastAccessedAt(),
                  session.getSessionDurationSeconds(),
                  session.getTimeToExpirationSeconds());
            });
  }

  /** Terminate a specific session */
  public void terminateSession(String sessionId, String reason, String ipAddress) {
    Optional<OAuth2Session> sessionOpt =
        sessionRepository.findBySessionIdAndIsActiveTrue(sessionId);

    if (sessionOpt.isPresent()) {
      OAuth2Session session = sessionOpt.get();
      session.terminate(reason);
      sessionRepository.save(session);

      // Log session termination
      auditService.logSessionTerminated(
          session.getProvider(),
          session.getUserInfo().getProviderUserId(),
          sessionId,
          reason,
          ipAddress);

      logger.info("Terminated OAuth2 session {} for reason: {}", sessionId, reason);
    }
  }

  /** Terminate all sessions for a specific user */
  public int terminateAllUserSessions(String provider, String providerUserId, String reason) {
    Optional<OAuth2UserInfo> userInfoOpt =
        userInfoRepository.findByProviderUserIdAndProvider(providerUserId, provider);

    if (userInfoOpt.isEmpty()) {
      return 0;
    }

    List<OAuth2Session> activeSessions =
        sessionRepository.findByUserInfoAndIsActiveTrueAndTerminatedAtIsNull(userInfoOpt.get());

    for (OAuth2Session session : activeSessions) {
      session.terminate(reason);
    }

    sessionRepository.saveAll(activeSessions);

    // Log bulk session termination
    auditService.logUserSessionsTerminated(provider, providerUserId, activeSessions.size(), reason);

    logger.info(
        "Terminated {} OAuth2 sessions for user {} ({})",
        activeSessions.size(),
        providerUserId,
        provider);

    return activeSessions.size();
  }

  /** Extend session expiration time */
  public void extendSession(String sessionId, long additionalHours) {
    sessionRepository
        .findBySessionIdAndIsActiveTrue(sessionId)
        .filter(OAuth2Session::isValid)
        .ifPresent(
            session -> {
              Instant newExpiresAt = session.getExpiresAt().plus(additionalHours, ChronoUnit.HOURS);
              session.extendExpiration(newExpiresAt);
              sessionRepository.save(session);

              logger.debug("Extended OAuth2 session {} by {} hours", sessionId, additionalHours);
            });
  }

  /** Cleanup expired sessions */
  public int cleanupExpiredSessions() {
    List<OAuth2Session> expiredSessions = sessionRepository.findExpiredSessions(Instant.now());

    for (OAuth2Session session : expiredSessions) {
      session.terminate("expired");
    }

    sessionRepository.saveAll(expiredSessions);

    if (!expiredSessions.isEmpty()) {
      logger.info("Cleaned up {} expired OAuth2 sessions", expiredSessions.size());
    }

    return expiredSessions.size();
  }

  /** Get active session count for monitoring */
  @Transactional(readOnly = true)
  public long getActiveSessionCount() {
    return sessionRepository.countByIsActiveTrueAndExpiresAtAfter(Instant.now());
  }

  /** Get active session count for a specific provider */
  @Transactional(readOnly = true)
  public long getActiveSessionCountByProvider(String provider) {
    return sessionRepository.countByProviderAndIsActiveTrueAndExpiresAtAfter(
        provider, Instant.now());
  }

  // Private helper methods

  private OAuth2UserInfo findOrCreateUserInfo(
      OAuth2User oauth2User, String provider, String providerUserId, String email) {

    // Try to find existing user info
    Optional<OAuth2UserInfo> existingUserInfo =
        userInfoRepository.findByProviderUserIdAndProvider(providerUserId, provider);

    if (existingUserInfo.isPresent()) {
      // Update user info with fresh data from provider
      OAuth2UserInfo userInfo = existingUserInfo.get();
      updateUserInfoFromOAuth2User(userInfo, oauth2User);
      return userInfoRepository.save(userInfo);
    } else {
      // Create new user info
      OAuth2UserInfo newUserInfo = new OAuth2UserInfo(providerUserId, provider, email);
      updateUserInfoFromOAuth2User(newUserInfo, oauth2User);
      return userInfoRepository.save(newUserInfo);
    }
  }

  private void updateUserInfoFromOAuth2User(OAuth2UserInfo userInfo, OAuth2User oauth2User) {
    String email = oauth2User.getAttribute("email");
    String name = oauth2User.getAttribute("name");
    String givenName = oauth2User.getAttribute("given_name");
    String familyName = oauth2User.getAttribute("family_name");
    String picture = oauth2User.getAttribute("picture");
    String locale = oauth2User.getAttribute("locale");
    Boolean emailVerified = oauth2User.getAttribute("email_verified");

    // Convert OAuth2User attributes to JSON for raw storage
    String rawAttributes = convertAttributesToJson(oauth2User.getAttributes());

    userInfo.updateFromProvider(
        email, name, givenName, familyName, picture, locale, emailVerified, rawAttributes);
  }

  private String convertAttributesToJson(java.util.Map<String, Object> attributes) {
    // Simple JSON conversion - in production, use Jackson ObjectMapper
    StringBuilder json = new StringBuilder("{");
    boolean first = true;
    for (java.util.Map.Entry<String, Object> entry : attributes.entrySet()) {
      if (!first) json.append(",");
      json.append("\"")
          .append(entry.getKey())
          .append("\":\"")
          .append(String.valueOf(entry.getValue()))
          .append("\"");
      first = false;
    }
    json.append("}");
    return json.toString();
  }

  private String generateSecurityHash(String type, String value) {
    // Simple hash generation - in production, use proper cryptographic hash
    return String.valueOf((type + value + System.currentTimeMillis()).hashCode());
  }

  /** Result of OAuth2 session creation */
  public record OAuth2SessionResult(
      String sessionId, java.time.Instant expiresAt, OAuth2UserInfoView userInfo, boolean success) {}

  /** OAuth2 session information */
  public record OAuth2SessionInfo(
      String sessionId,
      OAuth2UserInfoView userInfo,
      String provider,
      boolean isValid,
      Instant expiresAt,
      Instant lastAccessedAt,
      long sessionDurationSeconds,
      long timeToExpirationSeconds) {}

  /** Exception for OAuth2 session operations */
  public static class OAuth2SessionException extends RuntimeException {
    public OAuth2SessionException(String message) {
      super(message);
    }

    public OAuth2SessionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
