package com.platform.payment.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link PaymentMethod} entities.
 *
 * <p>This interface provides methods for querying and managing payment method records in the
 * database. It includes queries for finding payment methods by organization, Stripe ID, and default
 * status, ensuring that soft-deleted records are properly handled.
 * </p>
 */
@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

  /**
   * Finds all active (not deleted) payment methods for a given organization, ordered by creation
   * date in descending order.
   *
   * @param organizationId The ID of the organization.
   * @return A list of {@link PaymentMethod}s.
   */
  List<PaymentMethod> findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID organizationId);

  /**
   * Finds a payment method by its unique Stripe payment method ID.
   *
   * @param stripePaymentMethodId The ID from Stripe.
   * @return An {@link Optional} containing the matching {@link PaymentMethod}, or empty if not
   *     found.
   */
  Optional<PaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId);

  /**
   * Finds the default active payment method for a given organization.
   *
   * @param organizationId The ID of the organization.
   * @return An {@link Optional} containing the default {@link PaymentMethod}, or empty if none is
   *     set.
   */
  Optional<PaymentMethod> findByOrganizationIdAndIsDefaultTrueAndDeletedAtIsNull(
      UUID organizationId);

  /**
   * Finds all active payment methods for a given organization, filtered by type.
   *
   * @param organizationId The ID of the organization.
   * @param type The {@link PaymentMethod.Type} to filter by.
   * @return A list of matching {@link PaymentMethod}s.
   */
  @Query(
      "SELECT pm FROM PaymentMethod pm WHERE pm.organizationId = :organizationId AND pm.type = :type AND pm.deletedAt IS NULL ORDER BY pm.createdAt DESC")
  List<PaymentMethod> findByOrganizationIdAndType(
      @Param("organizationId") UUID organizationId, @Param("type") PaymentMethod.Type type);

  /**
   * Counts the number of active payment methods for a given organization.
   *
   * @param organizationId The ID of the organization.
   * @return The total count of active payment methods.
   */
  @Query(
      "SELECT COUNT(pm) FROM PaymentMethod pm WHERE pm.organizationId = :organizationId AND pm.deletedAt IS NULL")
  long countActivePaymentMethodsByOrganization(@Param("organizationId") UUID organizationId);

  /**
   * Finds an active payment method by its organization ID and Stripe payment method ID.
   *
   * @param organizationId The ID of the organization.
   * @param stripePaymentMethodId The ID from Stripe.
   * @return An {@link Optional} containing the matching {@link PaymentMethod}, or empty if not
   *     found.
   */
  @Query(
      "SELECT pm FROM PaymentMethod pm WHERE pm.organizationId = :organizationId AND pm.stripePaymentMethodId = :stripePaymentMethodId AND pm.deletedAt IS NULL")
  Optional<PaymentMethod> findByOrganizationIdAndStripePaymentMethodId(
      @Param("organizationId") UUID organizationId,
      @Param("stripePaymentMethodId") String stripePaymentMethodId);
}
