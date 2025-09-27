package com.platform.shared.validation;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Utility class for validating inputs to prevent SQL injection attacks.
 * Used by services that construct dynamic queries.
 */
@Component
public class SqlInjectionValidator {

    // Common SQL injection patterns
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
        Pattern.compile("('.+(--|\\/\\*))", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(union|select|insert|update|delete|drop|create|alter|exec|execute)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(script|javascript|vbscript|onload|onerror)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\||&|;|\\$|%|@|'|\"|\\?|<|>|\\(|\\)|\\+|\\-|\\*|\\/|=)", Pattern.CASE_INSENSITIVE)
    };

    // SQL keywords that should never appear in user input
    private static final Set<String> DANGEROUS_SQL_KEYWORDS = Set.of(
        "union", "select", "insert", "update", "delete", "drop", "create", "alter",
        "exec", "execute", "sp_", "xp_", "script", "declare", "char", "varchar",
        "nchar", "nvarchar", "cast", "convert", "substring", "ascii", "waitfor"
    );

    /**
     * Validates that input doesn't contain SQL injection patterns.
     *
     * @param input The input string to validate
     * @param fieldName Name of the field being validated (for error messages)
     * @throws IllegalArgumentException if input contains suspicious patterns
     */
    public void validateInput(String input, String fieldName) {
        if (input == null) {
            return; // Null inputs are handled separately
        }

        String normalizedInput = input.trim().toLowerCase();

        // Check against known SQL injection patterns
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(normalizedInput).find()) {
                throw new IllegalArgumentException(
                    String.format("Invalid characters detected in %s: potentially malicious input", fieldName)
                );
            }
        }

        // Check for dangerous SQL keywords
        for (String keyword : DANGEROUS_SQL_KEYWORDS) {
            if (normalizedInput.contains(keyword)) {
                throw new IllegalArgumentException(
                    String.format("Forbidden keyword '%s' detected in %s", keyword, fieldName)
                );
            }
        }
    }

    /**
     * Validates that input is from a predefined allowlist.
     *
     * @param input The input to validate
     * @param allowedValues Set of allowed values
     * @param fieldName Name of the field being validated
     * @throws IllegalArgumentException if input is not in allowlist
     */
    public void validateAgainstAllowlist(String input, Set<String> allowedValues, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }

        String normalizedInput = input.trim().toLowerCase();

        if (!allowedValues.contains(normalizedInput)) {
            throw new IllegalArgumentException(
                String.format("Invalid value for %s: '%s'. Allowed values: %s",
                    fieldName, input, allowedValues)
            );
        }
    }

    /**
     * Validates numeric input to prevent injection through number fields.
     *
     * @param value The numeric value
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @param fieldName Name of the field being validated
     */
    public void validateNumericRange(long value, long min, long max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d, got: %d", fieldName, min, max, value)
            );
        }
    }

    /**
     * Sanitizes string input by removing potentially dangerous characters.
     * Use with caution - validation and allowlists are preferred.
     *
     * @param input The input to sanitize
     * @return Sanitized string
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        return input
            .replaceAll("[<>\"'%;)(&+\\-]", "") // Remove dangerous characters
            .replaceAll("\\s+", " ") // Normalize whitespace
            .trim();
    }
}