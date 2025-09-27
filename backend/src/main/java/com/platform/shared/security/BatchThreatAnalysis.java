package com.platform.shared.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result object for batch threat analysis across multiple events
 */
public class BatchThreatAnalysis {

    private final int totalEvents;
    private final Instant analysisTimestamp;
    private final List<ThreatPattern> detectedPatterns;
    private final Map<String, Integer> threatTypeCounts;
    private final ThreatDetectionService.ThreatIntelligence threatIntelligence;
    private final List<AdvancedThreat> advancedThreats;
    private final double overallRiskScore;

    private BatchThreatAnalysis(Builder builder) {
        this.totalEvents = builder.totalEvents;
        this.analysisTimestamp = builder.analysisTimestamp;
        this.detectedPatterns = builder.detectedPatterns;
        this.threatTypeCounts = builder.threatTypeCounts;
        this.threatIntelligence = builder.threatIntelligence;
        this.advancedThreats = builder.advancedThreats;
        this.overallRiskScore = builder.overallRiskScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public int getTotalEvents() { return totalEvents; }
    public Instant getAnalysisTimestamp() { return analysisTimestamp; }
    public List<ThreatPattern> getDetectedPatterns() { return detectedPatterns; }
    public Map<String, Integer> getThreatTypeCounts() { return threatTypeCounts; }
    public ThreatDetectionService.ThreatIntelligence getThreatIntelligence() { return threatIntelligence; }
    public List<AdvancedThreat> getAdvancedThreats() { return advancedThreats; }
    public double getOverallRiskScore() { return overallRiskScore; }

    public static class Builder {
        private int totalEvents;
        private Instant analysisTimestamp;
        private List<ThreatPattern> detectedPatterns;
        private Map<String, Integer> threatTypeCounts;
        private ThreatDetectionService.ThreatIntelligence threatIntelligence;
        private List<AdvancedThreat> advancedThreats;
        private double overallRiskScore;

        public Builder totalEvents(int totalEvents) {
            this.totalEvents = totalEvents;
            return this;
        }

        public Builder analysisTimestamp(Instant analysisTimestamp) {
            this.analysisTimestamp = analysisTimestamp;
            return this;
        }

        public Builder detectedPatterns(List<ThreatPattern> detectedPatterns) {
            this.detectedPatterns = detectedPatterns;
            return this;
        }

        public Builder threatTypeCounts(Map<String, Integer> threatTypeCounts) {
            this.threatTypeCounts = threatTypeCounts;
            return this;
        }

        public Builder threatIntelligence(ThreatDetectionService.ThreatIntelligence threatIntelligence) {
            this.threatIntelligence = threatIntelligence;
            return this;
        }

        public Builder advancedThreats(List<AdvancedThreat> advancedThreats) {
            this.advancedThreats = advancedThreats;
            return this;
        }

        public Builder overallRiskScore(double overallRiskScore) {
            this.overallRiskScore = overallRiskScore;
            return this;
        }

        public BatchThreatAnalysis build() {
            return new BatchThreatAnalysis(this);
        }
    }

    public static class ThreatPattern {
        private String patternType;
        private String description;
        private int occurrences;
        private double confidence;

        // Constructors, getters, and setters
        public ThreatPattern() {}

        public String getPatternType() { return patternType; }
        public void setPatternType(String patternType) { this.patternType = patternType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getOccurrences() { return occurrences; }
        public void setOccurrences(int occurrences) { this.occurrences = occurrences; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }

    public static class AdvancedThreat {
        private String threatType;
        private String description;
        private List<String> affectedUsers;
        private List<String> indicators;
        private String severity;

        // Constructors, getters, and setters
        public AdvancedThreat() {}

        public String getThreatType() { return threatType; }
        public void setThreatType(String threatType) { this.threatType = threatType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getAffectedUsers() { return affectedUsers; }
        public void setAffectedUsers(List<String> affectedUsers) { this.affectedUsers = affectedUsers; }

        public List<String> getIndicators() { return indicators; }
        public void setIndicators(List<String> indicators) { this.indicators = indicators; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
}