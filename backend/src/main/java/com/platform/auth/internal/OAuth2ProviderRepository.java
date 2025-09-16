package com.platform.auth.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for OAuth2Provider entities.
 * Manages OAuth2 provider configurations and metadata.
 */
@Repository
public interface OAuth2ProviderRepository extends JpaRepository<OAuth2Provider, Long> {

    /**
     * Find a provider by its name (registration ID)
     */
    Optional<OAuth2Provider> findByName(String name);

    /**
     * Find a provider by name if it's enabled
     */
    Optional<OAuth2Provider> findByNameAndEnabledTrue(String name);

    /**
     * Find all enabled providers ordered by sort order
     */
    List<OAuth2Provider> findByEnabledTrueOrderBySortOrderAscNameAsc();

    /**
     * Check if a provider exists and is enabled
     */
    boolean existsByNameAndEnabledTrue(String name);

    /**
     * Count enabled providers
     */
    long countByEnabledTrue();

    /**
     * Find providers by client ID
     */
    Optional<OAuth2Provider> findByClientId(String clientId);

    /**
     * Find all providers that have a specific scope
     */
    @Query("SELECT DISTINCT p FROM OAuth2Provider p JOIN p.scopes s WHERE s = :scope")
    List<OAuth2Provider> findByScope(@Param("scope") String scope);

    /**
     * Find all providers ordered by sort order
     */
    List<OAuth2Provider> findAllByOrderBySortOrderAscNameAsc();
}