package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Data Transfer Object for paginated audit log responses.
 * This follows Spring Data's Page interface structure for consistency.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogResponseDTO(
    List<AuditLogEntryDTO> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {

    /**
     * Creates a builder for this DTO
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for AuditLogResponseDTO
     */
    public static class Builder {
        private List<AuditLogEntryDTO> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;

        public Builder content(List<AuditLogEntryDTO> content) {
            this.content = content;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public Builder totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder first(boolean first) {
            this.first = first;
            return this;
        }

        public Builder last(boolean last) {
            this.last = last;
            return this;
        }

        public AuditLogResponseDTO build() {
            return new AuditLogResponseDTO(
                content, page, size, totalElements, totalPages, first, last
            );
        }
    }

    /**
     * Calculates pagination fields from basic parameters
     */
    public static AuditLogResponseDTO of(List<AuditLogEntryDTO> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean first = page == 0;
        boolean last = page >= totalPages - 1;

        return builder()
            .content(content)
            .page(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .first(first)
            .last(last)
            .build();
    }
}