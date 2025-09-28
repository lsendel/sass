package com.platform.security.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * SecurityMetric entity for time-series security metrics and KPIs.
 *
 * This entity represents aggregated security metrics that are collected
 * over time for dashboard visualization and trend analysis.
 *
 * Key features:
 * - Time-series data with configurable aggregation intervals
 * - JSON metadata for flexible metric attributes
 * - Metric type classification for different visualizations
 * - Retention policy support with TTL-based cleanup
 * - Correlation with SecurityEvents for drill-down analysis
 * - Module-based metric categorization
 */
@Entity
@Table(name = "security_metrics",
       indexes = {
           @Index(name = "idx_security_metrics_timestamp", columnList = "timestamp DESC"),
           @Index(name = "idx_security_metrics_metric_type", columnList = "metricType"),
           @Index(name = "idx_security_metrics_source_module", columnList = "sourceModule"),
           @Index(name = "idx_security_metrics_aggregation_interval", columnList = "aggregationInterval"),
           @Index(name = "idx_security_metrics_retention", columnList = "retentionPolicy")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_metric_type_module_timestamp",
                           columnNames = {"metricType", "sourceModule", "timestamp", "aggregationInterval"})
       })
public class SecurityMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private MetricType metricType;

    @NotNull
    @Column(nullable = false)
    private Instant timestamp;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(nullable = false, precision = 19, scale = 4)
    private Double value;

    @NotNull
    @Column(name = "source_module", nullable = false, length = 100)
    private String sourceModule;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_interval", nullable = false, length = 20)
    private AggregationInterval aggregationInterval;

    @NotNull
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "retention_policy", nullable = false, length = 20)
    private RetentionPolicy retentionPolicy = RetentionPolicy.STANDARD;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @NotNull
    @Min(1)
    @Column(name = "sample_count", nullable = false)
    private Long sampleCount = 1L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Types of security metrics tracked by the system
     */
    public enum MetricType {
        // Authentication metrics
        LOGIN_SUCCESS_RATE,
        LOGIN_FAILURE_COUNT,
        FAILED_LOGIN_ATTEMPTS,
        SESSION_DURATION_AVG,

        // Payment security metrics
        FRAUD_DETECTION_RATE,
        PAYMENT_FAILURE_RATE,
        SUSPICIOUS_TRANSACTION_COUNT,
        CHARGEBACK_RATE,

        // API security metrics
        API_REQUEST_RATE,
        RATE_LIMIT_VIOLATIONS,
        UNAUTHORIZED_ACCESS_ATTEMPTS,
        API_ERROR_RATE,

        // System metrics
        ACTIVE_SESSIONS_COUNT,
        SECURITY_EVENTS_RATE,
        ALERT_TRIGGER_RATE,
        RESPONSE_TIME_P95,

        // Compliance metrics
        DATA_ACCESS_VIOLATIONS,
        AUDIT_LOG_RETENTION_STATUS,
        GDPR_REQUEST_PROCESSING_TIME,
        COMPLIANCE_SCORE
    }

    /**
     * Aggregation intervals for time-series data
     */
    public enum AggregationInterval {
        MINUTE_1(Duration.ofMinutes(1)),
        MINUTE_5(Duration.ofMinutes(5)),
        MINUTE_15(Duration.ofMinutes(15)),
        HOUR_1(Duration.ofHours(1)),
        HOUR_6(Duration.ofHours(6)),
        DAY_1(Duration.ofDays(1)),
        WEEK_1(Duration.ofDays(7));

        private final Duration duration;

        AggregationInterval(Duration duration) {
            this.duration = duration;
        }

        public Duration getDuration() {
            return duration;
        }

        public long getSeconds() {
            return duration.getSeconds();
        }
    }

    /**
     * Data retention policies for different metric types
     */
    public enum RetentionPolicy {
        SHORT_TERM(Duration.ofDays(7)),     // 7 days for high-frequency data
        STANDARD(Duration.ofDays(30)),      // 30 days for regular metrics
        LONG_TERM(Duration.ofDays(90)),     // 90 days for compliance data
        ARCHIVAL(Duration.ofDays(365));     // 1 year for historical analysis

        private final Duration duration;

        RetentionPolicy(Duration duration) {
            this.duration = duration;
        }

        public Duration getDuration() {
            return duration;
        }

        public Instant calculateExpirationTime(Instant createdAt) {
            return createdAt.plus(duration);
        }
    }

    // Constructors
    protected SecurityMetric() {
        // JPA constructor
    }

    public SecurityMetric(@NotNull MetricType metricType,
                         @NotNull Instant timestamp,
                         @NotNull Double value,
                         @NotNull String sourceModule,
                         @NotNull AggregationInterval aggregationInterval) {
        this.metricType = metricType;
        this.timestamp = timestamp;
        this.value = value;
        this.sourceModule = sourceModule;
        this.aggregationInterval = aggregationInterval;
        this.retentionPolicy = RetentionPolicy.STANDARD;
        this.sampleCount = 1L;
        this.expiresAt = RetentionPolicy.STANDARD.calculateExpirationTime(Instant.now());
    }

    public SecurityMetric(@NotNull MetricType metricType,
                         @NotNull Instant timestamp,
                         @NotNull Double value,
                         @NotNull String sourceModule,
                         @NotNull AggregationInterval aggregationInterval,
                         @NotNull Map<String, Object> metadata,
                         @NotNull RetentionPolicy retentionPolicy,
                         String correlationId) {
        this.metricType = metricType;
        this.timestamp = timestamp;
        this.value = value;
        this.sourceModule = sourceModule;
        this.aggregationInterval = aggregationInterval;
        this.metadata = new HashMap<>(metadata);
        this.retentionPolicy = retentionPolicy;
        this.correlationId = correlationId;
        this.sampleCount = 1L;
        this.expiresAt = retentionPolicy.calculateExpirationTime(Instant.now());
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(MetricType metricType) {
        this.metricType = metricType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        if (value != null && value >= 0) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Metric value must be non-negative");
        }
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public AggregationInterval getAggregationInterval() {
        return aggregationInterval;
    }

    public void setAggregationInterval(AggregationInterval aggregationInterval) {
        this.aggregationInterval = aggregationInterval;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public RetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(RetentionPolicy retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
        if (this.createdAt != null) {
            this.expiresAt = retentionPolicy.calculateExpirationTime(this.createdAt);
        }
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Long sampleCount) {
        this.sampleCount = sampleCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Business Logic Methods

    /**
     * Check if this metric has expired based on retention policy
     *
     * @return true if metric should be purged
     */
    public boolean hasExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Calculate time until expiration
     *
     * @return duration until expiration, null if no expiration set
     */
    public Duration getTimeUntilExpiration() {
        if (expiresAt == null) {
            return null;
        }

        Instant now = Instant.now();
        return expiresAt.isAfter(now) ? Duration.between(now, expiresAt) : Duration.ZERO;
    }

    /**
     * Check if this is a real-time metric (minute-level aggregation)
     *
     * @return true if metric is real-time
     */
    public boolean isRealTime() {
        return aggregationInterval == AggregationInterval.MINUTE_1 ||
               aggregationInterval == AggregationInterval.MINUTE_5;
    }

    /**
     * Check if this is a critical security metric
     *
     * @return true if metric type indicates critical security data
     */
    public boolean isCritical() {
        return metricType == MetricType.FRAUD_DETECTION_RATE ||
               metricType == MetricType.UNAUTHORIZED_ACCESS_ATTEMPTS ||
               metricType == MetricType.DATA_ACCESS_VIOLATIONS ||
               metricType == MetricType.RATE_LIMIT_VIOLATIONS;
    }

    /**
     * Get metadata value by key
     *
     * @param key The metadata key
     * @return the value or null if not found
     */
    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }

    /**
     * Set metadata value
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadataValue(String key, Object value) {
        if (key != null) {
            metadata.put(key, value);
        }
    }

    /**
     * Add to the sample count for aggregated metrics
     *
     * @param additionalSamples Number of samples to add
     */
    public void addSamples(long additionalSamples) {
        if (additionalSamples > 0) {
            this.sampleCount += additionalSamples;
        }
    }

    /**
     * Update the metric value using a weighted average for aggregation
     *
     * @param newValue The new value to incorporate
     * @param newSampleCount Number of samples in the new value
     */
    public void updateWithWeightedAverage(double newValue, long newSampleCount) {
        if (newSampleCount <= 0) {
            throw new IllegalArgumentException("Sample count must be positive");
        }

        double currentWeight = (double) this.sampleCount;
        double newWeight = (double) newSampleCount;
        double totalWeight = currentWeight + newWeight;

        this.value = ((this.value * currentWeight) + (newValue * newWeight)) / totalWeight;
        this.sampleCount += newSampleCount;
    }

    /**
     * Check if this metric belongs to a specific module
     *
     * @param module The module to check
     * @return true if metric belongs to the module
     */
    public boolean belongsToModule(String module) {
        return Objects.equals(sourceModule, module);
    }

    /**
     * Check if this metric matches the given correlation ID
     *
     * @param correlationId The correlation ID to check
     * @return true if correlation IDs match
     */
    public boolean hasCorrelationId(String correlationId) {
        return this.correlationId != null && this.correlationId.equals(correlationId);
    }

    /**
     * Create a bucket key for time-series aggregation
     *
     * @return string key for grouping metrics
     */
    public String createBucketKey() {
        return String.format("%s:%s:%s:%d",
                           metricType.name(),
                           sourceModule,
                           aggregationInterval.name(),
                           getBucketTimestamp());
    }

    /**
     * Get timestamp aligned to aggregation interval boundary
     *
     * @return aligned timestamp in seconds
     */
    public long getBucketTimestamp() {
        long intervalSeconds = aggregationInterval.getSeconds();
        return (timestamp.getEpochSecond() / intervalSeconds) * intervalSeconds;
    }

    /**
     * Calculate the relative age of this metric
     *
     * @return duration since creation
     */
    public Duration getAge() {
        return Duration.between(createdAt, Instant.now());
    }

    /**
     * Check if metric is within a time window
     *
     * @param windowStart Start of time window
     * @param windowEnd End of time window
     * @return true if metric timestamp is within window
     */
    public boolean isWithinTimeWindow(Instant windowStart, Instant windowEnd) {
        return !timestamp.isBefore(windowStart) && !timestamp.isAfter(windowEnd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityMetric that = (SecurityMetric) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SecurityMetric{" +
                "id=" + id +
                ", metricType=" + metricType +
                ", value=" + value +
                ", timestamp=" + timestamp +
                ", sourceModule='" + sourceModule + '\'' +
                ", aggregationInterval=" + aggregationInterval +
                ", sampleCount=" + sampleCount +
                ", retentionPolicy=" + retentionPolicy +
                '}';
    }
}