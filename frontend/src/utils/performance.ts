// Performance utilities for React components
import { lazy, Suspense, useState, useEffect, ComponentType } from 'react';
import React from 'react';

import { analyticsService } from '../services/analyticsService'

import { logger } from './logger'

// Type definitions
export type LazyComponent<T extends Record<string, unknown> = Record<string, never>> = ComponentType<T>;

// Performance API types
interface PerformanceEntryWithStartTime extends PerformanceEntry {
  startTime: number;
}

interface LargestContentfulPaintEntry extends PerformanceEntryWithStartTime {
  size: number;
  element?: Element;
}

interface FirstInputEntry extends PerformanceEntryWithStartTime {
  processingStart: number;
}

interface LayoutShiftEntry extends PerformanceEntryWithStartTime {
  value: number;
  hadRecentInput: boolean;
}

// Navigator connection types
interface NavigatorConnection {
  effectiveType?: string;
  type?: string;
  downlink?: number;
  rtt?: number;
}

interface NavigatorWithConnection extends Navigator {
  connection?: NavigatorConnection;
  mozConnection?: NavigatorConnection;
  webkitConnection?: NavigatorConnection;
}

// Lazy loading wrapper
export const lazyLoad = <T extends {}>(
  importFunc: () => Promise<{ default: ComponentType<T> }>
): ComponentType<T> => {
  const Component = lazy(importFunc);

  return (props: T) =>
    React.createElement(
      Suspense,
      {
        fallback: React.createElement('div', {
          className: 'animate-pulse bg-gray-200 h-32 rounded'
        })
      },
      React.createElement(Component, props)
    );
};

// Debounce hook for search/input
export const useDebounce = (value: string, delay: number): string => {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(handler);
  }, [value, delay]);

  return debouncedValue;
};

// Memoized API calls
export const memoizedFetch = (() => {
  const cache = new Map<string, unknown>();

  return async <T = unknown>(url: string, options?: RequestInit): Promise<T> => {
    const key = `${url}${JSON.stringify(options)}`;
    if (cache.has(key)) return cache.get(key) as T;

    const response = await fetch(url, options);
    const data = await response.json() as T;
    cache.set(key, data);
    return data;
  };
})();

// Performance monitoring
export const measurePerformance = (name: string, fn: () => void): void => {
  const start = performance.now();
  fn();
  const end = performance.now();
  console.log(`${name} took ${end - start} milliseconds`);
};

// Component performance wrapper
export const withPerformanceMonitoring = <T extends {}>(
  Component: ComponentType<T>,
  name: string
): ComponentType<T> => {
  return (props: T) => {
    useEffect(() => {
      const start = performance.now();
      return () => {
        const end = performance.now();
        console.log(`${name} render took ${end - start} milliseconds`);
      };
    });

    return React.createElement(Component, props);
  };
};

// ===== CORE WEB VITALS TRACKING SYSTEM =====

// Types for performance metrics
export interface CoreWebVitals {
  // Core Web Vitals
  cls?: number    // Cumulative Layout Shift
  fid?: number    // First Input Delay
  lcp?: number    // Largest Contentful Paint
  fcp?: number    // First Contentful Paint
  ttfb?: number   // Time to First Byte

  // Custom metrics
  timestamp: number
  url: string
  userAgent: string
  connectionType?: string
}

export interface PagePerformanceMetrics {
  pageLoadTime: number
  domContentLoaded: number
  resourcesLoaded: number
  interactionDelay: number
  renderTime: number
  timestamp: number
  url: string
}

export interface UserJourneyMetrics {
  journeyId: string
  startTime: number
  checkpoints: Array<{
    name: string
    timestamp: number
    duration: number
    metadata?: Record<string, any>
  }>
  totalDuration?: number
}

// Performance observer for Core Web Vitals
class WebVitalsTracker {
  private metrics: Partial<CoreWebVitals> = {}
  private observers: PerformanceObserver[] = []
  private reportCallback?: (metrics: CoreWebVitals) => void

  constructor(onReport?: (metrics: CoreWebVitals) => void) {
    this.reportCallback = onReport
    this.initializeObservers()
  }

  private initializeObservers() {
    // Largest Contentful Paint (LCP)
    if ('PerformanceObserver' in window) {
      try {
        const lcpObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries() as LargestContentfulPaintEntry[]
          const lastEntry = entries[entries.length - 1]
          this.metrics.lcp = lastEntry.startTime
          this.reportMetrics('lcp')
        })
        lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] })
        this.observers.push(lcpObserver)
      } catch (e) {
        logger.warn('LCP observer not supported', e)
      }

      // First Input Delay (FID)
      try {
        const fidObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries() as FirstInputEntry[]
          entries.forEach((entry) => {
            this.metrics.fid = entry.processingStart - entry.startTime
            this.reportMetrics('fid')
          })
        })
        fidObserver.observe({ entryTypes: ['first-input'] })
        this.observers.push(fidObserver)
      } catch (e) {
        logger.warn('FID observer not supported', e)
      }

      // Cumulative Layout Shift (CLS)
      try {
        let clsValue = 0
        const clsObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries() as LayoutShiftEntry[]
          entries.forEach((entry) => {
            if (!entry.hadRecentInput) {
              clsValue += entry.value
            }
          })
          this.metrics.cls = clsValue
          this.reportMetrics('cls')
        })
        clsObserver.observe({ entryTypes: ['layout-shift'] })
        this.observers.push(clsObserver)
      } catch (e) {
        logger.warn('CLS observer not supported', e)
      }

      // First Contentful Paint (FCP)
      try {
        const fcpObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries()
          entries.forEach((entry) => {
            if (entry.name === 'first-contentful-paint') {
              this.metrics.fcp = entry.startTime
              this.reportMetrics('fcp')
            }
          })
        })
        fcpObserver.observe({ entryTypes: ['paint'] })
        this.observers.push(fcpObserver)
      } catch (e) {
        logger.warn('FCP observer not supported', e)
      }
    }

    // Navigation timing for TTFB
    this.collectNavigationTiming()
  }

  private collectNavigationTiming() {
    if ('performance' in window && 'timing' in performance) {
      const timing = performance.timing
      this.metrics.ttfb = timing.responseStart - timing.navigationStart
      this.reportMetrics('ttfb')
    }
  }

  private reportMetrics(trigger: string) {
    const completeMetrics: CoreWebVitals = {
      ...this.metrics,
      timestamp: Date.now(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      connectionType: this.getConnectionType(),
    }

    logger.debug(`Web Vitals updated (${trigger}):`, completeMetrics)

    if (this.reportCallback) {
      this.reportCallback(completeMetrics)
    }
  }

  private getConnectionType(): string {
    const nav = navigator as NavigatorWithConnection
    const connection = nav.connection || nav.mozConnection || nav.webkitConnection
    return connection ? connection.effectiveType || 'unknown' : 'unknown'
  }

  public getCurrentMetrics(): CoreWebVitals {
    return {
      ...this.metrics,
      timestamp: Date.now(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      connectionType: this.getConnectionType(),
    }
  }

  public disconnect() {
    this.observers.forEach(observer => observer.disconnect())
    this.observers = []
  }
}

// Page performance tracking
export class PagePerformanceTracker {
  private startTime: number
  private metrics: Partial<PagePerformanceMetrics> = {}

  constructor() {
    this.startTime = performance.now()
    this.collectInitialMetrics()
  }

  private collectInitialMetrics() {
    // DOM Content Loaded timing
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', () => {
        this.metrics.domContentLoaded = performance.now() - this.startTime
      })
    } else {
      this.metrics.domContentLoaded = 0 // Already loaded
    }

    // Window load timing
    if (document.readyState !== 'complete') {
      window.addEventListener('load', () => {
        this.metrics.pageLoadTime = performance.now() - this.startTime
        this.collectResourceMetrics()
      })
    } else {
      this.metrics.pageLoadTime = 0
      this.collectResourceMetrics()
    }
  }

  private collectResourceMetrics() {
    if ('performance' in window) {
      const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[]
      const totalResourceTime = resources.reduce((total, resource) => {
        return total + (resource.responseEnd - resource.startTime)
      }, 0)
      this.metrics.resourcesLoaded = totalResourceTime
    }
  }

  public measureInteraction(callback: () => void): Promise<void> {
    return new Promise((resolve) => {
      const startTime = performance.now()

      callback()

      requestAnimationFrame(() => {
        this.metrics.interactionDelay = performance.now() - startTime
        resolve()
      })
    })
  }

  public measureRenderTime(renderCallback: () => void): Promise<void> {
    return new Promise((resolve) => {
      const startTime = performance.now()

      renderCallback()

      requestAnimationFrame(() => {
        this.metrics.renderTime = performance.now() - startTime
        resolve()
      })
    })
  }

  public getMetrics(): PagePerformanceMetrics {
    return {
      pageLoadTime: this.metrics.pageLoadTime || 0,
      domContentLoaded: this.metrics.domContentLoaded || 0,
      resourcesLoaded: this.metrics.resourcesLoaded || 0,
      interactionDelay: this.metrics.interactionDelay || 0,
      renderTime: this.metrics.renderTime || 0,
      timestamp: Date.now(),
      url: window.location.href,
    }
  }
}

// User journey tracking for complex workflows
export class UserJourneyTracker {
  private journeys = new Map<string, UserJourneyMetrics>()

  public startJourney(journeyId: string): void {
    const journey: UserJourneyMetrics = {
      journeyId,
      startTime: performance.now(),
      checkpoints: [],
    }
    this.journeys.set(journeyId, journey)
    logger.debug(`Started journey: ${journeyId}`)
  }

  public addCheckpoint(journeyId: string, name: string, metadata?: Record<string, any>): void {
    const journey = this.journeys.get(journeyId)
    if (!journey) {
      logger.warn(`Journey ${journeyId} not found for checkpoint ${name}`)
      return
    }

    const now = performance.now()
    const checkpoint = {
      name,
      timestamp: now,
      duration: now - journey.startTime,
      metadata,
    }

    journey.checkpoints.push(checkpoint)
    logger.debug(`Checkpoint added to ${journeyId}:`, checkpoint)
  }

  public completeJourney(journeyId: string): UserJourneyMetrics | null {
    const journey = this.journeys.get(journeyId)
    if (!journey) {
      logger.warn(`Journey ${journeyId} not found for completion`)
      return null
    }

    journey.totalDuration = performance.now() - journey.startTime
    this.journeys.delete(journeyId)

    logger.info(`Journey completed: ${journeyId}`, journey)
    return journey
  }

  public getActiveJourneys(): string[] {
    return Array.from(this.journeys.keys())
  }
}

// Performance monitoring service
class PerformanceMonitoringService {
  private webVitalsTracker: WebVitalsTracker
  private pageTracker: PagePerformanceTracker
  private journeyTracker: UserJourneyTracker
  private metricsQueue: Array<CoreWebVitals | PagePerformanceMetrics | UserJourneyMetrics> = []
  private reportingInterval: number | null = null

  constructor() {
    this.webVitalsTracker = new WebVitalsTracker((metrics) => {
      this.queueMetrics(metrics)
    })
    this.pageTracker = new PagePerformanceTracker()
    this.journeyTracker = new UserJourneyTracker()

    // Start periodic reporting
    this.startPeriodicReporting()
  }

  private queueMetrics(metrics: CoreWebVitals | PagePerformanceMetrics | UserJourneyMetrics) {
    this.metricsQueue.push(metrics)

    // Auto-flush if queue gets too large
    if (this.metricsQueue.length > 50) {
      this.flushMetrics()
    }
  }

  private startPeriodicReporting() {
    // Report metrics every 30 seconds
    this.reportingInterval = window.setInterval(() => {
      this.flushMetrics()
    }, 30000)
  }

  private async flushMetrics() {
    if (this.metricsQueue.length === 0) return

    const metricsToReport = [...this.metricsQueue]
    this.metricsQueue = []

    try {
      // In production, send to analytics service
      logger.info('Performance metrics batch:', {
        count: metricsToReport.length,
        metrics: metricsToReport,
      })

      // Send to analytics service
      await analyticsService.sendMetrics(metricsToReport)
    } catch (error) {
      logger.error('Failed to report performance metrics:', error)
      // Restore metrics to queue for retry
      this.metricsQueue.unshift(...metricsToReport)
    }
  }

  public measurePagePerformance(): PagePerformanceMetrics {
    const metrics = this.pageTracker.getMetrics()
    this.queueMetrics(metrics)
    return metrics
  }

  public measureInteraction(callback: () => void): Promise<void> {
    return this.pageTracker.measureInteraction(callback)
  }

  public measureRender(callback: () => void): Promise<void> {
    return this.pageTracker.measureRenderTime(callback)
  }

  public startUserJourney(journeyId: string): void {
    this.journeyTracker.startJourney(journeyId)
  }

  public addJourneyCheckpoint(journeyId: string, name: string, metadata?: Record<string, any>): void {
    this.journeyTracker.addCheckpoint(journeyId, name, metadata)
  }

  public completeUserJourney(journeyId: string): UserJourneyMetrics | null {
    const journey = this.journeyTracker.completeJourney(journeyId)
    if (journey) {
      this.queueMetrics(journey)
    }
    return journey
  }

  public getCurrentWebVitals(): CoreWebVitals {
    return this.webVitalsTracker.getCurrentMetrics()
  }

  public destroy() {
    this.webVitalsTracker.disconnect()
    if (this.reportingInterval) {
      clearInterval(this.reportingInterval)
    }
    this.flushMetrics()
  }
}

// Global performance monitoring instance
export const performanceMonitor = new PerformanceMonitoringService()

// Convenience functions for React components
export const usePerformanceTracking = () => {
  return {
    measurePageLoad: () => performanceMonitor.measurePagePerformance(),
    measureInteraction: (callback: () => void) => performanceMonitor.measureInteraction(callback),
    measureRender: (callback: () => void) => performanceMonitor.measureRender(callback),
    startJourney: (id: string) => performanceMonitor.startUserJourney(id),
    addCheckpoint: (id: string, name: string, metadata?: Record<string, any>) =>
      performanceMonitor.addJourneyCheckpoint(id, name, metadata),
    completeJourney: (id: string) => performanceMonitor.completeUserJourney(id),
    getWebVitals: () => performanceMonitor.getCurrentWebVitals(),
  }
}

// React hook for automatic page performance tracking
export const usePagePerformance = (pageName: string) => {
  useEffect(() => {
    const metrics = performanceMonitor.measurePagePerformance()
    logger.info(`Page performance for ${pageName}:`, metrics)

    return () => {
      // Final measurement on unmount
      const finalMetrics = performanceMonitor.measurePagePerformance()
      logger.info(`Final page performance for ${pageName}:`, finalMetrics)
    }
  }, [pageName])
}

// Auto-initialize performance tracking
if (typeof window !== 'undefined') {
  // Clean up on page unload
  window.addEventListener('beforeunload', () => {
    performanceMonitor.destroy()
  })
}
