package com.platform.audit.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Main test configuration that combines all test-specific configurations.
 * This ensures consistent test setup across all test classes.
 */
@TestConfiguration
@Profile("test")
@EnableAutoConfiguration(exclude = {
    RedisAutoConfiguration.class,
    SessionAutoConfiguration.class,
    FlywayAutoConfiguration.class
})
@ComponentScan(
        basePackages = "com.platform.audit",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Redis.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Flyway.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Session.*")
        }
)
@Import({TestDatabaseConfig.class, TestServiceConfig.class})
public final class IntegrationTestConfig {
}