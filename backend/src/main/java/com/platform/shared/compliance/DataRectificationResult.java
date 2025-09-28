package com.platform.shared.compliance;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Result of GDPR data rectification operation
 */
public class DataRectificationResult {
    private UUID userId;
    private Status status;
    private String error;
    private Instant completedAt;
    private int recordsUpdated;

    public DataRectificationResult() {
        this.status = Status.PENDING;
    }

    public DataRectificationResult(UUID userId) {
        this();
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public int getRecordsUpdated() {
        return recordsUpdated;
    }

    public void setRecordsUpdated(int recordsUpdated) {
        this.recordsUpdated = recordsUpdated;
    }

    public void setOldValues(Map<String, Object> oldValues) {
        // Store old values before rectification (could add a field for this)
    }

    public void setNewValues(Map<String, Object> newValues) {
        // Store new values after rectification (could add a field for this)
    }

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }
}