package com.platform.config;

import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditRequestValidator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.mockito.Mockito;

import java.util.Optional;

/**
 * Test configuration for audit module tests.
 * Extends BaseTestConfiguration to inherit common infrastructure beans.
 * Provides mock beans and configured validators for audit testing.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {CacheAutoConfiguration.class})
public class AuditTestConfiguration extends BaseTestConfiguration {

    @Bean
    @Primary
    public AuditLogViewService auditLogViewService() {
        return createMock(AuditLogViewService.class, "auditLogViewService");
    }

    @Bean
    @Primary
    public AuditLogExportService auditLogExportService() {
        return createMock(AuditLogExportService.class, "auditLogExportService");
    }

    @Bean
    @Primary
    public AuditRequestValidator auditRequestValidator() {
        final AuditRequestValidator validator = createMock(AuditRequestValidator.class, "auditRequestValidator");

        // Configure default validation results to avoid NullPointerException
        final AuditRequestValidator.ValidationResult validResult =
            new AuditRequestValidator.ValidationResult(true, null, null);
        final AuditRequestValidator.ParsedDateResult parsedDateResult =
            new AuditRequestValidator.ParsedDateResult(null, validResult);

        Mockito.when(validator.validatePageSize(Mockito.anyInt()))
            .thenReturn(validResult);
        Mockito.when(validator.validateDateRange(Mockito.any(), Mockito.any()))
            .thenReturn(validResult);
        Mockito.when(validator.validateExportFormat(Mockito.anyString()))
            .thenReturn(validResult);
        // Match both null and non-null string arguments
        Mockito.when(validator.parseDate(Mockito.nullable(String.class), Mockito.anyString()))
            .thenReturn(parsedDateResult);
        Mockito.when(validator.parseDate(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(parsedDateResult);

        return validator;
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("test-user");
    }
}