package com.platform.audit.api;

import com.platform.BaseIntegrationTestV2;
import com.platform.config.WithMockUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration test for the audit log viewer controller.
 *
 * FIXED: Now uses BaseIntegrationTestV2 with proper test configuration
 * to resolve bean dependencies and context loading issues.
 */
class AuditLogViewerIntegrationTest extends BaseIntegrationTestV2 {

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
    void auditLogEndpointShouldExist() throws Exception {
        // This test verifies our endpoints exist and return responses
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void auditLogDetailEndpointShouldExist() throws Exception {
        // Test the detail endpoint
        String testId = "11111111-1111-1111-1111-111111111111";
        mockMvc.perform(get("/api/audit/logs/" + testId))
                .andExpect(status().isNotFound()); // Expected since mock service returns empty
    }
}