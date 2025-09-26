package com.platform.auth.internal;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.platform.shared.audit.AuditService;
import com.platform.shared.security.SecurityProperties;

/**
 * Service for managing account lockouts with exponential backoff.
 * Implements Redis-based storage for lockout tracking with automatic expiry.
 */
@Service
public class AccountLockoutService {

    private static final String LOCKOUT_KEY_PREFIX = "auth:lockout:";
    private static final String ATTEMPTS_KEY_PREFIX = "auth:attempts:";
    private static final int BASE_LOCKOUT_MINUTES = 5;
    private static final int MAX_LOCKOUT_HOURS = 24;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;
    private final AuditService auditService;

    public AccountLockoutService(
            RedisTemplate<String, Object> redisTemplate,
            SecurityProperties securityProperties,
            AuditService auditService) {
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
        this.auditService = auditService;
    }

    /**
     * Records a failed login attempt for the given email.
     * Implements automatic lockout with exponential backoff.
     *
     * @param email the email address
     * @return true if account is now locked, false otherwise
     */
    public boolean recordFailedAttempt(String email) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + email.toLowerCase();
        String lockoutKey = LOCKOUT_KEY_PREFIX + email.toLowerCase();

        // Check if already locked
        if (isAccountLocked(email)) {
            auditService.logSecurityEvent(
                "FAILED_LOGIN_ATTEMPT_WHILE_LOCKED",
                email,
                "Additional login attempt while account is locked"
            );
            return true;
        }

        // Get current attempt count
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);
        int currentAttempts = (attempts != null) ? attempts + 1 : 1;

        // Update attempt count with TTL
        Duration attemptsTtl = Duration.ofHours(1); // Reset attempts after 1 hour
        redisTemplate.opsForValue().set(attemptsKey, currentAttempts, attemptsTtl);

        auditService.logSecurityEvent(
            "FAILED_LOGIN_ATTEMPT",
            email,
            String.format("Failed login attempt %d of %d", currentAttempts,
                         securityProperties.getMaxLoginAttempts())
        );

        // Check if lockout threshold reached
        if (currentAttempts >= securityProperties.getMaxLoginAttempts()) {
            lockAccount(email, currentAttempts);
            return true;
        }

        return false;
    }

    /**
     * Checks if an account is currently locked.
     *
     * @param email the email address
     * @return true if locked, false otherwise
     */
    public boolean isAccountLocked(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email.toLowerCase();
        AccountLockout lockout = (AccountLockout) redisTemplate.opsForValue().get(lockoutKey);

        if (lockout == null) {
            return false;
        }

        // Check if lockout has expired
        if (LocalDateTime.now().isAfter(lockout.expiresAt())) {
            unlockAccount(email);
            return false;
        }

        return true;
    }

    /**
     * Gets the lockout details for an account.
     *
     * @param email the email address
     * @return lockout details if locked, empty otherwise
     */
    public Optional<AccountLockout> getLockoutDetails(String email) {
        if (!isAccountLocked(email)) {
            return Optional.empty();
        }

        String lockoutKey = LOCKOUT_KEY_PREFIX + email.toLowerCase();
        AccountLockout lockout = (AccountLockout) redisTemplate.opsForValue().get(lockoutKey);
        return Optional.ofNullable(lockout);
    }

    /**
     * Clears failed attempts for successful login.
     *
     * @param email the email address
     */
    public void clearFailedAttempts(String email) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + email.toLowerCase();
        redisTemplate.delete(attemptsKey);

        auditService.logSecurityEvent(
            "SUCCESSFUL_LOGIN",
            email,
            "Failed attempts cleared after successful login"
        );
    }

    /**
     * Manually unlocks an account (admin function).
     *
     * @param email the email address
     */
    public void unlockAccount(String email) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email.toLowerCase();
        String attemptsKey = ATTEMPTS_KEY_PREFIX + email.toLowerCase();

        redisTemplate.delete(lockoutKey);
        redisTemplate.delete(attemptsKey);

        auditService.logSecurityEvent(
            "ACCOUNT_UNLOCKED",
            email,
            "Account manually unlocked or lockout expired"
        );
    }

    /**
     * Gets current failed attempt count.
     *
     * @param email the email address
     * @return number of failed attempts
     */
    public int getFailedAttemptCount(String email) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + email.toLowerCase();
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);
        return attempts != null ? attempts : 0;
    }

    /**
     * Locks an account with exponential backoff.
     */
    private void lockAccount(String email, int attemptCount) {
        String lockoutKey = LOCKOUT_KEY_PREFIX + email.toLowerCase();

        // Calculate lockout duration with exponential backoff
        int lockoutAttempts = attemptCount - securityProperties.getMaxLoginAttempts() + 1;
        Duration lockoutDuration = calculateLockoutDuration(lockoutAttempts);

        LocalDateTime expiresAt = LocalDateTime.now().plus(lockoutDuration);
        AccountLockout lockout = new AccountLockout(
            email,
            LocalDateTime.now(),
            expiresAt,
            lockoutAttempts,
            lockoutDuration
        );

        // Store lockout with TTL
        redisTemplate.opsForValue().set(lockoutKey, lockout, lockoutDuration);

        auditService.logHighPrioritySecurityEvent(
            "ACCOUNT_LOCKED",
            email,
            String.format("Account locked for %s after %d failed attempts (lockout #%d)",
                         formatDuration(lockoutDuration), attemptCount, lockoutAttempts)
        );
    }

    /**
     * Calculates lockout duration with exponential backoff.
     * Formula: BASE_LOCKOUT_MINUTES * 2^(attempt-1), capped at MAX_LOCKOUT_HOURS
     */
    private Duration calculateLockoutDuration(int lockoutAttempt) {
        long minutes = BASE_LOCKOUT_MINUTES * (long) Math.pow(2, lockoutAttempt - 1);
        long maxMinutes = MAX_LOCKOUT_HOURS * 60;

        minutes = Math.min(minutes, maxMinutes);
        return Duration.ofMinutes(minutes);
    }

    /**
     * Formats duration for human-readable logging.
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("%d hours %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }

    /**
     * Account lockout record.
     */
    public static record AccountLockout(
        String email,
        LocalDateTime lockedAt,
        LocalDateTime expiresAt,
        int lockoutNumber,
        Duration duration
    ) {}
}