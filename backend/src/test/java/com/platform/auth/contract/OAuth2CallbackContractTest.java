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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract test for GET /api/v1/auth/oauth2/callback/{provider} endpoint
 * Validates OpenAPI specification compliance and OAuth2 callback processing
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class OAuth2CallbackContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void oauth2Callback_ValidCode_ShouldReturnSuccessResponse() throws Exception {
        // ARRANGE: Valid OAuth2 callback parameters
        String code = "test_authorization_code_12345";
        String state = "secure_state_parameter_67890";

        // ACT & ASSERT: Validate successful callback response schema
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.session").exists())
                .andExpect(jsonPath("$.session.sessionId").exists())
                .andExpect(jsonPath("$.session.userId").exists())
                .andExpect(jsonPath("$.session.userInfo").exists())
                .andExpect(jsonPath("$.session.provider").exists())
                .andExpect(jsonPath("$.session.isAuthenticated").value(true))
                .andExpect(jsonPath("$.session.expiresAt").exists())
                .andExpect(jsonPath("$.session.createdAt").exists())
                .andExpect(jsonPath("$.redirectTo").exists());
    }

    @Test
    void oauth2Callback_GitHub_ShouldProcessCallbackCorrectly() throws Exception {
        // ARRANGE: Valid GitHub OAuth2 callback
        String code = "github_code_abcdef123456";
        String state = "github_state_xyz789";

        // ACT & ASSERT: Validate GitHub-specific callback processing
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/github")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.provider").value("github"))
                .andExpect(jsonPath("$.session.userInfo.provider").value("github"));
    }

    @Test
    void oauth2Callback_Microsoft_ShouldProcessCallbackCorrectly() throws Exception {
        // ARRANGE: Valid Microsoft OAuth2 callback
        String code = "microsoft_code_qwerty98765";
        String state = "microsoft_state_asdfgh";

        // ACT & ASSERT: Validate Microsoft-specific callback processing
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/microsoft")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.provider").value("microsoft"))
                .andExpect(jsonPath("$.session.userInfo.provider").value("microsoft"));
    }

    @Test
    void oauth2Callback_MissingCode_ShouldReturnBadRequest() throws Exception {
        // ARRANGE: Missing authorization code parameter
        String state = "valid_state_parameter";

        // ACT & ASSERT: Validate 400 Bad Request for missing code
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_MISSING_PARAMETER"))
                .andExpect(jsonPath("$.error.message", containsString("code")))
                .andExpect(jsonPath("$.error.timestamp").exists());
    }

    @Test
    void oauth2Callback_MissingState_ShouldReturnBadRequest() throws Exception {
        // ARRANGE: Missing state parameter
        String code = "valid_authorization_code";

        // ACT & ASSERT: Validate 400 Bad Request for missing state
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_MISSING_PARAMETER"))
                .andExpect(jsonPath("$.error.message", containsString("state")));
    }

    @Test
    void oauth2Callback_InvalidState_ShouldReturnUnauthorized() throws Exception {
        // ARRANGE: Invalid/expired state parameter
        String code = "valid_authorization_code";
        String invalidState = "invalid_or_expired_state";

        // ACT & ASSERT: Validate 401 Unauthorized for invalid state
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .param("state", invalidState)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_INVALID_STATE"))
                .andExpect(jsonPath("$.error.message", containsString("state parameter")));
    }

    @Test
    void oauth2Callback_ProviderError_ShouldReturnErrorResponse() throws Exception {
        // ARRANGE: OAuth2 provider returned error
        String error = "access_denied";
        String errorDescription = "The user denied the request";
        String state = "valid_state_parameter";

        // ACT & ASSERT: Validate error handling from OAuth2 provider
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("error", error)
                .param("error_description", errorDescription)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_AUTHORIZATION_DENIED"))
                .andExpect(jsonPath("$.error.message", containsString("access_denied")))
                .andExpect(jsonPath("$.error.details.provider_error").value(error))
                .andExpect(jsonPath("$.error.details.provider_description").value(errorDescription));
    }

    @Test
    void oauth2Callback_InvalidProvider_ShouldReturn404() throws Exception {
        // ARRANGE: Invalid OAuth2 provider
        String code = "valid_code";
        String state = "valid_state";

        // ACT & ASSERT: Validate 404 for unknown provider
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/invalid-provider")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_PROVIDER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message", containsString("invalid-provider")));
    }

    @Test
    void oauth2Callback_ShouldValidateUserInfoStructure() throws Exception {
        // ARRANGE: Valid OAuth2 callback
        String code = "test_code";
        String state = "test_state";

        // ACT & ASSERT: Validate userInfo object structure
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.userInfo.sub").exists())
                .andExpect(jsonPath("$.session.userInfo.email").exists())
                .andExpect(jsonPath("$.session.userInfo.name").exists())
                .andExpect(jsonPath("$.session.userInfo.provider").exists())
                .andExpect(jsonPath("$.session.userInfo.sub").isString())
                .andExpect(jsonPath("$.session.userInfo.email", matchesPattern("^[^@]+@[^@]+\\.[^@]+$"))) // Email format
                .andExpect(jsonPath("$.session.userInfo.name", hasLength(greaterThan(0)))); // Non-empty name
    }

    @Test
    void oauth2Callback_ShouldValidateSessionStructure() throws Exception {
        // ARRANGE: Valid OAuth2 callback
        String code = "test_code";
        String state = "test_state";

        // ACT & ASSERT: Validate session object structure
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.sessionId", hasLength(greaterThan(10)))) // Meaningful session ID
                .andExpect(jsonPath("$.session.userId", hasLength(greaterThan(5)))) // Meaningful user ID
                .andExpect(jsonPath("$.session.isAuthenticated").value(true))
                .andExpect(jsonPath("$.session.expiresAt", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"))) // ISO 8601
                .andExpect(jsonPath("$.session.createdAt", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"))); // ISO 8601
    }

    @Test
    void oauth2Callback_ShouldSetSecurityHeaders() throws Exception {
        // ARRANGE: Valid OAuth2 callback
        String code = "test_code";
        String state = "test_state";

        // ACT & ASSERT: Validate security headers
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Set-Cookie")) // Session cookie should be set
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")));
    }

    @Test
    void oauth2Callback_ShouldHandleAcceptHeaders() throws Exception {
        // ARRANGE: Valid callback parameters
        String code = "test_code";
        String state = "test_state";

        // ACT & ASSERT: Validate JSON response for application/json
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // ACT & ASSERT: Validate response for wildcard accept
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void oauth2Callback_UnsupportedMediaType_ShouldReturn406() throws Exception {
        // ARRANGE: Valid parameters but unsupported Accept header
        String code = "test_code";
        String state = "test_state";

        // ACT & ASSERT: Validate 406 Not Acceptable for XML
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .param("state", state)
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void oauth2Callback_ShouldHandleCorsRequests() throws Exception {
        // ARRANGE: CORS request with valid callback
        String code = "test_code";
        String state = "test_state";

        // ACT & ASSERT: Validate CORS headers for cross-origin requests
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", code)
                .param("state", state)
                .header("Origin", "http://localhost:3000")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void oauth2Callback_InvalidCode_ShouldReturnUnauthorized() throws Exception {
        // ARRANGE: Invalid authorization code
        String invalidCode = "invalid_or_expired_code";
        String state = "valid_state";

        // ACT & ASSERT: Validate 401 Unauthorized for invalid code
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", invalidCode)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_INVALID_CODE"))
                .andExpect(jsonPath("$.error.message", containsString("authorization code")));
    }

    @Test
    void oauth2Callback_ExpiredCode_ShouldReturnUnauthorized() throws Exception {
        // ARRANGE: Expired authorization code
        String expiredCode = "expired_code_from_10_minutes_ago";
        String state = "valid_state";

        // ACT & ASSERT: Validate 401 Unauthorized for expired code
        mockMvc.perform(get("/api/v1/auth/oauth2/callback/google")
                .param("code", expiredCode)
                .param("state", state)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_EXPIRED_CODE"))
                .andExpect(jsonPath("$.error.message", containsString("expired")));
    }
}