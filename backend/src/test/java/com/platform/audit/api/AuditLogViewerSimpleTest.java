package com.platform.audit.api;

import com.platform.config.AuditTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simple test to verify our audit log viewer endpoints.
 * Tests the controller implementation with mocked dependencies.
 */
@WebMvcTest(AuditLogViewController.class)
@Import(AuditTestConfiguration.class)
class AuditLogViewerSimpleTest {

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