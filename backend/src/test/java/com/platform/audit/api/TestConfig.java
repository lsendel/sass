package com.platform.audit.api;

import com.platform.audit.internal.AuditLogExportService;
import com.platform.audit.internal.AuditLogViewService;
import com.platform.audit.internal.AuditRequestValidator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}