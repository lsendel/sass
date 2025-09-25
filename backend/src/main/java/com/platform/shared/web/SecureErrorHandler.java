package com.platform.shared.web;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Secure error handler that prevents information disclosure in production.
 */
@RestControllerAdvice
public class SecureErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(SecureErrorHandler.class);

    @Value("${spring.profiles.active:development}")
    private String activeProfile;

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.warn("Access denied [{}]: {}", correlationId, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", "Access denied", correlationId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        logger.error("Unexpected error [{}]", correlationId, ex);
        
        String message = isProduction() ? "An error occurred" : ex.getMessage();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", message, correlationId));
    }

    private boolean isProduction() {
        return "prod".equals(activeProfile) || "production".equals(activeProfile);
    }

    public record ErrorResponse(String code, String message, String correlationId) {}
}
