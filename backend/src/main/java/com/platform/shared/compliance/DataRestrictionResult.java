package com.platform.shared.compliance;

import java.time.Instant;
import java.util.UUID;

/**
 * Result of GDPR data restriction operation
 */
public class DataRestrictionResult {
    private UUID userId;
    private Status status;
    private String reason;
    private String error;
    private Instant completedAt;

    public DataRestrictionResult() {
        this.status = Status.PENDING;
    }

    public DataRestrictionResult(UUID userId) {
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }
}