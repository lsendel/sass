package com.platform.user.internal;

import java.util.UUID;

/**
 * A read-only projection of a {@link User} entity, designed for use in the API layer.
 *
 * <p>This record provides a safe way to expose user data without revealing the full internal
 * entity. It includes essential information for displaying user details, along with basic
 * information about their organization.
 *
 * @param id the unique identifier of the user
 * @param email the email address of the user
 * @param name the full name of the user
 * @param organizationId the ID of the organization the user belongs to
 * @param organizationName the name of the organization the user belongs to
 */
public record UserView(
    UUID id,
    String email,
    String name,
    UUID organizationId,
    String organizationName) {

  /**
   * Creates a {@link UserView} from a {@link User} entity.
   *
   * @param user the user entity to convert
   * @return a new {@link UserView} instance
   */
  public static UserView fromEntity(User user) {
    UUID orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
    String orgName = user.getOrganization() != null ? user.getOrganization().getName() : null;
    return new UserView(user.getId(), user.getEmail().getValue(), user.getName(), orgId, orgName);
  }
}

