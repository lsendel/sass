package com.platform.subscription.internal;

import com.platform.shared.types.Money;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Subscription plan entity with Stripe integration and feature configuration.
 */
@Entity
@Table(name = "plans", indexes = {
    @Index(name = "idx_plans_active", columnList = "active"),
    @Index(name = "idx_plans_stripe_price", columnList = "stripe_price_id", unique = true)
})
public class Plan {

    /**
     * Billing intervals supported by the platform
     */
    public enum BillingInterval {
        MONTH("month"),
        YEAR("year");

        private final String value;

        BillingInterval(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static BillingInterval fromString(String interval) {
            for (BillingInterval bi : values()) {
                if (bi.value.equals(interval)) {
                    return bi;
                }
            }
            throw new IllegalArgumentException("Invalid billing interval: " + interval);
        }
    }

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "stripe_price_id", nullable = false, unique = true)
    private String stripePriceId;

    @Embedded
    private Money amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "interval", nullable = false, length = 20)
    private BillingInterval interval;

    @Column(name = "interval_count", nullable = false)
    private Integer intervalCount = 1;

    @Column(name = "trial_days")
    private Integer trialDays = 0;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "features")
    @Convert(converter = PlanFeaturesConverter.class)
    private Map<String, Object> features = Map.of();

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    // Constructors
    protected Plan() {
        // JPA constructor
    }

    public Plan(String name, String stripePriceId, Money amount, BillingInterval interval) {
        this.name = name;
        this.stripePriceId = stripePriceId;
        this.amount = amount;
        this.interval = interval;
    }

    // Business methods
    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void updateFeatures(Map<String, Object> features) {
        this.features = features != null ? Map.copyOf(features) : Map.of();
    }

    // Feature helper methods
    @SuppressWarnings("unchecked")
    public <T> T getFeature(String key, T defaultValue) {
        return (T) features.getOrDefault(key, defaultValue);
    }

    public Integer getMaxUsers() {
        return getFeature("maxUsers", 1);
    }

    public Long getApiCallsPerMonth() {
        return getFeature("apiCallsPerMonth", 1000L);
    }

    public String getSupportLevel() {
        return getFeature("supportLevel", "email");
    }

    public boolean hasCustomIntegrations() {
        return getFeature("customIntegrations", false);
    }

    public boolean hasAdvancedAnalytics() {
        return getFeature("advancedAnalytics", false);
    }

    public boolean hasPrioritySupport() {
        return getFeature("prioritySupport", false);
    }

    public boolean isUnlimitedUsers() {
        Integer maxUsers = getMaxUsers();
        return maxUsers == -1;
    }

    public boolean isUnlimitedApiCalls() {
        Long apiCalls = getApiCallsPerMonth();
        return apiCalls == -1L;
    }

    // Pricing calculations
    public Money getYearlyAmount() {
        if (interval == BillingInterval.YEAR) {
            return amount;
        }
        // Calculate yearly from monthly (12 months)
        return amount.multiply(12);
    }

    public Money getMonthlyAmount() {
        if (interval == BillingInterval.MONTH) {
            return amount;
        }
        // Calculate monthly from yearly
        return amount.divide(12);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getPrice() {
        return amount;
    }

    public String getCurrency() {
        return amount != null ? amount.getCurrency() : "USD";
    }

    public BillingInterval getInterval() {
        return interval;
    }

    public Integer getIntervalCount() {
        return intervalCount;
    }

    public Integer getTrialDays() {
        return trialDays;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public Map<String, Object> getFeatures() {
        return Map.copyOf(features);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Plan other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Plan{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", interval=" + interval +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}