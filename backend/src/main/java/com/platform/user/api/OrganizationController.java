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

@RestController
@RequestMapping("/api/v1/organizations")
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

  private final OrganizationManagementService organizationManagementService;

  public OrganizationController(OrganizationManagementService organizationManagementService) {
    this.organizationManagementService = organizationManagementService;
  }

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

  @DeleteMapping("/{organizationId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<Void> deleteOrganization(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {
    organizationManagementService.deleteOrganization(organizationId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{organizationId}/members")
  @PreAuthorize("@tenantGuard.canAccessOrganization(#principal, #organizationId)")
  public ResponseEntity<List<OrganizationMemberInfoResponse>> getMembers(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {

    List<OrganizationMemberInfoResponse> responses =
        organizationManagementService.getMembers(organizationId);
    return ResponseEntity.ok(responses);
  }

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

  @PostMapping("/invitations/{token}/accept")
  @PreAuthorize("permitAll()")
  public ResponseEntity<OrganizationMemberResponse> acceptInvitation(@PathVariable String token) {
    OrganizationMemberResponse member = organizationManagementService.acceptInvitation(token);
    return ResponseEntity.ok(member);
  }

  @PostMapping("/invitations/{token}/decline")
  @PreAuthorize("permitAll()")
  public ResponseEntity<Void> declineInvitation(@PathVariable String token) {
    organizationManagementService.declineInvitation(token);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{organizationId}/invitations")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<List<InvitationResponse>> getPendingInvitations(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId) {

    List<InvitationResponse> responses =
        organizationManagementService.getPendingInvitations(organizationId);
    return ResponseEntity.ok(responses);
  }

  @DeleteMapping("/invitations/{invitationId}")
  @PreAuthorize("@tenantGuard.isAdmin(#principal)")
  public ResponseEntity<Void> revokeInvitation(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable UUID invitationId) {
    organizationManagementService.revokeInvitation(invitationId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{organizationId}/members/{userId}")
  @PreAuthorize("@tenantGuard.canManageOrganization(#principal, #organizationId)")
  public ResponseEntity<Void> removeMember(
      @AuthenticationPrincipal @P("principal") PlatformUserPrincipal principal,
      @PathVariable("organizationId") @P("organizationId") UUID organizationId,
      @PathVariable UUID userId) {

    organizationManagementService.removeMember(organizationId, userId);
    return ResponseEntity.noContent().build();
  }

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
