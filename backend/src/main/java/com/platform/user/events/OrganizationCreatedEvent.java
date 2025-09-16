package com.platform.user.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an organization is created.
 * This event can be consumed by other modules (like audit) to perform
 * additional processing when organizations are created.
 */
public record OrganizationCreatedEvent(
    UUID organizationId,
    String name,
    String slug,
    UUID creatorId,
    Instant createdAt
) {
    public OrganizationCreatedEvent(UUID organizationId, String name, String slug, UUID creatorId) {
        this(organizationId, name, slug, creatorId, Instant.now());
    }
}