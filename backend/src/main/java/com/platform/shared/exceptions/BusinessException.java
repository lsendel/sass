package com.platform.shared.exceptions;

/**
 * Exception thrown when business logic validation fails.
 * Results in HTTP 409 responses indicating a conflict with current state.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}