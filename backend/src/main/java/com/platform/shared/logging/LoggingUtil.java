package com.platform.shared.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for common logging operations.
 */
public final class LoggingUtil {

    private LoggingUtil() {
        // Utility class
    }

    /**
     * Sets a correlation ID for the current thread context.
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put("correlationId", correlationId);
    }

    /**
     * Generates and sets a new correlation ID for the current thread context.
     */
    public static String generateAndSetCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        setCorrelationId(correlationId);
        return correlationId;
    }

    /**
     * Gets the current correlation ID from the thread context.
     */
    public static String getCorrelationId() {
        return MDC.get("correlationId");
    }

    /**
     * Clears the correlation ID from the thread context.
     */
    public static void clearCorrelationId() {
        MDC.remove("correlationId");
    }

    /**
     * Clears all MDC context.
     */
    public static void clearContext() {
        MDC.clear();
    }

    /**
     * Creates a logger instance for the given class.
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Logs an error with correlation ID.
     */
    public static void logError(Logger logger, String message, Throwable throwable) {
        String correlationId = getCorrelationId();
        if (correlationId != null) {
            logger.error("[{}] {}", correlationId, message, throwable);
        } else {
            logger.error(message, throwable);
        }
    }

    /**
     * Logs an info message with correlation ID.
     */
    public static void logInfo(Logger logger, String message, Object... args) {
        String correlationId = getCorrelationId();
        if (correlationId != null) {
            Object[] newArgs = new Object[args.length + 2];
            newArgs[0] = correlationId;
            newArgs[1] = message;
            System.arraycopy(args, 0, newArgs, 2, args.length);
            logger.info("[{}] {}", newArgs);
        } else {
            logger.info(message, args);
        }
    }
}