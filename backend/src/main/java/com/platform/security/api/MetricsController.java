package com.platform.security.api;

import com.platform.security.internal.AlertRuleService;
import com.platform.security.internal.MetricsService;
import com.platform.security.internal.SecurityMetric;
import com.platform.security.internal.AlertRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API controller for security metrics and alerting.
 * Implements all endpoints from metrics-api.yaml contract.
 *
 * Constitutional Compliance:
 * - Performance: <200ms API response times with Redis caching
 * - Real-time: <1s latency for metrics streaming via Server-Sent Events
 * - Security: Role-based access controls for all metrics endpoints
 * - Observability: Request/response logging with correlation IDs
 */
@RestController
@RequestMapping("/api/v1/metrics")
@Validated
@PreAuthorize("hasRole('SECURITY_ANALYST') or hasRole('SECURITY_ADMIN')")
public class MetricsController {

    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);

    private final MetricsService metricsService;
    private final AlertRuleService alertRuleService;
    private final Map<String, SseEmitter> activeMetricsConnections = new ConcurrentHashMap<>();

    public MetricsController(MetricsService metricsService, AlertRuleService alertRuleService) {
        this.metricsService = metricsService;
        this.alertRuleService = alertRuleService;
    }

    /**
     * Query security metrics with time-series aggregation.
     * GET /api/v1/metrics
     */
    @GetMapping
    public ResponseEntity<MetricsQueryResponse> queryMetrics(
            @RequestParam @NotNull List<String> metricNames,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @NotNull Instant fromTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @NotNull Instant toTimestamp,
            @RequestParam(defaultValue = "HOUR") SecurityMetric.AggregationInterval granularity,
            @RequestParam(required = false) Map<String, String> tags) {

        log.info("Querying metrics: names={}, from={}, to={}, granularity={}, tags={}",
                metricNames, fromTimestamp, toTimestamp, granularity, tags);

        var metrics = metricsService.queryMetrics(
                metricNames, fromTimestamp, toTimestamp, granularity, tags);

        var response = new MetricsQueryResponse(metrics);

        log.info("Retrieved {} metric series with {} total data points",
                metrics.size(), metrics.stream().mapToInt(s -> s.dataPoints().size()).sum());

        return ResponseEntity.ok(response);
    }

    /**
     * Get available metrics for dashboard configuration.
     * GET /api/v1/metrics/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<MetricsService.MetricDefinition>> listAvailableMetrics() {
        log.debug("Retrieving available metrics");

        var metrics = metricsService.getAvailableMetrics();

        log.debug("Retrieved {} available metrics", metrics.size());

        return ResponseEntity.ok(metrics);
    }

    /**
     * Real-time metrics stream via Server-Sent Events.
     * GET /api/v1/metrics/realtime
     */
    @GetMapping(value = "/realtime", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics(@RequestParam @NotNull List<String> metricNames) {
        log.info("Starting metrics stream: metricNames={}", metricNames);

        var emitter = new SseEmitter(60000L); // 60 second timeout
        var connectionId = java.util.UUID.randomUUID().toString();

        // Store connection for real-time updates
        activeMetricsConnections.put(connectionId, emitter);

        // Handle connection cleanup
        emitter.onCompletion(() -> {
            activeMetricsConnections.remove(connectionId);
            log.debug("Metrics stream completed: connectionId={}", connectionId);
        });

        emitter.onTimeout(() -> {
            activeMetricsConnections.remove(connectionId);
            log.debug("Metrics stream timed out: connectionId={}", connectionId);
        });

        emitter.onError(throwable -> {
            activeMetricsConnections.remove(connectionId);
            log.warn("Metrics stream error: connectionId={}", connectionId, throwable);
        });

        // Send initial connection event with latest metrics
        try {
            var latestMetrics = metricsService.getLatestMetrics(metricNames);
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "connectionId", connectionId,
                            "timestamp", Instant.now(),
                            "latestMetrics", latestMetrics
                    )));
        } catch (Exception e) {
            log.error("Failed to send initial metrics stream event", e);
            emitter.completeWithError(e);
        }

        log.info("Metrics stream established: connectionId={}, metricNames={}", connectionId, metricNames);

        return emitter;
    }

    /**
     * Get metric statistics for dashboard summary.
     * GET /api/v1/metrics/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getMetricStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {

        log.debug("Getting metric statistics: since={}", since);

        var statistics = metricsService.getMetricStatistics(
                since != null ? since : Instant.now().minusSeconds(24 * 60 * 60));

        log.debug("Retrieved metric statistics for {} metrics",
                ((Map<?, ?>) statistics.get("statistics")).size());

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get aggregated metrics for dashboard performance.
     * GET /api/v1/metrics/{metricName}/aggregate
     */
    @GetMapping("/{metricName}/aggregate")
    public ResponseEntity<List<MetricsService.AggregatedMetric>> aggregateMetrics(
            @PathVariable @NotNull String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @NotNull Instant fromTimestamp,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @NotNull Instant toTimestamp,
            @RequestParam(defaultValue = "hour") String granularity) {

        log.debug("Aggregating metrics: name={}, from={}, to={}, granularity={}",
                metricName, fromTimestamp, toTimestamp, granularity);

        var aggregatedMetrics = metricsService.aggregateMetrics(
                metricName, fromTimestamp, toTimestamp, granularity);

        log.debug("Aggregated {} time buckets for metric: {}", aggregatedMetrics.size(), metricName);

        return ResponseEntity.ok(aggregatedMetrics);
    }

    /**
     * Detect metric anomalies for alerting.
     * GET /api/v1/metrics/{metricName}/anomalies
     */
    @GetMapping("/{metricName}/anomalies")
    public ResponseEntity<List<MetricsService.MetricAnomaly>> detectAnomalies(
            @PathVariable @NotNull String metricName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {

        var analysisStart = since != null ? since : Instant.now().minusSeconds(24 * 60 * 60);
        log.debug("Detecting anomalies for metric: {}, since: {}", metricName, analysisStart);

        var anomalies = metricsService.detectAnomalies(metricName, analysisStart);

        log.debug("Detected {} anomalies for metric: {}", anomalies.size(), metricName);

        return ResponseEntity.ok(anomalies);
    }

    // Alert Rules Management

    /**
     * List alert rules.
     * GET /api/v1/alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<AlertRule>> listAlertRules(@RequestParam(required = false) Boolean enabled) {
        log.debug("Listing alert rules: enabled={}", enabled);

        var rules = alertRuleService.listAlertRules(enabled);

        log.debug("Retrieved {} alert rules", rules.size());

        return ResponseEntity.ok(rules);
    }

    /**
     * Create alert rule.
     * POST /api/v1/alerts
     */
    @PostMapping("/alerts")
    @PreAuthorize("hasRole('SECURITY_ADMIN')")
    public ResponseEntity<AlertRule> createAlertRule(@Valid @RequestBody AlertRuleCreateRequest request) {
        log.info("Creating alert rule: name={}, severity={}, condition={}",
                request.name(), request.severity(), request.condition());

        // Convert request to entity
        var alertRule = new AlertRule();
        alertRule.setName(request.name());
        alertRule.setDescription(request.description());
        alertRule.setCondition(request.condition());
        alertRule.setSeverity(request.severity());
        alertRule.setEnabled(request.enabled());
        alertRule.setThreshold(request.threshold());
        alertRule.setTimeWindow(java.time.Duration.parse(request.timeWindow()));
        alertRule.setCooldownPeriod(java.time.Duration.parse(request.cooldownPeriod()));
        alertRule.setNotificationChannels(request.notificationChannels());
        alertRule.setEscalationRules(request.escalationRules());

        var savedRule = alertRuleService.createAlertRule(alertRule);

        log.info("Alert rule created successfully: id={}, name={}",
                savedRule.getId(), savedRule.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedRule);
    }

    /**
     * Get alert rule by ID.
     * GET /api/v1/alerts/{ruleId}
     */
    @GetMapping("/alerts/{ruleId}")
    public ResponseEntity<AlertRule> getAlertRule(@PathVariable @NotNull String ruleId) {
        log.debug("Retrieving alert rule: id={}", ruleId);

        var rule = alertRuleService.getAlertRule(ruleId);

        if (rule.isPresent()) {
            log.debug("Alert rule found: id={}, name={}, enabled={}",
                    ruleId, rule.get().getName(), rule.get().getEnabled());
            return ResponseEntity.ok(rule.get());
        } else {
            log.warn("Alert rule not found: id={}", ruleId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update alert rule.
     * PUT /api/v1/alerts/{ruleId}
     */
    @PutMapping("/alerts/{ruleId}")
    @PreAuthorize("hasRole('SECURITY_ADMIN')")
    public ResponseEntity<AlertRule> updateAlertRule(
            @PathVariable @NotNull String ruleId,
            @Valid @RequestBody AlertRuleUpdateRequest request) {

        log.info("Updating alert rule: id={}", ruleId);

        try {
            // Convert request to entity
            var updates = new AlertRule();
            updates.setName(request.name());
            updates.setDescription(request.description());
            updates.setCondition(request.condition());
            updates.setEnabled(request.enabled());
            updates.setThreshold(request.threshold());
            if (request.timeWindow() != null) {
                updates.setTimeWindow(java.time.Duration.parse(request.timeWindow()));
            }
            if (request.cooldownPeriod() != null) {
                updates.setCooldownPeriod(java.time.Duration.parse(request.cooldownPeriod()));
            }
            updates.setNotificationChannels(request.notificationChannels());
            updates.setEscalationRules(request.escalationRules());

            var updatedRule = alertRuleService.updateAlertRule(ruleId, updates);

            log.info("Alert rule updated successfully: id={}, name={}",
                    updatedRule.getId(), updatedRule.getName());

            return ResponseEntity.ok(updatedRule);

        } catch (IllegalArgumentException e) {
            log.warn("Alert rule not found for update: id={}", ruleId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete alert rule.
     * DELETE /api/v1/alerts/{ruleId}
     */
    @DeleteMapping("/alerts/{ruleId}")
    @PreAuthorize("hasRole('SECURITY_ADMIN')")
    public ResponseEntity<Void> deleteAlertRule(@PathVariable @NotNull String ruleId) {
        log.info("Deleting alert rule: id={}", ruleId);

        try {
            alertRuleService.deleteAlertRule(ruleId);

            log.info("Alert rule deleted successfully: id={}", ruleId);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            log.warn("Alert rule not found for deletion: id={}", ruleId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get alert rule trigger history.
     * GET /api/v1/alerts/{ruleId}/history
     */
    @GetMapping("/alerts/{ruleId}/history")
    public ResponseEntity<List<AlertRuleService.AlertTrigger>> getAlertHistory(
            @PathVariable @NotNull String ruleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTimestamp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTimestamp) {

        log.debug("Getting alert history: ruleId={}, from={}, to={}", ruleId, fromTimestamp, toTimestamp);

        var history = alertRuleService.getAlertHistory(ruleId, fromTimestamp, toTimestamp);

        log.debug("Retrieved {} alert triggers for rule: {}", history.size(), ruleId);

        return ResponseEntity.ok(history);
    }

    /**
     * Get alert rule statistics.
     * GET /api/v1/alerts/statistics
     */
    @GetMapping("/alerts/statistics")
    public ResponseEntity<Map<String, Object>> getAlertStatistics() {
        log.debug("Getting alert rule statistics");

        var statistics = alertRuleService.getAlertStatistics();

        log.debug("Retrieved alert statistics: total={}, enabled={}",
                statistics.get("totalRules"), statistics.get("enabledRules"));

        return ResponseEntity.ok(statistics);
    }

    // Helper methods for real-time streaming

    public void notifyMetricsSubscribers(SecurityMetric metric) {
        if (activeMetricsConnections.isEmpty()) {
            return;
        }

        log.debug("Notifying {} metrics subscribers of metric: name={}, value={}",
                activeMetricsConnections.size(), metric.getMetricName(), metric.getValue());

        var metricData = Map.of(
                "type", "metric-update",
                "metric", metric,
                "timestamp", Instant.now()
        );

        // Send to all active connections
        activeMetricsConnections.entrySet().removeIf(entry -> {
            try {
                entry.getValue().send(SseEmitter.event()
                        .name("metric-update")
                        .data(metricData));
                return false; // Keep connection
            } catch (Exception e) {
                log.warn("Failed to send metric to connection: {}", entry.getKey(), e);
                return true; // Remove connection
            }
        });
    }

    // Request/Response DTOs

    public record MetricsQueryResponse(List<MetricsService.MetricSeries> metrics) {}

    public record AlertRuleCreateRequest(
            @NotNull String name,
            String description,
            @NotNull String condition,
            @NotNull AlertRule.Severity severity,
            Boolean enabled,
            @NotNull Double threshold,
            @NotNull String timeWindow,
            String cooldownPeriod,
            List<String> notificationChannels,
            Map<String, Object> escalationRules
    ) {}

    public record AlertRuleUpdateRequest(
            String name,
            String description,
            String condition,
            Boolean enabled,
            Double threshold,
            String timeWindow,
            String cooldownPeriod,
            List<String> notificationChannels,
            Map<String, Object> escalationRules
    ) {}
}