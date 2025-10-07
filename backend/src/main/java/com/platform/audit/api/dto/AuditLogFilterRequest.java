package com.platform.audit.api.dto;

import com.platform.audit.internal.AuditLogFilter;

import java.time.Instant;
import java.util.List;

/**
 * Request object for audit log filtering operations.
 *
 * <p>This record encapsulates all filtering parameters for audit log queries,
 * reducing method parameter complexity and improving maintainability.
 *
 * @param dateFrom start date for filtering (inclusive)
 * @param dateTo end date for filtering (inclusive)
 * @param search text search term for action descriptions
 * @param actionTypes list of action types to include
 * @param resourceTypes list of resource types to include
 * @param outcomes list of outcomes to include (SUCCESS, FAILURE, PARTIAL)
 * @param pagination pagination parameters
 * @param sort sorting parameters
 *
 * @since 1.0.0
 */
public record AuditLogFilterRequest(
    Instant dateFrom,
    Instant dateTo,
    String search,
    List<String> actionTypes,
    List<String> resourceTypes,
    List<String> outcomes,
    PaginationRequest pagination,
    SortRequest sort
) {

    /**
     * Creates an AuditLogFilterRequest from web request parameters.
     *
     * @param dateFrom start date string
     * @param dateTo end date string
     * @param search search term
     * @param actionTypes action types list
     * @param resourceTypes resource types list
     * @param outcomes outcomes list
     * @param page page number
     * @param size page size
     * @param sortField sort field
     * @param sortDirection sort direction
     * @return configured filter request
     */
    public static AuditLogFilterRequest from(
            Instant dateFrom,
            Instant dateTo,
            String search,
            List<String> actionTypes,
            List<String> resourceTypes,
            List<String> outcomes,
            int page,
            int size,
            String sortField,
            String sortDirection) {

        return new AuditLogFilterRequest(
            dateFrom,
            dateTo,
            search,
            actionTypes,
            resourceTypes,
            outcomes,
            new PaginationRequest(page, size),
            new SortRequest(sortField, sortDirection)
        );
    }

    /**
     * Converts this request to an internal AuditLogFilter.
     *
     * @return internal filter object
     */
    public AuditLogFilter toInternalFilter() {
        return new AuditLogFilter(
            null, // organizationId - set by service
            null, // userId - set by service
            dateFrom,
            dateTo,
            search,
            actionTypes,
            resourceTypes,
            null, // actorEmails
            null, // includeSystemActions - determined by permissions
            pagination.page(),
            pagination.size(),
            sort.field(),
            sort.direction()
        );
    }

    /**
     * Creates a default filter request with common defaults.
     *
     * @return default filter request
     */
    public static AuditLogFilterRequest defaultRequest() {
        return new AuditLogFilterRequest(
            null, null, null, null, null, null,
            PaginationRequest.DEFAULT,
            SortRequest.DEFAULT
        );
    }
}