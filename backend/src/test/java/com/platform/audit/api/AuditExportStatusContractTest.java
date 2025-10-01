package com.platform.audit.api;
import com.platform.config.TestBeanConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the Audit Export Status API endpoint GET /api/audit/export/{exportId}/status.
 *
 * CRITICAL: These tests MUST FAIL initially as part of TDD RED phase.
 * Implementation should only be created after these tests are written and failing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestBeanConfiguration.class)
@ActiveProfiles("test")
@Transactional
class AuditExportStatusContractTest {

    @Autowired
    private MockMvc mockMvc;

    private static final UUID SAMPLE_EXPORT_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

    /**
     * Contract: GET /api/audit/export/{exportId}/status should return export status
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldReturnExportStatus() throws Exception {
        mockMvc.perform(get("/api/audit/export/{exportId}/status", SAMPLE_EXPORT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.exportId").value(SAMPLE_EXPORT_ID.toString()))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.progress").exists())
                .andExpect(jsonPath("$.requestedAt").exists());
    }

    /**
     * Contract: Should return status for pending export
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldReturnPendingStatus() throws Exception {
        UUID pendingExportId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

        mockMvc.perform(get("/api/audit/export/{exportId}/status", pendingExportId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").value(pendingExportId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.progress").value(0))
                .andExpect(jsonPath("$.downloadToken").doesNotExist())
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    /**
     * Contract: Should return status for processing export
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldReturnProcessingStatus() throws Exception {
        UUID processingExportId = UUID.fromString("660e8400-e29b-41d4-a716-446655440002");

        mockMvc.perform(get("/api/audit/export/{exportId}/status", processingExportId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").value(processingExportId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.progress").isNumber())
                .andExpect(jsonPath("$.downloadToken").doesNotExist())
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    /**
     * Contract: Should return status for completed export with download token
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldReturnCompletedStatus() throws Exception {
        UUID completedExportId = UUID.fromString("660e8400-e29b-41d4-a716-446655440003");

        mockMvc.perform(get("/api/audit/export/{exportId}/status", completedExportId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").value(completedExportId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value(100))
                .andExpect(jsonPath("$.downloadToken").exists())
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.entryCount").exists())
                .andExpect(jsonPath("$.fileSize").exists())
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    /**
     * Contract: Should return status for failed export with error message
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldReturnFailedStatus() throws Exception {
        UUID failedExportId = UUID.fromString("660e8400-e29b-41d4-a716-446655440004");

        mockMvc.perform(get("/api/audit/export/{exportId}/status", failedExportId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").value(failedExportId.toString()))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.progress").value(0))
                .andExpect(jsonPath("$.errorMessage").exists())
                .andExpect(jsonPath("$.downloadToken").doesNotExist())
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    /**
     * Contract: Should handle non-existent export ID
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldHandleNonExistentExportId() throws Exception {
        UUID nonExistentId = UUID.fromString("660e8400-e29b-41d4-a716-446655440999");

        mockMvc.perform(get("/api/audit/export/{exportId}/status", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXPORT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    /**
     * Contract: Should handle invalid UUID format for export ID
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldHandleInvalidExportIdFormat() throws Exception {
        mockMvc.perform(get("/api/audit/export/{exportId}/status", "invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID_FORMAT"))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Contract: Should require authentication for status check
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/audit/export/{exportId}/status", SAMPLE_EXPORT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Contract: Should enforce access control for export status
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "unauthorized@example.com", roles = {"USER"})
    void shouldEnforceAccessControl() throws Exception {
        // User should only be able to check status of their own exports
        mockMvc.perform(get("/api/audit/export/{exportId}/status", SAMPLE_EXPORT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Return 404 instead of 403 to prevent information disclosure
    }

    /**
     * Contract: Should return progress updates during processing
     * Expected to FAIL until AuditLogExportController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldReturnProgressUpdates() throws Exception {
        UUID processingExportId = UUID.fromString("660e8400-e29b-41d4-a716-446655440005");

        mockMvc.perform(get("/api/audit/export/{exportId}/status", processingExportId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").isNumber())
                .andExpect(jsonPath("$.progress").value(lessThan(100))); // In progress, not completed
    }
}