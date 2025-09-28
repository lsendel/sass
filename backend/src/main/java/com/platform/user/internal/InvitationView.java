package com.platform.user.internal;

import java.time.Instant;
import java.util.UUID;

/**
 * A read-only projection of an {@link Invitation} entity, designed for use in the API layer.
 *
 * <p>This record provides a safe way to expose invitation data without revealing the full internal
 * entity. It includes all necessary information for displaying invitation details to a user.
 *
 * @param id the unique identifier of the invitation
 * @param organizationId the ID of the organization the user is invited to
 * @param invitedBy the ID of the user who sent the invitation
 * @param email the email address of the recipient
 * @param role the role the user is invited to have
 * @param status the current status of the invitation
 * @param token the unique invitation token
 * @param expiresAt the timestamp when the invitation expires
 * @param createdAt the timestamp when the invitation was created
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

  /**
   * Creates an {@link InvitationView} from an {@link Invitation} entity.
   *
   * @param invitation the invitation entity to convert
   * @return a new {@link InvitationView} instance
   */
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

