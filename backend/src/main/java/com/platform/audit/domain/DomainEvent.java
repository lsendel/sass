package com.platform.audit.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the audit system.
 * Implements the Domain Event pattern for decoupled communication.
 */
public abstract class DomainEvent {
    private final UUID eventId;
    private final Instant occurredOn;
    private final UUID aggregateId;
    private final Long version;

    /**
     * Constructor for domain events.
     *
     * @param aggregateId the aggregate ID
     * @param version the version number
     */
    protected DomainEvent(final UUID aggregateId, final Long version) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.aggregateId = aggregateId;
        this.version = version;
    }

    /**
     * Gets the event ID.
     *
     * @return the event ID
     */
    public final UUID getEventId() {
        return eventId;
    }

    /**
     * Gets the occurrence timestamp.
     *
     * @return when the event occurred
     */
    public final Instant getOccurredOn() {
        return occurredOn;
    }

    /**
     * Gets the aggregate ID.
     *
     * @return the aggregate ID
     */
    public final UUID getAggregateId() {
        return aggregateId;
    }

    /**
     * Gets the version.
     *
     * @return the version number
     */
    public final Long getVersion() {
        return version;
    }
}