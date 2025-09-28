package com.platform.audit.api.dto;

import java.time.Instant;

/**
 * DTO representing a single audit log entry for API responses.
 */
public record AuditLogEntryDTO(
    String id,
    Instant timestamp,
    String actorName,
    String actorType,
    String actionType,
    String actionDescription,
    String resourceType,
    String resourceName,
    String outcome,
    String sensitivity
) {
    // Action types
    public static class ActionType {
        public static final String LOGIN = "LOGIN";
        public static final String LOGOUT = "LOGOUT";
        public static final String CREATE = "CREATE";
        public static final String UPDATE = "UPDATE";
        public static final String DELETE = "DELETE";
        public static final String VIEW = "VIEW";
        public static final String EXPORT = "EXPORT";
        public static final String PAYMENT_CREATED = "PAYMENT_CREATED";
        public static final String SUBSCRIPTION_CREATED = "SUBSCRIPTION_CREATED";
        public static final String SUBSCRIPTION_MODIFIED = "SUBSCRIPTION_MODIFIED";
    }

    // Actor types
    public static class ActorType {
        public static final String USER = "USER";
        public static final String SYSTEM = "SYSTEM";
        public static final String SERVICE = "SERVICE";
    }

    // Resource types
    public static class ResourceType {
        public static final String USER = "USER";
        public static final String ORGANIZATION = "ORGANIZATION";
        public static final String PAYMENT = "PAYMENT";
        public static final String SUBSCRIPTION = "SUBSCRIPTION";
        public static final String AUDIT_LOG = "AUDIT_LOG";
        public static final String AUTHENTICATION = "AUTHENTICATION";
    }

    // Outcomes
    public static class Outcome {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";
        public static final String PARTIAL = "PARTIAL";
    }

    // Sensitivity levels
    public static class SensitivityLevel {
        public static final String PUBLIC = "PUBLIC";
        public static final String INTERNAL = "INTERNAL";
        public static final String CONFIDENTIAL = "CONFIDENTIAL";
    }
}