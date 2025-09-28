package com.platform.security.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * ThreatIndicator entity for external threat intelligence data.
 *
 * This entity represents threat indicators from external sources such as
 * IP reputation feeds, malware signatures, and threat intelligence platforms.
 *
 * Key features:
 * - Multiple indicator types (IP, domain, hash, URL)
 * - Confidence scoring and threat severity classification
 * - Source attribution and feed management
 * - TTL-based expiration for dynamic threat data
 * - Tag-based categorization and context metadata
 * - Whitelist/blacklist status tracking
 * - Integration with real-time threat detection
 */
@Entity
@Table(name = "threat_indicators",
       indexes = {
           @Index(name = "idx_threat_indicators_type", columnList = "indicatorType"),
           @Index(name = "idx_threat_indicators_value", columnList = "indicatorValue"),
           @Index(name = "idx_threat_indicators_severity", columnList = "severity"),
           @Index(name = "idx_threat_indicators_active", columnList = "active"),
           @Index(name = "idx_threat_indicators_expires_at", columnList = "expiresAt"),
           @Index(name = "idx_threat_indicators_source", columnList = "source"),
           @Index(name = "idx_threat_indicators_created_at", columnList = "createdAt DESC")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_indicator_type_value_source",
                           columnNames = {"indicatorType", "indicatorValue", "source"})
       })
public class ThreatIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false, length = 20)
    private IndicatorType indicatorType;

    @NotNull
    @Size(min = 1, max = 2048)
    @Column(name = "indicator_value", nullable = false, length = 2048)
    private String indicatorValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Column(nullable = false, precision = 5, scale = 2)
    private Double confidence;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String source;

    @Column(name = "source_reference", length = 1024)
    private String sourceReference;

    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ThreatType threatType;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @NotNull
    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(name = "first_seen")
    private Instant firstSeen;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "threat_indicator_tags", joinColumns = @JoinColumn(name = "indicator_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @NotNull
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> context = new HashMap<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListStatus listStatus = ListStatus.NEUTRAL;

    @Min(0)
    @Column(name = "detection_count")
    private Integer detectionCount = 0;

    @Column(name = "false_positive_count")
    @Min(0)
    private Integer falsePositiveCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Types of threat indicators supported
     */
    public enum IndicatorType {
        IP_ADDRESS,     // IPv4 or IPv6 addresses
        DOMAIN,         // Domain names and subdomains
        URL,            // Full URLs including paths
        FILE_HASH,      // MD5, SHA1, SHA256 file hashes
        EMAIL,          // Email addresses
        USER_AGENT,     // Browser user agent strings
        SSL_CERT        // SSL certificate fingerprints
    }

    /**
     * Threat severity levels
     */
    public enum Severity {
        CRITICAL,       // Immediate threat requiring blocking
        HIGH,           // High-risk threat requiring attention
        MEDIUM,         // Moderate risk, monitor closely
        LOW,            // Low risk, informational
        INFO            // Informational only, no threat
    }

    /**
     * Types of threats represented by indicators
     */
    public enum ThreatType {
        MALWARE,        // Malicious software
        PHISHING,       // Phishing campaigns
        BOTNET,         // Botnet command and control
        FRAUD,          // Fraudulent activity
        SPAM,           // Spam and unwanted content
        RECONNAISSANCE, // Scanning and reconnaissance
        EXPLOIT,        // Exploitation attempts
        APT,            // Advanced persistent threats
        IOC,            // Indicators of compromise
        SUSPICIOUS      // Suspicious but unclassified
    }

    /**
     * Whitelist/blacklist status
     */
    public enum ListStatus {
        WHITELIST,      // Explicitly trusted/allowed
        BLACKLIST,      // Explicitly blocked/denied
        GREYLIST,       // Requires additional scrutiny
        NEUTRAL         // No explicit status
    }

    // Constructors
    protected ThreatIndicator() {
        // JPA constructor
    }

    public ThreatIndicator(@NotNull IndicatorType indicatorType,
                          @NotNull String indicatorValue,
                          @NotNull Severity severity,
                          @NotNull Double confidence,
                          @NotNull String source,
                          @NotNull ThreatType threatType) {
        this.indicatorType = indicatorType;
        this.indicatorValue = indicatorValue;
        this.severity = severity;
        this.confidence = confidence;
        this.source = source;
        this.threatType = threatType;
        this.active = true;
        this.listStatus = ListStatus.NEUTRAL;
        this.lastSeen = Instant.now();
        this.firstSeen = this.lastSeen;
        this.detectionCount = 0;
        this.falsePositiveCount = 0;
    }

    public ThreatIndicator(@NotNull IndicatorType indicatorType,
                          @NotNull String indicatorValue,
                          @NotNull Severity severity,
                          @NotNull Double confidence,
                          @NotNull String source,
                          @NotNull ThreatType threatType,
                          String sourceReference,
                          Instant expiresAt,
                          @NotNull Set<String> tags,
                          @NotNull Map<String, Object> context) {
        this.indicatorType = indicatorType;
        this.indicatorValue = indicatorValue;
        this.severity = severity;
        this.confidence = confidence;
        this.source = source;
        this.threatType = threatType;
        this.sourceReference = sourceReference;
        this.expiresAt = expiresAt;
        this.tags = new HashSet<>(tags);
        this.context = new HashMap<>(context);
        this.active = true;
        this.listStatus = ListStatus.NEUTRAL;
        this.lastSeen = Instant.now();
        this.firstSeen = this.lastSeen;
        this.detectionCount = 0;
        this.falsePositiveCount = 0;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public IndicatorType getIndicatorType() {
        return indicatorType;
    }

    public void setIndicatorType(IndicatorType indicatorType) {
        this.indicatorType = indicatorType;
    }

    public String getIndicatorValue() {
        return indicatorValue;
    }

    public void setIndicatorValue(String indicatorValue) {
        this.indicatorValue = indicatorValue;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        if (confidence != null && confidence >= 0.0 && confidence <= 100.0) {
            this.confidence = confidence;
        } else {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 100.0");
        }
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public ThreatType getThreatType() {
        return threatType;
    }

    public void setThreatType(ThreatType threatType) {
        this.threatType = threatType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Instant firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
    }

    public ListStatus getListStatus() {
        return listStatus;
    }

    public void setListStatus(ListStatus listStatus) {
        this.listStatus = listStatus;
    }

    public Integer getDetectionCount() {
        return detectionCount;
    }

    public void setDetectionCount(Integer detectionCount) {
        this.detectionCount = detectionCount;
    }

    public Integer getFalsePositiveCount() {
        return falsePositiveCount;
    }

    public void setFalsePositiveCount(Integer falsePositiveCount) {
        this.falsePositiveCount = falsePositiveCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Business Logic Methods

    /**
     * Check if this threat indicator has expired
     *
     * @return true if indicator has expired
     */
    public boolean hasExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if this is a high-severity threat
     *
     * @return true if severity is CRITICAL or HIGH
     */
    public boolean isHighSeverity() {
        return severity == Severity.CRITICAL || severity == Severity.HIGH;
    }

    /**
     * Check if this indicator should be actively blocked
     *
     * @return true if active and not whitelisted
     */
    public boolean shouldBlock() {
        return active && !hasExpired() &&
               listStatus != ListStatus.WHITELIST &&
               (listStatus == ListStatus.BLACKLIST || isHighSeverity());
    }

    /**
     * Check if this indicator has high confidence
     *
     * @return true if confidence >= 80%
     */
    public boolean hasHighConfidence() {
        return confidence >= 80.0;
    }

    /**
     * Update the last seen timestamp and increment detection count
     */
    public void recordDetection() {
        this.lastSeen = Instant.now();
        this.detectionCount++;
    }

    /**
     * Record a false positive detection
     */
    public void recordFalsePositive() {
        this.falsePositiveCount++;

        // Automatically adjust confidence on repeated false positives
        if (falsePositiveCount >= 3 && confidence > 20.0) {
            this.confidence = Math.max(20.0, confidence - 10.0);
        }
    }

    /**
     * Calculate the false positive rate
     *
     * @return false positive rate as percentage
     */
    public double getFalsePositiveRate() {
        int totalDetections = detectionCount + falsePositiveCount;
        return totalDetections > 0 ? (double) falsePositiveCount / totalDetections * 100.0 : 0.0;
    }

    /**
     * Check if this indicator is stale (no recent activity)
     *
     * @param staleDuration Duration to consider stale
     * @return true if indicator is stale
     */
    public boolean isStale(Duration staleDuration) {
        return Duration.between(lastSeen, Instant.now()).compareTo(staleDuration) > 0;
    }

    /**
     * Add a tag to this indicator
     *
     * @param tag The tag to add
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            tags.add(tag.trim().toLowerCase());
        }
    }

    /**
     * Remove a tag from this indicator
     *
     * @param tag The tag to remove
     */
    public void removeTag(String tag) {
        if (tag != null) {
            tags.remove(tag.trim().toLowerCase());
        }
    }

    /**
     * Check if indicator has a specific tag
     *
     * @param tag The tag to check
     * @return true if tag exists
     */
    public boolean hasTag(String tag) {
        return tag != null && tags.contains(tag.trim().toLowerCase());
    }

    /**
     * Get context value by key
     *
     * @param key The context key
     * @return the value or null if not found
     */
    public Object getContextValue(String key) {
        return context.get(key);
    }

    /**
     * Set context value
     *
     * @param key The context key
     * @param value The context value
     */
    public void setContextValue(String key, Object value) {
        if (key != null) {
            context.put(key, value);
        }
    }

    /**
     * Deactivate this threat indicator
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Reactivate this threat indicator
     */
    public void reactivate() {
        this.active = true;
        this.lastSeen = Instant.now();
    }

    /**
     * Add to whitelist (mark as trusted)
     */
    public void whitelist() {
        this.listStatus = ListStatus.WHITELIST;
        this.active = false; // Deactivate blocking for whitelisted items
    }

    /**
     * Add to blacklist (mark as malicious)
     */
    public void blacklist() {
        this.listStatus = ListStatus.BLACKLIST;
        this.active = true;
        this.severity = Severity.HIGH; // Escalate severity for blacklisted items
    }

    /**
     * Calculate threat score based on severity, confidence, and history
     *
     * @return threat score from 0-100
     */
    public double calculateThreatScore() {
        double severityWeight = getSeverityWeight();
        double confidenceWeight = confidence / 100.0;
        double historyWeight = calculateHistoryWeight();

        return Math.min(100.0, (severityWeight * 0.4 + confidenceWeight * 0.4 + historyWeight * 0.2) * 100.0);
    }

    /**
     * Get numeric weight for severity level
     *
     * @return severity weight from 0.0 to 1.0
     */
    private double getSeverityWeight() {
        return switch (severity) {
            case CRITICAL -> 1.0;
            case HIGH -> 0.8;
            case MEDIUM -> 0.6;
            case LOW -> 0.4;
            case INFO -> 0.2;
        };
    }

    /**
     * Calculate history-based weight considering detections and false positives
     *
     * @return history weight from 0.0 to 1.0
     */
    private double calculateHistoryWeight() {
        if (detectionCount == 0) {
            return 0.5; // Neutral for new indicators
        }

        double falsePositiveRate = getFalsePositiveRate();
        return Math.max(0.0, 1.0 - (falsePositiveRate / 100.0));
    }

    /**
     * Check if indicator matches a given value (case-insensitive for some types)
     *
     * @param value The value to check
     * @return true if values match
     */
    public boolean matches(String value) {
        if (value == null) {
            return false;
        }

        return switch (indicatorType) {
            case DOMAIN, EMAIL -> indicatorValue.equalsIgnoreCase(value);
            case IP_ADDRESS, URL, FILE_HASH, USER_AGENT, SSL_CERT -> indicatorValue.equals(value);
        };
    }

    /**
     * Get the age of this indicator
     *
     * @return duration since first seen
     */
    public Duration getAge() {
        return firstSeen != null ? Duration.between(firstSeen, Instant.now()) : Duration.ZERO;
    }

    /**
     * Get time until expiration
     *
     * @return duration until expiration, null if no expiration
     */
    public Duration getTimeUntilExpiration() {
        if (expiresAt == null) {
            return null;
        }

        Instant now = Instant.now();
        return expiresAt.isAfter(now) ? Duration.between(now, expiresAt) : Duration.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreatIndicator that = (ThreatIndicator) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ThreatIndicator{" +
                "id=" + id +
                ", indicatorType=" + indicatorType +
                ", indicatorValue='" + indicatorValue + '\'' +
                ", severity=" + severity +
                ", confidence=" + confidence +
                ", source='" + source + '\'' +
                ", threatType=" + threatType +
                ", active=" + active +
                ", listStatus=" + listStatus +
                ", detectionCount=" + detectionCount +
                '}';
    }
}