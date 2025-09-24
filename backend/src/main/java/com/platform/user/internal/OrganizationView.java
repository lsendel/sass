package com.platform.user.internal;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only projection of Organization to avoid exposing JPA entity to API layer.
 */
public record OrganizationView(
    UUID id,
    String name,
    String slug,
    UUID ownerId,
    Map<String, Object> settings,
    Instant createdAt,
    Instant updatedAt) {

  public static OrganizationView fromEntity(Organization org) {
    return new OrganizationView(
        org.getId(),
        org.getName(),
        org.getSlug(),
        org.getOwnerId(),
        org.getSettings(),
        org.getCreatedAt(),
        org.getUpdatedAt());
  }
}

