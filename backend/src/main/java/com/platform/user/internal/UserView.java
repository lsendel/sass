package com.platform.user.internal;

import java.util.UUID;

/** Read-only projection of User for API responses. */
public record UserView(
    UUID id,
    String email,
    String name,
    UUID organizationId,
    String organizationName) {

  public static UserView fromEntity(User user) {
    UUID orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
    String orgName = user.getOrganization() != null ? user.getOrganization().getName() : null;
    return new UserView(user.getId(), user.getEmail().getValue(), user.getName(), orgId, orgName);
  }
}

