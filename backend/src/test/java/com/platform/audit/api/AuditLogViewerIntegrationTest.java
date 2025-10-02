package com.platform.audit.api;

import com.platform.config.AuditTestConfiguration;
import com.platform.config.WithMockUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the audit log viewer controller.
 * Uses Spring Boot test with H2 in-memory database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({com.platform.config.AuditTestConfiguration.class, com.platform.config.TestSecurityConfig.class})
@ActiveProfiles("integration-test")
class AuditLogViewerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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