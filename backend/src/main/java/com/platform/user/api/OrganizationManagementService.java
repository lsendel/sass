package com.platform.user.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.platform.user.api.UserDto.OrganizationResponse;
import com.platform.user.api.UserDto.OrganizationMemberInfoResponse;
import com.platform.user.api.UserDto.OrganizationMemberResponse;
import com.platform.user.api.UserDto.InvitationResponse;
import com.platform.user.api.UserDto.Role;

/**
 * Defines the contract for managing organizations, memberships, and invitations.
 *
 * <p>This service interface provides a high-level API for all organization-related operations,
 * serving as an abstraction layer between the API controllers and the internal implementation
 * details.
 */
public interface OrganizationManagementService {

  /**
   * Creates a new organization.
   *
   * @param name the name of the organization
   * @param slug a unique, URL-friendly slug for the organization
   * @param settings a map of custom settings for the organization
   * @return an {@link OrganizationResponse} representing the newly created organization
   */
  OrganizationResponse createOrganization(String name, String slug, Map<String, Object> settings);

  /**
   * Finds an organization by its ID.
   *
   * @param organizationId the ID of the organization
   * @return an {@link Optional} containing the {@link OrganizationResponse} if found, otherwise an
   *     empty {@link Optional}
   */
  Optional<OrganizationResponse> findById(UUID organizationId);

  /**
   * Finds an organization by its slug.
   *
   * @param slug the slug of the organization
   * @return an {@link Optional} containing the {@link OrganizationResponse} if found, otherwise an
   *     empty {@link Optional}
   */
  Optional<OrganizationResponse> findBySlug(String slug);

  /**
   * Retrieves all organizations the current user is a member of.
   *
   * @return a list of {@link OrganizationResponse}s
   */
  List<OrganizationResponse> getUserOrganizations();

  /**
   * Retrieves all organizations in the system.
   *
   * <p>This method is intended for administrative or testing purposes.
   *
   * @return a list of all {@link OrganizationResponse}s
   */
  List<OrganizationResponse> getAllOrganizations();

  /**
   * Updates the details of an organization.
   *
   * @param organizationId the ID of the organization to update
   * @param name the new name for the organization
   * @param settings a map of new settings for the organization
   * @return an {@link OrganizationResponse} representing the updated organization
   */
  OrganizationResponse updateOrganization(UUID organizationId, String name, Map<String, Object> settings);

  /**
   * Updates the settings of an organization.
   *
   * @param organizationId the ID of the organization to update
   * @param settings a map of new settings for the organization
   * @return an {@link OrganizationResponse} representing the updated organization
   */
  OrganizationResponse updateSettings(UUID organizationId, Map<String, Object> settings);

  /**
   * Deletes an organization.
   *
   * @param organizationId the ID of the organization to delete
   */
  void deleteOrganization(UUID organizationId);

  /**
   * Retrieves a list of all members in an organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of {@link OrganizationMemberInfoResponse}s
   */
  List<OrganizationMemberInfoResponse> getMembers(UUID organizationId);

  /**
   * Invites a user to join an organization.
   *
   * @param organizationId the ID of the organization
   * @param email the email address of the user to invite
   * @param role the role to assign to the user
   * @return an {@link InvitationResponse} representing the created invitation
   */
  InvitationResponse inviteUser(UUID organizationId, String email, Role role);

  /**
   * Accepts an invitation to join an organization.
   *
   * @param token the invitation token
   * @return an {@link OrganizationMemberResponse} representing the new membership
   */
  OrganizationMemberResponse acceptInvitation(String token);

  /**
   * Declines an invitation to join an organization.
   *
   * @param token the invitation token
   */
  void declineInvitation(String token);

  /**
   * Retrieves a list of pending invitations for an organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of {@link InvitationResponse}s
   */
  List<InvitationResponse> getPendingInvitations(UUID organizationId);

  /**
   * Revokes a pending invitation.
   *
   * @param invitationId the ID of the invitation to revoke
   */
  void revokeInvitation(UUID invitationId);

  /**
   * Removes a member from an organization.
   *
   * @param organizationId the ID of the organization
   * @param userId the ID of the user to remove
   */
  void removeMember(UUID organizationId, UUID userId);

  /**
   * Updates the role of a member in an organization.
   *
   * @param organizationId the ID of the organization
   * @param userId the ID of the user whose role is to be updated
   * @param role the new role to assign
   * @return an {@link OrganizationMemberResponse} representing the updated membership
   */
  OrganizationMemberResponse updateMemberRole(UUID organizationId, UUID userId, Role role);
}