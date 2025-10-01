package com.platform.auth.events;

import com.platform.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user successfully authenticates.
 * Other modules can listen to this event for audit logging, analytics, etc.
 *
 * @param userId the ID of the authenticated user
 * @param email the email of the authenticated user
 * @param occurredOn the timestamp when authentication occurred
 * @since 1.0.0
 */
public record UserAuthenticatedEvent(
    UUID userId,
    String email,
    Instant occurredOn
) implements DomainEvent {
}
