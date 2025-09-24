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
 * Service interface for organization management operations.
 * This interface provides the API layer with access to organization functionality
 * without depending on internal implementation details.
 */
public interface OrganizationManagementService {

  /**
   * Creates a new organization.
   *
   * @param name the organization name
   * @param slug the organization slug
   * @param settings the organization settings
   * @return the created organization
   */
  OrganizationResponse createOrganization(String name, String slug, Map<String, Object> settings);

  /**
   * Finds an organization by ID.
   *
   * @param organizationId the organization ID
   * @return the organization if found
   */
  Optional<OrganizationResponse> findById(UUID organizationId);

  /**
   * Finds an organization by slug.
   *
   * @param slug the organization slug
   * @return the organization if found
   */
  Optional<OrganizationResponse> findBySlug(String slug);

  /**
   * Gets organizations for the current user.
   *
   * @return list of user organizations
   */
  List<OrganizationResponse> getUserOrganizations();

  /**
   * Gets all organizations (test-only endpoint).
   *
   * @return list of all organizations
   */
  List<OrganizationResponse> getAllOrganizations();

  /**
   * Updates an organization.
   *
   * @param organizationId the organization ID
   * @param name the new organization name
   * @param settings the new organization settings
   * @return the updated organization
   */
  OrganizationResponse updateOrganization(UUID organizationId, String name, Map<String, Object> settings);

  /**
   * Updates organization settings.
   *
   * @param organizationId the organization ID
   * @param settings the new settings
   * @return the updated organization
   */
  OrganizationResponse updateSettings(UUID organizationId, Map<String, Object> settings);

  /**
   * Deletes an organization.
   *
   * @param organizationId the organization ID
   */
  void deleteOrganization(UUID organizationId);

  /**
   * Gets organization members.
   *
   * @param organizationId the organization ID
   * @return list of organization members
   */
  List<OrganizationMemberInfoResponse> getMembers(UUID organizationId);

  /**
   * Invites a user to an organization.
   *
   * @param organizationId the organization ID
   * @param email the user email
   * @param role the member role
   * @return the created invitation
   */
  InvitationResponse inviteUser(UUID organizationId, String email, Role role);

  /**
   * Accepts an invitation.
   *
   * @param token the invitation token
   * @return the created organization member
   */
  OrganizationMemberResponse acceptInvitation(String token);

  /**
   * Declines an invitation.
   *
   * @param token the invitation token
   */
  void declineInvitation(String token);

  /**
   * Gets pending invitations for an organization.
   *
   * @param organizationId the organization ID
   * @return list of pending invitations
   */
  List<InvitationResponse> getPendingInvitations(UUID organizationId);

  /**
   * Revokes an invitation.
   *
   * @param invitationId the invitation ID
   */
  void revokeInvitation(UUID invitationId);

  /**
   * Removes a member from an organization.
   *
   * @param organizationId the organization ID
   * @param userId the user ID
   */
  void removeMember(UUID organizationId, UUID userId);

  /**
   * Updates a member's role.
   *
   * @param organizationId the organization ID
   * @param userId the user ID
   * @param role the new role
   * @return the updated organization member
   */
  OrganizationMemberResponse updateMemberRole(UUID organizationId, UUID userId, Role role);
}