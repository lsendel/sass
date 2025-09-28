package com.platform.audit.api.dto;

import java.util.List;

/**
 * Response DTO for audit log search results with pagination.
 */
public record AuditLogResponseDTO(
    List<AuditLogEntryDTO> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static AuditLogResponseDTO of(
        List<AuditLogEntryDTO> entries,
        int pageNumber,
        int pageSize,
        long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean isFirst = pageNumber == 0;
        boolean isLast = pageNumber >= totalPages - 1;

        return new AuditLogResponseDTO(
            entries,
            pageNumber,
            pageSize,
            totalElements,
            totalPages,
            isFirst,
            isLast
        );
    }
}