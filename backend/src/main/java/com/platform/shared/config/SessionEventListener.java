package com.platform.shared.config;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;
import org.springframework.stereotype.Component;

import com.platform.shared.security.SecurityEventLogger;

/**
 * Session event listener for security monitoring and audit trails.
 * Tracks session lifecycle events for security analysis.
 */
@Component
public class SessionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionEventListener.class);

    @Autowired
    private SecurityEventLogger securityEventLogger;

    @EventListener
    public void handleSessionCreated(SessionCreatedEvent event) {
        String sessionId = event.getSessionId();
        Instant createdTime = event.getSession().getCreationTime();

        logger.debug("Session created: {} at {}", sessionId, createdTime);

        // Extract user information if available
        String userId = (String) event.getSession().getAttribute("userId");
        String ipAddress = (String) event.getSession().getAttribute("ipAddress");

        if (userId != null) {
            securityEventLogger.logSecurityEvent(
                SecurityEventLogger.SecurityEventType.SESSION_CREATED,
                userId,
                SecurityEventLogger.SecuritySeverity.INFO,
                ipAddress,
                null,
                Map.of(
                    "sessionId", sessionId,
                    "action", "session_created"
                )
            );
        }
    }

    @EventListener
    public void handleSessionExpired(SessionExpiredEvent event) {
        String sessionId = event.getSessionId();
        Instant expiredTime = Instant.now();

        logger.info("Session expired: {} at {}", sessionId, expiredTime);

        // Extract user information if available
        String userId = (String) event.getSession().getAttribute("userId");
        String ipAddress = (String) event.getSession().getAttribute("ipAddress");

        if (userId != null) {
            securityEventLogger.logSecurityEvent(
                SecurityEventLogger.SecurityEventType.SESSION_EXPIRED,
                userId,
                SecurityEventLogger.SecuritySeverity.INFO,
                ipAddress,
                null,
                Map.of(
                    "sessionId", sessionId,
                    "action", "session_expired"
                )
            );
        }
    }

    @EventListener
    public void handleSessionDeleted(SessionDeletedEvent event) {
        String sessionId = event.getSessionId();
        Instant deletedTime = Instant.now();

        logger.debug("Session deleted: {} at {}", sessionId, deletedTime);

        // Extract user information if available
        String userId = (String) event.getSession().getAttribute("userId");
        String ipAddress = (String) event.getSession().getAttribute("ipAddress");

        if (userId != null) {
            securityEventLogger.logSecurityEvent(
                SecurityEventLogger.SecurityEventType.SESSION_DESTROYED,
                userId,
                SecurityEventLogger.SecuritySeverity.INFO,
                ipAddress,
                null,
                Map.of(
                    "sessionId", sessionId,
                    "action", "session_deleted"
                )
            );
        }
    }
}