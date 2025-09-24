package com.platform.user.internal;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of Invitation for API layer.
 */
public record InvitationView(
    UUID id,
    UUID organizationId,
    UUID invitedBy,
    String email,
    OrganizationMember.Role role,
    Invitation.Status status,
    String token,
    Instant expiresAt,
    Instant createdAt) {

  public static InvitationView fromEntity(Invitation invitation) {
    return new InvitationView(
        invitation.getId(),
        invitation.getOrganizationId(),
        invitation.getInvitedBy(),
        invitation.getEmail(),
        invitation.getRole(),
        invitation.getStatus(),
        invitation.getToken(),
        invitation.getExpiresAt(),
        invitation.getCreatedAt());
  }
}

