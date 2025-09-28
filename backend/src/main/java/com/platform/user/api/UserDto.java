package com.platform.user.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A container for all Data Transfer Objects (DTOs) related to user and organization operations.
 *
 * <p>This class uses nested records and enums to define the data structures for API requests and
 * responses. This approach encapsulates all user-related data contracts, preventing the direct
 * exposure of internal domain entities.
 */
public class UserDto {

  /** Defines the roles a user can have within an organization at the API layer. */
  public enum Role {
    /** The owner of the organization with full access. */
    OWNER,
    /** An administrator with permissions to manage the organization. */
    ADMIN,
    /** A standard member of the organization. */
    MEMBER,
    /** A read-only member of the organization. */
    VIEWER
  }

  /** Defines the possible statuses of an invitation at the API layer. */
  public enum InvitationStatus {
    /** The invitation is pending and awaiting a response. */
    PENDING,
    /** The invitation has been accepted. */
    ACCEPTED,
    /** The invitation has been declined. */
    DECLINED,
    /** The invitation has expired. */
    EXPIRED,
    /** The invitation has been revoked by the sender. */
    REVOKED
  }

  /**
   * Represents the data for a user sent in API responses.
   *
   * @param id the unique identifier of the user
   * @param email the email address of the user
   * @param name the full name of the user
   * @param provider the authentication provider used by the user
   * @param preferences a map of the user's preferences
   * @param createdAt the timestamp when the user was created
   * @param lastActiveAt the timestamp of the user's last activity
   */
  public record UserResponse(
      UUID id,
      String email,
      String name,
      String provider,
      Map<String, Object> preferences,
      Instant createdAt,
      Instant lastActiveAt) {}

  /**
   * Represents the data required to update a user's profile.
   *
   * @param name the new name for the user
   * @param preferences a map of new preferences for the user
   */
  public record UpdateProfileRequest(
      @NotBlank String name, Map<String, Object> preferences) {}

  /**
   * Represents the data required to update a user's preferences.
   *
   * @param preferences a map of new preferences for the user
   */
  public record UpdatePreferencesRequest(@NotNull Map<String, Object> preferences) {}

  /**
   * Represents a paginated list of users.
   *
   * @param users the list of users on the current page
   * @param page the current page number
   * @param size the number of users per page
   * @param totalElements the total number of users across all pages
   * @param totalPages the total number of pages
   * @param first whether this is the first page
   * @param last whether this is the last page
   */
  public record PagedUserResponse(
      List<UserResponse> users,
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean first,
      boolean last) {}

  /**
   * Represents a summary of user-related statistics.
   *
   * @param totalUsers the total number of users
   * @param activeUsers the number of users active in a recent period
   * @param newUsersThisMonth the number of new users created in the current month
   * @param usersByProvider a map of user counts by authentication provider
   * @param averageSessionDuration an estimated average session duration in minutes
   */
  public record UserStatistics(
      long totalUsers,
      long activeUsers,
      long newUsersThisMonth,
      Map<String, Long> usersByProvider,
      double averageSessionDuration) {}

  /**
   * Represents the data for an organization sent in API responses.
   *
   * @param id the unique identifier of the organization
   * @param name the name of the organization
   * @param slug a unique, URL-friendly identifier for the organization
   * @param ownerId the ID of the user who owns the organization
   * @param settings a map of custom settings for the organization
   * @param createdAt the timestamp when the organization was created
   * @param updatedAt the timestamp when the organization was last updated
   */
  public record OrganizationResponse(
      UUID id,
      String name,
      String slug,
      UUID ownerId,
      Map<String, Object> settings,
      Instant createdAt,
      Instant updatedAt) {}

  /**
   * Represents information about a member of an organization.
   *
   * @param userId the ID of the user
   * @param userEmail the email address of the user
   * @param userName the name of the user
   * @param role the role of the member in the organization
   * @param joinedAt the timestamp when the member joined the organization
   */
  public record OrganizationMemberInfoResponse(
      UUID userId, String userEmail, String userName, Role role, Instant joinedAt) {}

  /**
   * Represents an organization membership record.
   *
   * @param id the unique identifier of the membership
   * @param userId the ID of the user
   * @param organizationId the ID of the organization
   * @param role the role of the member
   * @param joinedAt the timestamp when the member joined
   */
  public record OrganizationMemberResponse(
      UUID id, UUID userId, UUID organizationId, Role role, Instant joinedAt) {}

  /**
   * Represents an invitation to join an organization.
   *
   * @param id the unique identifier of the invitation
   * @param organizationId the ID of the organization
   * @param invitedBy the ID of the user who sent the invitation
   * @param email the email address of the recipient
   * @param role the role to be assigned upon acceptance
   * @param status the current status of the invitation
   * @param token the unique token for the invitation
   * @param expiresAt the timestamp when the invitation expires
   * @param createdAt the timestamp when the invitation was created
   */
  public record InvitationResponse(
      UUID id,
      UUID organizationId,
      UUID invitedBy,
      String email,
      Role role,
      InvitationStatus status,
      String token,
      Instant expiresAt,
      Instant createdAt) {}

  /**
   * Represents the data required to create a new organization.
   *
   * @param name the name of the organization
   * @param slug a unique, URL-friendly slug for the organization
   * @param settings a map of custom settings for the organization
   */
  public record CreateOrganizationRequest(
      @NotBlank String name, @NotBlank String slug, Map<String, Object> settings) {}

  /**
   * Represents the data required to update an organization.
   *
   * @param name the new name for the organization
   * @param settings a map of new settings for the organization
   */
  public record UpdateOrganizationRequest(
      @NotBlank String name, Map<String, Object> settings) {}

  /**
   * Represents the data required to update an organization's settings.
   *
   * @param settings a map of new settings for the organization
   */
  public record UpdateSettingsRequest(@NotNull Map<String, Object> settings) {}

  /**
   * Represents the data required to invite a user to an organization.
   *
   * @param email the email address of the user to invite
   * @param role the role to assign to the user
   */
  public record InviteUserRequest(@Email @NotBlank String email, @NotNull Role role) {}

  /**
   * Represents the data required to update a member's role.
   *
   * @param role the new role for the member
   */
  public record UpdateMemberRoleRequest(@NotNull Role role) {}

  /**
   * Represents the data required to create a new user.
   *
   * @param email the email address of the new user
   * @param name the name of the new user
   * @param role the role of the new user
   */
  public record CreateUserRequest(
      @Email @NotBlank String email, @NotBlank String name, String role) {}

  /**
   * Represents the data required to change a user's password.
   *
   * @param currentPassword the user's current password
   * @param newPassword the new password to set
   */
  public record ChangePasswordRequest(
      @NotBlank String currentPassword, @NotBlank String newPassword) {}
}