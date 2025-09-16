package com.platform.payment.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    List<PaymentMethod> findByOrganizationIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID organizationId);

    Optional<PaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId);

    Optional<PaymentMethod> findByOrganizationIdAndIsDefaultTrueAndDeletedAtIsNull(UUID organizationId);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.organizationId = :organizationId AND pm.type = :type AND pm.deletedAt IS NULL ORDER BY pm.createdAt DESC")
    List<PaymentMethod> findByOrganizationIdAndType(@Param("organizationId") UUID organizationId,
                                                    @Param("type") PaymentMethod.Type type);

    @Query("SELECT COUNT(pm) FROM PaymentMethod pm WHERE pm.organizationId = :organizationId AND pm.deletedAt IS NULL")
    long countActivePaymentMethodsByOrganization(@Param("organizationId") UUID organizationId);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.organizationId = :organizationId AND pm.stripePaymentMethodId = :stripePaymentMethodId AND pm.deletedAt IS NULL")
    Optional<PaymentMethod> findByOrganizationIdAndStripePaymentMethodId(@Param("organizationId") UUID organizationId,
                                                                         @Param("stripePaymentMethodId") String stripePaymentMethodId);
}