package com.platform.security.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for Dashboard entities with multi-tenancy and access control support.
 *
 * This repository provides secure access to dashboard configurations with
 * proper tenant isolation, role-based filtering, and performance optimization.
 *
 * Key features:
 * - Multi-tenant access control with owner-based filtering
 * - Role-based permission checking for dashboard access
 * - Shared dashboard discovery and management
 * - Tag-based categorization and search
 * - Widget count optimization for dashboard limits
 * - Default dashboard management per user/tenant
 */
@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {

    // Owner-based queries (multi-tenancy)

    /**
     * Find all dashboards owned by a specific user
     *
     * @param owner Owner user ID
     * @param pageable Pagination parameters
     * @return Page of user-owned dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.owner = :owner ORDER BY d.name ASC")
    Page<Dashboard> findByOwner(@Param("owner") String owner, Pageable pageable);

    /**
     * Find dashboard by name and owner (ensures tenant isolation)
     *
     * @param name Dashboard name
     * @param owner Owner user ID
     * @return Optional dashboard if found and owned by user
     */
    @Query("SELECT d FROM Dashboard d WHERE d.name = :name AND d.owner = :owner")
    Optional<Dashboard> findByNameAndOwner(@Param("name") String name, @Param("owner") String owner);

    /**
     * Find all dashboards owned by a user with default flag
     *
     * @param owner Owner user ID
     * @param isDefault Default flag value
     * @return List of matching dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.owner = :owner AND d.isDefault = :isDefault")
    List<Dashboard> findByOwnerAndIsDefault(@Param("owner") String owner,
                                           @Param("isDefault") Boolean isDefault);

    /**
     * Find the default dashboard for a specific owner
     *
     * @param owner Owner user ID
     * @return Optional default dashboard
     */
    @Query("SELECT d FROM Dashboard d WHERE d.owner = :owner AND d.isDefault = true")
    Optional<Dashboard> findDefaultByOwner(@Param("owner") String owner);

    // Permission-based access queries

    /**
     * Find dashboards accessible to user based on roles (owned + permitted)
     *
     * @param owner Owner user ID
     * @param userRoles Set of user roles for permission checking
     * @param pageable Pagination parameters
     * @return Page of accessible dashboards
     */
    @Query("SELECT DISTINCT d FROM Dashboard d LEFT JOIN d.permissions p WHERE " +
           "d.owner = :owner OR p IN :userRoles ORDER BY d.name ASC")
    Page<Dashboard> findAccessibleByUserRoles(@Param("owner") String owner,
                                             @Param("userRoles") Set<String> userRoles,
                                             Pageable pageable);

    /**
     * Find shared dashboards accessible to user based on roles
     *
     * @param userRoles Set of user roles for permission checking
     * @param pageable Pagination parameters
     * @return Page of shared dashboards user can access
     */
    @Query("SELECT DISTINCT d FROM Dashboard d LEFT JOIN d.permissions p WHERE " +
           "d.shared = true AND p IN :userRoles ORDER BY d.name ASC")
    Page<Dashboard> findSharedAccessibleByRoles(@Param("userRoles") Set<String> userRoles,
                                               Pageable pageable);

    /**
     * Check if user has access to specific dashboard
     *
     * @param dashboardId Dashboard ID
     * @param owner Owner user ID
     * @param userRoles Set of user roles
     * @return True if user has access
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Dashboard d " +
           "LEFT JOIN d.permissions p WHERE d.id = :dashboardId AND " +
           "(d.owner = :owner OR p IN :userRoles)")
    boolean hasAccess(@Param("dashboardId") UUID dashboardId,
                     @Param("owner") String owner,
                     @Param("userRoles") Set<String> userRoles);

    // Shared dashboard queries

    /**
     * Find all shared dashboards
     *
     * @param pageable Pagination parameters
     * @return Page of shared dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.shared = true ORDER BY d.name ASC")
    Page<Dashboard> findSharedDashboards(Pageable pageable);

    /**
     * Find shared dashboards by specific tags
     *
     * @param tags Set of tags to match
     * @param pageable Pagination parameters
     * @return Page of shared dashboards with matching tags
     */
    @Query("SELECT DISTINCT d FROM Dashboard d JOIN d.tags t WHERE d.shared = true AND t IN :tags " +
           "ORDER BY d.name ASC")
    Page<Dashboard> findSharedByTags(@Param("tags") Set<String> tags, Pageable pageable);

    /**
     * Count shared dashboards accessible to user roles
     *
     * @param userRoles Set of user roles
     * @return Number of accessible shared dashboards
     */
    @Query("SELECT COUNT(DISTINCT d) FROM Dashboard d LEFT JOIN d.permissions p WHERE " +
           "d.shared = true AND p IN :userRoles")
    long countSharedAccessibleByRoles(@Param("userRoles") Set<String> userRoles);

    // Tag-based queries

    /**
     * Find dashboards by owner with specific tags
     *
     * @param owner Owner user ID
     * @param tags Set of tags to match
     * @param pageable Pagination parameters
     * @return Page of dashboards with matching tags
     */
    @Query("SELECT DISTINCT d FROM Dashboard d JOIN d.tags t WHERE d.owner = :owner AND t IN :tags " +
           "ORDER BY d.name ASC")
    Page<Dashboard> findByOwnerAndTags(@Param("owner") String owner,
                                      @Param("tags") Set<String> tags,
                                      Pageable pageable);

    /**
     * Find all unique tags used in dashboards for a specific owner
     *
     * @param owner Owner user ID
     * @return Set of unique tags
     */
    @Query("SELECT DISTINCT t FROM Dashboard d JOIN d.tags t WHERE d.owner = :owner")
    Set<String> findTagsByOwner(@Param("owner") String owner);

    /**
     * Find all unique tags used in shared dashboards
     *
     * @return Set of unique tags from shared dashboards
     */
    @Query("SELECT DISTINCT t FROM Dashboard d JOIN d.tags t WHERE d.shared = true")
    Set<String> findSharedTags();

    // Search and filtering

    /**
     * Search dashboards by name pattern (case-insensitive) for specific owner
     *
     * @param owner Owner user ID
     * @param namePattern Name pattern (supports % wildcards)
     * @param pageable Pagination parameters
     * @return Page of matching dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.owner = :owner AND " +
           "LOWER(d.name) LIKE LOWER(:namePattern) ORDER BY d.name ASC")
    Page<Dashboard> searchByOwnerAndNamePattern(@Param("owner") String owner,
                                               @Param("namePattern") String namePattern,
                                               Pageable pageable);

    /**
     * Search dashboards by description pattern for specific owner
     *
     * @param owner Owner user ID
     * @param descriptionPattern Description pattern (supports % wildcards)
     * @param pageable Pagination parameters
     * @return Page of matching dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.owner = :owner AND " +
           "LOWER(d.description) LIKE LOWER(:descriptionPattern) ORDER BY d.name ASC")
    Page<Dashboard> searchByOwnerAndDescriptionPattern(@Param("owner") String owner,
                                                      @Param("descriptionPattern") String descriptionPattern,
                                                      Pageable pageable);

    /**
     * Find dashboards modified after specific timestamp
     *
     * @param owner Owner user ID
     * @param modifiedAfter Timestamp threshold
     * @param pageable Pagination parameters
     * @return Page of recently modified dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.owner = :owner AND d.lastModified > :modifiedAfter " +
           "ORDER BY d.lastModified DESC")
    Page<Dashboard> findByOwnerModifiedAfter(@Param("owner") String owner,
                                            @Param("modifiedAfter") Instant modifiedAfter,
                                            Pageable pageable);

    // Widget count and limit queries

    /**
     * Find dashboards approaching widget limit (18+ widgets out of 20 max)
     *
     * @param owner Owner user ID
     * @return List of dashboards near widget limit
     */
    @Query("SELECT d FROM Dashboard d WHERE d.owner = :owner AND SIZE(d.widgets) >= 18")
    List<Dashboard> findNearWidgetLimit(@Param("owner") String owner);

    /**
     * Count total widgets across all dashboards for an owner
     *
     * @param owner Owner user ID
     * @return Total widget count
     */
    @Query("SELECT SUM(SIZE(d.widgets)) FROM Dashboard d WHERE d.owner = :owner")
    Long countTotalWidgetsByOwner(@Param("owner") String owner);

    /**
     * Find dashboards with no widgets (empty dashboards)
     *
     * @param owner Owner user ID
     * @return List of empty dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.owner = :owner AND SIZE(d.widgets) = 0")
    List<Dashboard> findEmptyDashboards(@Param("owner") String owner);

    // Statistics and analytics

    /**
     * Count dashboards by owner
     *
     * @param owner Owner user ID
     * @return Number of dashboards owned by user
     */
    @Query("SELECT COUNT(d) FROM Dashboard d WHERE d.owner = :owner")
    long countByOwner(@Param("owner") String owner);

    /**
     * Get dashboard creation statistics by month for an owner
     *
     * @param owner Owner user ID
     * @param startDate Start date for statistics
     * @return List of [month, count] pairs
     */
    @Query(value = "SELECT DATE_TRUNC('month', created_at) as month, COUNT(*) as count " +
                   "FROM dashboards WHERE owner = :owner AND created_at >= :startDate " +
                   "GROUP BY DATE_TRUNC('month', created_at) ORDER BY month",
           nativeQuery = true)
    List<Object[]> getDashboardCreationStats(@Param("owner") String owner,
                                            @Param("startDate") Instant startDate);

    /**
     * Find most recently accessed dashboards for an owner
     *
     * @param owner Owner user ID
     * @param limit Maximum number of results
     * @return List of recently accessed dashboards
     */
    @Query("SELECT d FROM Dashboard d WHERE d.owner = :owner " +
           "ORDER BY d.lastModified DESC")
    List<Dashboard> findMostRecentlyAccessed(@Param("owner") String owner, Pageable pageable);

    // Bulk operations

    /**
     * Bulk update shared status for dashboards by owner
     *
     * @param owner Owner user ID
     * @param shared New shared status
     * @param dashboardIds List of dashboard IDs to update
     * @return Number of updated dashboards
     */
    @Modifying
    @Query("UPDATE Dashboard d SET d.shared = :shared WHERE d.owner = :owner AND d.id IN :dashboardIds")
    int bulkUpdateSharedStatus(@Param("owner") String owner,
                              @Param("shared") Boolean shared,
                              @Param("dashboardIds") List<UUID> dashboardIds);

    /**
     * Bulk delete dashboards by owner (with security check)
     *
     * @param owner Owner user ID
     * @param dashboardIds List of dashboard IDs to delete
     * @return Number of deleted dashboards
     */
    @Modifying
    @Query("DELETE FROM Dashboard d WHERE d.owner = :owner AND d.id IN :dashboardIds")
    int bulkDeleteByOwner(@Param("owner") String owner,
                         @Param("dashboardIds") List<UUID> dashboardIds);

    /**
     * Clear default flag for all dashboards of an owner
     *
     * @param owner Owner user ID
     * @return Number of updated dashboards
     */
    @Modifying
    @Query("UPDATE Dashboard d SET d.isDefault = false WHERE d.owner = :owner AND d.isDefault = true")
    int clearDefaultFlags(@Param("owner") String owner);

    // Advanced queries

    /**
     * Find dashboards with complex filtering
     *
     * @param owner Owner user ID
     * @param shared Optional shared status filter
     * @param isDefault Optional default status filter
     * @param tags Optional tags to match
     * @param namePattern Optional name pattern
     * @param pageable Pagination parameters
     * @return Page of filtered dashboards
     */
    @Query("SELECT DISTINCT d FROM Dashboard d LEFT JOIN d.tags t WHERE d.owner = :owner AND " +
           "(:shared IS NULL OR d.shared = :shared) AND " +
           "(:isDefault IS NULL OR d.isDefault = :isDefault) AND " +
           "(:tags IS NULL OR t IN :tags) AND " +
           "(:namePattern IS NULL OR LOWER(d.name) LIKE LOWER(:namePattern)) " +
           "ORDER BY d.name ASC")
    Page<Dashboard> findWithComplexFilters(@Param("owner") String owner,
                                          @Param("shared") Boolean shared,
                                          @Param("isDefault") Boolean isDefault,
                                          @Param("tags") Set<String> tags,
                                          @Param("namePattern") String namePattern,
                                          Pageable pageable);

    /**
     * Find dashboards that haven't been modified in specified days
     *
     * @param owner Owner user ID
     * @param daysAgo Number of days ago threshold
     * @return List of stale dashboards
     */
    @Query(value = "SELECT * FROM dashboards WHERE owner = :owner AND " +
                   "last_modified < NOW() - INTERVAL ':daysAgo days'",
           nativeQuery = true)
    List<Dashboard> findStaleByOwner(@Param("owner") String owner, @Param("daysAgo") int daysAgo);

    /**
     * Check if dashboard name is unique for owner
     *
     * @param name Dashboard name
     * @param owner Owner user ID
     * @param excludeId Optional dashboard ID to exclude from check
     * @return True if name is unique
     */
    @Query("SELECT CASE WHEN COUNT(d) = 0 THEN true ELSE false END FROM Dashboard d WHERE " +
           "d.name = :name AND d.owner = :owner AND (:excludeId IS NULL OR d.id != :excludeId)")
    boolean isNameUniqueForOwner(@Param("name") String name,
                                @Param("owner") String owner,
                                @Param("excludeId") UUID excludeId);

    /**
     * Find dashboards similar to given dashboard (by tags and name similarity)
     *
     * @param dashboardId Reference dashboard ID
     * @param owner Owner user ID
     * @param limit Maximum number of results
     * @return List of similar dashboards
     */
    @Query(value = "SELECT d2.* FROM dashboards d1, dashboards d2 " +
                   "WHERE d1.id = :dashboardId AND d2.owner = :owner AND d2.id != d1.id AND " +
                   "(d1.tags && d2.tags OR SIMILARITY(d1.name, d2.name) > 0.3) " +
                   "ORDER BY SIMILARITY(d1.name, d2.name) DESC LIMIT :limit",
           nativeQuery = true)
    List<Dashboard> findSimilarDashboards(@Param("dashboardId") UUID dashboardId,
                                         @Param("owner") String owner,
                                         @Param("limit") int limit);

    /**
     * Get dashboard usage metrics (widget count, last modified, etc.)
     *
     * @param owner Owner user ID
     * @return List of dashboard usage metrics
     */
    @Query("SELECT NEW map(d.id as id, d.name as name, SIZE(d.widgets) as widgetCount, " +
           "d.lastModified as lastModified, d.shared as shared) FROM Dashboard d WHERE d.owner = :owner")
    List<Object> getDashboardMetrics(@Param("owner") String owner);
}