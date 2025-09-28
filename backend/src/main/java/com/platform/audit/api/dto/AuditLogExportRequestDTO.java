package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for audit log export requests.
 * Specifies the format and filtering criteria for exporting audit logs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogExportRequestDTO(
    @NotNull(message = "Export format is required")
    ExportFormat format,
    AuditLogFilterDTO filters
) {

    /**
     * Available export formats
     */
    public enum ExportFormat {
        CSV("text/csv", ".csv"),
        JSON("application/json", ".json"),
        PDF("application/pdf", ".pdf");

        private final String contentType;
        private final String fileExtension;

        ExportFormat(String contentType, String fileExtension) {
            this.contentType = contentType;
            this.fileExtension = fileExtension;
        }

        public String getContentType() {
            return contentType;
        }

        public String getFileExtension() {
            return fileExtension;
        }
    }

    /**
     * Creates a builder for this DTO
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for AuditLogExportRequestDTO
     */
    public static class Builder {
        private ExportFormat format;
        private AuditLogFilterDTO filters;

        public Builder format(ExportFormat format) {
            this.format = format;
            return this;
        }

        public Builder filters(AuditLogFilterDTO filters) {
            this.filters = filters;
            return this;
        }

        public AuditLogExportRequestDTO build() {
            return new AuditLogExportRequestDTO(format, filters);
        }
    }
}