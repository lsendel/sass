package com.platform.shared.exceptions;

/**
 * Exception thrown when input validation fails.
 *
 * @since 1.0.0
 */
public class ValidationException extends DomainException {

    /**
     * Constructs a new validation exception with the specified detail message.
     *
     * @param message the detail message
     */
    public ValidationException(final String message) {
        super(message);
    }
}
