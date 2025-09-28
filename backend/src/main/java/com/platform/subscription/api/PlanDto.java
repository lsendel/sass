package com.platform.subscription.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A container for all Data Transfer Objects (DTOs) related to subscription plans.
 *
 * <p>This class uses nested records and enums to define the data structures for API requests and
 * responses concerning subscription plans. This approach encapsulates plan-related data contracts,
 * preventing the direct exposure of internal domain entities.
 * </p>
 */
public class PlanDto {

  /** Defines the billing interval for a subscription plan. */
  public enum BillingInterval {
    /** A monthly billing cycle. */
    MONTH,
    /** A yearly billing cycle. */
    YEAR
  }

  /**
   * Represents the data for a subscription plan sent in API responses.
   *
   * @param id The unique identifier of the plan.
   * @param name The user-friendly name of the plan.
   * @param slug A unique, URL-friendly identifier for the plan.
   * @param description A detailed description of the plan.
   * @param amount The cost of the plan per billing interval.
   * @param currency The ISO currency code for the amount.
   * @param interval The billing interval (e.g., MONTH, YEAR).
   * @param intervalCount The number of intervals between billing cycles (e.g., 1 for every month).
   * @param trialDays The number of free trial days offered with the plan.
   * @param active Whether the plan is currently active and available for new subscriptions.
   * @param displayOrder An integer used for ordering plans in a UI.
   * @param features A map of key-value pairs representing the features included in the plan.
   * @param createdAt The timestamp when the plan was created.
   * @param updatedAt The timestamp when the plan was last updated.
   */
  public record PlanResponse(
      UUID id,
      String name,
      String slug,
      String description,
      BigDecimal amount,
      String currency,
      BillingInterval interval,
      Integer intervalCount,
      Integer trialDays,
      boolean active,
      int displayOrder,
      Map<String, Object> features,
      Instant createdAt,
      Instant updatedAt) {}

  /**
   * Represents the data required to create a new subscription plan.
   *
   * @param name The name of the new plan.
   * @param slug The unique slug for the new plan.
   * @param description The description of the new plan.
   * @param amount The price of the new plan.
   * @param currency The currency of the new plan.
   * @param interval The billing interval for the new plan.
   * @param intervalCount The number of intervals between billing cycles.
   * @param trialDays The number of trial days for the new plan.
   * @param displayOrder The display order for the new plan.
   * @param features The features included in the new plan.
   */
  public record CreatePlanRequest(
      String name,
      String slug,
      String description,
      BigDecimal amount,
      String currency,
      BillingInterval interval,
      Integer intervalCount,
      Integer trialDays,
      int displayOrder,
      Map<String, Object> features) {}

  /**
   * Represents the data allowed for updating an existing subscription plan.
   *
   * @param name The updated name of the plan.
   * @param description The updated description.
   * @param amount The updated price.
   * @param trialDays The updated number of trial days.
   * @param active The updated active status.
   * @param displayOrder The updated display order.
   * @param features The updated map of features.
   */
  public record UpdatePlanRequest(
      String name,
      String description,
      BigDecimal amount,
      Integer trialDays,
      boolean active,
      int displayOrder,
      Map<String, Object> features) {}
}