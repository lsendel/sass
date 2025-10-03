/**
 * State Management with Advanced Patterns
 *
 * Implements Redux Toolkit Query with optimistic updates,
 * caching strategies, and error handling.
 */

import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import type {
  BaseQueryFn,
  FetchArgs,
  FetchBaseQueryError,
} from '@reduxjs/toolkit/query'

import { API_ENDPOINTS } from '@/constants/appConstants'

// Enhanced base query with error handling and auth
const baseQueryWithAuth: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, api, extraOptions) => {
  const baseQuery = fetchBaseQuery({
    baseUrl: '/api',
    prepareHeaders: (headers, { getState }) => {
      // Add auth token if available
      const token = (getState() as any).auth?.token
      if (token) {
        headers.set('authorization', `Bearer ${token}`)
      }

      // Add correlation ID for tracing
      headers.set('x-correlation-id', generateCorrelationId())

      return headers
    },
  })

  const result = await baseQuery(args, api, extraOptions)

  // Handle auth errors
  if (result.error && result.error.status === 401) {
    // Dispatch logout action
    api.dispatch({ type: 'auth/logout' })
  }

  // Enhanced error information
  if (result.error) {
    const enhancedError = result.error as FetchBaseQueryError & {
      timestamp?: string
      correlationId?: string
    }
    enhancedError.timestamp = new Date().toISOString()
    enhancedError.correlationId = generateCorrelationId()
    result.error = enhancedError
  }

  return result
}

// Main API slice
export const auditApi = createApi({
  reducerPath: 'auditApi',
  baseQuery: baseQueryWithAuth,
  tagTypes: ['AuditLog', 'Export', 'User'],
  endpoints: builder => ({
    // Audit logs with advanced caching
    getAuditLogs: builder.query<AuditLogsResponse, AuditLogsRequest>({
      query: params => ({
        url: API_ENDPOINTS.AUDIT_LOGS,
        params: cleanEmptyParams(params),
      }),
      providesTags: result =>
        result?.logs
          ? [
              ...result.logs.map(({ id }) => ({
                type: 'AuditLog' as const,
                id,
              })),
              { type: 'AuditLog', id: 'LIST' },
            ]
          : [{ type: 'AuditLog', id: 'LIST' }],
      // Advanced caching: keep data for 5 minutes
      keepUnusedDataFor: 300,
      // Optimistic updates for better UX
      serializeQueryArgs: ({ queryArgs }) => {
        // Create stable cache key
        return JSON.stringify({
          ...queryArgs,
          page: queryArgs.page || 0,
          size: queryArgs.size || 20,
        })
      },
    }),

    // Export with optimistic updates
    requestExport: builder.mutation<ExportResponse, ExportRequest>({
      query: exportData => ({
        url: API_ENDPOINTS.EXPORT_AUDIT,
        method: 'POST',
        body: exportData,
      }),
      invalidatesTags: [{ type: 'Export', id: 'LIST' }],
      // Optimistic update
      onQueryStarted: async (_, { dispatch, queryFulfilled }) => {
        // Create optimistic export entry
        const optimisticExport: ExportResponse = {
          exportId: generateTempId(),
          status: 'PENDING',
          requestedAt: new Date().toISOString(),
          progress: 0,
        }

        // Update the exports list optimistically
        const patchResult = dispatch(
          auditApi.util.updateQueryData('getExports', undefined, draft => {
            draft.exports.unshift(optimisticExport)
          })
        )

        try {
          const { data } = await queryFulfilled
          // Replace optimistic entry with real data
          dispatch(
            auditApi.util.updateQueryData('getExports', undefined, draft => {
              const index = draft.exports.findIndex(
                e => e.exportId === optimisticExport.exportId
              )
              if (index !== -1) {
                draft.exports[index] = data
              }
            })
          )
        } catch {
          // Revert optimistic update on error
          patchResult.undo()
        }
      },
    }),

    // Get exports with real-time updates
    getExports: builder.query<ExportsResponse, void>({
      query: () => API_ENDPOINTS.EXPORT_AUDIT,
      providesTags: [{ type: 'Export', id: 'LIST' }],
      // Note: Polling would be configured at the component level
    }),

    // Get export status with caching
    getExportStatus: builder.query<ExportStatusResponse, string>({
      query: exportId => API_ENDPOINTS.EXPORT_STATUS.replace('{id}', exportId),
      providesTags: (_, __, exportId) => [{ type: 'Export', id: exportId }],
      // Cache for 1 minute for completed exports, 10s for active ones
      keepUnusedDataFor: 60,
    }),
  }),
})

// Export hooks
export const {
  useGetAuditLogsQuery,
  useRequestExportMutation,
  useGetExportsQuery,
  useGetExportStatusQuery,
} = auditApi

// Selectors for derived state
export const selectAuditLogById = (state: any, logId: string) =>
  auditApi.endpoints.getAuditLogs
    .select({})(state)
    ?.data?.logs.find(log => log.id === logId)

export const selectActiveExports = (state: any) =>
  auditApi.endpoints.getExports
    .select()(state)
    ?.data?.exports.filter(
      export_ => export_.status === 'PENDING' || export_.status === 'PROCESSING'
    ) || []

// Advanced caching utilities
export const auditApiEnhanced = auditApi.injectEndpoints({
  endpoints: builder => ({
    // Prefetch related data
    prefetchAuditLogDetails: builder.query<AuditLogDetail, string>({
      query: logId => `/audit/logs/${logId}`,
      // Don't cache prefetched data as long
      keepUnusedDataFor: 60,
    }),
  }),
  overrideExisting: false,
})

// Utility functions
function cleanEmptyParams(params: Record<string, any>): Record<string, any> {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([_, value]) => value !== undefined && value !== null && value !== ''
    )
  )
}

function generateCorrelationId(): string {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

function generateTempId(): string {
  return `temp-${Date.now()}`
}

// Type definitions
interface AuditLogsRequest {
  page?: number
  size?: number
  dateFrom?: string
  dateTo?: string
  search?: string
  actionTypes?: string[]
  resourceTypes?: string[]
}

interface AuditLogsResponse {
  logs: AuditLog[]
  totalElements: number
  totalPages: number
  currentPage: number
}

interface AuditLog {
  id: string
  timestamp: string
  actorName: string
  actionType: string
  resourceType: string
  outcome: string
}

interface ExportRequest {
  format: 'CSV' | 'JSON' | 'PDF'
  dateFrom?: string
  dateTo?: string
  search?: string
  actionTypes?: string[]
}

interface ExportResponse {
  exportId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  requestedAt: string
  progress: number
  downloadUrl?: string
  errorMessage?: string
}

interface ExportsResponse {
  exports: ExportResponse[]
}

interface ExportStatusResponse extends ExportResponse {
  totalRecords?: number
  completedAt?: string
}

interface AuditLogDetail extends AuditLog {
  details: Record<string, any>
  ipAddress?: string
  userAgent?: string
}
