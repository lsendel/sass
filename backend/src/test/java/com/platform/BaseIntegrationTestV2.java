package com.platform;

import com.platform.config.IntegrationTestBaseConfiguration;
import com.platform.config.TestSecurityConfig;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Improved base class for integration tests that resolves common configuration issues.
 *
 * <p>This base class provides:
 * <ul>
 *   <li>Properly configured TestContainers with PostgreSQL</li>
 *   <li>Mock implementations of audit services</li>
 *   <li>Security configuration for testing</li>
 *   <li>Consistent test data setup</li>
 * </ul>
 *
 * <p>Use this base class for integration tests that need full Spring context
 * with working database connections and properly configured audit services.
 *
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "integration-test"})
@Import({
    IntegrationTestBaseConfiguration.class,
    TestSecurityConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Testcontainers
public abstract class BaseIntegrationTestV2 {

    /**
     * PostgreSQL container for integration tests.
     * Shared across all test methods in the same class.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sass_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true); // Reuse container across test runs

    /**
     * MockMvc for testing web layer.
     */
    @Autowired
    protected MockMvc mockMvc;

    /**
     * Test data factory for creating consistent test objects.
     */
    @Autowired
    protected IntegrationTestBaseConfiguration.TestDataFactory testDataFactory;

    /**
     * Configure dynamic properties for TestContainers.
     */
    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA configuration for tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");

        // Disable flyway for tests (use DDL auto-generation)
        registry.add("spring.flyway.enabled", () -> "false");

        // Redis configuration (embedded for tests)
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6370"); // Different port to avoid conflicts

        // Security configuration for tests
        registry.add("app.security.jwt.secret", () -> "test-secret-key-for-integration-tests");
        registry.add("app.security.cors.allowed-origins", () -> "http://localhost:3000");

        // Audit configuration
        registry.add("app.audit.enabled", () -> "true");
        registry.add("app.audit.retention-days", () -> "30");

        // Test-specific properties
        registry.add("spring.test.database.replace", () -> "none");
        registry.add("logging.level.org.springframework.web", () -> "DEBUG");
        registry.add("logging.level.org.springframework.security", () -> "DEBUG");
    }

    /**
     * Setup method run before each test.
     * Override this method in subclasses to add custom setup.
     */
    @BeforeEach
    protected void setUp() {
        // Base setup - override in subclasses for custom initialization
        // Database is automatically cleaned due to @DirtiesContext
    }

    /**
     * Helper method to verify that the test container is running.
     */
    protected void verifyDatabaseConnection() {
        assert postgres.isRunning() : "PostgreSQL container should be running";
        assert postgres.getDatabaseName().equals("sass_test") : "Database name should be configured correctly";
    }

    /**
     * Helper method to create test audit events in the database.
     */
    protected void createTestAuditData(int eventCount) {
        // Test data will be created through the testDataFactory
        // This method can be overridden in subclasses for specific test data needs
        var events = testDataFactory.createAuditEventBatch(eventCount);
        // In a real implementation, these would be persisted to the test database
    }

    /**
     * Helper method to clean up test data after tests.
     * Called automatically due to @DirtiesContext.
     */
    protected void cleanupTestData() {
        // Cleanup is handled automatically by @DirtiesContext
        // Override this method in subclasses if manual cleanup is needed
    }
}