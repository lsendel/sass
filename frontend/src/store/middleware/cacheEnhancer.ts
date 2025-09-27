/**
 * Enhanced RTK Query Cache Management
 *
 * Advanced caching strategies for optimal performance:
 * - Intelligent cache invalidation
 * - Predictive prefetching
 * - Background refresh strategies
 * - Cache analytics and optimization
 * - Memory pressure management
 */

import { createListenerMiddleware, isAnyOf } from '@reduxjs/toolkit'
import type { AnyAction, Middleware } from '@reduxjs/toolkit'

import { logger } from '../../utils/logger'
import { performanceMonitor } from '../../utils/performance'

// Types for cache management
export interface CachePolicy {
  maxAge?: number           // Cache lifetime in milliseconds
  staleTime?: number        // Time before data becomes stale
  gcTime?: number           // Time before garbage collection
  refetchOnFocus?: boolean  // Refetch when window gains focus
  refetchOnReconnect?: boolean  // Refetch when network reconnects
  backgroundRefresh?: boolean   // Enable background refresh
  prefetchRelated?: string[]    // Related endpoints to prefetch
}

export interface CacheMetrics {
  endpoint: string
  hitCount: number
  missCount: number
  totalQueries: number
  avgResponseTime: number
  lastAccessed: number
  cacheSize: number
}

export interface CacheStrategy {
  name: string
  apply: (endpoint: string, data: any) => CachePolicy
}

// Cache strategy implementations
const cacheStrategies: CacheStrategy[] = [
  // Static data strategy - long cache times
  {
    name: 'static',
    apply: (endpoint) => {
      if (endpoint.includes('auth/methods') || endpoint.includes('config')) {
        return {
          maxAge: 30 * 60 * 1000, // 30 minutes
          staleTime: 15 * 60 * 1000, // 15 minutes
          refetchOnFocus: false,
          backgroundRefresh: false
        }
      }
      return {}
    }
  },

  // User data strategy - moderate cache times with focus refresh
  {
    name: 'user-data',
    apply: (endpoint) => {
      if (endpoint.includes('user') || endpoint.includes('profile')) {
        return {
          maxAge: 5 * 60 * 1000,  // 5 minutes
          staleTime: 2 * 60 * 1000, // 2 minutes
          refetchOnFocus: true,
          refetchOnReconnect: true,
          backgroundRefresh: true
        }
      }
      return {}
    }
  },

  // Financial data strategy - short cache times with immediate updates
  {
    name: 'financial',
    apply: (endpoint) => {
      if (endpoint.includes('payment') || endpoint.includes('subscription') || endpoint.includes('billing')) {
        return {
          maxAge: 60 * 1000,      // 1 minute
          staleTime: 30 * 1000,   // 30 seconds
          refetchOnFocus: true,
          refetchOnReconnect: true,
          backgroundRefresh: true,
          prefetchRelated: ['statistics', 'history']
        }
      }
      return {}
    }
  },

  // Organization data strategy - moderate cache with team focus
  {
    name: 'organization',
    apply: (endpoint) => {
      if (endpoint.includes('organization') || endpoint.includes('member')) {
        return {
          maxAge: 3 * 60 * 1000,  // 3 minutes
          staleTime: 90 * 1000,   // 90 seconds
          refetchOnFocus: true,
          backgroundRefresh: true,
          prefetchRelated: ['members', 'settings']
        }
      }
      return {}
    }
  },

  // Analytics data strategy - longer cache for heavy computations
  {
    name: 'analytics',
    apply: (endpoint) => {
      if (endpoint.includes('analytics') || endpoint.includes('statistics') || endpoint.includes('report')) {
        return {
          maxAge: 10 * 60 * 1000, // 10 minutes
          staleTime: 5 * 60 * 1000, // 5 minutes
          refetchOnFocus: false,
          backgroundRefresh: true
        }
      }
      return {}
    }
  }
]

// Cache analytics tracker
class CacheAnalytics {
  private metrics = new Map<string, CacheMetrics>()
  private queryTimes = new Map<string, number>()

  public recordQueryStart(endpoint: string): void {
    this.queryTimes.set(endpoint, performance.now())
  }

  public recordQueryComplete(endpoint: string, wasHit: boolean): void {
    const startTime = this.queryTimes.get(endpoint)
    const responseTime = startTime ? performance.now() - startTime : 0

    const existing = this.metrics.get(endpoint) || {
      endpoint,
      hitCount: 0,
      missCount: 0,
      totalQueries: 0,
      avgResponseTime: 0,
      lastAccessed: Date.now(),
      cacheSize: 0
    }

    const newMetrics: CacheMetrics = {
      ...existing,
      hitCount: wasHit ? existing.hitCount + 1 : existing.hitCount,
      missCount: !wasHit ? existing.missCount + 1 : existing.missCount,
      totalQueries: existing.totalQueries + 1,
      avgResponseTime: (existing.avgResponseTime + responseTime) / 2,
      lastAccessed: Date.now()
    }

    this.metrics.set(endpoint, newMetrics)
    this.queryTimes.delete(endpoint)
  }

  public getMetrics(): CacheMetrics[] {
    return Array.from(this.metrics.values())
  }

  public getHitRate(endpoint?: string): number {
    if (endpoint) {
      const metric = this.metrics.get(endpoint)
      if (!metric || metric.totalQueries === 0) return 0
      return metric.hitCount / metric.totalQueries
    }

    // Overall hit rate
    let totalHits = 0
    let totalQueries = 0
    this.metrics.forEach(metric => {
      totalHits += metric.hitCount
      totalQueries += metric.totalQueries
    })

    return totalQueries > 0 ? totalHits / totalQueries : 0
  }

  public getRecommendations(): string[] {
    const recommendations: string[] = []
    const overallHitRate = this.getHitRate()

    if (overallHitRate < 0.7) {
      recommendations.push('Consider increasing cache times for frequently accessed endpoints')
    }

    this.metrics.forEach(metric => {
      const hitRate = metric.hitCount / metric.totalQueries

      if (hitRate < 0.5 && metric.totalQueries > 10) {
        recommendations.push(`Low hit rate for ${metric.endpoint} (${(hitRate * 100).toFixed(1)}%)`)
      }

      if (metric.avgResponseTime > 1000) {
        recommendations.push(`Slow response time for ${metric.endpoint} (${metric.avgResponseTime.toFixed(0)}ms)`)
      }
    })

    return recommendations
  }
}

// Memory pressure manager
class MemoryPressureManager {
  private readonly maxCacheSize = 50 * 1024 * 1024 // 50MB
  private readonly warningThreshold = 0.8

  public shouldEvictCache(): boolean {
    if ('memory' in performance && 'usedJSHeapSize' in (performance as any).memory) {
      const memory = (performance as any).memory
      const usageRatio = memory.usedJSHeapSize / memory.totalJSHeapSize
      return usageRatio > this.warningThreshold
    }

    // Fallback to simple heuristic
    const cacheCount = this.estimateCacheCount()
    return cacheCount > 1000
  }

  private estimateCacheCount(): number {
    // Simple estimation based on localStorage usage (fallback)
    try {
      let totalSize = 0
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key?.startsWith('rtk-query-')) {
          totalSize += localStorage.getItem(key)?.length || 0
        }
      }
      return totalSize / 1000 // Rough estimate of cache entries
    } catch {
      return 0
    }
  }

  public getMemoryStats() {
    if ('memory' in performance && 'usedJSHeapSize' in (performance as any).memory) {
      const memory = (performance as any).memory
      return {
        used: memory.usedJSHeapSize,
        total: memory.totalJSHeapSize,
        limit: memory.jsHeapSizeLimit,
        usagePercent: (memory.usedJSHeapSize / memory.totalJSHeapSize) * 100
      }
    }

    return null
  }
}

// Background refresh manager
class BackgroundRefreshManager {
  private refreshQueue = new Set<string>()
  private refreshInterval: number | null = null
  private isRefreshing = false

  public queueRefresh(endpoint: string, priority: 'high' | 'medium' | 'low' = 'medium'): void {
    this.refreshQueue.add(endpoint)

    if (priority === 'high' && !this.isRefreshing) {
      this.processQueue()
    }
  }

  public startBackgroundRefresh(): void {
    if (this.refreshInterval) return

    this.refreshInterval = window.setInterval(() => {
      if (this.refreshQueue.size > 0 && !this.isRefreshing) {
        this.processQueue()
      }
    }, 30000) // Process queue every 30 seconds
  }

  public stopBackgroundRefresh(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval)
      this.refreshInterval = null
    }
  }

  private async processQueue(): Promise<void> {
    if (this.isRefreshing || this.refreshQueue.size === 0) return

    this.isRefreshing = true
    const endpointsToRefresh = Array.from(this.refreshQueue)
    this.refreshQueue.clear()

    logger.info(`Processing background refresh queue: ${endpointsToRefresh.length} endpoints`)

    // Process refreshes with throttling
    for (const endpoint of endpointsToRefresh) {
      try {
        // Actual refresh would be handled by RTK Query
        await this.refreshEndpoint(endpoint)
        await new Promise(resolve => setTimeout(resolve, 100)) // Throttle requests
      } catch (error) {
        logger.error(`Background refresh failed for ${endpoint}:`, error)
      }
    }

    this.isRefreshing = false
  }

  private async refreshEndpoint(endpoint: string): Promise<void> {
    // This would integrate with RTK Query's invalidatesTags or refetch mechanisms
    logger.debug(`Background refreshing: ${endpoint}`)

    // Performance tracking for background refreshes
    performanceMonitor.addJourneyCheckpoint('cache-management', 'background-refresh', {
      endpoint
    })
  }
}

// Main cache enhancer
export class CacheEnhancer {
  private analytics: CacheAnalytics
  private memoryManager: MemoryPressureManager
  private backgroundRefresh: BackgroundRefreshManager
  private policies = new Map<string, CachePolicy>()

  constructor() {
    this.analytics = new CacheAnalytics()
    this.memoryManager = new MemoryPressureManager()
    this.backgroundRefresh = new BackgroundRefreshManager()

    this.initializeEnhancer()
  }

  private initializeEnhancer(): void {
    // Start background refresh
    this.backgroundRefresh.startBackgroundRefresh()

    // Memory pressure monitoring
    setInterval(() => {
      if (this.memoryManager.shouldEvictCache()) {
        logger.warn('Memory pressure detected - considering cache eviction')
        this.handleMemoryPressure()
      }
    }, 60000) // Check every minute

    // Periodic analytics reporting
    setInterval(() => {
      this.reportAnalytics()
    }, 5 * 60 * 1000) // Report every 5 minutes
  }

  public getCachePolicy(endpoint: string): CachePolicy {
    // Check if we have a cached policy
    if (this.policies.has(endpoint)) {
      return this.policies.get(endpoint)!
    }

    // Apply cache strategies
    let policy: CachePolicy = {
      maxAge: 60 * 1000, // Default 1 minute
      staleTime: 30 * 1000, // Default 30 seconds
      refetchOnFocus: true,
      refetchOnReconnect: true,
      backgroundRefresh: false
    }

    // Apply strategies in order
    for (const strategy of cacheStrategies) {
      const strategyPolicy = strategy.apply(endpoint, null)
      policy = { ...policy, ...strategyPolicy }
    }

    // Cache the policy
    this.policies.set(endpoint, policy)
    logger.debug(`Cache policy for ${endpoint}:`, policy)

    return policy
  }

  public recordQuery(endpoint: string, wasHit: boolean): void {
    this.analytics.recordQueryComplete(endpoint, wasHit)

    // Queue background refresh if needed
    const policy = this.getCachePolicy(endpoint)
    if (policy.backgroundRefresh && !wasHit) {
      this.backgroundRefresh.queueRefresh(endpoint, 'medium')
    }
  }

  private handleMemoryPressure(): void {
    // Get memory stats
    const memoryStats = this.memoryManager.getMemoryStats()
    if (memoryStats) {
      logger.info('Memory stats:', memoryStats)
    }

    // Get least recently used endpoints for eviction
    const metrics = this.analytics.getMetrics()
    const candidatesForEviction = metrics
      .sort((a, b) => a.lastAccessed - b.lastAccessed)
      .slice(0, Math.floor(metrics.length * 0.3)) // Evict oldest 30%

    logger.info('Cache eviction candidates:', candidatesForEviction.map(m => m.endpoint))

    // Actual eviction would be handled by RTK Query
    performanceMonitor.addJourneyCheckpoint('cache-management', 'memory-pressure', {
      candidatesCount: candidatesForEviction.length,
      totalMetrics: metrics.length
    })
  }

  private reportAnalytics(): void {
    const hitRate = this.analytics.getHitRate()
    const recommendations = this.analytics.getRecommendations()
    const metrics = this.analytics.getMetrics()

    logger.info('Cache Analytics Report:', {
      overallHitRate: (hitRate * 100).toFixed(1) + '%',
      totalEndpoints: metrics.length,
      recommendations: recommendations.length
    })

    if (recommendations.length > 0) {
      logger.info('Cache Optimization Recommendations:', recommendations)
    }

    // Send to performance monitoring
    performanceMonitor.addJourneyCheckpoint('cache-management', 'analytics-report', {
      hitRate,
      endpointCount: metrics.length,
      recommendationCount: recommendations.length
    })
  }

  public getAnalytics() {
    return {
      hitRate: this.analytics.getHitRate(),
      metrics: this.analytics.getMetrics(),
      recommendations: this.analytics.getRecommendations(),
      memoryStats: this.memoryManager.getMemoryStats()
    }
  }

  public destroy(): void {
    this.backgroundRefresh.stopBackgroundRefresh()
  }
}

// Global cache enhancer instance
export const cacheEnhancer = new CacheEnhancer()

// RTK Query middleware factory
export const createCacheEnhancerMiddleware = (): Middleware => {
  const listenerMiddleware = createListenerMiddleware()

  // Listen for RTK Query actions
  listenerMiddleware.startListening({
    matcher: isAnyOf(
      // Add matchers for RTK Query actions
      // This would be configured with actual RTK Query action creators
    ),
    effect: async (action: AnyAction, listenerApi) => {
      const { type, meta } = action

      if (type.endsWith('/pending')) {
        // Query started
        const endpoint = meta?.arg?.endpointName
        if (endpoint) {
          cacheEnhancer.analytics.recordQueryStart(endpoint)
        }
      }

      if (type.endsWith('/fulfilled') || type.endsWith('/rejected')) {
        // Query completed
        const endpoint = meta?.arg?.endpointName
        const fromCache = meta?.arg?.originalArgs?.forceRefetch === false

        if (endpoint) {
          cacheEnhancer.recordQuery(endpoint, fromCache)
        }
      }
    }
  })

  return listenerMiddleware.middleware
}

// React hooks for cache management
export const useCacheAnalytics = () => {
  const [analytics, setAnalytics] = React.useState(cacheEnhancer.getAnalytics())

  React.useEffect(() => {
    const interval = setInterval(() => {
      setAnalytics(cacheEnhancer.getAnalytics())
    }, 5000) // Update every 5 seconds

    return () => clearInterval(interval)
  }, [])

  return analytics
}

export const useCachePolicy = (endpoint: string) => {
  return React.useMemo(() => {
    return cacheEnhancer.getCachePolicy(endpoint)
  }, [endpoint])
}

// Auto-initialize cache enhancer
if (typeof window !== 'undefined') {
  // Clean up on page unload
  window.addEventListener('beforeunload', () => {
    cacheEnhancer.destroy()
  })
}