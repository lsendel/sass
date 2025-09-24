package com.platform.user.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.platform.user.api.UserDto.OrganizationResponse;
import com.platform.user.api.UserDto.OrganizationMemberInfoResponse;
import com.platform.user.api.UserDto.OrganizationMemberResponse;
import com.platform.user.api.UserDto.InvitationResponse;
import com.platform.user.api.UserDto.Role;
import com.platform.user.api.UserDto.InvitationStatus;
import com.platform.user.internal.OrganizationService;
import com.platform.user.internal.OrganizationView;
import com.platform.user.internal.OrganizationMember;
import com.platform.user.internal.OrganizationMemberView;
import com.platform.user.internal.Invitation;
import com.platform.user.internal.InvitationView;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;

/**
 * Implementation of OrganizationManagementService that bridges the API and internal layers.
 * This service converts between internal entities and API DTOs.
 */
@Service
public class OrganizationManagementServiceImpl implements OrganizationManagementService {

  private final OrganizationService organizationService;
  private final OrganizationRepository organizationRepository;

  public OrganizationManagementServiceImpl(OrganizationService organizationService, OrganizationRepository organizationRepository) {
    this.organizationService = organizationService;
    this.organizationRepository = organizationRepository;
  }

  @Override
  public OrganizationResponse createOrganization(String name, String slug, Map<String, Object> settings) {
    OrganizationView organization = organizationService.createOrganizationView(name, slug, settings);
    return mapToOrganizationResponse(organization);
  }

  @Override
  public Optional<OrganizationResponse> findById(UUID organizationId) {
    return organizationService.findViewById(organizationId)
        .map(this::mapToOrganizationResponse);
  }

  @Override
  public Optional<OrganizationResponse> findBySlug(String slug) {
    return organizationService.findViewBySlug(slug)
        .map(this::mapToOrganizationResponse);
  }

  @Override
  public List<OrganizationResponse> getUserOrganizations() {
    return organizationService.getUserOrganizationViews()
        .stream()
        .map(this::mapToOrganizationResponse)
        .toList();
  }

  @Override
  public OrganizationResponse updateOrganization(UUID organizationId, String name, Map<String, Object> settings) {
    OrganizationView organization = organizationService.updateOrganizationView(organizationId, name, settings);
    return mapToOrganizationResponse(organization);
  }

  @Override
  public OrganizationResponse updateSettings(UUID organizationId, Map<String, Object> settings) {
    OrganizationView organization = organizationService.updateSettingsView(organizationId, settings);
    return mapToOrganizationResponse(organization);
  }

  @Override
  public void deleteOrganization(UUID organizationId) {
    organizationService.deleteOrganization(organizationId);
  }

  @Override
  public List<OrganizationMemberInfoResponse> getMembers(UUID organizationId) {
    return organizationService.getOrganizationMembers(organizationId)
        .stream()
        .map(this::mapToOrganizationMemberInfoResponse)
        .toList();
  }

  @Override
  public InvitationResponse inviteUser(UUID organizationId, String email, Role role) {
    OrganizationMember.Role internalRole = mapToInternalRole(role);
    InvitationView invitation = organizationService.inviteUserView(organizationId, email, internalRole);
    return mapToInvitationResponse(invitation);
  }

  @Override
  public OrganizationMemberResponse acceptInvitation(String token) {
    OrganizationMemberView member = organizationService.acceptInvitationView(token);
    return mapToOrganizationMemberResponse(member);
  }

  @Override
  public void declineInvitation(String token) {
    organizationService.declineInvitation(token);
  }

  @Override
  public List<InvitationResponse> getPendingInvitations(UUID organizationId) {
    return organizationService.getPendingInvitationViews(organizationId)
        .stream()
        .map(this::mapToInvitationResponse)
        .toList();
  }

  @Override
  public void revokeInvitation(UUID invitationId) {
    organizationService.revokeInvitation(invitationId);
  }

  @Override
  public void removeMember(UUID organizationId, UUID userId) {
    organizationService.removeMember(organizationId, userId);
  }

  @Override
  public OrganizationMemberResponse updateMemberRole(UUID organizationId, UUID userId, Role role) {
    OrganizationMember.Role internalRole = mapToInternalRole(role);
    OrganizationMemberView member = organizationService.updateMemberRoleView(organizationId, userId, internalRole);
    return mapToOrganizationMemberResponse(member);
  }

  /**
   * Maps internal OrganizationView to API OrganizationResponse DTO.
   */
  @Override
  public List<OrganizationResponse> getAllOrganizations() {
    return organizationService.getAllOrganizationViews()
        .stream()
        .map(this::mapToOrganizationResponse)
        .toList();
  }

  private OrganizationResponse mapToOrganizationResponse(OrganizationView organization) {
    return new OrganizationResponse(
        organization.id(),
        organization.name(),
        organization.slug(),
        organization.ownerId(),
        organization.settings(),
        organization.createdAt(),
        organization.updatedAt());
  }

  // Mapping helpers below

  /**
   * Maps internal OrganizationMemberInfo to API OrganizationMemberInfoResponse DTO.
   */
  private OrganizationMemberInfoResponse mapToOrganizationMemberInfoResponse(
      OrganizationService.OrganizationMemberInfo info) {
    return new OrganizationMemberInfoResponse(
        info.userId(),
        info.userEmail(),
        info.userName(),
        mapToApiRole(info.role()),
        info.joinedAt());
  }

  /**
   * Maps internal OrganizationMemberView to API OrganizationMemberResponse DTO.
   */
  private OrganizationMemberResponse mapToOrganizationMemberResponse(OrganizationMemberView member) {
    return new OrganizationMemberResponse(
        member.id(),
        member.userId(),
        member.organizationId(),
        mapToApiRole(member.role()),
        member.joinedAt());
  }

  /**
   * Maps internal InvitationView to API InvitationResponse DTO.
   */
  private InvitationResponse mapToInvitationResponse(InvitationView invitation) {
    return new InvitationResponse(
        invitation.id(),
        invitation.organizationId(),
        invitation.invitedBy(),
        invitation.email(),
        mapToApiRole(invitation.role()),
        mapToApiInvitationStatus(invitation.status()),
        invitation.token(),
        invitation.expiresAt(),
        invitation.createdAt());
  }

  /**
   * Maps API Role to internal OrganizationMember.Role.
   */
  private OrganizationMember.Role mapToInternalRole(Role apiRole) {
    return switch (apiRole) {
      case OWNER -> OrganizationMember.Role.OWNER;
      case ADMIN -> OrganizationMember.Role.ADMIN;
      case MEMBER -> OrganizationMember.Role.MEMBER;
      case VIEWER -> OrganizationMember.Role.MEMBER; // Map VIEWER to MEMBER since VIEWER doesn't exist
    };
  }

  /**
   * Maps internal OrganizationMember.Role to API Role.
   */
  private Role mapToApiRole(OrganizationMember.Role internalRole) {
    return switch (internalRole) {
      case OWNER -> Role.OWNER;
      case ADMIN -> Role.ADMIN;
      case MEMBER -> Role.MEMBER;
    };
  }

  /**
   * Maps internal Invitation.Status to API InvitationStatus.
   */
  private InvitationStatus mapToApiInvitationStatus(Invitation.Status internalStatus) {
    return switch (internalStatus) {
      case PENDING -> InvitationStatus.PENDING;
      case ACCEPTED -> InvitationStatus.ACCEPTED;
      case DECLINED -> InvitationStatus.DECLINED;
      case EXPIRED -> InvitationStatus.EXPIRED;
      case REVOKED -> InvitationStatus.REVOKED;
    };
  }
}
