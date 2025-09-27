package com.platform.shared.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result object for anomaly detection analysis
 */
public class AnomalyDetectionResult {

    private final String userId;
    private final int analysisWindow;
    private final int totalEvents;
    private final Instant analysisTimestamp;
    private final List<Anomaly> detectedAnomalies;
    private final Map<String, Double> behavioralScores;
    private final double overallAnomalyScore;
    private final AnomalyRiskLevel riskLevel;

    private AnomalyDetectionResult(Builder builder) {
        this.userId = builder.userId;
        this.analysisWindow = builder.analysisWindow;
        this.totalEvents = builder.totalEvents;
        this.analysisTimestamp = builder.analysisTimestamp;
        this.detectedAnomalies = builder.detectedAnomalies;
        this.behavioralScores = builder.behavioralScores;
        this.overallAnomalyScore = builder.overallAnomalyScore;
        this.riskLevel = builder.riskLevel;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getUserId() { return userId; }
    public int getAnalysisWindow() { return analysisWindow; }
    public int getTotalEvents() { return totalEvents; }
    public Instant getAnalysisTimestamp() { return analysisTimestamp; }
    public List<Anomaly> getDetectedAnomalies() { return detectedAnomalies; }
    public Map<String, Double> getBehavioralScores() { return behavioralScores; }
    public double getOverallAnomalyScore() { return overallAnomalyScore; }
    public AnomalyRiskLevel getRiskLevel() { return riskLevel; }

    public static class Builder {
        private String userId;
        private int analysisWindow;
        private int totalEvents;
        private Instant analysisTimestamp;
        private List<Anomaly> detectedAnomalies;
        private Map<String, Double> behavioralScores;
        private double overallAnomalyScore;
        private AnomalyRiskLevel riskLevel = AnomalyRiskLevel.LOW;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder analysisWindow(int analysisWindow) {
            this.analysisWindow = analysisWindow;
            return this;
        }

        public Builder totalEvents(int totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder analysisTimestamp(Instant analysisTimestamp) {
            this.analysisTimestamp = analysisTimestamp;
            return this;
        }

        public Builder detectedAnomalies(List<Anomaly> detectedAnomalies) {
            this.detectedAnomalies = detectedAnomalies;
            return this;
        }

        public Builder behavioralScores(Map<String, Double> behavioralScores) {
            this.behavioralScores = behavioralScores;
            return this;
        }

        public Builder overallAnomalyScore(double overallAnomalyScore) {
            this.overallAnomalyScore = overallAnomalyScore;
            return this;
        }

        public Builder riskLevel(AnomalyRiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public AnomalyDetectionResult build() {
            return new AnomalyDetectionResult(this);
        }
    }

    public static class Anomaly {
        private String type;
        private String description;
        private double severity;
        private double confidence;
        private Instant detectedAt;
        private Map<String, Object> details;

        // Constructors, getters, and setters
        public Anomaly() {}

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public double getSeverity() { return severity; }
        public void setSeverity(double severity) { this.severity = severity; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public Instant getDetectedAt() { return detectedAt; }
        public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }

    public enum AnomalyRiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}