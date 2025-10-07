package com.platform.audit.api.dto;

import java.util.Set;

/**
 * Standard sorting request parameters.
 *
 * <p>This record encapsulates sorting parameters used across the platform,
 * ensuring consistent sorting behavior and validation.
 *
 * @param field the field to sort by
 * @param direction the sort direction (ASC or DESC)
 *
 * @since 1.0.0
 */
public record SortRequest(String field, String direction) {

    /**
     * Default sorting by timestamp descending (newest first).
     */
    public static final SortRequest DEFAULT = new SortRequest("timestamp", "DESC");

    /**
     * Valid sort directions.
     */
    public static final Set<String> VALID_DIRECTIONS = Set.of("ASC", "DESC");

    /**
     * Valid fields for audit log sorting.
     */
    public static final Set<String> VALID_AUDIT_FIELDS = Set.of(
        "timestamp", "action", "actorDisplayName", "resourceType", "outcome"
    );

    /**
     * Creates a sort request with validation.
     *
     * @param field the field to sort by
     * @param direction the sort direction
     */
    public SortRequest {
        if (field == null || field.trim().isEmpty()) {
            field = DEFAULT.field();
        }
        if (direction == null || direction.trim().isEmpty()) {
            direction = DEFAULT.direction();
        }

        direction = direction.toUpperCase();
        if (!VALID_DIRECTIONS.contains(direction)) {
            throw new IllegalArgumentException("Sort direction must be ASC or DESC");
        }
    }

    /**
     * Creates an ascending sort request.
     *
     * @param field the field to sort by
     * @return sort request with ASC direction
     */
    public static SortRequest ascending(String field) {
        return new SortRequest(field, "ASC");
    }

    /**
     * Creates a descending sort request.
     *
     * @param field the field to sort by
     * @return sort request with DESC direction
     */
    public static SortRequest descending(String field) {
        return new SortRequest(field, "DESC");
    }

    /**
     * Validates that the field is allowed for audit log sorting.
     *
     * @return true if the field is valid for audit logs
     */
    public boolean isValidAuditField() {
        return VALID_AUDIT_FIELDS.contains(field);
    }

    /**
     * Creates a Spring Sort object from this request.
     *
     * @return Spring Sort object
     */
    public org.springframework.data.domain.Sort toSpringSort() {
        org.springframework.data.domain.Sort.Direction springDirection =
            "ASC".equals(direction)
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        return org.springframework.data.domain.Sort.by(springDirection, field);
    }
}