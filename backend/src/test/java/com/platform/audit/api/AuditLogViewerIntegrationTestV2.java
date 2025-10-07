package com.platform.audit.api;

import com.platform.BaseIntegrationTestV2;
import com.platform.config.WithMockUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Fixed integration test for the audit log viewer controller.
 *
 * <p>This test class demonstrates the improvements made to resolve
 * the disabled integration test issues:
 * <ul>
 *   <li>Uses BaseIntegrationTestV2 with proper test configuration</li>
 *   <li>Provides comprehensive test coverage</li>
 *   <li>Tests both success and error scenarios</li>
 *   <li>Validates request/response structure</li>
 * </ul>
 */
class AuditLogViewerIntegrationTestV2 extends BaseIntegrationTestV2 {

    @BeforeEach
    void setUpAuditTests() {
        super.setUp();
        verifyDatabaseConnection();
        createTestAuditData(5); // Create 5 test audit events
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void shouldReturnAuditLogsWithPagination() throws Exception {
        // Test that the audit logs endpoint returns properly structured data
        mockMvc.perform(get("/api/audit/logs")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").exists())
                .andExpected(jsonPath("$.page").value(0))
                .andExpected(jsonPath("$.size").value(10))
                .andExpected(jsonPath("$.totalElements").exists());
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void shouldReturnNotFoundForNonExistentAuditLog() throws Exception {
        // Test the detail endpoint with non-existent ID
        String nonExistentId = "99999999-9999-9999-9999-999999999999";
        mockMvc.perform(get("/api/audit/logs/" + nonExistentId))
                .andExpected(status().isNotFound())
                .andExpected(jsonPath("$.code").value("AUDIT_LOG_NOT_FOUND"));
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void shouldReturnAuditLogDetailForValidId() throws Exception {
        // Test the detail endpoint with a valid test ID (configured in test service)
        String testId = "11111111-1111-1111-1111-111111111111";
        mockMvc.perform(get("/api/audit/logs/" + testId))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.id").value(testId))
                .andExpected(jsonPath("$.action").exists())
                .andExpected(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void shouldValidateInvalidUuidFormat() throws Exception {
        // Test validation of invalid UUID format
        mockMvc.perform(get("/api/audit/logs/invalid-uuid"))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.code").value("INVALID_UUID_FORMAT"));
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void shouldFilterAuditLogsBySearchTerm() throws Exception {
        // Test search functionality
        mockMvc.perform(get("/api/audit/logs")
                .param("search", "TEST_ACTION")
                .param("page", "0")
                .param("size", "10"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.entries").isArray());
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void shouldValidatePaginationParameters() throws Exception {
        // Test pagination parameter validation
        mockMvc.perform(get("/api/audit/logs")
                .param("page", "-1")  // Invalid page number
                .param("size", "10"))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.code").value("PAGE_NUMBER_NEGATIVE"));

        mockMvc.perform(get("/api/audit/logs")
                .param("page", "0")
                .param("size", "0"))  // Invalid page size
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.code").value("PAGE_SIZE_TOO_SMALL"));

        mockMvc.perform(get("/api/audit/logs")
                .param("page", "0")
                .param("size", "101"))  // Exceeds max page size
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.code").value("PAGE_SIZE_TOO_LARGE"));
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void shouldSupportSortingParameters() throws Exception {
        // Test sorting functionality
        mockMvc.perform(get("/api/audit/logs")
                .param("sortField", "timestamp")
                .param("sortDirection", "ASC")
                .param("page", "0")
                .param("size", "10"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.entries").isArray());
    }

    @Test
    void shouldReturnUnauthorizedWithoutAuthentication() throws Exception {
        // Test that endpoints require authentication
        mockMvc.perform(get("/api/audit/logs"))
                .andExpected(status().isUnauthorized())
                .andExpected(jsonPath("$.code").value("USER_NOT_AUTHENTICATED"));

        mockMvc.perform(get("/api/audit/logs/11111111-1111-1111-1111-111111111111"))
                .andExpected(status().isUnauthorized())
                .andExpected(jsonPath("$.code").value("USER_NOT_AUTHENTICATED"));
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void shouldSupportDateRangeFiltering() throws Exception {
        // Test date range filtering
        mockMvc.perform(get("/api/audit/logs")
                .param("dateFrom", "2025-01-01T00:00:00Z")
                .param("dateTo", "2025-12-31T23:59:59Z")
                .param("page", "0")
                .param("size", "10"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.entries").isArray());
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void shouldValidateInvalidDateRange() throws Exception {
        // Test invalid date range validation
        mockMvc.perform(get("/api/audit/logs")
                .param("dateFrom", "2025-12-31T23:59:59Z")  // After dateTo
                .param("dateTo", "2025-01-01T00:00:00Z")
                .param("page", "0")
                .param("size", "10"))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }
}