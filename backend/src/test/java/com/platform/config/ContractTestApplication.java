package com.platform.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application class for contract tests.
 * Excludes JPA and DataSource autoconfiguration since we use mocked repositories.
 * Also excludes repository interfaces from component scanning.
 *
 * <p>This configuration is only active for contract-test profile to prevent
 * interference with integration tests that need real JPA repositories.
 */
@SpringBootApplication(
    scanBasePackages = "com.platform",
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
    }
)
@ComponentScan(
    basePackages = {
        "com.platform.audit.api",  // Only scan controllers
        "com.platform.shared",      // Shared utilities
        "com.platform.config"       // Test configurations
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            com.platform.AuditApplication.class,
            com.platform.config.TestRedisConfiguration.class,
            com.platform.audit.config.TestDatabaseConfig.class,
            com.platform.audit.config.TestServiceConfig.class,
            com.platform.audit.config.IntegrationTestConfig.class
        }
    )
)
@EnableJpaRepositories(basePackages = {})  // Explicitly disable JPA repositories
@EnableCaching
@Profile("contract-test")
public class ContractTestApplication {
    // Test application - no main method needed
}
