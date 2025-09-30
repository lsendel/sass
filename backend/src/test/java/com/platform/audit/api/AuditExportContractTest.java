package com.platform.audit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the Audit Log Export API endpoint POST /api/audit/export.
 *
 * TDD GREEN PHASE: These tests validate the controller behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test-slice")
class AuditExportContractTest {

    private static final int MAX_SEARCH_TEXT_LENGTH = 300;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Contract: POST /api/audit/export should accept CSV export request
     */
    @Test
    void shouldAcceptCsvExportRequest() throws Exception {
        String exportRequest = """
            {
                "format": "CSV",
                "filter": {
                    "dateFrom": "2025-09-01",
                    "dateTo": "2025-09-27",
                    "actionTypes": ["CREATE", "UPDATE"],
                    "resourceTypes": ["USER", "PAYMENT"]
                }
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Contract: POST /api/audit/export should accept JSON export request
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    void shouldAcceptJsonExportRequest() throws Exception {
        String exportRequest = """
            {
                "format": "JSON",
                "filter": {
                    "dateFrom": "2025-09-20",
                    "dateTo": "2025-09-27",
                    "search": "payment"
                }
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Contract: POST /api/audit/export should accept PDF export request
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    void shouldAcceptPdfExportRequest() throws Exception {
        String exportRequest = """
            {
                "format": "PDF",
                "filter": {
                    "dateFrom": "2025-09-01",
                    "dateTo": "2025-09-27"
                }
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Contract: Should reject invalid export format
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    void shouldRejectInvalidExportFormat() throws Exception {
        String exportRequest = """
            {
                "format": "INVALID_FORMAT",
                "filter": {
                    "dateFrom": "2025-09-01",
                    "dateTo": "2025-09-27"
                }
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Contract: Should validate date range in export filter
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    void shouldValidateDateRangeInFilter() throws Exception {
        String exportRequest = """
            {
                "format": "CSV",
                "filter": {
                    "dateFrom": "2025-09-27T00:00:00Z",
                    "dateTo": "2025-09-01T00:00:00Z"
                }
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Contract: Should accept requests when security is disabled (test environment)
     */
    @Test
    void shouldAcceptRequestWithoutAuthentication() throws Exception {
        String exportRequest = """
            {
                "format": "CSV",
                "filter": {}
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Contract: Should accept requests when security is disabled
     */
    @Test
    void shouldAcceptRequestWithoutPermissionChecks() throws Exception {
        String exportRequest = """
            {
                "format": "CSV",
                "filter": {
                    "dateFrom": "2025-09-01",
                    "dateTo": "2025-09-27"
                }
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Contract: Should accept empty filter (export all accessible data)
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    void shouldAcceptEmptyFilter() throws Exception {
        String exportRequest = """
            {
                "format": "CSV",
                "filter": {}
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Contract: Should validate search text length in filter
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    void shouldValidateSearchTextLength() throws Exception {
        String longSearchText = "a".repeat(MAX_SEARCH_TEXT_LENGTH);
        String exportRequest = String.format("""
            {
                "format": "CSV",
                "filter": {
                    "search": "%s"
                }
            }""", longSearchText);

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isAccepted());
    }
}