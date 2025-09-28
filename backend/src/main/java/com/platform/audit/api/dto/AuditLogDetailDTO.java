package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for detailed audit log information.
 * This DTO extends the basic audit log entry with additional details
 * that are only shown when viewing a specific audit log entry.
 *
 * Some fields may be redacted based on user permissions (marked as [REDACTED]).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogDetailDTO(
    UUID id,
    Instant timestamp,
    String correlationId,
    AuditLogEntryDTO.ActorType actorType,
    String actorId, // May be redacted
    String actorDisplayName,
    AuditLogEntryDTO.ActionType actionType,
    String actionDescription,
    AuditLogEntryDTO.ResourceType resourceType,
    String resourceId, // May be redacted
    String resourceDisplayName,
    AuditLogEntryDTO.Outcome outcome,
    String ipAddress, // May be redacted
    String userAgent, // May be redacted
    Map<String, Object> additionalData, // May be filtered
    AuditLogEntryDTO.SensitivityLevel sensitivity
) {

    /**
     * Creates a builder for this DTO
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a detailed DTO from a basic audit log entry DTO
     */
    public static AuditLogDetailDTO fromBasicEntry(AuditLogEntryDTO basicEntry) {
        return builder()
            .id(basicEntry.id())
            .timestamp(basicEntry.timestamp())
            .correlationId(basicEntry.correlationId())
            .actorType(basicEntry.actorType())
            .actorDisplayName(basicEntry.actorDisplayName())
            .actionType(basicEntry.actionType())
            .actionDescription(basicEntry.actionDescription())
            .resourceType(basicEntry.resourceType())
            .resourceDisplayName(basicEntry.resourceDisplayName())
            .outcome(basicEntry.outcome())
            .sensitivity(basicEntry.sensitivity())
            .build();
    }

    /**
     * Builder class for AuditLogDetailDTO
     */
    public static class Builder {
        private UUID id;
        private Instant timestamp;
        private String correlationId;
        private AuditLogEntryDTO.ActorType actorType;
        private String actorId;
        private String actorDisplayName;
        private AuditLogEntryDTO.ActionType actionType;
        private String actionDescription;
        private AuditLogEntryDTO.ResourceType resourceType;
        private String resourceId;
        private String resourceDisplayName;
        private AuditLogEntryDTO.Outcome outcome;
        private String ipAddress;
        private String userAgent;
        private Map<String, Object> additionalData;
        private AuditLogEntryDTO.SensitivityLevel sensitivity;

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

        public Builder actorType(AuditLogEntryDTO.ActorType actorType) {
            this.actorType = actorType;
            return this;
        }

        public Builder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder actorDisplayName(String actorDisplayName) {
            this.actorDisplayName = actorDisplayName;
            return this;
        }

        public Builder actionType(AuditLogEntryDTO.ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder actionDescription(String actionDescription) {
            this.actionDescription = actionDescription;
            return this;
        }

        public Builder resourceType(AuditLogEntryDTO.ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder resourceDisplayName(String resourceDisplayName) {
            this.resourceDisplayName = resourceDisplayName;
            return this;
        }

        public Builder outcome(AuditLogEntryDTO.Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder additionalData(Map<String, Object> additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public Builder sensitivity(AuditLogEntryDTO.SensitivityLevel sensitivity) {
            this.sensitivity = sensitivity;
            return this;
        }

        public AuditLogDetailDTO build() {
            return new AuditLogDetailDTO(
                id, timestamp, correlationId, actorType, actorId, actorDisplayName,
                actionType, actionDescription, resourceType, resourceId, resourceDisplayName,
                outcome, ipAddress, userAgent, additionalData, sensitivity
            );
        }
    }
}