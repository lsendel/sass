package com.platform.shared.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced performance monitor that integrates monitoring with intelligent alerting.
 * Automatically triggers alerts based on performance thresholds and patterns.
 */
@Component
public class EnhancedPerformanceMonitor {

    private final PerformanceMonitoringService monitoringService;
    private final AlertingService alertingService;

    // Performance thresholds for alerting
    private static final long CRITICAL_API_RESPONSE_MS = 1000;
    private static final long WARNING_API_RESPONSE_MS = 500;
    private static final long CRITICAL_DB_QUERY_MS = 500;
    private static final long WARNING_DB_QUERY_MS = 100;
    private static final double CRITICAL_CACHE_HIT_RATIO = 0.6;
    private static final double WARNING_CACHE_HIT_RATIO = 0.8;
    private static final long CRITICAL_MEMORY_MB = 1536; // 1.5GB
    private static final long WARNING_MEMORY_MB = 1024;  // 1GB

    @Autowired
    public EnhancedPerformanceMonitor(
            PerformanceMonitoringService monitoringService,
            AlertingService alertingService) {
        this.monitoringService = monitoringService;
        this.alertingService = alertingService;
    }

    /**
     * Record database query with intelligent alerting
     */
    public void recordDatabaseQueryWithAlerting(String queryType, String query, Duration duration) {
        // Record the performance metric
        monitoringService.recordDatabaseQuery(queryType, query, duration);

        long durationMs = duration.toMillis();

        // Trigger alerts based on performance thresholds
        if (durationMs > CRITICAL_DB_QUERY_MS) {
            alertingService.sendDatabaseAlert(
                AlertingService.AlertSeverity.CRITICAL,
                "Critical Database Performance",
                String.format("Database query took %dms, exceeding critical threshold of %dms",
                    durationMs, CRITICAL_DB_QUERY_MS),
                queryType,
                durationMs
            );
        } else if (durationMs > WARNING_DB_QUERY_MS) {
            alertingService.sendDatabaseAlert(
                AlertingService.AlertSeverity.WARNING,
                "Slow Database Query",
                String.format("Database query took %dms, exceeding warning threshold of %dms",
                    durationMs, WARNING_DB_QUERY_MS),
                queryType,
                durationMs
            );
        }
    }

    /**
     * Record API response with intelligent alerting
     */
    public void recordApiResponseWithAlerting(String endpoint, String method, Duration duration, int statusCode) {
        // Record the performance metric
        monitoringService.recordApiResponse(endpoint, method, duration, statusCode);

        long durationMs = duration.toMillis();

        // Trigger alerts for slow API responses
        if (durationMs > CRITICAL_API_RESPONSE_MS) {
            Map<String, Object> apiMetrics = Map.of(
                "endpoint", endpoint,
                "method", method,
                "duration_ms", durationMs,
                "status_code", statusCode
            );

            alertingService.sendPerformanceAlert(
                AlertingService.AlertSeverity.CRITICAL,
                "Critical API Performance",
                String.format("API %s %s took %dms, exceeding critical threshold",
                    method, endpoint, durationMs),
                apiMetrics
            );
        } else if (durationMs > WARNING_API_RESPONSE_MS) {
            Map<String, Object> apiMetrics = Map.of(
                "endpoint", endpoint,
                "method", method,
                "duration_ms", durationMs,
                "status_code", statusCode
            );

            alertingService.sendPerformanceAlert(
                AlertingService.AlertSeverity.WARNING,
                "Slow API Response",
                String.format("API %s %s took %dms, exceeding warning threshold",
                    method, endpoint, durationMs),
                apiMetrics
            );
        }

        // Alert on error rates
        if (statusCode >= 500) {
            Map<String, Object> errorMetrics = Map.of(
                "endpoint", endpoint,
                "method", method,
                "status_code", statusCode,
                "error_type", "server_error"
            );

            alertingService.sendPerformanceAlert(
                AlertingService.AlertSeverity.WARNING,
                "API Server Error",
                String.format("API %s %s returned status %d", method, endpoint, statusCode),
                errorMetrics
            );
        }
    }

    /**
     * Monitor cache performance with alerting
     */
    public void monitorCachePerformanceWithAlerting(String cacheName) {
        double hitRatio = monitoringService.getCacheHitRatio(cacheName);

        if (hitRatio < CRITICAL_CACHE_HIT_RATIO && hitRatio > 0) {
            alertingService.sendCacheAlert(
                AlertingService.AlertSeverity.CRITICAL,
                cacheName,
                hitRatio,
                "has critically low hit ratio"
            );
        } else if (hitRatio < WARNING_CACHE_HIT_RATIO && hitRatio > 0) {
            alertingService.sendCacheAlert(
                AlertingService.AlertSeverity.WARNING,
                cacheName,
                hitRatio,
                "has low hit ratio"
            );
        }
    }

    /**
     * Monitor system resources with alerting
     */
    public void monitorSystemResourcesWithAlerting() {
        Map<String, Long> metrics = monitoringService.getRealTimeMetrics();
        long memoryUsage = metrics.getOrDefault("memory_usage_mb", 0L);

        if (memoryUsage > CRITICAL_MEMORY_MB) {
            Map<String, Object> systemMetrics = Map.of(
                "memory_usage_mb", memoryUsage,
                "threshold_exceeded", "critical",
                "available_memory_mb", Runtime.getRuntime().maxMemory() / 1024 / 1024
            );

            alertingService.sendSystemAlert(
                AlertingService.AlertSeverity.CRITICAL,
                "Critical Memory Usage",
                String.format("System memory usage is %dMB, exceeding critical threshold of %dMB",
                    memoryUsage, CRITICAL_MEMORY_MB),
                systemMetrics
            );
        } else if (memoryUsage > WARNING_MEMORY_MB) {
            Map<String, Object> systemMetrics = Map.of(
                "memory_usage_mb", memoryUsage,
                "threshold_exceeded", "warning",
                "available_memory_mb", Runtime.getRuntime().maxMemory() / 1024 / 1024
            );

            alertingService.sendSystemAlert(
                AlertingService.AlertSeverity.WARNING,
                "High Memory Usage",
                String.format("System memory usage is %dMB, exceeding warning threshold of %dMB",
                    memoryUsage, WARNING_MEMORY_MB),
                systemMetrics
            );
        }
    }

    /**
     * Comprehensive performance health check with alerting
     */
    public CompletableFuture<Void> performHealthCheckWithAlerting() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Check system resources
                monitorSystemResourcesWithAlerting();

                // Check cache performance
                String[] cacheNames = {"organizations", "users", "payment-methods", "plans", "audit-stats"};
                for (String cacheName : cacheNames) {
                    monitorCachePerformanceWithAlerting(cacheName);
                }

                // Check overall system health
                var healthSummary = monitoringService.health();
                if (healthSummary.getStatus().getCode().equals("DOWN")) {
                    Map<String, Object> healthMetrics = Map.of(
                        "status", "DOWN",
                        "details", healthSummary.getDetails(),
                        "check_time", Instant.now().toString()
                    );

                    alertingService.sendSystemAlert(
                        AlertingService.AlertSeverity.CRITICAL,
                        "System Health Check Failed",
                        "Application health check returned DOWN status",
                        healthMetrics
                    );
                }

            } catch (Exception e) {
                Map<String, Object> errorMetrics = Map.of(
                    "error_type", "health_check_failure",
                    "error_message", e.getMessage(),
                    "stack_trace", e.getStackTrace()[0].toString()
                );

                alertingService.sendSystemAlert(
                    AlertingService.AlertSeverity.CRITICAL,
                    "Health Check System Error",
                    "Failed to perform system health check: " + e.getMessage(),
                    errorMetrics
                );
            }
        });
    }

    /**
     * Monitor performance trends and predict issues
     */
    public void analyzePerformanceTrends() {
        try {
            Map<String, Long> currentMetrics = monitoringService.getRealTimeMetrics();

            // Simple trend analysis (in production, this would use historical data)
            long totalRequests = currentMetrics.getOrDefault("total_requests", 0L);
            long memoryUsage = currentMetrics.getOrDefault("memory_usage_mb", 0L);

            // Predict memory issues based on request volume
            if (totalRequests > 10000 && memoryUsage > WARNING_MEMORY_MB * 0.8) {
                Map<String, Object> trendMetrics = Map.of(
                    "total_requests", totalRequests,
                    "memory_usage_mb", memoryUsage,
                    "trend", "increasing",
                    "prediction", "memory_pressure_likely"
                );

                alertingService.sendPerformanceAlert(
                    AlertingService.AlertSeverity.INFO,
                    "Performance Trend Alert",
                    "High request volume with elevated memory usage detected - potential memory pressure",
                    trendMetrics
                );
            }

        } catch (Exception e) {
            // Log error but don't alert for trend analysis failures
            System.err.println("Failed to analyze performance trends: " + e.getMessage());
        }
    }

    /**
     * Advanced security performance monitoring
     */
    public void monitorSecurityPerformance(String organizationId, String securityEventType, Duration processingTime) {
        long processingMs = processingTime.toMillis();

        // Alert on slow security processing
        if (processingMs > 1000) { // 1 second threshold for security operations
            Map<String, Object> securityMetrics = Map.of(
                "organization_id", organizationId,
                "event_type", securityEventType,
                "processing_time_ms", processingMs,
                "threshold_exceeded", true
            );

            alertingService.sendSecurityAlert(
                AlertingService.AlertSeverity.WARNING,
                "Slow Security Processing",
                String.format("Security event processing took %dms for %s", processingMs, securityEventType),
                organizationId,
                securityMetrics
            );
        }
    }

    /**
     * Monitor audit system performance
     */
    public void monitorAuditPerformance(String auditOperation, Duration executionTime, int recordCount) {
        long executionMs = executionTime.toMillis();

        // Alert on slow audit operations
        if (executionMs > 2000) { // 2 second threshold for audit operations
            Map<String, Object> auditMetrics = Map.of(
                "operation", auditOperation,
                "execution_time_ms", executionMs,
                "record_count", recordCount,
                "records_per_second", recordCount / (executionMs / 1000.0)
            );

            alertingService.sendPerformanceAlert(
                AlertingService.AlertSeverity.WARNING,
                "Slow Audit Operation",
                String.format("Audit operation '%s' took %dms for %d records",
                    auditOperation, executionMs, recordCount),
                auditMetrics
            );
        }
    }

    /**
     * Get performance monitoring configuration
     */
    public PerformanceConfiguration getConfiguration() {
        return new PerformanceConfiguration(
            CRITICAL_API_RESPONSE_MS,
            WARNING_API_RESPONSE_MS,
            CRITICAL_DB_QUERY_MS,
            WARNING_DB_QUERY_MS,
            CRITICAL_CACHE_HIT_RATIO,
            WARNING_CACHE_HIT_RATIO,
            CRITICAL_MEMORY_MB,
            WARNING_MEMORY_MB
        );
    }

    /**
     * Performance configuration container
     */
    public static class PerformanceConfiguration {
        private final long criticalApiResponseMs;
        private final long warningApiResponseMs;
        private final long criticalDbQueryMs;
        private final long warningDbQueryMs;
        private final double criticalCacheHitRatio;
        private final double warningCacheHitRatio;
        private final long criticalMemoryMb;
        private final long warningMemoryMb;

        public PerformanceConfiguration(long criticalApiResponseMs, long warningApiResponseMs,
                                       long criticalDbQueryMs, long warningDbQueryMs,
                                       double criticalCacheHitRatio, double warningCacheHitRatio,
                                       long criticalMemoryMb, long warningMemoryMb) {
            this.criticalApiResponseMs = criticalApiResponseMs;
            this.warningApiResponseMs = warningApiResponseMs;
            this.criticalDbQueryMs = criticalDbQueryMs;
            this.warningDbQueryMs = warningDbQueryMs;
            this.criticalCacheHitRatio = criticalCacheHitRatio;
            this.warningCacheHitRatio = warningCacheHitRatio;
            this.criticalMemoryMb = criticalMemoryMb;
            this.warningMemoryMb = warningMemoryMb;
        }

        // Getters
        public long getCriticalApiResponseMs() { return criticalApiResponseMs; }
        public long getWarningApiResponseMs() { return warningApiResponseMs; }
        public long getCriticalDbQueryMs() { return criticalDbQueryMs; }
        public long getWarningDbQueryMs() { return warningDbQueryMs; }
        public double getCriticalCacheHitRatio() { return criticalCacheHitRatio; }
        public double getWarningCacheHitRatio() { return warningCacheHitRatio; }
        public long getCriticalMemoryMb() { return criticalMemoryMb; }
        public long getWarningMemoryMb() { return warningMemoryMb; }
    }
}