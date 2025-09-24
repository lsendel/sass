package com.platform.subscription.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Data transfer objects for plan-related operations.
 * These DTOs prevent direct access to internal entities from controllers.
 */
public class PlanDto {

  /**
   * Billing interval enumeration for API layer.
   */
  public enum BillingInterval {
    MONTH, YEAR
  }

  /**
   * Response DTO for plan information.
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
      Instant updatedAt) {
  }

  /**
   * Request DTO for creating a new plan.
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
      Map<String, Object> features) {
  }

  /**
   * Request DTO for updating an existing plan.
   */
  public record UpdatePlanRequest(
      String name,
      String description,
      BigDecimal amount,
      Integer trialDays,
      boolean active,
      int displayOrder,
      Map<String, Object> features) {
  }
}