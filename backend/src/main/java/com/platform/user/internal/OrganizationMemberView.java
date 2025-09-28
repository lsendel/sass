package com.platform.user.internal;

import java.time.Instant;
import java.util.UUID;

/**
 * A read-only projection of an {@link OrganizationMember} entity, designed for use in the API
 * layer.
 *
 * <p>This record provides a safe way to expose membership data without revealing the full internal
 * entity. It includes essential information for displaying a user's membership in an organization.
 *
 * @param id the unique identifier of the membership record
 * @param userId the ID of the user
 * @param organizationId the ID of the organization
 * @param role the role of the member in the organization
 * @param joinedAt the timestamp when the member joined the organization
 */
public record OrganizationMemberView(
    UUID id,
    UUID userId,
    UUID organizationId,
    OrganizationMember.Role role,
    Instant joinedAt) {

  /**
   * Creates an {@link OrganizationMemberView} from an {@link OrganizationMember} entity.
   *
   * @param member the membership entity to convert
   * @return a new {@link OrganizationMemberView} instance
   */
  public static OrganizationMemberView fromEntity(OrganizationMember member) {
    return new OrganizationMemberView(
        member.getId(),
        member.getUserId(),
        member.getOrganizationId(),
        member.getRole(),
        member.getJoinedAt());
  }
}

