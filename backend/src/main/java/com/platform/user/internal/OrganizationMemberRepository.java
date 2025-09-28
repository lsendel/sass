package com.platform.user.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link OrganizationMember} entities.
 *
 * <p>This interface provides methods for querying and managing the relationship between users and
 * organizations, including finding members by various criteria and performing aggregate counts.
 *
 * @see OrganizationMember
 */
@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

  /**
   * Finds all members of a specific organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of all members in the organization
   */
  List<OrganizationMember> findByOrganizationId(UUID organizationId);

  /**
   * Finds a specific membership record for a given user in a given organization.
   *
   * @param organizationId the ID of the organization
   * @param userId the ID of the user
   * @return an {@link Optional} containing the membership record if found, otherwise an empty
   *     {@link Optional}
   */
  Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  /**
   * Finds all organization memberships for a specific user.
   *
   * @param userId the ID of the user
   * @return a list of all memberships for the user across all organizations
   */
  List<OrganizationMember> findByUserId(UUID userId);

  /**
   * Finds all memberships with a specific role across all organizations.
   *
   * @param role the role to search for
   * @return a list of all memberships with the specified role
   */
  List<OrganizationMember> findByRole(OrganizationMember.Role role);

  /**
   * Finds all members in a specific organization who have a certain role.
   *
   * @param organizationId the ID of the organization
   * @param role the role to filter by
   * @return a list of members in the organization with the specified role
   */
  List<OrganizationMember> findByOrganizationIdAndRole(
      UUID organizationId, OrganizationMember.Role role);

  /**
   * Finds all members in a specific organization with a certain status.
   *
   * @param organizationId the ID of the organization
   * @param status the status to filter by
   * @return a list of members in the organization with the specified status
   */
  List<OrganizationMember> findByOrganizationIdAndStatus(
      UUID organizationId, OrganizationMember.Status status);

  /**
   * Checks if a user is a member of a specific organization.
   *
   * @param organizationId the ID of the organization
   * @param userId the ID of the user
   * @return {@code true} if the user is a member of the organization, {@code false} otherwise
   */
  boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  /**
   * Counts the total number of members in an organization.
   *
   * @param organizationId the ID of the organization
   * @return the total number of members in the organization
   */
  long countByOrganizationId(UUID organizationId);

  /**
   * Counts the number of members in an organization with a specific role.
   *
   * @param organizationId the ID of the organization
   * @param role the role to count
   * @return the number of members with the specified role
   */
  long countByOrganizationIdAndRole(UUID organizationId, OrganizationMember.Role role);

  /**
   * Counts the number of members in an organization with a specific status.
   *
   * @param organizationId the ID of the organization
   * @param status the status to count
   * @return the number of members with the specified status
   */
  long countByOrganizationIdAndStatus(UUID organizationId, OrganizationMember.Status status);

  /**
   * Finds all members with the 'OWNER' role in a specific organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of all owners of the organization
   */
  @Query(
      "SELECT om FROM OrganizationMember om WHERE om.organizationId = :organizationId AND om.role = 'OWNER'")
  List<OrganizationMember> findOwnersByOrganizationId(@Param("organizationId") UUID organizationId);

  /**
   * Finds all active members of a specific organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of all active members in the organization
   */
  @Query(
      "SELECT om FROM OrganizationMember om WHERE om.organizationId = :organizationId AND om.status = 'ACTIVE'")
  List<OrganizationMember> findActiveMembers(@Param("organizationId") UUID organizationId);

  /**
   * Deletes a membership record for a specific user in a specific organization.
   *
   * @param organizationId the ID of the organization
   * @param userId the ID of the user to remove
   */
  void deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  /**
   * Checks if a user is a member of a specific organization.
   *
   * @param userId the ID of the user
   * @param organizationId the ID of the organization
   * @return {@code true} if the user is a member of the organization, {@code false} otherwise
   */
  boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

  /**
   * Finds all active organization memberships for a specific user.
   *
   * @param userId the ID of the user
   * @return a list of active memberships for the user
   */
  @Query("SELECT om FROM OrganizationMember om WHERE om.userId = :userId AND om.status = 'ACTIVE'")
  List<OrganizationMember> findOrganizationsForUser(@Param("userId") UUID userId);

  /**
   * Finds all member information for a specific organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of all members in the organization
   */
  @Query("SELECT om FROM OrganizationMember om WHERE om.organizationId = :organizationId")
  List<OrganizationMember> findMemberInfoByOrganization(
      @Param("organizationId") UUID organizationId);

  /**
   * Finds a specific membership record for a given user in a given organization.
   *
   * @param userId the ID of the user
   * @param organizationId the ID of the organization
   * @return an {@link Optional} containing the membership record if found, otherwise an empty
   *     {@link Optional}
   */
  Optional<OrganizationMember> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

  /**
   * Counts the number of owners in a specific organization.
   *
   * @param organizationId the ID of the organization
   * @return the number of owners in the organization
   */
  @Query(
      "SELECT COUNT(om) FROM OrganizationMember om WHERE om.organizationId = :organizationId AND om.role = 'OWNER'")
  long countOwners(@Param("organizationId") UUID organizationId);
}
