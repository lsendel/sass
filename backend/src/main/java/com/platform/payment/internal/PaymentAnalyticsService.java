package com.platform.payment.internal;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for payment analytics with SQL injection protection.
 * Validates and sanitizes all user inputs before executing queries.
 */
@Service
public class PaymentAnalyticsService {

    private static final Set<String> ALLOWED_PERIODS = Set.of("day", "week", "month", "quarter", "year");

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Get revenue analytics with validated period parameter.
     * Prevents SQL injection by validating period against whitelist.
     */
    public List<Object[]> getRevenueAnalytics(String period, Instant startDate, Instant endDate) {
        // Validate period parameter to prevent SQL injection
        String validatedPeriod = validatePeriod(period);

        // Validate date range
        validateDateRange(startDate, endDate);

        return paymentRepository.getRevenueAnalyticsSecure(validatedPeriod, startDate, endDate);
    }

    /**
     * Get payment status distribution with validated inputs.
     */
    public List<Object[]> getPaymentStatusDistribution(Instant startDate, Instant endDate) {
        validateDateRange(startDate, endDate);
        return paymentRepository.getPaymentStatusDistribution(startDate, endDate);
    }

    /**
     * Get top performing organizations with validated inputs.
     */
    public List<Object[]> getTopPerformingOrganizations(Instant startDate, Instant endDate, int limit) {
        validateDateRange(startDate, endDate);
        validateLimit(limit);
        return paymentRepository.getTopPerformingOrganizations(startDate, endDate, limit);
    }

    private String validatePeriod(String period) {
        if (period == null || period.trim().isEmpty()) {
            throw new IllegalArgumentException("Period cannot be null or empty");
        }

        String normalizedPeriod = period.trim().toLowerCase();

        if (!ALLOWED_PERIODS.contains(normalizedPeriod)) {
            throw new IllegalArgumentException(
                "Invalid period: " + period + ". Allowed values: " + ALLOWED_PERIODS
            );
        }

        return normalizedPeriod;
    }

    private void validateDateRange(Instant startDate, Instant endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        // Limit to reasonable date range (e.g., 5 years maximum)
        Instant maxStartDate = Instant.now().minusSeconds(5 * 365 * 24 * 60 * 60L);
        if (startDate.isBefore(maxStartDate)) {
            throw new IllegalArgumentException("Start date cannot be more than 5 years ago");
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0 || limit > 1000) {
            throw new IllegalArgumentException("Limit must be between 1 and 1000");
        }
    }
}