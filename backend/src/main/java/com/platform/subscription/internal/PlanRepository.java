package com.platform.subscription.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Plan} entities.
 *
 * <p>This interface provides methods for querying subscription plan data from the database,
 * including finding active plans, filtering by various criteria, and counting.
 * </p>
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

  /**
   * Finds all plans with a given active status, ordered by their display order.
   *
   * @param active The active status to filter by.
   * @return A list of {@link Plan}s matching the criteria.
   */
  List<Plan> findByActiveOrderByDisplayOrderAsc(boolean active);

  /**
   * Finds an active plan by its unique slug.
   *
   * @param slug The slug of the plan.
   * @param active The active status (should be {@code true}).
   * @return An {@link Optional} containing the matching {@link Plan}, or empty if not found.
   */
  Optional<Plan> findBySlugAndActive(String slug, boolean active);

  /**
   * Finds a plan by its unique Stripe price ID.
   *
   * @param stripePriceId The price ID from the Stripe API.
   * @return An {@link Optional} containing the matching {@link Plan}, or empty if not found.
   */
  Optional<Plan> findByStripePriceId(String stripePriceId);

  /**
   * Finds all active plans with an amount less than or equal to a specified maximum.
   *
   * @param maxAmountCents The maximum amount in the smallest currency unit (e.g., cents).
   * @return A list of affordable, active {@link Plan}s.
   */
  @Query(
      "SELECT p FROM Plan p WHERE p.active = true AND p.amount.amount <= :maxAmount ORDER BY p.amount.amount ASC")
  List<Plan> findActivePlansUpToAmount(@Param("maxAmount") Long maxAmountCents);

  /**
   * Finds all active plans with a specific billing interval.
   *
   * @param interval The {@link Plan.BillingInterval} to filter by.
   * @return A list of active {@link Plan}s matching the interval.
   */
  @Query(
      "SELECT p FROM Plan p WHERE p.active = true AND p.interval = :interval ORDER BY p.displayOrder ASC")
  List<Plan> findActivePlansByInterval(@Param("interval") Plan.BillingInterval interval);

  /**
   * Counts the total number of active plans.
   *
   * @return The count of active plans.
   */
  @Query("SELECT COUNT(p) FROM Plan p WHERE p.active = true")
  long countActivePlans();

  /**
   * Finds all plans with a given active status, ordered by their price in ascending order.
   *
   * @param active The active status to filter by.
   * @return A list of {@link Plan}s, sorted by amount.
   */
  List<Plan> findByActiveOrderByAmountAmountAsc(boolean active);
}
