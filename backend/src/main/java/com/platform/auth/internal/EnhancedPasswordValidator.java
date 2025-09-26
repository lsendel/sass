package com.platform.auth.internal;

import java.util.ArrayList;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.platform.shared.security.PasswordProperties;

/**
 * Enhanced password validator implementing CLAUDE.md security requirements.
 * Enforces 12+ character passwords with complexity requirements and common password detection.
 */
@Component
public class EnhancedPasswordValidator {

    private final PasswordProperties passwordProperties;

    // Common password patterns to prevent
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "123456", "123456789", "12345678", "12345", "1234567",
        "qwerty", "abc123", "password123", "admin", "letmein", "welcome",
        "monkey", "dragon", "master", "shadow", "superman", "michael",
        "jordan", "iloveyou", "sunshine", "princess", "football", "baseball",
        "charlie", "bailey", "harley", "hunter", "soccer", "computer",
        "michelle", "jessica", "pepper", "1234567890", "zaq12wsx", "mustang"
    );

    // Common patterns to detect
    private static final Set<String> COMMON_PATTERNS = Set.of(
        "123", "abc", "qwe", "asd", "zxc", "111", "000", "aaa"
    );

    public EnhancedPasswordValidator(PasswordProperties passwordProperties) {
        this.passwordProperties = passwordProperties;
    }

    /**
     * Validates password against enhanced security policies.
     *
     * @param password the password to validate
     * @throws PasswordValidationException if validation fails
     */
    public void validatePassword(String password) {
        var violations = new ArrayList<String>();

        if (password == null) {
            violations.add("Password cannot be null");
            throw new PasswordValidationException(violations);
        }

        // Enhanced length check - CLAUDE.md requires 12+ characters
        int minLength = Math.max(12, passwordProperties.getMinLength());
        if (password.length() < minLength) {
            violations.add("Password must be at least " + minLength + " characters long");
        }

        if (password.length() > passwordProperties.getPolicy().getMaxLength()) {
            violations.add("Password cannot exceed " + passwordProperties.getPolicy().getMaxLength() + " characters");
        }

        // Complexity requirements
        if (passwordProperties.isRequireUppercase() && !password.matches(".*[A-Z].*")) {
            violations.add("Password must contain at least one uppercase letter");
        }

        if (passwordProperties.isRequireLowercase() && !password.matches(".*[a-z].*")) {
            violations.add("Password must contain at least one lowercase letter");
        }

        if (passwordProperties.isRequireDigits() && !password.matches(".*\\d.*")) {
            violations.add("Password must contain at least one digit");
        }

        if (passwordProperties.isRequireSpecialChars()) {
            String specialChars = passwordProperties.getPolicy().getSpecialChars();
            String escapedSpecialChars = specialChars.replaceAll("([\\[\\](){}*+?^$|\\\\.])", "\\\\$1");
            if (!password.matches(".*[" + escapedSpecialChars + "].*")) {
                violations.add("Password must contain at least one special character (" + specialChars + ")");
            }
        }

        // Check for common passwords and patterns
        if (passwordProperties.getPolicy().isPreventCommonPasswords()) {
            if (isCommonPassword(password)) {
                violations.add("Password is too common or predictable. Please choose a more unique password");
            }

            if (containsCommonPatterns(password)) {
                violations.add("Password contains common patterns. Please avoid sequential or repeated characters");
            }
        }

        // Check for obvious patterns
        if (containsObviousPatterns(password)) {
            violations.add("Password contains obvious patterns. Please use a more complex password");
        }

        if (!violations.isEmpty()) {
            throw new PasswordValidationException(violations);
        }
    }

    /**
     * Checks if password is in the common passwords list.
     */
    private boolean isCommonPassword(String password) {
        String lowercasePassword = password.toLowerCase();

        // Direct match
        if (COMMON_PASSWORDS.contains(lowercasePassword)) {
            return true;
        }

        // Check if password starts with or contains common passwords
        return COMMON_PASSWORDS.stream()
            .anyMatch(common ->
                lowercasePassword.startsWith(common) ||
                lowercasePassword.endsWith(common) ||
                lowercasePassword.contains(common)
            );
    }

    /**
     * Checks for common patterns like "123", "abc", "qwerty", etc.
     */
    private boolean containsCommonPatterns(String password) {
        String lowercasePassword = password.toLowerCase();

        return COMMON_PATTERNS.stream()
            .anyMatch(lowercasePassword::contains);
    }

    /**
     * Checks for obvious patterns like repeated characters, sequences, etc.
     */
    private boolean containsObviousPatterns(String password) {
        // Check for 3+ repeated characters
        if (password.matches(".*(.)\\1{2,}.*")) {
            return true;
        }

        // Check for ascending sequences (123, abc)
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }
        }

        // Check for descending sequences (321, cba)
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            if (c2 == c1 - 1 && c3 == c2 - 1) {
                return true;
            }
        }

        // Check keyboard patterns (qwerty, asdf)
        String lowercasePassword = password.toLowerCase();
        if (lowercasePassword.contains("qwerty") ||
            lowercasePassword.contains("asdf") ||
            lowercasePassword.contains("zxcv")) {
            return true;
        }

        return false;
    }

    /**
     * Checks password strength and returns a score.
     *
     * @param password the password to check
     * @return strength score (0-100)
     */
    public int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length scoring
        if (password.length() >= 12) score += 25;
        else if (password.length() >= 8) score += 15;
        else if (password.length() >= 6) score += 10;

        // Character variety
        if (password.matches(".*[A-Z].*")) score += 15;
        if (password.matches(".*[a-z].*")) score += 15;
        if (password.matches(".*\\d.*")) score += 15;
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) score += 20;

        // Bonus for length beyond minimum
        if (password.length() > 16) score += 10;

        // Penalty for common patterns
        if (isCommonPassword(password)) score -= 30;
        if (containsCommonPatterns(password)) score -= 20;
        if (containsObviousPatterns(password)) score -= 25;

        return Math.max(0, Math.min(100, score));
    }
}