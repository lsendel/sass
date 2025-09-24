package com.platform.user.internal;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of OrganizationMember for API layer.
 */
public record OrganizationMemberView(
    UUID id,
    UUID userId,
    UUID organizationId,
    OrganizationMember.Role role,
    Instant joinedAt) {

  public static OrganizationMemberView fromEntity(OrganizationMember member) {
    return new OrganizationMemberView(
        member.getId(),
        member.getUserId(),
        member.getOrganizationId(),
        member.getRole(),
        member.getJoinedAt());
  }
}

