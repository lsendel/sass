package com.platform.shared.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityNotFoundException(Class<?> entityType, Object id) {
        super(String.format("%s with id '%s' not found", entityType.getSimpleName(), id));
    }
}