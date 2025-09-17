package com.platform.user.internal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.audit.internal.AuditService;
import com.platform.shared.security.TenantContext;
import com.platform.user.events.InvitationCreatedEvent;
import com.platform.user.events.OrganizationCreatedEvent;

/** Service for managing organization operations and team collaboration. */
@Service
@Transactional
public class OrganizationService {

  private static final Logger logger = LoggerFactory.getLogger(OrganizationService.class);

  private final OrganizationRepository organizationRepository;
  private final OrganizationMemberRepository memberRepository;
  private final InvitationRepository invitationRepository;
  private final UserRepository userRepository;
  private final UserService userService;
  private final ApplicationEventPublisher eventPublisher;
  private final AuditService auditService;

  public OrganizationService(
      OrganizationRepository organizationRepository,
      OrganizationMemberRepository memberRepository,
      InvitationRepository invitationRepository,
      UserRepository userRepository,
      UserService userService,
      ApplicationEventPublisher eventPublisher,
      AuditService auditService) {
    this.organizationRepository = organizationRepository;
    this.memberRepository = memberRepository;
    this.invitationRepository = invitationRepository;
    this.userRepository = userRepository;
    this.userService = userService;
    this.eventPublisher = eventPublisher;
    this.auditService = auditService;
  }

  /** Create a new organization */
  public Organization createOrganization(String name, String slug, Map<String, Object> settings) {
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId == null) {
      throw new SecurityException("Authentication required");
    }

    // Audit organization creation attempt
    auditService.logEvent(
        "ORGANIZATION_CREATION_STARTED",
        "ORGANIZATION",
        slug,
        "CREATE",
        Map.of(
            "name",
            name,
            "slug",
            slug,
            "creator_user_id",
            currentUserId.toString(),
            "has_settings",
            settings != null && !settings.isEmpty()),
        null,
        "system",
        "OrganizationService",
        Map.of("action", "organization_creation_started"));

    // Validate slug is unique
    if (organizationRepository.existsBySlugAndDeletedAtIsNull(slug)) {
      // Audit failed creation attempt
      auditService.logEvent(
          "ORGANIZATION_CREATION_FAILED",
          "ORGANIZATION",
          slug,
          "CREATE",
          Map.of(
              "name",
              name,
              "slug",
              slug,
              "creator_user_id",
              currentUserId.toString(),
              "reason",
              "Organization slug already exists"),
          null,
          "system",
          "OrganizationService",
          Map.of("error", "duplicate_slug"));

      throw new IllegalArgumentException("Organization slug already exists: " + slug);
    }

    // Create organization
    Organization organization = new Organization(name, slug, currentUserId);
    if (settings != null && !settings.isEmpty()) {
      organization.updateSettings(settings);
    }

    Organization savedOrganization = organizationRepository.save(organization);

    // Add creator as owner
    OrganizationMember ownerMember =
        OrganizationMember.createOwner(currentUserId, savedOrganization.getId());
    memberRepository.save(ownerMember);

    // Audit successful organization creation
    auditService.logEvent(
        "ORGANIZATION_CREATED",
        "ORGANIZATION",
        savedOrganization.getId().toString(),
        "CREATE",
        Map.of(
            "organization_id",
            savedOrganization.getId().toString(),
            "name",
            name,
            "slug",
            slug,
            "creator_user_id",
            currentUserId.toString(),
            "has_settings",
            settings != null && !settings.isEmpty(),
            "owner_member_id",
            ownerMember.getId().toString()),
        Map.of(
            "organization_id", savedOrganization.getId().toString(),
            "created_at", savedOrganization.getCreatedAt().toString(),
            "owner_user_id", currentUserId.toString(),
            "slug", slug),
        "system",
        "OrganizationService",
        Map.of("success", "organization_created"));

    logger.info(
        "Created organization: {} with owner: {}", savedOrganization.getId(), currentUserId);

    // Publish organization created event
    publishOrganizationCreatedEvent(savedOrganization, currentUserId);

    return savedOrganization;
  }

  /** Get organization by ID */
  @Transactional(readOnly = true)
  public Optional<Organization> findById(UUID organizationId) {
    return organizationRepository.findById(organizationId).filter(org -> !org.isDeleted());
  }

  /** Get organization by slug */
  @Transactional(readOnly = true)
  public Optional<Organization> findBySlug(String slug) {
    return organizationRepository.findBySlugAndDeletedAtIsNull(slug);
  }

  /** Get organizations for current user */
  @Transactional(readOnly = true)
  public List<Organization> getUserOrganizations() {
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId == null) {
      return List.of();
    }

    return memberRepository.findOrganizationsForUser(currentUserId).stream()
        .map(member -> organizationRepository.findById(member.getOrganizationId()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(java.util.stream.Collectors.toList());
  }

  /** Update organization details */
  public Organization updateOrganization(
      UUID organizationId, String name, Map<String, Object> settings) {
    Organization organization =
        findById(organizationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Organization not found: " + organizationId));

    validateOrganizationAccess(organization, OrganizationMember.Role.ADMIN);

    // Store old values for audit trail
    String oldName = organization.getName();
    Map<String, Object> oldSettings = organization.getSettings();

    // Audit organization update attempt
    auditService.logEvent(
        "ORGANIZATION_UPDATE_STARTED",
        "ORGANIZATION",
        organizationId.toString(),
        "UPDATE",
        Map.of(
            "organization_id",
            organizationId.toString(),
            "old_name",
            oldName != null ? oldName : "",
            "new_name",
            name != null ? name : "",
            "settings_updated",
            settings != null && !settings.isEmpty(),
            "admin_user_id",
            TenantContext.getCurrentUserId().toString()),
        null,
        "system",
        "OrganizationService",
        Map.of("action", "organization_update_started"));

    organization.updateDetails(name, settings);
    Organization savedOrganization = organizationRepository.save(organization);

    // Audit successful organization update
    auditService.logEvent(
        "ORGANIZATION_UPDATED",
        "ORGANIZATION",
        organizationId.toString(),
        "UPDATE",
        Map.of(
            "organization_id", organizationId.toString(),
            "name_changed", !oldName.equals(name),
            "settings_changed", settings != null && !settings.isEmpty(),
            "admin_user_id", TenantContext.getCurrentUserId().toString()),
        Map.of(
            "organization_id", organizationId.toString(),
            "updated_at", savedOrganization.getUpdatedAt().toString(),
            "name", name != null ? name : ""),
        "system",
        "OrganizationService",
        Map.of("success", "organization_updated"));

    logger.info("Updated organization: {}", organizationId);
    return savedOrganization;
  }

  /** Update organization settings only */
  public Organization updateSettings(UUID organizationId, Map<String, Object> settings) {
    Organization organization =
        findById(organizationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Organization not found: " + organizationId));

    validateOrganizationAccess(organization, OrganizationMember.Role.ADMIN);

    organization.updateSettings(settings);
    Organization savedOrganization = organizationRepository.save(organization);

    logger.info("Updated settings for organization: {}", organizationId);
    return savedOrganization;
  }

  /** Delete organization (soft delete) */
  public void deleteOrganization(UUID organizationId) {
    Organization organization =
        findById(organizationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Organization not found: " + organizationId));

    validateOrganizationAccess(organization, OrganizationMember.Role.OWNER);

    // Get member count for audit
    long memberCount = memberRepository.countByOrganizationId(organizationId);
    UUID currentUserId = TenantContext.getCurrentUserId();

    // Audit organization deletion attempt (GDPR critical)
    auditService.logEvent(
        "ORGANIZATION_DELETION_STARTED",
        "ORGANIZATION",
        organizationId.toString(),
        "DELETE",
        Map.of(
            "organization_id", organizationId.toString(),
            "name", organization.getName(),
            "slug", organization.getSlug(),
            "member_count", memberCount,
            "deletion_type", "soft_delete",
            "owner_user_id", currentUserId.toString()),
        null,
        "system",
        "OrganizationService",
        Map.of("action", "organization_deletion_started"));

    // Store data for audit before deletion
    String orgName = organization.getName();
    String orgSlug = organization.getSlug();
    Instant orgCreatedAt = organization.getCreatedAt();

    organization.markAsDeleted();
    Organization deletedOrganization = organizationRepository.save(organization);

    // Audit successful organization deletion (GDPR compliance audit)
    auditService.logEvent(
        "ORGANIZATION_DELETED",
        "ORGANIZATION",
        organizationId.toString(),
        "DELETE",
        Map.of(
            "organization_id",
            organizationId.toString(),
            "name",
            orgName,
            "slug",
            orgSlug,
            "member_count",
            memberCount,
            "deletion_type",
            "soft_delete",
            "owner_user_id",
            currentUserId.toString(),
            "organization_age_days",
            java.time.Duration.between(orgCreatedAt, Instant.now()).toDays()),
        Map.of(
            "organization_id", organizationId.toString(),
            "deleted_at", deletedOrganization.getDeletedAt().toString(),
            "deletion_confirmed", "true"),
        "system",
        "OrganizationService",
        Map.of("gdpr_compliance", "organization_deleted"));

    logger.info("Soft deleted organization: {}", organizationId);
  }

  /** Get organization members */
  @Transactional(readOnly = true)
  public List<OrganizationMemberInfo> getOrganizationMembers(UUID organizationId) {
    validateOrganizationAccess(organizationId, OrganizationMember.Role.MEMBER);
    return memberRepository.findMemberInfoByOrganization(organizationId).stream()
        .map(
            member -> {
              Optional<User> user = userRepository.findById(member.getUserId());
              return user.map(
                      u ->
                          new OrganizationMemberInfo(
                              u.getId(),
                              u.getEmail().getValue(),
                              u.getName(),
                              member.getRole(),
                              member.getJoinedAt()))
                  .orElse(null);
            })
        .filter(info -> info != null)
        .collect(java.util.stream.Collectors.toList());
  }

  /** Invite user to organization */
  public Invitation inviteUser(UUID organizationId, String email, OrganizationMember.Role role) {
    validateOrganizationAccess(organizationId, OrganizationMember.Role.ADMIN);

    UUID currentUserId = TenantContext.getCurrentUserId();

    // Audit invitation creation attempt
    auditService.logEvent(
        "ORGANIZATION_INVITATION_STARTED",
        "ORGANIZATION",
        organizationId.toString(),
        "INVITE",
        Map.of(
            "organization_id", organizationId.toString(),
            "invitee_email", email,
            "invited_role", role.toString(),
            "inviter_user_id", currentUserId.toString()),
        null,
        "system",
        "OrganizationService",
        Map.of("action", "invitation_started"));

    // Check if user is already a member
    Optional<User> existingUser = userService.findByEmail(email);
    if (existingUser.isPresent()) {
      boolean isAlreadyMember =
          memberRepository.existsByUserIdAndOrganizationId(
              existingUser.get().getId(), organizationId);
      if (isAlreadyMember) {
        // Audit failed invitation attempt
        auditService.logEvent(
            "ORGANIZATION_INVITATION_FAILED",
            "ORGANIZATION",
            organizationId.toString(),
            "INVITE",
            Map.of(
                "organization_id",
                organizationId.toString(),
                "invitee_email",
                email,
                "existing_user_id",
                existingUser.get().getId().toString(),
                "reason",
                "User is already a member"),
            null,
            "system",
            "OrganizationService",
            Map.of("error", "already_member"));

        throw new IllegalArgumentException("User is already a member of this organization");
      }
    }

    // Check for existing pending invitation
    Optional<Invitation> existingInvitation =
        invitationRepository.findPendingInvitation(organizationId, email);
    if (existingInvitation.isPresent()) {
      // Audit failed invitation attempt
      auditService.logEvent(
          "ORGANIZATION_INVITATION_FAILED",
          "ORGANIZATION",
          organizationId.toString(),
          "INVITE",
          Map.of(
              "organization_id",
              organizationId.toString(),
              "invitee_email",
              email,
              "existing_invitation_id",
              existingInvitation.get().getId().toString(),
              "reason",
              "Pending invitation already exists"),
          null,
          "system",
          "OrganizationService",
          Map.of("error", "pending_invitation_exists"));

      throw new IllegalArgumentException("Pending invitation already exists for this email");
    }

    // Create invitation
    Invitation invitation =
        Invitation.create(
            organizationId,
            currentUserId,
            email,
            role,
            Instant.now().plus(7, ChronoUnit.DAYS) // 7 days to accept
            );

    Invitation savedInvitation = invitationRepository.save(invitation);

    // Audit successful invitation creation
    auditService.logEvent(
        "ORGANIZATION_INVITATION_CREATED",
        "ORGANIZATION",
        organizationId.toString(),
        "INVITE",
        Map.of(
            "organization_id", organizationId.toString(),
            "invitation_id", savedInvitation.getId().toString(),
            "invitee_email", email,
            "invited_role", role.toString(),
            "inviter_user_id", currentUserId.toString(),
            "expires_at", savedInvitation.getExpiresAt().toString()),
        Map.of(
            "invitation_id", savedInvitation.getId().toString(),
            "invitation_token", savedInvitation.getToken(),
            "created_at", savedInvitation.getCreatedAt().toString()),
        "system",
        "OrganizationService",
        Map.of("success", "invitation_created"));

    // Fetch organization details for the event
    Organization organization =
        organizationRepository
            .findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

    logger.info("Created invitation for {} to organization: {}", email, organizationId);

    // Publish invitation created event (for email notification)
    publishInvitationCreatedEvent(savedInvitation, organization, currentUserId, role);

    return savedInvitation;
  }

  /** Accept invitation */
  public OrganizationMember acceptInvitation(String token) {
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId == null) {
      throw new SecurityException("Authentication required");
    }

    // Audit invitation acceptance attempt
    auditService.logEvent(
        "ORGANIZATION_INVITATION_ACCEPTANCE_STARTED",
        "ORGANIZATION",
        "invitation",
        "ACCEPT",
        Map.of(
            "invitation_token",
            token.substring(0, Math.min(token.length(), 8)) + "...", // Partial token for security
            "accepting_user_id",
            currentUserId.toString()),
        null,
        "system",
        "OrganizationService",
        Map.of("action", "invitation_acceptance_started"));

    Invitation invitation =
        invitationRepository
            .findByTokenAndStatus(token, Invitation.Status.PENDING)
            .orElseThrow(
                () -> {
                  // Audit failed invitation acceptance
                  auditService.logEvent(
                      "ORGANIZATION_INVITATION_ACCEPTANCE_FAILED",
                      "ORGANIZATION",
                      "invitation",
                      "ACCEPT",
                      Map.of(
                          "invitation_token",
                          token.substring(0, Math.min(token.length(), 8)) + "...",
                          "accepting_user_id",
                          currentUserId.toString(),
                          "reason",
                          "Invalid or expired invitation token"),
                      null,
                      "system",
                      "OrganizationService",
                      Map.of("error", "invalid_token"));

                  return new IllegalArgumentException("Invalid or expired invitation");
                });

    if (invitation.isExpired()) {
      invitation.markAsExpired();
      invitationRepository.save(invitation);

      // Audit expired invitation
      auditService.logEvent(
          "ORGANIZATION_INVITATION_ACCEPTANCE_FAILED",
          "ORGANIZATION",
          invitation.getOrganizationId().toString(),
          "ACCEPT",
          Map.of(
              "invitation_id", invitation.getId().toString(),
              "organization_id", invitation.getOrganizationId().toString(),
              "accepting_user_id", currentUserId.toString(),
              "reason", "Invitation has expired",
              "expired_at", invitation.getExpiresAt().toString()),
          null,
          "system",
          "OrganizationService",
          Map.of("error", "invitation_expired"));

      throw new IllegalArgumentException("Invitation has expired");
    }

    // Verify current user's email matches invitation
    User currentUser =
        userService
            .findById(currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

    if (!currentUser.getEmail().getValue().equals(invitation.getEmail())) {
      // Audit email mismatch
      auditService.logEvent(
          "ORGANIZATION_INVITATION_ACCEPTANCE_FAILED",
          "ORGANIZATION",
          invitation.getOrganizationId().toString(),
          "ACCEPT",
          Map.of(
              "invitation_id", invitation.getId().toString(),
              "organization_id", invitation.getOrganizationId().toString(),
              "accepting_user_id", currentUserId.toString(),
              "invitation_email", invitation.getEmail(),
              "user_email", currentUser.getEmail().getValue(),
              "reason", "Invitation email does not match current user"),
          null,
          "system",
          "OrganizationService",
          Map.of("error", "email_mismatch"));

      throw new IllegalArgumentException("Invitation email does not match current user");
    }

    // Check if user is already a member
    boolean isAlreadyMember =
        memberRepository.existsByUserIdAndOrganizationId(
            currentUserId, invitation.getOrganizationId());
    if (isAlreadyMember) {
      // Audit already member error
      auditService.logEvent(
          "ORGANIZATION_INVITATION_ACCEPTANCE_FAILED",
          "ORGANIZATION",
          invitation.getOrganizationId().toString(),
          "ACCEPT",
          Map.of(
              "invitation_id", invitation.getId().toString(),
              "organization_id", invitation.getOrganizationId().toString(),
              "accepting_user_id", currentUserId.toString(),
              "reason", "User is already a member of this organization"),
          null,
          "system",
          "OrganizationService",
          Map.of("error", "already_member"));

      throw new IllegalArgumentException("User is already a member of this organization");
    }

    // Add user to organization
    OrganizationMember member =
        new OrganizationMember(currentUserId, invitation.getOrganizationId(), invitation.getRole());
    OrganizationMember savedMember = memberRepository.save(member);

    // Mark invitation as accepted
    invitation.accept();
    invitationRepository.save(invitation);

    // Audit successful invitation acceptance
    auditService.logEvent(
        "ORGANIZATION_INVITATION_ACCEPTED",
        "ORGANIZATION",
        invitation.getOrganizationId().toString(),
        "ACCEPT",
        Map.of(
            "invitation_id", invitation.getId().toString(),
            "organization_id", invitation.getOrganizationId().toString(),
            "new_member_user_id", currentUserId.toString(),
            "assigned_role", invitation.getRole().toString(),
            "member_id", savedMember.getId().toString()),
        Map.of(
            "member_id", savedMember.getId().toString(),
            "joined_at", savedMember.getJoinedAt().toString(),
            "role", savedMember.getRole().toString()),
        "system",
        "OrganizationService",
        Map.of("success", "invitation_accepted"));

    logger.info(
        "User {} accepted invitation and joined organization: {}",
        currentUserId,
        invitation.getOrganizationId());

    return savedMember;
  }

  /** Decline invitation */
  public void declineInvitation(String token) {
    Invitation invitation =
        invitationRepository
            .findByTokenAndStatus(token, Invitation.Status.PENDING)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invitation"));

    invitation.decline();
    invitationRepository.save(invitation);

    logger.info(
        "Invitation declined for email: {} to organization: {}",
        invitation.getEmail(),
        invitation.getOrganizationId());
  }

  /** Remove member from organization */
  public void removeMember(UUID organizationId, UUID userId) {
    validateOrganizationAccess(organizationId, OrganizationMember.Role.ADMIN);

    OrganizationMember member =
        memberRepository
            .findByUserIdAndOrganizationId(userId, organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));

    UUID currentUserId = TenantContext.getCurrentUserId();

    // Audit member removal attempt
    auditService.logEvent(
        "ORGANIZATION_MEMBER_REMOVAL_STARTED",
        "ORGANIZATION",
        organizationId.toString(),
        "REMOVE_MEMBER",
        Map.of(
            "organization_id", organizationId.toString(),
            "member_user_id", userId.toString(),
            "member_role", member.getRole().toString(),
            "removing_admin_user_id", currentUserId.toString(),
            "member_joined_at", member.getJoinedAt().toString()),
        null,
        "system",
        "OrganizationService",
        Map.of("action", "member_removal_started"));

    // Prevent removing the last owner
    if (member.isOwner()) {
      long ownerCount = memberRepository.countOwners(organizationId);
      if (ownerCount <= 1) {
        // Audit failed removal attempt
        auditService.logEvent(
            "ORGANIZATION_MEMBER_REMOVAL_FAILED",
            "ORGANIZATION",
            organizationId.toString(),
            "REMOVE_MEMBER",
            Map.of(
                "organization_id", organizationId.toString(),
                "member_user_id", userId.toString(),
                "member_role", member.getRole().toString(),
                "removing_admin_user_id", currentUserId.toString(),
                "reason", "Cannot remove the last owner"),
            null,
            "system",
            "OrganizationService",
            Map.of("error", "last_owner_protection"));

        throw new IllegalArgumentException("Cannot remove the last owner");
      }
    }

    // Store member data for audit
    String memberRole = member.getRole().toString();
    Instant memberJoinedAt = member.getJoinedAt();

    memberRepository.delete(member);

    // Audit successful member removal
    auditService.logEvent(
        "ORGANIZATION_MEMBER_REMOVED",
        "ORGANIZATION",
        organizationId.toString(),
        "REMOVE_MEMBER",
        Map.of(
            "organization_id", organizationId.toString(),
            "removed_member_user_id", userId.toString(),
            "removed_member_role", memberRole,
            "removing_admin_user_id", currentUserId.toString(),
            "membership_duration_days",
                java.time.Duration.between(memberJoinedAt, Instant.now()).toDays()),
        Map.of(
            "removed_at", Instant.now().toString(),
            "was_member_for_days",
                java.time.Duration.between(memberJoinedAt, Instant.now()).toDays()),
        "system",
        "OrganizationService",
        Map.of("success", "member_removed"));

    logger.info("Removed user {} from organization: {}", userId, organizationId);
  }

  /** Update member role */
  public OrganizationMember updateMemberRole(
      UUID organizationId, UUID userId, OrganizationMember.Role newRole) {
    validateOrganizationAccess(organizationId, OrganizationMember.Role.ADMIN);

    OrganizationMember member =
        memberRepository
            .findByUserIdAndOrganizationId(userId, organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));

    UUID currentUserId = TenantContext.getCurrentUserId();
    OrganizationMember.Role oldRole = member.getRole();

    // Audit role change attempt
    auditService.logEvent(
        "ORGANIZATION_MEMBER_ROLE_CHANGE_STARTED",
        "ORGANIZATION",
        organizationId.toString(),
        "CHANGE_ROLE",
        Map.of(
            "organization_id", organizationId.toString(),
            "member_user_id", userId.toString(),
            "old_role", oldRole.toString(),
            "new_role", newRole.toString(),
            "changing_admin_user_id", currentUserId.toString()),
        null,
        "system",
        "OrganizationService",
        Map.of("action", "role_change_started"));

    // Prevent changing the last owner
    if (member.isOwner() && newRole != OrganizationMember.Role.OWNER) {
      long ownerCount = memberRepository.countOwners(organizationId);
      if (ownerCount <= 1) {
        // Audit failed role change attempt
        auditService.logEvent(
            "ORGANIZATION_MEMBER_ROLE_CHANGE_FAILED",
            "ORGANIZATION",
            organizationId.toString(),
            "CHANGE_ROLE",
            Map.of(
                "organization_id", organizationId.toString(),
                "member_user_id", userId.toString(),
                "old_role", oldRole.toString(),
                "attempted_new_role", newRole.toString(),
                "changing_admin_user_id", currentUserId.toString(),
                "reason", "Cannot change role of the last owner"),
            null,
            "system",
            "OrganizationService",
            Map.of("error", "last_owner_protection"));

        throw new IllegalArgumentException("Cannot change role of the last owner");
      }
    }

    member.changeRole(newRole);
    OrganizationMember savedMember = memberRepository.save(member);

    // Audit successful role change
    auditService.logEvent(
        "ORGANIZATION_MEMBER_ROLE_CHANGED",
        "ORGANIZATION",
        organizationId.toString(),
        "CHANGE_ROLE",
        Map.of(
            "organization_id", organizationId.toString(),
            "member_user_id", userId.toString(),
            "old_role", oldRole.toString(),
            "new_role", newRole.toString(),
            "changing_admin_user_id", currentUserId.toString(),
            "member_id", savedMember.getId().toString()),
        Map.of(
            "member_id", savedMember.getId().toString(),
            "role_changed_at", Instant.now().toString(),
            "new_role", newRole.toString()),
        "system",
        "OrganizationService",
        Map.of("success", "role_changed"));

    logger.info(
        "Updated role for user {} in organization {} to: {}", userId, organizationId, newRole);

    return savedMember;
  }

  /** Get pending invitations for organization */
  @Transactional(readOnly = true)
  public List<Invitation> getPendingInvitations(UUID organizationId) {
    validateOrganizationAccess(organizationId, OrganizationMember.Role.ADMIN);
    return invitationRepository.findPendingInvitationsForOrganization(organizationId);
  }

  /** Revoke invitation */
  public void revokeInvitation(UUID invitationId) {
    Invitation invitation =
        invitationRepository
            .findById(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));

    validateOrganizationAccess(invitation.getOrganizationId(), OrganizationMember.Role.ADMIN);

    invitation.revoke();
    invitationRepository.save(invitation);

    logger.info("Revoked invitation: {}", invitationId);
  }

  // Validation and helper methods

  private void validateOrganizationAccess(
      UUID organizationId, OrganizationMember.Role requiredRole) {
    Organization organization =
        findById(organizationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Organization not found: " + organizationId));
    validateOrganizationAccess(organization, requiredRole);
  }

  private void validateOrganizationAccess(
      Organization organization, OrganizationMember.Role requiredRole) {
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId == null) {
      throw new SecurityException("Authentication required");
    }

    OrganizationMember member =
        memberRepository
            .findByUserIdAndOrganizationId(currentUserId, organization.getId())
            .orElseThrow(
                () -> new SecurityException("Access denied - not a member of this organization"));

    // Check role hierarchy
    boolean hasAccess =
        switch (requiredRole) {
          case OWNER -> member.isOwner();
          case ADMIN -> member.isAdmin();
          case MEMBER -> member.isMember();
        };

    if (!hasAccess) {
      throw new SecurityException("Access denied - insufficient privileges");
    }
  }

  private void publishOrganizationCreatedEvent(Organization organization, UUID creatorId) {
    OrganizationCreatedEvent event =
        new OrganizationCreatedEvent(
            organization.getId(), organization.getName(), organization.getSlug(), creatorId);

    eventPublisher.publishEvent(event);
    logger.debug("Published organization created event for: {}", organization.getId());
  }

  private void publishInvitationCreatedEvent(
      Invitation invitation,
      Organization organization,
      UUID invitedBy,
      OrganizationMember.Role role) {
    InvitationCreatedEvent event =
        new InvitationCreatedEvent(
            invitation.getId(),
            invitation.getEmail(),
            organization.getId(),
            organization.getName(),
            role,
            invitedBy);

    eventPublisher.publishEvent(event);
    logger.debug("Published invitation created event for: {}", invitation.getEmail());
  }

  /** Organization member information DTO */
  public record OrganizationMemberInfo(
      UUID userId,
      String userEmail,
      String userName,
      OrganizationMember.Role role,
      Instant joinedAt) {}
}
