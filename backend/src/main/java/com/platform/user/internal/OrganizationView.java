package com.platform.user.internal;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A read-only projection of an {@link Organization} entity, designed for use in the API layer.
 *
 * <p>This record provides a safe way to expose organization data without revealing the full
 * internal entity. It includes essential information for displaying organization details to a user.
 *
 * @param id the unique identifier of the organization
 * @param name the name of the organization
 * @param slug a unique, URL-friendly identifier for the organization
 * @param ownerId the ID of the user who owns the organization
 * @param settings a map of custom settings for the organization
 * @param createdAt the timestamp when the organization was created
 * @param updatedAt the timestamp when the organization was last updated
 */
public record OrganizationView(
    UUID id,
    String name,
    String slug,
    UUID ownerId,
    Map<String, Object> settings,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Creates an {@link OrganizationView} from an {@link Organization} entity.
   *
   * @param org the organization entity to convert
   * @return a new {@link OrganizationView} instance
   */
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

