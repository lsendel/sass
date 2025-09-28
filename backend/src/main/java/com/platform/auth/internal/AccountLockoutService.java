package com.platform.auth.internal;

import com.platform.shared.audit.AuditService;
import com.platform.shared.security.SecurityProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Manages account lockouts to prevent brute-force attacks.
 *
 * <p>This service tracks failed login attempts for user accounts and enforces a lockout policy with
 * exponential backoff. It uses Redis for distributed, stateful tracking of attempts and lockouts,
 * with automatic expiry of records.
 *
 * @see SecurityProperties
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

  /**
   * Constructs the service with its dependencies.
   *
   * @param redisTemplate The Redis template for data access.
   * @param securityProperties The application's security configuration properties.
   * @param auditService The service for logging security-related audit events.
   */
  public AccountLockoutService(
      RedisTemplate<String, Object> redisTemplate,
      SecurityProperties securityProperties,
      AuditService auditService) {
    this.redisTemplate = redisTemplate;
    this.securityProperties = securityProperties;
    this.auditService = auditService;
  }

  /**
   * Records a failed login attempt for a given email address.
   *
   * <p>If the number of failed attempts exceeds the configured threshold, this method will trigger a
   * lockout with an exponentially increasing duration.
   *
   * @param email The email address for which the failed attempt is being recorded.
   * @return {@code true} if the account is now locked as a result of this attempt, {@code false}
   *     otherwise.
   */
  public boolean recordFailedAttempt(String email) {
    String attemptsKey = ATTEMPTS_KEY_PREFIX + email.toLowerCase();
    if (isAccountLocked(email)) {
      auditService.logSecurityEvent(
          "FAILED_LOGIN_ATTEMPT_WHILE_LOCKED",
          email,
          "Additional login attempt while account is locked");
      return true;
    }
    Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);
    int currentAttempts = (attempts != null) ? attempts + 1 : 1;
    Duration attemptsTtl = Duration.ofHours(1);
    redisTemplate.opsForValue().set(attemptsKey, currentAttempts, attemptsTtl);
    auditService.logSecurityEvent(
        "FAILED_LOGIN_ATTEMPT",
        email,
        String.format(
            "Failed login attempt %d of %d",
            currentAttempts, securityProperties.getMaxLoginAttempts()));
    if (currentAttempts >= securityProperties.getMaxLoginAttempts()) {
      lockAccount(email, currentAttempts);
      return true;
    }
    return false;
  }

  /**
   * Checks if an account is currently locked.
   *
   * @param email The email address to check.
   * @return {@code true} if the account is locked, {@code false} otherwise.
   */
  public boolean isAccountLocked(String email) {
    String lockoutKey = LOCKOUT_KEY_PREFIX + email.toLowerCase();
    AccountLockout lockout = (AccountLockout) redisTemplate.opsForValue().get(lockoutKey);
    if (lockout == null) {
      return false;
    }
    if (LocalDateTime.now().isAfter(lockout.expiresAt())) {
      unlockAccount(email);
      return false;
    }
    return true;
  }

  /**
   * Retrieves the details of an active lockout for an account.
   *
   * @param email The email address to check.
   * @return An {@link Optional} containing the {@link AccountLockout} details if the account is
   *     locked, or an empty {@link Optional} otherwise.
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
   * Clears the failed login attempt counter for an account, typically after a successful login.
   *
   * @param email The email address for which to clear attempts.
   */
  public void clearFailedAttempts(String email) {
    String attemptsKey = ATTEMPTS_KEY_PREFIX + email.toLowerCase();
    redisTemplate.delete(attemptsKey);
    auditService.logSecurityEvent(
        "SUCCESSFUL_LOGIN", email, "Failed attempts cleared after successful login");
  }

  /**
   * Manually unlocks an account and clears its failed attempt history. This is typically an
   * administrative function.
   *
   * @param email The email address of the account to unlock.
   */
  public void unlockAccount(String email) {
    String lockoutKey = LOCKOUT_KEY_PREFIX + email.toLowerCase();
    String attemptsKey = ATTEMPTS_KEY_PREFIX + email.toLowerCase();
    redisTemplate.delete(lockoutKey);
    redisTemplate.delete(attemptsKey);
    auditService.logSecurityEvent(
        "ACCOUNT_UNLOCKED", email, "Account manually unlocked or lockout expired");
  }

  /**
   * Gets the current count of failed login attempts for an account.
   *
   * @param email The email address to check.
   * @return The number of failed attempts recorded within the current tracking window.
   */
  public int getFailedAttemptCount(String email) {
    String attemptsKey = ATTEMPTS_KEY_PREFIX + email.toLowerCase();
    Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);
    return attempts != null ? attempts : 0;
  }

  /**
   * Locks an account and records the lockout details in Redis.
   *
   * @param email The email of the account to lock.
   * @param attemptCount The number of failed attempts that triggered the lockout.
   */
  private void lockAccount(String email, int attemptCount) {
    String lockoutKey = LOCKOUT_KEY_PREFIX + email.toLowerCase();
    int lockoutAttempts = attemptCount - securityProperties.getMaxLoginAttempts() + 1;
    Duration lockoutDuration = calculateLockoutDuration(lockoutAttempts);
    LocalDateTime expiresAt = LocalDateTime.now().plus(lockoutDuration);
    AccountLockout lockout =
        new AccountLockout(email, LocalDateTime.now(), expiresAt, lockoutAttempts, lockoutDuration);
    redisTemplate.opsForValue().set(lockoutKey, lockout, lockoutDuration);
    auditService.logHighPrioritySecurityEvent(
        "ACCOUNT_LOCKED",
        email,
        String.format(
            "Account locked for %s after %d failed attempts (lockout #%d)",
            formatDuration(lockoutDuration), attemptCount, lockoutAttempts));
  }

  /**
   * Calculates the lockout duration based on the number of previous lockouts, implementing an
   * exponential backoff strategy.
   *
   * @param lockoutAttempt The sequential number of this lockout event.
   * @return The calculated {@link Duration} for the lockout.
   */
  private Duration calculateLockoutDuration(int lockoutAttempt) {
    long minutes = BASE_LOCKOUT_MINUTES * (long) Math.pow(2, lockoutAttempt - 1);
    long maxMinutes = MAX_LOCKOUT_HOURS * 60;
    minutes = Math.min(minutes, maxMinutes);
    return Duration.ofMinutes(minutes);
  }

  /**
   * Formats a {@link Duration} into a human-readable string for logging.
   *
   * @param duration The duration to format.
   * @return A string representation (e.g., "5 minutes", "1 hours 20 minutes").
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
   * A record representing the state of an account lockout.
   *
   * @param email The email address of the locked account.
   * @param lockedAt The timestamp when the lockout was initiated.
   * @param expiresAt The timestamp when the lockout will automatically expire.
   * @param lockoutNumber The sequential number of this lockout.
   * @param duration The total duration of this lockout.
   */
  public static record AccountLockout(
      String email,
      LocalDateTime lockedAt,
      LocalDateTime expiresAt,
      int lockoutNumber,
      Duration duration) {}
}