package com.platform.shared.monitoring;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Collects security metrics for monitoring and alerting.
 * Integrates with Micrometer for Prometheus/Grafana dashboards.
 */
@Component
public class SecurityMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;

    // Counters for security events
    private final Counter authenticationFailuresCounter;
    private final Counter rateLimitExceededCounter;
    private final Counter xssAttemptsCounter;
    private final Counter sqlInjectionAttemptsCounter;
    private final Counter suspiciousActivityCounter;

    // Gauges for current state
    private final AtomicLong activeSessions = new AtomicLong(0);
    private final AtomicLong lockedAccounts = new AtomicLong(0);

    // Timers for performance monitoring
    private final Timer authenticationTimer;
    private final Timer sessionCreationTimer;

    @Autowired
    public SecurityMetricsCollector(MeterRegistry meterRegistry, RedisTemplate<String, Object> redisTemplate) {
        this.meterRegistry = meterRegistry;
        this.redisTemplate = redisTemplate;

        // Initialize counters
        this.authenticationFailuresCounter = Counter.builder("security.authentication.failures")
            .description("Total number of authentication failures")
            .tag("component", "security")
            .register(meterRegistry);

        this.rateLimitExceededCounter = Counter.builder("security.rate_limit.exceeded")
            .description("Total number of rate limit violations")
            .tag("component", "security")
            .register(meterRegistry);

        this.xssAttemptsCounter = Counter.builder("security.xss.attempts")
            .description("Total number of XSS attack attempts")
            .tag("component", "security")
            .register(meterRegistry);

        this.sqlInjectionAttemptsCounter = Counter.builder("security.sql_injection.attempts")
            .description("Total number of SQL injection attempts")
            .tag("component", "security")
            .register(meterRegistry);

        this.suspiciousActivityCounter = Counter.builder("security.suspicious_activity")
            .description("Total number of suspicious activities detected")
            .tag("component", "security")
            .register(meterRegistry);

        // Initialize gauges
        Gauge.builder("security.sessions.active")
            .description("Number of currently active sessions")
            .tag("component", "security")
            .register(meterRegistry, this, SecurityMetricsCollector::getActiveSessionsCount);

        Gauge.builder("security.accounts.locked")
            .description("Number of currently locked accounts")
            .tag("component", "security")
            .register(meterRegistry, this, SecurityMetricsCollector::getLockedAccountsCount);

        // Initialize timers
        this.authenticationTimer = Timer.builder("security.authentication.duration")
            .description("Time taken for authentication operations")
            .tag("component", "security")
            .register(meterRegistry);

        this.sessionCreationTimer = Timer.builder("security.session.creation.duration")
            .description("Time taken for session creation")
            .tag("component", "security")
            .register(meterRegistry);
    }

    // Security event recording methods
    public void recordAuthenticationFailure(String reason) {
        authenticationFailuresCounter.increment();
        meterRegistry.counter("security.authentication.failures", "reason", reason).increment();
    }

    public void recordRateLimitExceeded(String endpoint) {
        rateLimitExceededCounter.increment();
        meterRegistry.counter("security.rate_limit.exceeded", "endpoint", endpoint).increment();
    }

    public void recordXssAttempt(String endpoint) {
        xssAttemptsCounter.increment();
        meterRegistry.counter("security.xss.attempts", "endpoint", endpoint).increment();
    }

    public void recordSqlInjectionAttempt(String context) {
        sqlInjectionAttemptsCounter.increment();
        meterRegistry.counter("security.sql_injection.attempts", "context", context).increment();
    }

    public void recordSuspiciousActivity(String activityType) {
        suspiciousActivityCounter.increment();
        meterRegistry.counter("security.suspicious_activity", "type", activityType).increment();
    }

    // Session management metrics
    public void recordSessionCreated() {
        activeSessions.incrementAndGet();
    }

    public void recordSessionDestroyed() {
        activeSessions.decrementAndGet();
    }

    public void recordAccountLocked() {
        lockedAccounts.incrementAndGet();
    }

    public void recordAccountUnlocked() {
        lockedAccounts.decrementAndGet();
    }

    // Timer utilities
    public Timer.Sample startAuthenticationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordAuthenticationTime(Timer.Sample sample) {
        sample.stop(authenticationTimer);
    }

    public Timer.Sample startSessionCreationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordSessionCreationTime(Timer.Sample sample) {
        sample.stop(sessionCreationTimer);
    }

    // Gauge value providers
    private double getActiveSessionsCount() {
        try {
            // Get count from Redis (more accurate in distributed environment)
            return redisTemplate.keys("spring:session:sessions:*").size();
        } catch (Exception e) {
            // Fallback to local counter
            return activeSessions.get();
        }
    }

    private double getLockedAccountsCount() {
        try {
            // Get count from Redis
            return redisTemplate.keys("account:locked:*").size();
        } catch (Exception e) {
            // Fallback to local counter
            return lockedAccounts.get();
        }
    }

    // Custom metrics for security dashboard
    public void recordSecurityMetric(String metricName, double value, String... tags) {
        Gauge.builder("security.custom." + metricName)
            .tags(tags)
            .register(meterRegistry, () -> value);
    }

    public void incrementSecurityCounter(String counterName, String... tags) {
        meterRegistry.counter("security.custom." + counterName, tags).increment();
    }

    // Utility methods for dashboard calculations
    public long getFailedLoginAttemptsLast24Hours() {
        try {
            String key = "security:failed_logins:" + Instant.now().truncatedTo(ChronoUnit.DAYS);
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public long getRateLimitViolationsLastHour() {
        try {
            String key = "security:rate_limits:" + Instant.now().truncatedTo(ChronoUnit.HOURS);
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public double getAverageSessionDuration() {
        return sessionCreationTimer.mean(java.util.concurrent.TimeUnit.MINUTES);
    }
}