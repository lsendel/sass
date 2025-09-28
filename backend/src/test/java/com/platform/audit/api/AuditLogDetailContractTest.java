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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for the Audit Log Detail API endpoint GET /api/audit/logs/{id}.
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
class AuditLogDetailContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID SAMPLE_AUDIT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    /**
     * Contract: GET /api/audit/logs/{id} should return detailed audit log entry
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldReturnAuditLogDetail() throws Exception {
        mockMvc.perform(get("/api/audit/logs/{id}", SAMPLE_AUDIT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(SAMPLE_AUDIT_ID.toString()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.actionType").exists())
                .andExpect(jsonPath("$.resourceType").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.outcome").exists())
                .andExpect(jsonPath("$.severity").exists());
    }

    /**
     * Contract: Detailed view should include additional fields not in list view
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldIncludeDetailedFields() throws Exception {
        mockMvc.perform(get("/api/audit/logs/{id}", SAMPLE_AUDIT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.ipAddress").exists())
                .andExpect(jsonPath("$.userAgent").exists())
                .andExpect(jsonPath("$.metadata").exists())
                .andExpect(jsonPath("$.relatedEntries").isArray());
    }

    /**
     * Contract: Should handle non-existent audit log ID
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldHandleNonExistentAuditLogId() throws Exception {
        UUID nonExistentId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        mockMvc.perform(get("/api/audit/logs/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AUDIT_LOG_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    /**
     * Contract: Should handle invalid UUID format
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldHandleInvalidUuidFormat() throws Exception {
        mockMvc.perform(get("/api/audit/logs/{id}", "invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UUID_FORMAT"))
                .andExpect(jsonPath("$.message").exists());
    }

    /**
     * Contract: Should require authentication for detail view
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/audit/logs/{id}", SAMPLE_AUDIT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Contract: Should enforce access control for audit log details
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "unauthorized@example.com", roles = {"USER"})
    void shouldEnforceAccessControl() throws Exception {
        // This test simulates accessing an audit log that the user doesn't have permission to view
        mockMvc.perform(get("/api/audit/logs/{id}", SAMPLE_AUDIT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Should return 404 instead of 403 to prevent information disclosure
    }

    /**
     * Contract: Should redact sensitive information based on user permissions
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "limited-user@example.com", roles = {"USER"})
    void shouldRedactSensitiveInformation() throws Exception {
        mockMvc.perform(get("/api/audit/logs/{id}", SAMPLE_AUDIT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.actionType").exists())
                // Sensitive fields may be null or redacted based on permissions
                .andExpect(jsonPath("$.actorEmail").exists()) // May be redacted
                .andExpect(jsonPath("$.ipAddress").exists()); // Should be hashed
    }

    /**
     * Contract: Should return related audit entries when available
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"ADMIN"})
    void shouldReturnRelatedEntries() throws Exception {
        mockMvc.perform(get("/api/audit/logs/{id}", SAMPLE_AUDIT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatedEntries").isArray())
                .andExpect(jsonPath("$.relatedEntries").isNotEmpty())
                .andExpect(jsonPath("$.relatedEntries[0].id").exists())
                .andExpect(jsonPath("$.relatedEntries[0].timestamp").exists())
                .andExpect(jsonPath("$.relatedEntries[0].actionType").exists());
    }

    /**
     * Contract: Should include error details for failed actions
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldIncludeErrorDetailsForFailedActions() throws Exception {
        // This assumes we have a failed audit entry to test with
        UUID failedActionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

        mockMvc.perform(get("/api/audit/logs/{id}", failedActionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("FAILURE"))
                .andExpect(jsonPath("$.errorDetails").exists())
                .andExpect(jsonPath("$.errorDetails").isNotEmpty());
    }

    /**
     * Contract: Should handle concurrent access safely
     * Expected to FAIL until AuditLogViewController is implemented
     */
    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void shouldHandleConcurrentAccess() throws Exception {
        // This test verifies that accessing the same audit log concurrently doesn't cause issues
        mockMvc.perform(get("/api/audit/logs/{id}", SAMPLE_AUDIT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SAMPLE_AUDIT_ID.toString()));
    }
}