package com.platform.user.internal;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing demo users and organizations for testing purposes.
 * This service is in the internal package and delegates to internal services.
 */
@Service
@Transactional
public class DemoUserManagementService {

  private final OrganizationService organizationService;
  private final UserService userService;

  public DemoUserManagementService(
      OrganizationService organizationService, UserService userService) {
    this.organizationService = organizationService;
    this.userService = userService;
  }

  /**
   * Finds a user by email if they are not deleted.
   *
   * @param email the email to search for
   * @return Optional containing the user if found
   */
  public Optional<UserInfo> findUserByEmail(String email) {
    return userService
        .findByEmail(email)
        .map(this::toUserInfo);
  }

  /**
   * Creates a demo organization for testing purposes.
   *
   * @param name the organization name
   * @param slug the organization slug
   * @param description the organization description
   * @return the created organization info
   */
  public OrganizationInfo createDemoOrganization(String name, String slug, String description) {
    // Create settings map with description
    Map<String, Object> settings = Map.of("description", description);
    var saved = organizationService.createOrganization(name, slug, settings);
    return toOrganizationInfo(saved);
  }

  /**
   * Creates a demo user for testing purposes.
   *
   * @param email the user email
   * @param name the user name
   * @param organizationId the organization ID
   * @param passwordHash the encoded password hash
   * @param authMethods the authentication methods
   * @return the created user info
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

  private UserInfo toUserInfo(com.platform.user.internal.User user) {
    return new UserInfo(
        user.getId(),
        user.getEmail().getValue(),
        user.getName(),
        user.getOrganization() != null ? user.getOrganization().getId() : null);
  }

  private OrganizationInfo toOrganizationInfo(com.platform.user.internal.Organization org) {
    return new OrganizationInfo(org.getId(), org.getName(), org.getSlug());
  }

  /**
   * DTO for user information to avoid exposing internal entities.
   */
  public record UserInfo(
      java.util.UUID id, String email, String name, java.util.UUID organizationId) {}

  /**
   * DTO for organization information to avoid exposing internal entities.
   */
  public record OrganizationInfo(java.util.UUID id, String name, String slug) {}
}