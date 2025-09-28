import { useEffect, useRef, useCallback, useState } from 'react';

interface PerformanceMetrics {
  renderTime: number;
  componentMountTime: number;
  updateCount: number;
  lastRenderTime: number;
  averageRenderTime: number;
  memoryUsage?: number;
}

/**
 * Hook for monitoring React component performance metrics.
 * Provides insights into render times, update frequency, and memory usage.
 */
export const usePerformanceMonitor = (componentName: string, enabled = process.env.NODE_ENV === 'development') => {
  const [metrics, setMetrics] = useState<PerformanceMetrics>({
    renderTime: 0,
    componentMountTime: 0,
    updateCount: 0,
    lastRenderTime: 0,
    averageRenderTime: 0,
  });

  const mountTimeRef = useRef<number>();
  const renderStartRef = useRef<number>();
  const renderTimesRef = useRef<number[]>([]);
  const updateCountRef = useRef(0);

  // Start performance measurement
  const startMeasurement = useCallback(() => {
    if (!enabled) return;
    renderStartRef.current = performance.now();
  }, [enabled]);

  // End performance measurement
  const endMeasurement = useCallback(() => {
    if (!enabled || !renderStartRef.current) return;

    const renderTime = performance.now() - renderStartRef.current;
    renderTimesRef.current.push(renderTime);
    updateCountRef.current += 1;

    // Keep only last 100 measurements to calculate average
    if (renderTimesRef.current.length > 100) {
      renderTimesRef.current.shift();
    }

    const averageRenderTime = renderTimesRef.current.reduce((sum, time) => sum + time, 0) / renderTimesRef.current.length;

    setMetrics(prev => ({
      ...prev,
      renderTime,
      lastRenderTime: renderTime,
      averageRenderTime,
      updateCount: updateCountRef.current,
    }));

    // Log performance warnings
    if (renderTime > 16) { // 16ms = 60fps threshold
      console.warn(
        `[Performance] ${componentName} render took ${renderTime.toFixed(2)}ms (> 16ms threshold)`
      );
    }
  }, [enabled, componentName]);

  // Memory usage monitoring
  const measureMemoryUsage = useCallback(() => {
    if (!enabled || !(performance as any).memory) return;

    const memory = (performance as any).memory;
    const memoryUsage = memory.usedJSHeapSize / 1024 / 1024; // Convert to MB

    setMetrics(prev => ({
      ...prev,
      memoryUsage,
    }));
  }, [enabled]);

  // Component mount time measurement
  useEffect(() => {
    if (!enabled) return;

    mountTimeRef.current = performance.now();

    return () => {
      if (mountTimeRef.current) {
        const mountTime = performance.now() - mountTimeRef.current;
        setMetrics(prev => ({
          ...prev,
          componentMountTime: mountTime,
        }));
      }
    };
  }, [enabled]);

  // Periodic memory measurement
  useEffect(() => {
    if (!enabled) return;

    const interval = setInterval(measureMemoryUsage, 5000); // Every 5 seconds
    return () => clearInterval(interval);
  }, [enabled, measureMemoryUsage]);

  return {
    metrics,
    startMeasurement,
    endMeasurement,
    logMetrics: () => {
      if (enabled) {
        console.group(`[Performance] ${componentName} Metrics`);
        console.table(metrics);
        console.groupEnd();
      }
    },
  };
};

/**
 * Higher-order component for automatic performance monitoring.
 */
export function withPerformanceMonitor<P extends object>(
  WrappedComponent: React.ComponentType<P>,
  componentName?: string
) {
  const displayName = componentName || WrappedComponent.displayName || WrappedComponent.name || 'Component';

  const PerformanceMonitoredComponent = (props: P) => {
    const { startMeasurement, endMeasurement, metrics, logMetrics } = usePerformanceMonitor(displayName);

    useEffect(() => {
      startMeasurement();
      return endMeasurement;
    });

    // Log metrics on unmount in development
    useEffect(() => {
      return () => {
        if (process.env.NODE_ENV === 'development') {
          logMetrics();
        }
      };
    }, [logMetrics]);

    return <WrappedComponent {...props} />;
  };

  PerformanceMonitoredComponent.displayName = `withPerformanceMonitor(${displayName})`;

  return PerformanceMonitoredComponent;
}

/**
 * Hook for monitoring Core Web Vitals.
 */
export const useWebVitals = () => {
  const [vitals, setVitals] = useState<{
    FCP?: number;
    LCP?: number;
    CLS?: number;
    FID?: number;
    TTFB?: number;
  }>({});

  useEffect(() => {
    // Dynamically import web-vitals to avoid bundling it in production if not needed
    import('web-vitals').then(({ onFCP, onLCP, onCLS, onFID, onTTFB }) => {
      onFCP((metric) => setVitals(prev => ({ ...prev, FCP: metric.value })));
      onLCP((metric) => setVitals(prev => ({ ...prev, LCP: metric.value })));
      onCLS((metric) => setVitals(prev => ({ ...prev, CLS: metric.value })));
      onFID((metric) => setVitals(prev => ({ ...prev, FID: metric.value })));
      onTTFB((metric) => setVitals(prev => ({ ...prev, TTFB: metric.value })));
    }).catch(() => {
      // web-vitals not available, skip
    });
  }, []);

  const reportVitals = useCallback(() => {
    // Send vitals to analytics service
    if (typeof window !== 'undefined' && window.gtag) {
      Object.entries(vitals).forEach(([name, value]) => {
        if (value !== undefined) {
          window.gtag('event', name, {
            event_category: 'Web Vitals',
            value: Math.round(name === 'CLS' ? value * 1000 : value),
          });
        }
      });
    }
  }, [vitals]);

  return { vitals, reportVitals };
};

/**
 * Hook for monitoring bundle size and loading performance.
 */
export const useBundlePerformance = () => {
  const [bundleMetrics, setBundleMetrics] = useState<{
    jsSize: number;
    cssSize: number;
    totalSize: number;
    loadTime: number;
    resourceTimings: PerformanceResourceTiming[];
  }>({
    jsSize: 0,
    cssSize: 0,
    totalSize: 0,
    loadTime: 0,
    resourceTimings: [],
  });

  useEffect(() => {
    const calculateBundleSize = () => {
      const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[];

      let jsSize = 0;
      let cssSize = 0;
      const relevantResources: PerformanceResourceTiming[] = [];

      resources.forEach(resource => {
        if (resource.name.includes('.js')) {
          jsSize += resource.transferSize || 0;
          relevantResources.push(resource);
        } else if (resource.name.includes('.css')) {
          cssSize += resource.transferSize || 0;
          relevantResources.push(resource);
        }
      });

      const totalSize = jsSize + cssSize;
      const loadTime = performance.timing.loadEventEnd - performance.timing.navigationStart;

      setBundleMetrics({
        jsSize,
        cssSize,
        totalSize,
        loadTime,
        resourceTimings: relevantResources,
      });
    };

    // Wait for page load to complete
    if (document.readyState === 'complete') {
      calculateBundleSize();
    } else {
      window.addEventListener('load', calculateBundleSize);
      return () => window.removeEventListener('load', calculateBundleSize);
    }
  }, []);

  const formatSize = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  };

  return {
    bundleMetrics: {
      ...bundleMetrics,
      jsSizeFormatted: formatSize(bundleMetrics.jsSize),
      cssSizeFormatted: formatSize(bundleMetrics.cssSize),
      totalSizeFormatted: formatSize(bundleMetrics.totalSize),
    },
  };
};