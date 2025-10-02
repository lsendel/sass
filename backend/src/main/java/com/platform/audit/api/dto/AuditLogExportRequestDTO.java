package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * DTO for audit log export request.
 */
public record AuditLogExportRequestDTO(
    String format,
    ExportFilter filter
) {
    /**
     * Nested filter for export criteria.
     * Accepts dates in ISO format (with or without time component).
     */
    public record ExportFilter(
        @JsonDeserialize(using = FlexibleInstantDeserializer.class)
        Instant dateFrom,
        @JsonDeserialize(using = FlexibleInstantDeserializer.class)
        Instant dateTo,
        String search,
        List<String> actionTypes,
        List<String> resourceTypes,
        List<String> outcomes
    ) {}

    // Convenience methods for backward compatibility
    public Instant dateFrom() {
        return filter != null ? filter.dateFrom() : null;
    }

    public Instant dateTo() {
        return filter != null ? filter.dateTo() : null;
    }

    public String search() {
        return filter != null ? filter.search() : null;
    }

    public List<String> actionTypes() {
        return filter != null ? filter.actionTypes() : null;
    }

    public List<String> resourceTypes() {
        return filter != null ? filter.resourceTypes() : null;
    }

    // Export formats
    public static class Format {
        public static final String CSV = "CSV";
        public static final String JSON = "JSON";
        public static final String PDF = "PDF";
    }
}