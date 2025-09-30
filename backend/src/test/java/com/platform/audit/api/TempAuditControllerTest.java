package com.platform.audit.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Direct test for TempAuditController to verify TDD GREEN phase.
 * This test verifies that the minimal audit log endpoint returns 200 OK
 * instead of 404, proving the GREEN phase is achieved.
 */
@WebMvcTest(excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class TempAuditControllerTest {

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void verifyTddGreenPhaseAuditLogsEndpoint() throws Exception {
        // This test verifies GREEN phase - endpoint should return 200 OK with empty data
        mockMvc.perform(get("/api/audit/logs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(DEFAULT_PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}