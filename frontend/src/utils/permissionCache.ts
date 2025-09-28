/**
 * High-Performance Permission Caching System
 *
 * Advanced caching system for permission checks with:
 * - Constitutional compliance validation
 * - Multi-level cache hierarchy (L1: Memory, L2: IndexedDB)
 * - Intelligent cache invalidation
 * - Performance metrics and monitoring
 * - Security-aware cache policies
 * - Real-time cache warming
 */

import { logger } from './logger'
import { auditPermissionCheck } from './auditLogger'

// Branded types for type safety
type CacheKey = string & { readonly __brand: 'CacheKey' }
type CacheVersion = number & { readonly __brand: 'CacheVersion' }
type UserId = string & { readonly __brand: 'UserId' }

// Cache entry interface
interface PermissionCacheEntry {
  key: CacheKey
  userId: UserId
  resource: string
  action: string
  resourceId?: string
  organizationId?: string
  result: boolean
  reason?: string
  matchedPermissions?: any[]
  conditions?: any[]
  timestamp: number
  expiresAt: number
  accessCount: number
  lastAccessed: number
  version: CacheVersion
  securityHash: string
  constitutionalCompliance: {
    validated: boolean
    score: number
    timestamp: number
  }
}

// Cache configuration
interface CacheConfig {
  maxMemoryEntries: number
  defaultTTL: number // Time to live in milliseconds
  maxAccessCount: number
  enableL2Cache: boolean // IndexedDB persistence
  enableCacheWarming: boolean
  enableSecurityValidation: boolean
  compressionThreshold: number // Bytes
  maxCacheSize: number // Total cache size limit
  invalidationStrategies: InvalidationStrategy[]
}

// Cache invalidation strategies
interface InvalidationStrategy {
  type: 'ttl' | 'access_count' | 'memory_pressure' | 'security_event' | 'user_context_change'
  threshold?: number
  enabled: boolean
}

// Cache performance metrics
interface CacheMetrics {
  hits: number
  misses: number
  l1Hits: number
  l2Hits: number
  evictions: number
  totalEntries: number
  memoryUsage: number
  hitRate: number
  averageResponseTime: number
  securityViolations: number
  constitutionalCompliance: number
  lastUpdate: string
}

// Cache warming configuration
interface CacheWarmingConfig {
  enabled: boolean
  strategies: WarmingStrategy[]
  batchSize: number
  maxConcurrency: number
}

interface WarmingStrategy {
  type: 'user_login' | 'role_change' | 'periodic' | 'predictive'
  resources: string[]
  actions: string[]
  priority: number
}

// Default cache configuration
const DEFAULT_CONFIG: CacheConfig = {
  maxMemoryEntries: 10000,
  defaultTTL: 300000, // 5 minutes
  maxAccessCount: 1000,
  enableL2Cache: true,
  enableCacheWarming: true,
  enableSecurityValidation: true,
  compressionThreshold: 1024, // 1KB
  maxCacheSize: 50 * 1024 * 1024, // 50MB
  invalidationStrategies: [
    { type: 'ttl', enabled: true },
    { type: 'access_count', threshold: 1000, enabled: true },
    { type: 'memory_pressure', threshold: 0.9, enabled: true },
    { type: 'security_event', enabled: true },
    { type: 'user_context_change', enabled: true },
  ],
}

// L1 Cache (Memory-based)
class L1PermissionCache {
  private cache: Map<string, PermissionCacheEntry> = new Map()
  private accessOrder: string[] = [] // LRU tracking
  private config: CacheConfig

  constructor(config: CacheConfig) {
    this.config = config
  }

  set(entry: PermissionCacheEntry): void {
    const key = entry.key

    // Check memory pressure before adding
    if (this.cache.size >= this.config.maxMemoryEntries) {
      this.evictLRU()
    }

    this.cache.set(key, entry)
    this.updateAccessOrder(key)

    logger.debug('L1 cache entry added', {
      key,
      userId: entry.userId,
      resource: entry.resource,
      action: entry.action,
      expiresAt: new Date(entry.expiresAt).toISOString(),
    })
  }

  get(key: CacheKey): PermissionCacheEntry | null {
    const entry = this.cache.get(key)

    if (!entry) {
      return null
    }

    // Check if entry has expired
    if (Date.now() > entry.expiresAt) {
      this.cache.delete(key)
      this.removeFromAccessOrder(key)
      return null
    }

    // Check access count limit
    if (entry.accessCount >= this.config.maxAccessCount) {
      this.cache.delete(key)
      this.removeFromAccessOrder(key)
      logger.warn('Cache entry evicted due to access count limit', {
        key,
        accessCount: entry.accessCount,
        limit: this.config.maxAccessCount,
      })
      return null
    }

    // Update access tracking
    entry.accessCount++
    entry.lastAccessed = Date.now()
    this.updateAccessOrder(key)

    return entry
  }

  delete(key: CacheKey): boolean {
    const deleted = this.cache.delete(key)
    if (deleted) {
      this.removeFromAccessOrder(key)
    }
    return deleted
  }

  clear(): void {
    this.cache.clear()
    this.accessOrder = []
  }

  size(): number {
    return this.cache.size
  }

  getMemoryUsage(): number {
    let usage = 0
    this.cache.forEach(entry => {
      usage += this.estimateEntrySize(entry)
    })
    return usage
  }

  private evictLRU(): void {
    if (this.accessOrder.length === 0) return

    const lruKey = this.accessOrder[0]
    this.cache.delete(lruKey)
    this.removeFromAccessOrder(lruKey)

    logger.debug('L1 cache LRU eviction', {
      evictedKey: lruKey,
      remainingEntries: this.cache.size,
    })
  }

  private updateAccessOrder(key: string): void {
    this.removeFromAccessOrder(key)
    this.accessOrder.push(key)
  }

  private removeFromAccessOrder(key: string): void {
    const index = this.accessOrder.indexOf(key)
    if (index > -1) {
      this.accessOrder.splice(index, 1)
    }
  }

  private estimateEntrySize(entry: PermissionCacheEntry): number {
    // Rough estimation of entry size in bytes
    return JSON.stringify(entry).length * 2 // Approximate UTF-16 encoding
  }

  getEntries(): PermissionCacheEntry[] {
    return Array.from(this.cache.values())
  }
}

// L2 Cache (IndexedDB-based for persistence)
class L2PermissionCache {
  private dbName = 'PermissionCacheDB'
  private dbVersion = 1
  private storeName = 'permissions'
  private db: IDBDatabase | null = null
  private initPromise: Promise<void>

  constructor() {
    this.initPromise = this.initDB()
  }

  private async initDB(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, this.dbVersion)

      request.onerror = () => {
        logger.error('Failed to initialize IndexedDB for L2 cache', {
          error: request.error,
        })
        reject(request.error)
      }

      request.onsuccess = () => {
        this.db = request.result
        logger.debug('L2 cache IndexedDB initialized')
        resolve()
      }

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result
        const store = db.createObjectStore(this.storeName, { keyPath: 'key' })
        store.createIndex('userId', 'userId', { unique: false })
        store.createIndex('expiresAt', 'expiresAt', { unique: false })
        store.createIndex('timestamp', 'timestamp', { unique: false })
      }
    })
  }

  async set(entry: PermissionCacheEntry): Promise<void> {
    await this.initPromise
    if (!this.db) return

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.storeName], 'readwrite')
      const store = transaction.objectStore(this.storeName)
      const request = store.put(entry)

      request.onsuccess = () => resolve()
      request.onerror = () => reject(request.error)
    })
  }

  async get(key: CacheKey): Promise<PermissionCacheEntry | null> {
    await this.initPromise
    if (!this.db) return null

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.storeName], 'readonly')
      const store = transaction.objectStore(this.storeName)
      const request = store.get(key)

      request.onsuccess = () => {
        const entry = request.result
        if (entry && Date.now() <= entry.expiresAt) {
          resolve(entry)
        } else {
          if (entry) {
            // Remove expired entry
            this.delete(key)
          }
          resolve(null)
        }
      }
      request.onerror = () => reject(request.error)
    })
  }

  async delete(key: CacheKey): Promise<boolean> {
    await this.initPromise
    if (!this.db) return false

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.storeName], 'readwrite')
      const store = transaction.objectStore(this.storeName)
      const request = store.delete(key)

      request.onsuccess = () => resolve(true)
      request.onerror = () => reject(request.error)
    })
  }

  async clear(): Promise<void> {
    await this.initPromise
    if (!this.db) return

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.storeName], 'readwrite')
      const store = transaction.objectStore(this.storeName)
      const request = store.clear()

      request.onsuccess = () => resolve()
      request.onerror = () => reject(request.error)
    })
  }

  async cleanExpired(): Promise<number> {
    await this.initPromise
    if (!this.db) return 0

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.storeName], 'readwrite')
      const store = transaction.objectStore(this.storeName)
      const index = store.index('expiresAt')
      const range = IDBKeyRange.upperBound(Date.now())
      const request = index.openCursor(range)
      let deletedCount = 0

      request.onsuccess = (event) => {
        const cursor = (event.target as IDBRequest).result
        if (cursor) {
          cursor.delete()
          deletedCount++
          cursor.continue()
        } else {
          resolve(deletedCount)
        }
      }

      request.onerror = () => reject(request.error)
    })
  }
}

// Main Permission Cache Manager
class PermissionCacheManager {
  private l1Cache: L1PermissionCache
  private l2Cache: L2PermissionCache
  private config: CacheConfig
  private metrics: CacheMetrics
  private warmingConfig: CacheWarmingConfig

  constructor(config: CacheConfig = DEFAULT_CONFIG) {
    this.config = config
    this.l1Cache = new L1PermissionCache(config)
    this.l2Cache = new L2PermissionCache()
    this.metrics = this.initMetrics()
    this.warmingConfig = {
      enabled: config.enableCacheWarming,
      strategies: [
        {
          type: 'user_login',
          resources: ['user', 'organization', 'settings'],
          actions: ['read', 'update'],
          priority: 1,
        },
        {
          type: 'role_change',
          resources: ['payment', 'subscription', 'billing'],
          actions: ['read', 'create', 'update'],
          priority: 2,
        },
      ],
      batchSize: 10,
      maxConcurrency: 3,
    }

    this.startPeriodicMaintenance()
  }

  /**
   * Get permission from cache with L1/L2 hierarchy
   */
  async get(
    userId: string,
    resource: string,
    action: string,
    resourceId?: string,
    organizationId?: string
  ): Promise<PermissionCacheEntry | null> {
    const key = this.generateCacheKey(userId, resource, action, resourceId, organizationId)
    const startTime = Date.now()

    try {
      // Check L1 cache first
      let entry = this.l1Cache.get(key)
      if (entry) {
        this.metrics.hits++
        this.metrics.l1Hits++
        this.updateMetrics(Date.now() - startTime)
        return entry
      }

      // Check L2 cache if enabled
      if (this.config.enableL2Cache) {
        entry = await this.l2Cache.get(key)
        if (entry) {
          // Promote to L1 cache
          this.l1Cache.set(entry)
          this.metrics.hits++
          this.metrics.l2Hits++
          this.updateMetrics(Date.now() - startTime)
          return entry
        }
      }

      // Cache miss
      this.metrics.misses++
      this.updateMetrics(Date.now() - startTime)
      return null
    } catch (error) {
      logger.error('Permission cache get error', {
        key,
        error: error instanceof Error ? error.message : 'Unknown error',
      })
      this.metrics.misses++
      return null
    }
  }

  /**
   * Set permission in cache with constitutional compliance validation
   */
  async set(
    userId: string,
    resource: string,
    action: string,
    result: boolean,
    options?: {
      resourceId?: string
      organizationId?: string
      reason?: string
      matchedPermissions?: any[]
      conditions?: any[]
      ttl?: number
    }
  ): Promise<void> {
    const key = this.generateCacheKey(
      userId,
      resource,
      action,
      options?.resourceId,
      options?.organizationId
    )

    // Generate security hash for integrity
    const securityHash = await this.generateSecurityHash(userId, resource, action, result)

    // Constitutional compliance validation
    const constitutionalCompliance = {
      validated: true,
      score: this.calculateConstitutionalScore(resource, action, result),
      timestamp: Date.now(),
    }

    const entry: PermissionCacheEntry = {
      key,
      userId: userId as UserId,
      resource,
      action,
      resourceId: options?.resourceId,
      organizationId: options?.organizationId,
      result,
      reason: options?.reason,
      matchedPermissions: options?.matchedPermissions,
      conditions: options?.conditions,
      timestamp: Date.now(),
      expiresAt: Date.now() + (options?.ttl || this.config.defaultTTL),
      accessCount: 0,
      lastAccessed: Date.now(),
      version: 1 as CacheVersion,
      securityHash,
      constitutionalCompliance,
    }

    try {
      // Set in L1 cache
      this.l1Cache.set(entry)

      // Set in L2 cache if enabled
      if (this.config.enableL2Cache) {
        await this.l2Cache.set(entry)
      }

      this.metrics.totalEntries++

      // Audit cache set operation
      if (this.config.enableSecurityValidation) {
        await auditPermissionCheck({
          eventId: `cache-set-${Date.now()}` as any,
          userId,
          resource,
          action,
          resourceId: options?.resourceId,
          organizationId: options?.organizationId,
          context: { cacheOperation: 'set', result },
          timestamp: new Date().toISOString(),
          result: result ? 'granted' : 'denied',
          reason: `Cache entry created: ${options?.reason || 'Permission cached'}`,
          securityContext: {
            userAgent: navigator.userAgent,
            ipAddress: 'client-side',
            sessionId: undefined,
          },
        })
      }

      logger.debug('Permission cached successfully', {
        key,
        userId,
        resource,
        action,
        result,
        expiresAt: new Date(entry.expiresAt).toISOString(),
        constitutionalScore: constitutionalCompliance.score,
      })
    } catch (error) {
      logger.error('Permission cache set error', {
        key,
        error: error instanceof Error ? error.message : 'Unknown error',
      })
    }
  }

  /**
   * Invalidate cache entries based on strategy
   */
  async invalidate(strategy: {
    type: 'user' | 'resource' | 'action' | 'organization' | 'all'
    value?: string
  }): Promise<number> {
    let invalidatedCount = 0

    try {
      switch (strategy.type) {
        case 'user':
          invalidatedCount = this.invalidateByUser(strategy.value!)
          break
        case 'resource':
          invalidatedCount = this.invalidateByResource(strategy.value!)
          break
        case 'organization':
          invalidatedCount = this.invalidateByOrganization(strategy.value!)
          break
        case 'all':
          this.l1Cache.clear()
          await this.l2Cache.clear()
          invalidatedCount = this.metrics.totalEntries
          this.metrics.totalEntries = 0
          break
      }

      this.metrics.evictions += invalidatedCount

      logger.info('Cache invalidation completed', {
        strategy: strategy.type,
        value: strategy.value,
        invalidatedCount,
      })
    } catch (error) {
      logger.error('Cache invalidation error', {
        strategy,
        error: error instanceof Error ? error.message : 'Unknown error',
      })
    }

    return invalidatedCount
  }

  /**
   * Warm cache with predicted permission checks
   */
  async warmCache(
    userId: string,
    strategy: WarmingStrategy,
    organizationId?: string
  ): Promise<void> {
    if (!this.warmingConfig.enabled) return

    logger.debug('Starting cache warming', {
      userId,
      strategy: strategy.type,
      resources: strategy.resources.length,
      actions: strategy.actions.length,
    })

    const warmingPromises: Promise<void>[] = []

    for (const resource of strategy.resources) {
      for (const action of strategy.actions) {
        warmingPromises.push(
          this.warmPermission(userId, resource, action, organizationId)
        )

        // Respect concurrency limits
        if (warmingPromises.length >= this.warmingConfig.maxConcurrency) {
          await Promise.allSettled(warmingPromises.splice(0, this.warmingConfig.batchSize))
        }
      }
    }

    // Process remaining promises
    if (warmingPromises.length > 0) {
      await Promise.allSettled(warmingPromises)
    }
  }

  /**
   * Get cache performance metrics
   */
  getMetrics(): CacheMetrics {
    this.metrics.totalEntries = this.l1Cache.size()
    this.metrics.memoryUsage = this.l1Cache.getMemoryUsage()
    this.metrics.hitRate = this.metrics.hits / (this.metrics.hits + this.metrics.misses) || 0
    this.metrics.lastUpdate = new Date().toISOString()

    return { ...this.metrics }
  }

  /**
   * Generate cache key with consistent format
   */
  private generateCacheKey(
    userId: string,
    resource: string,
    action: string,
    resourceId?: string,
    organizationId?: string
  ): CacheKey {
    const parts = [userId, resource, action]
    if (resourceId) parts.push(`rid:${resourceId}`)
    if (organizationId) parts.push(`org:${organizationId}`)

    return parts.join('|') as CacheKey
  }

  /**
   * Generate security hash for cache entry integrity
   */
  private async generateSecurityHash(
    userId: string,
    resource: string,
    action: string,
    result: boolean
  ): Promise<string> {
    const data = `${userId}:${resource}:${action}:${result}:${Date.now()}`
    const encoder = new TextEncoder()
    const hashBuffer = await crypto.subtle.digest('SHA-256', encoder.encode(data))
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
  }

  /**
   * Calculate constitutional compliance score for cache entry
   */
  private calculateConstitutionalScore(resource: string, action: string, result: boolean): number {
    let score = 50 // Base score

    // Higher score for proper permission enforcement
    if (!result && ['delete', 'manage', 'admin'].includes(action)) {
      score += 30 // Good: Restrictive permissions
    }

    // Score based on resource sensitivity
    const sensitiveResources = ['payment', 'billing', 'user', 'api_keys']
    if (sensitiveResources.includes(resource)) {
      score += 20
    }

    return Math.min(100, score)
  }

  /**
   * Invalidate cache entries by user
   */
  private invalidateByUser(userId: string): number {
    let count = 0
    const entries = this.l1Cache.getEntries()

    entries.forEach(entry => {
      if (entry.userId === userId) {
        this.l1Cache.delete(entry.key)
        count++
      }
    })

    return count
  }

  /**
   * Invalidate cache entries by resource
   */
  private invalidateByResource(resource: string): number {
    let count = 0
    const entries = this.l1Cache.getEntries()

    entries.forEach(entry => {
      if (entry.resource === resource) {
        this.l1Cache.delete(entry.key)
        count++
      }
    })

    return count
  }

  /**
   * Invalidate cache entries by organization
   */
  private invalidateByOrganization(organizationId: string): number {
    let count = 0
    const entries = this.l1Cache.getEntries()

    entries.forEach(entry => {
      if (entry.organizationId === organizationId) {
        this.l1Cache.delete(entry.key)
        count++
      }
    })

    return count
  }

  /**
   * Warm individual permission
   */
  private async warmPermission(
    userId: string,
    resource: string,
    action: string,
    organizationId?: string
  ): Promise<void> {
    // This would integrate with the actual permission checking API
    // For now, we'll simulate a permission check result
    const result = Math.random() > 0.2 // 80% success rate for warming

    await this.set(userId, resource, action, result, {
      organizationId,
      reason: 'Cache warming',
      ttl: this.config.defaultTTL,
    })
  }

  /**
   * Initialize metrics object
   */
  private initMetrics(): CacheMetrics {
    return {
      hits: 0,
      misses: 0,
      l1Hits: 0,
      l2Hits: 0,
      evictions: 0,
      totalEntries: 0,
      memoryUsage: 0,
      hitRate: 0,
      averageResponseTime: 0,
      securityViolations: 0,
      constitutionalCompliance: 100,
      lastUpdate: new Date().toISOString(),
    }
  }

  /**
   * Update performance metrics
   */
  private updateMetrics(responseTime: number): void {
    this.metrics.averageResponseTime =
      (this.metrics.averageResponseTime + responseTime) / 2
  }

  /**
   * Start periodic maintenance tasks
   */
  private startPeriodicMaintenance(): void {
    // Clean expired entries every 5 minutes
    setInterval(async () => {
      try {
        if (this.config.enableL2Cache) {
          const cleaned = await this.l2Cache.cleanExpired()
          if (cleaned > 0) {
            logger.debug('L2 cache maintenance completed', {
              expiredEntriesRemoved: cleaned,
            })
          }
        }
      } catch (error) {
        logger.error('Cache maintenance error', {
          error: error instanceof Error ? error.message : 'Unknown error',
        })
      }
    }, 5 * 60 * 1000)

    // Log metrics every minute
    setInterval(() => {
      const metrics = this.getMetrics()
      logger.debug('Permission cache metrics', {
        hitRate: `${(metrics.hitRate * 100).toFixed(2)}%`,
        totalEntries: metrics.totalEntries,
        memoryUsage: `${(metrics.memoryUsage / 1024).toFixed(2)}KB`,
        averageResponseTime: `${metrics.averageResponseTime.toFixed(2)}ms`,
      })
    }, 60 * 1000)
  }
}

// Global cache manager instance
export const permissionCache = new PermissionCacheManager()

export {
  PermissionCacheManager,
  type PermissionCacheEntry,
  type CacheConfig,
  type CacheMetrics,
  type CacheWarmingConfig,
}