import React, { memo, useMemo, useCallback, Suspense, lazy } from 'react'
import { ErrorBoundary } from 'react-error-boundary'
import { useSmartMemo, useDebouncedCallback, usePerformanceMonitor } from '../../hooks/usePerformanceOptimization'
import { useTenant } from '../../hooks/useTenant'

// Lazy load heavy chart components
const LineChart = lazy(() => import('recharts').then(module => ({ default: module.LineChart })))
const ResponsiveContainer = lazy(() => import('recharts').then(module => ({ default: module.ResponsiveContainer })))

interface OptimizedAnalyticsDashboardProps {
  className?: string
}

interface TimePeriod {
  value: string
  label: string
  hours: number
}

interface MetricData {
  timestamp: string
  value: number
  trend: number
}

/**
 * Performance-optimized analytics dashboard with intelligent caching
 * and lazy loading for better user experience.
 */
const OptimizedAnalyticsDashboard = memo<OptimizedAnalyticsDashboardProps>(({
  className = ''
}) => {
  const { markStart, markEnd } = usePerformanceMonitor('AnalyticsDashboard')
  const { currentTenant } = useTenant()

  // Optimized state management
  const [selectedTimePeriod, setSelectedTimePeriod] = React.useState<TimePeriod>({
    value: 'day',
    label: '24 Hours',
    hours: 24
  })
  const [selectedMetrics, setSelectedMetrics] = React.useState<string[]>(['active_users'])

  // Memoized time periods configuration
  const timePeriods = useMemo(() => [
    { value: 'hour', label: '1 Hour', hours: 1 },
    { value: 'day', label: '24 Hours', hours: 24 },
    { value: 'week', label: '7 Days', hours: 168 },
    { value: 'month', label: '30 Days', hours: 720 }
  ], [])

  // Smart memoization for expensive chart data calculation
  const chartData = useSmartMemo(() => {
    markStart('chart-data-calculation')

    const data: MetricData[] = []
    const now = new Date()
    const hours = selectedTimePeriod.hours

    // Generate optimized sample data
    for (let i = hours - 1; i >= 0; i--) {
      const timestamp = new Date(now.getTime() - i * 60 * 60 * 1000)
      data.push({
        timestamp: timestamp.toISOString(),
        value: Math.floor(Math.random() * 1000) + 500,
        trend: Math.random() * 10 - 5
      })
    }

    markEnd('chart-data-calculation')
    return data
  }, [selectedTimePeriod.hours], {
    maxAge: 30000, // Cache for 30 seconds
    compareFunction: (prev, next) => prev[0] === next[0] // Compare only first dependency
  })

  // Memoized metrics configuration
  const availableMetrics = useMemo(() => [
    {
      id: 'active_users',
      name: 'Active Users',
      color: '#3B82F6',
      icon: '👥'
    },
    {
      id: 'page_views',
      name: 'Page Views',
      color: '#10B981',
      icon: '📊'
    },
    {
      id: 'api_calls',
      name: 'API Calls',
      color: '#F59E0B',
      icon: '🔄'
    },
    {
      id: 'errors',
      name: 'Error Rate',
      color: '#EF4444',
      icon: '⚠️'
    }
  ], [])

  // Debounced handlers for better performance
  const handleTimePeriodChange = useDebouncedCallback((period: TimePeriod) => {
    setSelectedTimePeriod(period)
  }, 100)

  const handleMetricToggle = useCallback((metricId: string) => {
    setSelectedMetrics(prev =>
      prev.includes(metricId)
        ? prev.filter(id => id !== metricId)
        : [...prev, metricId]
    )
  }, [])

  // Memoized statistics calculation
  const statistics = useMemo(() => ({
    totalUsers: chartData.reduce((sum, point) => sum + point.value, 0),
    averageValue: chartData.length > 0
      ? Math.round(chartData.reduce((sum, point) => sum + point.value, 0) / chartData.length)
      : 0,
    trend: chartData.length > 1
      ? ((chartData[chartData.length - 1]?.value || 0) - (chartData[0]?.value || 0)) / (chartData[0]?.value || 1) * 100
      : 0
  }), [chartData])

  // Error fallback component
  const ErrorFallback = useCallback(({ error }: { error: Error }) => (
    <div className="p-4 border border-red-200 rounded-lg bg-red-50">
      <h3 className="text-red-800 font-medium">Dashboard Error</h3>
      <p className="text-red-600 text-sm mt-1">
        Failed to load analytics: {error.message}
      </p>
    </div>
  ), [])

  // Loading component for chart suspense
  const ChartLoading = useCallback(() => (
    <div className="h-64 bg-gray-100 rounded-lg animate-pulse flex items-center justify-center">
      <div className="text-gray-500">Loading chart...</div>
    </div>
  ), [])

  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <div className={`p-6 space-y-6 ${className}`}>
        {/* Header with controls */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">
              Analytics Dashboard
            </h2>
            <p className="text-gray-600 mt-1">
              Performance metrics for {currentTenant?.name || 'your organization'}
            </p>
          </div>

          {/* Time period selector */}
          <div className="flex flex-wrap gap-2">
            {timePeriods.map((period) => (
              <button
                key={period.value}
                onClick={() => handleTimePeriodChange(period)}
                className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  selectedTimePeriod.value === period.value
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {period.label}
              </button>
            ))}
          </div>
        </div>

        {/* Statistics cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">Total Events</p>
                <p className="text-2xl font-bold text-gray-900">
                  {statistics.totalUsers.toLocaleString()}
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
                <p className="text-gray-500 text-sm">Average</p>
                <p className="text-2xl font-bold text-gray-900">
                  {statistics.averageValue.toLocaleString()}
                </p>
              </div>
              <div className="p-3 bg-green-100 rounded-full">
                <span className="text-green-600 text-lg">📈</span>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-lg shadow-sm border">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">Trend</p>
                <p className={`text-2xl font-bold ${
                  statistics.trend >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  {statistics.trend >= 0 ? '+' : ''}{statistics.trend.toFixed(1)}%
                </p>
              </div>
              <div className={`p-3 rounded-full ${
                statistics.trend >= 0 ? 'bg-green-100' : 'bg-red-100'
              }`}>
                <span className={`text-lg ${
                  statistics.trend >= 0 ? 'text-green-600' : 'text-red-600'
                }`}>
                  {statistics.trend >= 0 ? '📈' : '📉'}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Metrics selector */}
        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Metrics</h3>
          <div className="flex flex-wrap gap-3">
            {availableMetrics.map((metric) => (
              <button
                key={metric.id}
                onClick={() => handleMetricToggle(metric.id)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg border transition-colors ${
                  selectedMetrics.includes(metric.id)
                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                    : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                <span>{metric.icon}</span>
                <span className="text-sm font-medium">{metric.name}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Chart section with lazy loading */}
        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <h3 className="text-lg font-medium text-gray-900 mb-4">
            Trends Over {selectedTimePeriod.label}
          </h3>

          <Suspense fallback={<ChartLoading />}>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                  {/* Chart configuration would go here */}
                  <defs>
                    <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.8}/>
                      <stop offset="95%" stopColor="#3B82F6" stopOpacity={0.1}/>
                    </linearGradient>
                  </defs>
                </LineChart>
              </ResponsiveContainer>
            </div>
          </Suspense>
        </div>
      </div>
    </ErrorBoundary>
  )
})

OptimizedAnalyticsDashboard.displayName = 'OptimizedAnalyticsDashboard'

export default OptimizedAnalyticsDashboard