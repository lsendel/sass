package com.platform.auth.internal;

import java.util.List;

/**
 * Exception thrown when password validation fails.
 * Contains details about what validation rules were violated.
 */
public class PasswordValidationException extends RuntimeException {

    private final List<String> violations;

    public PasswordValidationException(List<String> violations) {
        super("Password validation failed: " + String.join(", ", violations));
        this.violations = violations;
    }

    public List<String> getViolations() {
        return violations;
    }
}