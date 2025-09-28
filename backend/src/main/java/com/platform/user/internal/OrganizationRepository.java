package com.platform.user.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Organization} entities.
 *
 * <p>This interface provides methods for querying and managing organization records in the
 * database, including finding organizations by various criteria and performing existence checks.
 *
 * @see Organization
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

  /**
   * Finds an organization by its unique slug.
   *
   * @param slug the slug of the organization
   * @return an {@link Optional} containing the organization if found, otherwise an empty {@link
   *     Optional}
   */
  Optional<Organization> findBySlug(String slug);

  /**
   * Checks if an organization with the given slug exists.
   *
   * @param slug the slug to check for
   * @return {@code true} if an organization with the slug exists, {@code false} otherwise
   */
  boolean existsBySlug(String slug);

  /**
   * Finds all organizations owned by a specific user.
   *
   * @param ownerId the ID of the owner
   * @return a list of organizations owned by the user
   */
  List<Organization> findByOwnerId(UUID ownerId);

  /**
   * Finds all organizations with a specific status.
   *
   * @param status the status to search for
   * @return a list of organizations with the specified status
   */
  List<Organization> findByStatus(Organization.Status status);

  /**
   * Finds an organization by its name, ignoring case.
   *
   * @param name the name of the organization to find
   * @return an {@link Optional} containing the organization if found, otherwise an empty {@link
   *     Optional}
   */
  @Query("SELECT o FROM Organization o WHERE LOWER(o.name) = LOWER(:name)")
  Optional<Organization> findByNameIgnoreCase(@Param("name") String name);

  /**
   * Checks if an organization with the given name exists, ignoring case.
   *
   * @param name the name to check for
   * @return {@code true} if an organization with the name exists, {@code false} otherwise
   */
  @Query(
      "SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Organization o WHERE LOWER(o.name) = LOWER(:name)")
  boolean existsByNameIgnoreCase(@Param("name") String name);

  /**
   * Finds all organizations whose names contain the given pattern, ignoring case.
   *
   * @param namePattern the pattern to search for within organization names
   * @return a list of organizations matching the name pattern
   */
  @Query(
      "SELECT o FROM Organization o WHERE LOWER(o.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
  List<Organization> findByNameContainingIgnoreCase(@Param("namePattern") String namePattern);

  /**
   * Counts the number of organizations with a specific status.
   *
   * @param status the status to count
   * @return the number of organizations with the specified status
   */
  long countByStatus(Organization.Status status);

  /**
   * Finds all active organizations.
   *
   * @return a list of all organizations with the 'ACTIVE' status
   */
  @Query("SELECT o FROM Organization o WHERE o.status = 'ACTIVE'")
  List<Organization> findAllActive();

  /**
   * Checks if an organization with the given slug exists and has not been soft-deleted.
   *
   * @param slug the slug to check for
   * @return {@code true} if an active organization with the slug exists, {@code false} otherwise
   */
  @Query(
      "SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Organization o WHERE o.slug = :slug AND o.deletedAt IS NULL")
  boolean existsBySlugAndDeletedAtIsNull(@Param("slug") String slug);

  /**
   * Finds an organization by its slug, excluding any that have been soft-deleted.
   *
   * @param slug the slug of the organization
   * @return an {@link Optional} containing the active organization if found, otherwise an empty
   *     {@link Optional}
   */
  @Query("SELECT o FROM Organization o WHERE o.slug = :slug AND o.deletedAt IS NULL")
  Optional<Organization> findBySlugAndDeletedAtIsNull(@Param("slug") String slug);
}
