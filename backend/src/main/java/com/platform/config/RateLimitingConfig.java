package com.platform.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

/**
 * Configuration for rate limiting using Bucket4j and Redis.
 * Provides distributed rate limiting for authentication endpoints.
 *
 * @since 1.0.0
 */
@Configuration
@Profile("!test & !integration-test") // Disable in test profiles
public class RateLimitingConfig {

    /**
     * Creates a proxy manager for distributed rate limiting using Redis.
     *
     * @param redisConnectionFactory the Redis connection factory
     * @return the configured proxy manager
     */
    @Bean
    public LettuceBasedProxyManager<String> proxyManager(
            final RedisConnectionFactory redisConnectionFactory) {

        final LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) redisConnectionFactory;

        return LettuceBasedProxyManager.builderFor(lettuceFactory.getStandaloneConnection())
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                        Duration.ofMinutes(10))) // Expire unused buckets after 10 minutes
                .build();
    }

    /**
     * Creates a bucket configuration supplier for authentication endpoints.
     * Allows 5 requests per minute per IP address.
     *
     * @return the bucket configuration supplier
     */
    @Bean
    public java.util.function.Supplier<BucketConfiguration> authBucketConfiguration() {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(5, Duration.ofMinutes(1))) // 5 requests per minute
                .build();
    }
}