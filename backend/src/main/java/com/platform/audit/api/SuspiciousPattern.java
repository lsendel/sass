package com.platform.audit.api;

/**
 * Represents a suspicious pattern detected in audit logs during forensic analysis.
 * This is a placeholder implementation for the audit log viewer feature.
 */
public class SuspiciousPattern {
    private String patternType;
    private String description;
    private int severity;
    private int occurrences;

    public SuspiciousPattern() {}

    public SuspiciousPattern(String patternType, String description, int severity, int occurrences) {
        this.patternType = patternType;
        this.description = description;
        this.severity = severity;
        this.occurrences = occurrences;
    }

    public String getPatternType() {
        return patternType;
    }

    public void setPatternType(String patternType) {
        this.patternType = patternType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }
}