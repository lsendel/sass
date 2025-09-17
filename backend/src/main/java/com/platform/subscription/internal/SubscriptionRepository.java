package com.platform.subscription.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  Optional<Subscription> findByOrganizationId(UUID organizationId);

  Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

  List<Subscription> findByPlanIdAndStatus(UUID planId, Subscription.Status status);

  @Query(
      "SELECT s FROM Subscription s WHERE s.status = :status AND s.currentPeriodEnd < :cutoffDate")
  List<Subscription> findExpiredSubscriptions(
      @Param("status") Subscription.Status status, @Param("cutoffDate") Instant cutoffDate);

  @Query(
      "SELECT s FROM Subscription s WHERE s.status IN ('TRIALING', 'ACTIVE') AND s.trialEnd IS NOT NULL AND s.trialEnd < :cutoffDate")
  List<Subscription> findExpiredTrials(@Param("cutoffDate") Instant cutoffDate);

  @Query(
      "SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.currentPeriodEnd BETWEEN :startDate AND :endDate")
  List<Subscription> findSubscriptionsForRenewal(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  @Query(
      "SELECT COUNT(s) FROM Subscription s WHERE s.planId = :planId AND s.status IN ('TRIALING', 'ACTIVE')")
  long countActiveSubscriptionsByPlan(@Param("planId") UUID planId);

  @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = :status")
  long countSubscriptionsByStatus(@Param("status") Subscription.Status status);

  @Query(
      "SELECT s FROM Subscription s WHERE s.status = 'PAST_DUE' AND s.currentPeriodEnd < :cutoffDate")
  List<Subscription> findPastDueSubscriptionsOlderThan(@Param("cutoffDate") Instant cutoffDate);

  @Query(
      "SELECT s FROM Subscription s WHERE s.organizationId = :organizationId ORDER BY s.createdAt DESC")
  List<Subscription> findByOrganizationIdOrderByCreatedAtDesc(
      @Param("organizationId") UUID organizationId);

  @Query(
      "SELECT s FROM Subscription s WHERE s.cancelAt IS NOT NULL AND s.cancelAt <= :cutoffDate AND s.status = 'ACTIVE'")
  List<Subscription> findSubscriptionsToCancel(@Param("cutoffDate") Instant cutoffDate);
}
