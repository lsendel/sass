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
 * Contract test for GET /api/v1/auth/oauth2/authorize/{provider} endpoint
 * Validates OpenAPI specification compliance and OAuth2 authorization flow initiation
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class OAuth2AuthorizeContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authorizeOAuth2Provider_Google_ShouldReturnValidAuthorizationResponse() throws Exception {
        // ARRANGE: Valid Google OAuth2 provider

        // ACT & ASSERT: Validate OAuth2 authorization response schema
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.authorizationUrl").isString())
                .andExpect(jsonPath("$.authorizationUrl", startsWith("https://accounts.google.com")))
                .andExpect(jsonPath("$.state").exists())
                .andExpect(jsonPath("$.state").isString())
                .andExpect(jsonPath("$.codeChallenge").exists())
                .andExpect(jsonPath("$.codeChallenge").isString())
                .andExpect(jsonPath("$.codeChallengeMethod").value("S256"));
    }

    @Test
    void authorizeOAuth2Provider_GitHub_ShouldReturnValidAuthorizationResponse() throws Exception {
        // ARRANGE: Valid GitHub OAuth2 provider

        // ACT & ASSERT: Validate GitHub OAuth2 authorization response
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/github")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.authorizationUrl", startsWith("https://github.com/login/oauth/authorize")))
                .andExpect(jsonPath("$.state").exists())
                .andExpect(jsonPath("$.codeChallenge").exists())
                .andExpect(jsonPath("$.codeChallengeMethod").value("S256"));
    }

    @Test
    void authorizeOAuth2Provider_Microsoft_ShouldReturnValidAuthorizationResponse() throws Exception {
        // ARRANGE: Valid Microsoft OAuth2 provider

        // ACT & ASSERT: Validate Microsoft OAuth2 authorization response
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/microsoft")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.authorizationUrl", startsWith("https://login.microsoftonline.com")))
                .andExpect(jsonPath("$.state").exists())
                .andExpect(jsonPath("$.codeChallenge").exists())
                .andExpect(jsonPath("$.codeChallengeMethod").value("S256"));
    }

    @Test
    void authorizeOAuth2Provider_WithCustomRedirectUri_ShouldIncludeInAuthorizationUrl() throws Exception {
        // ARRANGE: Custom redirect URI parameter

        // ACT & ASSERT: Validate custom redirect URI is preserved
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .param("redirectUri", "http://localhost:3000/auth/callback")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl", containsString("redirect_uri=")))
                .andExpect(jsonPath("$.authorizationUrl", containsString("http%3A%2F%2Flocalhost%3A3000%2Fauth%2Fcallback")));
    }

    @Test
    void authorizeOAuth2Provider_ShouldValidatePKCEParameters() throws Exception {
        // ARRANGE: OAuth2 authorization request

        // ACT & ASSERT: Validate PKCE parameters format
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codeChallenge", hasLength(43))) // Base64URL encoded SHA256 hash = 43 chars
                .andExpect(jsonPath("$.codeChallenge", matchesPattern("^[A-Za-z0-9_-]+$"))) // Base64URL charset
                .andExpect(jsonPath("$.codeChallengeMethod").value("S256"))
                .andExpect(jsonPath("$.state", hasLength(greaterThan(15)))) // Minimum 16 chars for security
                .andExpect(jsonPath("$.state", matchesPattern("^[A-Za-z0-9_-]+$"))); // Safe URL chars
    }

    @Test
    void authorizeOAuth2Provider_ShouldIncludeRequiredOAuth2Parameters() throws Exception {
        // ARRANGE: OAuth2 authorization request

        // ACT & ASSERT: Validate required OAuth2 parameters in authorization URL
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl", containsString("response_type=code")))
                .andExpect(jsonPath("$.authorizationUrl", containsString("client_id=")))
                .andExpect(jsonPath("$.authorizationUrl", containsString("scope=")))
                .andExpect(jsonPath("$.authorizationUrl", containsString("state=")))
                .andExpect(jsonPath("$.authorizationUrl", containsString("code_challenge=")))
                .andExpect(jsonPath("$.authorizationUrl", containsString("code_challenge_method=S256")));
    }

    @Test
    void authorizeOAuth2Provider_InvalidProvider_ShouldReturn404() throws Exception {
        // ARRANGE: Invalid OAuth2 provider

        // ACT & ASSERT: Validate 404 for unknown provider
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/invalid-provider")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value("OAUTH2_PROVIDER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message", containsString("invalid-provider")))
                .andExpect(jsonPath("$.error.timestamp").exists());
    }

    @Test
    void authorizeOAuth2Provider_EmptyProvider_ShouldReturn404() throws Exception {
        // ARRANGE: Empty provider path parameter

        // ACT & ASSERT: Validate 404 for empty provider
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void authorizeOAuth2Provider_ShouldHandleAcceptHeaders() throws Exception {
        // ARRANGE: Different Accept headers

        // ACT & ASSERT: Validate JSON response for application/json
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // ACT & ASSERT: Validate response for wildcard accept
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void authorizeOAuth2Provider_UnsupportedMediaType_ShouldReturn406() throws Exception {
        // ARRANGE: Unsupported Accept header

        // ACT & ASSERT: Validate 406 Not Acceptable for XML
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void authorizeOAuth2Provider_ShouldValidateSecurityHeaders() throws Exception {
        // ARRANGE: OAuth2 authorization request

        // ACT & ASSERT: Validate security headers
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"));
    }

    @Test
    void authorizeOAuth2Provider_ShouldReturnUniqueStatePerRequest() throws Exception {
        // ARRANGE: Multiple OAuth2 authorization requests

        // ACT: Make first request and extract state
        String response1 = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // ACT: Make second request and extract state
        String response2 = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // ASSERT: States should be different for security
        var json1 = objectMapper.readTree(response1);
        var json2 = objectMapper.readTree(response2);

        String state1 = json1.get("state").asText();
        String state2 = json2.get("state").asText();

        assert !state1.equals(state2) : "OAuth2 state parameters must be unique per request";
    }

    @Test
    void authorizeOAuth2Provider_ShouldReturnUniquePKCEChallengePerRequest() throws Exception {
        // ARRANGE: Multiple OAuth2 authorization requests

        // ACT: Make first request and extract code challenge
        String response1 = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // ACT: Make second request and extract code challenge
        String response2 = mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // ASSERT: Code challenges should be different for security
        var json1 = objectMapper.readTree(response1);
        var json2 = objectMapper.readTree(response2);

        String challenge1 = json1.get("codeChallenge").asText();
        String challenge2 = json2.get("codeChallenge").asText();

        assert !challenge1.equals(challenge2) : "PKCE code challenges must be unique per request";
    }

    @Test
    void authorizeOAuth2Provider_ShouldHandleCorsRequests() throws Exception {
        // ARRANGE: CORS preflight request

        // ACT & ASSERT: Validate CORS headers for cross-origin requests
        mockMvc.perform(get("/api/v1/auth/oauth2/authorize/google")
                .header("Origin", "http://localhost:3000")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}