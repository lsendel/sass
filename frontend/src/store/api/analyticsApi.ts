/**
 * Analytics API Service
 *
 * RTK Query API service for advanced analytics platform supporting:
 * - Metrics definition and data retrieval
 * - Dashboard creation and management
 * - Report generation and scheduling
 * - Real-time analytics streaming
 * - Machine learning insights and predictions
 * - Cross-tenant analytics and reporting
 */

import { createApi } from '@reduxjs/toolkit/query/react'
import { z } from 'zod'

import { createValidatedBaseQuery, createValidatedEndpoint, wrapSuccessResponse } from '@/lib/api/validation'
import type {
  Metric,
  MetricId,
  MetricData,
  Dashboard,
  DashboardId,
  Report,
  ReportId,
  Insight,
  AnalyticsQuery,
  AnalyticsQueryResult,
  AnalyticsConfiguration,
  AnalyticsStream,
  CreateMetricRequest,
  UpdateMetricRequest,
  CreateDashboardRequest,
  UpdateDashboardRequest,
  CreateReportRequest,
  UpdateReportRequest,
  AnalyticsQueryRequest,
  MetricListResponse,
  DashboardListResponse,
  ReportListResponse,
} from '@/types/analytics'
import {
  MetricSchema,
  DashboardSchema,
  ReportSchema,
  AnalyticsQuerySchema,
} from '@/types/analytics'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// Response schemas
const MetricResponseSchema = wrapSuccessResponse(MetricSchema)
const MetricListResponseSchema = wrapSuccessResponse(
  z.object({
    metrics: z.array(MetricSchema),
    total: z.number(),
    page: z.number(),
    pageSize: z.number(),
    hasNext: z.boolean(),
  })
)

const MetricDataResponseSchema = wrapSuccessResponse(
  z.object({
    metricId: z.string(),
    period: z.enum(['hour', 'day', 'week', 'month', 'quarter', 'year']),
    granularity: z.enum(['minute', 'hour', 'day', 'week', 'month']),
    data: z.array(z.object({
      timestamp: z.string(),
      value: z.union([z.number(), z.string()]),
      metadata: z.record(z.union([z.string(), z.number(), z.boolean()])).optional(),
    })),
    aggregations: z.object({
      sum: z.number().optional(),
      avg: z.number().optional(),
      min: z.number().optional(),
      max: z.number().optional(),
      count: z.number().optional(),
      median: z.number().optional(),
      percentiles: z.record(z.number()).optional(),
    }).optional(),
    metadata: z.object({
      totalPoints: z.number(),
      startTime: z.string(),
      endTime: z.string(),
      sampling: z.string().optional(),
      quality: z.enum(['high', 'medium', 'low']),
    }),
  })
)

const DashboardResponseSchema = wrapSuccessResponse(DashboardSchema)
const DashboardListResponseSchema = wrapSuccessResponse(
  z.object({
    dashboards: z.array(DashboardSchema),
    total: z.number(),
    page: z.number(),
    pageSize: z.number(),
    hasNext: z.boolean(),
  })
)

const ReportResponseSchema = wrapSuccessResponse(ReportSchema)
const ReportListResponseSchema = wrapSuccessResponse(
  z.object({
    reports: z.array(ReportSchema),
    total: z.number(),
    page: z.number(),
    pageSize: z.number(),
    hasNext: z.boolean(),
  })
)

const InsightResponseSchema = wrapSuccessResponse(
  z.object({
    id: z.string(),
    type: z.enum(['trend', 'anomaly', 'correlation', 'prediction', 'recommendation']),
    severity: z.enum(['low', 'medium', 'high', 'critical']),
    confidence: z.number().min(0).max(1),
    title: z.string(),
    description: z.string(),
    summary: z.string(),
    metricIds: z.array(z.string()),
    timeRange: z.object({
      start: z.string(),
      end: z.string(),
    }),
    model: z.object({
      name: z.string(),
      version: z.string(),
      accuracy: z.number().optional(),
      lastTrained: z.string().optional(),
    }).optional(),
    recommendations: z.array(z.object({
      action: z.string(),
      impact: z.enum(['low', 'medium', 'high']),
      effort: z.enum(['low', 'medium', 'high']),
      description: z.string(),
      resources: z.array(z.string()).optional(),
    })).optional(),
    generatedAt: z.string(),
    expiresAt: z.string().optional(),
    viewedAt: z.string().optional(),
    acknowledged: z.boolean().optional(),
  })
)

const AnalyticsQueryResultSchema = wrapSuccessResponse(
  z.object({
    query: AnalyticsQuerySchema,
    data: z.array(MetricDataResponseSchema.shape.data),
    insights: z.array(InsightResponseSchema.shape.data),
    executionTime: z.number(),
    cached: z.boolean(),
    nextPageToken: z.string().optional(),
  })
)

export const analyticsApi = createApi({
  reducerPath: 'analyticsApi',
  baseQuery: createValidatedBaseQuery(`${API_BASE_URL}/analytics`),
  tagTypes: [
    'Metric',
    'MetricList',
    'MetricData',
    'Dashboard',
    'DashboardList',
    'Report',
    'ReportList',
    'Insight',
    'AnalyticsQuery',
    'AnalyticsStream',
    'AnalyticsConfiguration',
  ],
  endpoints: (builder) => ({
    // Metrics management
    getMetric: builder.query<Metric, { metricId: MetricId }>({
      ...createValidatedEndpoint(MetricResponseSchema, {
        query: ({ metricId }) => `/metrics/${metricId}`,
      }),
      providesTags: (result, error, { metricId }) => [
        { type: 'Metric', id: metricId },
      ],
    }),

    listMetrics: builder.query<MetricListResponse, {
      page?: number
      pageSize?: number
      category?: string
      type?: string
      search?: string
      tags?: string[]
      sortBy?: string
      sortOrder?: 'asc' | 'desc'
    }>({
      ...createValidatedEndpoint(MetricListResponseSchema, {
        query: (params) => ({
          url: '/metrics',
          params: {
            page: params.page || 1,
            pageSize: params.pageSize || 20,
            ...(params.category && { category: params.category }),
            ...(params.type && { type: params.type }),
            ...(params.search && { search: params.search }),
            ...(params.tags && { tags: params.tags.join(',') }),
            ...(params.sortBy && { sortBy: params.sortBy }),
            ...(params.sortOrder && { sortOrder: params.sortOrder }),
          },
        }),
      }),
      providesTags: ['MetricList'],
    }),

    createMetric: builder.mutation<Metric, CreateMetricRequest>({
      ...createValidatedEndpoint(MetricResponseSchema, {
        query: (createRequest) => {
          MetricSchema.parse(createRequest)
          return {
            url: '/metrics',
            method: 'POST',
            body: createRequest,
          }
        },
      }),
      invalidatesTags: ['MetricList'],
    }),

    updateMetric: builder.mutation<Metric, { metricId: MetricId; updates: UpdateMetricRequest }>({
      ...createValidatedEndpoint(MetricResponseSchema, {
        query: ({ metricId, updates }) => ({
          url: `/metrics/${metricId}`,
          method: 'PATCH',
          body: updates,
        }),
      }),
      invalidatesTags: (result, error, { metricId }) => [
        { type: 'Metric', id: metricId },
        'MetricList',
      ],
    }),

    deleteMetric: builder.mutation<void, { metricId: MetricId }>({
      query: ({ metricId }) => ({
        url: `/metrics/${metricId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, { metricId }) => [
        { type: 'Metric', id: metricId },
        'MetricList',
      ],
    }),

    // Metric data retrieval
    getMetricData: builder.query<MetricData, {
      metricId: MetricId
      timeRange: {
        start: string
        end: string
        granularity: 'minute' | 'hour' | 'day' | 'week' | 'month'
      }
      filters?: Record<string, string | number | boolean | string[]>
      groupBy?: string[]
    }>({
      ...createValidatedEndpoint(MetricDataResponseSchema, {
        query: ({ metricId, timeRange, filters, groupBy }) => ({
          url: `/metrics/${metricId}/data`,
          params: {
            start: timeRange.start,
            end: timeRange.end,
            granularity: timeRange.granularity,
            ...(filters && { filters: JSON.stringify(filters) }),
            ...(groupBy && { groupBy: groupBy.join(',') }),
          },
        }),
      }),
      providesTags: (result, error, { metricId, timeRange }) => [
        { type: 'MetricData', id: `${metricId}-${timeRange.start}-${timeRange.end}` },
      ],
    }),

    // Analytics queries
    executeAnalyticsQuery: builder.mutation<AnalyticsQueryResult, AnalyticsQueryRequest>({
      ...createValidatedEndpoint(AnalyticsQueryResultSchema, {
        query: (queryRequest) => {
          AnalyticsQuerySchema.parse(queryRequest)
          return {
            url: '/query',
            method: 'POST',
            body: queryRequest,
          }
        },
      }),
      invalidatesTags: ['AnalyticsQuery'],
    }),

    // Dashboard management
    getDashboard: builder.query<Dashboard, { dashboardId: DashboardId }>({
      ...createValidatedEndpoint(DashboardResponseSchema, {
        query: ({ dashboardId }) => `/dashboards/${dashboardId}`,
      }),
      providesTags: (result, error, { dashboardId }) => [
        { type: 'Dashboard', id: dashboardId },
      ],
    }),

    listDashboards: builder.query<DashboardListResponse, {
      page?: number
      pageSize?: number
      category?: string
      visibility?: string
      search?: string
      tags?: string[]
      sortBy?: string
      sortOrder?: 'asc' | 'desc'
    }>({
      ...createValidatedEndpoint(DashboardListResponseSchema, {
        query: (params) => ({
          url: '/dashboards',
          params: {
            page: params.page || 1,
            pageSize: params.pageSize || 20,
            ...(params.category && { category: params.category }),
            ...(params.visibility && { visibility: params.visibility }),
            ...(params.search && { search: params.search }),
            ...(params.tags && { tags: params.tags.join(',') }),
            ...(params.sortBy && { sortBy: params.sortBy }),
            ...(params.sortOrder && { sortOrder: params.sortOrder }),
          },
        }),
      }),
      providesTags: ['DashboardList'],
    }),

    createDashboard: builder.mutation<Dashboard, CreateDashboardRequest>({
      ...createValidatedEndpoint(DashboardResponseSchema, {
        query: (createRequest) => {
          DashboardSchema.parse(createRequest)
          return {
            url: '/dashboards',
            method: 'POST',
            body: createRequest,
          }
        },
      }),
      invalidatesTags: ['DashboardList'],
    }),

    updateDashboard: builder.mutation<Dashboard, { dashboardId: DashboardId; updates: UpdateDashboardRequest }>({
      ...createValidatedEndpoint(DashboardResponseSchema, {
        query: ({ dashboardId, updates }) => ({
          url: `/dashboards/${dashboardId}`,
          method: 'PATCH',
          body: updates,
        }),
      }),
      invalidatesTags: (result, error, { dashboardId }) => [
        { type: 'Dashboard', id: dashboardId },
        'DashboardList',
      ],
    }),

    deleteDashboard: builder.mutation<void, { dashboardId: DashboardId }>({
      query: ({ dashboardId }) => ({
        url: `/dashboards/${dashboardId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, { dashboardId }) => [
        { type: 'Dashboard', id: dashboardId },
        'DashboardList',
      ],
    }),

    cloneDashboard: builder.mutation<Dashboard, { dashboardId: DashboardId; name: string }>({
      ...createValidatedEndpoint(DashboardResponseSchema, {
        query: ({ dashboardId, name }) => ({
          url: `/dashboards/${dashboardId}/clone`,
          method: 'POST',
          body: { name },
        }),
      }),
      invalidatesTags: ['DashboardList'],
    }),

    shareDashboard: builder.mutation<Dashboard, {
      dashboardId: DashboardId
      shareWith: string[]
      permissions: string[]
    }>({
      ...createValidatedEndpoint(DashboardResponseSchema, {
        query: ({ dashboardId, shareWith, permissions }) => ({
          url: `/dashboards/${dashboardId}/share`,
          method: 'POST',
          body: { shareWith, permissions },
        }),
      }),
      invalidatesTags: (result, error, { dashboardId }) => [
        { type: 'Dashboard', id: dashboardId },
      ],
    }),

    // Report management
    getReport: builder.query<Report, { reportId: ReportId }>({
      ...createValidatedEndpoint(ReportResponseSchema, {
        query: ({ reportId }) => `/reports/${reportId}`,
      }),
      providesTags: (result, error, { reportId }) => [
        { type: 'Report', id: reportId },
      ],
    }),

    listReports: builder.query<ReportListResponse, {
      page?: number
      pageSize?: number
      category?: string
      type?: string
      status?: string
      search?: string
      sortBy?: string
      sortOrder?: 'asc' | 'desc'
    }>({
      ...createValidatedEndpoint(ReportListResponseSchema, {
        query: (params) => ({
          url: '/reports',
          params: {
            page: params.page || 1,
            pageSize: params.pageSize || 20,
            ...(params.category && { category: params.category }),
            ...(params.type && { type: params.type }),
            ...(params.status && { status: params.status }),
            ...(params.search && { search: params.search }),
            ...(params.sortBy && { sortBy: params.sortBy }),
            ...(params.sortOrder && { sortOrder: params.sortOrder }),
          },
        }),
      }),
      providesTags: ['ReportList'],
    }),

    createReport: builder.mutation<Report, CreateReportRequest>({
      ...createValidatedEndpoint(ReportResponseSchema, {
        query: (createRequest) => {
          ReportSchema.parse(createRequest)
          return {
            url: '/reports',
            method: 'POST',
            body: createRequest,
          }
        },
      }),
      invalidatesTags: ['ReportList'],
    }),

    updateReport: builder.mutation<Report, { reportId: ReportId; updates: UpdateReportRequest }>({
      ...createValidatedEndpoint(ReportResponseSchema, {
        query: ({ reportId, updates }) => ({
          url: `/reports/${reportId}`,
          method: 'PATCH',
          body: updates,
        }),
      }),
      invalidatesTags: (result, error, { reportId }) => [
        { type: 'Report', id: reportId },
        'ReportList',
      ],
    }),

    deleteReport: builder.mutation<void, { reportId: ReportId }>({
      query: ({ reportId }) => ({
        url: `/reports/${reportId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (result, error, { reportId }) => [
        { type: 'Report', id: reportId },
        'ReportList',
      ],
    }),

    runReport: builder.mutation<{ reportId: ReportId; runId: string; downloadUrl?: string }, {
      reportId: ReportId
      parameters?: Record<string, string | number | boolean>
    }>({
      query: ({ reportId, parameters }) => ({
        url: `/reports/${reportId}/run`,
        method: 'POST',
        body: { parameters },
      }),
      invalidatesTags: (result, error, { reportId }) => [
        { type: 'Report', id: reportId },
      ],
    }),

    scheduleReport: builder.mutation<Report, {
      reportId: ReportId
      schedule: {
        frequency: string
        interval: number
        startDate: string
        endDate?: string
        timezone: string
      }
    }>({
      ...createValidatedEndpoint(ReportResponseSchema, {
        query: ({ reportId, schedule }) => ({
          url: `/reports/${reportId}/schedule`,
          method: 'POST',
          body: schedule,
        }),
      }),
      invalidatesTags: (result, error, { reportId }) => [
        { type: 'Report', id: reportId },
      ],
    }),

    // Insights and ML predictions
    getInsights: builder.query<Insight[], {
      metricIds?: MetricId[]
      type?: string
      severity?: string
      acknowledged?: boolean
      limit?: number
    }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(InsightResponseSchema.shape.data)), {
        query: (params) => ({
          url: '/insights',
          params: {
            ...(params.metricIds && { metricIds: params.metricIds.join(',') }),
            ...(params.type && { type: params.type }),
            ...(params.severity && { severity: params.severity }),
            ...(params.acknowledged !== undefined && { acknowledged: params.acknowledged }),
            ...(params.limit && { limit: params.limit }),
          },
        }),
      }),
      providesTags: ['Insight'],
    }),

    acknowledgeInsight: builder.mutation<Insight, { insightId: string }>({
      ...createValidatedEndpoint(InsightResponseSchema, {
        query: ({ insightId }) => ({
          url: `/insights/${insightId}/acknowledge`,
          method: 'POST',
        }),
      }),
      invalidatesTags: ['Insight'],
    }),

    generateInsights: builder.mutation<Insight[], {
      metricIds: MetricId[]
      timeRange: {
        start: string
        end: string
      }
      types?: string[]
    }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.array(InsightResponseSchema.shape.data)), {
        query: ({ metricIds, timeRange, types }) => ({
          url: '/insights/generate',
          method: 'POST',
          body: {
            metricIds,
            timeRange,
            types,
          },
        }),
      }),
      invalidatesTags: ['Insight'],
    }),

    // Real-time analytics streams
    getAnalyticsStreams: builder.query<AnalyticsStream[], void>({
      query: () => '/streams',
      providesTags: ['AnalyticsStream'],
    }),

    createAnalyticsStream: builder.mutation<AnalyticsStream, Omit<AnalyticsStream, 'id'>>({
      query: (streamConfig) => ({
        url: '/streams',
        method: 'POST',
        body: streamConfig,
      }),
      invalidatesTags: ['AnalyticsStream'],
    }),

    updateAnalyticsStream: builder.mutation<AnalyticsStream, {
      streamId: string
      updates: Partial<AnalyticsStream>
    }>({
      query: ({ streamId, updates }) => ({
        url: `/streams/${streamId}`,
        method: 'PATCH',
        body: updates,
      }),
      invalidatesTags: ['AnalyticsStream'],
    }),

    deleteAnalyticsStream: builder.mutation<void, { streamId: string }>({
      query: ({ streamId }) => ({
        url: `/streams/${streamId}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['AnalyticsStream'],
    }),

    // Analytics configuration
    getAnalyticsConfiguration: builder.query<AnalyticsConfiguration, void>({
      query: () => '/configuration',
      providesTags: ['AnalyticsConfiguration'],
    }),

    updateAnalyticsConfiguration: builder.mutation<AnalyticsConfiguration, Partial<AnalyticsConfiguration>>({
      query: (updates) => ({
        url: '/configuration',
        method: 'PATCH',
        body: updates,
      }),
      invalidatesTags: ['AnalyticsConfiguration'],
    }),

    // Export capabilities
    exportMetricData: builder.mutation<{ exportId: string; downloadUrl: string }, {
      metricIds: MetricId[]
      timeRange: {
        start: string
        end: string
      }
      format: 'csv' | 'json' | 'excel'
      includeMetadata?: boolean
    }>({
      query: ({ metricIds, timeRange, format, includeMetadata = false }) => ({
        url: '/export/metrics',
        method: 'POST',
        body: {
          metricIds,
          timeRange,
          format,
          includeMetadata,
        },
      }),
    }),

    exportDashboard: builder.mutation<{ exportId: string; downloadUrl: string }, {
      dashboardId: DashboardId
      format: 'pdf' | 'png' | 'json'
      options?: {
        includeData?: boolean
        theme?: 'light' | 'dark'
        resolution?: 'low' | 'medium' | 'high'
      }
    }>({
      query: ({ dashboardId, format, options }) => ({
        url: `/export/dashboards/${dashboardId}`,
        method: 'POST',
        body: {
          format,
          options,
        },
      }),
    }),

    // Health and status
    getAnalyticsHealth: builder.query<{
      status: 'healthy' | 'degraded' | 'unhealthy'
      components: Record<string, {
        status: 'healthy' | 'degraded' | 'unhealthy'
        lastCheck: string
        details?: string
      }>
    }, void>({
      query: () => '/health',
    }),
  }),
})

// Export hooks
export const {
  useGetMetricQuery,
  useListMetricsQuery,
  useCreateMetricMutation,
  useUpdateMetricMutation,
  useDeleteMetricMutation,
  useGetMetricDataQuery,
  useExecuteAnalyticsQueryMutation,
  useGetDashboardQuery,
  useListDashboardsQuery,
  useCreateDashboardMutation,
  useUpdateDashboardMutation,
  useDeleteDashboardMutation,
  useCloneDashboardMutation,
  useShareDashboardMutation,
  useGetReportQuery,
  useListReportsQuery,
  useCreateReportMutation,
  useUpdateReportMutation,
  useDeleteReportMutation,
  useRunReportMutation,
  useScheduleReportMutation,
  useGetInsightsQuery,
  useAcknowledgeInsightMutation,
  useGenerateInsightsMutation,
  useGetAnalyticsStreamsQuery,
  useCreateAnalyticsStreamMutation,
  useUpdateAnalyticsStreamMutation,
  useDeleteAnalyticsStreamMutation,
  useGetAnalyticsConfigurationQuery,
  useUpdateAnalyticsConfigurationMutation,
  useExportMetricDataMutation,
  useExportDashboardMutation,
  useGetAnalyticsHealthQuery,
} = analyticsApi

// Utility selectors
export const selectMetricById = (metricId: MetricId) =>
  analyticsApi.endpoints.getMetric.select({ metricId })

export const selectDashboardById = (dashboardId: DashboardId) =>
  analyticsApi.endpoints.getDashboard.select({ dashboardId })

export const selectReportById = (reportId: ReportId) =>
  analyticsApi.endpoints.getReport.select({ reportId })

export const selectMetricData = (metricId: MetricId, timeRange: any) =>
  analyticsApi.endpoints.getMetricData.select({ metricId, timeRange })

// Advanced selectors for analytics context
export const createAnalyticsContextSelector = () => (state: any) => {
  const insights = analyticsApi.endpoints.getInsights.select({})(state)
  const configuration = analyticsApi.endpoints.getAnalyticsConfiguration.select()(state)
  const health = analyticsApi.endpoints.getAnalyticsHealth.select()(state)

  return {
    insights: insights.data || [],
    configuration: configuration.data,
    health: health.data,
    isLoading: insights.isLoading || configuration.isLoading || health.isLoading,
    hasUnacknowledgedInsights: insights.data?.some(insight => !insight.acknowledged) || false,
  }
}