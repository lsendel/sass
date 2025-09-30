package com.platform.audit.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Filter criteria for audit log queries.
 * Used internally by audit log services to build filtered queries.
 */
public record AuditLogFilter(
        UUID organizationId,
        UUID userId,
        Instant dateFrom,
        Instant dateTo,
        String search,
        List<String> actionTypes,
        List<String> resourceTypes,
        List<String> actorEmails,
        Boolean includeSystemActions,
        Integer pageNumber,
        Integer pageSize,
        String sortField,
        String sortDirection
) {

    public AuditLogFilter {
        // Default values for pagination
        if (pageNumber == null || pageNumber < 0) {
            pageNumber = 0;
        }
        if (pageSize == null || pageSize <= 0 || pageSize > 1000) {
            pageSize = 50;
        }
        // Default values for sorting
        if (sortField == null || sortField.trim().isEmpty()) {
            sortField = "timestamp";
        }
        if (sortDirection == null || (!sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC"))) {
            sortDirection = "DESC";
        }
    }

    /**
     * Create a filter for organization-level access.
     */
    public static AuditLogFilter forOrganization(UUID organizationId,
                                                Instant dateFrom,
                                                Instant dateTo,
                                                String search,
                                                Integer page,
                                                Integer size) {
        return new AuditLogFilter(
            organizationId,
            null,
            dateFrom,
            dateTo,
            search,
            null,
            null,
            null,
            false,
            page,
            size,
            "timestamp",
            "DESC"
        );
    }

    /**
     * Create a filter for user-specific access.
     */
    public static AuditLogFilter forUser(UUID organizationId,
                                        UUID userId,
                                        Instant dateFrom,
                                        Instant dateTo,
                                        String search,
                                        Integer page,
                                        Integer size) {
        return new AuditLogFilter(
            organizationId,
            userId,
            dateFrom,
            dateTo,
            search,
            null,
            null,
            null,
            false,
            page,
            size,
            "timestamp",
            "DESC"
        );
    }

    /**
     * Check if this filter has search criteria.
     */
    public boolean hasSearch() {
        return search != null && !search.trim().isEmpty();
    }

    /**
     * Check if this filter has date range criteria.
     */
    public boolean hasDateRange() {
        return dateFrom != null || dateTo != null;
    }

    /**
     * Check if this filter has action type criteria.
     */
    public boolean hasActionTypes() {
        return actionTypes != null && !actionTypes.isEmpty();
    }

    /**
     * Get the validated search term for database queries.
     */
    public String getSearchTerm() {
        if (search == null || search.trim().isEmpty()) {
            return null;
        }
        return search.trim().toLowerCase();
    }

    // Convenience methods for API compatibility
    public String searchText() { return search; }
    public Integer page() { return pageNumber; }
    public Integer size() { return pageSize; }
    public boolean hasResourceTypes() {
        return resourceTypes != null && !resourceTypes.isEmpty();
    }
}