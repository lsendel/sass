package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 * Response DTO for audit log export requests.
 */
public final class ExportResponseDTO {
    private String exportId;
    private String status;
    private String downloadUrl;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant requestedAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant completedAt;
    
    private Long totalRecords;
    private String format;
    private String message;

    // Default constructor
    public ExportResponseDTO() { }

    // Constructor with required fields
    public ExportResponseDTO(final String exportId, final String status) {
        this.exportId = exportId;
        this.status = status;
        this.requestedAt = Instant.now();
    }

    // Getters and setters
    public String getExportId() {
        return exportId;
    }

    public void setExportId(final String exportId) {
        this.exportId = exportId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(final String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(final Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(final Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(final Long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }
}