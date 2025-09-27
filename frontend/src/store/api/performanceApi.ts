import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

/**
 * Performance monitoring API integration with optimized caching
 * and selective invalidation for real-time dashboard updates.
 */

interface PerformanceMetrics {
  memory_usage_mb: number
  total_requests: number
  database_connections: number
  active_sessions: number
}

interface CacheStats {
  [cacheName: string]: {
    hitRatio: number
    status: string
  }
}

interface HealthSummary {
  healthScore: number
  status: string
  metrics: PerformanceMetrics
  recommendations: Recommendation[]
  cachePerformance: Record<string, number>
}

interface Recommendation {
  category: string
  issue: string
  suggestion: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
}

interface SecurityAnalysis {
  organizationId: string
  analysisTimestamp: string
  lookbackHours: number
  securityEventCount: number
  suspiciousIpCount: number
  userAnomalyCount: number
  riskScore: number
  securityStatus: string
  recommendations: string[]
}

interface AuditMetrics {
  recent_events_5min: number
  audit_cache_hit_ratio: number
  average_query_time_ms: number
  storage_usage_mb: number
  archival_status: string
  compression_status: string
}

interface StreamStatus {
  events_per_second: number
  events_last_minute: number
  events_last_5_minutes: number
  stream_health: string
  processing_lag_ms: number
  queue_depth: number
}

export const performanceApi = createApi({
  reducerPath: 'performanceApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/v1/performance',
    prepareHeaders: (headers, { getState }) => {
      // Add any auth headers if needed
      return headers
    },
  }),
  tagTypes: ['PerformanceMetrics', 'CacheStats', 'HealthSummary', 'AuditMetrics', 'SecurityAnalysis'],
  endpoints: (builder) => ({
    // Main performance endpoints
    getPerformanceMetrics: builder.query<{ metrics: PerformanceMetrics; timestamp: number }, void>({
      query: () => '/metrics',
      providesTags: ['PerformanceMetrics'],
      // Poll every 5 seconds for real-time updates
      pollingInterval: 5000,
    }),

    getHealthSummary: builder.query<HealthSummary, void>({
      query: () => '/health-summary',
      providesTags: ['HealthSummary'],
      // Poll every 10 seconds for health updates
      pollingInterval: 10000,
    }),

    getCacheStats: builder.query<CacheStats, void>({
      query: () => '/cache-stats',
      providesTags: ['CacheStats'],
      // Poll every 15 seconds for cache stats
      pollingInterval: 15000,
    }),

    getRecommendations: builder.query<{ recommendations: Recommendation[] }, void>({
      query: () => '/recommendations',
      providesTags: ['PerformanceMetrics'],
      // Poll every 30 seconds for recommendations
      pollingInterval: 30000,
    }),

    // Audit performance endpoints
    getAuditMetrics: builder.query<AuditMetrics, void>({
      query: () => '/audit/metrics',
      providesTags: ['AuditMetrics'],
      pollingInterval: 10000,
    }),

    getSecurityAnalysis: builder.query<SecurityAnalysis, { organizationId: string; lookbackHours?: number }>({
      query: ({ organizationId, lookbackHours = 24 }) =>
        `/audit/security-analysis/${organizationId}?lookbackHours=${lookbackHours}`,
      providesTags: (result, error, arg) => [
        { type: 'SecurityAnalysis', id: arg.organizationId }
      ],
      pollingInterval: 60000, // Poll every minute for security analysis
    }),

    getAuditStreamStatus: builder.query<StreamStatus, void>({
      query: () => '/audit/stream-status',
      providesTags: ['AuditMetrics'],
      pollingInterval: 5000, // Real-time stream monitoring
    }),

    getAuditRecommendations: builder.query<{ recommendations: Recommendation[] }, void>({
      query: () => '/audit/recommendations',
      providesTags: ['AuditMetrics'],
      pollingInterval: 30000,
    }),

    // Administrative actions
    triggerAuditArchival: builder.mutation<{ message: string; status: string }, void>({
      query: () => ({
        url: '/audit/archive',
        method: 'POST',
      }),
      invalidatesTags: ['AuditMetrics'],
    }),

    triggerAuditCompression: builder.mutation<{ message: string; status: string }, void>({
      query: () => ({
        url: '/audit/compress',
        method: 'POST',
      }),
      invalidatesTags: ['AuditMetrics'],
    }),
  }),
})

// Export hooks for components
export const {
  useGetPerformanceMetricsQuery,
  useGetHealthSummaryQuery,
  useGetCacheStatsQuery,
  useGetRecommendationsQuery,
  useGetAuditMetricsQuery,
  useGetSecurityAnalysisQuery,
  useGetAuditStreamStatusQuery,
  useGetAuditRecommendationsQuery,
  useTriggerAuditArchivalMutation,
  useTriggerAuditCompressionMutation,
} = performanceApi

// Optimized selectors for specific data
export const selectPerformanceHealth = (state: any) => {
  const healthSummary = performanceApi.endpoints.getHealthSummary.select()(state)
  return healthSummary?.data?.healthScore || 0
}

export const selectCriticalRecommendations = (state: any) => {
  const recommendations = performanceApi.endpoints.getRecommendations.select()(state)
  return recommendations?.data?.recommendations?.filter(r => r.priority === 'HIGH') || []
}

export const selectSecurityRiskScore = (organizationId: string) => (state: any) => {
  const securityAnalysis = performanceApi.endpoints.getSecurityAnalysis.select({ organizationId })(state)
  return securityAnalysis?.data?.riskScore || 0
}

// Performance monitoring utilities
export const performanceUtils = {
  /**
   * Check if performance metrics indicate healthy system
   */
  isSystemHealthy: (metrics?: PerformanceMetrics): boolean => {
    if (!metrics) return false

    return (
      metrics.memory_usage_mb < 1024 && // Less than 1GB memory
      metrics.database_connections < 16 // Less than 80% of typical pool size
    )
  },

  /**
   * Calculate overall performance score
   */
  calculatePerformanceScore: (
    healthScore: number,
    cacheStats: CacheStats,
    auditMetrics?: AuditMetrics
  ): number => {
    let score = healthScore * 0.5 // 50% weight for health score

    // Add cache performance (30% weight)
    const avgCacheHitRatio = Object.values(cacheStats || {})
      .reduce((sum, cache) => sum + cache.hitRatio, 0) / (Object.keys(cacheStats || {}).length || 1)
    score += avgCacheHitRatio * 100 * 0.3

    // Add audit performance (20% weight)
    if (auditMetrics) {
      const auditScore = Math.max(0, 100 - auditMetrics.average_query_time_ms)
      score += auditScore * 0.2
    }

    return Math.min(100, Math.max(0, score))
  },

  /**
   * Determine alert level based on metrics
   */
  getAlertLevel: (
    healthScore: number,
    securityRiskScore: number,
    systemMetrics?: PerformanceMetrics
  ): 'none' | 'info' | 'warning' | 'critical' => {
    if (securityRiskScore > 80 || healthScore < 30) return 'critical'
    if (securityRiskScore > 60 || healthScore < 50) return 'warning'
    if (securityRiskScore > 40 || healthScore < 70) return 'info'
    return 'none'
  },

  /**
   * Format metrics for display
   */
  formatMetrics: {
    memory: (mb: number): string => {
      if (mb > 1024) return `${(mb / 1024).toFixed(1)}GB`
      return `${mb}MB`
    },

    percentage: (ratio: number): string => `${(ratio * 100).toFixed(1)}%`,

    duration: (ms: number): string => {
      if (ms > 1000) return `${(ms / 1000).toFixed(1)}s`
      return `${ms}ms`
    },

    throughput: (eventsPerSecond: number): string => {
      if (eventsPerSecond > 1000) return `${(eventsPerSecond / 1000).toFixed(1)}k/s`
      return `${eventsPerSecond.toFixed(1)}/s`
    }
  }
}