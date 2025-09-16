package com.platform.subscription.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByActiveOrderByDisplayOrderAsc(boolean active);

    Optional<Plan> findBySlugAndActive(String slug, boolean active);

    Optional<Plan> findByStripePriceId(String stripePriceId);

    @Query("SELECT p FROM Plan p WHERE p.active = true AND p.amount.amount <= :maxAmount ORDER BY p.amount.amount ASC")
    List<Plan> findActivePlansUpToAmount(@Param("maxAmount") Long maxAmountCents);

    @Query("SELECT p FROM Plan p WHERE p.active = true AND p.interval = :interval ORDER BY p.displayOrder ASC")
    List<Plan> findActivePlansByInterval(@Param("interval") Plan.BillingInterval interval);


    @Query("SELECT COUNT(p) FROM Plan p WHERE p.active = true")
    long countActivePlans();

    List<Plan> findByActiveOrderByAmountAmountAsc(boolean active);
}