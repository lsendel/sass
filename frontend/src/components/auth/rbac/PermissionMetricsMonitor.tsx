/**
 * Permission Metrics Monitor Component
 *
 * Real-time performance monitoring for the RBAC permission system:
 * - Cache performance metrics
 * - Permission check latency tracking
 * - Security anomaly detection
 * - Constitutional compliance scoring
 * - System health monitoring
 */

import React, { useState, useEffect, useRef } from 'react'
import { permissionCache } from '../../../utils/permissionCache'
import { getAuditMetrics } from '../../../utils/auditLogger'
import { securityMonitor } from '../../../utils/securityMonitor'
import { logger } from '../../../utils/logger'
import type { CacheMetrics } from '../../../utils/permissionCache'

// Performance metrics interface
interface PermissionMetrics {
  cache: CacheMetrics
  audit: {
    totalEvents: number
    complianceScore: number
    securityViolations: number
    lastUpdate: string
  }
  security: {
    activeAnomalies: number
    threatLevel: 'low' | 'medium' | 'high' | 'critical'
    lastIncident: string | null
  }
  performance: {
    averageLatency: number
    p95Latency: number
    errorRate: number
    throughput: number
  }
  constitutional: {
    complianceRate: number
    violations: string[]
    score: number
  }
}

// Real-time metrics update interval
const METRICS_UPDATE_INTERVAL = 5000 // 5 seconds
const PERFORMANCE_SAMPLES_LIMIT = 100

export interface PermissionMetricsMonitorProps {
  /** Whether to show detailed metrics */
  detailed?: boolean
  /** Refresh interval in milliseconds */
  refreshInterval?: number
  /** Callback when critical issues are detected */
  onCriticalIssue?: (issue: { type: string; message: string; severity: string }) => void
  /** Whether to enable real-time alerts */
  enableAlerts?: boolean
  /** Component styling theme */
  theme?: 'light' | 'dark'
}

/**
 * Performance monitoring component for RBAC system
 */
export const PermissionMetricsMonitor: React.FC<PermissionMetricsMonitorProps> = ({
  detailed = false,
  refreshInterval = METRICS_UPDATE_INTERVAL,
  onCriticalIssue,
  enableAlerts = true,
  theme = 'light',
}) => {
  const [metrics, setMetrics] = useState<PermissionMetrics | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date())

  // Performance tracking
  const latencySamples = useRef<number[]>([])
  const errorCount = useRef<number>(0)
  const successCount = useRef<number>(0)
  const updateIntervalRef = useRef<NodeJS.Timeout | null>(null)

  // Track permission check performance
  const trackPermissionLatency = (latency: number) => {
    latencySamples.current.push(latency)
    if (latencySamples.current.length > PERFORMANCE_SAMPLES_LIMIT) {
      latencySamples.current.shift()
    }
  }

  // Calculate performance metrics
  const calculatePerformanceMetrics = () => {
    const samples = latencySamples.current
    if (samples.length === 0) {
      return {
        averageLatency: 0,
        p95Latency: 0,
        errorRate: 0,
        throughput: 0,
      }
    }

    const sorted = [...samples].sort((a, b) => a - b)
    const p95Index = Math.floor(samples.length * 0.95)
    const averageLatency = samples.reduce((a, b) => a + b, 0) / samples.length
    const p95Latency = sorted[p95Index] || 0
    const totalRequests = errorCount.current + successCount.current
    const errorRate = totalRequests > 0 ? errorCount.current / totalRequests : 0
    const throughput = totalRequests / (refreshInterval / 1000) // requests per second

    return {
      averageLatency,
      p95Latency,
      errorRate,
      throughput,
    }
  }

  // Collect metrics from all systems
  const collectMetrics = async (): Promise<PermissionMetrics> => {
    try {
      const startTime = Date.now()

      // Get cache metrics
      const cacheMetrics = permissionCache.getMetrics()

      // Get audit metrics
      const auditMetrics = getAuditMetrics()

      // Get security metrics (mock for now)
      const securityMetrics = {
        activeAnomalies: 0,
        threatLevel: 'low' as const,
        lastIncident: null,
      }

      // Calculate performance metrics
      const performanceMetrics = calculatePerformanceMetrics()

      // Constitutional compliance metrics
      const constitutionalMetrics = {
        complianceRate: cacheMetrics.constitutionalCompliance / 100,
        violations: [],
        score: cacheMetrics.constitutionalCompliance,
      }

      const endTime = Date.now()
      trackPermissionLatency(endTime - startTime)
      successCount.current++

      return {
        cache: cacheMetrics,
        audit: {
          totalEvents: 0, // Would come from audit service
          complianceScore: auditMetrics.configuration.retentionDays > 0 ? 100 : 0,
          securityViolations: cacheMetrics.securityViolations,
          lastUpdate: new Date().toISOString(),
        },
        security: securityMetrics,
        performance: performanceMetrics,
        constitutional: constitutionalMetrics,
      }
    } catch (error) {
      errorCount.current++
      throw error
    }
  }

  // Update metrics
  const updateMetrics = async () => {
    try {
      setIsLoading(true)
      const newMetrics = await collectMetrics()
      setMetrics(newMetrics)
      setError(null)
      setLastUpdate(new Date())

      // Check for critical issues
      if (enableAlerts && onCriticalIssue) {
        await checkForCriticalIssues(newMetrics)
      }
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
  }

  // Check for critical issues
  const checkForCriticalIssues = async (metrics: PermissionMetrics) => {
    const issues: Array<{ type: string; message: string; severity: string }> = []

    // High error rate
    if (metrics.performance.errorRate > 0.1) {
      issues.push({
        type: 'high_error_rate',
        message: `Permission check error rate is ${(metrics.performance.errorRate * 100).toFixed(2)}%`,
        severity: 'critical',
      })
    }

    // High latency
    if (metrics.performance.p95Latency > 1000) {
      issues.push({
        type: 'high_latency',
        message: `95th percentile latency is ${metrics.performance.p95Latency.toFixed(2)}ms`,
        severity: 'warning',
      })
    }

    // Low cache hit rate
    if (metrics.cache.hitRate < 0.5 && metrics.cache.totalEntries > 10) {
      issues.push({
        type: 'low_cache_hit_rate',
        message: `Cache hit rate is only ${(metrics.cache.hitRate * 100).toFixed(2)}%`,
        severity: 'warning',
      })
    }

    // Constitutional compliance issues
    if (metrics.constitutional.score < 80) {
      issues.push({
        type: 'compliance_violation',
        message: `Constitutional compliance score is ${metrics.constitutional.score}/100`,
        severity: 'critical',
      })
    }

    // Security violations
    if (metrics.cache.securityViolations > 0) {
      issues.push({
        type: 'security_violations',
        message: `${metrics.cache.securityViolations} security violations detected`,
        severity: 'critical',
      })
    }

    // Report critical issues
    issues.forEach(issue => {
      if (onCriticalIssue) {
        onCriticalIssue(issue)
      }
    })
  }

  // Setup periodic updates
  useEffect(() => {
    updateMetrics() // Initial load

    updateIntervalRef.current = setInterval(updateMetrics, refreshInterval)

    return () => {
      if (updateIntervalRef.current) {
        clearInterval(updateIntervalRef.current)
      }
    }
  }, [refreshInterval])

  // Format numbers for display
  const formatNumber = (num: number, precision = 2): string => {
    if (num >= 1000000) return `${(num / 1000000).toFixed(precision)}M`
    if (num >= 1000) return `${(num / 1000).toFixed(precision)}K`
    return num.toFixed(precision)
  }

  // Format bytes for display
  const formatBytes = (bytes: number): string => {
    if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
    if (bytes >= 1024) return `${(bytes / 1024).toFixed(2)} KB`
    return `${bytes} B`
  }

  // Get status color based on value
  const getStatusColor = (value: number, thresholds: { good: number; warning: number }): string => {
    const baseClasses = theme === 'dark' ? 'text-' : 'text-'
    if (value >= thresholds.good) return `${baseClasses}green-400`
    if (value >= thresholds.warning) return `${baseClasses}yellow-400`
    return `${baseClasses}red-400`
  }

  const themeClasses = {
    container: theme === 'dark'
      ? 'bg-gray-900 border-gray-700 text-white'
      : 'bg-white border-gray-200 text-gray-900',
    card: theme === 'dark'
      ? 'bg-gray-800 border-gray-600'
      : 'bg-gray-50 border-gray-200',
    text: theme === 'dark' ? 'text-gray-300' : 'text-gray-600',
    accent: theme === 'dark' ? 'text-blue-400' : 'text-blue-600',
  }

  if (error) {
    return (
      <div className={`p-4 border rounded-lg ${themeClasses.container}`} data-testid="metrics-error">
        <div className="flex items-center">
          <svg className="w-5 h-5 text-red-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
          </svg>
          <span className="font-medium">Metrics Collection Error</span>
        </div>
        <p className={`mt-2 text-sm ${themeClasses.text}`}>{error}</p>
        <button
          onClick={updateMetrics}
          className="mt-3 px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
        >
          Retry
        </button>
      </div>
    )
  }

  if (isLoading && !metrics) {
    return (
      <div className={`p-4 border rounded-lg ${themeClasses.container}`} data-testid="metrics-loading">
        <div className="flex items-center">
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600 mr-2"></div>
          <span>Loading metrics...</span>
        </div>
      </div>
    )
  }

  if (!metrics) {
    return null
  }

  return (
    <div className={`space-y-4 p-4 border rounded-lg ${themeClasses.container}`} data-testid="permission-metrics">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">Permission System Metrics</h3>
        <div className="flex items-center space-x-2">
          <span className={`text-xs ${themeClasses.text}`}>
            Last updated: {lastUpdate.toLocaleTimeString()}
          </span>
          {isLoading && (
            <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-blue-600"></div>
          )}
        </div>
      </div>

      {/* Key Performance Indicators */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className={`p-3 border rounded ${themeClasses.card}`}>
          <div className={`text-xs font-medium ${themeClasses.text} uppercase tracking-wide`}>
            Cache Hit Rate
          </div>
          <div className={`text-2xl font-bold ${getStatusColor(metrics.cache.hitRate, { good: 0.8, warning: 0.5 })}`}>
            {(metrics.cache.hitRate * 100).toFixed(1)}%
          </div>
        </div>

        <div className={`p-3 border rounded ${themeClasses.card}`}>
          <div className={`text-xs font-medium ${themeClasses.text} uppercase tracking-wide`}>
            Avg Latency
          </div>
          <div className={`text-2xl font-bold ${getStatusColor(1000 - metrics.performance.averageLatency, { good: 800, warning: 500 })}`}>
            {metrics.performance.averageLatency.toFixed(0)}ms
          </div>
        </div>

        <div className={`p-3 border rounded ${themeClasses.card}`}>
          <div className={`text-xs font-medium ${themeClasses.text} uppercase tracking-wide`}>
            Compliance Score
          </div>
          <div className={`text-2xl font-bold ${getStatusColor(metrics.constitutional.score, { good: 90, warning: 70 })}`}>
            {metrics.constitutional.score}/100
          </div>
        </div>

        <div className={`p-3 border rounded ${themeClasses.card}`}>
          <div className={`text-xs font-medium ${themeClasses.text} uppercase tracking-wide`}>
            Error Rate
          </div>
          <div className={`text-2xl font-bold ${getStatusColor(100 - (metrics.performance.errorRate * 100), { good: 95, warning: 90 })}`}>
            {(metrics.performance.errorRate * 100).toFixed(2)}%
          </div>
        </div>
      </div>

      {/* Detailed Metrics (if enabled) */}
      {detailed && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Cache Metrics */}
          <div className={`p-4 border rounded ${themeClasses.card}`}>
            <h4 className="font-medium mb-3">Cache Performance</h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className={themeClasses.text}>Total Entries:</span>
                <span>{formatNumber(metrics.cache.totalEntries, 0)}</span>
              </div>
              <div className="flex justify-between">
                <span className={themeClasses.text}>Memory Usage:</span>
                <span>{formatBytes(metrics.cache.memoryUsage)}</span>
              </div>
              <div className="flex justify-between">
                <span className={themeClasses.text}>L1 Hits:</span>
                <span>{formatNumber(metrics.cache.l1Hits, 0)}</span>
              </div>
              <div className="flex justify-between">
                <span className={themeClasses.text}>L2 Hits:</span>
                <span>{formatNumber(metrics.cache.l2Hits, 0)}</span>
              </div>
              <div className="flex justify-between">
                <span className={themeClasses.text}>Evictions:</span>
                <span>{formatNumber(metrics.cache.evictions, 0)}</span>
              </div>
            </div>
          </div>

          {/* Performance Metrics */}
          <div className={`p-4 border rounded ${themeClasses.card}`}>
            <h4 className="font-medium mb-3">Performance Stats</h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className={themeClasses.text}>95th Percentile:</span>
                <span>{metrics.performance.p95Latency.toFixed(0)}ms</span>
              </div>
              <div className="flex justify-between">
                <span className={themeClasses.text}>Throughput:</span>
                <span>{metrics.performance.throughput.toFixed(1)} req/s</span>
              </div>
              <div className="flex justify-between">
                <span className={themeClasses.text}>Success Rate:</span>
                <span>{((1 - metrics.performance.errorRate) * 100).toFixed(2)}%</span>
              </div>
              <div className="flex justify-between">
                <span className={themeClasses.text}>Security Violations:</span>
                <span className={metrics.cache.securityViolations > 0 ? 'text-red-500' : ''}>
                  {metrics.cache.securityViolations}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default PermissionMetricsMonitor