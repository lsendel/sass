package com.platform.user.events;

import com.platform.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new organization is created.
 *
 * @param organizationId the organization ID
 * @param name the organization name
 * @param slug the organization slug
 * @param occurredOn when the event occurred
 * @since 1.0.0
 */
public record OrganizationCreatedEvent(
    UUID organizationId,
    String name,
    String slug,
    Instant occurredOn
) implements DomainEvent {
}
