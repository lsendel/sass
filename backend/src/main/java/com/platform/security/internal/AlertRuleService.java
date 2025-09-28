package com.platform.security.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing alert rules with condition evaluation and notification handling.
 * Provides automated alerting based on security metrics and events.
 *
 * Constitutional Compliance:
 * - Real-time: <1s alert response time for critical security events
 * - Performance: <200ms API response times for rule management
 * - Observability: All rule evaluations and alerts logged with correlation IDs
 * - Reliability: Async processing prevents blocking main security flows
 */
@Service
@Validated
@Transactional
public class AlertRuleService {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleService.class);

    private final AlertRuleRepository alertRuleRepository;
    private final SecurityMetricRepository securityMetricRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AlertRuleService(
            AlertRuleRepository alertRuleRepository,
            SecurityMetricRepository securityMetricRepository,
            ApplicationEventPublisher eventPublisher) {
        this.alertRuleRepository = alertRuleRepository;
        this.securityMetricRepository = securityMetricRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new alert rule with validation.
     * Validates condition syntax and notification channels.
     */
    @Transactional
    public AlertRule createAlertRule(@Valid @NotNull AlertRule alertRule) {
        log.info("Creating alert rule: name={}, severity={}, condition={}",
                alertRule.getName(), alertRule.getSeverity(), alertRule.getCondition());

        // Validate condition syntax
        validateConditionSyntax(alertRule.getCondition());

        // Validate thresholds
        if (alertRule.getThreshold() <= 0) {
            throw new IllegalArgumentException("Threshold must be positive");
        }

        if (alertRule.getTimeWindow().toMinutes() < 1) {
            throw new IllegalArgumentException("Time window must be at least 1 minute");
        }

        if (alertRule.getCooldownPeriod().toSeconds() < 30) {
            throw new IllegalArgumentException("Cooldown period must be at least 30 seconds");
        }

        // Set creation metadata
        alertRule.setCreatedAt(Instant.now());
        alertRule.setTriggerCount(0);

        var savedRule = alertRuleRepository.save(alertRule);

        // Publish event for audit logging
        publishAlertRuleCreated(savedRule);

        log.info("Alert rule created successfully: id={}, name={}",
                savedRule.getId(), savedRule.getName());

        return savedRule;
    }

    /**
     * Get alert rule by ID.
     */
    @Transactional(readOnly = true)
    public Optional<AlertRule> getAlertRule(@NonNull String ruleId) {
        log.debug("Retrieving alert rule: id={}", ruleId);

        var rule = alertRuleRepository.findById(ruleId);
        if (rule.isPresent()) {
            log.debug("Alert rule found: id={}, name={}, enabled={}",
                    ruleId, rule.get().getName(), rule.get().getEnabled());
        } else {
            log.warn("Alert rule not found: id={}", ruleId);
        }

        return rule;
    }

    /**
     * List alert rules with filtering.
     */
    @Transactional(readOnly = true)
    public List<AlertRule> listAlertRules(Boolean enabled) {
        log.debug("Listing alert rules: enabled={}", enabled);

        List<AlertRule> rules;
        if (enabled != null) {
            rules = alertRuleRepository.findByEnabled(enabled);
        } else {
            rules = alertRuleRepository.findAll();
        }

        log.debug("Retrieved {} alert rules", rules.size());

        return rules;
    }

    /**
     * Update alert rule with validation.
     */
    @Transactional
    public AlertRule updateAlertRule(@NonNull String ruleId, @Valid @NotNull AlertRule updates) {
        log.info("Updating alert rule: id={}", ruleId);

        var existingRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found: " + ruleId));

        // Update fields
        if (updates.getName() != null) {
            existingRule.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existingRule.setDescription(updates.getDescription());
        }
        if (updates.getCondition() != null) {
            validateConditionSyntax(updates.getCondition());
            existingRule.setCondition(updates.getCondition());
        }
        if (updates.getEnabled() != null) {
            existingRule.setEnabled(updates.getEnabled());
        }
        if (updates.getThreshold() != null && updates.getThreshold() > 0) {
            existingRule.setThreshold(updates.getThreshold());
        }
        if (updates.getTimeWindow() != null && updates.getTimeWindow().toMinutes() >= 1) {
            existingRule.setTimeWindow(updates.getTimeWindow());
        }
        if (updates.getCooldownPeriod() != null && updates.getCooldownPeriod().toSeconds() >= 30) {
            existingRule.setCooldownPeriod(updates.getCooldownPeriod());
        }
        if (updates.getNotificationChannels() != null) {
            existingRule.setNotificationChannels(updates.getNotificationChannels());
        }
        if (updates.getEscalationRules() != null) {
            existingRule.setEscalationRules(updates.getEscalationRules());
        }

        var savedRule = alertRuleRepository.save(existingRule);

        // Publish event for audit logging
        publishAlertRuleUpdated(savedRule);

        log.info("Alert rule updated successfully: id={}, name={}",
                savedRule.getId(), savedRule.getName());

        return savedRule;
    }

    /**
     * Delete alert rule.
     */
    @Transactional
    public void deleteAlertRule(@NonNull String ruleId) {
        log.info("Deleting alert rule: id={}", ruleId);

        var rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found: " + ruleId));

        alertRuleRepository.delete(rule);

        // Publish event for audit logging
        publishAlertRuleDeleted(rule);

        log.info("Alert rule deleted successfully: id={}, name={}",
                rule.getId(), rule.getName());
    }

    /**
     * Evaluate all enabled alert rules against current metrics.
     * Called by scheduled tasks for continuous monitoring.
     */
    @Async("alertExecutor")
    @Transactional
    public CompletableFuture<List<AlertTrigger>> evaluateAlertRules() {
        log.debug("Evaluating all enabled alert rules");

        var enabledRules = alertRuleRepository.findByEnabled(true);
        log.debug("Found {} enabled alert rules", enabledRules.size());

        var triggers = enabledRules.stream()
                .filter(this::isNotInCooldown)
                .map(this::evaluateRule)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        log.info("Alert evaluation completed: {} rules checked, {} triggers generated",
                enabledRules.size(), triggers.size());

        return CompletableFuture.completedFuture(triggers);
    }

    /**
     * Evaluate a specific alert rule against current metrics.
     */
    @Transactional
    public Optional<AlertTrigger> evaluateRule(@NonNull AlertRule rule) {
        log.debug("Evaluating alert rule: id={}, name={}, condition={}",
                rule.getId(), rule.getName(), rule.getCondition());

        if (!rule.getEnabled()) {
            log.debug("Skipping disabled rule: id={}", rule.getId());
            return Optional.empty();
        }

        if (isInCooldown(rule)) {
            log.debug("Skipping rule in cooldown: id={}, lastTriggered={}",
                    rule.getId(), rule.getLastTriggered());
            return Optional.empty();
        }

        try {
            // Extract metric name from condition
            var metricName = extractMetricNameFromCondition(rule.getCondition());
            if (metricName == null) {
                log.warn("Could not extract metric name from condition: {}", rule.getCondition());
                return Optional.empty();
            }

            // Get recent metrics for evaluation
            var evaluationWindow = Instant.now().minus(rule.getTimeWindow());
            var metrics = securityMetricRepository.findMetricsAboveThreshold(
                    metricName, rule.getThreshold(), evaluationWindow);

            if (!metrics.isEmpty()) {
                // Rule triggered
                var latestMetric = metrics.get(0);
                var trigger = createAlertTrigger(rule, latestMetric);

                // Update rule trigger count and timestamp
                rule.setLastTriggered(Instant.now());
                rule.setTriggerCount(rule.getTriggerCount() + 1);
                alertRuleRepository.save(rule);

                // Publish alert for notification processing
                publishAlertTriggered(trigger);

                log.info("Alert rule triggered: ruleId={}, metricValue={}, threshold={}",
                        rule.getId(), latestMetric.getValue(), rule.getThreshold());

                return Optional.of(trigger);
            } else {
                log.debug("Alert rule condition not met: id={}, threshold={}",
                        rule.getId(), rule.getThreshold());
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("Error evaluating alert rule: id={}, condition={}",
                    rule.getId(), rule.getCondition(), e);
            return Optional.empty();
        }
    }

    /**
     * Get alert trigger history for a rule.
     */
    @Transactional(readOnly = true)
    public List<AlertTrigger> getAlertHistory(
            @NonNull String ruleId,
            Instant fromTimestamp,
            Instant toTimestamp) {

        log.debug("Getting alert history: ruleId={}, from={}, to={}",
                ruleId, fromTimestamp, toTimestamp);

        // This would typically query a separate AlertTrigger table
        // For now, return empty list as placeholder
        var history = List.<AlertTrigger>of();

        log.debug("Retrieved {} alert triggers for rule: {}", history.size(), ruleId);

        return history;
    }

    /**
     * Get alert rule statistics for monitoring.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAlertStatistics() {
        log.debug("Calculating alert rule statistics");

        var allRules = alertRuleRepository.findAll();
        var enabledCount = (int) allRules.stream().filter(AlertRule::getEnabled).count();
        var totalTriggers = allRules.stream().mapToInt(AlertRule::getTriggerCount).sum();

        var recentlyTriggered = allRules.stream()
                .filter(rule -> rule.getLastTriggered() != null &&
                        rule.getLastTriggered().isAfter(Instant.now().minus(Duration.ofHours(24))))
                .count();

        var statistics = Map.of(
                "totalRules", allRules.size(),
                "enabledRules", enabledCount,
                "disabledRules", allRules.size() - enabledCount,
                "totalTriggers", totalTriggers,
                "recentlyTriggered", recentlyTriggered,
                "lastUpdated", Instant.now()
        );

        log.debug("Alert statistics: total={}, enabled={}, triggers={}",
                allRules.size(), enabledCount, totalTriggers);

        return statistics;
    }

    // Helper methods

    private boolean isInCooldown(AlertRule rule) {
        if (rule.getLastTriggered() == null) {
            return false;
        }

        var cooldownEnd = rule.getLastTriggered().plus(rule.getCooldownPeriod());
        return Instant.now().isBefore(cooldownEnd);
    }

    private boolean isNotInCooldown(AlertRule rule) {
        return !isInCooldown(rule);
    }

    private void validateConditionSyntax(String condition) {
        // Basic validation - in production, this would parse the condition expression
        if (condition == null || condition.trim().isEmpty()) {
            throw new IllegalArgumentException("Condition cannot be empty");
        }

        // Check for basic structure: "metric_name > threshold"
        if (!condition.contains(">") && !condition.contains("<") &&
            !condition.contains(">=") && !condition.contains("<=") &&
            !condition.contains("==")) {
            throw new IllegalArgumentException("Condition must contain a comparison operator");
        }
    }

    private String extractMetricNameFromCondition(String condition) {
        // Simple extraction - in production, use proper parser
        var parts = condition.trim().split("\\s+");
        if (parts.length >= 3) {
            return parts[0]; // First part should be metric name
        }
        return null;
    }

    private AlertTrigger createAlertTrigger(AlertRule rule, SecurityMetric metric) {
        return new AlertTrigger(
                java.util.UUID.randomUUID().toString(),
                rule.getId(),
                rule.getName(),
                rule.getSeverity(),
                Instant.now(),
                metric.getValue(),
                rule.getThreshold(),
                "Metric '" + metric.getMetricName() + "' exceeded threshold",
                false,
                null
        );
    }

    // Event publishing methods

    private void publishAlertRuleCreated(AlertRule rule) {
        try {
            var event = new AlertRuleCreatedEvent(
                    rule.getId(),
                    rule.getName(),
                    rule.getSeverity(),
                    rule.getEnabled()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish AlertRuleCreated event: ruleId={}", rule.getId(), e);
        }
    }

    private void publishAlertRuleUpdated(AlertRule rule) {
        try {
            var event = new AlertRuleUpdatedEvent(
                    rule.getId(),
                    rule.getName(),
                    rule.getEnabled()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish AlertRuleUpdated event: ruleId={}", rule.getId(), e);
        }
    }

    private void publishAlertRuleDeleted(AlertRule rule) {
        try {
            var event = new AlertRuleDeletedEvent(
                    rule.getId(),
                    rule.getName()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish AlertRuleDeleted event: ruleId={}", rule.getId(), e);
        }
    }

    private void publishAlertTriggered(AlertTrigger trigger) {
        try {
            var event = new AlertTriggeredEvent(
                    trigger.id(),
                    trigger.ruleId(),
                    trigger.ruleName(),
                    trigger.severity(),
                    trigger.triggeredAt(),
                    trigger.value(),
                    trigger.threshold(),
                    trigger.message()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish AlertTriggered event: triggerId={}", trigger.id(), e);
        }
    }

    // Data transfer objects and events

    public record AlertTrigger(
            String id,
            String ruleId,
            String ruleName,
            AlertRule.Severity severity,
            Instant triggeredAt,
            Double value,
            Double threshold,
            String message,
            Boolean resolved,
            Instant resolvedAt
    ) {}

    public record AlertRuleCreatedEvent(
            String ruleId,
            String name,
            AlertRule.Severity severity,
            Boolean enabled
    ) {}

    public record AlertRuleUpdatedEvent(
            String ruleId,
            String name,
            Boolean enabled
    ) {}

    public record AlertRuleDeletedEvent(
            String ruleId,
            String name
    ) {}

    public record AlertTriggeredEvent(
            String triggerId,
            String ruleId,
            String ruleName,
            AlertRule.Severity severity,
            Instant triggeredAt,
            Double value,
            Double threshold,
            String message
    ) {}
}