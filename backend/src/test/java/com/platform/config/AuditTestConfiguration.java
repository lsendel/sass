package com.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for integration tests.
 * Provides beans that are excluded in test profiles but needed for tests.
 * Only active for integration-test profile.
 */
@TestConfiguration
@Profile("integration-test")
public class AuditTestConfiguration {

    /**
     * Provides a simple CacheManager for tests since CacheAutoConfiguration is excluded.
     * Uses ConcurrentMapCacheManager for in-memory caching.
     * Only creates this bean if one doesn't already exist.
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
