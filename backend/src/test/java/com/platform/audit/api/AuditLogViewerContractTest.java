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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for the Audit Log Viewer API endpoint GET /api/audit/logs.
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
class AuditLogViewerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Contract: GET /api/audit/logs should return paginated audit logs for authenticated users
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldReturnPaginatedAuditLogs() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.currentPage").isNumber())
                .andExpect(jsonPath("$.pageSize").isNumber())
                .andExpect(jsonPath("$.hasNext").isBoolean())
                .andExpect(jsonPath("$.hasPrevious").isBoolean());
    }

    /**
     * Contract: GET /api/audit/logs with pagination parameters
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldAcceptPaginationParameters() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .param("page", "0")
                .param("size", "25")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(25));
    }

    /**
     * Contract: GET /api/audit/logs with search parameter
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldAcceptSearchParameter() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .param("search", "payment")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray());
    }

    /**
     * Contract: GET /api/audit/logs with date range filters
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldAcceptDateRangeFilters() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .param("dateFrom", "2025-09-01")
                .param("dateTo", "2025-09-27")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray());
    }

    /**
     * Contract: GET /api/audit/logs with action type filters
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldAcceptActionTypeFilters() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .param("actionTypes", "CREATE,UPDATE,DELETE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray());
    }

    /**
     * Contract: GET /api/audit/logs with resource type filters
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldAcceptResourceTypeFilters() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .param("resourceTypes", "USER,PAYMENT,SUBSCRIPTION")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray());
    }

    /**
     * Contract: GET /api/audit/logs with sorting parameters
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldAcceptSortingParameters() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .param("sortBy", "timestamp")
                .param("sortDirection", "DESC")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray());
    }

    /**
     * Contract: GET /api/audit/logs should validate page size limits
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldValidatePageSizeLimits() throws Exception {
        // Test maximum page size
        mockMvc.perform(get("/api/audit/logs")
                .param("size", "150")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Contract: GET /api/audit/logs should validate date range
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldValidateDateRange() throws Exception {
        // Test invalid date range (from > to)
        mockMvc.perform(get("/api/audit/logs")
                .param("dateFrom", "2025-09-27")
                .param("dateTo", "2025-09-01")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Contract: GET /api/audit/logs should require authentication
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Contract: Each audit log entry should have required fields
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void auditLogEntriesShouldHaveRequiredFields() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[*].id").exists())
                .andExpect(jsonPath("$.entries[*].timestamp").exists())
                .andExpect(jsonPath("$.entries[*].actionType").exists())
                .andExpect(jsonPath("$.entries[*].resourceType").exists())
                .andExpect(jsonPath("$.entries[*].description").exists())
                .andExpect(jsonPath("$.entries[*].outcome").exists())
                .andExpect(jsonPath("$.entries[*].severity").exists())
                .andExpect(jsonPath("$.entries[*].hasDetails").exists());
    }
}