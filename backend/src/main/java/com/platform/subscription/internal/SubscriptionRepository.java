package com.platform.subscription.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Subscription} entities.
 *
 * <p>This interface provides methods for querying and managing subscription records in the
 * database. It includes standard finder methods as well as custom queries for more complex
 * scenarios like finding expired or renewable subscriptions.
 * </p>
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  /**
   * Finds a subscription by its associated organization ID.
   *
   * @param organizationId The ID of the organization.
   * @return An {@link Optional} containing the matching {@link Subscription}, or empty if not
   *     found.
   */
  Optional<Subscription> findByOrganizationId(UUID organizationId);

  /**
   * Finds a subscription by its unique Stripe subscription ID.
   *
   * @param stripeSubscriptionId The ID from the Stripe API.
   * @return An {@link Optional} containing the matching {@link Subscription}, or empty if not
   *     found.
   */
  Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

  /**
   * Finds all subscriptions for a given plan with a specific status.
   *
   * @param planId The ID of the plan.
   * @param status The status to filter by.
   * @return A list of matching {@link Subscription}s.
   */
  List<Subscription> findByPlanIdAndStatus(UUID planId, Subscription.Status status);

  /**
   * Finds subscriptions with a given status that have expired before a certain date.
   *
   * @param status The status to check for (e.g., ACTIVE).
   * @param cutoffDate The date to consider as the expiration cutoff.
   * @return A list of expired subscriptions.
   */
  @Query(
      "SELECT s FROM Subscription s WHERE s.status = :status AND s.currentPeriodEnd < :cutoffDate")
  List<Subscription> findExpiredSubscriptions(
      @Param("status") Subscription.Status status, @Param("cutoffDate") Instant cutoffDate);

  /**
   * Finds all subscriptions whose trial period has expired before a certain date.
   *
   * @param cutoffDate The date to consider as the trial expiration cutoff.
   * @return A list of subscriptions with expired trials.
   */
  @Query(
      "SELECT s FROM Subscription s WHERE s.status IN ('TRIALING', 'ACTIVE') AND s.trialEnd IS NOT NULL AND s.trialEnd < :cutoffDate")
  List<Subscription> findExpiredTrials(@Param("cutoffDate") Instant cutoffDate);

  /**
   * Finds active subscriptions that are due for renewal within a specific date range.
   *
   * @param startDate The start of the renewal window.
   * @param endDate The end of the renewal window.
   * @return A list of subscriptions due for renewal.
   */
  @Query(
      "SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.currentPeriodEnd BETWEEN :startDate AND :endDate")
  List<Subscription> findSubscriptionsForRenewal(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /**
   * Counts the number of active and trialing subscriptions for a specific plan.
   *
   * @param planId The ID of the plan.
   * @return The total count of active subscriptions for the plan.
   */
  @Query(
      "SELECT COUNT(s) FROM Subscription s WHERE s.planId = :planId AND s.status IN ('TRIALING', 'ACTIVE')")
  long countActiveSubscriptionsByPlan(@Param("planId") UUID planId);

  /**
   * Counts the total number of subscriptions with a specific status.
   *
   * @param status The status to count.
   * @return The total count of subscriptions with the given status.
   */
  @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = :status")
  long countSubscriptionsByStatus(@Param("status") Subscription.Status status);

  /**
   * Finds past-due subscriptions that are older than a specific cutoff date.
   *
   * @param cutoffDate The date to consider as the cutoff.
   * @return A list of old, past-due subscriptions.
   */
  @Query(
      "SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE' AND s.currentPeriodEnd < :cutoffDate")
  List<Subscription> findPastDueSubscriptionsOlderThan(@Param("cutoffDate") Instant cutoffDate);

  /**
   * Finds all subscriptions for an organization, ordered by creation date descending.
   *
   * @param organizationId The ID of the organization.
   * @return A list of subscriptions for the organization.
   */
  @Query(
      "SELECT s FROM Subscription s WHERE s.organizationId = :organizationId ORDER BY s.createdAt DESC")
  List<Subscription> findByOrganizationIdOrderByCreatedAtDesc(
      @Param("organizationId") UUID organizationId);

  /**
   * Finds active subscriptions that are scheduled to be canceled on or before a given date.
   *
   * @param cutoffDate The date to check for scheduled cancellations.
   * @return A list of subscriptions to be canceled.
   */
  @Query(
      "SELECT s FROM Subscription s WHERE s.cancelAt IS NOT NULL AND s.cancelAt <= :cutoffDate AND s.status = 'ACTIVE'")
  List<Subscription> findSubscriptionsToCancel(@Param("cutoffDate") Instant cutoffDate);
}
