package com.platform.user.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository interface for Organization entity operations. */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

  /** Find organization by slug */
  Optional<Organization> findBySlug(String slug);

  /** Check if slug exists */
  boolean existsBySlug(String slug);

  /** Find organizations by owner */
  List<Organization> findByOwnerId(UUID ownerId);

  /** Find organizations by status */
  List<Organization> findByStatus(Organization.Status status);

  /** Find organization by name (case-insensitive) */
  @Query("SELECT o FROM Organization o WHERE LOWER(o.name) = LOWER(:name)")
  Optional<Organization> findByNameIgnoreCase(@Param("name") String name);

  /** Check if name exists (case-insensitive) */
  @Query(
      "SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Organization o WHERE LOWER(o.name) = LOWER(:name)")
  boolean existsByNameIgnoreCase(@Param("name") String name);

  /** Find organizations containing name pattern */
  @Query(
      "SELECT o FROM Organization o WHERE LOWER(o.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
  List<Organization> findByNameContainingIgnoreCase(@Param("namePattern") String namePattern);

  /** Count organizations by status */
  long countByStatus(Organization.Status status);

  /** Find all active organizations */
  @Query("SELECT o FROM Organization o WHERE o.status = 'ACTIVE'")
  List<Organization> findAllActive();

  /** Check if slug exists (excluding soft-deleted) */
  @Query(
      "SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Organization o WHERE o.slug = :slug AND o.deletedAt IS NULL")
  boolean existsBySlugAndDeletedAtIsNull(@Param("slug") String slug);

  /** Find organization by slug (excluding soft-deleted) */
  @Query("SELECT o FROM Organization o WHERE o.slug = :slug AND o.deletedAt IS NULL")
  Optional<Organization> findBySlugAndDeletedAtIsNull(@Param("slug") String slug);
}
