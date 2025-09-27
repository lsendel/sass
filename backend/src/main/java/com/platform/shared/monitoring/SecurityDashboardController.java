package com.platform.shared.monitoring;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * REST controller providing security metrics for monitoring dashboards.
 * Restricted to administrators only.
 */
@RestController
@RequestMapping("/api/v1/admin/security")
@PreAuthorize("hasRole('ADMIN')")
public class SecurityDashboardController {

    @Autowired
    private SecurityMetricsCollector metricsCollector;

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * Get security overview metrics for the main dashboard.
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSecurityOverview() {
        Map<String, Object> overview = Map.of(
            "activeSessions", getCounterValue("security.sessions.active"),
            "lockedAccounts", getCounterValue("security.accounts.locked"),
            "authFailuresLast24h", metricsCollector.getFailedLoginAttemptsLast24Hours(),
            "rateLimitViolationsLastHour", metricsCollector.getRateLimitViolationsLastHour(),
            "averageSessionDuration", metricsCollector.getAverageSessionDuration(),
            "timestamp", Instant.now()
        );

        return ResponseEntity.ok(overview);
    }

    /**
     * Get detailed authentication metrics.
     */
    @GetMapping("/authentication")
    public ResponseEntity<Map<String, Object>> getAuthenticationMetrics(
            @RequestParam(defaultValue = "24") int hours) {

        Map<String, Object> authMetrics = Map.of(
            "totalFailures", getCounterValue("security.authentication.failures"),
            "failuresByReason", getAuthFailuresByReason(),
            "successRate", calculateAuthSuccessRate(),
            "averageAuthTime", getTimerMean("security.authentication.duration"),
            "timeWindow", hours + " hours",
            "timestamp", Instant.now()
        );

        return ResponseEntity.ok(authMetrics);
    }

    /**
     * Get attack attempt metrics.
     */
    @GetMapping("/attacks")
    public ResponseEntity<Map<String, Object>> getAttackMetrics() {
        Map<String, Object> attackMetrics = Map.of(
            "xssAttempts", getCounterValue("security.xss.attempts"),
            "sqlInjectionAttempts", getCounterValue("security.sql_injection.attempts"),
            "rateLimitViolations", getCounterValue("security.rate_limit.exceeded"),
            "suspiciousActivities", getCounterValue("security.suspicious_activity"),
            "topAttackedEndpoints", getTopAttackedEndpoints(),
            "timestamp", Instant.now()
        );

        return ResponseEntity.ok(attackMetrics);
    }

    /**
     * Get session management metrics.
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessionMetrics() {
        Map<String, Object> sessionMetrics = Map.of(
            "activeSessions", getCounterValue("security.sessions.active"),
            "averageSessionDuration", metricsCollector.getAverageSessionDuration(),
            "sessionCreationTime", getTimerMean("security.session.creation.duration"),
            "concurrentSessionViolations", getCounterValue("security.concurrent_sessions.exceeded"),
            "timestamp", Instant.now()
        );

        return ResponseEntity.ok(sessionMetrics);
    }

    /**
     * Get security alerts (high-priority events).
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getSecurityAlerts() {
        Map<String, Object> alerts = Map.of(
            "criticalEvents", getCriticalEventsLast24Hours(),
            "highRiskUsers", getHighRiskUsers(),
            "suspiciousIpAddresses", getSuspiciousIpAddresses(),
            "accountLockouts", getRecentAccountLockouts(),
            "timestamp", Instant.now()
        );

        return ResponseEntity.ok(alerts);
    }

    /**
     * Get real-time security status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSecurityStatus() {
        boolean isUnderAttack = isSystemUnderAttack();
        String securityLevel = determineSecurityLevel();

        Map<String, Object> status = Map.of(
            "securityLevel", securityLevel,
            "isUnderAttack", isUnderAttack,
            "activeThreats", getActiveThreats(),
            "systemHealth", "HEALTHY", // This would integrate with health checks
            "lastUpdate", Instant.now()
        );

        return ResponseEntity.ok(status);
    }

    // Helper methods
    private double getCounterValue(String counterName) {
        Counter counter = meterRegistry.find(counterName).counter();
        return counter != null ? counter.count() : 0.0;
    }

    private double getTimerMean(String timerName) {
        return meterRegistry.find(timerName).timer()
            .map(timer -> timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
            .orElse(0.0);
    }

    private Map<String, Double> getAuthFailuresByReason() {
        // This would aggregate failures by reason from the metrics
        return Map.of(
            "invalid_credentials", getCounterValue("security.authentication.failures.invalid_credentials"),
            "account_locked", getCounterValue("security.authentication.failures.account_locked"),
            "account_disabled", getCounterValue("security.authentication.failures.account_disabled"),
            "rate_limited", getCounterValue("security.authentication.failures.rate_limited")
        );
    }

    private double calculateAuthSuccessRate() {
        double successes = getCounterValue("security.authentication.successes");
        double failures = getCounterValue("security.authentication.failures");
        double total = successes + failures;
        return total > 0 ? (successes / total) * 100 : 100.0;
    }

    private Map<String, Long> getTopAttackedEndpoints() {
        // This would query the metrics for most attacked endpoints
        return Map.of(
            "/api/v1/auth/login", 150L,
            "/api/v1/auth/register", 85L,
            "/api/v1/payments", 42L
        );
    }

    private Map<String, Object> getCriticalEventsLast24Hours() {
        Instant yesterday = Instant.now().minus(24, ChronoUnit.HOURS);
        // This would query the audit log for critical events
        return Map.of(
            "count", 3,
            "events", java.util.List.of(
                Map.of("type", "PRIVILEGE_ESCALATION", "timestamp", yesterday.plus(2, ChronoUnit.HOURS)),
                Map.of("type", "SUSPICIOUS_LOGIN_PATTERN", "timestamp", yesterday.plus(8, ChronoUnit.HOURS)),
                Map.of("type", "MULTIPLE_FAILED_LOGINS", "timestamp", yesterday.plus(15, ChronoUnit.HOURS))
            )
        );
    }

    private java.util.List<String> getHighRiskUsers() {
        // This would identify users with suspicious activity patterns
        return java.util.List.of("user123", "admin456");
    }

    private java.util.List<String> getSuspiciousIpAddresses() {
        // This would identify IP addresses with high attack volumes
        return java.util.List.of("192.168.1.100", "10.0.0.50");
    }

    private Map<String, Object> getRecentAccountLockouts() {
        // This would query recent account lockouts
        return Map.of(
            "count", 5,
            "lastHour", 2
        );
    }

    private boolean isSystemUnderAttack() {
        // Logic to determine if system is under active attack
        long recentAttacks = metricsCollector.getRateLimitViolationsLastHour();
        return recentAttacks > 100; // Threshold for attack detection
    }

    private String determineSecurityLevel() {
        if (isSystemUnderAttack()) {
            return "HIGH";
        }

        long failures = metricsCollector.getFailedLoginAttemptsLast24Hours();
        if (failures > 50) {
            return "ELEVATED";
        }

        return "NORMAL";
    }

    private java.util.List<String> getActiveThreats() {
        java.util.List<String> threats = new java.util.ArrayList<>();

        if (getCounterValue("security.xss.attempts") > 10) {
            threats.add("XSS_ATTACKS");
        }
        if (getCounterValue("security.sql_injection.attempts") > 5) {
            threats.add("SQL_INJECTION");
        }
        if (metricsCollector.getRateLimitViolationsLastHour() > 50) {
            threats.add("BRUTE_FORCE");
        }

        return threats;
    }
}