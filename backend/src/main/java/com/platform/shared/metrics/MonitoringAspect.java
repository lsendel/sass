package com.platform.shared.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.platform.shared.logging.LoggingUtil;

/**
 * Aspect for monitoring and metrics collection.
 * Automatically tracks execution time and counts for methods annotated with monitoring annotations.
 */
@Aspect
@Component
public class MonitoringAspect {
    
    private static final Logger logger = LoggingUtil.getLogger(MonitoringAspect.class);
    
    private final MetricsService metricsService;
    
    public MonitoringAspect(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    @Around("@annotation(Monitored)")
    public Object monitorMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = String.format("%s.%s", className, methodName);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Increment the invocation counter
            metricsService.incrementCounter("method_invocations." + metricName);
            
            // Execute the original method
            Object result = joinPoint.proceed();
            
            // Record the execution time
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordTiming("method_execution_time." + metricName, duration);
            
            logger.debug("Method {} executed in {}ms", metricName, duration);
            
            return result;
        } catch (Exception e) {
            // Increment error counter
            metricsService.incrementCounter("method_errors." + metricName);
            long duration = System.currentTimeMillis() - startTime;
            logger.warn("Method {} failed after {}ms: {}", metricName, duration, e.getMessage());
            throw e;
        }
    }
}