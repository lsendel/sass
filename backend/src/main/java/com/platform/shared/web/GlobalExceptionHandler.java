package com.platform.shared.web;

import java.time.Instant;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stripe.exception.StripeException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(
      IllegalArgumentException ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleConflict(
      IllegalStateException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), req);
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(
      SecurityException ex, HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
  }

  @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      RuntimeException ex, HttpServletRequest req) {
    String message = ex.getMessage() != null ? ex.getMessage() : "Access denied";
    return build(HttpStatus.FORBIDDEN, message, req);
  }

  @ExceptionHandler(StripeException.class)
  public ResponseEntity<ErrorResponse> handleStripe(StripeException ex, HttpServletRequest req) {
    String message = ex.getMessage() != null ? ex.getMessage() : "Stripe error";
    return build(HttpStatus.BAD_REQUEST, message, req);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnhandled(Exception ex, HttpServletRequest req) {
    // Avoid leaking details; log server-side (logger omitted for brevity)
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", req);
  }

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status, String message, HttpServletRequest req) {
    String path = req != null ? req.getRequestURI() : "";
    ErrorResponse body =
        new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path,
            UUID.randomUUID().toString());
    return ResponseEntity.status(status).body(body);
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ErrorResponse(
      String timestamp,
      int status,
      String error,
      String message,
      String path,
      String correlationId) {}
}
