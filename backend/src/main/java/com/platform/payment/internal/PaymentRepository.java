package com.platform.payment.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

  List<Payment> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

  List<Payment> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
      UUID organizationId, Payment.Status status);

  @Query(
      "SELECT p FROM Payment p WHERE p.organizationId = :organizationId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
  List<Payment> findByOrganizationIdAndDateRange(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt < :cutoffDate")
  List<Payment> findOldPaymentsByStatus(
      @Param("status") Payment.Status status, @Param("cutoffDate") Instant cutoffDate);

  @Query(
      "SELECT COUNT(p) FROM Payment p WHERE p.organizationId = :organizationId AND p.status = 'SUCCEEDED'")
  long countSuccessfulPaymentsByOrganization(@Param("organizationId") UUID organizationId);

  @Query(
      "SELECT SUM(p.amount.amount) FROM Payment p WHERE p.organizationId = :organizationId AND p.status = 'SUCCEEDED'")
  Long sumSuccessfulPaymentAmountsByOrganization(@Param("organizationId") UUID organizationId);

  @Query(
      "SELECT SUM(p.amount.amount) FROM Payment p WHERE p.organizationId = :organizationId AND p.status = 'SUCCEEDED' AND p.createdAt BETWEEN :startDate AND :endDate")
  Long sumSuccessfulPaymentAmountsByOrganizationAndDateRange(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  @Query("SELECT p FROM Payment p WHERE p.invoiceId = :invoiceId ORDER BY p.createdAt DESC")
  List<Payment> findByInvoiceId(@Param("invoiceId") UUID invoiceId);

  @Query(
      "SELECT p FROM Payment p WHERE p.status IN ('PENDING', 'PROCESSING') AND p.createdAt < :cutoffDate")
  List<Payment> findStaleProcessingPayments(@Param("cutoffDate") Instant cutoffDate);

  // ===== ADVANCED PAYMENT ANALYTICS =====

  /** Revenue analytics by time period */
  @Query(value = """
      SELECT
          DATE_TRUNC(:period, created_at) as time_period,
          COUNT(*) as payment_count,
          SUM(amount) as total_revenue,
          AVG(amount) as average_payment,
          COUNT(DISTINCT organization_id) as unique_organizations
      FROM payments
      WHERE status = 'SUCCEEDED'
          AND created_at BETWEEN :startDate AND :endDate
      GROUP BY DATE_TRUNC(:period, created_at)
      ORDER BY time_period
      """, nativeQuery = true)
  List<Object[]> getRevenueAnalytics(
      @Param("period") String period, // 'day', 'week', 'month', 'quarter'
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /** Payment failure analysis */
  @Query(value = """
      SELECT
          p.status,
          COUNT(*) as payment_count,
          ROUND((COUNT(*) * 100.0 / SUM(COUNT(*)) OVER()), 2) as percentage,
          AVG(p.amount) as average_amount
      FROM payments p
      WHERE p.created_at BETWEEN :startDate AND :endDate
      GROUP BY p.status
      ORDER BY payment_count DESC
      """, nativeQuery = true)
  List<Object[]> getPaymentStatusDistribution(
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /** Top performing organizations by revenue */
  @Query(value = """
      SELECT
          o.name as organization_name,
          COUNT(p.id) as payment_count,
          SUM(p.amount) as total_revenue,
          AVG(p.amount) as average_payment,
          MAX(p.created_at) as last_payment_date
      FROM payments p
      JOIN organizations o ON p.organization_id = o.id
      WHERE p.status = 'SUCCEEDED'
          AND p.created_at >= :startDate
      GROUP BY o.id, o.name
      ORDER BY total_revenue DESC
      LIMIT :topN
      """, nativeQuery = true)
  List<Object[]> getTopOrganizationsByRevenue(
      @Param("startDate") Instant startDate,
      @Param("topN") int topN);

  /** Payment method analysis */
  @Query(value = """
      SELECT
          pm.type as payment_method_type,
          pm.brand as payment_method_brand,
          COUNT(p.id) as usage_count,
          SUM(p.amount) as total_amount,
          AVG(p.amount) as average_amount
      FROM payments p
      JOIN payment_methods pm ON p.payment_method_id = pm.id
      WHERE p.status = 'SUCCEEDED'
          AND p.created_at >= :startDate
      GROUP BY pm.type, pm.brand
      ORDER BY usage_count DESC
      """, nativeQuery = true)
  List<Object[]> getPaymentMethodAnalytics(@Param("startDate") Instant startDate);

  /** Cohort analysis for payment behavior */
  @Query(value = """
      SELECT
          DATE_TRUNC('month', first_payment.created_at) as cohort_month,
          EXTRACT(EPOCH FROM (DATE_TRUNC('month', p.created_at) - DATE_TRUNC('month', first_payment.created_at))) / (30.44 * 24 * 3600) as month_number,
          COUNT(DISTINCT p.organization_id) as active_organizations,
          SUM(p.amount) as revenue
      FROM payments p
      JOIN (
          SELECT organization_id, MIN(created_at) as created_at
          FROM payments
          WHERE status = 'SUCCEEDED'
          GROUP BY organization_id
      ) first_payment ON p.organization_id = first_payment.organization_id
      WHERE p.status = 'SUCCEEDED'
          AND DATE_TRUNC('month', first_payment.created_at) >= :cohortStartDate
      GROUP BY
          DATE_TRUNC('month', first_payment.created_at),
          EXTRACT(EPOCH FROM (DATE_TRUNC('month', p.created_at) - DATE_TRUNC('month', first_payment.created_at))) / (30.44 * 24 * 3600)
      ORDER BY cohort_month, month_number
      """, nativeQuery = true)
  List<Object[]> getPaymentCohortAnalysis(@Param("cohortStartDate") Instant cohortStartDate);

  /** Churn analysis - organizations that stopped paying */
  @Query(value = """
      SELECT
          o.name as organization_name,
          MAX(p.created_at) as last_payment_date,
          COUNT(p.id) as total_payments,
          SUM(p.amount) as total_spent,
          EXTRACT(EPOCH FROM (NOW() - MAX(p.created_at))) / 86400 as days_since_last_payment
      FROM payments p
      JOIN organizations o ON p.organization_id = o.id
      WHERE p.status = 'SUCCEEDED'
      GROUP BY o.id, o.name
      HAVING MAX(p.created_at) < :churnThreshold
      ORDER BY last_payment_date DESC
      """, nativeQuery = true)
  List<Object[]> getChurnedOrganizations(@Param("churnThreshold") Instant churnThreshold);

  /** Payment velocity analysis */
  @Query(value = """
      SELECT
          EXTRACT(DOW FROM created_at) as day_of_week,
          EXTRACT(HOUR FROM created_at) as hour_of_day,
          COUNT(*) as payment_count,
          SUM(amount) as total_amount
      FROM payments
      WHERE status = 'SUCCEEDED'
          AND created_at >= :startDate
      GROUP BY EXTRACT(DOW FROM created_at), EXTRACT(HOUR FROM created_at)
      ORDER BY day_of_week, hour_of_day
      """, nativeQuery = true)
  List<Object[]> getPaymentVelocityPatterns(@Param("startDate") Instant startDate);

  /** Fraud detection - suspicious payment patterns */
  @Query(value = """
      SELECT
          organization_id,
          COUNT(*) as failed_attempts,
          COUNT(DISTINCT stripe_payment_intent_id) as unique_intents,
          MAX(created_at) as last_attempt,
          STRING_AGG(DISTINCT status, ', ') as statuses
      FROM payments
      WHERE status IN ('FAILED', 'CANCELLED')
          AND created_at >= :lookbackDate
      GROUP BY organization_id
      HAVING COUNT(*) >= :failureThreshold
      ORDER BY failed_attempts DESC
      """, nativeQuery = true)
  List<Object[]> getSuspiciousPaymentActivity(
      @Param("lookbackDate") Instant lookbackDate,
      @Param("failureThreshold") int failureThreshold);

  /** Revenue forecasting data */
  @Query(value = """
      SELECT
          DATE_TRUNC('week', created_at) as week,
          SUM(amount) as weekly_revenue,
          COUNT(*) as weekly_payments,
          COUNT(DISTINCT organization_id) as active_organizations
      FROM payments
      WHERE status = 'SUCCEEDED'
          AND created_at >= :startDate
      GROUP BY DATE_TRUNC('week', created_at)
      ORDER BY week
      """, nativeQuery = true)
  List<Object[]> getWeeklyRevenueMetrics(@Param("startDate") Instant startDate);

  /** Payment recovery analysis - succeeded after failures */
  @Query(value = """
      WITH payment_attempts AS (
          SELECT
              organization_id,
              stripe_payment_intent_id,
              MIN(created_at) as first_attempt,
              MAX(created_at) as last_attempt,
              COUNT(*) as attempt_count,
              MAX(CASE WHEN status = 'SUCCEEDED' THEN created_at END) as success_date
          FROM payments
          WHERE created_at >= :startDate
          GROUP BY organization_id, stripe_payment_intent_id
          HAVING COUNT(*) > 1
      )
      SELECT
          COUNT(*) as multi_attempt_payments,
          SUM(CASE WHEN success_date IS NOT NULL THEN 1 ELSE 0 END) as eventually_succeeded,
          ROUND(
              SUM(CASE WHEN success_date IS NOT NULL THEN 1 ELSE 0 END) * 100.0 / COUNT(*),
              2
          ) as recovery_rate,
          AVG(attempt_count) as avg_attempts_per_payment
      FROM payment_attempts
      """, nativeQuery = true)
  List<Object[]> getPaymentRecoveryMetrics(@Param("startDate") Instant startDate);
}
