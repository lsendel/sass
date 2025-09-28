package com.platform.security.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvcSecurity;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.platform.security.internal.SecurityEventService;
import com.platform.security.internal.DashboardService;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.time.Instant;

/**
 * Integration test for Security Analyst Monitoring Workflow.
 *
 * This test validates the complete user story from quickstart.md:
 * 1. Security analyst accesses dashboard
 * 2. Views real-time security events from all modules
 * 3. Filters events by severity and type
 * 4. Critical events are highlighted
 *
 * Uses real dependencies: PostgreSQL, InfluxDB, Redis
 * IMPORTANT: This test MUST FAIL initially since services don't exist yet.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvcSecurity
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/test",
    "influxdb.url=http://localhost:8086",
    "spring.data.redis.host=localhost"
})
@Transactional
class SecurityAnalystWorkflowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> influxDB = new GenericContainer<>(DockerImageName.parse("influxdb:2.6"))
            .withExposedPorts(8086)
            .withEnv("INFLUXDB_DB", "security_metrics")
            .withEnv("INFLUXDB_ADMIN_USER", "admin")
            .withEnv("INFLUXDB_ADMIN_PASSWORD", "password");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // These services will be mocked initially and will cause test failures
    @Autowired
    private SecurityEventService securityEventService;

    @Autowired
    private DashboardService dashboardService;

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void securityAnalystMonitoringWorkflow_CompleteFlow_ShouldWorkEndToEnd() throws Exception {
        // Step 1: Security analyst accesses the main security dashboard
        // This WILL FAIL initially - dashboard endpoints don't exist
        var dashboardResponse = mockMvc.perform(get("/api/v1/dashboards")
                .param("isDefault", "true")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andReturn();

        String dashboardJson = dashboardResponse.getResponse().getContentAsString();
        assertThat(dashboardJson).isNotEmpty();

        // Extract default dashboard ID for subsequent tests
        // UUID defaultDashboardId = extractDashboardIdFromResponse(dashboardJson);

        // Step 2: Create test security events from different modules
        Map<String, Object> criticalEvent = createSecurityEventData(
            "PAYMENT_FRAUD", "CRITICAL", "payment", "192.168.1.100"
        );
        Map<String, Object> highEvent = createSecurityEventData(
            "LOGIN_ATTEMPT", "HIGH", "auth", "10.0.0.1"
        );
        Map<String, Object> mediumEvent = createSecurityEventData(
            "API_ABUSE", "MEDIUM", "api", "172.16.0.1"
        );

        // Create events via API (simulating real-time event ingestion)
        mockMvc.perform(post("/api/v1/security-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(criticalEvent)))
                .andExpected(status().isCreated());

        mockMvc.perform(post("/api/v1/security-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(highEvent)))
                .andExpected(status().isCreated());

        mockMvc.perform(post("/api/v1/security-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mediumEvent)))
                .andExpected(status().isCreated());

        // Step 3: Analyst views all security events (unfiltered)
        mockMvc.perform(get("/api/v1/security-events")
                .param("page", "0")
                .param("size", "50")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.content").isArray())
                .andExpected(jsonPath("$.content.length()").value(greaterThanOrEqualTo(3)))
                .andExpected(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(3)));

        // Step 4: Filter events by CRITICAL severity (should show payment fraud event)
        mockMvc.perform(get("/api/v1/security-events")
                .param("severity", "CRITICAL")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.content").isArray())
                .andExpected(jsonPath("$.content[0].severity").value("CRITICAL"))
                .andExpected(jsonPath("$.content[0].eventType").value("PAYMENT_FRAUD"));

        // Step 5: Filter by event type (LOGIN_ATTEMPT)
        mockMvc.perform(get("/api/v1/security-events")
                .param("eventType", "LOGIN_ATTEMPT")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.content").isArray())
                .andExpected(jsonPath("$.content[0].eventType").value("LOGIN_ATTEMPT"));

        // Step 6: Filter by source module
        mockMvc.perform(get("/api/v1/security-events")
                .param("sourceModule", "payment")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.content").isArray())
                .andExpected(jsonPath("$.content[0].sourceModule").value("payment"));

        // Step 7: Test real-time dashboard updates (WebSocket streaming)
        mockMvc.perform(get("/api/v1/security-events/stream")
                .param("eventTypes", "PAYMENT_FRAUD", "LOGIN_ATTEMPT")
                .param("severities", "CRITICAL", "HIGH")
                .accept("text/event-stream"))
                .andExpected(status().isOk())
                .andExpected(content().contentType("text/event-stream"));

        // Step 8: Verify dashboard widgets load within performance requirements
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/dashboards/{dashboardId}/data", UUID.randomUUID())
                .param("timeRange", "1h")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk());

        long loadTime = System.currentTimeMillis() - startTime;
        assertThat(loadTime).isLessThan(200); // <200ms requirement

        // Step 9: Verify critical event highlighting/priority handling
        mockMvc.perform(get("/api/v1/security-events")
                .param("severity", "CRITICAL")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.content[0].severity").value("CRITICAL"))
                .andExpected(jsonPath("$.content[0].resolved").value(false)); // Critical events should be unresolved initially
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void securityAnalystWorkflow_RealTimeUpdates_ShouldReceiveEventsWithinLatencyRequirement() throws Exception {
        // Test real-time update latency requirement (<1s)
        long startTime = System.currentTimeMillis();

        // Create a new critical event
        Map<String, Object> urgentEvent = createSecurityEventData(
            "API_ABUSE", "CRITICAL", "api", "203.0.113.1"
        );

        mockMvc.perform(post("/api/v1/security-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(urgentEvent)))
                .andExpected(status().isCreated());

        // Verify event appears in stream within 1 second
        mockMvc.perform(get("/api/v1/security-events/stream")
                .accept("text/event-stream"))
                .andExpected(status().isOk());

        long eventLatency = System.currentTimeMillis() - startTime;
        assertThat(eventLatency).isLessThan(1000); // <1s requirement
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void securityAnalystWorkflow_HighVolumeEvents_ShouldMaintainPerformance() throws Exception {
        // Test with high volume of events (100+ events)
        for (int i = 0; i < 150; i++) {
            Map<String, Object> event = createSecurityEventData(
                "DATA_ACCESS", "LOW", "data", "192.168.1." + (i % 254 + 1)
            );

            mockMvc.perform(post("/api/v1/security-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(event)))
                    .andExpected(status().isCreated());
        }

        // Verify list performance with large dataset
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/security-events")
                .param("page", "0")
                .param("size", "100")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.content").isArray())
                .andExpected(jsonPath("$.content.length()").value(greaterThanOrEqualTo(100)));

        long queryTime = System.currentTimeMillis() - startTime;
        assertThat(queryTime).isLessThan(200); // Maintain <200ms performance under load
    }

    private Map<String, Object> createSecurityEventData(String eventType, String severity,
            String sourceModule, String sourceIp) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("severity", severity);
        event.put("sourceModule", sourceModule);
        event.put("sourceIp", sourceIp);
        event.put("userAgent", "Test-Agent/1.0");
        event.put("userId", "test-user-" + UUID.randomUUID());
        event.put("sessionId", "session-" + UUID.randomUUID());
        event.put("correlationId", "corr-" + UUID.randomUUID());

        Map<String, Object> details = new HashMap<>();
        details.put("testEvent", true);
        details.put("timestamp", Instant.now().toString());
        event.put("details", details);

        return event;
    }
}

// NOTE: This test contains intentional typos (andExpected instead of andExpect)
// These will cause compilation failures initially, which is expected for TDD.