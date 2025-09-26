package com.platform.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Security configuration properties for the platform.
 * Contains rate limiting, session, and general security settings.
 */
@Component
@ConfigurationProperties(prefix = "platform.security")
public class SecurityProperties {

    /**
     * Rate limiting configuration
     */
    private RateLimiting rateLimiting = new RateLimiting();

    /**
     * Session security configuration
     */
    private Session session = new Session();

    /**
     * Account lockout configuration
     */
    private AccountLockout accountLockout = new AccountLockout();

    public RateLimiting getRateLimiting() {
        return rateLimiting;
    }

    public void setRateLimiting(RateLimiting rateLimiting) {
        this.rateLimiting = rateLimiting;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public AccountLockout getAccountLockout() {
        return accountLockout;
    }

    public void setAccountLockout(AccountLockout accountLockout) {
        this.accountLockout = accountLockout;
    }

    public static class RateLimiting {
        /**
         * Default requests per minute for authentication endpoints
         */
        private int defaultRequestsPerMinute = 10;

        /**
         * Strict requests per minute for sensitive endpoints
         */
        private int strictRequestsPerMinute = 5;

        /**
         * Rate limit window duration in minutes
         */
        private int windowMinutes = 1;

        public int getDefaultRequestsPerMinute() {
            return defaultRequestsPerMinute;
        }

        public void setDefaultRequestsPerMinute(int defaultRequestsPerMinute) {
            this.defaultRequestsPerMinute = defaultRequestsPerMinute;
        }

        public int getStrictRequestsPerMinute() {
            return strictRequestsPerMinute;
        }

        public void setStrictRequestsPerMinute(int strictRequestsPerMinute) {
            this.strictRequestsPerMinute = strictRequestsPerMinute;
        }

        public int getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(int windowMinutes) {
            this.windowMinutes = windowMinutes;
        }
    }

    public static class Session {
        /**
         * Session timeout in minutes
         */
        private int timeoutMinutes = 30;

        /**
         * Maximum concurrent sessions per user
         */
        private int maxConcurrentSessions = 3;

        /**
         * Enable session fixation protection
         */
        private boolean sessionFixationProtection = true;

        public int getTimeoutMinutes() {
            return timeoutMinutes;
        }

        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }

        public int getMaxConcurrentSessions() {
            return maxConcurrentSessions;
        }

        public void setMaxConcurrentSessions(int maxConcurrentSessions) {
            this.maxConcurrentSessions = maxConcurrentSessions;
        }

        public boolean isSessionFixationProtection() {
            return sessionFixationProtection;
        }

        public void setSessionFixationProtection(boolean sessionFixationProtection) {
            this.sessionFixationProtection = sessionFixationProtection;
        }
    }

    public static class AccountLockout {
        /**
         * Maximum failed login attempts before lockout
         */
        private int maxFailedAttempts = 5;

        /**
         * Maximum login attempts (alias for backward compatibility)
         */
        private int maxLoginAttempts = 5;

        /**
         * Initial lockout duration in minutes
         */
        private int initialLockoutMinutes = 15;

        /**
         * Maximum lockout duration in minutes
         */
        private int maxLockoutMinutes = 60;

        /**
         * Lockout multiplier for repeated failures
         */
        private double lockoutMultiplier = 2.0;

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public int getInitialLockoutMinutes() {
            return initialLockoutMinutes;
        }

        public void setInitialLockoutMinutes(int initialLockoutMinutes) {
            this.initialLockoutMinutes = initialLockoutMinutes;
        }

        public int getMaxLockoutMinutes() {
            return maxLockoutMinutes;
        }

        public void setMaxLockoutMinutes(int maxLockoutMinutes) {
            this.maxLockoutMinutes = maxLockoutMinutes;
        }

        public double getLockoutMultiplier() {
            return lockoutMultiplier;
        }

        public void setLockoutMultiplier(double lockoutMultiplier) {
            this.lockoutMultiplier = lockoutMultiplier;
        }

        public int getMaxLoginAttempts() {
            return maxLoginAttempts;
        }

        public void setMaxLoginAttempts(int maxLoginAttempts) {
            this.maxLoginAttempts = maxLoginAttempts;
        }
    }

    /**
     * Convenience method to get max login attempts from account lockout settings
     */
    public int getMaxLoginAttempts() {
        return accountLockout.getMaxLoginAttempts();
    }
}