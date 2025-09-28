package com.platform.subscription.internal;

import com.platform.shared.types.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents a subscription plan available for customers.
 *
 * <p>This entity defines the terms of a subscription, including its name, price, billing interval,
 * and a flexible map of features. It is linked to a corresponding price object in the Stripe
 * payment provider via the {@code stripePriceId}.
 *
 * <p>Plans can be activated or deactivated to control their availability for new subscriptions.
 *
 * @see Subscription
 * @see Money
 */
@Entity
@Table(
    name = "plans",
    indexes = {
      @Index(name = "idx_plans_active", columnList = "active"),
      @Index(name = "idx_plans_stripe_price", columnList = "stripe_price_id", unique = true)
    })
public class Plan {

  /** Enumerates the supported billing intervals for a subscription plan. */
  public enum BillingInterval {
    /** A monthly billing interval. */
    MONTH("month"),
    /** A yearly billing interval. */
    YEAR("year");

    private final String value;

    BillingInterval(String value) {
      this.value = value;
    }

    /**
     * Gets the string value of the billing interval.
     *
     * @return the string value of the interval
     */
    public String getValue() {
      return value;
    }

    /**
     * Creates a {@link BillingInterval} enum from a string value.
     *
     * @param interval the string value of the interval
     * @return the corresponding {@link BillingInterval} enum
     * @throws IllegalArgumentException if the interval string is invalid
     */
    public static BillingInterval fromString(String interval) {
      for (BillingInterval bi : values()) {
        if (bi.value.equals(interval)) {
          return bi;
        }
      }
      throw new IllegalArgumentException("Invalid billing interval: " + interval);
    }
  }

  /** The unique identifier for the plan. */
  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The name of the plan. */
  @NotBlank
  @Column(name = "name", nullable = false)
  private String name;

  /** A URL-friendly slug for the plan. */
  @NotBlank
  @Column(name = "slug", nullable = false, unique = true)
  private String slug;

  /** A description of the plan. */
  @Column(name = "description")
  private String description;

  /** The ID of the price in Stripe. */
  @NotBlank
  @Column(name = "stripe_price_id", nullable = false, unique = true)
  private String stripePriceId;

  /** The monetary amount of the plan. */
  @Embedded private Money amount;

  /** The billing interval for the plan. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "interval", nullable = false, length = 20)
  private BillingInterval interval;

  /** The number of intervals between subscription billings. */
  @Column(name = "interval_count", nullable = false)
  private Integer intervalCount = 1;

  /** The number of trial days for the plan. */
  @Column(name = "trial_days")
  private Integer trialDays = 0;

  /** The display order of the plan. */
  @Column(name = "display_order", nullable = false)
  private Integer displayOrder = 0;

  /** A map of features included in the plan. */
  @Column(name = "features")
  @Convert(converter = PlanFeaturesConverter.class)
  private Map<String, Object> features = Map.of();

  /** Whether the plan is active and available for new subscriptions. */
  @Column(name = "active", nullable = false)
  private boolean active = true;

  /** The timestamp of when the plan was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** The timestamp of the last update to the plan. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** The version number for optimistic locking. */
  @Version private Long version;

  /**
   * Protected no-argument constructor for JPA.
   *
   * <p>This constructor is required by JPA and should not be used directly.
   */
  protected Plan() {
    // JPA constructor
  }

  /**
   * Constructs a new Plan.
   *
   * @param name the name of the plan
   * @param stripePriceId the corresponding price ID from the Stripe API
   * @param amount the monetary value of the plan
   * @param interval the billing interval for the plan
   */
  public Plan(String name, String stripePriceId, Money amount, BillingInterval interval) {
    this.name = name;
    this.stripePriceId = stripePriceId;
    this.amount = amount;
    this.interval = interval;
  }

  /**
   * Checks if the plan is currently active and available for new subscriptions.
   *
   * @return {@code true} if the plan is active, {@code false} otherwise
   */
  public boolean isActive() {
    return active;
  }

  /** Activates the plan, making it available for new subscriptions. */
  public void activate() {
    this.active = true;
  }

  /** Deactivates the plan, preventing new subscriptions. */
  public void deactivate() {
    this.active = false;
  }

  /**
   * Updates the feature map for this plan.
   *
   * @param features a map containing the new set of features
   */
  public void updateFeatures(Map<String, Object> features) {
    this.features = features != null ? Map.copyOf(features) : Map.of();
  }

  /**
   * Retrieves a specific feature's value from the features map.
   *
   * @param key the key of the feature to retrieve
   * @param defaultValue the value to return if the feature is not found
   * @param <T> the type of the feature's value
   * @return the feature's value, or the default value if not present
   */
  @SuppressWarnings("unchecked")
  public <T> T getFeature(String key, T defaultValue) {
    return (T) features.getOrDefault(key, defaultValue);
  }

  /**
   * Gets the maximum number of users allowed by the plan.
   *
   * @return the maximum number of users
   */
  public Integer getMaxUsers() {
    return getFeature("maxUsers", 1);
  }

  /**
   * Gets the number of API calls allowed per month.
   *
   * @return the number of API calls per month
   */
  public Long getApiCallsPerMonth() {
    return getFeature("apiCallsPerMonth", 1000L);
  }

  /**
   * Gets the support level for the plan.
   *
   * @return the support level
   */
  public String getSupportLevel() {
    return getFeature("supportLevel", "email");
  }

  /**
   * Checks if the plan includes custom integrations.
   *
   * @return {@code true} if custom integrations are included, {@code false} otherwise
   */
  public boolean hasCustomIntegrations() {
    return getFeature("customIntegrations", false);
  }

  /**
   * Checks if the plan includes advanced analytics.
   *
   * @return {@code true} if advanced analytics are included, {@code false} otherwise
   */
  public boolean hasAdvancedAnalytics() {
    return getFeature("advancedAnalytics", false);
  }

  /**
   * Checks if the plan includes priority support.
   *
   * @return {@code true} if priority support is included, {@code false} otherwise
   */
  public boolean hasPrioritySupport() {
    return getFeature("prioritySupport", false);
  }

  /**
   * Checks if the plan allows unlimited users.
   *
   * @return {@code true} if the plan allows unlimited users, {@code false} otherwise
   */
  public boolean isUnlimitedUsers() {
    Integer maxUsers = getMaxUsers();
    return maxUsers == -1;
  }

  /**
   * Checks if the plan allows unlimited API calls.
   *
   * @return {@code true} if the plan allows unlimited API calls, {@code false} otherwise
   */
  public boolean isUnlimitedApiCalls() {
    Long apiCalls = getApiCallsPerMonth();
    return apiCalls == -1L;
  }

  /**
   * Calculates the equivalent yearly amount for the plan.
   *
   * @return the yearly cost of the plan
   */
  public Money getYearlyAmount() {
    if (interval == BillingInterval.YEAR) {
      return amount;
    }
    return amount.multiply(12);
  }

  /**
   * Calculates the equivalent monthly amount for the plan.
   *
   * @return the monthly cost of the plan
   */
  public Money getMonthlyAmount() {
    if (interval == BillingInterval.MONTH) {
      return amount;
    }
    return amount.divide(12);
  }

  // Getters

  /**
   * Gets the unique identifier for the plan.
   *
   * @return the ID of the plan
   */
  public UUID getId() {
    return id;
  }

  /**
   * Gets the name of the plan.
   *
   * @return the name of the plan
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the URL-friendly slug for the plan.
   *
   * @return the slug of the plan
   */
  public String getSlug() {
    return slug;
  }

  /**
   * Gets the description of the plan.
   *
   * @return the description of the plan
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the ID of the price in Stripe.
   *
   * @return the Stripe price ID
   */
  public String getStripePriceId() {
    return stripePriceId;
  }

  /**
   * Gets the monetary amount of the plan.
   *
   * @return the amount of the plan
   */
  public Money getAmount() {
    return amount;
  }

  /**
   * Gets the price of the plan.
   *
   * @return the price of the plan
   */
  public Money getPrice() {
    return amount;
  }

  /**
   * Gets the currency of the plan.
   *
   * @return the currency of the plan
   */
  public String getCurrency() {
    return amount != null ? amount.getCurrency() : "USD";
  }

  /**
   * Gets the billing interval for the plan.
   *
   * @return the billing interval
   */
  public BillingInterval getInterval() {
    return interval;
  }

  /**
   * Gets the number of intervals between subscription billings.
   *
   * @return the interval count
   */
  public Integer getIntervalCount() {
    return intervalCount;
  }

  /**
   * Gets the number of trial days for the plan.
   *
   * @return the number of trial days
   */
  public Integer getTrialDays() {
    return trialDays;
  }

  /**
   * Gets the display order of the plan.
   *
   * @return the display order
   */
  public Integer getDisplayOrder() {
    return displayOrder;
  }

  /**
   * Gets a map of features included in the plan.
   *
   * @return an immutable map of features
   */
  public Map<String, Object> getFeatures() {
    return Map.copyOf(features);
  }

  /**
   * Gets the timestamp of when the plan was created.
   *
   * @return the creation timestamp
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Gets the timestamp of the last update to the plan.
   *
   * @return the last update timestamp
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Gets the version number for optimistic locking.
   *
   * @return the version number
   */
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
    return "Plan{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", amount="
        + amount
        + ", interval="
        + interval
        + ", active="
        + active
        + ", createdAt="
        + createdAt
        + '}';
  }
}
