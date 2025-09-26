/**
 * Real-time Performance Monitoring Dashboard
 *
 * Comprehensive performance monitoring interface showing:
 * - Core Web Vitals in real-time
 * - Cache performance metrics
 * - Bundle loading analysis
 * - User journey performance
 * - Memory usage tracking
 * - Network performance
 */

import React, { useState, useEffect, useMemo } from 'react'
import {
  ChartBarIcon,
  ClockIcon,
  CpuChipIcon,
  GlobeAltIcon,
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline'
import { format } from 'date-fns'
import { clsx } from 'clsx'

import { usePerformanceTracking, performanceMonitor } from '../../utils/performance'
import { useCacheAnalytics } from '../../store/middleware/cacheEnhancer'
import { logger } from '../../utils/logger'

// Types for performance dashboard
interface PerformanceMetricCard {
  title: string
  value: string | number
  unit?: string
  trend?: 'up' | 'down' | 'stable'
  status: 'good' | 'warning' | 'critical'
  description: string
  threshold?: { good: number; warning: number }
}

interface PerformanceAlert {
  id: string
  type: 'error' | 'warning' | 'info'
  message: string
  timestamp: number
  metric?: string
}

// Web Vitals scoring thresholds
const WEB_VITALS_THRESHOLDS = {
  lcp: { good: 2500, warning: 4000 },      // Largest Contentful Paint
  fid: { good: 100, warning: 300 },        // First Input Delay
  cls: { good: 0.1, warning: 0.25 },       // Cumulative Layout Shift
  fcp: { good: 1800, warning: 3000 },      // First Contentful Paint
  ttfb: { good: 800, warning: 1800 },      // Time to First Byte
}

const PerformanceDashboard: React.FC = () => {
  const [alerts, setAlerts] = useState<PerformanceAlert[]>([])
  const [isVisible, setIsVisible] = useState(false)
  const [refreshInterval, setRefreshInterval] = useState(5000)

  const performanceTracking = usePerformanceTracking()
  const cacheAnalytics = useCacheAnalytics()

  // Real-time metrics state
  const [webVitals, setWebVitals] = useState(performanceTracking.getWebVitals())
  const [memoryInfo, setMemoryInfo] = useState<any>(null)
  const [networkInfo, setNetworkInfo] = useState<any>(null)

  // Update metrics periodically
  useEffect(() => {
    const interval = setInterval(() => {
      setWebVitals(performanceTracking.getWebVitals())
      updateMemoryInfo()
      updateNetworkInfo()
      checkPerformanceAlerts()
    }, refreshInterval)

    return () => clearInterval(interval)
  }, [refreshInterval, performanceTracking])

  const updateMemoryInfo = () => {
    if ('memory' in performance) {
      const memory = (performance as any).memory
      setMemoryInfo({
        used: memory.usedJSHeapSize,
        total: memory.totalJSHeapSize,
        limit: memory.jsHeapSizeLimit,
        usagePercent: (memory.usedJSHeapSize / memory.totalJSHeapSize) * 100
      })
    }
  }

  const updateNetworkInfo = () => {
    if ('connection' in navigator) {
      const connection = (navigator as any).connection
      setNetworkInfo({
        effectiveType: connection.effectiveType,
        downlink: connection.downlink,
        rtt: connection.rtt,
        saveData: connection.saveData
      })
    }
  }

  const checkPerformanceAlerts = () => {
    const newAlerts: PerformanceAlert[] = []
    const now = Date.now()

    // Check Web Vitals
    if (webVitals.lcp && webVitals.lcp > WEB_VITALS_THRESHOLDS.lcp.warning) {
      newAlerts.push({
        id: `lcp-${now}`,
        type: 'warning',
        message: `LCP is ${webVitals.lcp.toFixed(0)}ms (target: <2.5s)`,
        timestamp: now,
        metric: 'lcp'
      })
    }

    if (webVitals.cls && webVitals.cls > WEB_VITALS_THRESHOLDS.cls.warning) {
      newAlerts.push({
        id: `cls-${now}`,
        type: 'error',
        message: `High CLS detected: ${webVitals.cls.toFixed(3)}`,
        timestamp: now,
        metric: 'cls'
      })
    }

    // Check cache performance
    if (cacheAnalytics.hitRate < 0.5) {
      newAlerts.push({
        id: `cache-${now}`,
        type: 'warning',
        message: `Low cache hit rate: ${(cacheAnalytics.hitRate * 100).toFixed(1)}%`,
        timestamp: now,
        metric: 'cache'
      })
    }

    // Check memory usage
    if (memoryInfo && memoryInfo.usagePercent > 80) {
      newAlerts.push({
        id: `memory-${now}`,
        type: 'error',
        message: `High memory usage: ${memoryInfo.usagePercent.toFixed(1)}%`,
        timestamp: now,
        metric: 'memory'
      })
    }

    setAlerts(prev => [...newAlerts, ...prev.slice(0, 9)]) // Keep last 10 alerts
  }

  // Calculate performance metrics for cards
  const performanceMetrics = useMemo((): PerformanceMetricCard[] => {
    const getStatus = (value: number, thresholds: { good: number; warning: number }): 'good' | 'warning' | 'critical' => {
      if (value <= thresholds.good) return 'good'
      if (value <= thresholds.warning) return 'warning'
      return 'critical'
    }

    return [
      {
        title: 'LCP',
        value: webVitals.lcp ? (webVitals.lcp / 1000).toFixed(2) : 'N/A',
        unit: 's',
        status: webVitals.lcp ? getStatus(webVitals.lcp, WEB_VITALS_THRESHOLDS.lcp) : 'good',
        description: 'Largest Contentful Paint - loading performance',
        threshold: WEB_VITALS_THRESHOLDS.lcp
      },
      {
        title: 'FID',
        value: webVitals.fid ? webVitals.fid.toFixed(0) : 'N/A',
        unit: 'ms',
        status: webVitals.fid ? getStatus(webVitals.fid, WEB_VITALS_THRESHOLDS.fid) : 'good',
        description: 'First Input Delay - interactivity',
        threshold: WEB_VITALS_THRESHOLDS.fid
      },
      {
        title: 'CLS',
        value: webVitals.cls ? webVitals.cls.toFixed(3) : 'N/A',
        status: webVitals.cls ? getStatus(webVitals.cls, WEB_VITALS_THRESHOLDS.cls) : 'good',
        description: 'Cumulative Layout Shift - visual stability',
        threshold: WEB_VITALS_THRESHOLDS.cls
      },
      {
        title: 'Cache Hit Rate',
        value: (cacheAnalytics.hitRate * 100).toFixed(1),
        unit: '%',
        status: cacheAnalytics.hitRate >= 0.7 ? 'good' : cacheAnalytics.hitRate >= 0.5 ? 'warning' : 'critical',
        description: 'API cache effectiveness',
      },
      {
        title: 'Memory Usage',
        value: memoryInfo ? memoryInfo.usagePercent.toFixed(1) : 'N/A',
        unit: '%',
        status: memoryInfo ? (memoryInfo.usagePercent < 60 ? 'good' : memoryInfo.usagePercent < 80 ? 'warning' : 'critical') : 'good',
        description: 'JavaScript heap memory usage',
      },
      {
        title: 'Network',
        value: networkInfo?.effectiveType || 'N/A',
        status: networkInfo?.effectiveType === '4g' ? 'good' : networkInfo?.effectiveType === '3g' ? 'warning' : 'critical',
        description: 'Connection quality',
      }
    ]
  }, [webVitals, cacheAnalytics, memoryInfo, networkInfo])

  if (!isVisible) {
    return (
      <div className="fixed bottom-4 right-4 z-50">
        <button
          onClick={() => setIsVisible(true)}
          className="bg-blue-600 text-white p-3 rounded-full shadow-lg hover:bg-blue-700 transition-colors"
          title="Show Performance Dashboard"
        >
          <ChartBarIcon className="w-6 h-6" />
        </button>
      </div>
    )
  }

  return (
    <div className="fixed inset-0 z-50 bg-black bg-opacity-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-6xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <div className="flex items-center space-x-3">
            <ChartBarIcon className="w-8 h-8 text-blue-600" />
            <div>
              <h2 className="text-2xl font-bold text-gray-900">Performance Dashboard</h2>
              <p className="text-sm text-gray-500">Real-time performance monitoring</p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <select
              value={refreshInterval}
              onChange={(e) => setRefreshInterval(Number(e.target.value))}
              className="text-sm border border-gray-300 rounded px-2 py-1"
            >
              <option value={1000}>1s</option>
              <option value={5000}>5s</option>
              <option value={10000}>10s</option>
              <option value={30000}>30s</option>
            </select>
            <button
              onClick={() => setIsVisible(false)}
              className="text-gray-400 hover:text-gray-600"
            >
              ✕
            </button>
          </div>
        </div>

        {/* Alerts */}
        {alerts.length > 0 && (
          <div className="p-6 border-b border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-3">Performance Alerts</h3>
            <div className="space-y-2">
              {alerts.slice(0, 3).map((alert) => (
                <div
                  key={alert.id}
                  className={clsx(
                    'flex items-center space-x-3 p-3 rounded-lg',
                    alert.type === 'error' && 'bg-red-50 border border-red-200',
                    alert.type === 'warning' && 'bg-yellow-50 border border-yellow-200',
                    alert.type === 'info' && 'bg-blue-50 border border-blue-200'
                  )}
                >
                  <ExclamationTriangleIcon
                    className={clsx(
                      'w-5 h-5',
                      alert.type === 'error' && 'text-red-500',
                      alert.type === 'warning' && 'text-yellow-500',
                      alert.type === 'info' && 'text-blue-500'
                    )}
                  />
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-900">{alert.message}</p>
                    <p className="text-xs text-gray-500">
                      {format(alert.timestamp, 'HH:mm:ss')}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Metrics Grid */}
        <div className="p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Performance Metrics</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {performanceMetrics.map((metric) => (
              <PerformanceMetricCard key={metric.title} metric={metric} />
            ))}
          </div>
        </div>

        {/* Cache Details */}
        <div className="p-6 border-t border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Cache Performance</h3>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div>
              <h4 className="font-medium text-gray-900 mb-2">Overall Statistics</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-600">Hit Rate:</span>
                  <span className="font-medium">{(cacheAnalytics.hitRate * 100).toFixed(1)}%</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Tracked Endpoints:</span>
                  <span className="font-medium">{cacheAnalytics.metrics?.length || 0}</span>
                </div>
              </div>
            </div>

            <div>
              <h4 className="font-medium text-gray-900 mb-2">Recommendations</h4>
              <div className="space-y-1">
                {cacheAnalytics.recommendations?.slice(0, 3).map((rec, index) => (
                  <p key={index} className="text-xs text-gray-600">{rec}</p>
                )) || <p className="text-xs text-gray-500">No recommendations</p>}
              </div>
            </div>
          </div>
        </div>

        {/* System Info */}
        <div className="p-6 border-t border-gray-200 bg-gray-50">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
            <div>
              <p className="text-gray-600">User Agent:</p>
              <p className="font-medium text-xs">{navigator.userAgent.split(' ')[0]}</p>
            </div>
            <div>
              <p className="text-gray-600">Connection:</p>
              <p className="font-medium">{networkInfo?.effectiveType || 'Unknown'}</p>
            </div>
            <div>
              <p className="text-gray-600">Memory Limit:</p>
              <p className="font-medium">
                {memoryInfo ? `${(memoryInfo.limit / 1024 / 1024 / 1024).toFixed(1)}GB` : 'N/A'}
              </p>
            </div>
            <div>
              <p className="text-gray-600">Last Updated:</p>
              <p className="font-medium">{format(Date.now(), 'HH:mm:ss')}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

// Individual metric card component
const PerformanceMetricCard: React.FC<{ metric: PerformanceMetricCard }> = ({ metric }) => {
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'good':
        return <CheckCircleIcon className="w-5 h-5 text-green-500" />
      case 'warning':
        return <ExclamationTriangleIcon className="w-5 h-5 text-yellow-500" />
      case 'critical':
        return <ExclamationTriangleIcon className="w-5 h-5 text-red-500" />
      default:
        return <ClockIcon className="w-5 h-5 text-gray-400" />
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'good':
        return 'border-green-200 bg-green-50'
      case 'warning':
        return 'border-yellow-200 bg-yellow-50'
      case 'critical':
        return 'border-red-200 bg-red-50'
      default:
        return 'border-gray-200 bg-gray-50'
    }
  }

  return (
    <div className={clsx('p-4 rounded-lg border-2', getStatusColor(metric.status))}>
      <div className="flex items-center justify-between mb-2">
        <h4 className="font-medium text-gray-900">{metric.title}</h4>
        {getStatusIcon(metric.status)}
      </div>
      <div className="mb-2">
        <span className="text-2xl font-bold text-gray-900">
          {metric.value}
        </span>
        {metric.unit && (
          <span className="text-sm text-gray-500 ml-1">{metric.unit}</span>
        )}
      </div>
      <p className="text-xs text-gray-600">{metric.description}</p>
      {metric.threshold && (
        <div className="mt-2 text-xs text-gray-500">
          Target: &lt;{metric.threshold.good}{metric.unit} • Warning: &gt;{metric.threshold.warning}{metric.unit}
        </div>
      )}
    </div>
  )
}

export default PerformanceDashboard