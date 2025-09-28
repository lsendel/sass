package com.platform.user.events;

import java.time.Instant;
import java.util.UUID;

import com.platform.user.internal.OrganizationMember;

/**
 * A domain event published when an invitation to join an organization is created.
 *
 * <p>This event can be consumed by other services, such as a notification service, to send an email
 * to the invited user.
 *
 * @param invitationId the unique identifier of the invitation
 * @param email the email address of the recipient
 * @param organizationId the ID of the organization the user is invited to
 * @param organizationName the name of the organization
 * @param role the role the user is invited to have
 * @param invitedBy the ID of the user who sent the invitation
 * @param createdAt the timestamp when the event was created
 */
public record InvitationCreatedEvent(
    UUID invitationId,
    String email,
    UUID organizationId,
    String organizationName,
    OrganizationMember.Role role,
    UUID invitedBy,
    Instant createdAt) {

  /**
   * Constructs a new InvitationCreatedEvent with the creation timestamp set to the current time.
   *
   * @param invitationId the unique identifier of the invitation
   * @param email the email address of the recipient
   * @param organizationId the ID of the organization
   * @param organizationName the name of the organization
   * @param role the role to be assigned
   * @param invitedBy the ID of the user who sent the invitation
   */
  public InvitationCreatedEvent(
      UUID invitationId,
      String email,
      UUID organizationId,
      String organizationName,
      OrganizationMember.Role role,
      UUID invitedBy) {
    this(invitationId, email, organizationId, organizationName, role, invitedBy, Instant.now());
  }
}
