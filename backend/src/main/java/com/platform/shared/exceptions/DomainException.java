package com.platform.shared.exceptions;

/**
 * Base exception for all domain-level exceptions.
 * Domain exceptions represent business rule violations and invalid operations.
 *
 * @since 1.0.0
 */
public class DomainException extends RuntimeException {

    /**
     * Constructs a new domain exception with the specified detail message.
     *
     * @param message the detail message
     */
    public DomainException(final String message) {
        super(message);
    }

    /**
     * Constructs a new domain exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public DomainException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
