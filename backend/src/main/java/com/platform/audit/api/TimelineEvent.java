package com.platform.audit.api;

import java.time.Instant;

/**
 * Represents a timeline event in forensic analysis.
 * This is a placeholder implementation for the audit log viewer feature.
 */
public class TimelineEvent {
    private Instant timestamp;
    private String eventType;
    private String description;
    private String severity;

    public TimelineEvent() {}

    public TimelineEvent(Instant timestamp, String eventType, String description, String severity) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.description = description;
        this.severity = severity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}