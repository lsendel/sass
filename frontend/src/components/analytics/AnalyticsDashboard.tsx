/**
 * Advanced Analytics Dashboard
 *
 * Comprehensive analytics platform interface supporting:
 * - Real-time metrics monitoring and visualization
 * - Custom dashboard creation and management
 * - Machine learning insights and predictions
 * - Interactive data exploration and filtering
 * - Cross-tenant analytics and reporting capabilities
 * - Performance monitoring and business intelligence
 */

import React, { useState, useMemo } from 'react'
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend as RechartsLegend,
  ResponsiveContainer,
} from 'recharts'
import {
  ChartBarIcon,
  ChartPieIcon,
  CursorArrowRaysIcon,
  EyeIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  InformationCircleIcon,
  ClockIcon,
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  LightBulbIcon,
  BellIcon,
  CalendarIcon,
  PlusIcon,
  AdjustmentsHorizontalIcon,
} from '@heroicons/react/24/outline'

import {
  useListMetricsQuery,
  useListDashboardsQuery,
  useGetInsightsQuery,
  useGetAnalyticsHealthQuery,
  useExecuteAnalyticsQueryMutation,
  useAcknowledgeInsightMutation,
} from '@/store/api/analyticsApi'
import { useTenant } from '@/components/tenants/TenantProvider'
import type {
  TimePeriod,
  TimeGranularity,
} from '@/types/analytics'

// Chart library (would be imported from a charting library like Chart.js, D3, or Recharts)
// For now, using placeholder components

// Icons

interface AnalyticsDashboardProps {
  className?: string
}

const AnalyticsDashboard: React.FC<AnalyticsDashboardProps> = ({
  className = '',
}) => {
  const {} = useTenant()
  const [selectedTimePeriod, setSelectedTimePeriod] = useState<TimePeriod>('day')
  const [selectedGranularity, setSelectedGranularity] = useState<TimeGranularity>('hour')
  const [activeTab, setActiveTab] = useState<'overview' | 'metrics' | 'dashboards' | 'insights' | 'health'>('overview')
  const [selectedMetrics, setSelectedMetrics] = useState<string[]>([])

  // API hooks
  const {
    data: metricsResponse,
    isLoading: isLoadingMetrics,
  } = useListMetricsQuery({
    page: 1,
    pageSize: 50,
    sortBy: 'name',
    sortOrder: 'asc',
  })

  const {
    data: dashboardsResponse,
    isLoading: isLoadingDashboards,
  } = useListDashboardsQuery({
    page: 1,
    pageSize: 20,
    sortBy: 'lastViewedAt',
    sortOrder: 'desc',
  })

  const {
    data: insights,
    isLoading: isLoadingInsights,
  } = useGetInsightsQuery({
    acknowledged: false,
    limit: 10,
  })

  const {
    data: analyticsHealth,
  } = useGetAnalyticsHealthQuery()

  const [] = useExecuteAnalyticsQueryMutation()
  const [acknowledgeInsight] = useAcknowledgeInsightMutation()

  const metrics = metricsResponse?.metrics ?? []
  const dashboards = dashboardsResponse?.dashboards ?? []


  // Key metrics overview
  const keyMetrics = useMemo(() => [
    {
      id: 'active_users',
      name: 'Active Users',
      value: '2,847',
      change: '+12.5%',
      trend: 'up',
      icon: <CursorArrowRaysIcon className="h-6 w-6" />,
      color: 'text-blue-600',
      bgColor: 'bg-blue-50',
    },
    {
      id: 'revenue',
      name: 'Revenue',
      value: '$45,231',
      change: '+8.2%',
      trend: 'up',
      icon: <ChartBarIcon className="h-6 w-6" />,
      color: 'text-green-600',
      bgColor: 'bg-green-50',
    },
    {
      id: 'conversion_rate',
      name: 'Conversion Rate',
      value: '3.24%',
      change: '-2.1%',
      trend: 'down',
      icon: <ChartPieIcon className="h-6 w-6" />,
      color: 'text-yellow-600',
      bgColor: 'bg-yellow-50',
    },
    {
      id: 'avg_session_duration',
      name: 'Avg Session Duration',
      value: '4m 32s',
      change: '+5.7%',
      trend: 'up',
      icon: <ClockIcon className="h-6 w-6" />,
      color: 'text-purple-600',
      bgColor: 'bg-purple-50',
    },
  ], [])

  // Sample chart data (would be fetched from API)
  const chartData = useMemo(() => {
    const data = []
    const now = new Date()
    for (let i = 23; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 60 * 60 * 1000)
      data.push({
        time: time.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
        users: Math.floor(Math.random() * 1000) + 500,
        revenue: Math.floor(Math.random() * 5000) + 2000,
        sessions: Math.floor(Math.random() * 800) + 300,
        conversions: Math.floor(Math.random() * 50) + 10,
      })
    }
    return data
  }, [])

  const handleAcknowledgeInsight = async (insightId: string) => {
    try {
      await acknowledgeInsight({ insightId }).unwrap()
    } catch (error) {
      console.error('Failed to acknowledge insight:', error)
    }
  }

  const getInsightIcon = (type: string) => {
    switch (type) {
      case 'trend':
        return <ArrowTrendingUpIcon className="h-5 w-5" />
      case 'anomaly':
        return <ExclamationTriangleIcon className="h-5 w-5" />
      case 'prediction':
        return <LightBulbIcon className="h-5 w-5" />
      case 'recommendation':
        return <InformationCircleIcon className="h-5 w-5" />
      default:
        return <BellIcon className="h-5 w-5" />
    }
  }

  const getInsightColor = (severity: string) => {
    switch (severity) {
      case 'critical':
        return 'border-red-200 bg-red-50 text-red-800'
      case 'high':
        return 'border-orange-200 bg-orange-50 text-orange-800'
      case 'medium':
        return 'border-yellow-200 bg-yellow-50 text-yellow-800'
      case 'low':
        return 'border-blue-200 bg-blue-50 text-blue-800'
      default:
        return 'border-gray-200 bg-gray-50 text-gray-800'
    }
  }

  const tabs = [
    { key: 'overview', label: 'Overview', icon: ChartBarIcon },
    { key: 'metrics', label: 'Metrics', icon: ChartPieIcon },
    { key: 'dashboards', label: 'Dashboards', icon: EyeIcon },
    { key: 'insights', label: 'Insights', icon: LightBulbIcon },
    { key: 'health', label: 'Health', icon: CheckCircleIcon },
  ]

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Analytics Dashboard</h1>
          <p className="mt-2 text-sm text-gray-600">
            Monitor your key metrics, insights, and performance indicators in real-time.
          </p>
        </div>

        <div className="flex items-center space-x-4">
          {/* Time Period Selector */}
          <div className="flex items-center space-x-2">
            <CalendarIcon className="h-5 w-5 text-gray-400" />
            <select
              value={selectedTimePeriod}
              onChange={(e) => setSelectedTimePeriod(e.target.value as TimePeriod)}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
            >
              <option value="hour">Last Hour</option>
              <option value="day">Last 24 Hours</option>
              <option value="week">Last Week</option>
              <option value="month">Last Month</option>
              <option value="quarter">Last Quarter</option>
              <option value="year">Last Year</option>
            </select>
          </div>

          {/* Granularity Selector */}
          <div className="flex items-center space-x-2">
            <AdjustmentsHorizontalIcon className="h-5 w-5 text-gray-400" />
            <select
              value={selectedGranularity}
              onChange={(e) => setSelectedGranularity(e.target.value as TimeGranularity)}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
            >
              <option value="minute">Minute</option>
              <option value="hour">Hour</option>
              <option value="day">Day</option>
              <option value="week">Week</option>
              <option value="month">Month</option>
            </select>
          </div>

          <button
            type="button"
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
          >
            <PlusIcon className="h-4 w-4 mr-2" />
            Create Dashboard
          </button>
        </div>
      </div>

      {/* Health Status Bar */}
      {analyticsHealth && (
        <div className={`rounded-lg p-4 ${
          analyticsHealth.status === 'healthy' ? 'bg-green-50 border border-green-200' :
          analyticsHealth.status === 'degraded' ? 'bg-yellow-50 border border-yellow-200' :
          'bg-red-50 border border-red-200'
        }`}>
          <div className="flex items-center">
            <div className={`flex-shrink-0 ${
              analyticsHealth.status === 'healthy' ? 'text-green-400' :
              analyticsHealth.status === 'degraded' ? 'text-yellow-400' :
              'text-red-400'
            }`}>
              <CheckCircleIcon className="h-5 w-5" />
            </div>
            <div className="ml-3">
              <h3 className={`text-sm font-medium ${
                analyticsHealth.status === 'healthy' ? 'text-green-800' :
                analyticsHealth.status === 'degraded' ? 'text-yellow-800' :
                'text-red-800'
              }`}>
                Analytics System {analyticsHealth.status === 'healthy' ? 'Operational' :
                analyticsHealth.status === 'degraded' ? 'Degraded' : 'Down'}
              </h3>
              <div className={`mt-1 text-sm ${
                analyticsHealth.status === 'healthy' ? 'text-green-700' :
                analyticsHealth.status === 'degraded' ? 'text-yellow-700' :
                'text-red-700'
              }`}>
                <span>Components: </span>
                {Object.entries(analyticsHealth.components).map(([name, component]) => (
                  <span key={name} className="mr-2">
                    {name}: {component.status}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tabs Navigation */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as 'overview' | 'metrics' | 'dashboards' | 'insights' | 'health')}
              className={`group inline-flex items-center py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === tab.key
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <tab.icon
                className={`-ml-0.5 mr-2 h-5 w-5 ${
                  activeTab === tab.key ? 'text-indigo-500' : 'text-gray-400 group-hover:text-gray-500'
                }`}
              />
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          {/* Key Metrics Cards */}
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {keyMetrics.map((metric) => (
              <div key={metric.id} className="bg-white overflow-hidden shadow rounded-lg">
                <div className="p-5">
                  <div className="flex items-center">
                    <div className="flex-shrink-0">
                      <div className={`inline-flex items-center justify-center p-3 ${metric.bgColor} rounded-md`}>
                        <div className={metric.color}>
                          {metric.icon}
                        </div>
                      </div>
                    </div>
                    <div className="ml-5 w-0 flex-1">
                      <dl>
                        <dt className="text-sm font-medium text-gray-500 truncate">
                          {metric.name}
                        </dt>
                        <dd className="flex items-baseline">
                          <div className="text-2xl font-semibold text-gray-900">
                            {metric.value}
                          </div>
                          <div className={`ml-2 flex items-baseline text-sm font-semibold ${
                            metric.trend === 'up' ? 'text-green-600' : 'text-red-600'
                          }`}>
                            {metric.trend === 'up' ? (
                              <ArrowTrendingUpIcon className="self-center flex-shrink-0 h-4 w-4" />
                            ) : (
                              <ArrowTrendingDownIcon className="self-center flex-shrink-0 h-4 w-4" />
                            )}
                            <span className="ml-1">{metric.change}</span>
                          </div>
                        </dd>
                      </dl>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Charts Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Users Over Time */}
            <div className="bg-white shadow rounded-lg p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">Active Users</h3>
                <div className="flex items-center space-x-2">
                  <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                    Live
                  </span>
                </div>
              </div>
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="time" />
                    <YAxis />
                    <Tooltip />
                    <RechartsLegend />
                    <Line
                      type="monotone"
                      dataKey="users"
                      stroke="#3B82F6"
                      strokeWidth={2}
                      dot={{ fill: '#3B82F6' }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Revenue Over Time */}
            <div className="bg-white shadow rounded-lg p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">Revenue</h3>
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-gray-500">USD</span>
                </div>
              </div>
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="time" />
                    <YAxis />
                    <Tooltip formatter={(value) => [`$${value}`, 'Revenue']} />
                    <RechartsLegend />
                    <Area
                      type="monotone"
                      dataKey="revenue"
                      stroke="#10B981"
                      fill="#10B981"
                      fillOpacity={0.3}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Sessions and Conversions */}
            <div className="bg-white shadow rounded-lg p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">Sessions & Conversions</h3>
              </div>
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="time" />
                    <YAxis />
                    <Tooltip />
                    <RechartsLegend />
                    <Bar dataKey="sessions" fill="#8B5CF6" />
                    <Bar dataKey="conversions" fill="#F59E0B" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Top Insights */}
            <div className="bg-white shadow rounded-lg p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">Recent Insights</h3>
                <button
                  onClick={() => setActiveTab('insights')}
                  className="text-sm text-indigo-600 hover:text-indigo-900"
                >
                  View all
                </button>
              </div>
              <div className="space-y-3">
                {insights?.slice(0, 5).map((insight) => (
                  <div
                    key={insight.id}
                    className={`p-3 rounded-lg border ${getInsightColor(insight.severity)}`}
                  >
                    <div className="flex items-start">
                      <div className="flex-shrink-0">
                        {getInsightIcon(insight.type)}
                      </div>
                      <div className="ml-3 flex-1">
                        <h4 className="text-sm font-medium">{insight.title}</h4>
                        <p className="mt-1 text-sm opacity-90">{insight.summary}</p>
                        <div className="mt-2 flex items-center justify-between">
                          <span className="text-xs opacity-75">
                            Confidence: {Math.round(insight.confidence * 100)}%
                          </span>
                          <button
                            onClick={() => void handleAcknowledgeInsight(insight.id)}
                            className="text-xs hover:underline"
                          >
                            Acknowledge
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
                {(!insights || insights.length === 0) && (
                  <div className="text-center py-4 text-gray-500">
                    <LightBulbIcon className="mx-auto h-8 w-8 mb-2" />
                    <p className="text-sm">No insights available</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'metrics' && (
        <div className="space-y-6">
          {/* Metrics Management Interface */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Available Metrics</h3>
              {isLoadingMetrics ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mx-auto"></div>
                  <p className="mt-2 text-sm text-gray-500">Loading metrics...</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {metrics.map((metric) => (
                    <div
                      key={metric.id}
                      role="button"
                      tabIndex={0}
                      className="border border-gray-200 rounded-lg p-4 hover:border-indigo-300 cursor-pointer focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      onClick={() => {
                        if (selectedMetrics.includes(metric.id)) {
                          setSelectedMetrics(selectedMetrics.filter(id => id !== metric.id))
                        } else {
                          setSelectedMetrics([...selectedMetrics, metric.id])
                        }
                      }}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault()
                          if (selectedMetrics.includes(metric.id)) {
                            setSelectedMetrics(selectedMetrics.filter(id => id !== metric.id))
                          } else {
                            setSelectedMetrics([...selectedMetrics, metric.id])
                          }
                        }
                      }}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <h4 className="text-sm font-medium text-gray-900">{metric.name}</h4>
                          <p className="text-xs text-gray-500">{metric.category}</p>
                        </div>
                        <div className="flex items-center space-x-2">
                          <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                            metric.type === 'counter' ? 'bg-blue-100 text-blue-800' :
                            metric.type === 'gauge' ? 'bg-green-100 text-green-800' :
                            metric.type === 'rate' ? 'bg-yellow-100 text-yellow-800' :
                            'bg-gray-100 text-gray-800'
                          }`}>
                            {metric.type}
                          </span>
                          {selectedMetrics.includes(metric.id) && (
                            <CheckCircleIcon className="h-5 w-5 text-indigo-600" />
                          )}
                        </div>
                      </div>
                      <p className="mt-2 text-sm text-gray-600">{metric.description}</p>
                      <div className="mt-2 flex flex-wrap gap-1">
                        {metric.tags.map((tag) => (
                          <span
                            key={tag}
                            className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'dashboards' && (
        <div className="space-y-6">
          {/* Dashboard Management Interface */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">My Dashboards</h3>
              {isLoadingDashboards ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mx-auto"></div>
                  <p className="mt-2 text-sm text-gray-500">Loading dashboards...</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {dashboards.map((dashboard) => (
                    <div
                      key={dashboard.id}
                      className="border border-gray-200 rounded-lg p-4 hover:border-indigo-300 cursor-pointer transition-colors"
                    >
                      <div className="flex items-center justify-between mb-3">
                        <h4 className="text-lg font-medium text-gray-900">{dashboard.name}</h4>
                        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                          dashboard.visibility === 'public' ? 'bg-green-100 text-green-800' :
                          dashboard.visibility === 'private' ? 'bg-red-100 text-red-800' :
                          'bg-blue-100 text-blue-800'
                        }`}>
                          {dashboard.visibility}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 mb-3">{dashboard.description}</p>
                      <div className="flex items-center justify-between text-xs text-gray-500">
                        <span>{dashboard.widgets.length} widgets</span>
                        <span>{dashboard.viewCount} views</span>
                      </div>
                      <div className="mt-3 flex flex-wrap gap-1">
                        {dashboard.tags.map((tag) => (
                          <span
                            key={tag}
                            className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'insights' && (
        <div className="space-y-6">
          {/* Insights Management Interface */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">AI-Powered Insights</h3>
              {isLoadingInsights ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 mx-auto"></div>
                  <p className="mt-2 text-sm text-gray-500">Loading insights...</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {insights?.map((insight) => (
                    <div
                      key={insight.id}
                      className={`p-4 rounded-lg border ${getInsightColor(insight.severity)}`}
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex items-start">
                          <div className="flex-shrink-0 mr-3">
                            {getInsightIcon(insight.type)}
                          </div>
                          <div className="flex-1">
                            <h4 className="text-sm font-medium">{insight.title}</h4>
                            <p className="mt-1 text-sm">{insight.description}</p>
                            <div className="mt-2 flex items-center space-x-4 text-xs opacity-75">
                              <span>Type: {insight.type}</span>
                              <span>Confidence: {Math.round(insight.confidence * 100)}%</span>
                              <span>Generated: {new Date(insight.generatedAt).toLocaleString()}</span>
                            </div>
                            {insight.recommendations && insight.recommendations.length > 0 && (
                              <div className="mt-3">
                                <h5 className="text-xs font-medium mb-2">Recommendations:</h5>
                                <ul className="space-y-1">
                                  {insight.recommendations.map((rec, index) => (
                                    <li key={index} className="text-xs flex items-center">
                                      <span className={`inline-block w-2 h-2 rounded-full mr-2 ${
                                        rec.impact === 'high' ? 'bg-red-400' :
                                        rec.impact === 'medium' ? 'bg-yellow-400' :
                                        'bg-green-400'
                                      }`}></span>
                                      {rec.action}
                                    </li>
                                  ))}
                                </ul>
                              </div>
                            )}
                          </div>
                        </div>
                        <div className="flex items-center space-x-2">
                          <button
                            onClick={() => void handleAcknowledgeInsight(insight.id)}
                            className="text-xs px-3 py-1 rounded bg-white bg-opacity-50 hover:bg-opacity-75 transition-colors"
                          >
                            Acknowledge
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                  {(!insights || insights.length === 0) && (
                    <div className="text-center py-8 text-gray-500">
                      <LightBulbIcon className="mx-auto h-12 w-12 mb-4" />
                      <h3 className="text-lg font-medium mb-2">No insights available</h3>
                      <p className="text-sm">
                        Insights will appear here as our AI analyzes your data patterns.
                      </p>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'health' && (
        <div className="space-y-6">
          {/* System Health Interface */}
          <div className="bg-white shadow rounded-lg">
            <div className="px-4 py-5 sm:p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">System Health</h3>
              {analyticsHealth && (
                <div className="space-y-4">
                  <div className={`p-4 rounded-lg ${
                    analyticsHealth.status === 'healthy' ? 'bg-green-50 border border-green-200' :
                    analyticsHealth.status === 'degraded' ? 'bg-yellow-50 border border-yellow-200' :
                    'bg-red-50 border border-red-200'
                  }`}>
                    <div className="flex items-center">
                      <CheckCircleIcon className={`h-6 w-6 ${
                        analyticsHealth.status === 'healthy' ? 'text-green-500' :
                        analyticsHealth.status === 'degraded' ? 'text-yellow-500' :
                        'text-red-500'
                      }`} />
                      <h4 className={`ml-2 text-lg font-medium ${
                        analyticsHealth.status === 'healthy' ? 'text-green-800' :
                        analyticsHealth.status === 'degraded' ? 'text-yellow-800' :
                        'text-red-800'
                      }`}>
                        Overall Status: {analyticsHealth.status.charAt(0).toUpperCase() + analyticsHealth.status.slice(1)}
                      </h4>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {Object.entries(analyticsHealth.components).map(([name, component]) => (
                      <div
                        key={name}
                        className={`p-4 rounded-lg border ${
                          component.status === 'healthy' ? 'border-green-200 bg-green-50' :
                          component.status === 'degraded' ? 'border-yellow-200 bg-yellow-50' :
                          'border-red-200 bg-red-50'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <h5 className="font-medium text-gray-900">{name}</h5>
                          <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                            component.status === 'healthy' ? 'bg-green-100 text-green-800' :
                            component.status === 'degraded' ? 'bg-yellow-100 text-yellow-800' :
                            'bg-red-100 text-red-800'
                          }`}>
                            {component.status}
                          </span>
                        </div>
                        <p className="mt-1 text-sm text-gray-600">
                          Last checked: {new Date(component.lastCheck).toLocaleString()}
                        </p>
                        {component.details && (
                          <p className="mt-1 text-xs text-gray-500">{component.details}</p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default AnalyticsDashboard