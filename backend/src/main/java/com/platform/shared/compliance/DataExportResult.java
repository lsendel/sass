package com.platform.shared.compliance;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Result of GDPR data export operation
 */
public class DataExportResult {
    private UUID userId;
    private Status status;
    private String error;
    private Instant completedAt;
    private Map<String, Map<String, Object>> dataCategories;

    public DataExportResult() {
        this.status = Status.PENDING;
        this.dataCategories = new HashMap<>();
    }

    public DataExportResult(UUID userId) {
        this.userId = userId;
        this.status = Status.PENDING;
        this.dataCategories = new HashMap<>();
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

    public Map<String, Map<String, Object>> getDataCategories() {
        return dataCategories;
    }

    public void addDataCategory(String category, Map<String, Object> data) {
        this.dataCategories.put(category, data);
    }

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, REJECTED
    }
}