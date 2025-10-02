package com.platform;

import com.platform.config.TestSecurityConfiguration;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for integration tests.
 * Provides full Spring context with H2 in-memory database.
 * Redis, session, and cache auto-configuration are disabled for tests.
 *
 * All integration tests should extend this class to get consistent test configuration.
 */
@SpringBootTest(
    classes = AuditApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Import(TestSecurityConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // H2 Database configuration
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",

    // JPA/Hibernate configuration
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.format_sql=false",

    // Logging
    "logging.level.org.hibernate=WARN",
    "logging.level.org.springframework=WARN",
    "logging.level.com.platform=INFO"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {
    // Base class for integration tests
    // Extend this class to get full Spring context with test configuration
}
