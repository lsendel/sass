package com.platform.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * Test configuration providing real implementations for testing.
 * No mocks - uses actual service implementations with real dependencies.
 * Services are auto-wired from Spring context.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {CacheAutoConfiguration.class, RedisAutoConfiguration.class})
public class AuditTestConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("test-user");
    }

    /**
     * Provides a simple CacheManager for tests.
     * Uses ConcurrentMapCacheManager for in-memory caching.
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
