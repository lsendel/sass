package com.platform.integration;

import com.platform.config.TestSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test to verify database connectivity with TestContainers.
 * This is a minimal test to ensure the infrastructure works.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestSecurityConfiguration.class)
class DatabaseConnectivityTest {

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

        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Disable components that might cause issues
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("management.endpoints.enabled-by-default", () -> "false");
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        // Verify Spring context loads successfully
        assertNotNull(dataSource);
    }

    @Test
    void databaseContainerIsRunning() {
        // Verify TestContainer is running
        assertTrue(POSTGRES.isRunning());
        assertTrue(POSTGRES.isCreated());
    }

    @Test
    void canConnectToDatabase() throws Exception {
        // Verify we can connect to the test database
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(1));
            var metadata = connection.getMetaData();
            assertNotNull(metadata.getDatabaseProductName());
        }
    }
}