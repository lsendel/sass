package com.platform.audit.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object for export request responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportResponseDTO(
    String exportId,
    String status,
    String message
) {

    /**
     * Create a pending export response.
     */
    public static ExportResponseDTO pending(String exportId) {
        return new ExportResponseDTO(
            exportId,
            "PENDING",
            "Export request created and will be processed shortly"
        );
    }

    /**
     * Create an error export response.
     */
    public static ExportResponseDTO error(String message) {
        return new ExportResponseDTO(
            null,
            "ERROR",
            message
        );
    }
}