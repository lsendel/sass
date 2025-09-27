package com.platform.shared.security;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Centralized security event logging for audit trails.
 */
@Component
public class SecurityEventLogger {

    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void logAuthenticationSuccess(String userId, String method, String ipAddress) {
        logSecurityEvent("AUTH_SUCCESS", userId, method, ipAddress, null);
    }

    public void logAuthenticationFailure(String email, String method, String ipAddress, String reason) {
        logSecurityEvent("AUTH_FAILURE", email, method, ipAddress, reason);
    }

    public void logAccessDenied(String userId, String resource, String ipAddress) {
        logSecurityEvent("ACCESS_DENIED", userId, resource, ipAddress, null);
    }

    public void logSuspiciousActivity(String userId, String activity, String ipAddress, String details) {
        logSecurityEvent("SUSPICIOUS_ACTIVITY", userId, activity, ipAddress, details);
    }

    public void logPasswordChange(String userId, String ipAddress) {
        logSecurityEvent("PASSWORD_CHANGE", userId, "password", ipAddress, null);
    }

    public void logAccountLockout(String userId, String ipAddress, String reason) {
        logSecurityEvent("ACCOUNT_LOCKOUT", userId, "lockout", ipAddress, reason);
    }

    public void logSessionCreated(String userId, String sessionId, String ipAddress) {
        logSecurityEvent("SESSION_CREATED", userId, "session_created", ipAddress, "sessionId:" + sessionId);
    }

    public void logSessionExpired(String userId, String sessionId, String ipAddress) {
        logSecurityEvent("SESSION_EXPIRED", userId, "session_expired", ipAddress, "sessionId:" + sessionId);
    }

    public void logSessionDestroyed(String userId, String sessionId, String ipAddress) {
        logSecurityEvent("SESSION_DESTROYED", userId, "session_destroyed", ipAddress, "sessionId:" + sessionId);
    }

    private void logSecurityEvent(String eventType, String subject, String action, String ipAddress, String details) {
        try {
            SecurityEvent event = new SecurityEvent(
                eventType, subject, action, ipAddress, details, Instant.now()
            );
            securityLogger.info("SECURITY_EVENT: {}", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            securityLogger.error("Failed to log security event", e);
        }
    }

    public record SecurityEvent(
        String eventType,
        String subject,
        String action,
        String ipAddress,
        String details,
        Instant timestamp
    ) {}
}
