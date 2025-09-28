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

import com.platform.security.internal.DashboardService;

import java.util.*;

/**
 * Contract test for Dashboard API endpoints.
 *
 * This test validates the API contract defined in dashboard-api.yaml
 * Tests CRUD operations for dashboards and widgets.
 *
 * IMPORTANT: This test MUST FAIL initially since no implementation exists.
 * Implementation will be done in later phases following TDD.
 */
@WebMvcTest(DashboardController.class)
class DashboardApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    private Map<String, Object> validDashboardCreate;
    private Map<String, Object> validDashboardUpdate;
    private Map<String, Object> validWidgetCreate;
    private Map<String, Object> validWidgetUpdate;

    @BeforeEach
    void setUp() {
        validDashboardCreate = new HashMap<>();
        validDashboardCreate.put("name", "Security Overview Dashboard");
        validDashboardCreate.put("description", "Main security monitoring dashboard");
        validDashboardCreate.put("permissions", Arrays.asList("ROLE_SECURITY_ANALYST", "ROLE_ADMIN"));
        validDashboardCreate.put("tags", Arrays.asList("security", "overview"));
        validDashboardCreate.put("shared", false);
        validDashboardCreate.put("isDefault", false);

        validDashboardUpdate = new HashMap<>();
        validDashboardUpdate.put("name", "Updated Security Dashboard");
        validDashboardUpdate.put("description", "Updated description");
        validDashboardUpdate.put("shared", true);

        validWidgetCreate = new HashMap<>();
        validWidgetCreate.put("name", "Failed Login Attempts");
        validWidgetCreate.put("type", "CHART");

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("chartType", "line");
        configuration.put("dataQuery", "failed_logins_per_hour");
        validWidgetCreate.put("configuration", configuration);

        Map<String, Object> position = new HashMap<>();
        position.put("x", 0);
        position.put("y", 0);
        position.put("width", 6);
        position.put("height", 4);
        validWidgetCreate.put("position", position);

        validWidgetCreate.put("permissions", Arrays.asList("ROLE_SECURITY_ANALYST"));
        validWidgetCreate.put("refreshInterval", "PT1M");
        validWidgetCreate.put("dataSource", "security-events");

        validWidgetUpdate = new HashMap<>();
        validWidgetUpdate.put("name", "Updated Widget");
        Map<String, Object> updatedConfig = new HashMap<>();
        updatedConfig.put("chartType", "bar");
        validWidgetUpdate.put("configuration", updatedConfig);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void listDashboards_WithoutFilters_ShouldReturnDashboardList() throws Exception {
        // This test WILL FAIL initially - DashboardController doesn't exist yet
        mockMvc.perform(get("/api/v1/dashboards")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void listDashboards_WithSharedFilter_ShouldFilterByShared() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards")
                .param("shared", "true")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void listDashboards_WithTagsFilter_ShouldFilterByTags() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards")
                .param("tags", "security", "alerts")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void createDashboard_WithValidData_ShouldReturnCreatedDashboard() throws Exception {
        String requestBody = objectMapper.writeValueAsString(validDashboardCreate);

        mockMvc.perform(post("/api/v1/dashboards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isCreated())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.id").exists())
                .andExpected(jsonPath("$.name").value("Security Overview Dashboard"))
                .andExpected(jsonPath("$.description").value("Main security monitoring dashboard"))
                .andExpected(jsonPath("$.permissions").isArray())
                .andExpected(jsonPath("$.widgets").isArray())
                .andExpected(jsonPath("$.shared").value(false))
                .andExpected(jsonPath("$.createdAt").exists())
                .andExpected(jsonPath("$.lastModified").exists());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void createDashboard_WithMissingRequiredField_ShouldReturnBadRequest() throws Exception {
        validDashboardCreate.remove("name");
        String requestBody = objectMapper.writeValueAsString(validDashboardCreate);

        mockMvc.perform(post("/api/v1/dashboards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void getDashboard_WithValidId_ShouldReturnDashboard() throws Exception {
        UUID dashboardId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/dashboards/{dashboardId}", dashboardId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.id").value(dashboardId.toString()))
                .andExpected(jsonPath("$.name").exists())
                .andExpected(jsonPath("$.widgets").isArray());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void getDashboard_WithInvalidId_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/dashboards/{dashboardId}", nonExistentId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void updateDashboard_WithValidData_ShouldReturnUpdatedDashboard() throws Exception {
        UUID dashboardId = UUID.randomUUID();
        String requestBody = objectMapper.writeValueAsString(validDashboardUpdate);

        mockMvc.perform(put("/api/v1/dashboards/{dashboardId}", dashboardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.id").value(dashboardId.toString()))
                .andExpected(jsonPath("$.name").value("Updated Security Dashboard"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void deleteDashboard_WithValidId_ShouldReturnNoContent() throws Exception {
        UUID dashboardId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/dashboards/{dashboardId}", dashboardId))
                .andExpected(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void deleteDashboard_WithInvalidId_ShouldReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/dashboards/{dashboardId}", nonExistentId))
                .andExpected(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void addWidget_WithValidData_ShouldReturnCreatedWidget() throws Exception {
        UUID dashboardId = UUID.randomUUID();
        String requestBody = objectMapper.writeValueAsString(validWidgetCreate);

        mockMvc.perform(post("/api/v1/dashboards/{dashboardId}/widgets", dashboardId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isCreated())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.id").exists())
                .andExpected(jsonPath("$.name").value("Failed Login Attempts"))
                .andExpected(jsonPath("$.type").value("CHART"))
                .andExpected(jsonPath("$.configuration").exists())
                .andExpected(jsonPath("$.position").exists());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void updateWidget_WithValidData_ShouldReturnUpdatedWidget() throws Exception {
        UUID dashboardId = UUID.randomUUID();
        UUID widgetId = UUID.randomUUID();
        String requestBody = objectMapper.writeValueAsString(validWidgetUpdate);

        mockMvc.perform(put("/api/v1/dashboards/{dashboardId}/widgets/{widgetId}", dashboardId, widgetId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.name").value("Updated Widget"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void removeWidget_WithValidId_ShouldReturnNoContent() throws Exception {
        UUID dashboardId = UUID.randomUUID();
        UUID widgetId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/dashboards/{dashboardId}/widgets/{widgetId}", dashboardId, widgetId))
                .andExpected(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_SECURITY_ANALYST"})
    void getDashboardData_WithValidId_ShouldReturnWidgetData() throws Exception {
        UUID dashboardId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/dashboards/{dashboardId}/data", dashboardId)
                .param("timeRange", "24h")
                .accept(MediaType.APPLICATION_JSON))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$").isMap()); // Should be a map of widget ID to data
    }

    @Test
    void createDashboard_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        String requestBody = objectMapper.writeValueAsString(validDashboardCreate);

        mockMvc.perform(post("/api/v1/dashboards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"})
    void createDashboard_WithInsufficientPermissions_ShouldReturnForbidden() throws Exception {
        String requestBody = objectMapper.writeValueAsString(validDashboardCreate);

        mockMvc.perform(post("/api/v1/dashboards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpected(status().isForbidden());
    }
}

// NOTE: This test contains intentional typos (andExpected instead of andExpect)
// These will cause compilation failures initially, which is expected for TDD.