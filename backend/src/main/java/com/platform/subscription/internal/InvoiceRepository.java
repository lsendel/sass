package com.platform.subscription.internal;

import com.platform.payment.internal.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId);

    List<Invoice> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<Invoice> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, Invoice.Status status);

    List<Invoice> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

    @Query("SELECT i FROM Invoice i WHERE i.status = :status AND i.dueDate < :cutoffDate")
    List<Invoice> findOverdueInvoices(@Param("status") Invoice.Status status,
                                     @Param("cutoffDate") Instant cutoffDate);

    @Query("SELECT i FROM Invoice i WHERE i.organizationId = :organizationId AND i.createdAt BETWEEN :startDate AND :endDate ORDER BY i.createdAt DESC")
    List<Invoice> findByOrganizationIdAndDateRange(@Param("organizationId") UUID organizationId,
                                                   @Param("startDate") Instant startDate,
                                                   @Param("endDate") Instant endDate);

    @Query("SELECT SUM(i.totalAmount.amount) FROM Invoice i WHERE i.organizationId = :organizationId AND i.status = 'PAID'")
    Long sumPaidInvoiceAmountsByOrganization(@Param("organizationId") UUID organizationId);

    @Query("SELECT SUM(i.totalAmount.amount) FROM Invoice i WHERE i.organizationId = :organizationId AND i.status = 'PAID' AND i.createdAt BETWEEN :startDate AND :endDate")
    Long sumPaidInvoiceAmountsByOrganizationAndDateRange(@Param("organizationId") UUID organizationId,
                                                        @Param("startDate") Instant startDate,
                                                        @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.organizationId = :organizationId AND i.status = 'PAID'")
    long countPaidInvoicesByOrganization(@Param("organizationId") UUID organizationId);

    @Query("SELECT i FROM Invoice i WHERE i.status = 'OPEN' AND i.dueDate BETWEEN :startDate AND :endDate ORDER BY i.dueDate ASC")
    List<Invoice> findInvoicesDueSoon(@Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate);

    @Query("SELECT i FROM Invoice i WHERE i.status = 'DRAFT' AND i.createdAt < :cutoffDate")
    List<Invoice> findStaleDraftInvoices(@Param("cutoffDate") Instant cutoffDate);
}