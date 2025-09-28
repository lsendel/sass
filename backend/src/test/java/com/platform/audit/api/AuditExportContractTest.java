package com.platform.audit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for the Audit Log Export API endpoint POST /api/audit/export.
 *
 * CRITICAL: These tests MUST FAIL initially as part of TDD RED phase.
 * Implementation should only be created after these tests are written and failing.
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class AuditExportContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Contract: POST /api/audit/export should accept CSV export request
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
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
                .andExpect(jsonPath("$.exportId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requestedAt").exists())
                .andExpect(jsonPath("$.estimatedCompletionTime").exists());
    }

    /**
     * Contract: POST /api/audit/export should accept JSON export request
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
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
                .andExpect(jsonPath("$.exportId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Contract: POST /api/audit/export should accept PDF export request
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
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
                .andExpect(jsonPath("$.exportId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Contract: Should reject invalid export format
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
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
                .andExpect(jsonPath("$.code").value("INVALID_EXPORT_FORMAT"))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Contract: Should validate date range in export filter
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldValidateDateRangeInFilter() throws Exception {
        String exportRequest = """
            {
                "format": "CSV",
                "filter": {
                    "dateFrom": "2025-09-27",
                    "dateTo": "2025-09-01"
                }
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Contract: Should require authentication for export requests
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    void shouldRequireAuthentication() throws Exception {
        String exportRequest = """
            {
                "format": "CSV",
                "filter": {}
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Contract: Should enforce export permissions
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "no-export-permission@example.com", roles = {"USER"})
    void shouldEnforceExportPermissions() throws Exception {
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXPORT_PERMISSION_DENIED"))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Contract: Should handle missing request body
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldHandleMissingRequestBody() throws Exception {
        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Contract: Should validate required format field
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldValidateRequiredFormatField() throws Exception {
        String exportRequest = """
            {
                "filter": {
                    "dateFrom": "2025-09-01",
                    "dateTo": "2025-09-27"
                }
            }""";

        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_FIELD"))
                .andExpect(jsonPath("$.message").value("format is required"));
    }

    /**
     * Contract: Should handle rate limiting for export requests
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "rate-limited@example.com", roles = {"USER"})
    void shouldHandleRateLimiting() throws Exception {
        String exportRequest = """
            {
                "format": "CSV",
                "filter": {}
            }""";

        // First request should succeed
        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isAccepted());

        // Subsequent requests within rate limit window should be rejected
        mockMvc.perform(post("/api/audit/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportRequest))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Contract: Should accept empty filter (export all accessible data)
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
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
                .andExpect(jsonPath("$.exportId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Contract: Should validate search text length in filter
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldValidateSearchTextLength() throws Exception {
        String longSearchText = "a".repeat(300); // Exceeds 255 character limit
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_TEXT_TOO_LONG"))
                .andExpect(jsonPath("$.message").exists());
    }
}