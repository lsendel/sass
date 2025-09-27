package com.platform.shared.security;

import java.time.Instant;

/**
 * Represents a coordinated response to multiple related security threats
 */
public class CoordinatedResponse {

    private final String responseId;
    private final Instant startTime;
    private final Instant endTime;
    private final int threatCount;
    private final SecurityIncidentResponseService.ThreatCorrelation correlation;
    private final String masterIncidentId;
    private final String status;
    private final double effectivenessScore;

    private CoordinatedResponse(Builder builder) {
        this.responseId = builder.responseId;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.threatCount = builder.threatCount;
        this.correlation = builder.correlation;
        this.masterIncidentId = builder.masterIncidentId;
        this.status = builder.status;
        this.effectivenessScore = builder.effectivenessScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getResponseId() { return responseId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public int getThreatCount() { return threatCount; }
    public SecurityIncidentResponseService.ThreatCorrelation getCorrelation() { return correlation; }
    public String getMasterIncidentId() { return masterIncidentId; }
    public String getStatus() { return status; }
    public double getEffectivenessScore() { return effectivenessScore; }

    public static class Builder {
        private String responseId;
        private Instant startTime;
        private Instant endTime;
        private int threatCount;
        private SecurityIncidentResponseService.ThreatCorrelation correlation;
        private String masterIncidentId;
        private String status;
        private double effectivenessScore;

        public Builder responseId(String responseId) {
            this.responseId = responseId;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder threatCount(int threatCount) {
            this.threatCount = threatCount;
            return this;
        }

        public Builder correlation(SecurityIncidentResponseService.ThreatCorrelation correlation) {
            this.correlation = correlation;
            return this;
        }

        public Builder masterIncidentId(String masterIncidentId) {
            this.masterIncidentId = masterIncidentId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder effectivenessScore(double effectivenessScore) {
            this.effectivenessScore = effectivenessScore;
            return this;
        }

        public CoordinatedResponse build() {
            return new CoordinatedResponse(this);
        }
    }
}