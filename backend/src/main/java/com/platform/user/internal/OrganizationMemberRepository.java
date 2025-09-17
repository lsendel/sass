package com.platform.user.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository interface for OrganizationMember entity operations. */
@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

  /** Find members by organization ID */
  List<OrganizationMember> findByOrganizationId(UUID organizationId);

  /** Find member by organization and user */
  Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  /** Find organizations where user is a member */
  List<OrganizationMember> findByUserId(UUID userId);

  /** Find members by role */
  List<OrganizationMember> findByRole(OrganizationMember.Role role);

  /** Find members by organization and role */
  List<OrganizationMember> findByOrganizationIdAndRole(
      UUID organizationId, OrganizationMember.Role role);

  /** Find members by organization and status */
  List<OrganizationMember> findByOrganizationIdAndStatus(
      UUID organizationId, OrganizationMember.Status status);

  /** Check if user is member of organization */
  boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  /** Count members in organization */
  long countByOrganizationId(UUID organizationId);

  /** Count members in organization by role */
  long countByOrganizationIdAndRole(UUID organizationId, OrganizationMember.Role role);

  /** Count members in organization by status */
  long countByOrganizationIdAndStatus(UUID organizationId, OrganizationMember.Status status);

  /** Find organization owners */
  @Query(
      "SELECT om FROM OrganizationMember om WHERE om.organizationId = :organizationId AND om.role = 'OWNER'")
  List<OrganizationMember> findOwnersByOrganizationId(@Param("organizationId") UUID organizationId);

  /** Find active members with user details */
  @Query(
      "SELECT om FROM OrganizationMember om WHERE om.organizationId = :organizationId AND om.status = 'ACTIVE'")
  List<OrganizationMember> findActiveMembers(@Param("organizationId") UUID organizationId);

  /** Delete member by organization and user */
  void deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  /** Check if user exists in organization */
  boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

  /** Find organizations for a user */
  @Query("SELECT om FROM OrganizationMember om WHERE om.userId = :userId AND om.status = 'ACTIVE'")
  List<OrganizationMember> findOrganizationsForUser(@Param("userId") UUID userId);

  /** Find member info by organization (returns member details with user info) */
  @Query("SELECT om FROM OrganizationMember om WHERE om.organizationId = :organizationId")
  List<OrganizationMember> findMemberInfoByOrganization(
      @Param("organizationId") UUID organizationId);

  /** Find member by user and organization */
  Optional<OrganizationMember> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

  /** Count owners in organization */
  @Query(
      "SELECT COUNT(om) FROM OrganizationMember om WHERE om.organizationId = :organizationId AND om.role = 'OWNER'")
  long countOwners(@Param("organizationId") UUID organizationId);
}
