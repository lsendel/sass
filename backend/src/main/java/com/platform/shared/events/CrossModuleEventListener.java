package com.platform.shared.events;

import org.slf4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.platform.shared.logging.LoggingUtil;

/**
 * Event listener that handles cross-module communication.
 * Demonstrates how different modules can react to events from other modules.
 */
@Component
public class CrossModuleEventListener {
    
    private static final Logger logger = LoggingUtil.getLogger(CrossModuleEventListener.class);
    
    @EventListener
    @Async
    public void handleUserCreated(UserCreatedEvent event) {
        logger.info("Processing UserCreatedEvent: {} for user {} ({})", 
            event.getEventId(), event.getUserId(), event.getEmail());
        
        // This could trigger actions in other modules
        // For example: send welcome email, create default organization, etc.
        
        logger.debug("User created event processed for: {}", event.getEmail());
    }
    
    @EventListener
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        logger.info("Processing PaymentSuccessEvent: {} for payment {} in org {}", 
            event.getEventId(), event.getPaymentId(), event.getOrganizationId());
        
        // This could trigger actions like:
        // - Update subscription status
        // - Send receipt email
        // - Update metrics
        // - Trigger fulfillment
        
        logger.debug("Payment success event processed for: {} {}", 
            event.getAmount(), event.getCurrency());
    }
}