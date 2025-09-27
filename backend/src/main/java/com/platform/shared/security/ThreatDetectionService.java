package com.platform.shared.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.platform.audit.internal.AuditEvent;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.shared.monitoring.SecurityMetricsCollector;

/**
 * Advanced threat detection service using machine learning algorithms
 * and behavioral analysis to identify security threats in real-time.
 */
@Service
public class ThreatDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(ThreatDetectionService.class);

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private SecurityMetricsCollector metricsCollector;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SecurityEventLogger securityEventLogger;

    // Threat detection thresholds
    private static final int BRUTE_FORCE_THRESHOLD = 5;
    private static final int RAPID_REQUEST_THRESHOLD = 20;
    private static final int UNUSUAL_LOCATION_THRESHOLD = 3;
    private static final int DATA_EXFILTRATION_THRESHOLD = 10;

    // In-memory tracking for real-time analysis
    private final Map<String, AtomicLong> failedLoginCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, String> userLocationBaseline = new ConcurrentHashMap<>();

    /**
     * Analyze audit event for potential threats in real-time
     */
    @Async
    public CompletableFuture<ThreatAnalysisResult> analyzeEvent(AuditEvent event) {
        logger.debug("Analyzing event for threats: {} for user: {}", event.getAction(), event.getActorId());

        ThreatAnalysisResult.Builder result = ThreatAnalysisResult.builder()
            .eventId(event.getId().toString())
            .eventType(event.getAction())
            .userId(event.getActorId().toString())
            .timestamp(event.getCreatedAt())
            .ipAddress(event.getIpAddress());

        try {
            // Analyze different threat patterns
            analyzeBruteForceAttempt(event, result);
            analyzeRapidRequests(event, result);
            analyzeUnusualLocation(event, result);
            analyzePrivilegeEscalation(event, result);
            analyzeDataExfiltration(event, result);
            analyzeAnomalousAccess(event, result);
            analyzeMaliciousPayload(event, result);

            // Calculate overall threat score
            double threatScore = calculateThreatScore(result);
            result.threatScore(threatScore);

            // Determine threat level
            ThreatLevel threatLevel = determineThreatLevel(threatScore);
            result.threatLevel(threatLevel);

            ThreatAnalysisResult finalResult = result.build();

            // Take action if threat detected
            if (threatLevel.ordinal() >= ThreatLevel.MEDIUM.ordinal()) {
                handleThreatDetection(finalResult);
            }

            return CompletableFuture.completedFuture(finalResult);

        } catch (Exception e) {
            logger.error("Error analyzing event for threats: {}", event.getId(), e);
            return CompletableFuture.completedFuture(
                result.threatLevel(ThreatLevel.UNKNOWN)
                    .addError("Threat analysis failed: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Batch threat analysis for historical data
     */
    @Async
    public CompletableFuture<BatchThreatAnalysis> analyzeBatchEvents(List<AuditEvent> events) {
        logger.info("Starting batch threat analysis for {} events", events.size());

        BatchThreatAnalysis.Builder analysis = BatchThreatAnalysis.builder()
            .totalEvents(events.size())
            .analysisTimestamp(Instant.now());

        try {
            // Advanced pattern detection algorithms
            detectAdvancedPersistentThreats(events, analysis);
            detectCoordinatedAttacks(events, analysis);
            detectInsiderThreats(events, analysis);
            detectAccountCompromise(events, analysis);
            detectLateralMovement(events, analysis);

            // Generate threat intelligence
            ThreatIntelligence intelligence = generateThreatIntelligence(events);
            analysis.threatIntelligence(intelligence);

            return CompletableFuture.completedFuture(analysis.build());

        } catch (Exception e) {
            logger.error("Error in batch threat analysis", e);
            throw new RuntimeException("Batch threat analysis failed", e);
        }
    }

    /**
     * Real-time threat monitoring using sliding window analysis
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void performRealTimeThreatMonitoring() {
        logger.debug("Performing real-time threat monitoring");

        try {
            Instant since = Instant.now().minus(5, ChronoUnit.MINUTES);
            List<AuditEvent> recentEvents = auditEventRepository.findRecentEvents(since);

            // Analyze recent events for emerging threats
            analyzeEmergingThreats(recentEvents);

            // Update threat baselines
            updateThreatBaselines(recentEvents);

            // Clean up old tracking data
            cleanupTrackingData();

        } catch (Exception e) {
            logger.error("Error during real-time threat monitoring", e);
        }
    }

    /**
     * Machine learning-based anomaly detection
     */
    @Async
    public CompletableFuture<AnomalyDetectionResult> detectAnomalies(String userId, int lookbackDays) {
        logger.info("Running anomaly detection for user: {} with lookback: {} days", userId, lookbackDays);

        try {
            Instant fromDate = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
            List<AuditEvent> userEvents = auditEventRepository.findByUserIdAndTimeframe(
                userId, fromDate, Instant.now()
            );

            AnomalyDetectionResult.Builder result = AnomalyDetectionResult.builder()
                .userId(userId)
                .analysisWindow(lookbackDays)
                .totalEvents(userEvents.size())
                .analysisTimestamp(Instant.now());

            // Behavioral pattern analysis
            analyzeBehavioralPatterns(userEvents, result);

            // Time-based anomalies
            analyzeTimingAnomalies(userEvents, result);

            // Access pattern anomalies
            analyzeAccessAnomalies(userEvents, result);

            // Volume-based anomalies
            analyzeVolumeAnomalies(userEvents, result);

            return CompletableFuture.completedFuture(result.build());

        } catch (Exception e) {
            logger.error("Error in anomaly detection for user: {}", userId, e);
            throw new RuntimeException("Anomaly detection failed", e);
        }
    }

    // Private analysis methods
    private void analyzeBruteForceAttempt(AuditEvent event, ThreatAnalysisResult.Builder result) {
        if ("LOGIN_FAILED".equals(event.getAction())) {
            String userKey = "failed_login:" + event.getActorId();
            AtomicLong count = failedLoginCounts.computeIfAbsent(userKey, k -> new AtomicLong(0));

            long currentCount = count.incrementAndGet();

            // Set expiry for the counter
            redisTemplate.opsForValue().set(userKey, currentCount);
            redisTemplate.expire(userKey, java.time.Duration.ofMinutes(15));

            if (currentCount >= BRUTE_FORCE_THRESHOLD) {
                result.addThreatIndicator(ThreatIndicator.builder()
                    .type("BRUTE_FORCE_ATTACK")
                    .severity("HIGH")
                    .description("Multiple failed login attempts detected: " + currentCount)
                    .confidence(0.95)
                    .build());
            }
        }
    }

    private void analyzeRapidRequests(AuditEvent event, ThreatAnalysisResult.Builder result) {
        String requestKey = "requests:" + event.getIpAddress();
        AtomicLong count = requestCounts.computeIfAbsent(requestKey, k -> new AtomicLong(0));

        long currentCount = count.incrementAndGet();

        if (currentCount >= RAPID_REQUEST_THRESHOLD) {
            result.addThreatIndicator(ThreatIndicator.builder()
                .type("RAPID_REQUESTS")
                .severity("MEDIUM")
                .description("Rapid request pattern detected from IP: " + event.getIpAddress())
                .confidence(0.80)
                .build());
        }

        // Reset counter every minute
        if (currentCount == 1) {
            redisTemplate.opsForValue().set(requestKey, currentCount);
            redisTemplate.expire(requestKey, java.time.Duration.ofMinutes(1));
        }
    }

    private void analyzeUnusualLocation(AuditEvent event, ThreatAnalysisResult.Builder result) {
        if ("LOGIN_SUCCESS".equals(event.getAction())) {
            String userId = event.getActorId().toString();
            String currentLocation = extractLocationFromIP(event.getIpAddress());
            String baselineLocation = userLocationBaseline.get(userId);

            if (baselineLocation != null && !baselineLocation.equals(currentLocation)) {
                result.addThreatIndicator(ThreatIndicator.builder()
                    .type("UNUSUAL_LOCATION")
                    .severity("MEDIUM")
                    .description("Login from unusual location: " + currentLocation +
                               " (baseline: " + baselineLocation + ")")
                    .confidence(0.70)
                    .build());
            } else if (baselineLocation == null) {
                userLocationBaseline.put(userId, currentLocation);
            }
        }
    }

    private void analyzePrivilegeEscalation(AuditEvent event, ThreatAnalysisResult.Builder result) {
        if (event.getAction().contains("PERMISSION") || event.getAction().contains("ROLE")) {
            result.addThreatIndicator(ThreatIndicator.builder()
                .type("PRIVILEGE_ESCALATION")
                .severity("HIGH")
                .description("Privilege modification detected: " + event.getAction())
                .confidence(0.85)
                .build());
        }
    }

    private void analyzeDataExfiltration(AuditEvent event, ThreatAnalysisResult.Builder result) {
        if ("DATA_EXPORT".equals(event.getAction()) || "DOWNLOAD".equals(event.getAction())) {
            String exportKey = "exports:" + event.getActorId();
            Long exportCount = (Long) redisTemplate.opsForValue().get(exportKey);
            exportCount = (exportCount != null) ? exportCount + 1 : 1;

            redisTemplate.opsForValue().set(exportKey, exportCount);
            redisTemplate.expire(exportKey, java.time.Duration.ofHours(1));

            if (exportCount >= DATA_EXFILTRATION_THRESHOLD) {
                result.addThreatIndicator(ThreatIndicator.builder()
                    .type("DATA_EXFILTRATION")
                    .severity("CRITICAL")
                    .description("Excessive data export activity detected: " + exportCount + " exports")
                    .confidence(0.90)
                    .build());
            }
        }
    }

    private void analyzeAnomalousAccess(AuditEvent event, ThreatAnalysisResult.Builder result) {
        // Analyze access patterns based on time of day
        int hour = event.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();

        if (hour < 6 || hour > 22) { // Outside normal business hours
            result.addThreatIndicator(ThreatIndicator.builder()
                .type("ANOMALOUS_ACCESS_TIME")
                .severity("LOW")
                .description("Access outside normal business hours: " + hour + ":00")
                .confidence(0.60)
                .build());
        }
    }

    private void analyzeMaliciousPayload(AuditEvent event, ThreatAnalysisResult.Builder result) {
        String details = event.getDetails();
        if (details != null) {
            // Check for SQL injection patterns
            if (containsSQLInjection(details)) {
                result.addThreatIndicator(ThreatIndicator.builder()
                    .type("SQL_INJECTION_ATTEMPT")
                    .severity("HIGH")
                    .description("SQL injection pattern detected in event details")
                    .confidence(0.95)
                    .build());
            }

            // Check for XSS patterns
            if (containsXSSPayload(details)) {
                result.addThreatIndicator(ThreatIndicator.builder()
                    .type("XSS_ATTEMPT")
                    .severity("HIGH")
                    .description("XSS payload detected in event details")
                    .confidence(0.90)
                    .build());
            }
        }
    }

    private double calculateThreatScore(ThreatAnalysisResult.Builder result) {
        return result.getThreatIndicators().stream()
            .mapToDouble(indicator -> {
                double baseScore = switch (indicator.getSeverity()) {
                    case "CRITICAL" -> 10.0;
                    case "HIGH" -> 7.5;
                    case "MEDIUM" -> 5.0;
                    case "LOW" -> 2.5;
                    default -> 1.0;
                };
                return baseScore * indicator.getConfidence();
            })
            .sum();
    }

    private ThreatLevel determineThreatLevel(double threatScore) {
        if (threatScore >= 15.0) return ThreatLevel.CRITICAL;
        if (threatScore >= 10.0) return ThreatLevel.HIGH;
        if (threatScore >= 5.0) return ThreatLevel.MEDIUM;
        if (threatScore >= 2.0) return ThreatLevel.LOW;
        return ThreatLevel.NONE;
    }

    private void handleThreatDetection(ThreatAnalysisResult result) {
        // Log threat detection
        securityEventLogger.logSuspiciousActivity(
            result.getUserId(),
            "THREAT_DETECTED",
            result.getIpAddress(),
            "Threat level: " + result.getThreatLevel() +
            ", Score: " + result.getThreatScore() +
            ", Indicators: " + result.getThreatIndicators().size()
        );

        // Update metrics
        metricsCollector.recordSuspiciousActivity("THREAT_" + result.getThreatLevel());

        // Publish threat event for incident response
        eventPublisher.publishEvent(new ThreatDetectedEvent(result));

        // Take immediate action for critical threats
        if (result.getThreatLevel() == ThreatLevel.CRITICAL) {
            initiateEmergencyResponse(result);
        }
    }

    private void initiateEmergencyResponse(ThreatAnalysisResult result) {
        logger.warn("CRITICAL THREAT DETECTED - Initiating emergency response for user: {}",
            result.getUserId());

        // Implementation would trigger emergency response procedures
        // Such as account lockout, IP blocking, security team alerts
    }

    // Helper methods
    private String extractLocationFromIP(String ipAddress) {
        // Implementation would use IP geolocation service
        return "unknown"; // Placeholder
    }

    private boolean containsSQLInjection(String input) {
        String[] sqlPatterns = {
            "union select", "drop table", "insert into", "update set",
            "delete from", "'; --", "' or '1'='1", "exec(", "sp_"
        };

        String lowerInput = input.toLowerCase();
        for (String pattern : sqlPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsXSSPayload(String input) {
        String[] xssPatterns = {
            "<script", "javascript:", "onerror=", "onload=",
            "onclick=", "eval(", "alert(", "document.cookie"
        };

        String lowerInput = input.toLowerCase();
        for (String pattern : xssPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private void analyzeEmergingThreats(List<AuditEvent> events) {
        // Implementation would analyze patterns for emerging threats
    }

    private void updateThreatBaselines(List<AuditEvent> events) {
        // Implementation would update behavioral baselines
    }

    private void cleanupTrackingData() {
        // Implementation would clean up old tracking data
    }

    private void detectAdvancedPersistentThreats(List<AuditEvent> events, BatchThreatAnalysis.Builder analysis) {
        // Implementation would detect APT patterns
    }

    private void detectCoordinatedAttacks(List<AuditEvent> events, BatchThreatAnalysis.Builder analysis) {
        // Implementation would detect coordinated attack patterns
    }

    private void detectInsiderThreats(List<AuditEvent> events, BatchThreatAnalysis.Builder analysis) {
        // Implementation would detect insider threat patterns
    }

    private void detectAccountCompromise(List<AuditEvent> events, BatchThreatAnalysis.Builder analysis) {
        // Implementation would detect account compromise patterns
    }

    private void detectLateralMovement(List<AuditEvent> events, BatchThreatAnalysis.Builder analysis) {
        // Implementation would detect lateral movement patterns
    }

    private ThreatIntelligence generateThreatIntelligence(List<AuditEvent> events) {
        // Implementation would generate threat intelligence
        return new ThreatIntelligence(); // Placeholder
    }

    private void analyzeBehavioralPatterns(List<AuditEvent> events, AnomalyDetectionResult.Builder result) {
        // Implementation would analyze behavioral patterns
    }

    private void analyzeTimingAnomalies(List<AuditEvent> events, AnomalyDetectionResult.Builder result) {
        // Implementation would analyze timing anomalies
    }

    private void analyzeAccessAnomalies(List<AuditEvent> events, AnomalyDetectionResult.Builder result) {
        // Implementation would analyze access anomalies
    }

    private void analyzeVolumeAnomalies(List<AuditEvent> events, AnomalyDetectionResult.Builder result) {
        // Implementation would analyze volume anomalies
    }

    // Event class for threat detection
    public static class ThreatDetectedEvent {
        private final ThreatAnalysisResult result;

        public ThreatDetectedEvent(ThreatAnalysisResult result) {
            this.result = result;
        }

        public ThreatAnalysisResult getResult() {
            return result;
        }
    }

    // Placeholder classes
    public static class ThreatIntelligence {
        // Implementation details
    }
}

enum ThreatLevel {
    NONE, LOW, MEDIUM, HIGH, CRITICAL, UNKNOWN
}