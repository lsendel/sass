package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object for audit log filtering criteria.
 * Used to filter audit log queries based on user-specified parameters.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogFilterDTO(
    Instant dateFrom,
    Instant dateTo,
    @Size(max = 255, message = "Search text cannot exceed 255 characters")
    String searchText,
    List<AuditLogEntryDTO.ActionType> actionTypes,
    List<AuditLogEntryDTO.ResourceType> resourceTypes,
    List<AuditLogEntryDTO.Outcome> outcomes,
    @Min(value = 0, message = "Page number cannot be negative")
    Integer page,
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    Integer size,
    SortField sortField,
    SortDirection sortDirection
) {

    /**
     * Available sort fields for audit logs
     */
    public enum SortField {
        TIMESTAMP("timestamp"),
        ACTION_TYPE("actionType"),
        ACTOR_DISPLAY_NAME("actorDisplayName"),
        RESOURCE_TYPE("resourceType");

        private final String fieldName;

        SortField(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    /**
     * Sort direction options
     */
    public enum SortDirection {
        ASC, DESC
    }

    /**
     * Creates a builder for this DTO
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default filter with standard pagination
     */
    public static AuditLogFilterDTO defaultFilter() {
        return builder()
            .page(0)
            .size(50)
            .sortField(SortField.TIMESTAMP)
            .sortDirection(SortDirection.DESC)
            .build();
    }

    /**
     * Validates that dateFrom is before dateTo if both are specified
     */
    public boolean isValidDateRange() {
        if (dateFrom != null && dateTo != null) {
            return dateFrom.isBefore(dateTo) || dateFrom.equals(dateTo);
        }
        return true;
    }

    /**
     * Checks if the date range exceeds one year
     */
    public boolean isDateRangeWithinLimits() {
        if (dateFrom != null && dateTo != null) {
            long daysBetween = java.time.Duration.between(dateFrom, dateTo).toDays();
            return daysBetween <= 365;
        }
        return true;
    }

    /**
     * Builder class for AuditLogFilterDTO
     */
    public static class Builder {
        private Instant dateFrom;
        private Instant dateTo;
        private String searchText;
        private List<AuditLogEntryDTO.ActionType> actionTypes;
        private List<AuditLogEntryDTO.ResourceType> resourceTypes;
        private List<AuditLogEntryDTO.Outcome> outcomes;
        private Integer page;
        private Integer size;
        private SortField sortField;
        private SortDirection sortDirection;

        public Builder dateFrom(Instant dateFrom) {
            this.dateFrom = dateFrom;
            return this;
        }

        public Builder dateTo(Instant dateTo) {
            this.dateTo = dateTo;
            return this;
        }

        public Builder searchText(String searchText) {
            this.searchText = searchText;
            return this;
        }

        public Builder actionTypes(List<AuditLogEntryDTO.ActionType> actionTypes) {
            this.actionTypes = actionTypes;
            return this;
        }

        public Builder resourceTypes(List<AuditLogEntryDTO.ResourceType> resourceTypes) {
            this.resourceTypes = resourceTypes;
            return this;
        }

        public Builder outcomes(List<AuditLogEntryDTO.Outcome> outcomes) {
            this.outcomes = outcomes;
            return this;
        }

        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public Builder sortField(SortField sortField) {
            this.sortField = sortField;
            return this;
        }

        public Builder sortDirection(SortDirection sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        public AuditLogFilterDTO build() {
            return new AuditLogFilterDTO(
                dateFrom, dateTo, searchText, actionTypes, resourceTypes, outcomes,
                page, size, sortField, sortDirection
            );
        }
    }
}