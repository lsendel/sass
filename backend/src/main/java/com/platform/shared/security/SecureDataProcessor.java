package com.platform.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Secure data processing utility for handling sensitive information.
 * Implements clean code principles while maintaining strict security standards.
 */
@Component
public class SecureDataProcessor {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SALT_ALGORITHM = "SHA1PRNG";
    private static final int SALT_LENGTH = 32;
    private static final int MIN_PASSWORD_LENGTH = 12;

    // Security patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$" // E.164 format
    );
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b(?:\\d[ -]*?){13,19}\\b"
    );
    private static final Pattern SSN_PATTERN = Pattern.compile(
        "\\b\\d{3}-?\\d{2}-?\\d{4}\\b"
    );

    private final SecureRandom secureRandom;

    public SecureDataProcessor() {
        try {
            this.secureRandom = SecureRandom.getInstance(SALT_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityConfigurationException("Failed to initialize secure random generator", e);
        }
    }

    /**
     * Securely hashes a password with a random salt.
     *
     * @param password the plain text password
     * @return a SecureHashResult containing the hash and salt
     * @throws SecurityValidationException if password doesn't meet requirements
     */
    public SecureHashResult hashPassword(String password) {
        validatePasswordStrength(password);

        byte[] salt = generateSalt();
        byte[] hash = hashWithSalt(password, salt);

        return new SecureHashResult(
            Base64.getEncoder().encodeToString(hash),
            Base64.getEncoder().encodeToString(salt)
        );
    }

    /**
     * Verifies a password against a stored hash and salt.
     *
     * @param password the plain text password to verify
     * @param storedHash the stored hash (base64 encoded)
     * @param storedSalt the stored salt (base64 encoded)
     * @return true if password matches
     */
    public boolean verifyPassword(String password, String storedHash, String storedSalt) {
        if (!StringUtils.hasText(password) || !StringUtils.hasText(storedHash) || !StringUtils.hasText(storedSalt)) {
            return false;
        }

        try {
            byte[] salt = Base64.getDecoder().decode(storedSalt);
            byte[] computedHash = hashWithSalt(password, salt);
            byte[] expectedHash = Base64.getDecoder().decode(storedHash);

            return MessageDigest.isEqual(computedHash, expectedHash);
        } catch (Exception e) {
            // Log security event but don't expose details
            logSecurityEvent("Password verification failed", e);
            return false;
        }
    }

    /**
     * Sanitizes input to prevent injection attacks.
     *
     * @param input the input string to sanitize
     * @return sanitized string safe for processing
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        return input
            .trim()
            .replaceAll("[<>\"'&]", "") // Remove potential XSS characters
            .replaceAll("\\p{Cntrl}", "") // Remove control characters
            .replaceAll("\\s+", " "); // Normalize whitespace
    }

    /**
     * Redacts sensitive information from strings for logging.
     *
     * @param input the input string that may contain sensitive data
     * @return string with sensitive information redacted
     */
    public String redactSensitiveData(String input) {
        if (input == null) {
            return null;
        }

        String redacted = input;

        // Redact email addresses
        redacted = EMAIL_PATTERN.matcher(redacted).replaceAll("[EMAIL_REDACTED]");

        // Redact phone numbers
        redacted = PHONE_PATTERN.matcher(redacted).replaceAll("[PHONE_REDACTED]");

        // Redact credit card numbers
        redacted = CREDIT_CARD_PATTERN.matcher(redacted).replaceAll("[CARD_REDACTED]");

        // Redact SSN
        redacted = SSN_PATTERN.matcher(redacted).replaceAll("[SSN_REDACTED]");

        return redacted;
    }

    /**
     * Validates if a string contains only safe characters for SQL queries.
     *
     * @param input the input to validate
     * @return true if input is safe for SQL processing
     */
    public boolean isSqlSafe(String input) {
        if (input == null) {
            return true; // null is safe
        }

        // Check for common SQL injection patterns
        String lowercaseInput = input.toLowerCase();
        String[] dangerousPatterns = {
            "--", "/*", "*/", "xp_", "sp_", "exec", "execute",
            "drop", "create", "alter", "insert", "update", "delete",
            "union", "select", "from", "where", "or 1=1", "' or '1'='1"
        };

        for (String pattern : dangerousPatterns) {
            if (lowercaseInput.contains(pattern)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Generates a cryptographically secure token.
     *
     * @param length the desired token length in bytes
     * @return base64-encoded secure token
     */
    public String generateSecureToken(int length) {
        if (length <= 0 || length > 1024) {
            throw new IllegalArgumentException("Token length must be between 1 and 1024 bytes");
        }

        byte[] tokenBytes = new byte[length];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Validates password strength according to security policy.
     */
    private void validatePasswordStrength(String password) {
        if (!StringUtils.hasText(password)) {
            throw new SecurityValidationException("Password cannot be empty");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new SecurityValidationException(
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long"
            );
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecialChar = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecialChar) {
            throw new SecurityValidationException(
                "Password must contain at least one uppercase letter, lowercase letter, digit, and special character"
            );
        }

        // Check for common weak passwords
        String[] commonPasswords = {
            "password123", "admin123", "qwerty123", "letmein123",
            "welcome123", "password1234", "123456789012"
        };

        for (String commonPassword : commonPasswords) {
            if (password.toLowerCase().contains(commonPassword.toLowerCase())) {
                throw new SecurityValidationException("Password contains common weak patterns");
            }
        }
    }

    /**
     * Generates a cryptographically secure salt.
     */
    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes a password with the provided salt.
     */
    private byte[] hashWithSalt(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(salt);
            return digest.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityConfigurationException("Hash algorithm not available: " + HASH_ALGORITHM, e);
        }
    }

    /**
     * Logs security events for monitoring.
     */
    private void logSecurityEvent(String message, Exception e) {
        // In production, this would integrate with security monitoring system
        // For now, we'll use standard logging
        org.slf4j.LoggerFactory.getLogger(SecureDataProcessor.class)
            .warn("Security event: {} - {}", message, e.getMessage());
    }

    /**
     * Result object for secure hash operations.
     */
    public record SecureHashResult(String hash, String salt) {
        public SecureHashResult {
            if (hash == null || salt == null) {
                throw new IllegalArgumentException("Hash and salt cannot be null");
            }
        }
    }

    /**
     * Exception for security validation failures.
     */
    public static class SecurityValidationException extends RuntimeException {
        public SecurityValidationException(String message) {
            super(message);
        }

        public SecurityValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception for security configuration issues.
     */
    public static class SecurityConfigurationException extends RuntimeException {
        public SecurityConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}