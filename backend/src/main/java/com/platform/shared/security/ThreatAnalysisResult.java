package com.platform.shared.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result object for threat analysis containing detected threats and risk assessment
 */
public class ThreatAnalysisResult {

    private final String eventId;
    private final String eventType;
    private final String userId;
    private final Instant timestamp;
    private final String ipAddress;
    private final List<ThreatIndicator> threatIndicators;
    private final double threatScore;
    private final ThreatLevel threatLevel;
    private final List<String> errors;
    private final String riskAssessment;

    private ThreatAnalysisResult(Builder builder) {
        this.eventId = builder.eventId;
        this.eventType = builder.eventType;
        this.userId = builder.userId;
        this.timestamp = builder.timestamp;
        this.ipAddress = builder.ipAddress;
        this.threatIndicators = builder.threatIndicators;
        this.threatScore = builder.threatScore;
        this.threatLevel = builder.threatLevel;
        this.errors = builder.errors;
        this.riskAssessment = builder.riskAssessment;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getUserId() { return userId; }
    public Instant getTimestamp() { return timestamp; }
    public String getIpAddress() { return ipAddress; }
    public List<ThreatIndicator> getThreatIndicators() { return threatIndicators; }
    public double getThreatScore() { return threatScore; }
    public ThreatLevel getThreatLevel() { return threatLevel; }
    public List<String> getErrors() { return errors; }
    public String getRiskAssessment() { return riskAssessment; }

    public boolean hasThreat() {
        return threatLevel != ThreatLevel.NONE && threatLevel != ThreatLevel.UNKNOWN;
    }

    public boolean isCriticalThreat() {
        return threatLevel == ThreatLevel.CRITICAL;
    }

    public static class Builder {
        private String eventId;
        private String eventType;
        private String userId;
        private Instant timestamp;
        private String ipAddress;
        private List<ThreatIndicator> threatIndicators = new ArrayList<>();
        private double threatScore = 0.0;
        private ThreatLevel threatLevel = ThreatLevel.NONE;
        private List<String> errors = new ArrayList<>();
        private String riskAssessment;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder addThreatIndicator(ThreatIndicator indicator) {
            this.threatIndicators.add(indicator);
            return this;
        }

        public Builder threatScore(double threatScore) {
            this.threatScore = threatScore;
            return this;
        }

        public Builder threatLevel(ThreatLevel threatLevel) {
            this.threatLevel = threatLevel;
            return this;
        }

        public Builder addError(String error) {
            this.errors.add(error);
            return this;
        }

        public Builder riskAssessment(String riskAssessment) {
            this.riskAssessment = riskAssessment;
            return this;
        }

        public List<ThreatIndicator> getThreatIndicators() {
            return threatIndicators;
        }

        public ThreatAnalysisResult build() {
            return new ThreatAnalysisResult(this);
        }
    }
}