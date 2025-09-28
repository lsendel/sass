package com.platform.user.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.user.api.UserDto.CreateOrganizationRequest;
import com.platform.user.api.UserDto.InvitationResponse;
import com.platform.user.api.UserDto.InviteUserRequest;
import com.platform.user.api.UserDto.OrganizationMemberInfoResponse;
import com.platform.user.api.UserDto.OrganizationMemberResponse;
import com.platform.user.api.UserDto.OrganizationResponse;
import com.platform.user.api.UserDto.UpdateMemberRoleRequest;
import com.platform.user.api.UserDto.UpdateOrganizationRequest;
import com.platform.user.api.UserDto.UpdateSettingsRequest;

/**
 * REST controller for managing organizations, memberships, and invitations.
 *
 * <p>This controller provides endpoints for creating and managing organizations, inviting users,
 * handling invitations, and managing member roles. All endpoints require authentication and are
 * protected by authorization rules.
 */
@RestController
@RequestMapping("/api/v1/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

  private final OrganizationManagementService organizationManagementService;

  /**
   * Constructs a new OrganizationController.
   *
   * @param organizationManagementService the service for managing organizations
   */
  public OrganizationController(OrganizationManagementService organizationManagementService) {
    this.organizationManagementService = organizationManagementService;
  }

  /**
   * Creates a new organization.
   *
   * @param principal the authenticated user principal
   * @param request the request body containing the details for the new organization
   * @return a {@link ResponseEntity} with the created {@link OrganizationResponse}
   */
  @PostMapping
  public ResponseEntity<OrganizationResponse> createOrganization(
      @AuthenticationPrincipal PlatformUserPrincipal principal,
      @Valid @RequestBody CreateOrganizationRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    OrganizationResponse organization =
        organizationManagementService.createOrganization(
            request.name(), request.slug(), request.settings());
    return ResponseEntity.status(HttpStatus.CREATED).body(organization);
  }

  /**
   * Retrieves an organization by its ID.
   *
   * @param principal the authenticated user principal
   * @param organizationId the ID of the organization to retrieve
   * @return a {@link ResponseEntity} containing the {@link OrganizationResponse} if found, otherwise
   *     a 404 Not Found response
   */
  @GetMapping("/{organizationId}")
  @PreAuthorize("@tenantGuard.canAccessOrganization(#principal, #organizationId)")
  public ResponseEntity<OrganizationResponse> getOrganization(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {

    return organizationManagementService
        .findById(organizationId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieves an organization by its slug.
   *
   * @param principal the authenticated user principal
   * @param slug the slug of the organization to retrieve
   * @return a {@link ResponseEntity} containing the {@link OrganizationResponse} if found, otherwise
   *     a 404 Not Found response
   */
  @GetMapping("/slug/{slug}")
  public ResponseEntity<OrganizationResponse> getOrganizationBySlug(
      @AuthenticationPrincipal PlatformUserPrincipal principal, @PathVariable String slug) {
    Optional<OrganizationResponse> organization = organizationManagementService.findBySlug(slug);
    if (organization.isPresent()
        && (principal == null || !principal.belongsToOrganization(organization.get().id()))) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    return organization.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieves all organizations the current user is a member of.
   *
   * @param principal the authenticated user principal
   * @return a {@link ResponseEntity} containing a list of {@link OrganizationResponse}s
   */
  @GetMapping
  public ResponseEntity<List<OrganizationResponse>> getUserOrganizations(
      @AuthenticationPrincipal PlatformUserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    List<OrganizationResponse> responses = organizationManagementService.getUserOrganizations();
    return ResponseEntity.ok(responses);
  }

  // REMOVED: Test endpoint that exposed all organizations without proper authorization
  // This endpoint was a security vulnerability that could expose tenant data in production

  /**
   * Updates an organization's details.
   *
   * @param principal the authenticated user principal
   * @param organizationId the ID of the organization to update
   * @param request the request body containing the updated details
   * @return a {@link ResponseEntity} with the updated {@link OrganizationResponse}
   */
  @PutMapping("/{organizationId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<OrganizationResponse> updateOrganization(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId,
      @Valid @RequestBody UpdateOrganizationRequest request) {

    OrganizationResponse organization =
        organizationManagementService.updateOrganization(
            organizationId, request.name(), request.settings());
    return ResponseEntity.ok(organization);
  }

  /**
   * Updates an organization's settings.
   *
   * @param principal the authenticated user principal
   * @param organizationId the ID of the organization to update
   * @param request the request body containing the updated settings
   * @return a {@link ResponseEntity} with the updated {@link OrganizationResponse}
   */
  @PutMapping("/{organizationId}/settings")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<OrganizationResponse> updateSettings(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId,
      @Valid @RequestBody UpdateSettingsRequest request) {

    OrganizationResponse organization =
        organizationManagementService.updateSettings(organizationId, request.settings());
    return ResponseEntity.ok(organization);
  }

  /**
   * Deletes an organization.
   *
   * @param principal the authenticated user principal
   * @param organizationId the ID of the organization to delete
   * @return a {@link ResponseEntity} with no content
   */
  @DeleteMapping("/{organizationId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<Void> deleteOrganization(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    organizationManagementService.deleteOrganization(organizationId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Retrieves a list of all members in an organization.
   *
   * @param principal the authenticated user principal
   * @param organizationId the ID of the organization
   * @return a {@link ResponseEntity} containing a list of {@link OrganizationMemberInfoResponse}s
   */
  @GetMapping("/{organizationId}/members")
  @PreAuthorize("@tenantGuard.canAccessOrganization(#principal, #organizationId)")
  public ResponseEntity<List<OrganizationMemberInfoResponse>> getMembers(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {

    List<OrganizationMemberInfoResponse> responses =
        organizationManagementService.getMembers(organizationId);
    return ResponseEntity.ok(responses);
  }

  /**
   * Invites a user to join an organization.
   *
   * @param principal the authenticated user principal
   * @param organizationId the ID of the organization
   * @param request the request body containing the invitation details
   * @return a {@link ResponseEntity} with the created {@link InvitationResponse}
   */
  @PostMapping("/{organizationId}/invitations")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<InvitationResponse> inviteUser(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId,
      @Valid @RequestBody InviteUserRequest request) {

    InvitationResponse invitation =
        organizationManagementService.inviteUser(organizationId, request.email(), request.role());
    return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
  }

  /**
   * Accepts an invitation to join an organization.
   *
   * @param token the invitation token
   * @return a {@link ResponseEntity} with the new {@link OrganizationMemberResponse}
   */
  @PostMapping("/invitations/{token}/accept")
  @PreAuthorize("permitAll()")
  public ResponseEntity<OrganizationMemberResponse> acceptInvitation(@PathVariable String token) {
    OrganizationMemberResponse member = organizationManagementService.acceptInvitation(token);
    return ResponseEntity.ok(member);
  }

  /**
   * Declines an invitation to join an organization.
   *
   * @param token the invitation token
   * @return a {@link ResponseEntity} with no content
   */
  @PostMapping("/invitations/{token}/decline")
  @PreAuthorize("permitAll()")
  public ResponseEntity<Void> declineInvitation(@PathVariable String token) {
    organizationManagementService.declineInvitation(token);
    return ResponseEntity.noContent().build();
  }

  /**
   * Retrieves a list of pending invitations for an organization.
   *
   * @param principal the authenticated user principal
   * @param organizationId the ID of the organization
   * @return a {@link ResponseEntity} containing a list of {@link InvitationResponse}s
   */
  @GetMapping("/{organizationId}/invitations")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<InvitationResponse>> getPendingInvitations(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {

    List<InvitationResponse> responses =
        organizationManagementService.getPendingInvitations(organizationId);
    return ResponseEntity.ok(responses);
  }

  /**
   * Revokes a pending invitation.
   *
   * @param principal the authenticated user principal
   * @param invitationId the ID of the invitation to revoke
   * @return a {@link ResponseEntity} with no content
   */
  @DeleteMapping("/invitations/{invitationId}")
  @PreAuthorize("@tenantGuard.isAdmin(#principal)")
  public ResponseEntity<Void> revokeInvitation(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable UUID invitationId) {
    organizationManagementService.revokeInvitation(invitationId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Removes a member from an organization.
   *
   * @param principal the authenticated user principal
   * @param organizationId the ID of the organization
   * @param userId the ID of the user to remove
   * @return a {@link ResponseEntity} with no content
   */
  @DeleteMapping("/{organizationId}/members/{userId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<Void> removeMember(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId,
      @PathVariable UUID userId) {

    organizationManagementService.removeMember(organizationId, userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Updates the role of a member in an organization.
   *
   * @param principal the authenticated user principal
   * @param organizationId the ID of the organization
   * @param userId the ID of the user whose role is to be updated
   * @param request the request body containing the new role
   * @return a {@link ResponseEntity} with the updated {@link OrganizationMemberResponse}
   */
  @PutMapping("/{organizationId}/members/{userId}/role")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<OrganizationMemberResponse> updateMemberRole(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateMemberRoleRequest request) {

    OrganizationMemberResponse member =
        organizationManagementService.updateMemberRole(organizationId, userId, request.role());
    return ResponseEntity.ok(member);
  }
}
