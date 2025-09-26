package com.platform.user.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.user.internal.OrganizationService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "platform.multi-tenancy.enabled=true",
    "platform.invitations.expiration-hours=24"
})
@Transactional
class UserControllerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(UserControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        log.info("Setting up UserControllerIntegrationTest");
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testGetCurrentUser_ShouldReturnUserProfile() throws Exception {
        log.info("Testing GET /api/v1/users/me");

        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.name").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> userProfile = objectMapper.readValue(response, Map.class);

        assertTrue(userProfile.containsKey("id"));
        assertTrue(userProfile.containsKey("email"));
        assertTrue(userProfile.containsKey("name"));

        log.info("User profile retrieved successfully for user: {}", userProfile.get("email"));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testUpdateUserProfile_ShouldUpdateSuccessfully() throws Exception {
        log.info("Testing PUT /api/v1/users/me");

        Map<String, Object> updateRequest = Map.of(
            "name", "Updated Test User",
            "phone", "+1234567890",
            "timezone", "UTC"
        );

        String requestBody = objectMapper.writeValueAsString(updateRequest);

        MvcResult result = mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Test User"))
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(jsonPath("$.timezone").value("UTC"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> updatedProfile = objectMapper.readValue(response, Map.class);

        assertEquals("Updated Test User", updatedProfile.get("name"));
        assertEquals("+1234567890", updatedProfile.get("phone"));
        assertEquals("UTC", updatedProfile.get("timezone"));

        log.info("User profile updated successfully");
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testChangePassword_ShouldChangePasswordSuccessfully() throws Exception {
        log.info("Testing POST /api/v1/users/me/password");

        Map<String, Object> passwordRequest = Map.of(
            "currentPassword", "oldPassword123!",
            "newPassword", "newSecurePassword456!",
            "confirmPassword", "newSecurePassword456!"
        );

        String requestBody = objectMapper.writeValueAsString(passwordRequest);

        mockMvc.perform(post("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isNoContent())
                .andReturn();

        log.info("Password changed successfully");
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testChangePassword_InvalidCurrentPassword_ShouldReturnBadRequest() throws Exception {
        log.info("Testing POST /api/v1/users/me/password with invalid current password");

        Map<String, Object> passwordRequest = Map.of(
            "currentPassword", "wrongPassword",
            "newPassword", "newSecurePassword456!",
            "confirmPassword", "newSecurePassword456!"
        );

        String requestBody = objectMapper.writeValueAsString(passwordRequest);

        MvcResult result = mockMvc.perform(post("/api/v1/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> errorResponse = objectMapper.readValue(response, Map.class);

        assertTrue(errorResponse.containsKey("error"));
        assertTrue(errorResponse.get("message").toString().contains("current password"));

        log.info("Invalid current password correctly rejected");
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testGetUserOrganizations_ShouldReturnUserOrganizations() throws Exception {
        log.info("Testing GET /api/v1/users/me/organizations");

        MvcResult result = mockMvc.perform(get("/api/v1/users/me/organizations")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizations").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> organizationsResponse = objectMapper.readValue(response, Map.class);

        assertTrue(organizationsResponse.containsKey("organizations"));

        log.info("User organizations retrieved successfully");
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN"})
    void testCreateUser_ShouldCreateNewUser() throws Exception {
        log.info("Testing POST /api/v1/users");

        Map<String, Object> userRequest = Map.of(
            "email", "newuser@example.com",
            "name", "New Test User",
            "role", "USER"
        );

        String requestBody = objectMapper.writeValueAsString(userRequest);

        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.name").value("New Test User"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> createdUser = objectMapper.readValue(response, Map.class);

        assertTrue(createdUser.containsKey("id"));
        assertEquals("newuser@example.com", createdUser.get("email"));
        assertEquals("New Test User", createdUser.get("name"));

        log.info("New user created successfully with ID: {}", createdUser.get("id"));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"USER"})
    void testCreateUser_InsufficientPermissions_ShouldReturnForbidden() throws Exception {
        log.info("Testing POST /api/v1/users with insufficient permissions");

        Map<String, Object> userRequest = Map.of(
            "email", "newuser@example.com",
            "name", "New Test User",
            "role", "USER"
        );

        String requestBody = objectMapper.writeValueAsString(userRequest);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden())
                .andReturn();

        log.info("Insufficient permissions correctly rejected");
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN"})
    void testListUsers_ShouldReturnPaginatedUsers() throws Exception {
        log.info("Testing GET /api/v1/users");

        MvcResult result = mockMvc.perform(get("/api/v1/users")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "email,asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> usersPage = objectMapper.readValue(response, Map.class);

        assertTrue(usersPage.containsKey("content"));
        assertTrue(usersPage.containsKey("pageable"));
        assertTrue(usersPage.containsKey("totalElements"));

        log.info("Users list retrieved successfully");
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN"})
    void testSearchUsers_ShouldReturnFilteredResults() throws Exception {
        log.info("Testing GET /api/v1/users/search");

        MvcResult result = mockMvc.perform(get("/api/v1/users/search")
                .param("q", "test@example.com")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> searchResults = objectMapper.readValue(response, Map.class);

        assertTrue(searchResults.containsKey("content"));

        log.info("User search completed successfully");
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN"})
    void testDeactivateUser_ShouldDeactivateUser() throws Exception {
        log.info("Testing POST /api/v1/users/{userId}/deactivate");

        String userId = "test-user-id";

        mockMvc.perform(post("/api/v1/users/{userId}/deactivate", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andReturn();

        log.info("User deactivated successfully");
    }

    @Test
    @WithMockUser(username = "admin@example.com", authorities = {"ADMIN"})
    void testActivateUser_ShouldActivateUser() throws Exception {
        log.info("Testing POST /api/v1/users/{userId}/activate");

        String userId = "test-user-id";

        mockMvc.perform(post("/api/v1/users/{userId}/activate", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andReturn();

        log.info("User activated successfully");
    }

    @Test
    void testGetCurrentUser_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        log.info("Testing GET /api/v1/users/me without authentication");

        mockMvc.perform(get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();

        log.info("Unauthorized access correctly rejected");
    }
}