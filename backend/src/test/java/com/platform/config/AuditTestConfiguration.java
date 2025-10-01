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
import org.springframework.data.redis.connection.RedisConnectionFactory;
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
        AuditRequestValidator validator = Mockito.mock(AuditRequestValidator.class);
        // Configure default validation results to avoid NullPointerException
        AuditRequestValidator.ValidationResult validResult =
            new AuditRequestValidator.ValidationResult(true, null, null);
        Mockito.when(validator.validatePageSize(Mockito.anyInt()))
            .thenReturn(validResult);
        Mockito.when(validator.validateDateRange(Mockito.any(), Mockito.any()))
            .thenReturn(validResult);
        Mockito.when(validator.validateExportFormat(Mockito.anyString()))
            .thenReturn(validResult);
        return validator;
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("test-user");
    }

    /**
     * Provides a mock RedisConnectionFactory for tests.
     * Since Redis is disabled in test configuration, we provide a mock to satisfy dependencies.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    /**
     * Provides a simple CacheManager for tests.
     * Uses ConcurrentMapCacheManager for in-memory caching.
     */
    @Bean
    public org.springframework.cache.CacheManager cacheManager() {
        return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
    }

}