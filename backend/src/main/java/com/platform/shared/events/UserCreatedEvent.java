package com.platform.shared.events;

/**
 * Event published when a user is created in the system.
 */
public class UserCreatedEvent extends DomainEvent {
    
    private final String userId;
    private final String email;
    private final String name;
    
    public UserCreatedEvent(Object source, String userId, String email, String name) {
        super(source, "USER_CREATED", "USER");
        this.userId = userId;
        this.email = email;
        this.name = name;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getName() {
        return name;
    }
}