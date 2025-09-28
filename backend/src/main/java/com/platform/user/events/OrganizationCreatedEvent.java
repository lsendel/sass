package com.platform.user.events;

import java.time.Instant;
import java.util.UUID;

/**
 * A domain event published when a new organization is created.
 *
 * <p>This event can be consumed by other services to perform additional setup or processing, such as
 * creating default projects, sending welcome notifications, or logging audit trails.
 *
 * @param organizationId the unique identifier of the newly created organization
 * @param name the name of the organization
 * @param slug a unique, URL-friendly slug for the organization
 * @param creatorId the ID of the user who created the organization
 * @param createdAt the timestamp when the event was created
 */
public record OrganizationCreatedEvent(
    UUID organizationId, String name, String slug, UUID creatorId, Instant createdAt) {

  /**
   * Constructs a new OrganizationCreatedEvent with the creation timestamp set to the current time.
   *
   * @param organizationId the unique identifier of the organization
   * @param name the name of the organization
   * @param slug a unique, URL-friendly slug for the organization
   * @param creatorId the ID of the user who created the organization
   */
  public OrganizationCreatedEvent(UUID organizationId, String name, String slug, UUID creatorId) {
    this(organizationId, name, slug, creatorId, Instant.now());
  }
}
