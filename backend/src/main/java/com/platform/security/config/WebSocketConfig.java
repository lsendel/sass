package com.platform.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket configuration for real-time security event streaming.
 *
 * Enables STOMP messaging over WebSocket for:
 * - Real-time security event notifications
 * - Live dashboard updates
 * - Alert notifications
 * - Metrics streaming
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker with destinations for security events
        config.enableSimpleBroker(
            "/topic/security-events",     // Security events stream
            "/topic/dashboard-updates",   // Dashboard data updates
            "/topic/alerts",              // Alert notifications
            "/topic/metrics",             // Real-time metrics
            "/topic/threat-intel"         // Threat intelligence updates
        );

        // Set application destination prefix for client messages
        config.setApplicationDestinationPrefixes("/app");

        // Configure user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint for security dashboard WebSocket connections
        registry.addEndpoint("/ws/security-dashboard")
                .setAllowedOriginPatterns("*")  // Configure properly for production
                .withSockJS()  // Enable SockJS fallback for better compatibility
                .setHeartbeatTime(25000)  // 25 seconds heartbeat
                .setDisconnectDelay(5000);  // 5 seconds disconnect delay

        // Register endpoint for metrics streaming
        registry.addEndpoint("/ws/security-metrics")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(10000)  // More frequent heartbeat for metrics
                .setDisconnectDelay(3000);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure message size limits and timeouts
        registration
                .setMessageSizeLimit(64 * 1024)        // 64KB max message size
                .setSendBufferSizeLimit(512 * 1024)    // 512KB send buffer
                .setSendTimeLimit(20000)               // 20 second send timeout
                .setTimeToFirstMessage(60000);         // 1 minute to first message
    }

    /**
     * Bean for WebSocket session registry to track active connections
     * Used for broadcasting to specific user sessions
     */
    // Note: This will be implemented later with proper session management
    // @Bean
    // public SessionRegistry sessionRegistry() {
    //     return new SessionRegistryImpl();
    // }
}