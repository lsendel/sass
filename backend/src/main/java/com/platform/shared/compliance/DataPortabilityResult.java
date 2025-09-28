package com.platform.shared.compliance;

import java.time.Instant;
import java.util.UUID;

/**
 * Result of GDPR data portability operation
 */
public class DataPortabilityResult {
    private UUID userId;
    private String format;
    private Status status;
    private String data;
    private String error;
    private Instant completedAt;

    public DataPortabilityResult() {
        this.status = Status.PENDING;
    }

    public DataPortabilityResult(UUID userId, String format) {
        this();
        this.userId = userId;
        this.format = format;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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