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
}
