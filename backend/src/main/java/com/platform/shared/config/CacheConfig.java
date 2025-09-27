package com.platform.shared.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Advanced caching configuration for performance optimization.
 * Implements multi-tier caching with different TTL strategies.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Primary cache manager with Redis backend
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Organization data - cache for 1 hour (rarely changes)
        cacheConfigurations.put("organizations", defaultConfig.entryTtl(Duration.ofHours(1)));

        // User data - cache for 30 minutes
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Payment methods - cache for 15 minutes (can change frequently)
        cacheConfigurations.put("payment-methods", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Plans - cache for 4 hours (very stable data)
        cacheConfigurations.put("plans", defaultConfig.entryTtl(Duration.ofHours(4)));

        // Audit statistics - cache for 5 minutes (for dashboards)
        cacheConfigurations.put("audit-stats", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Security analysis - cache for 10 minutes (balance between freshness and performance)
        cacheConfigurations.put("security-analysis", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Session validation - very short cache for performance
        cacheConfigurations.put("session-validation", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Custom key generator for multi-tenant caching
     */
    @Bean("tenantAwareKeyGenerator")
    public KeyGenerator tenantAwareKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName())
               .append(":")
               .append(method.getName());

            // Add tenant context if available
            try {
                var tenantId = com.platform.shared.security.TenantContext.getCurrentTenantId();
                if (tenantId != null) {
                    key.append(":tenant:").append(tenantId);
                }
            } catch (Exception e) {
                // Tenant context not available, continue without it
            }

            // Add method parameters
            for (Object param : params) {
                if (param != null) {
                    key.append(":").append(param.toString());
                }
            }

            return key.toString();
        };
    }

    /**
     * Performance-optimized key generator for high-frequency operations
     */
    @Bean("performanceKeyGenerator")
    public KeyGenerator performanceKeyGenerator() {
        return (target, method, params) -> {
            // Use hash for better Redis performance with long keys
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName())
               .append(":")
               .append(method.getName());

            for (Object param : params) {
                if (param != null) {
                    key.append(":").append(param.hashCode());
                }
            }

            return key.toString();
        };
    }
}