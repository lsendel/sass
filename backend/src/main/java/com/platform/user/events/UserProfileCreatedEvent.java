package com.platform.user.events;

import com.platform.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new user profile is created.
 *
 * @param userId the user ID
 * @param email the user's email
 * @param organizationId the organization ID
 * @param occurredOn when the event occurred
 * @since 1.0.0
 */
public record UserProfileCreatedEvent(
    UUID userId,
    String email,
    UUID organizationId,
    Instant occurredOn
) implements DomainEvent {
}
