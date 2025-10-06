package com.platform.shared.exceptions;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 responses.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}