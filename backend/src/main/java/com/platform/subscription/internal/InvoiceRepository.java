package com.platform.subscription.internal;

import com.platform.payment.internal.Invoice;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Invoice} entities.
 *
 * <p>This interface provides methods for querying and managing invoice records in the database,
 * including finding invoices by various criteria and performing analytical queries.
 * </p>
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

  /**
   * Finds an invoice by its unique Stripe invoice ID.
   *
   * @param stripeInvoiceId The ID of the invoice from the Stripe API.
   * @return An {@link Optional} containing the matching {@link Invoice}, or empty if not found.
   */
  Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId);

  /**
   * Finds all invoices for a given organization, ordered by creation date descending.
   *
   * @param organizationId The ID of the organization.
   * @return A list of {@link Invoice}s.
   */
  List<Invoice> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

  /**
   * Finds all invoices for a given organization with a specific status.
   *
   * @param organizationId The ID of the organization.
   * @param status The invoice status to filter by.
   * @return A list of matching {@link Invoice}s.
   */
  List<Invoice> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
      UUID organizationId, Invoice.Status status);

  /**
   * Finds all invoices associated with a specific subscription.
   *
   * @param subscriptionId The ID of the subscription.
   * @return A list of associated {@link Invoice}s.
   */
  List<Invoice> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

  /**
   * Finds all invoices with a given status that are past their due date.
   *
   * @param status The status to filter by (e.g., "OPEN").
   * @param cutoffDate The current date, used as the cutoff for determining if an invoice is
   *     overdue.
   * @return A list of overdue invoices.
   */
  @Query("SELECT i FROM Invoice i WHERE i.status = :status AND i.dueDate < :cutoffDate")
  List<Invoice> findOverdueInvoices(
      @Param("status") Invoice.Status status, @Param("cutoffDate") Instant cutoffDate);

  /**
   * Finds all invoices for an organization within a specific date range.
   *
   * @param organizationId The ID of the organization.
   * @param startDate The start of the date range.
   * @param endDate The end of the date range.
   * @return A list of matching {@link Invoice}s.
   */
  @Query(
      "SELECT i FROM Invoice i WHERE i.organizationId = :organizationId AND i.createdAt BETWEEN :startDate AND :endDate ORDER BY i.createdAt DESC")
  List<Invoice> findByOrganizationIdAndDateRange(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * Calculates the sum of all paid invoice amounts for an organization.
   *
   * @param organizationId The ID of the organization.
   * @return The total sum of paid invoices.
   */
  @Query(
      "SELECT SUM(i.totalAmount.amount) FROM Invoice i WHERE i.organizationId = :organizationId AND i.status = 'PAID'")
  Long sumPaidInvoiceAmountsByOrganization(@Param("organizationId") UUID organizationId);

  /**
   * Calculates the sum of paid invoice amounts for an organization within a date range.
   *
   * @param organizationId The ID of the organization.
   * @param startDate The start of the date range.
   * @param endDate The end of the date range.
   * @return The total sum of paid invoices in the given range.
   */
  @Query(
      "SELECT SUM(i.totalAmount.amount) FROM Invoice i WHERE i.organizationId = :organizationId AND i.status = 'PAID' AND i.createdAt BETWEEN :startDate AND :endDate")
  Long sumPaidInvoiceAmountsByOrganizationAndDateRange(
      @Param("organizationId") UUID organizationId,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);

  /**
   * Counts the total number of paid invoices for an organization.
   *
   * @param organizationId The ID of the organization.
   * @return The count of paid invoices.
   */
  @Query(
      "SELECT COUNT(i) FROM Invoice i WHERE i.organizationId = :organizationId AND i.status = 'PAID'")
  long countPaidInvoicesByOrganization(@Param("organizationId") UUID organizationId);

  /**
   * Finds all open invoices that are due within a specific date range.
   *
   * @param startDate The start of the date range.
   * @param endDate The end of the date range.
   * @return A list of invoices that are due soon.
   */
  @Query(
      "SELECT i FROM Invoice i WHERE i.status = 'OPEN' AND i.dueDate BETWEEN :startDate AND :endDate ORDER BY i.dueDate ASC")
  List<Invoice> findInvoicesDueSoon(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /**
   * Finds draft invoices that have not been finalized and are older than a cutoff date.
   *
   * @param cutoffDate The timestamp to consider drafts as stale.
   * @return A list of stale draft invoices.
   */
  @Query("SELECT i FROM Invoice i WHERE i.status = 'DRAFT' AND i.createdAt < :cutoffDate")
  List<Invoice> findStaleDraftInvoices(@Param("cutoffDate") Instant cutoffDate);
}
