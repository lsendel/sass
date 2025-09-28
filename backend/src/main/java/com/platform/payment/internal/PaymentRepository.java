package com.platform.payment.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Payment} entities.
 *
 * <p>This interface provides a comprehensive set of methods for querying payment data, including
 * standard finder methods and complex native queries for advanced analytics and reporting.
 * </p>
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  /**
   * Finds a payment by its unique Stripe PaymentIntent ID.
   *
   * @param stripePaymentIntentId The ID from Stripe.
   * @return An {@link Optional} containing the matching {@link Payment}, or empty if not found.
   */
  Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

  /**
   * Finds all payments for a given organization, ordered by creation date descending.
   *
   * @param organizationId The ID of the organization.
   * @return A list of {@link Payment}s.
   */
  List<Payment> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

  /**
   * Finds all payments for a given organization with a specific status.
   *
   * @param organizationId The ID of the organization.
   * @param status The payment status to filter by.
   * @return A list of matching {@link Payment}s.
   */
  List<Payment> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
      UUID organizationId, Payment.Status status);

  /**
   * Finds all payments for an organization within a specific date range.
   *
   * @param organizationId The ID of the organization.
   * @param startDate The start of the date range.
   * @param endDate The end of the date range.
   * @return A list of matching {@link Payment}s.
   */
  @Query(
      "SELECT p FROM Payment p WHERE p.organizationId = :organizationId AND p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
  List<Payment> findByOrganizationIdAndDateRange(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * Finds payments with a specific status that are older than a given cutoff date.
   *
   * @param status The status to filter by.
   * @param cutoffDate The cutoff date.
   * @return A list of old payments matching the criteria.
   */
  @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt < :cutoffDate")
  List<Object[]> findOldPaymentsByStatus(
      @Param("status") Payment.Status status, @Param("cutoffDate") Instant cutoffDate);

  /**
   * Counts the total number of successful payments for an organization.
   *
   * @param organizationId The ID of the organization.
   * @return The count of successful payments.
   */
  @Query(
      "SELECT COUNT(p) FROM Payment p WHERE p.organizationId = :organizationId AND p.status = 'SUCCEEDED'")
  long countSuccessfulPaymentsByOrganization(@Param("organizationId") UUID organizationId);

  /**
   * Calculates the sum of all successful payment amounts for an organization.
   *
   * @param organizationId The ID of the organization.
   * @return The total sum of successful payments.
   */
  @Query(
      "SELECT SUM(p.amount.amount) FROM Payment p WHERE p.organizationId = :organizationId AND p.status = 'SUCCEEDED'")
  Long sumSuccessfulPaymentAmountsByOrganization(@Param("organizationId") UUID organizationId);

  /**
   * Calculates the sum of successful payment amounts for an organization within a date range.
   *
   * @param organizationId The ID of the organization.
   * @param startDate The start of the date range.
   * @param endDate The end of the date range.
   * @return The total sum of successful payments in the given range.
   */
  @Query(
      "SELECT SUM(p.amount.amount) FROM Payment p WHERE p.organizationId = :organizationId AND p.status = 'SUCCEEDED' AND p.createdAt BETWEEN :startDate AND :endDate")
  Long sumSuccessfulPaymentAmountsByOrganizationAndDateRange(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * Finds all payments associated with a specific invoice.
   *
   * @param invoiceId The ID of the invoice.
   * @return A list of associated {@link Payment}s.
   */
  @Query("SELECT p FROM Payment p WHERE p.invoiceId = :invoiceId ORDER BY p.createdAt DESC")
  List<Payment> findByInvoiceId(@Param("invoiceId") UUID invoiceId);

  /**
   * Finds payments that have been in a pending or processing state for too long.
   *
   * @param cutoffDate The timestamp to consider payments as stale.
   * @return A list of stale payments.
   */
  @Query(
      "SELECT p FROM Payment p WHERE p.status IN ('PENDING', 'PROCESSING') AND p.createdAt < :cutoffDate")
  List<Payment> findStaleProcessingPayments(@Param("cutoffDate") Instant cutoffDate);

  // ===== ADVANCED PAYMENT ANALYTICS =====

  /**
   * Calculates revenue analytics, grouping by a specified time period.
   *
   * @param period The time period for grouping (e.g., 'day', 'week'). Must be validated by the
   *     service layer.
   * @param startDate The start of the analysis window.
   * @param endDate The end of the analysis window.
   * @return A list of results, each containing the time period, payment count, total revenue,
   *     average payment, and count of unique organizations.
   */
  @Query(
      value =
          """
      SELECT
          DATE_TRUNC(CAST(:period AS VARCHAR), created_at) as time_period,
          COUNT(*) as payment_count,
          SUM(amount) as total_revenue,
          AVG(amount) as average_payment,
          COUNT(DISTINCT organization_id) as unique_organizations
      FROM payments
      WHERE status = 'SUCCEEDED'
          AND created_at BETWEEN :startDate AND :endDate
      GROUP BY DATE_TRUNC(CAST(:period AS VARCHAR), created_at)
      ORDER BY time_period
      """,
      nativeQuery = true)
  List<Object[]> getRevenueAnalyticsSecure(
      @Param("period") String period,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * Calculates the distribution of payment statuses within a date range.
   *
   * @param startDate The start of the analysis window.
   * @param endDate The end of the analysis window.
   * @return A list of results, each containing the status, count, percentage, and average amount.
   */
  @Query(
      value =
          """
      SELECT
          p.status,
          COUNT(*) as payment_count,
          ROUND((COUNT(*) * 100.0 / SUM(COUNT(*)) OVER()), 2) as percentage,
          AVG(p.amount) as average_amount
      FROM payments p
      WHERE p.created_at BETWEEN :startDate AND :endDate
      GROUP BY p.status
      ORDER BY payment_count DESC
      """,
      nativeQuery = true)
  List<Object[]> getPaymentStatusDistribution(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /**
   * Identifies the top-performing organizations by revenue.
   *
   * @param startDate The start of the analysis window.
   * @param topN The number of top organizations to return.
   * @return A list of results, each containing the organization name, payment count, total revenue,
   *     average payment, and last payment date.
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getTopPerformingOrganizations(
      @Param("startDate") Instant startDate, @Param("topN") int topN);

  /** Alias for getTopOrganizationsByRevenue for backwards compatibility */
  default List<Object[]> getTopPerformingOrganizations(Instant startDate, Instant endDate, int limit) {
      return getTopPerformingOrganizations(startDate, limit);
  }

  /**
   * Analyzes the performance of different payment methods.
   *
   * @param startDate The start of the analysis window.
   * @return A list of results, each containing the payment method type, brand, usage count, total
   *     amount, and average amount.
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getPaymentMethodAnalytics(@Param("startDate") Instant startDate);

  /**
   * Performs a cohort analysis of payment behavior over time.
   *
   * @param cohortStartDate The earliest month to include in the cohort analysis.
   * @return A list of results, each containing the cohort month, month number, number of active
   *     organizations, and revenue for that cohort and month.
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getPaymentCohortAnalysis(@Param("cohortStartDate") Instant cohortStartDate);

  /**
   * Identifies organizations that have stopped paying (churned).
   *
   * @param churnThreshold The timestamp before which a last payment is considered churned.
   * @return A list of results, each containing details about a churned organization.
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getChurnedOrganizations(@Param("churnThreshold") Instant churnThreshold);

  /**
   * Analyzes payment velocity patterns by day of the week and hour of the day.
   *
   * @param startDate The start of the analysis window.
   * @return A list of results, each containing the day, hour, payment count, and total amount.
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getPaymentVelocityPatterns(@Param("startDate") Instant startDate);

  /**
   * Detects suspicious payment activity based on a high number of failed attempts.
   *
   * @param lookbackDate The start of the analysis window.
   * @param failureThreshold The minimum number of failed attempts to be considered suspicious.
   * @return A list of results identifying organizations with suspicious activity.
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getSuspiciousPaymentActivity(
      @Param("lookbackDate") Instant lookbackDate, @Param("failureThreshold") int failureThreshold);

  /**
   * Gathers weekly revenue metrics for forecasting purposes.
   *
   * @param startDate The start of the analysis window.
   * @return A list of weekly revenue and payment data.
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getWeeklyRevenueMetrics(@Param("startDate") Instant startDate);

  /**
   * Analyzes payment recovery rates (payments that succeed after initial failures).
   *
   * @param startDate The start of the analysis window.
   * @return A list containing aggregated recovery metrics.
   */
  @Query(
      value =
          """
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
      """,
      nativeQuery = true)
  List<Object[]> getPaymentRecoveryMetrics(@Param("startDate") Instant startDate);
}
