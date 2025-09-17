package com.platform.user.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.platform.user.internal.*;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

  private final OrganizationService organizationService;

  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @PostMapping
  public ResponseEntity<OrganizationResponse> createOrganization(
      @Valid @RequestBody CreateOrganizationRequest request) {
    Organization organization =
        organizationService.createOrganization(request.name(), request.slug(), request.settings());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(OrganizationResponse.fromOrganization(organization));
  }

  @GetMapping("/{organizationId}")
  public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID organizationId) {
    Optional<Organization> organization = organizationService.findById(organizationId);
    return organization
        .map(org -> ResponseEntity.ok(OrganizationResponse.fromOrganization(org)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/slug/{slug}")
  public ResponseEntity<OrganizationResponse> getOrganizationBySlug(@PathVariable String slug) {
    Optional<Organization> organization = organizationService.findBySlug(slug);
    return organization
        .map(org -> ResponseEntity.ok(OrganizationResponse.fromOrganization(org)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping
  public ResponseEntity<List<OrganizationResponse>> getUserOrganizations() {
    List<Organization> organizations = organizationService.getUserOrganizations();
    List<OrganizationResponse> responses =
        organizations.stream().map(OrganizationResponse::fromOrganization).toList();
    return ResponseEntity.ok(responses);
  }

  @PutMapping("/{organizationId}")
  public ResponseEntity<OrganizationResponse> updateOrganization(
      @PathVariable UUID organizationId, @Valid @RequestBody UpdateOrganizationRequest request) {

    Organization organization =
        organizationService.updateOrganization(organizationId, request.name(), request.settings());
    return ResponseEntity.ok(OrganizationResponse.fromOrganization(organization));
  }

  @PutMapping("/{organizationId}/settings")
  public ResponseEntity<OrganizationResponse> updateSettings(
      @PathVariable UUID organizationId, @Valid @RequestBody UpdateSettingsRequest request) {

    Organization organization =
        organizationService.updateSettings(organizationId, request.settings());
    return ResponseEntity.ok(OrganizationResponse.fromOrganization(organization));
  }

  @DeleteMapping("/{organizationId}")
  public ResponseEntity<Void> deleteOrganization(@PathVariable UUID organizationId) {
    organizationService.deleteOrganization(organizationId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{organizationId}/members")
  public ResponseEntity<List<OrganizationMemberInfoResponse>> getMembers(
      @PathVariable UUID organizationId) {
    List<OrganizationService.OrganizationMemberInfo> members =
        organizationService.getOrganizationMembers(organizationId);
    List<OrganizationMemberInfoResponse> responses =
        members.stream().map(OrganizationMemberInfoResponse::fromMemberInfo).toList();
    return ResponseEntity.ok(responses);
  }

  @PostMapping("/{organizationId}/invitations")
  public ResponseEntity<InvitationResponse> inviteUser(
      @PathVariable UUID organizationId, @Valid @RequestBody InviteUserRequest request) {

    Invitation invitation =
        organizationService.inviteUser(organizationId, request.email(), request.role());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(InvitationResponse.fromInvitation(invitation));
  }

  @PostMapping("/invitations/{token}/accept")
  public ResponseEntity<OrganizationMemberResponse> acceptInvitation(@PathVariable String token) {
    OrganizationMember member = organizationService.acceptInvitation(token);
    return ResponseEntity.ok(OrganizationMemberResponse.fromMember(member));
  }

  @PostMapping("/invitations/{token}/decline")
  public ResponseEntity<Void> declineInvitation(@PathVariable String token) {
    organizationService.declineInvitation(token);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{organizationId}/invitations")
  public ResponseEntity<List<InvitationResponse>> getPendingInvitations(
      @PathVariable UUID organizationId) {
    List<Invitation> invitations = organizationService.getPendingInvitations(organizationId);
    List<InvitationResponse> responses =
        invitations.stream().map(InvitationResponse::fromInvitation).toList();
    return ResponseEntity.ok(responses);
  }

  @DeleteMapping("/invitations/{invitationId}")
  public ResponseEntity<Void> revokeInvitation(@PathVariable UUID invitationId) {
    organizationService.revokeInvitation(invitationId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{organizationId}/members/{userId}")
  public ResponseEntity<Void> removeMember(
      @PathVariable UUID organizationId, @PathVariable UUID userId) {

    organizationService.removeMember(organizationId, userId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{organizationId}/members/{userId}/role")
  public ResponseEntity<OrganizationMemberResponse> updateMemberRole(
      @PathVariable UUID organizationId,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateMemberRoleRequest request) {

    OrganizationMember member =
        organizationService.updateMemberRole(organizationId, userId, request.role());
    return ResponseEntity.ok(OrganizationMemberResponse.fromMember(member));
  }

  public record OrganizationResponse(
      UUID id,
      String name,
      String slug,
      UUID ownerId,
      Map<String, Object> settings,
      Instant createdAt,
      Instant updatedAt) {
    public static OrganizationResponse fromOrganization(Organization organization) {
      return new OrganizationResponse(
          organization.getId(),
          organization.getName(),
          organization.getSlug(),
          organization.getOwnerId(),
          organization.getSettings(),
          organization.getCreatedAt(),
          organization.getUpdatedAt());
    }
  }

  public record OrganizationMemberInfoResponse(
      UUID userId,
      String userEmail,
      String userName,
      OrganizationMember.Role role,
      Instant joinedAt) {
    public static OrganizationMemberInfoResponse fromMemberInfo(
        OrganizationService.OrganizationMemberInfo info) {
      return new OrganizationMemberInfoResponse(
          info.userId(), info.userEmail(), info.userName(), info.role(), info.joinedAt());
    }
  }

  public record OrganizationMemberResponse(
      UUID id, UUID userId, UUID organizationId, OrganizationMember.Role role, Instant joinedAt) {
    public static OrganizationMemberResponse fromMember(OrganizationMember member) {
      return new OrganizationMemberResponse(
          member.getId(),
          member.getUserId(),
          member.getOrganizationId(),
          member.getRole(),
          member.getJoinedAt());
    }
  }

  public record InvitationResponse(
      UUID id,
      UUID organizationId,
      UUID invitedBy,
      String email,
      OrganizationMember.Role role,
      Invitation.Status status,
      String token,
      Instant expiresAt,
      Instant createdAt) {
    public static InvitationResponse fromInvitation(Invitation invitation) {
      return new InvitationResponse(
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

  public record CreateOrganizationRequest(
      @NotBlank String name, @NotBlank String slug, Map<String, Object> settings) {}

  public record UpdateOrganizationRequest(@NotBlank String name, Map<String, Object> settings) {}

  public record UpdateSettingsRequest(@NotNull Map<String, Object> settings) {}

  public record InviteUserRequest(
      @Email @NotBlank String email, @NotNull OrganizationMember.Role role) {}

  public record UpdateMemberRoleRequest(@NotNull OrganizationMember.Role role) {}
}
