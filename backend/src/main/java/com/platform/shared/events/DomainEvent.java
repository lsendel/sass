package com.platform.shared.events;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

/**
 * Base class for all domain events in the system.
 * Provides common properties for all events across modules.
 */
public abstract class DomainEvent extends ApplicationEvent {
    
    private final String eventId;
    private final String eventType;
    private final Instant occurredAt;
    private final String sourceModule;
    private final String correlationId;
    
    public DomainEvent(Object source, String eventType, String sourceModule) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.sourceModule = sourceModule;
        this.occurredAt = Instant.now();
        this.correlationId = UUID.randomUUID().toString(); // Could be passed from context
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public Instant getOccurredAt() {
        return occurredAt;
    }
    
    public String getSourceModule() {
        return sourceModule;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
}
