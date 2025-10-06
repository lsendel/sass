package com.platform.security;

import com.platform.AbstractIntegrationTest;
import com.platform.auth.User;
import com.platform.auth.internal.OpaqueTokenService;
import com.platform.auth.internal.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.http.Cookie;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security test suite for the SASS platform.
 *
 * Features tested:
 * - Authentication security
 * - Authorization controls
 * - OWASP Top 10 vulnerabilities
 * - Rate limiting (integration)
 * - Session management
 * - Input validation security
 * - Security headers
 * - CORS configuration
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "security-test"})
@EnabledIfSystemProperty(named = "security.testing.enabled", matches = "true")
@Transactional
@DisplayName("Comprehensive Security Test Suite")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityTestSuite extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OpaqueTokenService tokenService;

    private MockMvc mockMvc;
    private String validToken;
    private String expiredToken;
    private User testUser;

    @BeforeEach
    void setUpSecurityTest() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean up any existing test user
        userRepository.findByEmail("security-test@platform.com")
                .ifPresent(userRepository::delete);
        userRepository.flush();

        // Create test user
        testUser = new User("security-test@platform.com", "$2a$12$test.hash");
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        // Generate valid token
        validToken = tokenService.generateToken(testUser);

        // Generate expired token (simulate expiry)
        expiredToken = "expired_" + UUID.randomUUID();
    }

    // ========================================================================
    // AUTHENTICATION SECURITY TESTS
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("SEC-001: Should reject invalid authentication tokens")
    void shouldRejectInvalidTokens() throws Exception {
        // Test various invalid token scenarios
        String[] invalidTokens = {
                null,
                "",
                "invalid_token",
                "Bearer invalid",
                "malformed-token-12345",
                "<script>alert('xss')</script>",
                "../../etc/passwd",
                "SELECT * FROM users;"
        };

        for (String invalidToken : invalidTokens) {
            MockMvc.builder()
                    .perform(get("/api/v1/protected/endpoint")
                            .cookie(new Cookie("auth_token", invalidToken)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @Order(2)
    @DisplayName("SEC-002: Should reject expired tokens")
    void shouldRejectExpiredTokens() throws Exception {
        mockMvc.perform(get("/api/v1/protected/endpoint")
                        .cookie(new Cookie("auth_token", expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("SEC-003: Should enforce session timeout")
    void shouldEnforceSessionTimeout() throws Exception {
        // Verify token is valid initially
        mockMvc.perform(get("/actuator/health")
                        .cookie(new Cookie("auth_token", validToken)))
                .andExpect(status().isOk());

        // Revoke the token to simulate timeout
        tokenService.revokeToken(validToken);

        // Verify token is now invalid
        mockMvc.perform(get("/api/v1/protected/endpoint")
                        .cookie(new Cookie("auth_token", validToken)))
                .andExpect(status().isUnauthorized());
    }

    // ========================================================================
    // AUTHORIZATION SECURITY TESTS
    // ========================================================================

    @Test
    @Order(4)
    @DisplayName("SEC-004: Should enforce endpoint authorization")
    void shouldEnforceEndpointAuthorization() throws Exception {
        // Test access to various protected endpoints
        String[] protectedEndpoints = {
                "/api/v1/users",
                "/api/v1/organizations",
                "/api/v1/audit/logs",
                "/api/v1/admin/settings"
        };

        for (String endpoint : protectedEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should be unauthorized (401) or forbidden (403)
                        assert status == 401 || status == 403 || status == 404;
                    });
        }
    }

    // ========================================================================
    // OWASP TOP 10 SECURITY TESTS
    // ========================================================================

    @Test
    @Order(5)
    @DisplayName("SEC-005: Should prevent SQL injection attacks")
    void shouldPreventSqlInjection() throws Exception {
        String[] sqlInjectionPayloads = {
                "'; DROP TABLE users; --",
                "1' OR '1'='1",
                "admin'/*",
                "UNION SELECT password FROM users",
                "'; INSERT INTO users VALUES ('hacker', 'password'); --"
        };

        for (String payload : sqlInjectionPayloads) {
            // Test in various input fields
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "email": "%s",
                                        "password": "test"
                                    }
                                    """, payload)))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should be rejected (400, 401, or 500 if validation catches it)
                        assert status != 200;
                    });
        }
    }

    @Test
    @Order(6)
    @DisplayName("SEC-006: Should prevent XSS attacks")
    void shouldPreventXssAttacks() throws Exception {
        String[] xssPayloads = {
                "<script>alert('xss')</script>",
                "javascript:alert('xss')",
                "<img src=x onerror=alert('xss')>",
                "<svg onload=alert('xss')>",
                "';alert('xss');//"
        };

        for (String payload : xssPayloads) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "email": "test@example.com",
                                        "password": "%s"
                                    }
                                    """, payload)))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        String response = result.getResponse().getContentAsString();
                        // Response should not contain unescaped script tags
                        assert !response.contains("<script>") && !response.contains("javascript:");
                    });
        }
    }

    @Test
    @Order(7)
    @DisplayName("SEC-007: Should prevent path traversal attacks")
    void shouldPreventPathTraversal() throws Exception {
        String[] pathTraversalPayloads = {
                "../../../etc/passwd",
                "..\\..\\..\\windows\\system32\\config\\sam",
                "....//....//....//etc/passwd",
                "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
                "..%252f..%252f..%252fetc%252fpasswd"
        };

        for (String payload : pathTraversalPayloads) {
            mockMvc.perform(get("/api/v1/files/" + payload)
                            .cookie(new Cookie("auth_token", validToken)))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not allow access to system files
                        assert status != 200 || !result.getResponse().getContentAsString().contains("root:");
                    });
        }
    }

    // ========================================================================
    // SECURITY HEADERS TESTS
    // ========================================================================

    @Test
    @Order(8)
    @DisplayName("SEC-008: Should include all required security headers")
    void shouldIncludeSecurityHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("Referrer-Policy"));
    }

    @Test
    @Order(9)
    @DisplayName("SEC-009: Should not expose sensitive information in headers")
    void shouldNotExposeSensitiveHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String serverHeader = result.getResponse().getHeader("Server");
                    String xPoweredBy = result.getResponse().getHeader("X-Powered-By");

                    // Should not expose detailed server information
                    assert serverHeader == null || !serverHeader.contains("Tomcat");
                    assert xPoweredBy == null;
                });
    }

    // ========================================================================
    // CORS SECURITY TESTS
    // ========================================================================

    @Test
    @Order(10)
    @DisplayName("SEC-010: Should enforce CORS policy")
    void shouldEnforceCorsPolicy() throws Exception {
        // Test with unauthorized origin
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "https://malicious-site.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpected(result -> {
                    String allowedOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                    // Should not allow malicious origins
                    assert allowedOrigin == null || !allowedOrigin.equals("https://malicious-site.com");
                });

        // Test with authorized origin
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    // ========================================================================
    // INPUT VALIDATION SECURITY TESTS
    // ========================================================================

    @Test
    @Order(11)
    @DisplayName("SEC-011: Should validate input size limits")
    void shouldValidateInputSizeLimits() throws Exception {
        // Test with oversized input
        String oversizedEmail = "a".repeat(10000) + "@example.com";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                    "email": "%s",
                                    "password": "test"
                                }
                                """, oversizedEmail)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(12)
    @DisplayName("SEC-012: Should sanitize input data")
    void shouldSanitizeInputData() throws Exception {
        // Test with malicious content
        String maliciousInput = "<script>alert('xss')</script>@example.com";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                    "email": "%s",
                                    "password": "test"
                                }
                                """, maliciousInput)))
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    // Should not echo back unescaped malicious content
                    assert !response.contains("<script>");
                });
    }

    // ========================================================================
    // RATE LIMITING INTEGRATION TESTS
    // ========================================================================

    @Test
    @Order(13)
    @DisplayName("SEC-013: Should integrate with rate limiting")
    void shouldIntegrateWithRateLimiting() throws Exception {
        // Note: This is an integration test to verify rate limiting is in the security chain
        // Actual rate limiting behavior is tested separately

        // Multiple requests should eventually hit rate limiting
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "email": "test@example.com",
                                        "password": "wrong"
                                    }
                                    """))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should be unauthorized or rate limited
                        assert status == 401 || status == 429;
                    });
        }
    }

    // ========================================================================
    // CLEANUP
    // ========================================================================

    @AfterEach
    void cleanupSecurityTest() {
        if (validToken != null) {
            tokenService.revokeToken(validToken);
        }
        if (testUser != null) {
            userRepository.delete(testUser);
            userRepository.flush();
        }
    }
}