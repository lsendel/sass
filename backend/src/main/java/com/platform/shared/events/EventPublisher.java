package com.platform.shared.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Event publisher service that provides a centralized way to publish domain events.
 */
@Component
public class EventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public EventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Publishes a domain event to the Spring application event system.
     */
    public void publishEvent(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }
    
    /**
     * Publishes a generic event.
     */
    public void publishEvent(Object event) {
        eventPublisher.publishEvent(event);
    }
}