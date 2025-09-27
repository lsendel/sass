package com.platform.shared.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced alerting service for performance degradation and security incidents.
 * Implements intelligent alerting with rate limiting and escalation.
 */
@Service
public class AlertingService {

    private static final Logger logger = LoggerFactory.getLogger(AlertingService.class);

    @Value("${app.alerts.enabled:true}")
    private boolean alertsEnabled;

    @Value("${app.alerts.rate-limit.window-minutes:5}")
    private int rateLimitWindowMinutes;

    @Value("${app.alerts.rate-limit.max-alerts:10}")
    private int maxAlertsPerWindow;

    // Alert tracking for rate limiting
    private final Map<String, AtomicLong> alertCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> alertWindows = new ConcurrentHashMap<>();

    // Alert severity levels
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL, EMERGENCY
    }

    // Alert categories
    public enum AlertCategory {
        PERFORMANCE, SECURITY, SYSTEM, DATABASE, CACHE, AUDIT
    }

    /**
     * Send performance alert with intelligent rate limiting
     */
    @Async
    public void sendPerformanceAlert(
            AlertSeverity severity,
            String title,
            String message,
            Map<String, Object> metrics) {

        if (!alertsEnabled) return;

        String alertKey = generateAlertKey(AlertCategory.PERFORMANCE, title);

        if (shouldSendAlert(alertKey)) {
            PerformanceAlert alert = new PerformanceAlert(
                    severity, title, message, metrics, Instant.now()
            );

            processAlert(alert);
            incrementAlertCount(alertKey);

            logger.info("Performance alert sent: {} - {}", severity, title);
        } else {
            logger.debug("Performance alert rate limited: {}", title);
        }
    }

    /**
     * Send security alert with immediate escalation for critical issues
     */
    @Async
    public void sendSecurityAlert(
            AlertSeverity severity,
            String title,
            String message,
            String organizationId,
            Map<String, Object> securityContext) {

        if (!alertsEnabled) return;

        // Security alerts bypass rate limiting for critical/emergency levels
        String alertKey = generateAlertKey(AlertCategory.SECURITY, title);
        boolean bypassRateLimit = severity == AlertSeverity.CRITICAL || severity == AlertSeverity.EMERGENCY;

        if (bypassRateLimit || shouldSendAlert(alertKey)) {
            SecurityAlert alert = new SecurityAlert(
                    severity, title, message, organizationId, securityContext, Instant.now()
            );

            processSecurityAlert(alert);
            if (!bypassRateLimit) {
                incrementAlertCount(alertKey);
            }

            logger.warn("Security alert sent: {} - {} (org: {})", severity, title, organizationId);

            // Immediate escalation for emergency alerts
            if (severity == AlertSeverity.EMERGENCY) {
                escalateAlert(alert);
            }
        }
    }

    /**
     * Send system health alert
     */
    @Async
    public void sendSystemAlert(
            AlertSeverity severity,
            String title,
            String message,
            Map<String, Object> systemMetrics) {

        if (!alertsEnabled) return;

        String alertKey = generateAlertKey(AlertCategory.SYSTEM, title);

        if (shouldSendAlert(alertKey)) {
            SystemAlert alert = new SystemAlert(
                    severity, title, message, systemMetrics, Instant.now()
            );

            processAlert(alert);
            incrementAlertCount(alertKey);

            logger.info("System alert sent: {} - {}", severity, title);
        }
    }

    /**
     * Send database performance alert
     */
    @Async
    public void sendDatabaseAlert(
            AlertSeverity severity,
            String title,
            String message,
            String queryType,
            long executionTimeMs) {

        if (!alertsEnabled) return;

        Map<String, Object> dbMetrics = Map.of(
                "query_type", queryType,
                "execution_time_ms", executionTimeMs,
                "threshold_exceeded", executionTimeMs > 1000
        );

        String alertKey = generateAlertKey(AlertCategory.DATABASE, queryType);

        if (shouldSendAlert(alertKey)) {
            DatabaseAlert alert = new DatabaseAlert(
                    severity, title, message, queryType, executionTimeMs, Instant.now()
            );

            processAlert(alert);
            incrementAlertCount(alertKey);

            logger.warn("Database alert sent: {} - {} ({}ms)", severity, title, executionTimeMs);
        }
    }

    /**
     * Send cache performance alert
     */
    @Async
    public void sendCacheAlert(
            AlertSeverity severity,
            String cacheName,
            double hitRatio,
            String issue) {

        if (!alertsEnabled) return;

        String title = String.format("Cache Performance Issue: %s", cacheName);
        String message = String.format("Cache '%s' %s (hit ratio: %.2f%%)", cacheName, issue, hitRatio * 100);

        Map<String, Object> cacheMetrics = Map.of(
                "cache_name", cacheName,
                "hit_ratio", hitRatio,
                "issue", issue
        );

        String alertKey = generateAlertKey(AlertCategory.CACHE, cacheName);

        if (shouldSendAlert(alertKey)) {
            CacheAlert alert = new CacheAlert(
                    severity, title, message, cacheName, hitRatio, Instant.now()
            );

            processAlert(alert);
            incrementAlertCount(alertKey);

            logger.info("Cache alert sent: {} - {}", severity, title);
        }
    }

    /**
     * Process general alert through configured channels
     */
    private void processAlert(Alert alert) {
        // Send to configured alert channels
        sendToSlack(alert);
        sendToEmail(alert);
        logToAuditSystem(alert);

        // Store in alert history
        storeAlertHistory(alert);
    }

    /**
     * Process security alert with enhanced handling
     */
    private void processSecurityAlert(SecurityAlert alert) {
        // All security alert processing
        processAlert(alert);

        // Additional security-specific handling
        if (alert.getSeverity() == AlertSeverity.CRITICAL || alert.getSeverity() == AlertSeverity.EMERGENCY) {
            notifySecurityTeam(alert);
            createSecurityIncident(alert);
        }
    }

    /**
     * Escalate critical alerts to emergency channels
     */
    private void escalateAlert(Alert alert) {
        logger.error("ESCALATING ALERT: {} - {}", alert.getSeverity(), alert.getTitle());

        // Send to emergency channels
        sendToEmergencySlack(alert);
        sendToEmergencyEmail(alert);
        sendToSMS(alert);

        // Create high-priority incident
        createEmergencyIncident(alert);
    }

    /**
     * Check if alert should be sent based on rate limiting
     */
    private boolean shouldSendAlert(String alertKey) {
        Instant now = Instant.now();
        Instant windowStart = alertWindows.get(alertKey);

        // Reset window if expired
        if (windowStart == null || now.isAfter(windowStart.plusSeconds(rateLimitWindowMinutes * 60L))) {
            alertWindows.put(alertKey, now);
            alertCounts.put(alertKey, new AtomicLong(0));
            return true;
        }

        // Check if within rate limit
        AtomicLong count = alertCounts.get(alertKey);
        return count == null || count.get() < maxAlertsPerWindow;
    }

    /**
     * Increment alert count for rate limiting
     */
    private void incrementAlertCount(String alertKey) {
        alertCounts.computeIfAbsent(alertKey, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Generate unique alert key for rate limiting
     */
    private String generateAlertKey(AlertCategory category, String identifier) {
        return String.format("%s:%s", category.name(), identifier);
    }

    // Alert channel implementations

    private void sendToSlack(Alert alert) {
        try {
            // Implement Slack webhook integration
            logger.debug("Sending alert to Slack: {}", alert.getTitle());
            // SlackWebhookClient.send(alert.toSlackMessage());
        } catch (Exception e) {
            logger.error("Failed to send alert to Slack", e);
        }
    }

    private void sendToEmail(Alert alert) {
        try {
            // Implement email notification
            logger.debug("Sending alert email: {}", alert.getTitle());
            // EmailService.send(alert.toEmailMessage());
        } catch (Exception e) {
            logger.error("Failed to send alert email", e);
        }
    }

    private void sendToEmergencySlack(Alert alert) {
        try {
            // Send to emergency Slack channel
            logger.error("Sending EMERGENCY alert to Slack: {}", alert.getTitle());
            // EmergencySlackClient.send(alert.toUrgentSlackMessage());
        } catch (Exception e) {
            logger.error("Failed to send emergency alert to Slack", e);
        }
    }

    private void sendToEmergencyEmail(Alert alert) {
        try {
            // Send to emergency email list
            logger.error("Sending EMERGENCY alert email: {}", alert.getTitle());
            // EmergencyEmailService.send(alert.toUrgentEmailMessage());
        } catch (Exception e) {
            logger.error("Failed to send emergency alert email", e);
        }
    }

    private void sendToSMS(Alert alert) {
        try {
            // Send SMS for emergency alerts
            logger.error("Sending EMERGENCY SMS alert: {}", alert.getTitle());
            // SMSService.send(alert.toSMSMessage());
        } catch (Exception e) {
            logger.error("Failed to send SMS alert", e);
        }
    }

    private void logToAuditSystem(Alert alert) {
        try {
            // Log alert to audit system
            logger.info("Logging alert to audit system: {}", alert.getTitle());
            // AuditService.logAlert(alert);
        } catch (Exception e) {
            logger.error("Failed to log alert to audit system", e);
        }
    }

    private void storeAlertHistory(Alert alert) {
        try {
            // Store alert in database for history
            logger.debug("Storing alert in history: {}", alert.getTitle());
            // AlertHistoryRepository.save(alert);
        } catch (Exception e) {
            logger.error("Failed to store alert history", e);
        }
    }

    private void notifySecurityTeam(SecurityAlert alert) {
        try {
            // Notify security team via dedicated channels
            logger.warn("Notifying security team: {}", alert.getTitle());
            // SecurityTeamNotifier.send(alert);
        } catch (Exception e) {
            logger.error("Failed to notify security team", e);
        }
    }

    private void createSecurityIncident(SecurityAlert alert) {
        try {
            // Create security incident in incident management system
            logger.warn("Creating security incident: {}", alert.getTitle());
            // IncidentManagementService.createSecurityIncident(alert);
        } catch (Exception e) {
            logger.error("Failed to create security incident", e);
        }
    }

    private void createEmergencyIncident(Alert alert) {
        try {
            // Create emergency incident
            logger.error("Creating EMERGENCY incident: {}", alert.getTitle());
            // IncidentManagementService.createEmergencyIncident(alert);
        } catch (Exception e) {
            logger.error("Failed to create emergency incident", e);
        }
    }

    // Alert data classes

    public static abstract class Alert {
        protected final AlertSeverity severity;
        protected final String title;
        protected final String message;
        protected final Instant timestamp;

        public Alert(AlertSeverity severity, String title, String message, Instant timestamp) {
            this.severity = severity;
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
        }

        // Getters
        public AlertSeverity getSeverity() { return severity; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class PerformanceAlert extends Alert {
        private final Map<String, Object> metrics;

        public PerformanceAlert(AlertSeverity severity, String title, String message,
                                Map<String, Object> metrics, Instant timestamp) {
            super(severity, title, message, timestamp);
            this.metrics = metrics;
        }

        public Map<String, Object> getMetrics() { return metrics; }
    }

    public static class SecurityAlert extends Alert {
        private final String organizationId;
        private final Map<String, Object> securityContext;

        public SecurityAlert(AlertSeverity severity, String title, String message,
                            String organizationId, Map<String, Object> securityContext, Instant timestamp) {
            super(severity, title, message, timestamp);
            this.organizationId = organizationId;
            this.securityContext = securityContext;
        }

        public String getOrganizationId() { return organizationId; }
        public Map<String, Object> getSecurityContext() { return securityContext; }
    }

    public static class SystemAlert extends Alert {
        private final Map<String, Object> systemMetrics;

        public SystemAlert(AlertSeverity severity, String title, String message,
                          Map<String, Object> systemMetrics, Instant timestamp) {
            super(severity, title, message, timestamp);
            this.systemMetrics = systemMetrics;
        }

        public Map<String, Object> getSystemMetrics() { return systemMetrics; }
    }

    public static class DatabaseAlert extends Alert {
        private final String queryType;
        private final long executionTimeMs;

        public DatabaseAlert(AlertSeverity severity, String title, String message,
                            String queryType, long executionTimeMs, Instant timestamp) {
            super(severity, title, message, timestamp);
            this.queryType = queryType;
            this.executionTimeMs = executionTimeMs;
        }

        public String getQueryType() { return queryType; }
        public long getExecutionTimeMs() { return executionTimeMs; }
    }

    public static class CacheAlert extends Alert {
        private final String cacheName;
        private final double hitRatio;

        public CacheAlert(AlertSeverity severity, String title, String message,
                         String cacheName, double hitRatio, Instant timestamp) {
            super(severity, title, message, timestamp);
            this.cacheName = cacheName;
            this.hitRatio = hitRatio;
        }

        public String getCacheName() { return cacheName; }
        public double getHitRatio() { return hitRatio; }
    }
}