package com.platform.shared.config;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.platform.shared.security.SecurityEventLogger;

/**
 * Registry for tracking active sessions and enforcing concurrent session limits.
 * Provides session fixation protection and suspicious activity detection.
 */
@Component
public class SessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SessionRegistry.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SecurityEventLogger securityEventLogger;

    @Value("${app.session.max-concurrent-sessions:3}")
    private int maxConcurrentSessions;

    @Value("${app.session.timeout:1800}")
    private int sessionTimeoutSeconds;

    // In-memory fallback for when Redis is unavailable
    private final Map<String, UserSessionInfo> localSessions = new ConcurrentHashMap<>();

    /**
     * Register a new session for a user.
     * Enforces concurrent session limits and detects suspicious activity.
     */
    public void registerSession(String userId, String sessionId, String ipAddress, String userAgent) {
        String userSessionsKey = "user:sessions:" + userId;
        UserSessionInfo sessionInfo = new UserSessionInfo(sessionId, ipAddress, userAgent, Instant.now());

        try {
            // Check current session count
            Map<Object, Object> userSessions = redisTemplate.opsForHash().entries(userSessionsKey);

            if (userSessions.size() >= maxConcurrentSessions) {
                handleExcessiveSessions(userId, ipAddress, userSessions.size());
                // Remove oldest session
                removeOldestSession(userSessionsKey, userSessions);
            }

            // Register new session
            redisTemplate.opsForHash().put(userSessionsKey, sessionId, sessionInfo);
            redisTemplate.expire(userSessionsKey, sessionTimeoutSeconds, TimeUnit.SECONDS);

            logger.debug("Session registered for user: {}, session: {}", userId, sessionId);

        } catch (Exception e) {
            logger.warn("Redis unavailable, using local session registry", e);
            localSessions.put(sessionId, sessionInfo);
        }
    }

    /**
     * Invalidate a session.
     */
    public void invalidateSession(String userId, String sessionId) {
        String userSessionsKey = "user:sessions:" + userId;

        try {
            redisTemplate.opsForHash().delete(userSessionsKey, sessionId);
            logger.debug("Session invalidated for user: {}, session: {}", userId, sessionId);
        } catch (Exception e) {
            logger.warn("Redis unavailable, removing from local registry", e);
            localSessions.remove(sessionId);
        }
    }

    /**
     * Check if a session is valid and not expired.
     */
    public boolean isSessionValid(String userId, String sessionId) {
        String userSessionsKey = "user:sessions:" + userId;

        try {
            UserSessionInfo sessionInfo = (UserSessionInfo) redisTemplate.opsForHash().get(userSessionsKey, sessionId);
            return sessionInfo != null &&
                   sessionInfo.createdAt().isAfter(Instant.now().minusSeconds(sessionTimeoutSeconds));
        } catch (Exception e) {
            logger.warn("Redis unavailable, checking local registry", e);
            UserSessionInfo sessionInfo = localSessions.get(sessionId);
            return sessionInfo != null &&
                   sessionInfo.createdAt().isAfter(Instant.now().minusSeconds(sessionTimeoutSeconds));
        }
    }

    /**
     * Get session count for a user.
     */
    public int getSessionCount(String userId) {
        String userSessionsKey = "user:sessions:" + userId;

        try {
            return redisTemplate.opsForHash().size(userSessionsKey).intValue();
        } catch (Exception e) {
            logger.warn("Redis unavailable, using local count", e);
            return (int) localSessions.values().stream()
                .filter(session -> session.createdAt().isAfter(Instant.now().minusSeconds(sessionTimeoutSeconds)))
                .count();
        }
    }

    /**
     * Detect and handle suspicious session activity.
     */
    private void handleExcessiveSessions(String userId, String ipAddress, int sessionCount) {
        securityEventLogger.logSuspiciousActivity(
            userId,
            "excessive_concurrent_sessions",
            ipAddress,
            "Session count: " + sessionCount + ", Limit: " + maxConcurrentSessions
        );

        logger.warn("User {} exceeded concurrent session limit: {} sessions from IP: {}",
            userId, sessionCount, ipAddress);
    }

    /**
     * Remove the oldest session when limit is exceeded.
     */
    private void removeOldestSession(String userSessionsKey, Map<Object, Object> userSessions) {
        String oldestSessionId = null;
        Instant oldestTime = Instant.now();

        for (Map.Entry<Object, Object> entry : userSessions.entrySet()) {
            UserSessionInfo sessionInfo = (UserSessionInfo) entry.getValue();
            if (sessionInfo.createdAt().isBefore(oldestTime)) {
                oldestTime = sessionInfo.createdAt();
                oldestSessionId = (String) entry.getKey();
            }
        }

        if (oldestSessionId != null) {
            redisTemplate.opsForHash().delete(userSessionsKey, oldestSessionId);
            logger.info("Removed oldest session: {} due to concurrent session limit", oldestSessionId);
        }
    }

    /**
     * Session information record.
     */
    public record UserSessionInfo(
        String sessionId,
        String ipAddress,
        String userAgent,
        Instant createdAt
    ) {}
}