import React, { useState, useEffect, memo, useMemo, useCallback } from 'react'
import { useOptimizedSelector, usePerformanceMonitor, useDebouncedCallback } from '../../hooks/usePerformanceOptimization'
import {
  useGetHealthSummaryQuery,
  useGetCacheStatsQuery,
  useGetAuditMetricsQuery,
  useGetSecurityAnalysisQuery,
  useGetAuditStreamStatusQuery,
  useTriggerAuditArchivalMutation,
  useTriggerAuditCompressionMutation,
  performanceUtils
} from '../../store/api/performanceApi'
import { useTenant } from '../../hooks/useTenant'

interface PerformanceMetrics {
  memory_usage_mb: number
  total_requests: number
  database_connections: number
  active_sessions: number
}

interface CacheStats {
  organizations: { hitRatio: number; status: string }
  users: { hitRatio: number; status: string }
  'payment-methods': { hitRatio: number; status: string }
}

interface Recommendation {
  category: string
  issue: string
  suggestion: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
}

interface HealthSummary {
  healthScore: number
  status: string
  metrics: PerformanceMetrics
  recommendations: Recommendation[]
  cachePerformance: Record<string, number>
}

/**
 * Enhanced real-time performance monitoring dashboard with audit integration,
 * security analysis, and automated optimization recommendations.
 */
const RealTimePerformanceDashboard = memo(() => {
  const { markStart, markEnd } = usePerformanceMonitor('RealTimePerformanceDashboard')
  const { currentTenant } = useTenant()

  const [selectedTab, setSelectedTab] = useState<'overview' | 'audit' | 'security'>('overview')
  const [alertsEnabled, setAlertsEnabled] = useState(true)

  // RTK Query hooks with real-time polling
  const {
    data: healthSummary,
    isLoading: healthLoading,
    error: healthError
  } = useGetHealthSummaryQuery()

  const {
    data: cacheStats,
    isLoading: cacheLoading
  } = useGetCacheStatsQuery()

  const {
    data: auditMetrics,
    isLoading: auditLoading
  } = useGetAuditMetricsQuery()

  const {
    data: securityAnalysis,
    isLoading: securityLoading
  } = useGetSecurityAnalysisQuery(
    { organizationId: currentTenant?.id || '', lookbackHours: 24 },
    { skip: !currentTenant?.id }
  )

  const {
    data: streamStatus,
    isLoading: streamLoading
  } = useGetAuditStreamStatusQuery()

  // Mutations for administrative actions
  const [triggerArchival, { isLoading: archivalLoading }] = useTriggerAuditArchivalMutation()
  const [triggerCompression, { isLoading: compressionLoading }] = useTriggerAuditCompressionMutation()

  const isLoading = healthLoading || cacheLoading || auditLoading
  const error = healthError

  // Memoized calculations
  const overallPerformanceScore = useMemo(() => {
    if (!healthSummary || !cacheStats) return 0
    return performanceUtils.calculatePerformanceScore(
      healthSummary.healthScore,
      cacheStats,
      auditMetrics
    )
  }, [healthSummary, cacheStats, auditMetrics])

  const alertLevel = useMemo(() => {
    if (!healthSummary || !securityAnalysis) return 'none'
    return performanceUtils.getAlertLevel(
      healthSummary.healthScore,
      securityAnalysis.riskScore,
      healthSummary.metrics
    )
  }, [healthSummary, securityAnalysis])

  // Debounced tab change
  const handleTabChange = useDebouncedCallback((tab: 'overview' | 'audit' | 'security') => {
    setSelectedTab(tab)
  }, 100)

  // Administrative action handlers
  const handleArchival = useCallback(async () => {
    try {
      await triggerArchival().unwrap()
      // Show success notification
    } catch (error) {
      console.error('Archival failed:', error)
      // Show error notification
    }
  }, [triggerArchival])

  const handleCompression = useCallback(async () => {
    try {
      await triggerCompression().unwrap()
      // Show success notification
    } catch (error) {
      console.error('Compression failed:', error)
      // Show error notification
    }
  }, [triggerCompression])

  // Memoized health status styling
  const getStatusColor = useMemo(() => (status: string) => {
    switch (status) {
      case 'excellent': return 'text-green-600 bg-green-100'
      case 'good': return 'text-blue-600 bg-blue-100'
      case 'fair': return 'text-yellow-600 bg-yellow-100'
      case 'poor': return 'text-orange-600 bg-orange-100'
      case 'critical': return 'text-red-600 bg-red-100'
      default: return 'text-gray-600 bg-gray-100'
    }
  }, [])

  // Memoized priority styling
  const getPriorityColor = useMemo(() => (priority: string) => {
    switch (priority) {
      case 'HIGH': return 'text-red-600 bg-red-100 border-red-200'
      case 'MEDIUM': return 'text-yellow-600 bg-yellow-100 border-yellow-200'
      case 'LOW': return 'text-blue-600 bg-blue-100 border-blue-200'
      default: return 'text-gray-600 bg-gray-100 border-gray-200'
    }
  }, [])

  // Memoized cache status styling
  const getCacheStatusColor = useMemo(() => (hitRatio: number) => {
    if (hitRatio >= 0.9) return 'text-green-600'
    if (hitRatio >= 0.8) return 'text-blue-600'
    if (hitRatio >= 0.6) return 'text-yellow-600'
    return 'text-red-600'
  }, [])

  if (isLoading) {
    return (
      <div className="p-6 bg-white rounded-lg shadow-sm border">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded mb-4"></div>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="h-24 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="p-6 bg-white rounded-lg shadow-sm border">
        <div className="text-center text-red-600">
          <div className="text-lg font-medium mb-2">Error Loading Performance Data</div>
          <div className="text-sm">{error}</div>
          <button
            onClick={fetchPerformanceData}
            className="mt-4 px-4 py-2 bg-red-100 text-red-700 rounded-md hover:bg-red-200 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  if (!healthSummary) return null

  return (
    <div className="space-y-6">
      {/* Header with controls */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">System Performance</h2>
          <p className="text-gray-600 mt-1">Real-time monitoring and optimization</p>
        </div>

        <div className="flex items-center gap-3">
          <select
            value={refreshInterval}
            onChange={(e) => setRefreshInterval(Number(e.target.value))}
            className="px-3 py-2 border border-gray-300 rounded-md text-sm"
          >
            <option value={2000}>2s refresh</option>
            <option value={5000}>5s refresh</option>
            <option value={10000}>10s refresh</option>
            <option value={30000}>30s refresh</option>
          </select>

          <div className={`px-3 py-2 rounded-md text-sm font-medium ${getStatusColor(healthSummary.status)}`}>
            Health: {healthSummary.healthScore}/100 ({healthSummary.status})
          </div>
        </div>
      </div>

      {/* Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-500 text-sm">Memory Usage</p>
              <p className="text-2xl font-bold text-gray-900">
                {healthSummary.metrics.memory_usage_mb} MB
              </p>
            </div>
            <div className={`p-3 rounded-full ${
              healthSummary.metrics.memory_usage_mb > 1024 ? 'bg-red-100' : 'bg-green-100'
            }`}>
              <span className={`text-lg ${
                healthSummary.metrics.memory_usage_mb > 1024 ? 'text-red-600' : 'text-green-600'
              }`}>
                🧠
              </span>
            </div>
          </div>
          <div className="mt-2">
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className={`h-2 rounded-full transition-all duration-300 ${
                  healthSummary.metrics.memory_usage_mb > 1024 ? 'bg-red-500' : 'bg-green-500'
                }`}
                style={{ width: `${Math.min(100, (healthSummary.metrics.memory_usage_mb / 2048) * 100)}%` }}
              />
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-500 text-sm">Total Requests</p>
              <p className="text-2xl font-bold text-gray-900">
                {healthSummary.metrics.total_requests.toLocaleString()}
              </p>
            </div>
            <div className="p-3 bg-blue-100 rounded-full">
              <span className="text-blue-600 text-lg">📊</span>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-500 text-sm">DB Connections</p>
              <p className="text-2xl font-bold text-gray-900">
                {healthSummary.metrics.database_connections}
              </p>
            </div>
            <div className="p-3 bg-purple-100 rounded-full">
              <span className="text-purple-600 text-lg">🗄️</span>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-500 text-sm">Active Sessions</p>
              <p className="text-2xl font-bold text-gray-900">
                {healthSummary.metrics.active_sessions}
              </p>
            </div>
            <div className="p-3 bg-orange-100 rounded-full">
              <span className="text-orange-600 text-lg">👥</span>
            </div>
          </div>
        </div>
      </div>

      {/* Cache Performance */}
      {cacheStats && (
        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Cache Performance</h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {Object.entries(cacheStats).map(([cacheName, stats]) => (
              <div key={cacheName} className="p-4 border rounded-lg">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-gray-700 capitalize">
                    {cacheName.replace('-', ' ')}
                  </span>
                  <span className={`text-sm font-medium ${getCacheStatusColor(stats.hitRatio)}`}>
                    {stats.status}
                  </span>
                </div>
                <div className="text-2xl font-bold text-gray-900 mb-1">
                  {(stats.hitRatio * 100).toFixed(1)}%
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full transition-all duration-300 ${
                      stats.hitRatio >= 0.8 ? 'bg-green-500' : 'bg-yellow-500'
                    }`}
                    style={{ width: `${stats.hitRatio * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Performance Recommendations */}
      {healthSummary.recommendations.length > 0 && (
        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Performance Recommendations</h3>
          <div className="space-y-3">
            {healthSummary.recommendations.map((rec, index) => (
              <div
                key={index}
                className={`p-4 rounded-lg border ${getPriorityColor(rec.priority)}`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-medium">{rec.category}</span>
                      <span className={`px-2 py-1 text-xs font-medium rounded ${getPriorityColor(rec.priority)}`}>
                        {rec.priority}
                      </span>
                    </div>
                    <p className="text-sm mb-2">{rec.issue}</p>
                    <p className="text-sm opacity-75">💡 {rec.suggestion}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Status Indicator */}
      <div className="text-center text-sm text-gray-500">
        Last updated: {new Date().toLocaleTimeString()} •
        Refreshing every {refreshInterval / 1000}s
      </div>
    </div>
  )
})

RealTimePerformanceDashboard.displayName = 'RealTimePerformanceDashboard'

export default RealTimePerformanceDashboard