package com.platform.shared.security;

import java.time.Instant;
import java.util.List;

import com.platform.audit.internal.AuditEvent;

/**
 * Comprehensive incident report for post-incident analysis
 */
public class IncidentReport {

    private final String incidentId;
    private final String reportId;
    private final Instant generatedAt;
    private final SecurityIncident incident;
    private final List<SecurityIncidentResponseService.IncidentTimelineEvent> timeline;
    private final List<AuditEvent> relatedEvents;
    private final SecurityIncidentResponseService.IncidentImpactAnalysis impactAnalysis;
    private final List<String> lessonsLearned;
    private final List<String> recommendations;
    private final String summary;

    private IncidentReport(Builder builder) {
        this.incidentId = builder.incidentId;
        this.reportId = builder.reportId;
        this.generatedAt = builder.generatedAt;
        this.incident = builder.incident;
        this.timeline = builder.timeline;
        this.relatedEvents = builder.relatedEvents;
        this.impactAnalysis = builder.impactAnalysis;
        this.lessonsLearned = builder.lessonsLearned;
        this.recommendations = builder.recommendations;
        this.summary = builder.summary;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getIncidentId() { return incidentId; }
    public String getReportId() { return reportId; }
    public Instant getGeneratedAt() { return generatedAt; }
    public SecurityIncident getIncident() { return incident; }
    public List<SecurityIncidentResponseService.IncidentTimelineEvent> getTimeline() { return timeline; }
    public List<AuditEvent> getRelatedEvents() { return relatedEvents; }
    public SecurityIncidentResponseService.IncidentImpactAnalysis getImpactAnalysis() { return impactAnalysis; }
    public List<String> getLessonsLearned() { return lessonsLearned; }
    public List<String> getRecommendations() { return recommendations; }
    public String getSummary() { return summary; }

    public static class Builder {
        private String incidentId;
        private String reportId;
        private Instant generatedAt;
        private SecurityIncident incident;
        private List<SecurityIncidentResponseService.IncidentTimelineEvent> timeline;
        private List<AuditEvent> relatedEvents;
        private SecurityIncidentResponseService.IncidentImpactAnalysis impactAnalysis;
        private List<String> lessonsLearned;
        private List<String> recommendations;
        private String summary;

        public Builder incidentId(String incidentId) {
            this.incidentId = incidentId;
            return this;
        }

        public Builder reportId(String reportId) {
            this.reportId = reportId;
            return this;
        }

        public Builder generatedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public Builder incident(SecurityIncident incident) {
            this.incident = incident;
            return this;
        }

        public Builder timeline(List<SecurityIncidentResponseService.IncidentTimelineEvent> timeline) {
            this.timeline = timeline;
            return this;
        }

        public Builder relatedEvents(List<AuditEvent> relatedEvents) {
            this.relatedEvents = relatedEvents;
            return this;
        }

        public Builder impactAnalysis(SecurityIncidentResponseService.IncidentImpactAnalysis impactAnalysis) {
            this.impactAnalysis = impactAnalysis;
            return this;
        }

        public Builder lessonsLearned(List<String> lessonsLearned) {
            this.lessonsLearned = lessonsLearned;
            return this;
        }

        public Builder recommendations(List<String> recommendations) {
            this.recommendations = recommendations;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public IncidentReport build() {
            return new IncidentReport(this);
        }
    }
}