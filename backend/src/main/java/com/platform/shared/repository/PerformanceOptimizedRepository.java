package com.platform.shared.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.NoRepositoryBean;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface with performance optimizations.
 * Provides common caching and query optimization patterns.
 */
@NoRepositoryBean
public interface PerformanceOptimizedRepository<T, ID> {

    /**
     * Find by ID with caching enabled
     */
    @Cacheable(value = "entities", key = "#id", keyGenerator = "performanceKeyGenerator")
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheMode", value = "NORMAL")
    })
    Optional<T> findById(ID id);

    /**
     * Find all with pagination and query optimization
     */
    @QueryHints({
        @QueryHint(name = "org.hibernate.fetchSize", value = "50"),
        @QueryHint(name = "org.hibernate.readOnly", value = "true")
    })
    Page<T> findAll(Pageable pageable);

    /**
     * Batch find operations for better performance
     */
    @QueryHints({
        @QueryHint(name = "org.hibernate.fetchSize", value = "100"),
        @QueryHint(name = "org.hibernate.readOnly", value = "true")
    })
    List<T> findAllById(Iterable<ID> ids);

    /**
     * Count with caching for dashboard statistics
     */
    @Cacheable(value = "entity-counts", keyGenerator = "performanceKeyGenerator")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    long count();
}

/**
 * Enhanced organization repository with performance optimizations
 */
interface OptimizedOrganizationRepository extends PerformanceOptimizedRepository<Object, UUID> {

    @Cacheable(value = "organizations", key = "#slug", keyGenerator = "tenantAwareKeyGenerator")
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "organizations")
    })
    Optional<Object> findBySlugAndDeletedAtIsNull(String slug);

    @Cacheable(value = "organizations", keyGenerator = "tenantAwareKeyGenerator")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Object> findByOwnerId(UUID ownerId);

    // Batch operations for better performance
    @QueryHints({
        @QueryHint(name = "org.hibernate.fetchSize", value = "25"),
        @QueryHint(name = "org.hibernate.batchSize", value = "25")
    })
    List<Object> findByOwnerIdIn(List<UUID> ownerIds);
}

/**
 * Performance patterns for audit repository
 */
interface OptimizedAuditRepository extends PerformanceOptimizedRepository<Object, UUID> {

    @Cacheable(value = "audit-stats", keyGenerator = "tenantAwareKeyGenerator")
    @QueryHints({
        @QueryHint(name = "org.hibernate.readOnly", value = "true"),
        @QueryHint(name = "org.hibernate.fetchSize", value = "1")
    })
    long countByOrganizationIdAndActionAndCreatedAtAfter(
        UUID organizationId, String action, java.time.Instant since);

    // Use native queries for complex analytics with result caching
    @Cacheable(value = "security-analysis", keyGenerator = "tenantAwareKeyGenerator")
    @QueryHints({
        @QueryHint(name = "org.hibernate.readOnly", value = "true"),
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
    List<Object[]> findSecurityTrendsSummary(
        UUID organizationId, java.time.Instant startDate, java.time.Instant endDate);
}