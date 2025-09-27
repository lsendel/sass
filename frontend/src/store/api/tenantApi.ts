/**
 * Tenant Management API
 *
 * RTK Query API service for multi-tenant operations supporting:
 * - Tenant CRUD operations with validation
 * - Tenant hierarchy management
 * - Cross-tenant analytics and reporting
 * - Tenant-aware routing and middleware
 * - Resource quota management
 */

import { createApi } from '@reduxjs/toolkit/query/react'
import { z } from 'zod'

import { createValidatedBaseQuery, createValidatedEndpoint, wrapSuccessResponse } from '@/lib/api/validation'
import type {
  Tenant,
  TenantId,
  TenantSlug,
  TenantContext,
  TenantAnalytics,
  TenantSettings,
  TenantListResponse,
  TenantCreationResponse,
  CrossTenantQuery,
  CrossTenantResult,
  CreateTenantRequest,
  UpdateTenantRequest,
  TenantAnalyticsRequest,
} from '@/types/multitenancy'
import {
  TenantSchema,
  CreateTenantRequestSchema,
  UpdateTenantRequestSchema,
  TenantAnalyticsRequestSchema,
} from '@/types/multitenancy'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// Response schemas
const TenantResponseSchema = wrapSuccessResponse(TenantSchema)
const TenantListResponseSchema = wrapSuccessResponse(
  z.object({
    tenants: z.array(TenantSchema),
    total: z.number(),
    page: z.number(),
    pageSize: z.number(),
    hasNext: z.boolean(),
  })
)

const TenantCreationResponseSchema = wrapSuccessResponse(
  z.object({
    tenant: TenantSchema,
    setupUrl: z.string().url(),
    adminCredentials: z.object({
      username: z.string(),
      temporaryPassword: z.string(),
    }).optional(),
  })
)

const TenantAnalyticsResponseSchema = wrapSuccessResponse(
  z.object({
    tenantId: z.string(),
    period: z.enum(['day', 'week', 'month', 'quarter', 'year']),
    usage: z.object({
      activeUsers: z.number(),
      totalSessions: z.number(),
      apiCalls: z.number(),
      storageUsed: z.number(),
      bandwidthUsed: z.number(),
    }),
    business: z.object({
      revenue: z.number(),
      subscriptions: z.number(),
      churnRate: z.number(),
      growthRate: z.number(),
    }),
    performance: z.object({
      avgResponseTime: z.number(),
      errorRate: z.number(),
      uptime: z.number(),
    }),
    security: z.object({
      failedLogins: z.number(),
      suspiciousActivity: z.number(),
      mfaAdoption: z.number(),
    }),
  })
)

const TenantSettingsResponseSchema = wrapSuccessResponse(
  z.object({
    tenantId: z.string(),
    general: z.object({
      allowUserRegistration: z.boolean(),
      defaultUserRole: z.string(),
      emailVerificationRequired: z.boolean(),
      userInviteExpirationDays: z.number(),
    }),
    security: z.object({
      passwordPolicy: z.object({
        minLength: z.number(),
        requireUppercase: z.boolean(),
        requireLowercase: z.boolean(),
        requireNumbers: z.boolean(),
        requireSpecialChars: z.boolean(),
        preventReuse: z.number(),
        expirationDays: z.number().optional(),
      }),
      sessionSettings: z.object({
        timeoutMinutes: z.number(),
        maxConcurrentSessions: z.number(),
        requireReauthForSensitive: z.boolean(),
      }),
      auditSettings: z.object({
        enabled: z.boolean(),
        retentionDays: z.number(),
        logLevel: z.enum(['basic', 'detailed', 'comprehensive']),
        realTimeAlerts: z.boolean(),
      }),
    }),
    integrations: z.object({
      stripe: z.object({
        publishableKey: z.string(),
        webhookSecret: z.string(),
        enabledPaymentMethods: z.array(z.string()),
        currency: z.string(),
      }),
      oauth: z.object({
        providers: z.array(z.object({
          name: z.string(),
          clientId: z.string(),
          enabled: z.boolean(),
          scopes: z.array(z.string()),
        })),
        allowedDomains: z.array(z.string()).optional(),
        autoProvision: z.boolean(),
      }),
      smtp: z.object({
        host: z.string(),
        port: z.number(),
        username: z.string(),
        fromAddress: z.string(),
        encryption: z.enum(['none', 'tls', 'ssl']),
      }),
    }),
    notifications: z.object({
      emailNotifications: z.object({
        welcomeEmails: z.boolean(),
        billingNotifications: z.boolean(),
        securityAlerts: z.boolean(),
        systemUpdates: z.boolean(),
      }),
      webhookNotifications: z.object({
        endpoints: z.array(z.object({
          url: z.string().url(),
          events: z.array(z.string()),
          active: z.boolean(),
          secret: z.string(),
        })),
        retryPolicy: z.object({
          maxRetries: z.number(),
          retryDelayMs: z.number(),
          backoffMultiplier: z.number(),
        }),
      }),
    }),
  })
)

const CrossTenantResultSchema = wrapSuccessResponse(
  z.array(z.object({
    tenantId: z.string(),
    data: z.any(),
    metadata: z.object({
      executionTime: z.number(),
      recordCount: z.number(),
      hasMore: z.boolean(),
    }),
  }))
)

export const tenantApi = createApi({
  reducerPath: 'tenantApi',
  baseQuery: createValidatedBaseQuery(`${API_BASE_URL}/tenants`),
  tagTypes: [
    'Tenant',
    'TenantList',
    'TenantSettings',
    'TenantAnalytics',
    'TenantQuotas',
    'TenantHierarchy',
    'CrossTenantData',
  ],
  endpoints: (builder) => ({
    // Tenant CRUD operations
    getTenant: builder.query<Tenant, { tenantId: TenantId }>({
      ...createValidatedEndpoint(TenantResponseSchema, {
        query: ({ tenantId }) => `/${tenantId}`,
      }),
      providesTags: (result, error, { tenantId }) => [
        { type: 'Tenant', id: tenantId },
      ],
    }),

    getTenantBySlug: builder.query<Tenant, { slug: TenantSlug }>({
      ...createValidatedEndpoint(TenantResponseSchema, {
        query: ({ slug }) => `/slug/${slug}`,
      }),
      providesTags: (result, error, { slug }) => [
        { type: 'Tenant', id: slug },
      ],
    }),

    listTenants: builder.query<TenantListResponse, {
      page?: number
      pageSize?: number
      status?: string
      tier?: string
      search?: string
      sortBy?: string
      sortOrder?: 'asc' | 'desc'
      parentTenantId?: TenantId
    }>({
      ...createValidatedEndpoint(TenantListResponseSchema, {
        query: (params) => ({
          url: '',
          params: {
            page: params.page || 1,
            pageSize: params.pageSize || 20,
            ...(params.status && { status: params.status }),
            ...(params.tier && { tier: params.tier }),
            ...(params.search && { search: params.search }),
            ...(params.sortBy && { sortBy: params.sortBy }),
            ...(params.sortOrder && { sortOrder: params.sortOrder }),
            ...(params.parentTenantId && { parentTenantId: params.parentTenantId }),
          },
        }),
      }),
      providesTags: ['TenantList'],
    }),

    createTenant: builder.mutation<TenantCreationResponse, CreateTenantRequest>({
      ...createValidatedEndpoint(TenantCreationResponseSchema, {
        query: (createRequest) => {
          CreateTenantRequestSchema.parse(createRequest)
          return {
            url: '',
            method: 'POST',
            body: createRequest,
          }
        },
      }),
      invalidatesTags: ['TenantList'],
    }),

    updateTenant: builder.mutation<Tenant, { tenantId: TenantId; updates: UpdateTenantRequest }>({
      ...createValidatedEndpoint(TenantResponseSchema, {
        query: ({ tenantId, updates }) => {
          UpdateTenantRequestSchema.parse(updates)
          return {
            url: `/${tenantId}`,
            method: 'PATCH',
            body: updates,
          }
        },
      }),
      invalidatesTags: (result, error, { tenantId }) => [
        { type: 'Tenant', id: tenantId },
        'TenantList',
      ],
    }),

    deleteTenant: builder.mutation<void, { tenantId: TenantId; cascade?: boolean }>({
      query: ({ tenantId, cascade = false }) => ({
        url: `/${tenantId}`,
        method: 'DELETE',
        params: { cascade },
      }),
      invalidatesTags: (result, error, { tenantId }) => [
        { type: 'Tenant', id: tenantId },
        'TenantList',
        'TenantHierarchy',
      ],
    }),

    // Tenant settings management
    getTenantSettings: builder.query<TenantSettings, { tenantId: TenantId }>({
      ...createValidatedEndpoint(TenantSettingsResponseSchema, {
        query: ({ tenantId }) => `/${tenantId}/settings`,
      }),
      providesTags: (result, error, { tenantId }) => [
        { type: 'TenantSettings', id: tenantId },
      ],
    }),

    updateTenantSettings: builder.mutation<TenantSettings, {
      tenantId: TenantId
      settings: Partial<TenantSettings>
    }>({
      ...createValidatedEndpoint(TenantSettingsResponseSchema, {
        query: ({ tenantId, settings }) => ({
          url: `/${tenantId}/settings`,
          method: 'PATCH',
          body: settings,
        }),
      }),
      invalidatesTags: (result, error, { tenantId }) => [
        { type: 'TenantSettings', id: tenantId },
        { type: 'Tenant', id: tenantId },
      ],
    }),

    // Tenant analytics and reporting
    getTenantAnalytics: builder.query<TenantAnalytics, {
      tenantId: TenantId
      period: 'day' | 'week' | 'month' | 'quarter' | 'year'
      startDate?: string
      endDate?: string
      metrics?: string[]
    }>({
      ...createValidatedEndpoint(TenantAnalyticsResponseSchema, {
        query: ({ tenantId, period, startDate, endDate, metrics }) => ({
          url: `/${tenantId}/analytics`,
          params: {
            period,
            ...(startDate && { startDate }),
            ...(endDate && { endDate }),
            ...(metrics && { metrics: metrics.join(',') }),
          },
        }),
      }),
      providesTags: (result, error, { tenantId, period }) => [
        { type: 'TenantAnalytics', id: `${tenantId}-${period}` },
      ],
    }),

    // Cross-tenant operations
    executeCrossTenantQuery: builder.mutation<CrossTenantResult[], CrossTenantQuery>({
      ...createValidatedEndpoint(CrossTenantResultSchema, {
        query: (query) => ({
          url: '/cross-tenant/query',
          method: 'POST',
          body: query,
        }),
      }),
      invalidatesTags: ['CrossTenantData'],
    }),

    // Tenant hierarchy operations
    getTenantHierarchy: builder.query<{ tenant: Tenant; children: Tenant[]; ancestors: Tenant[] }, {
      tenantId: TenantId
    }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        tenant: TenantSchema,
        children: z.array(TenantSchema),
        ancestors: z.array(TenantSchema),
      })), {
        query: ({ tenantId }) => `/${tenantId}/hierarchy`,
      }),
      providesTags: (result, error, { tenantId }) => [
        { type: 'TenantHierarchy', id: tenantId },
      ],
    }),

    moveTenant: builder.mutation<Tenant, {
      tenantId: TenantId
      newParentId?: TenantId
    }>({
      ...createValidatedEndpoint(TenantResponseSchema, {
        query: ({ tenantId, newParentId }) => ({
          url: `/${tenantId}/move`,
          method: 'POST',
          body: { newParentId },
        }),
      }),
      invalidatesTags: (result, error, { tenantId }) => [
        { type: 'Tenant', id: tenantId },
        'TenantList',
        'TenantHierarchy',
      ],
    }),

    // Tenant resource management
    getTenantQuotas: builder.query<{ tenantId: TenantId; quotas: any }, { tenantId: TenantId }>({
      ...createValidatedEndpoint(wrapSuccessResponse(z.object({
        tenantId: z.string(),
        quotas: z.any(), // TenantQuotas type
      })), {
        query: ({ tenantId }) => `/${tenantId}/quotas`,
      }),
      providesTags: (result, error, { tenantId }) => [
        { type: 'TenantQuotas', id: tenantId },
      ],
    }),

    updateTenantQuotas: builder.mutation<any, {
      tenantId: TenantId
      quotas: any // Partial<TenantQuotas>
    }>({
      query: ({ tenantId, quotas }) => ({
        url: `/${tenantId}/quotas`,
        method: 'PATCH',
        body: quotas,
      }),
      invalidatesTags: (result, error, { tenantId }) => [
        { type: 'TenantQuotas', id: tenantId },
        { type: 'Tenant', id: tenantId },
      ],
    }),

    // Tenant status operations
    suspendTenant: builder.mutation<Tenant, { tenantId: TenantId; reason: string }>({
      ...createValidatedEndpoint(TenantResponseSchema, {
        query: ({ tenantId, reason }) => ({
          url: `/${tenantId}/suspend`,
          method: 'POST',
          body: { reason },
        }),
      }),
      invalidatesTags: (result, error, { tenantId }) => [
        { type: 'Tenant', id: tenantId },
        'TenantList',
      ],
    }),

    activateTenant: builder.mutation<Tenant, { tenantId: TenantId }>({
      ...createValidatedEndpoint(TenantResponseSchema, {
        query: ({ tenantId }) => ({
          url: `/${tenantId}/activate`,
          method: 'POST',
        }),
      }),
      invalidatesTags: (result, error, { tenantId }) => [
        { type: 'Tenant', id: tenantId },
        'TenantList',
      ],
    }),

    // Tenant domain management
    validateCustomDomain: builder.mutation<{ valid: boolean; errors?: string[] }, {
      tenantId: TenantId
      domain: string
    }>({
      query: ({ tenantId, domain }) => ({
        url: `/${tenantId}/domains/validate`,
        method: 'POST',
        body: { domain },
      }),
    }),

    configureTenantDomain: builder.mutation<{ tenant: Tenant; dnsRecords: any[] }, {
      tenantId: TenantId
      domain: string
      sslEnabled?: boolean
    }>({
      query: ({ tenantId, domain, sslEnabled = true }) => ({
        url: `/${tenantId}/domains/configure`,
        method: 'POST',
        body: { domain, sslEnabled },
      }),
      invalidatesTags: (result, error, { tenantId }) => [
        { type: 'Tenant', id: tenantId },
      ],
    }),

    // Tenant impersonation (admin only)
    generateTenantAccessToken: builder.mutation<{ token: string; expiresAt: string }, {
      tenantId: TenantId
      userId?: string
      duration?: number
    }>({
      query: ({ tenantId, userId, duration = 3600 }) => ({
        url: `/${tenantId}/impersonate`,
        method: 'POST',
        body: { userId, duration },
      }),
    }),

    // Tenant export/import
    exportTenantData: builder.mutation<{ exportId: string; downloadUrl: string }, {
      tenantId: TenantId
      includeUsers?: boolean
      includeAnalytics?: boolean
      format?: 'json' | 'csv'
    }>({
      query: ({ tenantId, includeUsers = false, includeAnalytics = false, format = 'json' }) => ({
        url: `/${tenantId}/export`,
        method: 'POST',
        body: { includeUsers, includeAnalytics, format },
      }),
    }),

    importTenantData: builder.mutation<{ importId: string; status: string }, {
      tenantId: TenantId
      data: any
      overwrite?: boolean
    }>({
      query: ({ tenantId, data, overwrite = false }) => ({
        url: `/${tenantId}/import`,
        method: 'POST',
        body: { data, overwrite },
      }),
      invalidatesTags: (result, error, { tenantId }) => [
        { type: 'Tenant', id: tenantId },
        'TenantList',
      ],
    }),
  }),
})

// Export hooks
export const {
  useGetTenantQuery,
  useGetTenantBySlugQuery,
  useListTenantsQuery,
  useCreateTenantMutation,
  useUpdateTenantMutation,
  useDeleteTenantMutation,
  useGetTenantSettingsQuery,
  useUpdateTenantSettingsMutation,
  useGetTenantAnalyticsQuery,
  useExecuteCrossTenantQueryMutation,
  useGetTenantHierarchyQuery,
  useMoveTenantMutation,
  useGetTenantQuotasQuery,
  useUpdateTenantQuotasMutation,
  useSuspendTenantMutation,
  useActivateTenantMutation,
  useValidateCustomDomainMutation,
  useConfigureTenantDomainMutation,
  useGenerateTenantAccessTokenMutation,
  useExportTenantDataMutation,
  useImportTenantDataMutation,
} = tenantApi

// Utility selectors
export const selectTenantById = (tenantId: TenantId) =>
  tenantApi.endpoints.getTenant.select({ tenantId })

export const selectTenantBySlug = (slug: TenantSlug) =>
  tenantApi.endpoints.getTenantBySlug.select({ slug })

export const selectTenantSettings = (tenantId: TenantId) =>
  tenantApi.endpoints.getTenantSettings.select({ tenantId })

export const selectTenantAnalytics = (tenantId: TenantId, period: string) =>
  tenantApi.endpoints.getTenantAnalytics.select({
    tenantId,
    period: period as 'day' | 'week' | 'month' | 'quarter' | 'year'
  })

// Advanced selectors for multi-tenant context
export const createTenantContextSelector = (tenantId: TenantId) => (state: any): TenantContext | null => {
  const tenantResult = selectTenantById(tenantId)(state)
  if (!tenantResult.data) return null

  const tenant = tenantResult.data
  return {
    tenantId: tenant.id,
    tenantSlug: tenant.slug,
    tier: tenant.tier,
    features: tenant.features,
    quotas: tenant.quotas,
  }
}