package com.platform.shared.monitoring;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Performance monitoring API endpoints for real-time system health
 * and optimization recommendations.
 */
@RestController
@RequestMapping("/api/v1/performance")
@Tag(name = "Performance Monitoring", description = "Real-time performance metrics and recommendations")
@PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
public class PerformanceController {

    private final PerformanceMonitoringService performanceService;

    @Autowired
    public PerformanceController(PerformanceMonitoringService performanceService) {
        this.performanceService = performanceService;
    }

    /**
     * Get real-time system metrics
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get real-time performance metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Long> realTimeMetrics = performanceService.getRealTimeMetrics();

        Map<String, Object> response = Map.of(
            "timestamp", System.currentTimeMillis(),
            "metrics", realTimeMetrics,
            "status", "healthy"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get performance recommendations
     */
    @GetMapping("/recommendations")
    @Operation(summary = "Get performance optimization recommendations")
    public ResponseEntity<PerformanceMonitoringService.PerformanceRecommendations> getRecommendations() {
        PerformanceMonitoringService.PerformanceRecommendations recommendations =
            performanceService.getPerformanceRecommendations();

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/cache-stats")
    @Operation(summary = "Get cache performance statistics")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> cacheStats = Map.of(
            "organizations", Map.of(
                "hitRatio", performanceService.getCacheHitRatio("organizations"),
                "status", performanceService.getCacheHitRatio("organizations") > 0.8 ? "good" : "needs_attention"
            ),
            "users", Map.of(
                "hitRatio", performanceService.getCacheHitRatio("users"),
                "status", performanceService.getCacheHitRatio("users") > 0.8 ? "good" : "needs_attention"
            ),
            "payment-methods", Map.of(
                "hitRatio", performanceService.getCacheHitRatio("payment-methods"),
                "status", performanceService.getCacheHitRatio("payment-methods") > 0.8 ? "good" : "needs_attention"
            )
        );

        return ResponseEntity.ok(cacheStats);
    }

    /**
     * Get system health summary
     */
    @GetMapping("/health-summary")
    @Operation(summary = "Get comprehensive system health summary")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        Map<String, Long> metrics = performanceService.getRealTimeMetrics();
        PerformanceMonitoringService.PerformanceRecommendations recommendations =
            performanceService.getPerformanceRecommendations();

        // Calculate overall health score
        int healthScore = calculateHealthScore(metrics, recommendations);

        Map<String, Object> summary = Map.of(
            "healthScore", healthScore,
            "status", getHealthStatus(healthScore),
            "metrics", metrics,
            "recommendations", recommendations.getRecommendations(),
            "cachePerformance", Map.of(
                "organizations", performanceService.getCacheHitRatio("organizations"),
                "users", performanceService.getCacheHitRatio("users"),
                "payment-methods", performanceService.getCacheHitRatio("payment-methods")
            )
        );

        return ResponseEntity.ok(summary);
    }

    /**
     * Calculate health score based on metrics and recommendations
     */
    private int calculateHealthScore(Map<String, Long> metrics,
                                   PerformanceMonitoringService.PerformanceRecommendations recommendations) {
        int score = 100; // Start with perfect score

        // Deduct points for memory usage
        long memoryUsage = metrics.getOrDefault("memory_usage_mb", 0L);
        if (memoryUsage > 1024) score -= 20; // High memory usage
        else if (memoryUsage > 512) score -= 10; // Medium memory usage

        // Deduct points for recommendations
        long highPriorityRecs = recommendations.getRecommendations().stream()
            .filter(rec -> "HIGH".equals(rec.getPriority()))
            .count();
        long mediumPriorityRecs = recommendations.getRecommendations().stream()
            .filter(rec -> "MEDIUM".equals(rec.getPriority()))
            .count();

        score -= (int)(highPriorityRecs * 15); // 15 points per high priority issue
        score -= (int)(mediumPriorityRecs * 5);  // 5 points per medium priority issue

        // Deduct points for poor cache performance
        double avgCacheHitRatio = (
            performanceService.getCacheHitRatio("organizations") +
            performanceService.getCacheHitRatio("users") +
            performanceService.getCacheHitRatio("payment-methods")
        ) / 3.0;

        if (avgCacheHitRatio < 0.6) score -= 15; // Poor cache performance
        else if (avgCacheHitRatio < 0.8) score -= 5; // Medium cache performance

        return Math.max(0, Math.min(100, score)); // Ensure score is between 0-100
    }

    /**
     * Get health status based on score
     */
    private String getHealthStatus(int healthScore) {
        if (healthScore >= 90) return "excellent";
        if (healthScore >= 75) return "good";
        if (healthScore >= 60) return "fair";
        if (healthScore >= 40) return "poor";
        return "critical";
    }
}