package com.platform.shared.events;

import java.time.Instant;

/**
 * Base interface for all domain events in the system.
 * Events are used for inter-module communication following the event-driven architecture pattern.
 *
 * <p>All domain events should:
 * <ul>
 *   <li>Be immutable</li>
 *   <li>Include a timestamp</li>
 *   <li>Be published via ApplicationEventPublisher</li>
 *   <li>Be handled asynchronously</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface DomainEvent {

    /**
     * Gets the timestamp when this event occurred.
     *
     * @return the event timestamp
     */
    Instant occurredOn();

    /**
     * Gets the type of the event for logging and routing purposes.
     *
     * @return the event type name
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }
}
