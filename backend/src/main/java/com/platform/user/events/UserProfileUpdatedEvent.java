package com.platform.user.events;

import com.platform.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user profile is updated.
 *
 * @param userId the user ID
 * @param email the user's email
 * @param occurredOn when the event occurred
 * @since 1.0.0
 */
public record UserProfileUpdatedEvent(
    UUID userId,
    String email,
    Instant occurredOn
) implements DomainEvent {
}
