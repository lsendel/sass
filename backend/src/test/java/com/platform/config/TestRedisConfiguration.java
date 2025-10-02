package com.platform.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * Test configuration that provides an embedded Redis server for integration tests.
 *
 * This allows tests to run without requiring an external Redis instance,
 * making tests faster, more reliable, and easier to run in CI/CD environments.
 */
@TestConfiguration
public class TestRedisConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(TestRedisConfiguration.class);
    private static final int REDIS_PORT = 6370; // Use different port to avoid conflicts

    private RedisServer redisServer;

    /**
     * Start embedded Redis server before tests run.
     */
    @PostConstruct
    public void startRedis() {
        try {
            LOG.info("Starting embedded Redis server on port {}", REDIS_PORT);
            redisServer = new RedisServer(REDIS_PORT);
            redisServer.start();
            LOG.info("Embedded Redis server started successfully");
        } catch (IOException e) {
            LOG.error("Failed to start embedded Redis server", e);
            throw new RuntimeException("Could not start embedded Redis server", e);
        }
    }

    /**
     * Stop embedded Redis server after tests complete.
     */
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            LOG.info("Stopping embedded Redis server");
            redisServer.stop();
            LOG.info("Embedded Redis server stopped");
        }
    }

    /**
     * Provide Redis connection factory for tests.
     * Uses @Primary to override any other Redis configuration.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        LOG.info("Creating Redis connection factory for embedded Redis on port {}", REDIS_PORT);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", REDIS_PORT);
        return new LettuceConnectionFactory(config);
    }
}
