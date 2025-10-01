package com.platform.user.internal;

import com.platform.user.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Organization entity.
 *
 * @since 1.0.0
 */
interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Finds an organization by its slug.
     *
     * @param slug the organization slug
     * @return the organization if found
     */
    Optional<Organization> findBySlug(String slug);

    /**
     * Finds an active organization by slug.
     *
     * @param slug the organization slug
     * @return the organization if found and active
     */
    @Query("SELECT o FROM Organization o WHERE o.slug = :slug " +
           "AND o.status = 'ACTIVE' AND o.deletedAt IS NULL")
    Optional<Organization> findActiveBySlug(@Param("slug") String slug);

    /**
     * Checks if an organization with the given slug exists.
     *
     * @param slug the organization slug
     * @return true if exists
     */
    boolean existsBySlug(String slug);

    /**
     * Finds an organization by domain.
     *
     * @param domain the domain name
     * @return the organization if found
     */
    Optional<Organization> findByDomain(String domain);
}
