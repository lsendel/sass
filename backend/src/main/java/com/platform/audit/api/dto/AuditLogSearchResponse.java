package com.platform.audit.api.dto;

import java.util.List;

/**
 * Response DTO for audit log search operations with pagination.
 * This is the internal service response class for the audit log viewer.
 */
public record AuditLogSearchResponse(
    List<AuditLogEntryDTO> entries,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static AuditLogSearchResponse of(
        List<AuditLogEntryDTO> entries,
        int pageNumber,
        int pageSize,
        long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean isFirst = pageNumber == 0;
        boolean isLast = pageNumber >= totalPages - 1;

        return new AuditLogSearchResponse(
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
