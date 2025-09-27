package com.platform.shared.monitoring;

import com.platform.audit.internal.EnhancedAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced audit performance monitoring endpoints with real-time
 * security analysis and optimization capabilities.
 */
@RestController
@RequestMapping("/api/v1/performance/audit")
@Tag(name = "Audit Performance", description = "Audit system performance monitoring and security analysis")
@PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_ANALYST')")
public class AuditPerformanceController {

    private final EnhancedAuditService auditService;
    private final PerformanceMonitoringService performanceService;

    @Autowired
    public AuditPerformanceController(
            EnhancedAuditService auditService,
            PerformanceMonitoringService performanceService) {
        this.auditService = auditService;
        this.performanceService = performanceService;
    }

    /**
     * Get comprehensive security analysis for an organization
     */
    @GetMapping("/security-analysis/{organizationId}")
    @Operation(summary = "Get comprehensive security analysis")
    public ResponseEntity<SecurityAnalysisResponse> getSecurityAnalysis(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "24") int lookbackHours) {

        Instant since = Instant.now().minus(lookbackHours, ChronoUnit.HOURS);

        EnhancedAuditService.SecurityAnalysisResult analysis =
            auditService.performSecurityAnalysis(organizationId, since, lookbackHours);

        SecurityAnalysisResponse response = new SecurityAnalysisResponse(
            organizationId,
            since,
            lookbackHours,
            analysis.getSecurityEventCount(),
            analysis.getSuspiciousIpCount(),
            analysis.getUserAnomalyCount(),
            analysis.getRiskScore(),
            calculateSecurityStatus(analysis.getRiskScore()),
            generateSecurityRecommendations(analysis)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get audit system performance metrics
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get audit system performance metrics")
    public ResponseEntity<Map<String, Object>> getAuditMetrics() {

        // Get recent events count
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        int recentEventCount = auditService.getRecentEvents(fiveMinutesAgo).size();

        // Get audit-specific performance data
        Map<String, Object> metrics = Map.of(
            "recent_events_5min", recentEventCount,
            "audit_cache_hit_ratio", performanceService.getCacheHitRatio("audit-stats"),
            "average_query_time_ms", getAverageAuditQueryTime(),
            "storage_usage_mb", estimateAuditStorageUsage(),
            "archival_status", getArchivalStatus(),
            "compression_status", getCompressionStatus()
        );

        return ResponseEntity.ok(metrics);
    }

    /**
     * Trigger manual audit archival
     */
    @PostMapping("/archive")
    @Operation(summary = "Trigger manual audit event archival")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerArchival() {

        CompletableFuture<Long> archivalResult = auditService.archiveOldEvents();

        return ResponseEntity.accepted().body(Map.of(
            "message", "Archival process started",
            "status", "processing",
            "estimated_completion", "5-10 minutes"
        ));
    }

    /**
     * Trigger manual audit compression
     */
    @PostMapping("/compress")
    @Operation(summary = "Trigger manual audit event compression")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerCompression() {

        CompletableFuture<Long> compressionResult = auditService.compressOldEvents();

        return ResponseEntity.accepted().body(Map.of(
            "message", "Compression process started",
            "status", "processing",
            "estimated_completion", "2-5 minutes"
        ));
    }

    /**
     * Get audit performance recommendations
     */
    @GetMapping("/recommendations")
    @Operation(summary = "Get audit system performance recommendations")
    public ResponseEntity<AuditPerformanceRecommendations> getAuditRecommendations() {

        AuditPerformanceRecommendations recommendations = new AuditPerformanceRecommendations();

        // Analyze recent performance
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        int recentEvents = auditService.getRecentEvents(oneHourAgo).size();

        if (recentEvents > 10000) {
            recommendations.addRecommendation(
                "High Volume",
                "High audit event volume detected: " + recentEvents + " events in last hour",
                "Consider implementing event sampling or increasing archival frequency",
                "MEDIUM"
            );
        }

        double cacheHitRatio = performanceService.getCacheHitRatio("audit-stats");
        if (cacheHitRatio < 0.8 && cacheHitRatio > 0) {
            recommendations.addRecommendation(
                "Cache Performance",
                String.format("Audit cache hit ratio is low: %.2f", cacheHitRatio),
                "Review audit query patterns and cache TTL settings",
                "MEDIUM"
            );
        }

        long storageUsage = estimateAuditStorageUsage();
        if (storageUsage > 10000) { // > 10GB
            recommendations.addRecommendation(
                "Storage Usage",
                "High audit storage usage: " + storageUsage + " MB",
                "Consider running archival process more frequently or implementing data retention policies",
                "HIGH"
            );
        }

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get real-time audit event stream status
     */
    @GetMapping("/stream-status")
    @Operation(summary = "Get real-time audit event stream status")
    public ResponseEntity<Map<String, Object>> getStreamStatus() {

        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
        Instant fiveMinutesAgo = now.minus(5, ChronoUnit.MINUTES);

        int eventsLastMinute = auditService.getRecentEvents(oneMinuteAgo).size();
        int eventsLastFiveMinutes = auditService.getRecentEvents(fiveMinutesAgo).size();

        double eventsPerSecond = eventsLastMinute / 60.0;
        String streamHealth = eventsPerSecond < 10 ? "healthy" : eventsPerSecond < 50 ? "moderate" : "high";

        Map<String, Object> status = Map.of(
            "events_per_second", Math.round(eventsPerSecond * 100.0) / 100.0,
            "events_last_minute", eventsLastMinute,
            "events_last_5_minutes", eventsLastFiveMinutes,
            "stream_health", streamHealth,
            "processing_lag_ms", estimateProcessingLag(),
            "queue_depth", estimateQueueDepth()
        );

        return ResponseEntity.ok(status);
    }

    // Helper methods

    private double getAverageAuditQueryTime() {
        // This would integrate with actual performance monitoring
        // For now, return a mock value
        return 45.0; // milliseconds
    }

    private long estimateAuditStorageUsage() {
        // This would calculate actual storage usage
        // For now, return a mock value
        return 2048L; // MB
    }

    private String getArchivalStatus() {
        // Check last archival run status
        return "completed_last_week";
    }

    private String getCompressionStatus() {
        // Check last compression run status
        return "completed_last_week";
    }

    private String calculateSecurityStatus(int riskScore) {
        if (riskScore < 20) return "low";
        if (riskScore < 50) return "moderate";
        if (riskScore < 80) return "high";
        return "critical";
    }

    private String[] generateSecurityRecommendations(EnhancedAuditService.SecurityAnalysisResult analysis) {
        java.util.List<String> recommendations = new java.util.ArrayList<>();

        if (analysis.getSecurityEventCount() > 50) {
            recommendations.add("High number of security events detected - review authentication policies");
        }

        if (analysis.getSuspiciousIpCount() > 5) {
            recommendations.add("Multiple suspicious IP addresses found - consider IP blocking");
        }

        if (analysis.getUserAnomalyCount() > 10) {
            recommendations.add("User behavior anomalies detected - review user access patterns");
        }

        if (analysis.getRiskScore() > 70) {
            recommendations.add("High risk score - immediate security review recommended");
        }

        return recommendations.toArray(new String[0]);
    }

    private int estimateProcessingLag() {
        // This would measure actual processing lag
        return 150; // milliseconds
    }

    private int estimateQueueDepth() {
        // This would measure actual queue depth
        return 25; // events
    }

    // Response DTOs

    public static class SecurityAnalysisResponse {
        private final UUID organizationId;
        private final Instant analysisTimestamp;
        private final int lookbackHours;
        private final int securityEventCount;
        private final int suspiciousIpCount;
        private final int userAnomalyCount;
        private final int riskScore;
        private final String securityStatus;
        private final String[] recommendations;

        public SecurityAnalysisResponse(UUID organizationId, Instant analysisTimestamp,
                                        int lookbackHours, int securityEventCount, int suspiciousIpCount,
                                        int userAnomalyCount, int riskScore, String securityStatus,
                                        String[] recommendations) {
            this.organizationId = organizationId;
            this.analysisTimestamp = analysisTimestamp;
            this.lookbackHours = lookbackHours;
            this.securityEventCount = securityEventCount;
            this.suspiciousIpCount = suspiciousIpCount;
            this.userAnomalyCount = userAnomalyCount;
            this.riskScore = riskScore;
            this.securityStatus = securityStatus;
            this.recommendations = recommendations;
        }

        // Getters
        public UUID getOrganizationId() { return organizationId; }
        public Instant getAnalysisTimestamp() { return analysisTimestamp; }
        public int getLookbackHours() { return lookbackHours; }
        public int getSecurityEventCount() { return securityEventCount; }
        public int getSuspiciousIpCount() { return suspiciousIpCount; }
        public int getUserAnomalyCount() { return userAnomalyCount; }
        public int getRiskScore() { return riskScore; }
        public String getSecurityStatus() { return securityStatus; }
        public String[] getRecommendations() { return recommendations; }
    }

    public static class AuditPerformanceRecommendations {
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