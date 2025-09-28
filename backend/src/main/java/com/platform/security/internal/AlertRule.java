package com.platform.security.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * AlertRule entity for automated security alerting configuration.
 *
 * This entity represents configurable rules that monitor security metrics
 * and trigger notifications when thresholds are exceeded.
 *
 * Key features:
 * - Flexible condition expressions for complex alerting logic
 * - Configurable notification channels and escalation rules
 * - Cooldown periods to prevent alert spam
 * - Trigger tracking for analytics and tuning
 * - Time window-based evaluation
 * - Severity-based classification
 */
@Entity
@Table(name = "alert_rules",
       indexes = {
           @Index(name = "idx_alert_rules_enabled", columnList = "enabled"),
           @Index(name = "idx_alert_rules_severity", columnList = "severity"),
           @Index(name = "idx_alert_rules_last_triggered", columnList = "lastTriggered")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_alert_rule_name", columnNames = {"name"})
       })
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(nullable = false, columnDefinition = "TEXT")
    private String condition;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @NotNull
    @Column(nullable = false)
    private Boolean enabled = true;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 19, scale = 4)
    private Double threshold;

    @NotNull
    @Column(name = "time_window", nullable = false)
    private Duration timeWindow;

    @NotNull
    @Column(name = "cooldown_period", nullable = false)
    private Duration cooldownPeriod = Duration.ofSeconds(30);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alert_rule_notification_channels",
                     joinColumns = @JoinColumn(name = "alert_rule_id"))
    @Column(name = "channel")
    private List<String> notificationChannels = new ArrayList<>();

    @Column(name = "escalation_rules", columnDefinition = "jsonb")
    private Map<String, Object> escalationRules = new HashMap<>();

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "last_triggered")
    private Instant lastTriggered;

    @NotNull
    @Min(0)
    @Column(name = "trigger_count", nullable = false)
    private Integer triggerCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Alert severity levels
     */
    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    // Constructors
    protected AlertRule() {
        // JPA constructor
    }

    public AlertRule(@NotNull String name,
                    @NotNull String condition,
                    @NotNull Severity severity,
                    @NotNull Double threshold,
                    @NotNull Duration timeWindow,
                    @NotNull String createdBy) {
        this.name = name;
        this.condition = condition;
        this.severity = severity;
        this.threshold = threshold;
        this.timeWindow = timeWindow;
        this.createdBy = createdBy;
        this.enabled = true;
        this.cooldownPeriod = Duration.ofSeconds(30);
        this.triggerCount = 0;
    }

    public AlertRule(@NotNull String name,
                    String description,
                    @NotNull String condition,
                    @NotNull Severity severity,
                    @NotNull Double threshold,
                    @NotNull Duration timeWindow,
                    @NotNull Duration cooldownPeriod,
                    @NotNull List<String> notificationChannels,
                    @NotNull String createdBy) {
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.severity = severity;
        this.threshold = threshold;
        this.timeWindow = timeWindow;
        this.cooldownPeriod = cooldownPeriod;
        this.notificationChannels = new ArrayList<>(notificationChannels);
        this.createdBy = createdBy;
        this.enabled = true;
        this.triggerCount = 0;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        if (threshold != null && threshold > 0) {
            this.threshold = threshold;
        } else {
            throw new IllegalArgumentException("Threshold must be positive");
        }
    }

    public Duration getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(Duration timeWindow) {
        if (timeWindow != null && timeWindow.toMinutes() >= 1) {
            this.timeWindow = timeWindow;
        } else {
            throw new IllegalArgumentException("Time window must be at least 1 minute");
        }
    }

    public Duration getCooldownPeriod() {
        return cooldownPeriod;
    }

    public void setCooldownPeriod(Duration cooldownPeriod) {
        if (cooldownPeriod != null && cooldownPeriod.getSeconds() >= 30) {
            this.cooldownPeriod = cooldownPeriod;
        } else {
            throw new IllegalArgumentException("Cooldown period must be at least 30 seconds");
        }
    }

    public List<String> getNotificationChannels() {
        return notificationChannels;
    }

    public void setNotificationChannels(List<String> notificationChannels) {
        this.notificationChannels = notificationChannels != null ?
            new ArrayList<>(notificationChannels) : new ArrayList<>();
    }

    public Map<String, Object> getEscalationRules() {
        return escalationRules;
    }

    public void setEscalationRules(Map<String, Object> escalationRules) {
        this.escalationRules = escalationRules != null ?
            new HashMap<>(escalationRules) : new HashMap<>();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastTriggered() {
        return lastTriggered;
    }

    public void setLastTriggered(Instant lastTriggered) {
        this.lastTriggered = lastTriggered;
    }

    public Integer getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(Integer triggerCount) {
        this.triggerCount = triggerCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Business Logic Methods

    /**
     * Check if this alert rule is currently in cooldown period
     *
     * @return true if in cooldown
     */
    public boolean isInCooldown() {
        if (lastTriggered == null) {
            return false;
        }

        Instant cooldownEnd = lastTriggered.plus(cooldownPeriod);
        return Instant.now().isBefore(cooldownEnd);
    }

    /**
     * Record that this alert rule has been triggered
     *
     * @return the new trigger count
     */
    public int recordTrigger() {
        this.lastTriggered = Instant.now();
        this.triggerCount++;
        return this.triggerCount;
    }

    /**
     * Check if this rule can be triggered (enabled and not in cooldown)
     *
     * @return true if can trigger
     */
    public boolean canTrigger() {
        return enabled && !isInCooldown();
    }

    /**
     * Check if this is a critical alert rule
     *
     * @return true if severity is CRITICAL
     */
    public boolean isCritical() {
        return severity == Severity.CRITICAL;
    }

    /**
     * Check if this rule has been triggered recently (within time window)
     *
     * @return true if triggered recently
     */
    public boolean wasTriggeredRecently() {
        if (lastTriggered == null) {
            return false;
        }

        Instant recentThreshold = Instant.now().minus(timeWindow);
        return lastTriggered.isAfter(recentThreshold);
    }

    /**
     * Add a notification channel
     *
     * @param channel The channel to add
     */
    public void addNotificationChannel(String channel) {
        if (channel != null && !channel.trim().isEmpty() &&
            !notificationChannels.contains(channel.trim())) {
            notificationChannels.add(channel.trim());
        }
    }

    /**
     * Remove a notification channel
     *
     * @param channel The channel to remove
     */
    public void removeNotificationChannel(String channel) {
        if (channel != null) {
            notificationChannels.remove(channel.trim());
        }
    }

    /**
     * Check if this rule has a specific notification channel
     *
     * @param channel The channel to check
     * @return true if channel exists
     */
    public boolean hasNotificationChannel(String channel) {
        return channel != null && notificationChannels.contains(channel.trim());
    }

    /**
     * Set escalation rule for a specific level
     *
     * @param level The escalation level (e.g., "level1", "level2")
     * @param action The action to take (e.g., "email", "pagerduty")
     */
    public void setEscalationRule(String level, String action) {
        if (level != null && action != null) {
            escalationRules.put(level, action);
        }
    }

    /**
     * Get escalation action for a specific level
     *
     * @param level The escalation level
     * @return the action or null if not found
     */
    public String getEscalationAction(String level) {
        Object action = escalationRules.get(level);
        return action instanceof String ? (String) action : null;
    }

    /**
     * Enable this alert rule
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * Disable this alert rule
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * Reset trigger statistics
     */
    public void resetTriggerStats() {
        this.triggerCount = 0;
        this.lastTriggered = null;
    }

    /**
     * Get the cooldown remaining time in seconds
     *
     * @return seconds remaining in cooldown, 0 if not in cooldown
     */
    public long getCooldownRemainingSeconds() {
        if (!isInCooldown()) {
            return 0;
        }

        Instant cooldownEnd = lastTriggered.plus(cooldownPeriod);
        return Math.max(0, cooldownEnd.getEpochSecond() - Instant.now().getEpochSecond());
    }

    /**
     * Calculate trigger frequency (triggers per day)
     *
     * @return triggers per day based on recent activity
     */
    public double getTriggerFrequency() {
        if (triggerCount == 0 || createdAt == null) {
            return 0.0;
        }

        long daysSinceCreation = Duration.between(createdAt, Instant.now()).toDays();
        if (daysSinceCreation == 0) {
            daysSinceCreation = 1; // Avoid division by zero
        }

        return (double) triggerCount / daysSinceCreation;
    }

    /**
     * Check if user can modify this alert rule
     *
     * @param userId The user ID
     * @param userRoles The user roles
     * @return true if user can modify
     */
    public boolean canModify(String userId, java.util.Set<String> userRoles) {
        // Creator can always modify
        if (Objects.equals(createdBy, userId)) {
            return true;
        }

        // Admin roles can modify any alert rule
        return userRoles.contains("ROLE_SECURITY_ADMIN") || userRoles.contains("ROLE_ADMIN");
    }

    /**
     * Validate the condition expression format
     *
     * @param condition The condition to validate
     * @return true if valid
     */
    public static boolean isValidCondition(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return false;
        }

        // Basic validation - contains comparison operators and is not just whitespace
        String trimmed = condition.trim();
        return trimmed.matches(".*[><=!]+.*") && trimmed.length() > 3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertRule alertRule = (AlertRule) o;
        return Objects.equals(id, alertRule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AlertRule{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", severity=" + severity +
                ", enabled=" + enabled +
                ", threshold=" + threshold +
                ", triggerCount=" + triggerCount +
                ", lastTriggered=" + lastTriggered +
                '}';
    }
}