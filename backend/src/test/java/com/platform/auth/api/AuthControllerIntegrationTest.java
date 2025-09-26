package com.platform.auth.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.auth.internal.OAuth2ProvidersService;
import com.platform.auth.internal.OAuth2ProvidersService.OAuth2ProviderInfo;
import com.platform.auth.internal.SessionService;
import com.platform.auth.internal.SessionService.SessionInfo;
import com.platform.shared.security.PasswordProperties;
import com.platform.shared.security.PlatformUserPrincipal;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("controller-test")
@ImportAutoConfiguration(
    exclude = {
      OAuth2ClientAutoConfiguration.class,
      OAuth2ClientWebSecurityAutoConfiguration.class
    })
@TestPropertySource(
    properties = {
      "app.frontend-url=http://localhost:3000",
      "platform.security.password.enabled=true",
      "platform.security.password.min-length=12"
    })
@Import(AuthControllerIntegrationTestConfig.class)
class AuthControllerIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(AuthControllerIntegrationTest.class);

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordProperties passwordProperties;

  @MockBean private OAuth2ProvidersService providersService;
  @MockBean private SessionService sessionService;
  @MockBean private com.platform.shared.security.InputSanitizer inputSanitizer;
  @MockBean private com.platform.shared.security.RateLimitingFilter rateLimitingFilter;

  private OAuth2ProviderInfo githubProvider;

  @BeforeEach
  void setUp() {
    log.info("Setting up AuthControllerIntegrationTest");
    githubProvider =
        new OAuth2ProviderInfo(
            "github",
            "GitHub",
            "https://github.com/login/oauth/authorize",
            "/images/providers/github.svg",
            true);

    when(providersService.getAvailableProviders()).thenReturn(List.of(githubProvider));
    when(providersService.isProviderEnabled("github")).thenReturn(true);
    when(providersService.isProviderEnabled("invalid-provider")).thenReturn(false);
    passwordProperties.setEnabled(true);
  }

  @Test
  void testListProviders_ShouldReturnAvailableProviders() throws Exception {
    log.info("Testing GET /api/v1/auth/providers");

    MvcResult result =
        mockMvc
            .perform(get("/api/v1/auth/providers").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.providers").exists())
            .andReturn();

    String response = result.getResponse().getContentAsString();
    @SuppressWarnings("unchecked")
    Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);

    assertTrue(responseMap.containsKey("providers"));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> providers = (List<Map<String, Object>>) responseMap.get("providers");
    assertNotNull(providers);
    assertTrue(providers.stream().anyMatch(p -> "github".equals(p.get("name"))));

    log.info("Providers endpoint returned {} providers", providers.size());
  }

  @Test
  void testGetAuthMethods_ShouldReturnAvailableMethods() throws Exception {
    log.info("Testing GET /api/v1/auth/methods");

    MvcResult result =
        mockMvc
            .perform(get("/api/v1/auth/methods").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.methods").exists())
            .andExpect(jsonPath("$.passwordAuthEnabled").exists())
            .andExpect(jsonPath("$.oauth2Providers").exists())
            .andReturn();

    String response = result.getResponse().getContentAsString();
    @SuppressWarnings("unchecked")
    Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);

    assertTrue(responseMap.containsKey("methods"));
    assertTrue(responseMap.containsKey("passwordAuthEnabled"));
    assertTrue(responseMap.containsKey("oauth2Providers"));

    @SuppressWarnings("unchecked")
    List<String> methods = (List<String>) responseMap.get("methods");
    assertTrue(methods.contains("OAUTH2"));

    log.info("Auth methods: {}, Password enabled: {}", methods, responseMap.get("passwordAuthEnabled"));
  }

  @Test
  void testInitiateAuthorization_WithValidProvider_ShouldRedirect() throws Exception {
    log.info("Testing GET /api/v1/auth/authorize with valid provider");

    mockMvc
        .perform(
            get("/api/v1/auth/authorize")
                .param("provider", "github")
                .param("redirect_uri", "http://localhost:3000/auth/callback"))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    log.info("Authorization redirect successful");
  }

  @Test
  void testInitiateAuthorization_WithInvalidProvider_ShouldReturnBadRequest() throws Exception {
    log.info("Testing GET /api/v1/auth/authorize with invalid provider");

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/auth/authorize")
                    .param("provider", "invalid-provider")
                    .param("redirect_uri", "http://localhost:3000/auth/callback"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();

    String response = result.getResponse().getContentAsString();
    @SuppressWarnings("unchecked")
    Map<String, Object> errorResponse = objectMapper.readValue(response, Map.class);

    assertTrue(errorResponse.containsKey("error"));
    assertTrue(errorResponse.containsKey("message"));

    log.info("Invalid provider correctly rejected");
  }

  @Test
  void testInitiateAuthorization_WithInvalidRedirectUri_ShouldReturnBadRequest() throws Exception {
    log.info("Testing GET /api/v1/auth/authorize with invalid redirect URI");

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/auth/authorize")
                    .param("provider", "github")
                    .param("redirect_uri", "https://malicious-site.com/callback"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();

    String response = result.getResponse().getContentAsString();
    @SuppressWarnings("unchecked")
    Map<String, Object> errorResponse = objectMapper.readValue(response, Map.class);

    assertTrue(errorResponse.containsKey("error"));
    log.info("Invalid redirect URI correctly rejected");
  }

  @Test
  void testGetCurrentSession_WithAuthenticatedUser_ShouldReturnSessionInfo() throws Exception {
    log.info("Testing GET /api/v1/auth/session with authenticated user");

    UUID userId = UUID.randomUUID();
    PlatformUserPrincipal principal =
        PlatformUserPrincipal.organizationMember(
            userId, "test@example.com", "Test User", UUID.randomUUID(), "example-org", "ADMIN");

    when(sessionService.getSessionInfo(userId))
        .thenReturn(new SessionInfo(2L, Instant.parse("2025-01-01T00:00:00Z")));

    var authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    org.springframework.security.core.context.SecurityContextHolder.getContext()
        .setAuthentication(authentication);
    try {
      mockMvc
          .perform(get("/api/v1/auth/session").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.user").exists())
          .andExpect(jsonPath("$.session").exists())
          .andReturn();
    } finally {
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    log.info("Session info retrieved successfully");
  }

  @Test
  void testGetCurrentSession_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
    log.info("Testing GET /api/v1/auth/session without authentication");

    mockMvc
        .perform(get("/api/v1/auth/session").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andReturn();

    log.info("Unauthorized access correctly rejected");
  }

  @Test
  void testLogout_WithAuthenticatedUser_ShouldSucceed() throws Exception {
    log.info("Testing POST /api/v1/auth/logout with authenticated user");

    UUID userId = UUID.randomUUID();
    PlatformUserPrincipal principal =
        PlatformUserPrincipal.organizationMember(
            userId, "test@example.com", "Test User", UUID.randomUUID(), "example-org", "MEMBER");

    doNothing().when(sessionService).revokeToken("token-123");

    var authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    org.springframework.security.core.context.SecurityContextHolder.getContext()
        .setAuthentication(authentication);
    try {
      mockMvc
          .perform(
              post("/api/v1/auth/logout")
                  .header("Authorization", "Bearer token-123")
                  .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isNoContent())
          .andReturn();
    } finally {
      org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    verify(sessionService).revokeToken("token-123");
    log.info("Logout completed successfully");
  }

  @Test
  void testLogout_WithoutAuthentication_ShouldStillSucceed() throws Exception {
    log.info("Testing POST /api/v1/auth/logout without authentication");

    mockMvc
        .perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent())
        .andReturn();

    log.info("Logout without authentication completed successfully");
  }

  @Test
  void testHelperMethods_ClientIpExtraction() throws Exception {
    log.info("Testing client IP extraction with X-Forwarded-For header");

    mockMvc
        .perform(
            get("/api/v1/auth/methods")
                .header("X-Forwarded-For", "192.168.1.1, 10.0.0.1")
                .header("X-Real-IP", "172.16.0.1")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    log.info("Request with IP headers processed successfully");
  }

  @Test
  void testHelperMethods_RedirectUriValidation() throws Exception {
    log.info("Testing redirect URI validation edge cases");

    mockMvc
        .perform(
            get("/api/v1/auth/authorize")
                .param("provider", "github")
                .param("redirect_uri", "https://localhost:3000/callback"))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    mockMvc
        .perform(
            get("/api/v1/auth/authorize")
                .param("provider", "github")
                .param("redirect_uri", ""))
        .andExpect(status().isBadRequest())
        .andReturn();

    mockMvc
        .perform(get("/api/v1/auth/authorize").param("provider", "github"))
        .andExpect(status().isBadRequest())
        .andReturn();

    log.info("Redirect URI validation tests completed");
  }

  @Test
  void testAuthorizationUrlBuilding() throws Exception {
    log.info("Testing authorization URL building logic");

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/auth/authorize")
                    .param("provider", "github")
                    .param("redirect_uri", "http://localhost:3000/auth/callback?param=value"))
            .andExpect(status().is3xxRedirection())
            .andReturn();

    String redirectUrl = result.getResponse().getHeader("Location");
    assertNotNull(redirectUrl);
    assertTrue(redirectUrl.contains("/oauth2/authorization/github"));
    assertTrue(redirectUrl.contains("redirect_uri="));
    assertTrue(redirectUrl.contains("state="));

    log.info("Authorization URL built correctly: {}", redirectUrl);
  }
}

@TestConfiguration
class AuthControllerIntegrationTestConfig {

  @Bean
  PasswordProperties passwordProperties() {
    PasswordProperties props = new PasswordProperties();
    props.setEnabled(true);
    props.setMinLength(12);
    return props;
  }
}
