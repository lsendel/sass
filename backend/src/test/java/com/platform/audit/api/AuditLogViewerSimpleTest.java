package com.platform.audit.api;

import com.platform.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simple test to verify our audit log viewer endpoints.
 * Uses full Spring context from BaseIntegrationTest.
 */
class AuditLogViewerSimpleTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "USER")
    void auditLogEndpointShouldExist() throws Exception {
        // This test verifies our endpoints exist and return responses (GREEN phase)
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isOk());
    }
}