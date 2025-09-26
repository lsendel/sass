package com.platform.user.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer objects for user-related operations.
 * These DTOs prevent direct access to internal entities from controllers.
 */
public class UserDto {

  /**
   * Role enumeration for API layer.
   */
  public enum Role {
    OWNER, ADMIN, MEMBER, VIEWER
  }

  /**
   * Invitation status enumeration for API layer.
   */
  public enum InvitationStatus {
    PENDING, ACCEPTED, DECLINED, EXPIRED, REVOKED
  }

  /**
   * Response DTO for user information.
   */
  public record UserResponse(
      UUID id,
      String email,
      String name,
      String provider,
      Map<String, Object> preferences,
      Instant createdAt,
      Instant lastActiveAt) {
  }

  /**
   * Request DTO for updating user profile.
   */
  public record UpdateProfileRequest(
      @NotBlank String name,
      Map<String, Object> preferences) {
  }

  /**
   * Request DTO for updating user preferences.
   */
  public record UpdatePreferencesRequest(
      @NotNull Map<String, Object> preferences) {
  }

  /**
   * Response DTO for paginated user results.
   */
  public record PagedUserResponse(
      List<UserResponse> users,
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean first,
      boolean last) {
  }

  /**
   * Response DTO for user statistics.
   */
  public record UserStatistics(
      long totalUsers,
      long activeUsers,
      long newUsersThisMonth,
      Map<String, Long> usersByProvider,
      double averageSessionDuration) {
  }

  /**
   * Response DTO for organization information.
   */
  public record OrganizationResponse(
      UUID id,
      String name,
      String slug,
      UUID ownerId,
      Map<String, Object> settings,
      Instant createdAt,
      Instant updatedAt) {
  }

  /**
   * Response DTO for organization member information.
   */
  public record OrganizationMemberInfoResponse(
      UUID userId,
      String userEmail,
      String userName,
      Role role,
      Instant joinedAt) {
  }

  /**
   * Response DTO for organization member.
   */
  public record OrganizationMemberResponse(
      UUID id,
      UUID userId,
      UUID organizationId,
      Role role,
      Instant joinedAt) {
  }

  /**
   * Response DTO for invitation information.
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
      Instant createdAt) {
  }

  /**
   * Request DTO for creating an organization.
   */
  public record CreateOrganizationRequest(
      @NotBlank String name,
      @NotBlank String slug,
      Map<String, Object> settings) {
  }

  /**
   * Request DTO for updating an organization.
   */
  public record UpdateOrganizationRequest(
      @NotBlank String name,
      Map<String, Object> settings) {
  }

  /**
   * Request DTO for updating organization settings.
   */
  public record UpdateSettingsRequest(
      @NotNull Map<String, Object> settings) {
  }

  /**
   * Request DTO for inviting a user.
   */
  public record InviteUserRequest(
      @Email @NotBlank String email,
      @NotNull Role role) {
  }

  /**
   * Request DTO for updating member role.
   */
  public record UpdateMemberRoleRequest(
      @NotNull Role role) {
  }

  /**
   * Request DTO for creating a user.
   */
  public record CreateUserRequest(
      @Email @NotBlank String email,
      @NotBlank String name,
      String role) {
  }

  /**
   * Request DTO for changing password.
   */
  public record ChangePasswordRequest(
      @NotBlank String currentPassword,
      @NotBlank String newPassword) {
  }
}