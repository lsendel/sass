package com.platform.shared.exceptions;

import com.platform.shared.dto.ErrorResponse;
import com.platform.shared.dto.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for consistent error responses across the application.
 *
 * Features:
 * - Standardized error response format
 * - Correlation ID tracking for debugging
 * - Structured logging with context
 * - Security-aware error messages
 * - Validation error handling
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC = "correlationId";

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String correlationId = getOrCreateCorrelationId();

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .message("Validation failed")
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        logger.warn("Validation error: {} fields failed validation for request: {}",
                fieldErrors.size(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(errorResponse);
    }

    /**
     * Handles custom validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {

        String correlationId = getOrCreateCorrelationId();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        logger.warn("Custom validation error: {} for request: {}",
                ex.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(errorResponse);
    }

    /**
     * Handles authentication failures.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        String correlationId = getOrCreateCorrelationId();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Invalid credentials")
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        // Log with additional security context
        logger.warn("Authentication failed for request: {} from IP: {}",
                request.getRequestURI(),
                getClientIpAddress(request));

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(errorResponse);
    }

    /**
     * Handles authorization failures.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        String correlationId = getOrCreateCorrelationId();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Access denied")
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        logger.warn("Access denied for request: {} from IP: {}",
                request.getRequestURI(),
                getClientIpAddress(request));

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(errorResponse);
    }

    /**
     * Handles resource not found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        String correlationId = getOrCreateCorrelationId();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        logger.info("Resource not found: {} for request: {}",
                ex.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(errorResponse);
    }

    /**
     * Handles business logic exceptions.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        String correlationId = getOrCreateCorrelationId();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        logger.warn("Business exception: {} for request: {}",
                ex.getMessage(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(errorResponse);
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        String correlationId = getOrCreateCorrelationId();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("An unexpected error occurred")
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        // Log full exception details for debugging
        logger.error("Unexpected error for request: {} from IP: {}",
                request.getRequestURI(),
                getClientIpAddress(request),
                ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(errorResponse);
    }

    /**
     * Gets or creates a correlation ID for request tracking.
     */
    private String getOrCreateCorrelationId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String correlationId = null;
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            correlationId = request.getHeader(CORRELATION_ID_HEADER);
        }

        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Add to MDC for structured logging
        MDC.put(CORRELATION_ID_MDC, correlationId);

        return correlationId;
    }

    /**
     * Extracts client IP address considering proxies.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}