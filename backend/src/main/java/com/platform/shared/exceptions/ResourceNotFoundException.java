package com.platform.shared.exceptions;

/**
 * Exception thrown when a requested resource is not found.
 *
 * @since 1.0.0
 */
public class ResourceNotFoundException extends DomainException {

    /**
     * Constructs a new resource not found exception with the specified detail message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(final String message) {
        super(message);
    }
}
