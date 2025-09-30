package com.platform.audit.api;

import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditRequestValidator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

@TestConfiguration
public class TestConfig {
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
}