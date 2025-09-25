package com.platform.shared.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Security metrics for monitoring and alerting.
 */
@Component
public class SecurityMetrics {

    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Counter accessDeniedCounter;
    private final Counter rateLimitCounter;
    private final Counter suspiciousActivityCounter;

    public SecurityMetrics(MeterRegistry meterRegistry) {
        this.authSuccessCounter = Counter.builder("security.auth.success")
                .description("Successful authentication attempts")
                .register(meterRegistry);
                
        this.authFailureCounter = Counter.builder("security.auth.failure")
                .description("Failed authentication attempts")
                .register(meterRegistry);
                
        this.accessDeniedCounter = Counter.builder("security.access.denied")
                .description("Access denied events")
                .register(meterRegistry);
                
        this.rateLimitCounter = Counter.builder("security.rate.limit")
                .description("Rate limit violations")
                .register(meterRegistry);
                
        this.suspiciousActivityCounter = Counter.builder("security.suspicious.activity")
                .description("Suspicious activity detected")
                .register(meterRegistry);
    }

    public void recordAuthSuccess() {
        authSuccessCounter.increment();
    }

    public void recordAuthFailure() {
        authFailureCounter.increment();
    }

    public void recordAccessDenied() {
        accessDeniedCounter.increment();
    }

    public void recordRateLimit() {
        rateLimitCounter.increment();
    }

    public void recordSuspiciousActivity() {
        suspiciousActivityCounter.increment();
    }
}
