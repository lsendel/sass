package com.platform.shared.compliance;

import java.time.Instant;
import java.util.UUID;

/**
 * Result of GDPR data erasure operation
 */
public class DataErasureResult {
    private UUID userId;
    private Status status;
    private String error;
    private Instant completedAt;
    private int recordsErased;

    public DataErasureResult() {
        this.status = Status.PENDING;
    }

    public DataErasureResult(UUID userId) {
        this.userId = userId;
        this.status = Status.PENDING;
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

    public int getRecordsErased() {
        return recordsErased;
    }

    public void setRecordsErased(int recordsErased) {
        this.recordsErased = recordsErased;
    }

    public void setReason(String reason) {
        // Store the reason for erasure (could add a field for this)
    }

    public void addDeletedCategory(String category) {
        // Track which data categories were deleted (could add a field for this)
    }

    public void addAnonymizedCategory(String category) {
        // Track which data categories were anonymized (could add a field for this)
    }

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, REJECTED
    }
}