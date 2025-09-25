package com.platform.shared.metrics;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Metrics endpoint for exposing application metrics.
 * Provides access to tracked application metrics for monitoring.
 */
@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "Application metrics and monitoring")
public class MetricsController {
    
    private final MetricsService metricsService;
    
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    @GetMapping
    @Operation(
        summary = "Get metrics snapshot",
        description = "Returns a snapshot of all current application metrics"
    )
    @ApiResponse(responseCode = "200", description = "Metrics snapshot")
    public ResponseEntity<MetricsService.MetricsSnapshot> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetricsSnapshot());
    }
    
    @GetMapping("/counters")
    @Operation(
        summary = "Get counter metrics",
        description = "Returns all current counter metrics"
    )
    @ApiResponse(responseCode = "200", description = "Counter metrics")
    public ResponseEntity<Object> getCounters() {
        return ResponseEntity.ok(metricsService.getMetricsSnapshot().counters());
    }
    
    @GetMapping("/timers")
    @Operation(
        summary = "Get timer metrics",
        description = "Returns all current timer metrics"
    )
    @ApiResponse(responseCode = "200", description = "Timer metrics")
    public ResponseEntity<Object> getTimers() {
        return ResponseEntity.ok(metricsService.getMetricsSnapshot().timers());
    }
    
    @GetMapping("/gauges")
    @Operation(
        summary = "Get gauge metrics",
        description = "Returns all current gauge metrics"
    )
    @ApiResponse(responseCode = "200", description = "Gauge metrics")
    public ResponseEntity<Object> getGauges() {
        return ResponseEntity.ok(metricsService.getMetricsSnapshot().gauges());
    }
}