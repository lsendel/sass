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
 * Contract test for POST /api/v1/auth/oauth2/logout endpoint
 * Validates OpenAPI specification compliance and OAuth2 logout functionality
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class OAuth2LogoutContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void oauth2Logout_ValidSession_ShouldReturnSuccessResponse() throws Exception {
        // ARRANGE: Valid logout request with authenticated session
        String logoutRequest = """
            {
                "terminateProviderSession": false
            }
            """;

        // ACT & ASSERT: Validate successful logout response schema
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.redirectTo").exists());
    }

    @Test
    void oauth2Logout_WithProviderTermination_ShouldReturnSuccessWithRedirect() throws Exception {
        // ARRANGE: Logout request with provider session termination
        String logoutRequest = """
            {
                "terminateProviderSession": true,
                "redirectUri": "http://localhost:3000/goodbye"
            }
            """;

        // ACT & ASSERT: Validate logout with provider termination
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.redirectTo").exists())
                .andExpect(jsonPath("$.redirectTo", containsString("http://localhost:3000/goodbye")))
                .andExpect(jsonPath("$.message", containsString("logged out")));
    }

    @Test
    void oauth2Logout_EmptyBody_ShouldUseDefaults() throws Exception {
        // ARRANGE: Empty logout request body (should use defaults)
        String emptyRequest = "{}";

        // ACT & ASSERT: Validate default behavior for empty request
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.redirectTo").exists());
    }

    @Test
    void oauth2Logout_NoSession_ShouldReturnUnauthorized() throws Exception {
        // ARRANGE: Logout request without valid session

        String logoutRequest = """
            {
                "terminateProviderSession": false
            }
            """;

        // ACT & ASSERT: Validate 401 Unauthorized for missing session
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message", containsString("not authenticated")))
                .andExpect(jsonPath("$.error.timestamp").exists());
    }

    @Test
    void oauth2Logout_InvalidJson_ShouldReturnBadRequest() throws Exception {
        // ARRANGE: Invalid JSON in request body
        String invalidJson = "{ terminateProviderSession: true }"; // Missing quotes

        // ACT & ASSERT: Validate 400 Bad Request for invalid JSON
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message", containsString("JSON")));
    }

    @Test
    void oauth2Logout_InvalidContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // ARRANGE: Request with unsupported content type
        String logoutRequest = "terminateProviderSession=false";

        // ACT & ASSERT: Validate 415 Unsupported Media Type
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void oauth2Logout_ShouldValidateRequestStructure() throws Exception {
        // ARRANGE: Logout request with all optional fields
        String logoutRequest = """
            {
                "terminateProviderSession": true,
                "redirectUri": "https://example.com/logout-complete"
            }
            """;

        // ACT & ASSERT: Validate request field handling
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.redirectTo", containsString("https://example.com/logout-complete")));
    }

    @Test
    void oauth2Logout_ShouldClearSessionCookie() throws Exception {
        // ARRANGE: Valid logout request with session cookie
        String logoutRequest = """
            {
                "terminateProviderSession": false
            }
            """;

        // ACT & ASSERT: Validate session cookie is cleared
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .cookie(new javax.servlet.http.Cookie("JSESSIONID", "test-session-id"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("JSESSIONID=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0"))); // Cookie deletion
    }

    @Test
    void oauth2Logout_ShouldValidateSecurityHeaders() throws Exception {
        // ARRANGE: Standard logout request
        String logoutRequest = """
            {
                "terminateProviderSession": false
            }
            """;

        // ACT & ASSERT: Validate security headers
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"));
    }

    @Test
    void oauth2Logout_ShouldHandleAcceptHeaders() throws Exception {
        // ARRANGE: Valid logout request
        String logoutRequest = """
            {
                "terminateProviderSession": false
            }
            """;

        // ACT & ASSERT: Validate JSON response for application/json
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // ACT & ASSERT: Validate response for wildcard accept
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void oauth2Logout_UnsupportedAcceptType_ShouldReturn406() throws Exception {
        // ARRANGE: Valid request with unsupported Accept header
        String logoutRequest = """
            {
                "terminateProviderSession": false
            }
            """;

        // ACT & ASSERT: Validate 406 Not Acceptable for XML
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void oauth2Logout_ShouldHandleCorsRequests() throws Exception {
        // ARRANGE: CORS request with logout
        String logoutRequest = """
            {
                "terminateProviderSession": false
            }
            """;

        // ACT & ASSERT: Validate CORS headers for cross-origin requests
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .header("Origin", "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void oauth2Logout_InvalidRedirectUri_ShouldReturnBadRequest() throws Exception {
        // ARRANGE: Logout request with invalid redirect URI
        String logoutRequest = """
            {
                "terminateProviderSession": true,
                "redirectUri": "not-a-valid-url"
            }
            """;

        // ACT & ASSERT: Validate 400 Bad Request for invalid redirect URI
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_INVALID_REDIRECT_URI"))
                .andExpect(jsonPath("$.error.message", containsString("redirect URI")));
    }

    @Test
    void oauth2Logout_NullTerminateProviderSession_ShouldUseDefault() throws Exception {
        // ARRANGE: Logout request with null terminateProviderSession
        String logoutRequest = """
            {
                "terminateProviderSession": null
            }
            """;

        // ACT & ASSERT: Validate default behavior for null value
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void oauth2Logout_ShouldValidateHttpMethods() throws Exception {
        // ARRANGE: Test only POST method is supported

        // ACT & ASSERT: Validate 405 Method Not Allowed for GET
        mockMvc.perform(get("/api/v1/auth/oauth2/logout")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());

        // ACT & ASSERT: Validate 405 Method Not Allowed for PUT
        mockMvc.perform(put("/api/v1/auth/oauth2/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());

        // ACT & ASSERT: Validate 405 Method Not Allowed for DELETE
        mockMvc.perform(delete("/api/v1/auth/oauth2/logout")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void oauth2Logout_WithCSRFToken_ShouldValidateCSRF() throws Exception {
        // ARRANGE: Logout request with CSRF protection
        String logoutRequest = """
            {
                "terminateProviderSession": false
            }
            """;

        // ACT & ASSERT: Validate CSRF token handling (if enabled)
        mockMvc.perform(post("/api/v1/auth/oauth2/logout")
                .header("X-CSRF-TOKEN", "valid-csrf-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Should succeed with valid CSRF token
    }
}