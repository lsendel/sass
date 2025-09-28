package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for audit log entries visible to end users.
 * This DTO represents the basic audit log information shown in the list view.
 *
 * Fields may be redacted based on user permissions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogEntryDTO(
    UUID id,
    Instant timestamp,
    String correlationId,
    ActorType actorType,
    String actorDisplayName,
    ActionType actionType,
    String actionDescription,
    ResourceType resourceType,
    String resourceDisplayName,
    Outcome outcome,
    SensitivityLevel sensitivity
) {

    /**
     * Actor types for audit events
     */
    public enum ActorType {
        USER, SYSTEM, API_KEY, SERVICE
    }

    /**
     * Action types for audit events
     */
    public enum ActionType {
        CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT,
        PAYMENT_CREATED, PAYMENT_CONFIRMED, PAYMENT_FAILED,
        SUBSCRIPTION_CREATED, SUBSCRIPTION_UPDATED, SUBSCRIPTION_CANCELLED,
        USER_REGISTERED, USER_INVITED,
        ORGANIZATION_CREATED, ORGANIZATION_UPDATED,
        ROLE_ASSIGNED, ROLE_REVOKED,
        PASSWORD_CHANGED, PASSWORD_RESET,
        EXPORT_REQUESTED, EXPORT_DOWNLOADED
    }

    /**
     * Resource types for audit events
     */
    public enum ResourceType {
        USER, ORGANIZATION, PAYMENT, SUBSCRIPTION, AUDIT_LOG,
        SESSION, API_KEY, ROLE, PERMISSION
    }

    /**
     * Outcome of the audited action
     */
    public enum Outcome {
        SUCCESS, FAILURE, PARTIAL
    }

    /**
     * Sensitivity level of the audit data
     */
    public enum SensitivityLevel {
        PUBLIC, INTERNAL, CONFIDENTIAL
    }

    /**
     * Creates a builder for this DTO
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for AuditLogEntryDTO
     */
    public static class Builder {
        private UUID id;
        private Instant timestamp;
        private String correlationId;
        private ActorType actorType;
        private String actorDisplayName;
        private ActionType actionType;
        private String actionDescription;
        private ResourceType resourceType;
        private String resourceDisplayName;
        private Outcome outcome;
        private SensitivityLevel sensitivity;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder actorType(ActorType actorType) {
            this.actorType = actorType;
            return this;
        }

        public Builder actorDisplayName(String actorDisplayName) {
            this.actorDisplayName = actorDisplayName;
            return this;
        }

        public Builder actionType(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder actionDescription(String actionDescription) {
            this.actionDescription = actionDescription;
            return this;
        }

        public Builder resourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceDisplayName(String resourceDisplayName) {
            this.resourceDisplayName = resourceDisplayName;
            return this;
        }

        public Builder outcome(Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder sensitivity(SensitivityLevel sensitivity) {
            this.sensitivity = sensitivity;
            return this;
        }

        public AuditLogEntryDTO build() {
            return new AuditLogEntryDTO(
                id, timestamp, correlationId, actorType, actorDisplayName,
                actionType, actionDescription, resourceType, resourceDisplayName,
                outcome, sensitivity
            );
        }
    }
}