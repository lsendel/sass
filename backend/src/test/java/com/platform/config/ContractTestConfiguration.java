package com.platform.config;

import com.platform.audit.internal.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for contract tests.
 * Extends BaseTestConfiguration to inherit common infrastructure beans.
 * Mocks repositories but uses real service implementations.
 * This allows contract tests to run without needing full JPA/database setup.
 */
@TestConfiguration
@Profile("contract-test")
public class ContractTestConfiguration extends BaseTestConfiguration {

    // Services use real implementations, so no beans needed

    // Repositories are mocked
    @Bean
    @Primary
    public AuditEventRepository auditEventRepository() {
        return createMock(AuditEventRepository.class, "auditEventRepository");
    }

    @Bean
    @Primary
    public AuditLogViewRepository auditLogViewRepository() {
        return createMock(AuditLogViewRepository.class, "auditLogViewRepository");
    }

    @Bean
    @Primary
    public AuditLogExportRepository auditLogExportRepository() {
        return createMock(AuditLogExportRepository.class, "auditLogExportRepository");
    }

    @Bean
    @Primary
    public ComplianceRepository complianceRepository() {
        return createMock(ComplianceRepository.class, "complianceRepository");
    }

    @Bean
    @Primary
    public SecurityAnalyticsRepository securityAnalyticsRepository() {
        return createMock(SecurityAnalyticsRepository.class, "securityAnalyticsRepository");
    }
}