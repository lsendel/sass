package com.platform.shared.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced performance monitoring service with real-time metrics collection,
 * anomaly detection, and automated optimization recommendations.
 */
@Service
public class PerformanceMonitoringService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);

    private final MeterRegistry meterRegistry;
    private final EntityManager entityManager;

    // Performance counters
    private final Counter slowQueriesCounter;
    private final Counter cacheHitsCounter;
    private final Counter cacheMissesCounter;
    private final Timer databaseQueryTimer;
    private final Timer apiResponseTimer;

    // Real-time metrics storage
    private final Map<String, AtomicLong> realTimeMetrics = new ConcurrentHashMap<>();
    private final Map<String, QueryPerformanceMetric> slowQueries = new ConcurrentHashMap<>();

    // Performance thresholds
    private static final long SLOW_QUERY_THRESHOLD_MS = 100;
    private static final long CRITICAL_QUERY_THRESHOLD_MS = 1000;
    private static final double CACHE_HIT_RATIO_THRESHOLD = 0.80;
    private static final long MEMORY_THRESHOLD_MB = 1024;

    @Autowired
    public PerformanceMonitoringService(MeterRegistry meterRegistry, EntityManager entityManager) {
        this.meterRegistry = meterRegistry;
        this.entityManager = entityManager;

        // Initialize counters
        this.slowQueriesCounter = Counter.builder("database.queries.slow")
                .description("Number of slow database queries")
                .register(meterRegistry);

        this.cacheHitsCounter = Counter.builder("cache.hits")
                .description("Number of cache hits")
                .register(meterRegistry);

        this.cacheMissesCounter = Counter.builder("cache.misses")
                .description("Number of cache misses")
                .register(meterRegistry);

        this.databaseQueryTimer = Timer.builder("database.query.duration")
                .description("Database query execution time")
                .register(meterRegistry);

        this.apiResponseTimer = Timer.builder("api.response.duration")
                .description("API response time")
                .register(meterRegistry);

        // Initialize real-time metrics
        initializeMetrics();
    }

    private void initializeMetrics() {
        realTimeMetrics.put("total_requests", new AtomicLong(0));
        realTimeMetrics.put("active_sessions", new AtomicLong(0));
        realTimeMetrics.put("database_connections", new AtomicLong(0));
        realTimeMetrics.put("memory_usage_mb", new AtomicLong(0));
        realTimeMetrics.put("cpu_usage_percent", new AtomicLong(0));
    }

    /**
     * Record database query performance
     */
    public void recordDatabaseQuery(String queryType, String query, Duration duration) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("database.query.duration")
            .tags("query_type", queryType)
            .register(meterRegistry));

        long durationMs = duration.toMillis();

        if (durationMs > SLOW_QUERY_THRESHOLD_MS) {
            slowQueriesCounter.increment();
            recordSlowQuery(queryType, query, durationMs);

            logger.warn("Slow query detected: {} ms - Type: {} - Query: {}",
                    durationMs, queryType, truncateQuery(query));

            if (durationMs > CRITICAL_QUERY_THRESHOLD_MS) {
                logger.error("CRITICAL: Very slow query detected: {} ms - {}", durationMs, queryType);
                // Trigger alert mechanism
                triggerSlowQueryAlert(queryType, query, durationMs);
            }
        }
    }

    /**
     * Record API response performance
     */
    public void recordApiResponse(String endpoint, String method, Duration duration, int statusCode) {
        Timer.builder("api.response.duration")
                .tags("endpoint", endpoint,
                      "method", method,
                      "status", String.valueOf(statusCode))
                .register(meterRegistry)
                .record(duration);

        // Track request count
        realTimeMetrics.get("total_requests").incrementAndGet();

        // Log slow API responses
        long durationMs = duration.toMillis();
        if (durationMs > 500) { // 500ms threshold for API responses
            logger.warn("Slow API response: {} ms - {} {}", durationMs, method, endpoint);
        }
    }

    /**
     * Record cache performance
     */
    public void recordCacheHit(String cacheName) {
        Counter.builder("cache.hits")
            .tags("cache", cacheName)
            .register(meterRegistry)
            .increment();
    }

    public void recordCacheMiss(String cacheName) {
        Counter.builder("cache.misses")
            .tags("cache", cacheName)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Calculate cache hit ratio
     */
    public double getCacheHitRatio(String cacheName) {
        double hits = Counter.builder("cache.hits")
            .tags("cache", cacheName)
            .register(meterRegistry)
            .count();
        double misses = Counter.builder("cache.misses")
            .tags("cache", cacheName)
            .register(meterRegistry)
            .count();
        double total = hits + misses;

        return total > 0 ? hits / total : 0.0;
    }

    /**
     * Record system metrics periodically
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void collectSystemMetrics() {
        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = (totalMemory - freeMemory) / 1024 / 1024; // MB

        realTimeMetrics.get("memory_usage_mb").set(usedMemory);

        // Database connection count
        try {
            Query query = entityManager.createNativeQuery(
                    "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'"
            );
            Number activeConnections = (Number) query.getSingleResult();
            realTimeMetrics.get("database_connections").set(activeConnections.longValue());
        } catch (Exception e) {
            logger.warn("Could not fetch database connection count: {}", e.getMessage());
        }

        // Check for performance issues
        checkPerformanceThresholds();
    }

    /**
     * Check performance thresholds and trigger alerts
     */
    private void checkPerformanceThresholds() {
        long memoryUsage = realTimeMetrics.get("memory_usage_mb").get();
        if (memoryUsage > MEMORY_THRESHOLD_MB) {
            logger.warn("High memory usage detected: {} MB", memoryUsage);
            // Trigger memory alert
        }

        // Check cache hit ratios
        String[] cacheNames = {"organizations", "users", "payment-methods"};
        for (String cacheName : cacheNames) {
            double hitRatio = getCacheHitRatio(cacheName);
            if (hitRatio < CACHE_HIT_RATIO_THRESHOLD && hitRatio > 0) {
                logger.warn("Low cache hit ratio for {}: {:.2f}", cacheName, hitRatio);
            }
        }
    }

    /**
     * Get performance recommendations based on collected metrics
     */
    public PerformanceRecommendations getPerformanceRecommendations() {
        PerformanceRecommendations recommendations = new PerformanceRecommendations();

        // Analyze slow queries
        if (!slowQueries.isEmpty()) {
            recommendations.addRecommendation(
                    "Database Performance",
                    "Detected " + slowQueries.size() + " slow query patterns",
                    "Consider adding indexes or optimizing query structure",
                    "HIGH"
            );
        }

        // Analyze cache performance
        for (String cacheName : new String[]{"organizations", "users", "payment-methods"}) {
            double hitRatio = getCacheHitRatio(cacheName);
            if (hitRatio < CACHE_HIT_RATIO_THRESHOLD && hitRatio > 0) {
                recommendations.addRecommendation(
                        "Cache Performance",
                        String.format("Cache '%s' has low hit ratio: %.2f", cacheName, hitRatio),
                        "Review cache TTL settings or cache key strategy",
                        "MEDIUM"
                );
            }
        }

        // Memory usage recommendations
        long memoryUsage = realTimeMetrics.get("memory_usage_mb").get();
        if (memoryUsage > MEMORY_THRESHOLD_MB) {
            recommendations.addRecommendation(
                    "Memory Usage",
                    String.format("High memory usage: %d MB", memoryUsage),
                    "Consider increasing heap size or investigating memory leaks",
                    "HIGH"
            );
        }

        return recommendations;
    }

    /**
     * Health indicator implementation
     */
    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // Add key metrics to health endpoint
        builder.withDetail("memory_usage_mb", realTimeMetrics.get("memory_usage_mb").get())
                .withDetail("total_requests", realTimeMetrics.get("total_requests").get())
                .withDetail("database_connections", realTimeMetrics.get("database_connections").get())
                .withDetail("slow_queries_count", slowQueries.size());

        // Check if any critical thresholds are exceeded
        long memoryUsage = realTimeMetrics.get("memory_usage_mb").get();
        if (memoryUsage > MEMORY_THRESHOLD_MB * 1.5) { // 1.5x threshold for critical
            builder.down().withDetail("reason", "Critical memory usage detected");
        }

        if (slowQueries.size() > 10) { // More than 10 unique slow queries
            builder.down().withDetail("reason", "Too many slow queries detected");
        }

        return builder.build();
    }

    /**
     * Record slow query for analysis
     */
    private void recordSlowQuery(String queryType, String query, long durationMs) {
        String queryHash = Integer.toString(query.hashCode());
        slowQueries.computeIfAbsent(queryHash, k -> new QueryPerformanceMetric())
                .recordExecution(queryType, query, durationMs);
    }

    /**
     * Trigger alert for critical slow queries
     */
    @Async
    private void triggerSlowQueryAlert(String queryType, String query, long durationMs) {
        // Implementation for alerting mechanism (email, Slack, etc.)
        logger.error("ALERT: Critical slow query - Type: {}, Duration: {}ms, Query: {}",
                queryType, durationMs, truncateQuery(query));
    }

    /**
     * Truncate query for logging
     */
    private String truncateQuery(String query) {
        return query.length() > 200 ? query.substring(0, 200) + "..." : query;
    }

    /**
     * Get real-time metrics
     */
    public Map<String, Long> getRealTimeMetrics() {
        return realTimeMetrics.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                ));
    }

    /**
     * Inner class for query performance tracking
     */
    private static class QueryPerformanceMetric {
        private String queryType;
        private String query;
        private long totalExecutions = 0;
        private long totalDuration = 0;
        private long maxDuration = 0;
        private Instant firstSeen = Instant.now();
        private Instant lastSeen = Instant.now();

        public void recordExecution(String queryType, String query, long durationMs) {
            this.queryType = queryType;
            this.query = query;
            this.totalExecutions++;
            this.totalDuration += durationMs;
            this.maxDuration = Math.max(this.maxDuration, durationMs);
            this.lastSeen = Instant.now();
        }

        public double getAverageDuration() {
            return totalExecutions > 0 ? (double) totalDuration / totalExecutions : 0;
        }
    }

    /**
     * Performance recommendations container
     */
    public static class PerformanceRecommendations {
        private final java.util.List<Recommendation> recommendations = new java.util.ArrayList<>();

        public void addRecommendation(String category, String issue, String suggestion, String priority) {
            recommendations.add(new Recommendation(category, issue, suggestion, priority));
        }

        public java.util.List<Recommendation> getRecommendations() {
            return recommendations;
        }

        public static class Recommendation {
            private final String category;
            private final String issue;
            private final String suggestion;
            private final String priority;

            public Recommendation(String category, String issue, String suggestion, String priority) {
                this.category = category;
                this.issue = issue;
                this.suggestion = suggestion;
                this.priority = priority;
            }

            // Getters
            public String getCategory() { return category; }
            public String getIssue() { return issue; }
            public String getSuggestion() { return suggestion; }
            public String getPriority() { return priority; }
        }
    }
}