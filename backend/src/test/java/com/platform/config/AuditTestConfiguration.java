package com.platform.config;

import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditRequestValidator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.mockito.Mockito;

import java.util.Optional;

/**
 * Test configuration to provide mock beans and minimal security setup for tests.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {CacheAutoConfiguration.class})
public class AuditTestConfiguration {

    @Bean
    @Primary
    public AuditLogViewService auditLogViewService() {
        return Mockito.mock(AuditLogViewService.class);
    }

    @Bean
    @Primary
    public AuditLogExportService auditLogExportService() {
        return Mockito.mock(AuditLogExportService.class);
    }

    @Bean
    @Primary
    public AuditRequestValidator auditRequestValidator() {
        return Mockito.mock(AuditRequestValidator.class);
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("test-user");
    }

}