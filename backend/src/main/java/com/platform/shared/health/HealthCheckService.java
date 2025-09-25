package com.platform.shared.health;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.platform.shared.logging.LoggingUtil;

/**
 * Health check service that monitors the overall health of the application.
 * Provides health checks for various components and external dependencies.
 */
@Service
public class HealthCheckService {
    
    private static final Logger logger = LoggingUtil.getLogger(HealthCheckService.class);
    
    private final AtomicBoolean databaseHealthy = new AtomicBoolean(true);
    private final AtomicBoolean stripeHealthy = new AtomicBoolean(true);
    private final AtomicBoolean redisHealthy = new AtomicBoolean(true);
    
    /**
     * Performs a comprehensive health check of the application.
     * 
     * @return HealthStatus containing overall health and component-specific status
     */
    public HealthStatus performHealthCheck() {
        boolean overallHealthy = databaseHealthy.get() && stripeHealthy.get() && redisHealthy.get();
        
        Map<String, Object> details = Map.of(
            "database", databaseHealthy.get(),
            "stripe", stripeHealthy.get(),
            "redis", redisHealthy.get()
        );
        
        HealthStatus status = new HealthStatus(
            overallHealthy,
            details,
            System.currentTimeMillis()
        );
        
        if (!overallHealthy) {
            logger.warn("Health check failed: {}", status);
        } else {
            logger.debug("Health check passed: {}", status);
        }
        
        return status;
    }
    
    /**
     * Updates the health status of the database component.
     */
    public void updateDatabaseHealth(boolean healthy) {
        databaseHealthy.set(healthy);
        logger.info("Database health status updated to: {}", healthy);
    }
    
    /**
     * Updates the health status of the Stripe payment service.
     */
    public void updateStripeHealth(boolean healthy) {
        stripeHealthy.set(healthy);
        logger.info("Stripe health status updated to: {}", healthy);
    }
    
    /**
     * Updates the health status of the Redis cache service.
     */
    public void updateRedisHealth(boolean healthy) {
        redisHealthy.set(healthy);
        logger.info("Redis health status updated to: {}", healthy);
    }
    
    /**
     * Record for health status response.
     */
    public record HealthStatus(
        boolean healthy,
        Map<String, Object> components,
        long timestamp
    ) {}
}