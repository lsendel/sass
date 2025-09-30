package com.platform.audit.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * Test configuration to ensure proper H2 database setup for tests.
 * Fixes issues with PostgreSQL/H2 conflicts in test environment.
 */
@TestConfiguration
@Profile("test")
public final class TestDatabaseConfig {

    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb")
            .addScript("classpath:schema-test.sql")
            .build();
    }
}