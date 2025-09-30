package com.platform.audit.config;

import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditRequestValidator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.mockito.Mockito;

/**
 * Test configuration for mocking services in integration tests.
 * This prevents Spring from trying to create real service beans that might have complex dependencies.
 */
@TestConfiguration
@Profile("test")
public final class TestServiceConfig {

    /**
     * Provides mock services for tests that need them
     */
    @Bean
    @Primary
    public AuditLogExportService mockAuditLogExportService() {
        return Mockito.mock(AuditLogExportService.class);
    }

    @Bean
    @Primary
    public AuditLogViewService mockAuditLogViewService() {
        return Mockito.mock(AuditLogViewService.class);
    }

    @Bean
    @Primary
    public AuditRequestValidator mockAuditRequestValidator() {
        return Mockito.mock(AuditRequestValidator.class);
    }
}