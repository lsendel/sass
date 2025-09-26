package com.platform.shared.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.platform.shared.logging.StructuredLogger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Performance monitoring aspect that tracks method execution times,
 * error rates, and provides structured logging for performance metrics.
 */
@Aspect
@Component
public class PerformanceMonitor {

    private static final StructuredLogger log = StructuredLogger.getLogger(PerformanceMonitor.class);
    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();

    public PerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(monitored)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String metricName = getMetricName(joinPoint, monitored);

        Instant startTime = Instant.now();
        Timer timer = getOrCreateTimer(metricName);

        try {
            // Add performance context to MDC
            MDC.put("monitoredMethod", methodName);
            MDC.put("startTime", startTime.toString());

            Object result = joinPoint.proceed();

            // Record success
            recordSuccess(metricName, startTime);

            return result;

        } catch (Throwable throwable) {
            // Record error
            recordError(metricName, startTime, throwable);
            throw throwable;

        } finally {
            // Clean up MDC
            MDC.remove("monitoredMethod");
            MDC.remove("startTime");

            // Record timing
            timer.record(Duration.between(startTime, Instant.now()));
        }
    }

    @Around("execution(* com.platform.*.api.*Controller.*(..))")
    public Object monitorControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = "controller." + className.substring(className.lastIndexOf('.') + 1) + "." + methodName;

        return monitorExecution(joinPoint, metricName, "CONTROLLER");
    }

    @Around("execution(* com.platform.*.internal.*Service.*(..))")
    public Object monitorServices(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = "service." + className.substring(className.lastIndexOf('.') + 1) + "." + methodName;

        return monitorExecution(joinPoint, metricName, "SERVICE");
    }

    @Around("execution(* com.platform.*.internal.*Repository.*(..))")
    public Object monitorRepositories(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = "repository." + className.substring(className.lastIndexOf('.') + 1) + "." + methodName;

        return monitorExecution(joinPoint, metricName, "REPOSITORY");
    }

    private Object monitorExecution(ProceedingJoinPoint joinPoint, String metricName, String layer) throws Throwable {
        Instant startTime = Instant.now();
        Timer timer = getOrCreateTimer(metricName);

        try {
            MDC.put("performanceLayer", layer);
            MDC.put("performanceMetric", metricName);

            Object result = joinPoint.proceed();
            recordSuccess(metricName, startTime);
            return result;

        } catch (Throwable throwable) {
            recordError(metricName, startTime, throwable);
            throw throwable;

        } finally {
            MDC.remove("performanceLayer");
            MDC.remove("performanceMetric");
            timer.record(Duration.between(startTime, Instant.now()));
        }
    }

    private String getMetricName(ProceedingJoinPoint joinPoint, Monitored monitored) {
        if (monitored.value() != null && !monitored.value().isEmpty()) {
            return monitored.value();
        }

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        return className.substring(className.lastIndexOf('.') + 1) + "." + methodName;
    }

    private Timer getOrCreateTimer(String metricName) {
        return timers.computeIfAbsent(metricName, name ->
            Timer.builder("method.execution.time")
                .tag("method", name)
                .description("Execution time for method: " + name)
                .register(meterRegistry)
        );
    }

    private void recordSuccess(String metricName, Instant startTime) {
        Counter successCounter = successCounters.computeIfAbsent(metricName, name ->
            Counter.builder("method.execution.success")
                .tag("method", name)
                .description("Successful executions for method: " + name)
                .register(meterRegistry)
        );

        successCounter.increment();

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        log.performanceEvent(metricName, durationMs,
            Map.of("status", "success", "correlationId", MDC.get("correlationId") != null ? MDC.get("correlationId") : "unknown"));
    }

    private void recordError(String metricName, Instant startTime, Throwable throwable) {
        Counter errorCounter = errorCounters.computeIfAbsent(metricName, name ->
            Counter.builder("method.execution.error")
                .tag("method", name)
                .tag("error.type", throwable.getClass().getSimpleName())
                .description("Failed executions for method: " + name)
                .register(meterRegistry)
        );

        errorCounter.increment();

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        log.performanceEvent(metricName, durationMs,
            Map.of(
                "status", "error",
                "errorType", throwable.getClass().getSimpleName(),
                "errorMessage", throwable.getMessage() != null ? throwable.getMessage() : "Unknown error",
                "correlationId", MDC.get("correlationId") != null ? MDC.get("correlationId") : "unknown"
            ));
    }

    /**
     * Annotation to mark methods for performance monitoring
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Monitored {
        /**
         * Custom metric name. If empty, will use class.method format
         */
        String value() default "";

        /**
         * Whether to include method parameters in logging (be careful with sensitive data)
         */
        boolean includeParameters() default false;

        /**
         * Slow execution threshold in milliseconds. If exceeded, will log as warning
         */
        long slowThresholdMs() default 1000;
    }

    /**
     * Manual performance tracking for custom operations
     */
    public static class PerformanceTracker implements AutoCloseable {
        private final String operationName;
        private final Instant startTime;
        private final StructuredLogger logger;

        public PerformanceTracker(String operationName) {
            this.operationName = operationName;
            this.startTime = Instant.now();
            this.logger = StructuredLogger.getLogger(PerformanceTracker.class);
        }

        public static PerformanceTracker track(String operationName) {
            return new PerformanceTracker(operationName);
        }

        @Override
        public void close() {
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();
            logger.performanceEvent(operationName, durationMs,
                Map.of("manual", true, "correlationId", MDC.get("correlationId") != null ? MDC.get("correlationId") : "unknown"));
        }

        public void addMetadata(String key, Object value) {
            MDC.put("perf." + key, String.valueOf(value));
        }
    }

    /**
     * Database query performance tracking
     */
    @Around("execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object monitorDatabaseQueries(ProceedingJoinPoint joinPoint) throws Throwable {
        String repositoryName = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = "db.query." + repositoryName.substring(repositoryName.lastIndexOf('.') + 1) + "." + methodName;

        Instant startTime = Instant.now();
        Timer timer = getOrCreateTimer(metricName);

        try {
            MDC.put("dbQuery", metricName);

            Object result = joinPoint.proceed();

            long durationMs = Duration.between(startTime, Instant.now()).toMillis();

            // Log slow queries
            if (durationMs > 100) { // 100ms threshold for slow queries
                log.warn("Slow database query detected",
                    "query", metricName,
                    "durationMs", durationMs,
                    "threshold", 100);
            }

            recordSuccess(metricName, startTime);
            return result;

        } catch (Throwable throwable) {
            recordError(metricName, startTime, throwable);
            throw throwable;

        } finally {
            MDC.remove("dbQuery");
            timer.record(Duration.between(startTime, Instant.now()));
        }
    }

    /**
     * External API call monitoring
     */
    public static void trackExternalApiCall(String apiName, Runnable operation) {
        try (PerformanceTracker tracker = PerformanceTracker.track("external.api." + apiName)) {
            tracker.addMetadata("apiName", apiName);
            operation.run();
        }
    }

    /**
     * Cache operation monitoring
     */
    public static <T> T trackCacheOperation(String cacheOperation, String cacheName, java.util.function.Supplier<T> operation) {
        try (PerformanceTracker tracker = PerformanceTracker.track("cache." + cacheOperation)) {
            tracker.addMetadata("cacheName", cacheName);
            return operation.get();
        }
    }
}