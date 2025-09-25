package com.platform.shared.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for automatic monitoring and metrics collection.
 * Methods annotated with this will have their execution time and invocation count tracked.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitored {
    /**
     * Optional name override for the metric. If not specified,
     * the metric name will be derived from the class and method name.
     */
    String value() default "";
    
    /**
     * Whether to track execution time (default: true).
     */
    boolean trackExecutionTime() default true;
    
    /**
     * Whether to track invocation count (default: true).
     */
    boolean trackInvocationCount() default true;
    
    /**
     * Whether to track error count (default: true).
     */
    boolean trackErrorCount() default true;
}