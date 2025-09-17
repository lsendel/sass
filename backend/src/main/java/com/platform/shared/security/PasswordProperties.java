package com.platform.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for password authentication policies. Maps to application.yml
 * app.auth.password configuration.
 */
@Component
@ConfigurationProperties(prefix = "app.auth.password")
public class PasswordProperties {

  private boolean enabled = false;
  private Policy policy = new Policy();
  private Lockout lockout = new Lockout();
  private int maxFailedAttempts = 3;
  private int passwordResetTokenExpiryHours = 1;
  private int emailVerificationTokenExpiryHours = 24;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Policy getPolicy() {
    return policy;
  }

  public void setPolicy(Policy policy) {
    this.policy = policy;
  }

  public Lockout getLockout() {
    return lockout;
  }

  public void setLockout(Lockout lockout) {
    this.lockout = lockout;
  }

  public int getMaxFailedAttempts() {
    return maxFailedAttempts;
  }

  public void setMaxFailedAttempts(int maxFailedAttempts) {
    this.maxFailedAttempts = maxFailedAttempts;
  }

  public int getPasswordResetTokenExpiryHours() {
    return passwordResetTokenExpiryHours;
  }

  public void setPasswordResetTokenExpiryHours(int passwordResetTokenExpiryHours) {
    this.passwordResetTokenExpiryHours = passwordResetTokenExpiryHours;
  }

  public int getEmailVerificationTokenExpiryHours() {
    return emailVerificationTokenExpiryHours;
  }

  public void setEmailVerificationTokenExpiryHours(int emailVerificationTokenExpiryHours) {
    this.emailVerificationTokenExpiryHours = emailVerificationTokenExpiryHours;
  }

  // Convenience methods for service layer
  public int getMinLength() {
    return policy.getMinLength();
  }

  // Backwards-compat convenience setter used in tests
  public void setMinLength(int minLength) {
    this.policy.setMinLength(minLength);
  }

  public boolean isRequireUppercase() {
    return policy.isRequireUppercase();
  }

  public boolean isRequireLowercase() {
    return policy.isRequireLowercase();
  }

  public boolean isRequireDigits() {
    return policy.isRequireDigits();
  }

  public boolean isRequireSpecialChars() {
    return policy.isRequireSpecialChars();
  }

  public static class Policy {
    private int minLength = 12; // Updated to 12 for 2024 security standards
    private int maxLength = 128;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigits = true;
    private boolean requireSpecialChars = true;
    private String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private int passwordHistorySize = 5; // Track last 5 passwords
    private boolean preventCommonPasswords = true; // Check against common password list
    private int minPasswordAge = 1; // Minimum days before password can be changed
    private int maxPasswordAge = 90; // Maximum days before password must be changed

    public int getMinLength() {
      return minLength;
    }

    public void setMinLength(int minLength) {
      this.minLength = minLength;
    }

    public int getMaxLength() {
      return maxLength;
    }

    public void setMaxLength(int maxLength) {
      this.maxLength = maxLength;
    }

    public boolean isRequireUppercase() {
      return requireUppercase;
    }

    public void setRequireUppercase(boolean requireUppercase) {
      this.requireUppercase = requireUppercase;
    }

    public boolean isRequireLowercase() {
      return requireLowercase;
    }

    public void setRequireLowercase(boolean requireLowercase) {
      this.requireLowercase = requireLowercase;
    }

    public boolean isRequireDigits() {
      return requireDigits;
    }

    public void setRequireDigits(boolean requireDigits) {
      this.requireDigits = requireDigits;
    }

    public boolean isRequireSpecialChars() {
      return requireSpecialChars;
    }

    // Compatibility accessors for legacy naming
    public boolean isRequireDigit() {
      return isRequireDigits();
    }

    public void setRequireDigit(boolean requireDigit) {
      setRequireDigits(requireDigit);
    }

    public boolean isRequireSpecial() {
      return isRequireSpecialChars();
    }

    public void setRequireSpecial(boolean requireSpecial) {
      setRequireSpecialChars(requireSpecial);
    }

    public void setRequireSpecialChars(boolean requireSpecialChars) {
      this.requireSpecialChars = requireSpecialChars;
    }

    public String getSpecialChars() {
      return specialChars;
    }

    public void setSpecialChars(String specialChars) {
      this.specialChars = specialChars;
    }

    public int getPasswordHistorySize() {
      return passwordHistorySize;
    }

    public void setPasswordHistorySize(int passwordHistorySize) {
      this.passwordHistorySize = passwordHistorySize;
    }

    public boolean isPreventCommonPasswords() {
      return preventCommonPasswords;
    }

    public void setPreventCommonPasswords(boolean preventCommonPasswords) {
      this.preventCommonPasswords = preventCommonPasswords;
    }

    public int getMinPasswordAge() {
      return minPasswordAge;
    }

    public void setMinPasswordAge(int minPasswordAge) {
      this.minPasswordAge = minPasswordAge;
    }

    public int getMaxPasswordAge() {
      return maxPasswordAge;
    }

    public void setMaxPasswordAge(int maxPasswordAge) {
      this.maxPasswordAge = maxPasswordAge;
    }
  }

  public static class Lockout {
    private String strategy = "exponential"; // "exponential" or "fixed"
    private int initialDurationMinutes = 5;
    private int maxDurationMinutes = 60;
    private double multiplier = 2.0;
    // Compatibility fields used in some tests
    private int maxAttempts = 5;
    private int durationMinutes = 30;

    public String getStrategy() {
      return strategy;
    }

    public void setStrategy(String strategy) {
      this.strategy = strategy;
    }

    public int getInitialDurationMinutes() {
      return initialDurationMinutes;
    }

    public void setInitialDurationMinutes(int initialDurationMinutes) {
      this.initialDurationMinutes = initialDurationMinutes;
    }

    public int getMaxDurationMinutes() {
      return maxDurationMinutes;
    }

    public void setMaxDurationMinutes(int maxDurationMinutes) {
      this.maxDurationMinutes = maxDurationMinutes;
    }

    public double getMultiplier() {
      return multiplier;
    }

    public void setMultiplier(double multiplier) {
      this.multiplier = multiplier;
    }

    // Legacy-style getters/setters for tests
    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public int getDurationMinutes() {
      return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
      this.durationMinutes = durationMinutes;
      this.initialDurationMinutes = durationMinutes;
    }
  }
}
