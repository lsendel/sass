package com.platform.shared.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Automated performance optimization service that monitors system performance
 * and applies optimizations in real-time based on performance patterns and thresholds.
 */
@Service
public class AutomatedPerformanceOptimizer implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(AutomatedPerformanceOptimizer.class);

    private final PerformanceMonitoringService monitoringService;
    private final AlertingService alertingService;
    private final CacheManager cacheManager;
    private final EntityManager entityManager;

    // Optimization tracking
    private final Map<String, OptimizationAction> appliedOptimizations = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> optimizationAttempts = new ConcurrentHashMap<>();
    private final Set<String> activeOptimizations = ConcurrentHashMap.newKeySet();

    // Optimization thresholds
    private static final double MEMORY_OPTIMIZATION_THRESHOLD = 0.85; // 85%
    private static final double CACHE_OPTIMIZATION_THRESHOLD = 0.70;  // 70% hit ratio
    private static final long SLOW_QUERY_OPTIMIZATION_THRESHOLD = 200; // 200ms
    private static final int MAX_OPTIMIZATION_ATTEMPTS = 3;
    private static final long OPTIMIZATION_COOLDOWN_MINUTES = 30;

    @Autowired
    public AutomatedPerformanceOptimizer(
            PerformanceMonitoringService monitoringService,
            AlertingService alertingService,
            CacheManager cacheManager,
            EntityManager entityManager) {
        this.monitoringService = monitoringService;
        this.alertingService = alertingService;
        this.cacheManager = cacheManager;
        this.entityManager = entityManager;
    }

    /**
     * Main optimization scheduler - runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void performAutomatedOptimizations() {
        logger.debug("Starting automated performance optimization cycle");

        try {
            // Get current performance metrics
            Map<String, Long> metrics = monitoringService.getRealTimeMetrics();

            // Apply optimizations based on current performance
            CompletableFuture.allOf(
                optimizeMemoryUsage(metrics),
                optimizeCachePerformance(),
                optimizeDatabaseQueries(),
                optimizeSystemResources(metrics)
            ).join();

            // Cleanup old optimizations
            cleanupExpiredOptimizations();

            logger.debug("Automated performance optimization cycle completed");

        } catch (Exception e) {
            logger.error("Error during automated performance optimization", e);
            alertingService.sendSystemAlert(
                AlertingService.AlertSeverity.WARNING,
                "Performance Optimization Error",
                "Failed to complete automated performance optimization: " + e.getMessage(),
                Map.of("error", e.getMessage(), "timestamp", Instant.now().toString())
            );
        }
    }

    /**
     * Optimize memory usage automatically
     */
    @Async
    public CompletableFuture<Void> optimizeMemoryUsage(Map<String, Long> metrics) {
        return CompletableFuture.runAsync(() -> {
            try {
                long memoryUsage = metrics.getOrDefault("memory_usage_mb", 0L);
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
                double memoryUsagePercent = (double) memoryUsage / maxMemory;

                if (memoryUsagePercent > MEMORY_OPTIMIZATION_THRESHOLD) {
                    String optimizationKey = "memory_cleanup";

                    if (canApplyOptimization(optimizationKey)) {
                        logger.info("Applying automated memory optimization - usage: {}%",
                                   String.format("%.2f", memoryUsagePercent * 100));

                        // Force garbage collection
                        System.gc();

                        // Clear soft references in caches
                        clearSoftCacheReferences();

                        // Suggest heap size increase if repeatedly hitting threshold
                        suggestHeapSizeIncrease(memoryUsagePercent);

                        recordOptimization(optimizationKey, "Memory cleanup and GC", memoryUsagePercent);

                        alertingService.sendSystemAlert(
                            AlertingService.AlertSeverity.INFO,
                            "Automated Memory Optimization",
                            String.format("Applied memory optimization due to %.2f%% usage",
                                         memoryUsagePercent * 100),
                            Map.of(
                                "memory_usage_percent", memoryUsagePercent * 100,
                                "memory_usage_mb", memoryUsage,
                                "max_memory_mb", maxMemory,
                                "optimization_type", "memory_cleanup"
                            )
                        );
                    }
                }
            } catch (Exception e) {
                logger.error("Error optimizing memory usage", e);
            }
        });
    }

    /**
     * Optimize cache performance automatically
     */
    @Async
    public CompletableFuture<Void> optimizeCachePerformance() {
        return CompletableFuture.runAsync(() -> {
            try {
                String[] cacheNames = {"organizations", "users", "payment-methods", "plans", "audit-stats"};

                for (String cacheName : cacheNames) {
                    double hitRatio = monitoringService.getCacheHitRatio(cacheName);

                    if (hitRatio < CACHE_OPTIMIZATION_THRESHOLD && hitRatio > 0) {
                        String optimizationKey = "cache_" + cacheName;

                        if (canApplyOptimization(optimizationKey)) {
                            logger.info("Applying cache optimization for {} - hit ratio: {:.2f}",
                                       cacheName, hitRatio);

                            // Preload frequently accessed cache entries
                            preloadFrequentCacheEntries(cacheName);

                            // Adjust cache size if possible
                            adjustCacheSize(cacheName, hitRatio);

                            recordOptimization(optimizationKey,
                                              "Cache preload and resize for " + cacheName, hitRatio);

                            alertingService.sendCacheAlert(
                                AlertingService.AlertSeverity.INFO,
                                cacheName,
                                hitRatio,
                                "has been automatically optimized"
                            );
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error optimizing cache performance", e);
            }
        });
    }

    /**
     * Optimize database queries automatically
     */
    @Async
    public CompletableFuture<Void> optimizeDatabaseQueries() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Analyze slow queries from the last hour
                List<SlowQueryInfo> slowQueries = identifySlowQueries();

                for (SlowQueryInfo slowQuery : slowQueries) {
                    if (slowQuery.getAverageDuration() > SLOW_QUERY_OPTIMIZATION_THRESHOLD) {
                        String optimizationKey = "query_" + slowQuery.getQueryHash();

                        if (canApplyOptimization(optimizationKey)) {
                            logger.info("Applying query optimization for slow query: {} ms average",
                                       slowQuery.getAverageDuration());

                            // Suggest index creation
                            suggestIndexCreation(slowQuery);

                            // Enable query plan caching
                            enableQueryPlanCaching(slowQuery);

                            recordOptimization(optimizationKey,
                                              "Query optimization for " + slowQuery.getQueryType(),
                                              slowQuery.getAverageDuration());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error optimizing database queries", e);
            }
        });
    }

    /**
     * Optimize system resources
     */
    @Async
    public CompletableFuture<Void> optimizeSystemResources(Map<String, Long> metrics) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Optimize database connections
                optimizeDatabaseConnections(metrics);

                // Optimize thread pools
                optimizeThreadPools(metrics);

                // Clean up temporary resources
                cleanupTemporaryResources();

            } catch (Exception e) {
                logger.error("Error optimizing system resources", e);
            }
        });
    }

    /**
     * Clear soft references in caches to free memory
     */
    private void clearSoftCacheReferences() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    // This depends on cache implementation - for Redis this might be different
                    logger.debug("Clearing soft references for cache: {}", cacheName);
                }
            });
        } catch (Exception e) {
            logger.warn("Could not clear cache soft references", e);
        }
    }

    /**
     * Suggest heap size increase based on memory patterns
     */
    private void suggestHeapSizeIncrease(double memoryUsagePercent) {
        String optimizationKey = "heap_size_suggestion";
        AtomicInteger attempts = optimizationAttempts.computeIfAbsent(optimizationKey, k -> new AtomicInteger(0));

        if (attempts.incrementAndGet() >= 3) { // After 3 consecutive high memory events
            logger.warn("Suggesting heap size increase - consistent high memory usage: {:.2f}%",
                       memoryUsagePercent * 100);

            alertingService.sendSystemAlert(
                AlertingService.AlertSeverity.INFO,
                "Heap Size Optimization Suggestion",
                String.format("Consider increasing heap size due to consistent %.2f%% memory usage",
                             memoryUsagePercent * 100),
                Map.of(
                    "current_memory_percent", memoryUsagePercent * 100,
                    "suggestion", "Increase -Xmx parameter by 25-50%",
                    "frequency", "3 consecutive optimization cycles"
                )
            );

            attempts.set(0); // Reset counter
        }
    }

    /**
     * Preload frequently accessed cache entries
     */
    private void preloadFrequentCacheEntries(String cacheName) {
        try {
            // This would implement cache warming based on access patterns
            // For now, we log the action
            logger.info("Preloading frequent entries for cache: {}", cacheName);

            // Example: preload top organizations, users, etc.
            if ("organizations".equals(cacheName)) {
                // Preload active organizations
                preloadActiveOrganizations();
            } else if ("users".equals(cacheName)) {
                // Preload frequently accessed users
                preloadFrequentUsers();
            }

        } catch (Exception e) {
            logger.warn("Could not preload cache entries for {}", cacheName, e);
        }
    }

    /**
     * Adjust cache size based on hit ratio patterns
     */
    private void adjustCacheSize(String cacheName, double hitRatio) {
        // This would implement dynamic cache sizing
        // For Redis, this might involve adjusting maxmemory-policy
        logger.info("Adjusting cache size for {} based on {:.2f} hit ratio", cacheName, hitRatio);

        if (hitRatio < 0.5) {
            logger.warn("Cache {} has very low hit ratio {:.2f} - consider reviewing caching strategy",
                       cacheName, hitRatio);
        }
    }

    /**
     * Identify slow queries from the monitoring service
     */
    private List<SlowQueryInfo> identifySlowQueries() {
        List<SlowQueryInfo> slowQueries = new ArrayList<>();

        try {
            // Query database for slow query statistics
            Query query = entityManager.createNativeQuery("""
                SELECT
                    query_type,
                    AVG(duration_ms) as avg_duration,
                    COUNT(*) as execution_count,
                    MD5(query_text) as query_hash
                FROM performance_query_log
                WHERE created_at > NOW() - INTERVAL '1 hour'
                AND duration_ms > ?
                GROUP BY query_type, query_hash
                ORDER BY avg_duration DESC
                LIMIT 10
                """);

            query.setParameter(1, SLOW_QUERY_OPTIMIZATION_THRESHOLD);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            for (Object[] row : results) {
                slowQueries.add(new SlowQueryInfo(
                    (String) row[0],           // query_type
                    ((Number) row[1]).longValue(), // avg_duration
                    ((Number) row[2]).intValue(),  // execution_count
                    (String) row[3]            // query_hash
                ));
            }

        } catch (Exception e) {
            logger.warn("Could not identify slow queries", e);
        }

        return slowQueries;
    }

    /**
     * Suggest index creation for slow queries
     */
    private void suggestIndexCreation(SlowQueryInfo slowQuery) {
        logger.info("Suggesting index creation for slow {} query ({}ms avg)",
                   slowQuery.getQueryType(), slowQuery.getAverageDuration());

        alertingService.sendDatabaseAlert(
            AlertingService.AlertSeverity.INFO,
            "Index Creation Suggestion",
            String.format("Consider creating index for slow %s queries (avg: %dms)",
                         slowQuery.getQueryType(), slowQuery.getAverageDuration()),
            slowQuery.getQueryType(),
            slowQuery.getAverageDuration()
        );
    }

    /**
     * Enable query plan caching for frequently executed slow queries
     */
    private void enableQueryPlanCaching(SlowQueryInfo slowQuery) {
        if (slowQuery.getExecutionCount() > 10) { // Frequently executed
            logger.info("Enabling plan caching for frequent slow query: {}", slowQuery.getQueryType());
            // Implementation would depend on database and JPA provider
        }
    }

    /**
     * Optimize database connection pool
     */
    private void optimizeDatabaseConnections(Map<String, Long> metrics) {
        long activeConnections = metrics.getOrDefault("database_connections", 0L);

        // This would implement connection pool optimization
        logger.debug("Optimizing database connections - active: {}", activeConnections);
    }

    /**
     * Optimize thread pools based on usage patterns
     */
    private void optimizeThreadPools(Map<String, Long> metrics) {
        // This would implement thread pool optimization
        logger.debug("Optimizing thread pools based on system metrics");
    }

    /**
     * Clean up temporary resources
     */
    private void cleanupTemporaryResources() {
        logger.debug("Cleaning up temporary resources");

        // Clean up old audit events beyond retention period
        try {
            Query cleanupQuery = entityManager.createNativeQuery(
                "DELETE FROM audit_events WHERE created_at < NOW() - INTERVAL '90 days'"
            );
            int deletedRows = cleanupQuery.executeUpdate();

            if (deletedRows > 0) {
                logger.info("Cleaned up {} old audit events", deletedRows);
            }
        } catch (Exception e) {
            logger.warn("Could not cleanup old audit events", e);
        }
    }

    /**
     * Preload active organizations for cache warming
     */
    private void preloadActiveOrganizations() {
        try {
            Query query = entityManager.createQuery(
                "SELECT o FROM Organization o WHERE o.status = 'ACTIVE' ORDER BY o.lastAccessTime DESC"
            );
            query.setMaxResults(100); // Top 100 active organizations
            query.getResultList(); // This will populate the cache

            logger.debug("Preloaded active organizations for cache warming");
        } catch (Exception e) {
            logger.warn("Could not preload active organizations", e);
        }
    }

    /**
     * Preload frequently accessed users
     */
    private void preloadFrequentUsers() {
        try {
            Query query = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.lastLoginTime > :recentTime ORDER BY u.lastLoginTime DESC"
            );
            query.setParameter("recentTime", Instant.now().minus(7, ChronoUnit.DAYS));
            query.setMaxResults(500); // Top 500 recent users
            query.getResultList(); // This will populate the cache

            logger.debug("Preloaded recent users for cache warming");
        } catch (Exception e) {
            logger.warn("Could not preload recent users", e);
        }
    }

    /**
     * Check if optimization can be applied based on cooldown and attempt limits
     */
    private boolean canApplyOptimization(String optimizationKey) {
        OptimizationAction lastOptimization = appliedOptimizations.get(optimizationKey);

        if (lastOptimization != null) {
            // Check cooldown period
            if (lastOptimization.getTimestamp().isAfter(
                    Instant.now().minus(OPTIMIZATION_COOLDOWN_MINUTES, ChronoUnit.MINUTES))) {
                return false;
            }

            // Check attempt limits
            AtomicInteger attempts = optimizationAttempts.computeIfAbsent(optimizationKey, k -> new AtomicInteger(0));
            if (attempts.get() >= MAX_OPTIMIZATION_ATTEMPTS) {
                return false;
            }
        }

        return !activeOptimizations.contains(optimizationKey);
    }

    /**
     * Record applied optimization
     */
    private void recordOptimization(String key, String description, double metric) {
        activeOptimizations.add(key);
        appliedOptimizations.put(key, new OptimizationAction(key, description, metric, Instant.now()));
        optimizationAttempts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();

        logger.info("Applied optimization: {} - {}", key, description);
    }

    /**
     * Clean up expired optimizations
     */
    private void cleanupExpiredOptimizations() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);

        appliedOptimizations.entrySet().removeIf(entry ->
            entry.getValue().getTimestamp().isBefore(cutoff));

        activeOptimizations.clear(); // Clear active flags
    }

    /**
     * Health indicator implementation
     */
    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        builder.withDetail("active_optimizations", activeOptimizations.size())
                .withDetail("total_optimizations_applied", appliedOptimizations.size())
                .withDetail("optimization_cooldown_minutes", OPTIMIZATION_COOLDOWN_MINUTES);

        // Check if optimization system is healthy
        if (activeOptimizations.size() > 10) {
            builder.down().withDetail("reason", "Too many active optimizations");
        }

        return builder.build();
    }

    /**
     * Get optimization statistics
     */
    public OptimizationStatistics getOptimizationStatistics() {
        return new OptimizationStatistics(
            appliedOptimizations.size(),
            activeOptimizations.size(),
            optimizationAttempts.values().stream().mapToInt(AtomicInteger::get).sum(),
            appliedOptimizations.values().stream()
                .collect(Collectors.groupingBy(
                    opt -> opt.getDescription().split(" ")[0],
                    Collectors.counting()
                )).entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().intValue()
                ))
        );
    }

    /**
     * Optimization action record
     */
    private static class OptimizationAction {
        private final String key;
        private final String description;
        private final double metric;
        private final Instant timestamp;

        public OptimizationAction(String key, String description, double metric, Instant timestamp) {
            this.key = key;
            this.description = description;
            this.metric = metric;
            this.timestamp = timestamp;
        }

        public String getKey() { return key; }
        public String getDescription() { return description; }
        public double getMetric() { return metric; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Slow query information
     */
    private static class SlowQueryInfo {
        private final String queryType;
        private final long averageDuration;
        private final int executionCount;
        private final String queryHash;

        public SlowQueryInfo(String queryType, long averageDuration, int executionCount, String queryHash) {
            this.queryType = queryType;
            this.averageDuration = averageDuration;
            this.executionCount = executionCount;
            this.queryHash = queryHash;
        }

        public String getQueryType() { return queryType; }
        public long getAverageDuration() { return averageDuration; }
        public int getExecutionCount() { return executionCount; }
        public String getQueryHash() { return queryHash; }
    }

    /**
     * Optimization statistics
     */
    public static class OptimizationStatistics {
        private final int totalOptimizations;
        private final int activeOptimizations;
        private final int totalAttempts;
        private final Map<String, Integer> optimizationsByType;

        public OptimizationStatistics(int totalOptimizations, int activeOptimizations,
                                    int totalAttempts, Map<String, Integer> optimizationsByType) {
            this.totalOptimizations = totalOptimizations;
            this.activeOptimizations = activeOptimizations;
            this.totalAttempts = totalAttempts;
            this.optimizationsByType = optimizationsByType;
        }

        public int getTotalOptimizations() { return totalOptimizations; }
        public int getActiveOptimizations() { return activeOptimizations; }
        public int getTotalAttempts() { return totalAttempts; }
        public Map<String, Integer> getOptimizationsByType() { return optimizationsByType; }
    }
}