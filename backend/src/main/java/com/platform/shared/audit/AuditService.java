package com.platform.shared.audit;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Audit service interface for logging security and compliance events.
 * This interface provides the public API for the audit module.
 */
public interface AuditService {

    /**
     * Logs a security-related event with minimal information.
     * Used for rate limiting, authentication failures, and other security events.
     *
     * @param eventType    The type of security event
     * @param clientId     The client identifier (usually IP address)
     * @param description  Description of the security event
     */
    default void logSecurityEvent(String eventType, String clientId, String description) {
        logSecurityEvent(eventType, description, clientId, null, null);
    }

    /**
     * Logs a comprehensive security event with full context.
     *
     * @param eventType    The type of security event
     * @param description  Description of the security event
     * @param ipAddress    The client IP address
     * @param userAgent    The client user agent
     * @param metadata     Additional metadata about the event
     * @return CompletableFuture for async processing
     */
    CompletableFuture<Void> logSecurityEvent(
            String eventType,
            String description,
            String ipAddress,
            String userAgent,
            Map<String, String> metadata);

    /**
     * Logs a high-priority security event that requires immediate attention.
     * Used for account lockouts, security breaches, and other critical events.
     *
     * @param eventType    The type of security event
     * @param clientId     The client identifier (usually email address)
     * @param description  Description of the security event
     */
    default void logHighPrioritySecurityEvent(String eventType, String clientId, String description) {
        logSecurityEvent(eventType, description, clientId, null, Map.of("priority", "HIGH"));
    }

    /**
     * Logs a general audit event with request/response data.
     *
     * @param eventType     The type of event
     * @param resourceType  The type of resource being accessed
     * @param resourceId    The ID of the resource
     * @param action        The action being performed
     * @param requestData   Request data for the operation
     * @param responseData  Response data from the operation
     * @param ipAddress     The client IP address
     * @param userAgent     The client user agent
     * @param metadata      Additional metadata
     * @return CompletableFuture for async processing
     */
    CompletableFuture<Void> logEvent(
            String eventType,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> requestData,
            Map<String, Object> responseData,
            String ipAddress,
            String userAgent,
            Map<String, String> metadata);
}