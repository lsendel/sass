package com.platform.shared.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a security incident in the system
 */
public class SecurityIncident {

    private final String incidentId;
    private final String title;
    private final String description;
    private final IncidentSeverity severity;
    private final IncidentStatus status;
    private final IncidentPriority priority;
    private final String affectedUserId;
    private final String sourceIpAddress;
    private final String detectionSource;
    private final String assignedTo;
    private final String createdBy;
    private final String updatedBy;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String source;
    private final ThreatAnalysisResult threatAnalysis;
    private final Map<String, Object> metadata;
    private final List<String> tags;

    private SecurityIncident(Builder builder) {
        this.incidentId = builder.incidentId;
        this.title = builder.title;
        this.description = builder.description;
        this.severity = builder.severity;
        this.status = builder.status;
        this.priority = builder.priority;
        this.affectedUserId = builder.affectedUserId;
        this.sourceIpAddress = builder.sourceIpAddress;
        this.detectionSource = builder.detectionSource;
        this.assignedTo = builder.assignedTo;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.source = builder.source;
        this.threatAnalysis = builder.threatAnalysis;
        this.metadata = builder.metadata;
        this.tags = builder.tags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
            .incidentId(incidentId)
            .title(title)
            .description(description)
            .severity(severity)
            .status(status)
            .priority(priority)
            .affectedUserId(affectedUserId)
            .sourceIpAddress(sourceIpAddress)
            .detectionSource(detectionSource)
            .assignedTo(assignedTo)
            .createdBy(createdBy)
            .updatedBy(updatedBy)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .source(source)
            .threatAnalysis(threatAnalysis)
            .metadata(metadata)
            .tags(tags);
    }

    // Getters
    public String getIncidentId() { return incidentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IncidentSeverity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public IncidentPriority getPriority() { return priority; }
    public String getAffectedUserId() { return affectedUserId; }
    public String getSourceIpAddress() { return sourceIpAddress; }
    public String getDetectionSource() { return detectionSource; }
    public String getAssignedTo() { return assignedTo; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getSource() { return source; }
    public ThreatAnalysisResult getThreatAnalysis() { return threatAnalysis; }
    public Map<String, Object> getMetadata() { return metadata; }
    public List<String> getTags() { return tags; }

    public boolean isCritical() {
        return severity == IncidentSeverity.CRITICAL;
    }

    public boolean isOpen() {
        return status == IncidentStatus.OPEN || status == IncidentStatus.IN_PROGRESS;
    }

    public boolean isResolved() {
        return status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED;
    }

    public static class Builder {
        private String incidentId;
        private String title;
        private String description;
        private IncidentSeverity severity;
        private IncidentStatus status;
        private IncidentPriority priority;
        private String affectedUserId;
        private String sourceIpAddress;
        private String detectionSource;
        private String assignedTo;
        private String createdBy;
        private String updatedBy;
        private Instant createdAt;
        private Instant updatedAt;
        private String source;
        private ThreatAnalysisResult threatAnalysis;
        private Map<String, Object> metadata;
        private List<String> tags;

        public Builder incidentId(String incidentId) {
            this.incidentId = incidentId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder severity(IncidentSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder status(IncidentStatus status) {
            this.status = status;
            return this;
        }

        public Builder priority(IncidentPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder affectedUserId(String affectedUserId) {
            this.affectedUserId = affectedUserId;
            return this;
        }

        public Builder sourceIpAddress(String sourceIpAddress) {
            this.sourceIpAddress = sourceIpAddress;
            return this;
        }

        public Builder detectionSource(String detectionSource) {
            this.detectionSource = detectionSource;
            return this;
        }

        public Builder assignedTo(String assignedTo) {
            this.assignedTo = assignedTo;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder threatAnalysis(ThreatAnalysisResult threatAnalysis) {
            this.threatAnalysis = threatAnalysis;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public SecurityIncident build() {
            return new SecurityIncident(this);
        }
    }
}

enum IncidentSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum IncidentStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED
}

enum IncidentPriority {
    P0, P1, P2, P3
}