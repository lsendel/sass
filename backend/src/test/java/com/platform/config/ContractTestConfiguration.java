package com.platform.config;

import com.platform.audit.internal.AuditLogViewRepository;
import com.platform.audit.internal.AuditLogExportRepository;
import com.platform.audit.internal.AuditEventRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.mockito.Mockito;

/**
 * Test configuration for contract tests.
 * Provides mock repositories instead of services to avoid CGLIB proxy issues with final classes.
 */
@TestConfiguration
@Profile("contract-test")
public class ContractTestConfiguration {

    /**
     * Mock the repository layer instead of the service layer to avoid final class proxy issues.
     */
    @Bean
    @Primary
    public AuditLogViewRepository auditLogViewRepository() {
        return Mockito.mock(AuditLogViewRepository.class);
    }

    @Bean
    @Primary
    public AuditLogExportRepository auditLogExportRepository() {
        return Mockito.mock(AuditLogExportRepository.class);
    }

    @Bean
    @Primary
    public AuditEventRepository auditEventRepository() {
        return Mockito.mock(AuditEventRepository.class);
    }
}