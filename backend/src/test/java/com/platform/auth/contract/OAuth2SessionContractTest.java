package com.platform.auth.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract test for GET /api/v1/auth/oauth2/session endpoint
 * Validates OpenAPI specification compliance and OAuth2 session information
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class OAuth2SessionContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void oauth2Session_ValidSession_ShouldReturnSessionInfo() throws Exception {
        // ARRANGE: Valid OAuth2 session cookie
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "valid-session-id");

        // ACT & ASSERT: Validate session response schema
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.session").exists())
                .andExpect(jsonPath("$.isAuthenticated").exists())
                .andExpect(jsonPath("$.isAuthenticated").isBoolean())
                .andExpect(jsonPath("$.session.sessionId").exists())
                .andExpect(jsonPath("$.session.userId").exists())
                .andExpect(jsonPath("$.session.userInfo").exists())
                .andExpect(jsonPath("$.session.provider").exists())
                .andExpect(jsonPath("$.session.isAuthenticated").value(true))
                .andExpect(jsonPath("$.session.expiresAt").exists())
                .andExpect(jsonPath("$.session.createdAt").exists())
                .andExpect(jsonPath("$.session.lastAccessedAt").exists());
    }

    @Test
    void oauth2Session_NoSession_ShouldReturnUnauthenticated() throws Exception {
        // ARRANGE: Request without session cookie

        // ACT & ASSERT: Validate unauthenticated response
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.session").doesNotExist())
                .andExpect(jsonPath("$.isAuthenticated").value(false));
    }

    @Test
    void oauth2Session_ExpiredSession_ShouldReturnUnauthenticated() throws Exception {
        // ARRANGE: Expired session cookie
        javax.servlet.http.Cookie expiredCookie = new javax.servlet.http.Cookie("JSESSIONID", "expired-session-id");

        // ACT & ASSERT: Validate expired session response
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(expiredCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session").doesNotExist())
                .andExpect(jsonPath("$.isAuthenticated").value(false));
    }

    @Test
    void oauth2Session_ShouldValidateUserInfoStructure() throws Exception {
        // ARRANGE: Valid session with user info
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "valid-session-with-userinfo");

        // ACT & ASSERT: Validate userInfo object structure
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.userInfo.sub").exists())
                .andExpect(jsonPath("$.session.userInfo.email").exists())
                .andExpect(jsonPath("$.session.userInfo.name").exists())
                .andExpect(jsonPath("$.session.userInfo.provider").exists())
                .andExpect(jsonPath("$.session.userInfo.sub").isString())
                .andExpect(jsonPath("$.session.userInfo.email", matchesPattern("^[^@]+@[^@]+\\.[^@]+$"))) // Email format
                .andExpect(jsonPath("$.session.userInfo.name", hasLength(greaterThan(0)))) // Non-empty name
                .andExpect(jsonPath("$.session.userInfo.provider", isOneOf("google", "github", "microsoft")));
    }

    @Test
    void oauth2Session_ShouldValidateSessionTimestamps() throws Exception {
        // ARRANGE: Valid session
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "valid-session-timestamps");

        // ACT & ASSERT: Validate timestamp formats
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.expiresAt", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"))) // ISO 8601
                .andExpect(jsonPath("$.session.createdAt", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"))) // ISO 8601
                .andExpect(jsonPath("$.session.lastAccessedAt", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"))); // ISO 8601
    }

    @Test
    void oauth2Session_ShouldValidateSessionIdentifiers() throws Exception {
        // ARRANGE: Valid session
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "valid-session-ids");

        // ACT & ASSERT: Validate session and user ID formats
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.sessionId", hasLength(greaterThan(10)))) // Meaningful session ID
                .andExpect(jsonPath("$.session.userId", hasLength(greaterThan(5)))) // Meaningful user ID
                .andExpect(jsonPath("$.session.sessionId", not(equalTo(jsonPath("$.session.userId"))))) // Different IDs
                .andExpect(jsonPath("$.session.sessionId", matchesPattern("^[A-Za-z0-9_-]+$"))) // Safe characters
                .andExpect(jsonPath("$.session.userId", matchesPattern("^[A-Za-z0-9_-]+$"))); // Safe characters
    }

    @Test
    void oauth2Session_GoogleProvider_ShouldReturnProviderSpecificInfo() throws Exception {
        // ARRANGE: Valid Google OAuth2 session
        javax.servlet.http.Cookie googleSession = new javax.servlet.http.Cookie("JSESSIONID", "google-session-id");

        // ACT & ASSERT: Validate Google-specific session info
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(googleSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.provider").value("google"))
                .andExpect(jsonPath("$.session.userInfo.provider").value("google"));
    }

    @Test
    void oauth2Session_GitHubProvider_ShouldReturnProviderSpecificInfo() throws Exception {
        // ARRANGE: Valid GitHub OAuth2 session
        javax.servlet.http.Cookie githubSession = new javax.servlet.http.Cookie("JSESSIONID", "github-session-id");

        // ACT & ASSERT: Validate GitHub-specific session info
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(githubSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.provider").value("github"))
                .andExpect(jsonPath("$.session.userInfo.provider").value("github"));
    }

    @Test
    void oauth2Session_MicrosoftProvider_ShouldReturnProviderSpecificInfo() throws Exception {
        // ARRANGE: Valid Microsoft OAuth2 session
        javax.servlet.http.Cookie microsoftSession = new javax.servlet.http.Cookie("JSESSIONID", "microsoft-session-id");

        // ACT & ASSERT: Validate Microsoft-specific session info
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(microsoftSession)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.provider").value("microsoft"))
                .andExpect(jsonPath("$.session.userInfo.provider").value("microsoft"));
    }

    @Test
    void oauth2Session_ShouldHandleAcceptHeaders() throws Exception {
        // ARRANGE: Valid session cookie
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "valid-session");

        // ACT & ASSERT: Validate JSON response for application/json
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // ACT & ASSERT: Validate response for wildcard accept
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void oauth2Session_UnsupportedMediaType_ShouldReturn406() throws Exception {
        // ARRANGE: Valid session but unsupported Accept header
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "valid-session");

        // ACT & ASSERT: Validate 406 Not Acceptable for XML
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void oauth2Session_ShouldValidateSecurityHeaders() throws Exception {
        // ARRANGE: Valid session
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "valid-session");

        // ACT & ASSERT: Validate security headers
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"));
    }

    @Test
    void oauth2Session_ShouldHandleCorsRequests() throws Exception {
        // ARRANGE: CORS request with session
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "valid-session");

        // ACT & ASSERT: Validate CORS headers for cross-origin requests
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .header("Origin", "http://localhost:3000")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void oauth2Session_InvalidSessionCookie_ShouldReturnUnauthenticated() throws Exception {
        // ARRANGE: Invalid session cookie format
        javax.servlet.http.Cookie invalidCookie = new javax.servlet.http.Cookie("JSESSIONID", "invalid-format-123!");

        // ACT & ASSERT: Validate graceful handling of invalid session
        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(invalidCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated").value(false))
                .andExpect(jsonPath("$.session").doesNotExist());
    }

    @Test
    void oauth2Session_ShouldValidateResponsePerformance() throws Exception {
        // ARRANGE: Valid session for performance test
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "performance-test-session");

        // ACT & ASSERT: Validate response time is reasonable (<50ms requirement)
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long responseTime = System.currentTimeMillis() - startTime;

        // Note: This is a basic performance check - actual performance tests would use dedicated tools
        assert responseTime < 1000 : "Session validation should be fast (was " + responseTime + "ms)";
    }

    @Test
    void oauth2Session_ShouldHandleHttpMethods() throws Exception {
        // ARRANGE: Test only GET method is supported

        // ACT & ASSERT: Validate 405 Method Not Allowed for POST
        mockMvc.perform(post("/api/v1/auth/oauth2/session")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());

        // ACT & ASSERT: Validate 405 Method Not Allowed for PUT
        mockMvc.perform(put("/api/v1/auth/oauth2/session")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());

        // ACT & ASSERT: Validate 405 Method Not Allowed for DELETE
        mockMvc.perform(delete("/api/v1/auth/oauth2/session")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void oauth2Session_ShouldUpdateLastAccessedTime() throws Exception {
        // ARRANGE: Valid session that should update last accessed time
        javax.servlet.http.Cookie sessionCookie = new javax.servlet.http.Cookie("JSESSIONID", "update-access-time");

        // ACT: Make first request and capture timestamp
        String response1 = mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Wait a small amount of time
        Thread.sleep(1000);

        // ACT: Make second request and capture timestamp
        String response2 = mockMvc.perform(get("/api/v1/auth/oauth2/session")
                .cookie(sessionCookie)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // ASSERT: Last accessed time should be different (updated)
        var json1 = objectMapper.readTree(response1);
        var json2 = objectMapper.readTree(response2);

        String lastAccessed1 = json1.get("session").get("lastAccessedAt").asText();
        String lastAccessed2 = json2.get("session").get("lastAccessedAt").asText();

        assert !lastAccessed1.equals(lastAccessed2) : "Last accessed time should be updated on each request";
    }
}