/**
 * API Optimization utilities for improved performance and caching
 */

import { createSelector } from '@reduxjs/toolkit'

import type { RootState } from '../store'

// Cache duration constants (in milliseconds)
export const CACHE_DURATIONS = {
  SHORT: 30 * 1000, // 30 seconds - for frequently changing data
  MEDIUM: 5 * 60 * 1000, // 5 minutes - for moderately changing data
  LONG: 30 * 60 * 1000, // 30 minutes - for rarely changing data
  STATIC: 24 * 60 * 60 * 1000, // 24 hours - for static data
} as const

// Cache tags for different data types
export const CACHE_TAGS = {
  User: 'User',
  Organization: 'Organization',
  Payment: 'Payment',
  PaymentStatistics: 'PaymentStatistics',
  PaymentMethod: 'PaymentMethod',
  Subscription: 'Subscription',
  SubscriptionPlan: 'SubscriptionPlan',
  Invoice: 'Invoice',
  SetupIntent: 'SetupIntent',
} as const

// Intelligent cache key generation
export const generateCacheKey = (
  endpoint: string,
  params?: Record<string, any>,
  userContext?: { userId?: string; organizationId?: string }
): string => {
  const baseKey = endpoint
  const paramString = params ? JSON.stringify(params) : ''
  const contextString = userContext
    ? `${userContext.userId}-${userContext.organizationId}`
    : ''

  return `${baseKey}:${paramString}:${contextString}`
}

// Enhanced selectors for memoized data access
export const createMemoizedSelector = <T>(
  inputSelectors: any[],
  resultFunc: (...args: any[]) => T
) => {
  return createSelector(inputSelectors, resultFunc)
}

// Optimized data selectors
export const selectCurrentUserWithOrganizations = createSelector(
  [(state: RootState) => state.auth.user],
  user => user
)

export const selectPrimaryOrganization = createSelector(
  [(state: RootState) => state.auth.user],
  user => (user ? null : null)
)

// Background data prefetching utility
export class DataPrefetcher {
  private static instance: DataPrefetcher
  private prefetchQueue = new Set<string>()
  private isProcessing = false

  static getInstance(): DataPrefetcher {
    if (!DataPrefetcher.instance) {
      DataPrefetcher.instance = new DataPrefetcher()
    }
    return DataPrefetcher.instance
  }

  addToPrefetchQueue(endpoint: string): void {
    this.prefetchQueue.add(endpoint)
    this.processPrefetchQueue()
  }

  private async processPrefetchQueue(): Promise<void> {
    if (this.isProcessing || this.prefetchQueue.size === 0) {
      return
    }

    this.isProcessing = true

    // Process prefetch queue with delay to avoid overwhelming the server
    const endpoints = Array.from(this.prefetchQueue)
    this.prefetchQueue.clear()

    for (const endpoint of endpoints) {
      try {
        // Add a small delay between requests
        await new Promise(resolve => setTimeout(resolve, 100))
        // The actual prefetching would be handled by RTK Query
        console.debug(`Prefetching data for: ${endpoint}`)
      } catch (error) {
        console.warn(`Prefetch failed for ${endpoint}:`, error)
      }
    }

    this.isProcessing = false
  }
}

// Smart retry logic
export const createRetryConfig = (maxRetries = 3) => ({
  maxRetries,
  retryCondition: (error: any) => {
    // Retry on network errors or 5xx server errors
    const status = error?.status || error?.response?.status
    return (
      !status || // Network error
      (status >= 500 && status <= 599) // Server error
    )
  },
  retryDelay: (retryAttempt: number) => {
    // Exponential backoff: 1s, 2s, 4s, etc.
    return Math.min(1000 * Math.pow(2, retryAttempt), 10000)
  },
})

// Request deduplication utility
class RequestDeduplicator {
  private static instance: RequestDeduplicator
  private pendingRequests = new Map<string, Promise<any>>()

  static getInstance(): RequestDeduplicator {
    if (!RequestDeduplicator.instance) {
      RequestDeduplicator.instance = new RequestDeduplicator()
    }
    return RequestDeduplicator.instance
  }

  deduplicate<T>(key: string, requestFn: () => Promise<T>): Promise<T> {
    if (this.pendingRequests.has(key)) {
      return this.pendingRequests.get(key)!
    }

    const promise = requestFn().finally(() => {
      this.pendingRequests.delete(key)
    })

    this.pendingRequests.set(key, promise)
    return promise
  }
}

export const requestDeduplicator = RequestDeduplicator.getInstance()

// Performance monitoring utility
export class PerformanceMonitor {
  private static instance: PerformanceMonitor
  private metrics = new Map<
    string,
    { count: number; totalTime: number; errors: number }
  >()

  static getInstance(): PerformanceMonitor {
    if (!PerformanceMonitor.instance) {
      PerformanceMonitor.instance = new PerformanceMonitor()
    }
    return PerformanceMonitor.instance
  }

  startTimer(operation: string): () => void {
    const startTime = performance.now()

    return () => {
      const endTime = performance.now()
      const duration = endTime - startTime
      this.recordMetric(operation, duration)
    }
  }

  recordError(operation: string): void {
    const metric = this.metrics.get(operation) || {
      count: 0,
      totalTime: 0,
      errors: 0,
    }
    metric.errors++
    this.metrics.set(operation, metric)
  }

  private recordMetric(operation: string, duration: number): void {
    const metric = this.metrics.get(operation) || {
      count: 0,
      totalTime: 0,
      errors: 0,
    }
    metric.count++
    metric.totalTime += duration
    this.metrics.set(operation, metric)
  }

  getMetrics(): Record<
    string,
    { avgTime: number; count: number; errorRate: number }
  > {
    const result: Record<
      string,
      { avgTime: number; count: number; errorRate: number }
    > = {}

    this.metrics.forEach((metric, operation) => {
      result[operation] = {
        avgTime: metric.totalTime / metric.count,
        count: metric.count,
        errorRate: metric.errors / metric.count,
      }
    })

    return result
  }

  reset(): void {
    this.metrics.clear()
  }
}

export const performanceMonitor = PerformanceMonitor.getInstance()

// Connection quality detection
export class ConnectionMonitor {
  private static instance: ConnectionMonitor
  private isOnline = navigator.onLine
  private connectionQuality: 'fast' | 'slow' | 'offline' = 'fast'
  private listeners: Array<(quality: 'fast' | 'slow' | 'offline') => void> = []

  static getInstance(): ConnectionMonitor {
    if (!ConnectionMonitor.instance) {
      ConnectionMonitor.instance = new ConnectionMonitor()
    }
    return ConnectionMonitor.instance
  }

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => {
        this.isOnline = true
        this.updateConnectionQuality()
      })

      window.addEventListener('offline', () => {
        this.isOnline = false
        this.connectionQuality = 'offline'
        this.notifyListeners()
      })

      // Monitor connection quality using performance timing
      this.monitorConnectionQuality()
    }
  }

  getConnectionQuality(): 'fast' | 'slow' | 'offline' {
    return this.connectionQuality
  }

  onConnectionChange(
    callback: (quality: 'fast' | 'slow' | 'offline') => void
  ): () => void {
    this.listeners.push(callback)
    return () => {
      const index = this.listeners.indexOf(callback)
      if (index > -1) {
        this.listeners.splice(index, 1)
      }
    }
  }

  private monitorConnectionQuality(): void {
    if (!this.isOnline) return

    const startTime = Date.now()

    // Make a small test request to gauge connection speed
    fetch('/api/health', { method: 'HEAD' })
      .then(() => {
        const duration = Date.now() - startTime
        const newQuality = duration > 2000 ? 'slow' : 'fast'

        if (newQuality !== this.connectionQuality) {
          this.connectionQuality = newQuality
          this.notifyListeners()
        }
      })
      .catch(() => {
        // If health check fails, consider connection slow
        this.connectionQuality = 'slow'
        this.notifyListeners()
      })

    // Monitor every 30 seconds
    setTimeout(() => this.monitorConnectionQuality(), 30000)
  }

  private updateConnectionQuality(): void {
    if (this.isOnline) {
      this.monitorConnectionQuality()
    }
  }

  private notifyListeners(): void {
    this.listeners.forEach(callback => callback(this.connectionQuality))
  }
}

export const connectionMonitor = ConnectionMonitor.getInstance()

// Adaptive loading strategies based on connection quality
export const getAdaptiveLoadingStrategy = () => {
  const quality = connectionMonitor.getConnectionQuality()

  switch (quality) {
    case 'offline':
      return {
        enablePrefetch: false,
        enableBackgroundRefresh: false,
        cachePolicy: 'cache-only',
        imageOptimization: 'aggressive',
      }
    case 'slow':
      return {
        enablePrefetch: false,
        enableBackgroundRefresh: false,
        cachePolicy: 'cache-first',
        imageOptimization: 'moderate',
      }
    case 'fast':
    default:
      return {
        enablePrefetch: true,
        enableBackgroundRefresh: true,
        cachePolicy: 'stale-while-revalidate',
        imageOptimization: 'minimal',
      }
  }
}

// Export utilities for easy access
export const dataPrefetcher = DataPrefetcher.getInstance()
