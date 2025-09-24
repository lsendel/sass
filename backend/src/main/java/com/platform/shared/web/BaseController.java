package com.platform.shared.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Base controller with common functionality to avoid architecture violations.
 */
@RestControllerAdvice
public class BaseController {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        if (e instanceof SecurityException
            || e instanceof AccessDeniedException
            || e instanceof AuthorizationDeniedException) {
            throw e;
        }
        return ResponseEntity.internalServerError().body("Internal error");
    }
}
