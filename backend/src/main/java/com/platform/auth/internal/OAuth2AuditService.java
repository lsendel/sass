package com.platform.auth.internal;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for OAuth2 audit logging and compliance tracking. Provides comprehensive audit trails for
 * all OAuth2-related activities supporting GDPR compliance and forensic analysis.
 *
 * <p>This service creates detailed audit logs for: - OAuth2 authentication flows - Session
 * management events - Security incidents and violations - User consent and data processing
 * activities - Provider configuration changes
 */
@Service
@Transactional
public class OAuth2AuditService {

  private static final Logger logger = LoggerFactory.getLogger(OAuth2AuditService.class);

  private final OAuth2AuditEventRepository auditEventRepository;
  private final ApplicationEventPublisher eventPublisher;

  public OAuth2AuditService(
      OAuth2AuditEventRepository auditEventRepository, ApplicationEventPublisher eventPublisher) {
    this.auditEventRepository = auditEventRepository;
    this.eventPublisher = eventPublisher;
  }

  // Authentication Flow Events

  /** Log OAuth2 authorization started */
  public void logAuthorizationStarted(
      String provider, String userId, String sessionId, String ipAddress, String userAgent) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_STARTED,
            "OAuth2 authorization started for provider: " + provider);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.setRequestContext(ipAddress, userAgent, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "OAuth2 authorization started: user={}, provider={}, session={}",
        userId,
        provider,
        sessionId);
  }

  /** Log OAuth2 authorization completed successfully */
  public void logAuthorizationCompleted(
      String provider,
      String userId,
      String sessionId,
      String authCodeHash,
      String stateHash,
      String ipAddress,
      long durationMs) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_COMPLETED,
            "OAuth2 authorization completed successfully for provider: " + provider);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.setSecurityHashes(authCodeHash, stateHash);
    event.setDurationMs(durationMs);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "OAuth2 authorization completed: user={}, provider={}, duration={}ms",
        userId,
        provider,
        durationMs);
  }

  /** Log OAuth2 authorization failure */
  public void logAuthorizationFailure(
      String provider,
      String errorCode,
      String errorMessage,
      String userId,
      String sessionId,
      String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_FAILED,
            "OAuth2 authorization failed: " + errorMessage);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.markAsError(errorCode, errorMessage);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.warn(
        "OAuth2 authorization failed: user={}, provider={}, error={}", userId, provider, errorCode);
  }

  /** Log OAuth2 authorization denied by user */
  public void logAuthorizationDenied(String provider, String userId, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.AUTHORIZATION_DENIED,
            "OAuth2 authorization denied by user for provider: " + provider);

    event.setUserId(userId);
    event.setProvider(provider);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info("OAuth2 authorization denied: user={}, provider={}", userId, provider);
  }

  // Token Events

  /** Log OAuth2 token exchange started */
  public void logTokenExchangeStarted(
      String provider, String userId, String authCodeHash, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.TOKEN_EXCHANGE_STARTED,
            "OAuth2 token exchange started for provider: " + provider);

    event.setUserId(userId);
    event.setProvider(provider);
    event.setAuthorizationCodeHash(authCodeHash);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.debug("OAuth2 token exchange started: user={}, provider={}", userId, provider);
  }

  /** Log OAuth2 token exchange completed */
  public void logTokenExchangeCompleted(
      String provider, String userId, String ipAddress, long durationMs) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.TOKEN_EXCHANGE_COMPLETED,
            "OAuth2 token exchange completed for provider: " + provider);

    event.setUserId(userId);
    event.setProvider(provider);
    event.setDurationMs(durationMs);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.debug(
        "OAuth2 token exchange completed: user={}, provider={}, duration={}ms",
        userId,
        provider,
        durationMs);
  }

  /** Log OAuth2 token exchange failure */
  public void logTokenExchangeFailure(
      String provider, String userId, String errorCode, String errorMessage, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.TOKEN_EXCHANGE_FAILED,
            "OAuth2 token exchange failed: " + errorMessage);

    event.setUserId(userId);
    event.setProvider(provider);
    event.markAsError(errorCode, errorMessage);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.warn(
        "OAuth2 token exchange failed: user={}, provider={}, error={}",
        userId,
        provider,
        errorCode);
  }

  /** Log OAuth2 token validation failure */
  public void logTokenValidationFailure(
      String provider, String userId, String errorCode, String errorMessage, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.TOKEN_VALIDATION_FAILED,
            "OAuth2 token validation failed: " + errorMessage);

    event.setUserId(userId);
    event.setProvider(provider);
    event.markAsError(errorCode, errorMessage);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.warn(
        "OAuth2 token validation failed: user={}, provider={}, error={}",
        userId,
        provider,
        errorCode);
  }

  // Session Events

  /** Log OAuth2 session created */
  public void logSessionCreated(
      String provider, String userId, String sessionId, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.SESSION_CREATED,
            "OAuth2 session created for provider: " + provider);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "OAuth2 session created: user={}, provider={}, session={}", userId, provider, sessionId);
  }

  /** Log OAuth2 session renewed */
  public void logSessionRenewed(
      String provider, String userId, String sessionId, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.SESSION_RENEWED,
            "OAuth2 session renewed for provider: " + provider);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "OAuth2 session renewed: user={}, provider={}, session={}", userId, provider, sessionId);
  }

  /** Log OAuth2 session terminated */
  public void logSessionTerminated(
      String provider,
      String userId,
      String sessionId,
      String terminationReason,
      String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.SESSION_TERMINATED,
            "OAuth2 session terminated: " + terminationReason);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.setEventDetails("{\"terminationReason\":\"" + terminationReason + "\"}");
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "OAuth2 session terminated: user={}, provider={}, session={}, reason={}",
        userId,
        provider,
        sessionId,
        terminationReason);
  }

  /** Log multiple sessions terminated for a user */
  public void logUserSessionsTerminated(
      String provider, String userId, int sessionCount, String reason) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.SESSION_TERMINATED,
            "Terminated " + sessionCount + " OAuth2 sessions for user: " + reason);

    event.setUserId(userId);
    event.setProvider(provider);
    event.setEventDetails("{\"sessionCount\":" + sessionCount + ",\"reason\":\"" + reason + "\"}");

    saveAuditEvent(event);
    logger.info(
        "OAuth2 user sessions terminated: user={}, provider={}, count={}, reason={}",
        userId,
        provider,
        sessionCount,
        reason);
  }

  /** Log OAuth2 session expired */
  public void logSessionExpired(
      String provider, String userId, String sessionId, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.SESSION_EXPIRED,
            "OAuth2 session expired for provider: " + provider);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "OAuth2 session expired: user={}, provider={}, session={}", userId, provider, sessionId);
  }

  // User Events

  /** Log user login via OAuth2 */
  public void logUserLogin(
      String provider, String userId, String sessionId, String ipAddress, String userAgent) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.USER_LOGIN,
            "User logged in via OAuth2 provider: " + provider);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.setRequestContext(ipAddress, userAgent, generateCorrelationId());

    saveAuditEvent(event);
    logger.info("OAuth2 user login: user={}, provider={}, session={}", userId, provider, sessionId);
  }

  /** Log user logout */
  public void logUserLogout(String provider, String userId, String sessionId, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.USER_LOGOUT,
            "User logged out from OAuth2 provider: " + provider);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "OAuth2 user logout: user={}, provider={}, session={}", userId, provider, sessionId);
  }

  /** Log user information retrieved from provider */
  public void logUserInfoRetrieved(String provider, String userId, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.USER_INFO_RETRIEVED,
            "User information retrieved from OAuth2 provider: " + provider);

    event.setUserId(userId);
    event.setProvider(provider);
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.debug("OAuth2 user info retrieved: user={}, provider={}", userId, provider);
  }

  /** Log user information updated from provider */
  public void logUserInfoUpdated(
      String provider, String userId, String changedFields, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.USER_INFO_UPDATED,
            "User information updated from OAuth2 provider: " + provider);

    event.setUserId(userId);
    event.setProvider(provider);
    event.setEventDetails("{\"changedFields\":[" + changedFields + "]}");
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "OAuth2 user info updated: user={}, provider={}, fields={}",
        userId,
        provider,
        changedFields);
  }

  /** Log user information deleted (GDPR compliance) */
  public void logUserInfoDeleted(
      String provider, String userId, String requestedBy, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.USER_INFO_DELETED,
            "User information deleted from OAuth2 provider: " + provider + " (GDPR compliance)");

    event.setUserId(userId);
    event.setProvider(provider);
    event.setEventDetails(
        "{\"requestedBy\":\"" + requestedBy + "\",\"reason\":\"GDPR_compliance\"}");
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "OAuth2 user info deleted: user={}, provider={}, requestedBy={}",
        userId,
        provider,
        requestedBy);
  }

  // Security Events

  /** Log PKCE validation failure */
  public void logPkceValidationFailure(
      String provider, String userId, String sessionId, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.PKCE_VALIDATION_FAILED,
            "PKCE validation failed for provider: " + provider);

    event.setUserId(userId);
    event.setSessionId(sessionId);
    event.setProvider(provider);
    event.markAsCritical();
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.error(
        "OAuth2 PKCE validation failed: user={}, provider={}, session={}",
        userId,
        provider,
        sessionId);
  }

  /** Log state validation failure */
  public void logStateValidationFailure(
      String provider, String expectedState, String actualState, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.STATE_VALIDATION_FAILED,
            "OAuth2 state validation failed for provider: " + provider);

    event.setProvider(provider);
    event.markAsCritical();
    event.setEventDetails(
        "{\"expectedState\":\"" + expectedState + "\",\"actualState\":\"" + actualState + "\"}");
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.error("OAuth2 state validation failed: provider={}, ip={}", provider, ipAddress);
  }

  /** Log suspicious activity */
  public void logSuspiciousActivity(
      String provider, String description, String userId, String ipAddress, String userAgent) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.SUSPICIOUS_ACTIVITY,
            "Suspicious OAuth2 activity detected: " + description);

    event.setUserId(userId);
    event.setProvider(provider);
    event.markAsCritical();
    event.setEventDetails("{\"description\":\"" + description + "\"}");
    event.setRequestContext(ipAddress, userAgent, generateCorrelationId());

    saveAuditEvent(event);
    logger.error(
        "OAuth2 suspicious activity: user={}, provider={}, description={}",
        userId,
        provider,
        description);
  }

  // Provider Configuration Events

  /** Log OAuth2 provider configured */
  public void logProviderConfigured(String provider, String configuredBy) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.PROVIDER_CONFIGURED,
            "OAuth2 provider configured: " + provider);

    event.setProvider(provider);
    event.setEventDetails("{\"configuredBy\":\"" + configuredBy + "\"}");

    saveAuditEvent(event);
    logger.info("OAuth2 provider configured: provider={}, by={}", provider, configuredBy);
  }

  /** Log OAuth2 provider disabled */
  public void logProviderDisabled(String provider, String disabledBy, String reason) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.PROVIDER_DISABLED,
            "OAuth2 provider disabled: " + provider + " - " + reason);

    event.setProvider(provider);
    event.setEventDetails("{\"disabledBy\":\"" + disabledBy + "\",\"reason\":\"" + reason + "\"}");

    saveAuditEvent(event);
    logger.warn(
        "OAuth2 provider disabled: provider={}, by={}, reason={}", provider, disabledBy, reason);
  }

  // Convenience methods for common scenarios

  /** Log simple authentication failure */
  public void logAuthenticationFailure(String provider, String errorMessage, String ipAddress) {
    logAuthorizationFailure(provider, "AUTH_FAILED", errorMessage, null, null, ipAddress);
  }

  /** Log rate limit exceeded */
  public void logRateLimitExceeded(String provider, String userId, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.RATE_LIMIT_EXCEEDED,
            "Rate limit exceeded for OAuth2 provider: " + provider);

    event.setUserId(userId);
    event.setProvider(provider);
    event.markAsCritical();
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.warn(
        "OAuth2 rate limit exceeded: user={}, provider={}, ip={}", userId, provider, ipAddress);
  }

  // Query methods for audit analysis

  /** Get audit events for a specific user */
  @Transactional(readOnly = true)
  public List<OAuth2AuditEvent> getUserAuditEvents(
      String userId, Instant fromDate, Instant toDate) {
    return auditEventRepository.findByUserIdAndEventTimestampBetweenOrderByEventTimestampDesc(
        userId, fromDate, toDate);
  }

  /** Get audit events for a specific provider */
  @Transactional(readOnly = true)
  public List<OAuth2AuditEvent> getProviderAuditEvents(
      String provider, Instant fromDate, Instant toDate) {
    return auditEventRepository.findByProviderAndEventTimestampBetweenOrderByEventTimestampDesc(
        provider, fromDate, toDate);
  }

  /** Get critical security events */
  @Transactional(readOnly = true)
  public List<OAuth2AuditEvent> getCriticalSecurityEvents(Instant fromDate, Instant toDate) {
    return auditEventRepository.findBySeverityAndEventTimestampBetweenOrderByEventTimestampDesc(
        OAuth2AuditEvent.AuditSeverity.CRITICAL, fromDate, toDate);
  }

  /** Get failed authentication attempts from an IP */
  @Transactional(readOnly = true)
  public List<OAuth2AuditEvent> getFailedAuthenticationsByIp(
      String ipAddress, Instant fromDate, Instant toDate) {
    return auditEventRepository
        .findByIpAddressAndSuccessFalseAndEventTimestampBetweenOrderByEventTimestampDesc(
            ipAddress, fromDate, toDate);
  }

  // Performance and Analytics Methods

  /** Get event statistics by type for a time period */
  @Transactional(readOnly = true)
  public List<Object[]> getEventStatistics(Instant startTime, Instant endTime) {
    return auditEventRepository.getEventStatistics(startTime, endTime);
  }

  /** Get provider statistics including success rates and performance */
  @Transactional(readOnly = true)
  public List<Object[]> getProviderStatistics(Instant startTime, Instant endTime) {
    return auditEventRepository.getProviderStatistics(startTime, endTime);
  }

  /** Get performance metrics including percentiles for operations */
  @Transactional(readOnly = true)
  public List<Object[]> getPerformanceMetrics(Instant startTime, Instant endTime) {
    return auditEventRepository.getPerformanceMetrics(startTime, endTime);
  }

  /** Get suspicious activity patterns */
  @Transactional(readOnly = true)
  public List<Object[]> getSuspiciousActivityPatterns(Instant since, long threshold) {
    return auditEventRepository.findSuspiciousActivityPatterns(since, threshold);
  }

  /** Get GDPR-related events for a user */
  @Transactional(readOnly = true)
  public List<OAuth2AuditEvent> getGdprEventsForUser(String userId) {
    return auditEventRepository.findGdprEventsForUser(userId);
  }

  /** Count failed login attempts for a user within a time window */
  @Transactional(readOnly = true)
  public long countFailedLoginAttempts(String userId, Instant since) {
    return auditEventRepository.countFailedLoginAttempts(userId, since);
  }

  /** Count failed attempts from an IP address within a time window */
  @Transactional(readOnly = true)
  public long countFailedAttemptsFromIp(String ipAddress, Instant since) {
    return auditEventRepository.countFailedAttemptsFromIp(ipAddress, since);
  }

  // GDPR Compliance Events

  /** Log GDPR data export request */
  public void logGdprDataExport(
      String userId, String requestedBy, String dataTypes, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.GDPR_DATA_EXPORT,
            "GDPR data export requested for user: " + userId);

    event.setUserId(userId);
    event.setEventDetails(
        "{\"requestedBy\":\"" + requestedBy + "\",\"dataTypes\":[" + dataTypes + "]}");
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "GDPR data export: user={}, requestedBy={}, dataTypes={}", userId, requestedBy, dataTypes);
  }

  /** Log GDPR data deletion request */
  public void logGdprDataDeletion(
      String userId, String requestedBy, String deletionScope, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.GDPR_DATA_DELETION,
            "GDPR data deletion requested for user: " + userId);

    event.setUserId(userId);
    event.setEventDetails(
        "{\"requestedBy\":\"" + requestedBy + "\",\"deletionScope\":\"" + deletionScope + "\"}");
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "GDPR data deletion: user={}, requestedBy={}, scope={}",
        userId,
        requestedBy,
        deletionScope);
  }

  /** Log user consent granted */
  public void logConsentGranted(
      String provider, String userId, String consentType, String consentVersion, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.CONSENT_GRANTED,
            "User consent granted for " + consentType + " via provider: " + provider);

    event.setUserId(userId);
    event.setProvider(provider);
    event.setEventDetails(
        "{\"consentType\":\"" + consentType + "\",\"version\":\"" + consentVersion + "\"}");
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "Consent granted: user={}, provider={}, type={}, version={}",
        userId,
        provider,
        consentType,
        consentVersion);
  }

  /** Log user consent revoked */
  public void logConsentRevoked(
      String provider, String userId, String consentType, String reason, String ipAddress) {
    OAuth2AuditEvent event =
        new OAuth2AuditEvent(
            OAuth2AuditEvent.OAuth2EventType.CONSENT_REVOKED,
            "User consent revoked for " + consentType + " via provider: " + provider);

    event.setUserId(userId);
    event.setProvider(provider);
    event.setEventDetails(
        "{\"consentType\":\"" + consentType + "\",\"reason\":\"" + reason + "\"}");
    event.setRequestContext(ipAddress, null, generateCorrelationId());

    saveAuditEvent(event);
    logger.info(
        "Consent revoked: user={}, provider={}, type={}, reason={}",
        userId,
        provider,
        consentType,
        reason);
  }

  // Data Retention and Cleanup Methods

  /** Clean up old OAuth2 audit events according to retention policy */
  @Transactional
  public int cleanupOldAuditEvents(Instant cutoffDate) {
    try {
      // Log the cleanup operation
      OAuth2AuditEvent cleanupEvent =
          new OAuth2AuditEvent(
              OAuth2AuditEvent.OAuth2EventType.GDPR_DATA_DELETION,
              "OAuth2 audit event retention cleanup started");
      cleanupEvent.setEventDetails(
          "{\"cutoffDate\":\"" + cutoffDate + "\",\"retentionPolicy\":true}");
      saveAuditEvent(cleanupEvent);

      // Delete old events
      auditEventRepository.deleteByEventTimestampBefore(cutoffDate);

      logger.info("OAuth2 audit event cleanup completed: cutoff={}", cutoffDate);
      return 1; // Return count would require separate query

    } catch (Exception e) {
      logger.error("Failed to cleanup OAuth2 audit events", e);
      return 0;
    }
  }

  /** Clean up old OAuth2 audit events with default retention (7 years) */
  @Transactional
  public int cleanupOldAuditEvents() {
    Instant cutoffDate = Instant.now().minus(2555, java.time.temporal.ChronoUnit.DAYS); // 7 years
    return cleanupOldAuditEvents(cutoffDate);
  }

  // Private helper methods

  private void saveAuditEvent(OAuth2AuditEvent event) {
    try {
      OAuth2AuditEvent savedEvent = auditEventRepository.save(event);

      // Publish event for real-time monitoring
      eventPublisher.publishEvent(new OAuth2AuditEventCreated(savedEvent));

    } catch (Exception e) {
      // Log error but don't fail the operation
      logger.error("Failed to save OAuth2 audit event: {}", e.getMessage(), e);
    }
  }

  private String generateCorrelationId() {
    return java.util.UUID.randomUUID().toString();
  }

  /** Event published when an audit event is created */
  public record OAuth2AuditEventCreated(OAuth2AuditEvent auditEvent) {}
}
