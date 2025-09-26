package com.platform.shared.events;

/**
 * Analytics event for tracking frontend performance and user interactions.
 */
public record AnalyticsEvent(
    String type,
    Object data,
    long timestamp,
    String sessionId,
    String userId
) {}