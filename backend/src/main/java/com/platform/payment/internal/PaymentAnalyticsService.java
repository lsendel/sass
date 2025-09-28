package com.platform.payment.internal;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Provides services for payment analytics with a strong focus on security.
 *
 * <p>This service is responsible for generating analytical data based on payment history. A key
 * feature of this service is its robust input validation to prevent security vulnerabilities,
 * particularly SQL injection. All user-provided parameters, such as time periods and date ranges,
 * are strictly validated against a whitelist before being used in database queries.
 * </p>
 */
@Service
public class PaymentAnalyticsService {

  private static final Set<String> ALLOWED_PERIODS =
      Set.of("day", "week", "month", "quarter", "year");

  @Autowired private PaymentRepository paymentRepository;

  /**
   * Retrieves revenue analytics data grouped by a specified time period.
   *
   * @param period The time period to group by (e.g., "day", "week"). This value is validated
   *     against a whitelist to prevent SQL injection.
   * @param startDate The start of the date range for the analysis.
   * @param endDate The end of the date range for the analysis.
   * @return A list of object arrays, where each array represents a data point in the time series.
   * @throws IllegalArgumentException if the period or date range is invalid.
   */
  public List<Object[]> getRevenueAnalytics(String period, Instant startDate, Instant endDate) {
    String validatedPeriod = validatePeriod(period);
    validateDateRange(startDate, endDate);
    return paymentRepository.getRevenueAnalyticsSecure(validatedPeriod, startDate, endDate);
  }

  /**
   * Retrieves the distribution of payment statuses within a given date range.
   *
   * @param startDate The start of the date range.
   * @param endDate The end of the date range.
   * @return A list of object arrays, where each array contains a payment status and its count.
   * @throws IllegalArgumentException if the date range is invalid.
   */
  public List<Object[]> getPaymentStatusDistribution(Instant startDate, Instant endDate) {
    validateDateRange(startDate, endDate);
    return paymentRepository.getPaymentStatusDistribution(startDate, endDate);
  }

  /**
   * Retrieves the top-performing organizations based on payment volume within a date range.
   *
   * @param startDate The start of the date range.
   * @param endDate The end of the date range.
   * @param limit The maximum number of organizations to return.
   * @return A list of object arrays, where each array contains organization details and their total
   *     payment volume.
   * @throws IllegalArgumentException if the date range or limit is invalid.
   */
  public List<Object[]> getTopPerformingOrganizations(
      Instant startDate, Instant endDate, int limit) {
    validateDateRange(startDate, endDate);
    validateLimit(limit);
    return paymentRepository.getTopPerformingOrganizations(startDate, endDate, limit);
  }

  /**
   * Validates that the provided period string is in a whitelist of allowed values.
   *
   * @param period The period string to validate.
   * @return The normalized, validated period string.
   * @throws IllegalArgumentException if the period is null, empty, or not in the whitelist.
   */
  private String validatePeriod(String period) {
    if (period == null || period.trim().isEmpty()) {
      throw new IllegalArgumentException("Period cannot be null or empty");
    }
    String normalizedPeriod = period.trim().toLowerCase();
    if (!ALLOWED_PERIODS.contains(normalizedPeriod)) {
      throw new IllegalArgumentException(
          "Invalid period: " + period + ". Allowed values: " + ALLOWED_PERIODS);
    }
    return normalizedPeriod;
  }

  /**
   * Validates the start and end dates for a query.
   *
   * @param startDate The start date of the range.
   * @param endDate The end date of the range.
   * @throws IllegalArgumentException if the dates are null, if start is after end, or if the range
   *     is unreasonably large.
   */
  private void validateDateRange(Instant startDate, Instant endDate) {
    if (startDate == null || endDate == null) {
      throw new IllegalArgumentException("Start date and end date cannot be null");
    }
    if (startDate.isAfter(endDate)) {
      throw new IllegalArgumentException("Start date cannot be after end date");
    }
    Instant maxStartDate = Instant.now().minusSeconds(5 * 365 * 24 * 60 * 60L);
    if (startDate.isBefore(maxStartDate)) {
      throw new IllegalArgumentException("Start date cannot be more than 5 years ago");
    }
  }

  /**
   * Validates the limit parameter for a query.
   *
   * @param limit The limit value to validate.
   * @throws IllegalArgumentException if the limit is not within the allowed range (1-1000).
   */
  private void validateLimit(int limit) {
    if (limit <= 0 || limit > 1000) {
      throw new IllegalArgumentException("Limit must be between 1 and 1000");
    }
  }
}