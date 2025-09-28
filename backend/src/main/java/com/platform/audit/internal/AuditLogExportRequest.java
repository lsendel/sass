package com.platform.audit.internal;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an audit log export request.
 * Tracks the status and metadata of export operations for compliance and user experience.
 */
@Entity
@Table(name = "audit_export_requests",
       indexes = {
           @Index(name = "idx_audit_export_user", columnList = "user_id"),
           @Index(name = "idx_audit_export_status", columnList = "status"),
           @Index(name = "idx_audit_export_created", columnList = "created_at"),
           @Index(name = "idx_audit_export_token", columnList = "download_token")
       })
@EntityListeners(AuditingEntityListener.class)
public class AuditLogExportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private ExportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExportStatus status;

    @Column(name = "date_from")
    private Instant dateFrom;

    @Column(name = "date_to")
    private Instant dateTo;

    @Column(name = "search_term")
    private String searchTerm;

    @Column(name = "action_types", columnDefinition = "TEXT")
    private String actionTypes;

    @Column(name = "total_records")
    private Long totalRecords;

    @Column(name = "processed_records")
    private Long processedRecords;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "download_token", unique = true)
    private String downloadToken;

    @Column(name = "download_expires_at")
    private Instant downloadExpiresAt;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "max_downloads")
    private Integer maxDownloads = 5;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Constructors
    public AuditLogExportRequest() {}

    public AuditLogExportRequest(UUID userId, UUID organizationId, ExportFormat format) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.format = format;
        this.status = ExportStatus.PENDING;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public ExportFormat getFormat() { return format; }
    public void setFormat(ExportFormat format) { this.format = format; }

    public ExportStatus getStatus() { return status; }
    public void setStatus(ExportStatus status) { this.status = status; }

    public Instant getDateFrom() { return dateFrom; }
    public void setDateFrom(Instant dateFrom) { this.dateFrom = dateFrom; }

    public Instant getDateTo() { return dateTo; }
    public void setDateTo(Instant dateTo) { this.dateTo = dateTo; }

    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }

    public String getActionTypes() { return actionTypes; }
    public void setActionTypes(String actionTypes) { this.actionTypes = actionTypes; }

    public Long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Long totalRecords) { this.totalRecords = totalRecords; }

    public Long getProcessedRecords() { return processedRecords; }
    public void setProcessedRecords(Long processedRecords) { this.processedRecords = processedRecords; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getDownloadToken() { return downloadToken; }
    public void setDownloadToken(String downloadToken) { this.downloadToken = downloadToken; }

    public Instant getDownloadExpiresAt() { return downloadExpiresAt; }
    public void setDownloadExpiresAt(Instant downloadExpiresAt) { this.downloadExpiresAt = downloadExpiresAt; }

    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }

    public Integer getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(Integer maxDownloads) { this.maxDownloads = maxDownloads; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    // Business methods
    public void markAsStarted() {
        this.status = ExportStatus.PROCESSING;
        this.startedAt = Instant.now();
    }

    public void markAsCompleted(String filePath, long fileSizeBytes, String downloadToken) {
        this.status = ExportStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.downloadToken = downloadToken;
        this.downloadExpiresAt = Instant.now().plusSeconds(3600 * 24 * 7); // 7 days
    }

    public void markAsFailed(String errorMessage) {
        this.status = ExportStatus.FAILED;
        this.completedAt = Instant.now();
        this.errorMessage = errorMessage;
    }

    public boolean isDownloadExpired() {
        return downloadExpiresAt != null && Instant.now().isAfter(downloadExpiresAt);
    }

    public boolean canDownload() {
        return status == ExportStatus.COMPLETED &&
               !isDownloadExpired() &&
               downloadCount < maxDownloads;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public double getProgressPercentage() {
        if (totalRecords == null || totalRecords == 0) {
            return 0.0;
        }
        if (processedRecords == null) {
            return 0.0;
        }
        return (double) processedRecords / totalRecords * 100.0;
    }

    /**
     * Export format enumeration.
     */
    public enum ExportFormat {
        CSV("text/csv", ".csv"),
        JSON("application/json", ".json"),
        PDF("application/pdf", ".pdf");

        private final String mimeType;
        private final String fileExtension;

        ExportFormat(String mimeType, String fileExtension) {
            this.mimeType = mimeType;
            this.fileExtension = fileExtension;
        }

        public String getMimeType() { return mimeType; }
        public String getFileExtension() { return fileExtension; }
    }

    /**
     * Export status enumeration.
     */
    public enum ExportStatus {
        PENDING,     // Export request created, waiting to start
        PROCESSING,  // Export is currently being generated
        COMPLETED,   // Export completed successfully, ready for download
        FAILED,      // Export failed due to error
        EXPIRED      // Export completed but download link has expired
    }
}