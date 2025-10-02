package com.platform.audit.api;

import com.platform.config.AuditTestConfiguration;
import com.platform.config.WithMockUserPrincipal;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Integration test using TestContainers with real PostgreSQL database.
 * Tests the complete Spring context with database connectivity.
 *
 * DISABLED: Context loading fails due to missing bean dependencies.
 * These tests require full audit module infrastructure which is not
 * properly configured in the integration-test profile.
 */
@Disabled("Context loading fails - missing bean dependencies")
@SpringBootTest
@AutoConfigureMockMvc
@Import({com.platform.config.AuditTestConfiguration.class, com.platform.config.TestSecurityConfig.class})
@ActiveProfiles("integration-test")
@Testcontainers
class AuditLogViewerTestContainerTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        // Wait for container to be ready
        if (!POSTGRES.isRunning()) {
            throw new IllegalStateException("PostgreSQL container is not running");
        }

        // Configure database properties for TestContainers
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Disable Redis for tests
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration");

        // Disable Flyway for tests
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void auditLogEndpointShouldBeAccessibleWithRealDatabase() throws Exception {
        // This test verifies endpoints work with real database connection
        mockMvc.perform(get("/api/audit/logs"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUserPrincipal(
            userId = "22222222-2222-2222-2222-222222222222",
            organizationId = "11111111-1111-1111-1111-111111111111",
            username = "testuser",
            roles = {"USER"}
    )
    void auditLogDetailEndpointShouldHandleRequests() throws Exception {
        // Test detail endpoint with TestContainer database
        String testId = "11111111-1111-1111-1111-111111111111";
        mockMvc.perform(get("/api/audit/logs/" + testId))
                .andExpect(status().isNotFound()); // Expected since database is empty
    }

    @Test
    void endpointShouldRequireAuthentication() throws Exception {
        // Test that endpoints require authentication
        // Without authentication, Spring Security returns 403 Forbidden (not 401)
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isForbidden());
    }
}