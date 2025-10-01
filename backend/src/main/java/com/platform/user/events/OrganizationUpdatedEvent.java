package com.platform.user.events;

import com.platform.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when an organization is updated.
 *
 * @param organizationId the organization ID
 * @param name the updated organization name
 * @param occurredOn the timestamp when the event occurred
 * @since 1.0.0
 */
public record OrganizationUpdatedEvent(
    UUID organizationId,
    String name,
    Instant occurredOn
) implements DomainEvent {

    public OrganizationUpdatedEvent(final UUID organizationId,
                                     final String name) {
        this(organizationId, name, Instant.now());
    }
}
