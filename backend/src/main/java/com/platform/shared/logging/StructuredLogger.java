package com.platform.shared.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StructuredLogger {

    private final Logger logger;
    private final ObjectMapper objectMapper;

    public StructuredLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
        this.objectMapper = new ObjectMapper();
    }

    public static StructuredLogger getLogger(Class<?> clazz) {
        return new StructuredLogger(clazz);
    }

    public void info(String message, Object... context) {
        logStructured("INFO", message, null, context);
    }

    public void warn(String message, Object... context) {
        logStructured("WARN", message, null, context);
    }

    public void error(String message, Throwable throwable, Object... context) {
        logStructured("ERROR", message, throwable, context);
    }

    public void debug(String message, Object... context) {
        logStructured("DEBUG", message, null, context);
    }

    public void auditEvent(String event, UUID userId, UUID organizationId, Object details) {
        Map<String, Object> auditContext = new HashMap<>();
        auditContext.put("event", event);
        auditContext.put("userId", userId);
        auditContext.put("organizationId", organizationId);
        auditContext.put("details", details);
        auditContext.put("timestamp", System.currentTimeMillis());

        logStructured("AUDIT", "Audit event: " + event, null, auditContext);
    }

    public void securityEvent(String event, String details, String remoteIp) {
        Map<String, Object> securityContext = new HashMap<>();
        securityContext.put("event", event);
        securityContext.put("details", details);
        securityContext.put("remoteIp", remoteIp);
        securityContext.put("timestamp", System.currentTimeMillis());

        logStructured("SECURITY", "Security event: " + event, null, securityContext);
    }

    public void performanceEvent(String operation, long durationMs, Object metadata) {
        Map<String, Object> perfContext = new HashMap<>();
        perfContext.put("operation", operation);
        perfContext.put("durationMs", durationMs);
        perfContext.put("metadata", metadata);
        perfContext.put("timestamp", System.currentTimeMillis());

        logStructured("PERFORMANCE", "Performance metric: " + operation, null, perfContext);
    }

    private void logStructured(String level, String message, Throwable throwable, Object... context) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("level", level);
            logEntry.put("message", message);
            logEntry.put("correlationId", MDC.get("correlationId"));
            logEntry.put("timestamp", System.currentTimeMillis());

            if (context.length > 0) {
                Map<String, Object> contextMap = new HashMap<>();
                for (int i = 0; i < context.length; i += 2) {
                    if (i + 1 < context.length) {
                        contextMap.put(String.valueOf(context[i]), context[i + 1]);
                    }
                }
                logEntry.put("context", contextMap);
            }

            String jsonLog = objectMapper.writeValueAsString(logEntry);

            switch (level) {
                case "DEBUG":
                    if (throwable != null) {
                        logger.debug(jsonLog, throwable);
                    } else {
                        logger.debug(jsonLog);
                    }
                    break;
                case "INFO":
                    if (throwable != null) {
                        logger.info(jsonLog, throwable);
                    } else {
                        logger.info(jsonLog);
                    }
                    break;
                case "WARN":
                    if (throwable != null) {
                        logger.warn(jsonLog, throwable);
                    } else {
                        logger.warn(jsonLog);
                    }
                    break;
                case "ERROR":
                    if (throwable != null) {
                        logger.error(jsonLog, throwable);
                    } else {
                        logger.error(jsonLog);
                    }
                    break;
                case "AUDIT":
                    logger.info("[AUDIT] " + jsonLog);
                    break;
                case "SECURITY":
                    logger.warn("[SECURITY] " + jsonLog);
                    break;
                case "PERFORMANCE":
                    logger.info("[PERFORMANCE] " + jsonLog);
                    break;
                default:
                    logger.info(jsonLog);
            }

        } catch (JsonProcessingException e) {
            // Fallback to regular logging if JSON serialization fails
            if (throwable != null) {
                logger.error("Failed to serialize log message: {} - Original message: {}", e.getMessage(), message, throwable);
            } else {
                logger.error("Failed to serialize log message: {} - Original message: {}", e.getMessage(), message);
            }
        }
    }
}