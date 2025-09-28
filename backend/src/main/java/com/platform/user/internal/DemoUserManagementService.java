package com.platform.user.internal;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing demo users and organizations for testing and demonstration purposes.
 *
 * <p>This service provides a simplified interface for creating and retrieving user and organization
 * data, intended for use in non-production environments. It delegates to the internal {@link
 * UserService} and {@link OrganizationService} to perform its operations.
 *
 * @see UserService
 * @see OrganizationService
 */
@Service
@Transactional
public class DemoUserManagementService {

  private final OrganizationService organizationService;
  private final UserService userService;

  /**
   * Constructs a new DemoUserManagementService.
   *
   * @param organizationService the service for managing organizations
   * @param userService the service for managing users
   */
  public DemoUserManagementService(
      OrganizationService organizationService, UserService userService) {
    this.organizationService = organizationService;
    this.userService = userService;
  }

  /**
   * Finds a user by their email address.
   *
   * <p>This method searches for a user with the specified email and returns their information as a
   * {@link UserInfo} DTO if found.
   *
   * @param email the email address to search for
   * @return an {@link Optional} containing the {@link UserInfo} if a user is found, otherwise an
   *     empty {@link Optional}
   */
  public Optional<UserInfo> findUserByEmail(String email) {
    return userService
        .findByEmail(email)
        .map(this::toUserInfo);
  }

  /**
   * Creates a new demo organization.
   *
   * @param name the name of the organization
   * @param slug a URL-friendly slug for the organization
   * @param description a description for the organization
   * @return an {@link OrganizationInfo} DTO representing the newly created organization
   */
  public OrganizationInfo createDemoOrganization(String name, String slug, String description) {
    // Create settings map with description
    Map<String, Object> settings = Map.of("description", description);
    var saved = organizationService.createOrganization(name, slug, settings);
    return toOrganizationInfo(saved);
  }

  /**
   * Creates a new demo user.
   *
   * <p><b>Note:</b> This method is currently not implemented and will throw an {@link
   * UnsupportedOperationException}.
   *
   * @param email the email address of the user
   * @param name the name of the user
   * @param organizationId the ID of the organization the user belongs to
   * @param passwordHash the encoded password hash for the user
   * @param authMethods the authentication methods for the user
   * @return a {@link UserInfo} DTO representing the newly created user
   * @throws UnsupportedOperationException as this method is not yet implemented
   */
  public UserInfo createDemoUser(
      String email,
      String name,
      java.util.UUID organizationId,
      String passwordHash,
      Set<com.platform.user.internal.User.AuthenticationMethod> authMethods) {

    // For now, create a simplified user through the user service
    // This should be refactored to use proper service methods
    throw new UnsupportedOperationException("Demo user creation needs to be implemented");
  }

  /**
   * Converts a {@link User} entity to a {@link UserInfo} DTO.
   *
   * @param user the user entity to convert
   * @return the corresponding user info DTO
   */
  private UserInfo toUserInfo(com.platform.user.internal.User user) {
    return new UserInfo(
        user.getId(),
        user.getEmail().getValue(),
        user.getName(),
        user.getOrganization() != null ? user.getOrganization().getId() : null);
  }

  /**
   * Converts an {@link Organization} entity to an {@link OrganizationInfo} DTO.
   *
   * @param org the organization entity to convert
   * @return the corresponding organization info DTO
   */
  private OrganizationInfo toOrganizationInfo(com.platform.user.internal.Organization org) {
    return new OrganizationInfo(org.getId(), org.getName(), org.getSlug());
  }

  /**
   * DTO for user information, used to avoid exposing internal entities.
   *
   * @param id the unique identifier of the user
   * @param email the email address of the user
   * @param name the name of the user
   * @param organizationId the ID of the organization the user belongs to
   */
  public record UserInfo(
      java.util.UUID id, String email, String name, java.util.UUID organizationId) {}

  /**
   * DTO for organization information, used to avoid exposing internal entities.
   *
   * @param id the unique identifier of the organization
   * @param name the name of the organization
   * @param slug a URL-friendly slug for the organization
   */
  public record OrganizationInfo(java.util.UUID id, String name, String slug) {}
}