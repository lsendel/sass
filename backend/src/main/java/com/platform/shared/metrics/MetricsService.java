package com.platform.shared.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/**
 * Metrics service for tracking application performance and usage metrics.
 * Provides counters, gauges, and timing utilities for monitoring.
 */
@Service
public class MetricsService {
    
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> timers = new ConcurrentHashMap<>();
    private final Map<String, Long> gauges = new ConcurrentHashMap<>();
    
    /**
     * Increments a counter metric.
     * 
     * @param metricName the name of the metric
     * @return the new value after increment
     */
    public long incrementCounter(String metricName) {
        return counters.computeIfAbsent(metricName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Increments a counter by a specific value.
     * 
     * @param metricName the name of the metric
     * @param value the value to add
     * @return the new value after increment
     */
    public long incrementCounter(String metricName, long value) {
        return counters.computeIfAbsent(metricName, k -> new AtomicLong(0)).addAndGet(value);
    }
    
    /**
     * Records a timing metric.
     * 
     * @param metricName the name of the timing metric
     * @param duration the duration in milliseconds
     */
    public void recordTiming(String metricName, long duration) {
        timers.computeIfAbsent(metricName, k -> new AtomicLong(0)).addAndGet(duration);
    }
    
    /**
     * Sets a gauge value.
     * 
     * @param metricName the name of the gauge
     * @param value the value to set
     */
    public void setGauge(String metricName, long value) {
        gauges.put(metricName, value);
    }
    
    /**
     * Gets the current value of a counter.
     * 
     * @param metricName the name of the metric
     * @return the current value, or 0 if not found
     */
    public long getCounterValue(String metricName) {
        return counters.getOrDefault(metricName, new AtomicLong(0)).get();
    }
    
    /**
     * Gets the current value of a timing metric (total time).
     * 
     * @param metricName the name of the timing metric
     * @return the total accumulated time, or 0 if not found
     */
    public long getTimingValue(String metricName) {
        return timers.getOrDefault(metricName, new AtomicLong(0)).get();
    }
    
    /**
     * Gets the current value of a gauge.
     * 
     * @param metricName the name of the gauge
     * @return the current value, or 0 if not found
     */
    public long getGaugeValue(String metricName) {
        return gauges.getOrDefault(metricName, 0L);
    }
    
    /**
     * Gets all metrics as a map.
     * 
     * @return a map containing all metrics
     */
    public MetricsSnapshot getMetricsSnapshot() {
        Map<String, Long> currentCounters = new ConcurrentHashMap<>();
        counters.forEach((key, value) -> currentCounters.put(key, value.get()));
        
        Map<String, Long> currentTimers = new ConcurrentHashMap<>();
        timers.forEach((key, value) -> currentTimers.put(key, value.get()));
        
        return new MetricsSnapshot(currentCounters, currentTimers, new ConcurrentHashMap<>(gauges));
    }
    
    /**
     * Resets a specific counter.
     * 
     * @param metricName the name of the metric to reset
     */
    public void resetCounter(String metricName) {
        AtomicLong counter = counters.get(metricName);
        if (counter != null) {
            counter.set(0);
        }
    }
    
    /**
     * Resets all metrics.
     */
    public void resetAll() {
        counters.clear();
        timers.clear();
        gauges.clear();
    }
    
    /**
     * Metrics snapshot record for collecting all metrics at a point in time.
     */
    public record MetricsSnapshot(
        Map<String, Long> counters,
        Map<String, Long> timers,
        Map<String, Long> gauges
    ) {}
}