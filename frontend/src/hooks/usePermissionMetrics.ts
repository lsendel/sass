/**
 * Performance Metrics Hook for RBAC Permission System
 *
 * Provides real-time monitoring and analytics for permission system performance:
 * - Cache performance tracking
 * - Permission check latency monitoring
 * - Security anomaly detection
 * - Constitutional compliance monitoring
 * - System health alerts
 */

import { useState, useEffect, useRef, useCallback } from 'react'
import { permissionCache } from '../utils/permissionCache'
import { getAuditMetrics } from '../utils/auditLogger'
// import { securityMonitor } from '../utils/securityMonitor'
import { logger } from '../utils/logger'
import type { CacheMetrics } from '../utils/permissionCache'

// Performance metrics interfaces
export interface PermissionPerformanceMetrics {
  averageLatency: number
  p95Latency: number
  errorRate: number
  throughput: number
  successRate: number
  totalRequests: number
}

export interface SystemHealthMetrics {
  cache: CacheMetrics
  performance: PermissionPerformanceMetrics
  security: {
    activeAnomalies: number
    threatLevel: 'low' | 'medium' | 'high' | 'critical'
    violationsCount: number
  }
  constitutional: {
    complianceScore: number
    violations: string[]
    isCompliant: boolean
  }
  audit: {
    eventsCount: number
    retentionCompliance: boolean
    lastAuditTime: string
  }
}

export interface AlertConfig {
  latencyThreshold: number // ms
  errorRateThreshold: number // percentage (0-1)
  cacheHitRateThreshold: number // percentage (0-1)
  complianceScoreThreshold: number // 0-100
  enableRealTimeAlerts: boolean
}

export interface PerformanceAlert {
  id: string
  type: 'latency' | 'error_rate' | 'cache_performance' | 'compliance' | 'security'
  severity: 'low' | 'medium' | 'high' | 'critical'
  message: string
  timestamp: Date
  acknowledged: boolean
  metadata?: Record<string, unknown>
}

// Default alert configuration
const DEFAULT_ALERT_CONFIG: AlertConfig = {
  latencyThreshold: 1000, // 1 second
  errorRateThreshold: 0.1, // 10%
  cacheHitRateThreshold: 0.5, // 50%
  complianceScoreThreshold: 80, // 80/100
  enableRealTimeAlerts: true,
}

/**
 * Hook for monitoring RBAC permission system performance
 */
export const usePermissionMetrics = (
  config: Partial<AlertConfig> = {},
  refreshInterval = 5000
) => {
  const alertConfig = { ...DEFAULT_ALERT_CONFIG, ...config }

  // State
  const [metrics, setMetrics] = useState<SystemHealthMetrics | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [alerts, setAlerts] = useState<PerformanceAlert[]>([])
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date())

  // Performance tracking refs
  const latencySamples = useRef<number[]>([])
  const errorCount = useRef<number>(0)
  const successCount = useRef<number>(0)
  const requestCount = useRef<number>(0)
  const intervalRef = useRef<NodeJS.Timeout | null>(null)

  // Track permission check performance
  const trackPermissionLatency = useCallback((latency: number, success: boolean) => {
    latencySamples.current.push(latency)
    requestCount.current++

    if (success) {
      successCount.current++
    } else {
      errorCount.current++
    }

    // Keep only last 100 samples
    if (latencySamples.current.length > 100) {
      latencySamples.current.shift()
    }
  }, [])

  // Calculate performance metrics
  const calculatePerformanceMetrics = useCallback((): PermissionPerformanceMetrics => {
    const samples = latencySamples.current
    const totalRequests = requestCount.current

    if (samples.length === 0 || totalRequests === 0) {
      return {
        averageLatency: 0,
        p95Latency: 0,
        errorRate: 0,
        throughput: 0,
        successRate: 1,
        totalRequests: 0,
      }
    }

    // Calculate latency metrics
    const sorted = [...samples].sort((a, b) => a - b)
    const averageLatency = samples.reduce((a, b) => a + b, 0) / samples.length
    const p95Index = Math.floor(samples.length * 0.95)
    const p95Latency = sorted[p95Index] || 0

    // Calculate error metrics
    const errorRate = errorCount.current / totalRequests
    const successRate = successCount.current / totalRequests

    // Calculate throughput (requests per second)
    const timeWindow = refreshInterval / 1000
    const throughput = totalRequests / timeWindow

    return {
      averageLatency,
      p95Latency,
      errorRate,
      throughput,
      successRate,
      totalRequests,
    }
  }, [refreshInterval])

  // Generate alert ID
  const generateAlertId = (): string => {
    return `alert-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  }

  // Create alert
  const createAlert = useCallback((
    type: PerformanceAlert['type'],
    severity: PerformanceAlert['severity'],
    message: string,
    metadata?: Record<string, unknown>
  ): PerformanceAlert => {
    return {
      id: generateAlertId(),
      type,
      severity,
      message,
      timestamp: new Date(),
      acknowledged: false,
      metadata: metadata || {},
    }
  }, [])

  // Check for performance alerts
  const checkForAlerts = useCallback((currentMetrics: SystemHealthMetrics): PerformanceAlert[] => {
    const newAlerts: PerformanceAlert[] = []

    if (!alertConfig.enableRealTimeAlerts) {
      return newAlerts
    }

    // High latency alert
    if (currentMetrics.performance.p95Latency > alertConfig.latencyThreshold) {
      newAlerts.push(createAlert(
        'latency',
        currentMetrics.performance.p95Latency > alertConfig.latencyThreshold * 2 ? 'critical' : 'high',
        `High latency detected: ${currentMetrics.performance.p95Latency.toFixed(0)}ms (threshold: ${alertConfig.latencyThreshold}ms)`,
        { latency: currentMetrics.performance.p95Latency }
      ))
    }

    // High error rate alert
    if (currentMetrics.performance.errorRate > alertConfig.errorRateThreshold) {
      newAlerts.push(createAlert(
        'error_rate',
        currentMetrics.performance.errorRate > alertConfig.errorRateThreshold * 2 ? 'critical' : 'high',
        `High error rate detected: ${(currentMetrics.performance.errorRate * 100).toFixed(2)}% (threshold: ${(alertConfig.errorRateThreshold * 100).toFixed(2)}%)`,
        { errorRate: currentMetrics.performance.errorRate }
      ))
    }

    // Low cache hit rate alert
    if (currentMetrics.cache.hitRate < alertConfig.cacheHitRateThreshold && currentMetrics.cache.totalEntries > 10) {
      newAlerts.push(createAlert(
        'cache_performance',
        'medium',
        `Low cache hit rate: ${(currentMetrics.cache.hitRate * 100).toFixed(2)}% (threshold: ${(alertConfig.cacheHitRateThreshold * 100).toFixed(2)}%)`,
        { hitRate: currentMetrics.cache.hitRate }
      ))
    }

    // Constitutional compliance alert
    if (currentMetrics.constitutional.complianceScore < alertConfig.complianceScoreThreshold) {
      newAlerts.push(createAlert(
        'compliance',
        currentMetrics.constitutional.complianceScore < 50 ? 'critical' : 'high',
        `Constitutional compliance below threshold: ${currentMetrics.constitutional.complianceScore}/100 (threshold: ${alertConfig.complianceScoreThreshold})`,
        { complianceScore: currentMetrics.constitutional.complianceScore }
      ))
    }

    // Security violations alert
    if (currentMetrics.security.violationsCount > 0) {
      newAlerts.push(createAlert(
        'security',
        currentMetrics.security.violationsCount > 5 ? 'critical' : 'high',
        `Security violations detected: ${currentMetrics.security.violationsCount} violations`,
        { violationsCount: currentMetrics.security.violationsCount }
      ))
    }

    return newAlerts
  }, [alertConfig, createAlert])

  // Collect all system metrics
  const collectMetrics = useCallback(async (): Promise<SystemHealthMetrics> => {
    const startTime = Date.now()

    try {
      // Get cache metrics
      const cacheMetrics = permissionCache.getMetrics()

      // Calculate performance metrics
      const performanceMetrics = calculatePerformanceMetrics()

      // Get audit metrics
      const auditMetrics = getAuditMetrics()

      // Security metrics (would integrate with actual security monitoring)
      const securityMetrics = {
        activeAnomalies: 0, // Would come from security monitor
        threatLevel: 'low' as const,
        violationsCount: cacheMetrics.securityViolations,
      }

      // Constitutional compliance metrics
      const constitutionalMetrics = {
        complianceScore: cacheMetrics.constitutionalCompliance,
        violations: [], // Would come from compliance validator
        isCompliant: cacheMetrics.constitutionalCompliance >= alertConfig.complianceScoreThreshold,
      }

      // Audit metrics
      const auditSystemMetrics = {
        eventsCount: 0, // Would come from audit service
        retentionCompliance: auditMetrics.configuration.retentionDays >= 2555, // 7 years
        lastAuditTime: new Date().toISOString(),
      }

      const endTime = Date.now()
      trackPermissionLatency(endTime - startTime, true)

      return {
        cache: cacheMetrics,
        performance: performanceMetrics,
        security: securityMetrics,
        constitutional: constitutionalMetrics,
        audit: auditSystemMetrics,
      }
    } catch (error) {
      const endTime = Date.now()
      trackPermissionLatency(endTime - startTime, false)
      throw error
    }
  }, [calculatePerformanceMetrics, trackPermissionLatency, alertConfig.complianceScoreThreshold])

  // Update metrics and check for alerts
  const updateMetrics = useCallback(async () => {
    try {
      setIsLoading(true)
      const newMetrics = await collectMetrics()
      setMetrics(newMetrics)
      setError(null)
      setLastUpdate(new Date())

      // Check for new alerts
      const newAlerts = checkForAlerts(newMetrics)
      if (newAlerts.length > 0) {
        setAlerts(prevAlerts => [...prevAlerts, ...newAlerts])
      }

      // Reset counters for next interval
      errorCount.current = 0
      successCount.current = 0
      requestCount.current = 0

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Unknown error'
      setError(errorMessage)
      logger.error('Failed to collect permission metrics', {
        error: errorMessage,
        timestamp: new Date().toISOString(),
      })
    } finally {
      setIsLoading(false)
    }
  }, [collectMetrics, checkForAlerts])

  // Acknowledge alert
  const acknowledgeAlert = useCallback((alertId: string) => {
    setAlerts(prevAlerts =>
      prevAlerts.map(alert =>
        alert.id === alertId ? { ...alert, acknowledged: true } : alert
      )
    )
  }, [])

  // Clear acknowledged alerts
  const clearAcknowledgedAlerts = useCallback(() => {
    setAlerts(prevAlerts => prevAlerts.filter(alert => !alert.acknowledged))
  }, [])

  // Get unacknowledged alerts
  const getUnacknowledgedAlerts = useCallback(() => {
    return alerts.filter(alert => !alert.acknowledged)
  }, [alerts])

  // Get alerts by severity
  const getAlertsBySeverity = useCallback((severity: PerformanceAlert['severity']) => {
    return alerts.filter(alert => alert.severity === severity)
  }, [alerts])

  // Reset metrics
  const resetMetrics = useCallback(() => {
    latencySamples.current = []
    errorCount.current = 0
    successCount.current = 0
    requestCount.current = 0
    setAlerts([])
    setError(null)
  }, [])

  // Setup periodic updates
  useEffect(() => {
    updateMetrics() // Initial load

    intervalRef.current = setInterval(updateMetrics, refreshInterval)

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
      }
    }
  }, [updateMetrics, refreshInterval])

  return {
    // State
    metrics,
    isLoading,
    error,
    alerts,
    lastUpdate,

    // Actions
    updateMetrics,
    resetMetrics,
    acknowledgeAlert,
    clearAcknowledgedAlerts,

    // Performance tracking
    trackPermissionLatency,

    // Alert utilities
    getUnacknowledgedAlerts,
    getAlertsBySeverity,

    // Configuration
    alertConfig,
  }
}

export default usePermissionMetrics