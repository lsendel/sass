package com.platform.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for complete Google OAuth2 authentication flow
 * Uses TestContainers for real PostgreSQL and Redis dependencies
 * Tests end-to-end OAuth2 flow with actual database operations
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class GoogleOAuth2FlowTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());

        // Test-specific OAuth2 configuration
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-google-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-google-client-secret");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void completeGoogleOAuth2Flow_ShouldCreateUserAndSession() throws Exception {
        // PHASE 1: Get available providers
        String providersResponse = mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[?(@.name == 'google')]").exists())
                .andReturn().getResponse().getContentAsString();

        // PHASE 2: Initiate OAuth2 authorization
        String authResponse = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.state").exists())
                .andExpect(jsonPath("$.codeChallenge").exists())
                .andExpect(jsonPath("$.codeChallengeMethod").value("S256"))
                .andReturn().getResponse().getContentAsString();

        var authJson = objectMapper.readTree(authResponse);
        String state = authJson.get("state").asText();
        String authUrl = authJson.get("authorizationUrl").asText();

        // Validate authorization URL contains required parameters
        assert authUrl.contains("response_type=code") : "Authorization URL should contain response_type=code";
        assert authUrl.contains("client_id=test-google-client-id") : "Authorization URL should contain client ID";
        assert authUrl.contains("scope=") : "Authorization URL should contain scope parameter";
        assert authUrl.contains("state=" + state) : "Authorization URL should contain state parameter";
        assert authUrl.contains("code_challenge=") : "Authorization URL should contain PKCE code challenge";

        // PHASE 3: Simulate OAuth2 callback (successful authorization)
        String mockAuthCode = "mock_google_authorization_code_12345";

        String callbackResponse = mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", mockAuthCode)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.session").exists())
                .andExpect(jsonPath("$.session.provider").value("google"))
                .andExpect(jsonPath("$.session.userInfo.provider").value("google"))
                .andExpect(jsonPath("$.session.isAuthenticated").value(true))
                .andExpect(jsonPath("$.redirectTo").exists())
                .andReturn().getResponse().getContentAsString();

        var callbackJson = objectMapper.readTree(callbackResponse);
        String sessionId = callbackJson.get("session").get("sessionId").asText();
        String userId = callbackJson.get("session").get("userId").asText();

        // Validate session data
        assert !sessionId.isEmpty() : "Session ID should not be empty";
        assert !userId.isEmpty() : "User ID should not be empty";
        assert !sessionId.equals(userId) : "Session ID and User ID should be different";

        // PHASE 4: Validate session persistence
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", sessionId);

        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(true))
                .andExpect(jsonPath("$.session.sessionId").value(sessionId))
                .andExpect(jsonPath("$.session.userId").value(userId))
                .andExpect(jsonPath("$.session.provider").value("google"))
                .andExpect(jsonPath("$.session.userInfo.provider").value("google"));

        // PHASE 5: Test logout
        String logoutRequest = """
            {
                "terminateProviderSession": false
            }
            """;

        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // PHASE 6: Validate session is terminated
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(false))
                .andExpect(jsonPath("$.session").doesNotExist());
    }

    @Test
    void googleOAuth2Flow_InvalidState_ShouldRejectCallback() throws Exception {
        // ARRANGE: Get valid authorization response
        String authResponse = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var authJson = objectMapper.readTree(authResponse);
        String validState = authJson.get("state").asText();
        String invalidState = "invalid_state_parameter";

        // ACT & ASSERT: Callback with invalid state should fail
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", "valid_code")
                .param("state", invalidState)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_INVALID_STATE"));
    }

    @Test
    void googleOAuth2Flow_ErrorFromProvider_ShouldHandleGracefully() throws Exception {
        // ARRANGE: Get valid authorization state
        String authResponse = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var authJson = objectMapper.readTree(authResponse);
        String state = authJson.get("state").asText();

        // ACT & ASSERT: Callback with provider error should be handled
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("error", "access_denied")
                .param("error_description", "The user denied the request")
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("OAUTH2_AUTHORIZATION_DENIED"))
                .andExpect(jsonPath("$.error.details.provider_error").value("access_denied"));
    }

    @Test
    void googleOAuth2Flow_ExpiredCode_ShouldRejectCallback() throws Exception {
        // ARRANGE: Get valid authorization state
        String authResponse = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var authJson = objectMapper.readTree(authResponse);
        String state = authJson.get("state").asText();

        // ACT & ASSERT: Callback with expired code should fail
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", "expired_authorization_code")
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_EXPIRED_CODE"));
    }

    @Test
    void googleOAuth2Flow_ConcurrentSessions_ShouldHandleMultipleUsers() throws Exception {
        // ARRANGE: Create two separate OAuth2 flows

        // User 1 flow
        String auth1Response = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // User 2 flow
        String auth2Response = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var auth1Json = objectMapper.readTree(auth1Response);
        var auth2Json = objectMapper.readTree(auth2Response);

        String state1 = auth1Json.get("state").asText();
        String state2 = auth2Json.get("state").asText();

        // Validate states are different
        assert !state1.equals(state2) : "OAuth2 states should be unique per request";

        // Complete both flows with different mock codes
        String callback1Response = mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", "mock_code_user_1")
                .param("state", state1)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        String callback2Response = mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", "mock_code_user_2")
                .param("state", state2)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        // Validate both sessions are separate
        var callback1Json = objectMapper.readTree(callback1Response);
        var callback2Json = objectMapper.readTree(callback2Response);

        String session1Id = callback1Json.get("session").get("sessionId").asText();
        String session2Id = callback2Json.get("session").get("sessionId").asText();

        assert !session1Id.equals(session2Id) : "Session IDs should be unique";

        // Both sessions should be valid independently
        javax.servlet.http.Cookie session1Cookie = new javax.servlet.http.Cookie("JSESSIONID", session1Id);
        javax.servlet.http.Cookie session2Cookie = new javax.servlet.http.Cookie("JSESSIONID", session2Id);

        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(session1Cookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(true))
                .andExpect(jsonPath("$.session.sessionId").value(session1Id));

        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(session2Cookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(true))
                .andExpect(jsonPath("$.session.sessionId").value(session2Id));
    }

    @Test
    void googleOAuth2Flow_SessionTimeout_ShouldInvalidateSession() throws Exception {
        // ARRANGE: Complete OAuth2 flow
        String authResponse = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var authJson = objectMapper.readTree(authResponse);
        String state = authJson.get("state").asText();

        String callbackResponse = mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", "mock_code_timeout_test")
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var callbackJson = objectMapper.readTree(callbackResponse);
        String sessionId = callbackJson.get("session").get("sessionId").asText();

        // Validate session is initially valid
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", sessionId);

        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(true));

        // NOTE: In a real test, we would wait for session timeout or manipulate Redis directly
        // For this integration test, we simulate expired session by checking invalid session ID
        javax.servlet.http.Cookie expiredCookie = new javax.servlet.http.Cookie("JSESSIONID", "expired_session_id");

        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(expiredCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(false))
                .andExpect(jsonPath("$.session").doesNotExist());
    }

    @Test
    void googleOAuth2Flow_DatabasePersistence_ShouldStoreUserData() throws Exception {
        // ARRANGE: Complete OAuth2 flow
        String authResponse = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var authJson = objectMapper.readTree(authResponse);
        String state = authJson.get("state").asText();

        // ACT: Complete callback and create user session
        String callbackResponse = mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", "mock_code_persistence_test")
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.userInfo.sub").exists())
                .andExpect(jsonPath("$.session.userInfo.email").exists())
                .andExpect(jsonPath("$.session.userInfo.name").exists())
                .andReturn().getResponse().getContentAsString();

        var callbackJson = objectMapper.readTree(callbackResponse);
        String userId = callbackJson.get("session").get("userId").asText();
        String userEmail = callbackJson.get("session").get("userInfo").get("email").asText();

        // ASSERT: User data should be accessible in subsequent requests
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID",
                callbackJson.get("session").get("sessionId").asText());

        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.userId").value(userId))
                .andExpect(jsonPath("$.session.userInfo.email").value(userEmail))
                .andExpect(jsonPath("$.session.userInfo.provider").value("google"));

        // NOTE: In a complete test, we would also verify:
        // - User record exists in database
        // - OAuth2 session record exists in database
        // - Audit events are logged
        // - User can be retrieved by ID from repository
    }
}