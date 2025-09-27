package com.platform.shared.security;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a specific threat indicator detected in the system
 */
public class ThreatIndicator {

    private final String type;
    private final String severity;
    private final String description;
    private final double confidence;
    private final Instant detectedAt;
    private final Map<String, Object> metadata;
    private final String mitigationSuggestion;

    private ThreatIndicator(Builder builder) {
        this.type = builder.type;
        this.severity = builder.severity;
        this.description = builder.description;
        this.confidence = builder.confidence;
        this.detectedAt = builder.detectedAt;
        this.metadata = builder.metadata;
        this.mitigationSuggestion = builder.mitigationSuggestion;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getType() { return type; }
    public String getSeverity() { return severity; }
    public String getDescription() { return description; }
    public double getConfidence() { return confidence; }
    public Instant getDetectedAt() { return detectedAt; }
    public Map<String, Object> getMetadata() { return metadata; }
    public String getMitigationSuggestion() { return mitigationSuggestion; }

    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    public boolean isCriticalSeverity() {
        return "CRITICAL".equals(severity);
    }

    public static class Builder {
        private String type;
        private String severity;
        private String description;
        private double confidence = 0.5;
        private Instant detectedAt = Instant.now();
        private Map<String, Object> metadata;
        private String mitigationSuggestion;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder detectedAt(Instant detectedAt) {
            this.detectedAt = detectedAt;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder mitigationSuggestion(String mitigationSuggestion) {
            this.mitigationSuggestion = mitigationSuggestion;
            return this;
        }

        public ThreatIndicator build() {
            return new ThreatIndicator(this);
        }
    }
}