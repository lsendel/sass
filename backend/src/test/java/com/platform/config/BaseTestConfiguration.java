package com.platform.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Base test configuration providing common infrastructure beans for all test profiles.
 * This abstract class consolidates duplicate configuration across test types.
 *
 * <p>Provides:
 * <ul>
 *   <li>Mock RedisConnectionFactory for session management</li>
 *   <li>In-memory CacheManager for caching operations</li>
 *   <li>BCrypt PasswordEncoder for security operations</li>
 *   <li>Utility method for creating typed mock beans</li>
 * </ul>
 *
 * @see TestBeanConfiguration
 * @see AuditTestConfiguration
 * @see ContractTestConfiguration
 */
@TestConfiguration
public abstract class BaseTestConfiguration {

    /**
     * Provides a mock RedisConnectionFactory for tests.
     * Since Redis is disabled in test configuration, we provide a mock to satisfy dependencies.
     *
     * @return mocked RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    /**
     * Provides a simple CacheManager for tests.
     * Uses ConcurrentMapCacheManager for in-memory caching without external dependencies.
     *
     * @return in-memory CacheManager implementation
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    /**
     * Provides a BCrypt PasswordEncoder for security operations in tests.
     * Uses default strength (10) for faster test execution.
     *
     * @return BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Utility method to create typed mock beans.
     * Provides consistent mock creation across all test configurations.
     *
     * @param <T> the type of the bean to mock
     * @param clazz the class to create a mock for
     * @return mocked instance of the specified class
     */
    protected <T> T createMock(final Class<T> clazz) {
        return Mockito.mock(clazz);
    }

    /**
     * Utility method to create a mock with a specific name for better debugging.
     *
     * @param <T> the type of the bean to mock
     * @param clazz the class to create a mock for
     * @param name the name for the mock (used in error messages)
     * @return named mock instance of the specified class
     */
    protected <T> T createMock(final Class<T> clazz, final String name) {
        return Mockito.mock(clazz, name);
    }
}
