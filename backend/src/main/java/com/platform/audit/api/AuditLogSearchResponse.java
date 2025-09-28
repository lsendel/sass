package com.platform.audit.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Response object for paginated audit log search results.
 * Contains the audit log entries along with pagination metadata.
 */
public record AuditLogSearchResponse(
    @NotNull
    List<AuditLogEntryDTO> entries,

    @NotNull
    @Min(0)
    Long totalElements,

    @NotNull
    @Min(0)
    Integer totalPages,

    @NotNull
    @Min(0)
    Integer currentPage,

    @NotNull
    @Min(1)
    Integer pageSize,

    @NotNull
    Boolean hasNext,

    @NotNull
    Boolean hasPrevious
) {
    /**
     * Creates an empty search response for cases where no results are found.
     */
    public static AuditLogSearchResponse empty(Integer currentPage, Integer pageSize) {
        return new AuditLogSearchResponse(
            List.of(),
            0L,
            0,
            currentPage,
            pageSize,
            false,
            currentPage > 0
        );
    }

    /**
     * Creates a search response from a page of results.
     */
    public static AuditLogSearchResponse of(
        List<AuditLogEntryDTO> entries,
        Long totalElements,
        Integer currentPage,
        Integer pageSize
    ) {
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = currentPage < totalPages - 1;
        boolean hasPrevious = currentPage > 0;

        return new AuditLogSearchResponse(
            entries,
            totalElements,
            totalPages,
            currentPage,
            pageSize,
            hasNext,
            hasPrevious
        );
    }
}