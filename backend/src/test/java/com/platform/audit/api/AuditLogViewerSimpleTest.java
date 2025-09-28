package com.platform.audit.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simple test to verify our audit log viewer endpoints.
 * This test should FAIL since we haven't implemented the controller yet.
 */
@WebMvcTest
class AuditLogViewerSimpleTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void auditLogEndpointShouldNotExist() throws Exception {
        // This test verifies our endpoints don't exist yet (RED phase)
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isNotFound());
    }
}