package com.platform.audit.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO for audit log export request.
 */
public record AuditLogExportRequestDTO(
    String format,
    Instant dateFrom,
    Instant dateTo,
    String search,
    List<String> actionTypes,
    List<String> resourceTypes,
    List<String> outcomes,
    boolean includeMetadata
) {
    // Export formats
    public static class Format {
        public static final String CSV = "CSV";
        public static final String JSON = "JSON";
        public static final String PDF = "PDF";
    }
}