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
 * Contract test for GET /api/v1/auth/oauth2/providers endpoint
 * Validates OpenAPI specification compliance and response schema
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class OAuth2ProvidersContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getOAuth2Providers_ShouldReturnValidProvidersSchema() throws Exception {
        // ARRANGE: No setup required for providers endpoint

        // ACT & ASSERT: Validate response structure against OpenAPI schema
        mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.providers").exists())
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(jsonPath("$.providers[*].name").exists())
                .andExpect(jsonPath("$.providers[*].displayName").exists())
                .andExpect(jsonPath("$.providers[*].authorizationUrl").exists())
                .andExpect(jsonPath("$.providers[*].scopes").exists())
                .andExpect(jsonPath("$.providers[*].scopes").isArray());
    }

    @Test
    void getOAuth2Providers_ShouldReturnExpectedProviders() throws Exception {
        // ARRANGE: Expected providers based on application.yml configuration

        // ACT & ASSERT: Validate all three providers are present
        mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(jsonPath("$.providers.length()").value(3))

                // Google provider validation
                .andExpect(jsonPath("$.providers[?(@.name == 'google')].displayName").value("Google"))
                .andExpect(jsonPath("$.providers[?(@.name == 'google')].authorizationUrl").exists())
                .andExpect(jsonPath("$.providers[?(@.name == 'google')].scopes[*]", hasItem("openid")))
                .andExpect(jsonPath("$.providers[?(@.name == 'google')].scopes[*]", hasItem("profile")))
                .andExpect(jsonPath("$.providers[?(@.name == 'google')].scopes[*]", hasItem("email")))

                // GitHub provider validation
                .andExpect(jsonPath("$.providers[?(@.name == 'github')].displayName").value("GitHub"))
                .andExpect(jsonPath("$.providers[?(@.name == 'github')].authorizationUrl").exists())
                .andExpect(jsonPath("$.providers[?(@.name == 'github')].scopes[*]", hasItem("read:user")))
                .andExpect(jsonPath("$.providers[?(@.name == 'github')].scopes[*]", hasItem("user:email")))

                // Microsoft provider validation
                .andExpect(jsonPath("$.providers[?(@.name == 'microsoft')].displayName").value("Microsoft"))
                .andExpect(jsonPath("$.providers[?(@.name == 'microsoft')].authorizationUrl").exists())
                .andExpect(jsonPath("$.providers[?(@.name == 'microsoft')].scopes[*]", hasItem("openid")))
                .andExpect(jsonPath("$.providers[?(@.name == 'microsoft')].scopes[*]", hasItem("profile")))
                .andExpect(jsonPath("$.providers[?(@.name == 'microsoft')].scopes[*]", hasItem("email")));
    }

    @Test
    void getOAuth2Providers_ShouldValidateProviderNameFormat() throws Exception {
        // ARRANGE: No setup required

        // ACT & ASSERT: Validate provider names follow expected format
        mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[*].name", containsInAnyOrder("google", "github", "microsoft")));
    }

    @Test
    void getOAuth2Providers_ShouldValidateAuthorizationUrlFormat() throws Exception {
        // ARRANGE: No setup required

        // ACT & ASSERT: Validate authorization URLs are valid HTTPS URLs
        mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[*].authorizationUrl", everyItem(startsWith("https://"))));
    }

    @Test
    void getOAuth2Providers_ShouldHandleAcceptHeader() throws Exception {
        // ARRANGE: Test different Accept headers

        // ACT & ASSERT: Validate JSON response for application/json
        mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // ACT & ASSERT: Validate response for wildcard accept
        mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .accept(MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void getOAuth2Providers_ShouldRejectUnsupportedContentType() throws Exception {
        // ARRANGE: Request with unsupported Accept header

        // ACT & ASSERT: Validate 406 Not Acceptable for XML
        mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void getOAuth2Providers_ShouldValidateResponseHeaders() throws Exception {
        // ARRANGE: No setup required

        // ACT & ASSERT: Validate response headers
        mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void getOAuth2Providers_ShouldHandleCors() throws Exception {
        // ARRANGE: CORS preflight request

        // ACT & ASSERT: Validate CORS headers for cross-origin requests
        mockMvc.perform(get("/api/v1/auth/oauth2/providers")
                .header("Origin", "http://localhost:3000")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}