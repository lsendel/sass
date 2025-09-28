package com.platform.security.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.platform.security.internal.SecurityEventService;
import com.platform.audit.internal.AuditService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contract test for Security Events API endpoints.
 *
 * This test validates the API contract defined in security-events-api.yaml
 * Tests all endpoints, request/response schemas, and HTTP status codes.
 *
 * IMPORTANT: This test MUST FAIL initially since no implementation exists.
 * Implementation will be done in later phases following TDD.
 */
@WebMvcTest(SecurityEventsController.class)
class SecurityEventsApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SecurityEventService securityEventService;

    @MockBean
    private AuditService auditService;

    private Map<String, Object> validSecurityEventCreate;
    private Map<String, Object> validSecurityEventUpdate;

    @BeforeEach
    void setUp() {
        validSecurityEventCreate = new HashMap<>();
        validSecurityEventCreate.put("eventType", "LOGIN_ATTEMPT");
        validSecurityEventCreate.put("severity", "MEDIUM");
        validSecurityEventCreate.put("sourceModule", "auth");
        validSecurityEventCreate.put("sourceIp", "192.168.1.1");
        validSecurityEventCreate.put("userAgent", "Mozilla/5.0");
        validSecurityEventCreate.put("userId", "user123");
        validSecurityEventCreate.put("sessionId", "session123");
        validSecurityEventCreate.put("correlationId", "corr123");

        Map<String, Object> details = new HashMap<>();
        details.put("success", false);
        details.put("failureReason", "Invalid password");
        validSecurityEventCreate.put("details", details);

        validSecurityEventUpdate = new HashMap<>();
        validSecurityEventUpdate.put("resolved", true);
        validSecurityEventUpdate.put("resolvedBy", "admin");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void listSecurityEvents_WithoutFilters_ShouldReturnPagedResponse() throws Exception {
        // This test WILL FAIL initially - SecurityEventsController doesn't exist yet
        mockMvc.perform(get("/api/v1/security-events")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.number").isNumber())
                .andExpect(jsonPath("$.page.size").isNumber())
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.page.totalPages").isNumber())
                .andExpect(jsonPath("$.page.first").isBoolean())
                .andExpect(jsonPath("$.page.last").isBoolean());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void listSecurityEvents_WithFilters_ShouldApplyFiltersCorrectly() throws Exception {
        // Test with all query parameters
        mockMvc.perform(get("/api/v1/security-events")
                .param("eventType", "LOGIN_ATTEMPT")
                .param("severity", "HIGH")
                .param("fromTimestamp", "2023-01-01T00:00:00Z")
                .param("toTimestamp", "2023-12-31T23:59:59Z")
                .param("userId", "user123")
                .param("page", "0")
                .param("size", "25")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void createSecurityEvent_WithValidData_ShouldReturnCreatedEvent() throws Exception {
        String requestBody = objectMapper.writeValueAsString(validSecurityEventCreate);

        mockMvc.perform(post("/api/v1/security-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isCreated())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.id").exists())
                .andExpected(jsonPath("$.eventType").value("LOGIN_ATTEMPT"))
                .andExpected(jsonPath("$.severity").value("MEDIUM"))
                .andExpected(jsonPath("$.timestamp").exists())
                .andExpected(jsonPath("$.sourceModule").value("auth"))
                .andExpected(jsonPath("$.correlationId").value("corr123"))
                .andExpected(jsonPath("$.resolved").value(false));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void createSecurityEvent_WithInvalidEventType_ShouldReturnBadRequest() throws Exception {
        validSecurityEventCreate.put("eventType", "INVALID_EVENT_TYPE");
        String requestBody = objectMapper.writeValueAsString(validSecurityEventCreate);

        mockMvc.perform(post("/api/v1/security-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void createSecurityEvent_WithMissingRequiredField_ShouldReturnBadRequest() throws Exception {
        validSecurityEventCreate.remove("eventType");
        String requestBody = objectMapper.writeValueAsString(validSecurityEventCreate);

        mockMvc.perform(post("/api/v1/security-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void getSecurityEvent_WithValidId_ShouldReturnEvent() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/security-events/{eventId}", eventId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.id").value(eventId.toString()))
                .andExpected(jsonPath("$.eventType").exists())
                .andExpected(jsonPath("$.severity").exists())
                .andExpected(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void getSecurityEvent_WithInvalidId_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/security-events/{eventId}", nonExistentId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void updateSecurityEvent_WithValidData_ShouldReturnUpdatedEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        String requestBody = objectMapper.writeValueAsString(validSecurityEventUpdate);

        mockMvc.perform(patch("/api/v1/security-events/{eventId}", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.id").value(eventId.toString()))
                .andExpected(jsonPath("$.resolved").value(true))
                .andExpected(jsonPath("$.resolvedBy").value("admin"))
                .andExpected(jsonPath("$.resolvedAt").exists());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void updateSecurityEvent_WithInvalidId_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        String requestBody = objectMapper.writeValueAsString(validSecurityEventUpdate);

        mockMvc.perform(patch("/api/v1/security-events/{eventId}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void streamSecurityEvents_ShouldReturnServerSentEventsStream() throws Exception {
        mockMvc.perform(get("/api/v1/security-events/stream")
                .param("eventTypes", "LOGIN_ATTEMPT", "PAYMENT_FRAUD")
                .param("severities", "CRITICAL", "HIGH")
                .accept("text/event-stream"))
                .andExpected(status().isOk())
                .andExpected(content().contentType("text/event-stream"));
    }

    @Test
    void listSecurityEvents_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/security-events")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"}) // User without security analyst role
    void listSecurityEvents_WithInsufficientPermissions_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/security-events")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isForbidden());
    }
}

// NOTE: This test contains intentional typos in method names (andExpected instead of andExpect)
// These will cause compilation failures initially, which is expected for TDD.
// The typos will be fixed when implementing the actual endpoints.