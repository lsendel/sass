package com.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for integration tests.
 * Provides beans required for testing with real databases.
 *
 * <p>This configuration is active for the integration-test profile and provides:
 * <ul>
 *   <li>CacheManager for @EnableCaching support</li>
 *   <li>RedisConnectionFactory connected to TestContainers Redis</li>
 *   <li>RedisTemplate for OpaqueTokenService</li>
 * </ul>
 *
 * @since 1.0.0
 */
@TestConfiguration
@Profile("integration-test")
public class IntegrationTestConfiguration {

    /**
     * Provides a simple in-memory CacheManager for integration tests.
     * Required because @EnableCaching is on AuditApplication.
     *
     * @return in-memory cache manager
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    /**
     * Provides RedisConnectionFactory connected to TestContainers Redis.
     * Properties are dynamically set by AbstractIntegrationTest.
     *
     * @param host Redis host from TestContainers
     * @param port Redis port from TestContainers
     * @return Redis connection factory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host}") final String host,
            @Value("${spring.data.redis.port}") final int port) {
        final RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Provides RedisTemplate<String, String> for OpaqueTokenService.
     * Uses String serializers for both keys and values.
     *
     * @param connectionFactory the Redis connection factory
     * @return configured Redis template
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(
            final RedisConnectionFactory connectionFactory) {
        final RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}

