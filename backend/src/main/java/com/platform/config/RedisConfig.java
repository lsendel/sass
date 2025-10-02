package com.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis configuration for session management and token storage.
 * Only active in non-test profiles.
 *
 * @since 1.0.0
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400) // 24 hours
@Profile("!test & !integration-test") // Disable in test profiles
public class RedisConfig {

    /**
     * Configures Redis template for string operations.
     * Used for storing opaque tokens.
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
