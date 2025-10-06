package com.platform;

import com.platform.config.IntegrationTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Abstract base class for integration tests using real databases via TestContainers.
 *
 * <p>This class provides:
 * <ul>
 *   <li>Real PostgreSQL database via TestContainers</li>
 *   <li>Real Redis instance via TestContainers</li>
 *   <li>Full Spring Boot application context</li>
 *   <li>TestRestTemplate for HTTP integration tests</li>
 *   <li>JdbcTemplate for direct database access</li>
 *   <li>Transactional test support with rollback</li>
 * </ul>
 *
 * <p><b>Constitutional Compliance:</b>
 * <ul>
 *   <li>Zero mocks - all dependencies are real</li>
 *   <li>Real database connections via TestContainers</li>
 *   <li>Integration testing with actual Spring Security</li>
 *   <li>Test-first development (TDD) required</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 * {@code
 * @SpringBootTest
 * class MyServiceIntegrationTest extends AbstractIntegrationTest {
 *
 *     @Autowired
 *     private MyService myService;
 *
 *     @Test
 *     void shouldDoSomething() {
 *         // Test with real database and Redis
 *         var result = myService.doSomething();
 *         assertThat(result).isNotNull();
 *     }
 * }
 * }
 * </pre>
 *
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Import(IntegrationTestConfiguration.class)
public abstract class AbstractIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    /**
     * PostgreSQL container shared across ALL integration tests using singleton pattern.
     * Starts once and reused across all test classes to avoid connection issues.
     * Uses PostgreSQL 15 Alpine for smaller image size and faster startup.
     */
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    /**
     * Redis container shared across ALL integration tests using singleton pattern.
     * Starts once and reused across all test classes to avoid connection issues.
     * Uses Redis 7 Alpine for smaller image size and faster startup.
     */
    protected static final GenericContainer<?> REDIS_CONTAINER;

    static {
        // Initialize containers once for entire test suite (singleton pattern)
        POSTGRES_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        POSTGRES_CONTAINER.start();

        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        REDIS_CONTAINER.start();

        // Register shutdown hook to clean database at start of each test class
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                POSTGRES_CONTAINER.stop();
                REDIS_CONTAINER.stop();
            } catch (Exception e) {
                logger.warn("Error stopping containers during shutdown: {}", e.getMessage());
            }
        }));
    }

    /**
     * JdbcTemplate for direct database access in tests.
     * Useful for setup, verification, and cleanup operations.
     */
    @Autowired(required = false) // Optional - only needed for HTTP tests
    protected JdbcTemplate jdbcTemplate;

    /**
     * Configures Spring Boot properties to use TestContainers databases.
     * This method is called before the Spring context is created.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Redis configuration
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port",
                () -> REDIS_CONTAINER.getMappedPort(6379).toString());

        // JPA configuration for tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Flyway disabled for integration tests (using create-drop instead)
        registry.add("spring.flyway.enabled", () -> "false");

        // Logging configuration for tests
        registry.add("logging.level.com.platform", () -> "DEBUG");
        registry.add("logging.level.org.hibernate.SQL", () -> "DEBUG");
        registry.add("logging.level.org.springframework.security", () -> "DEBUG");
    }

    /**
     * One-time setup before all tests in a test class.
     * Cleans database to ensure fresh start for the test suite.
     */
    @org.junit.jupiter.api.BeforeAll
    static void setUpClass() {
        // Clean database once before all tests in this class
        cleanDatabaseStatic();
    }

    /**
     * Static method to clean database (callable from @BeforeAll).
     * Creates temporary JdbcTemplate to perform cleanup.
     */
    protected static void cleanDatabaseStatic() {
        try {
            // Create a temporary DataSource and JdbcTemplate for cleanup
            var dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES_CONTAINER.getJdbcUrl(),
                POSTGRES_CONTAINER.getUsername(),
                POSTGRES_CONTAINER.getPassword()
            );
            var jdbcTemplate = new JdbcTemplate(dataSource);

            // Clean all tables in dependency order
            jdbcTemplate.execute("TRUNCATE TABLE auth_login_attempts CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE opaque_tokens CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE auth_users CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE audit_logs CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE audit_exports CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        } catch (Exception e) {
            // Ignore errors if tables don't exist yet (first test run)
            logger.warn("Note: Database cleanup skipped (tables may not exist yet): {}", e.getMessage());
        }
    }

    /**
     * Setup method called before each test.
     * Override this method in subclasses to add custom setup logic.
     */
    @BeforeEach
    void setUp() {
        // Base setup - can be overridden by subclasses
        // Transactional tests will automatically rollback after each test
    }

    /**
     * Helper method to clean up all tables in the database.
     * Useful when you need a fresh database state between tests.
     *
     * <p><b>Note:</b> Called automatically for non-transactional tests.
     * For transactional tests, rollback handles cleanup.
     */
    protected void cleanDatabase() {
        if (jdbcTemplate == null) {
            return; // Skip if jdbcTemplate not available
        }
        try {
            // Clean auth module tables
            jdbcTemplate.execute("TRUNCATE TABLE auth_login_attempts CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE opaque_tokens CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE auth_users CASCADE");

            // Clean audit tables
            jdbcTemplate.execute("TRUNCATE TABLE audit_logs CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE audit_exports CASCADE");

            // Clean other tables as needed
            jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        } catch (Exception e) {
            // Ignore errors if tables don't exist yet
            logger.warn("Warning: Could not clean database: {}", e.getMessage());
        }
    }

    /**
     * Helper method to count rows in a table.
     * Useful for assertions in tests.
     *
     * @param tableName the name of the table
     * @return the number of rows in the table
     */
    protected int countRowsInTable(final String tableName) {
        final String sql = String.format("SELECT COUNT(*) FROM %s", tableName);
        final Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Helper method to verify a table exists in the database.
     *
     * @param tableName the name of the table
     * @return true if the table exists, false otherwise
     */
    protected boolean tableExists(final String tableName) {
        final String sql =
                "SELECT EXISTS (SELECT FROM information_schema.tables "
                + "WHERE table_schema = 'public' AND table_name = ?)";
        final Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableName);
        return exists != null && exists;
    }
}
