package com.platform.config;

import com.platform.audit.internal.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.Mockito;

/**
 * Test configuration for contract tests.
 * Mocks repositories but uses real service implementations.
 * This allows contract tests to run without needing full JPA/database setup.
 */
@TestConfiguration
@Profile("contract-test")
public class ContractTestConfiguration {

    // Services use real implementations, so no beans needed

    // Repositories are mocked
    @Bean
    @Primary
    public AuditEventRepository auditEventRepository() {
        return Mockito.mock(AuditEventRepository.class);
    }

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
    public ComplianceRepository complianceRepository() {
        return Mockito.mock(ComplianceRepository.class);
    }

    @Bean
    @Primary
    public SecurityAnalyticsRepository securityAnalyticsRepository() {
        return Mockito.mock(SecurityAnalyticsRepository.class);
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