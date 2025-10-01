import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

// API base URL configuration
// Note: Audit API is at /api/audit (not /api/v1/audit like other APIs)
const API_BASE_URL = import.meta.env.VITE_AUDIT_API_BASE_URL || 'http://localhost:3000';

// Types for the audit log API
export interface AuditLogEntry {
  id: string;
  timestamp: string;
  correlationId?: string;
  actorType: 'USER' | 'SYSTEM' | 'API_KEY' | 'SERVICE';
  actorDisplayName: string;
  actionType: string;
  actionDescription: string;
  resourceType: string;
  resourceDisplayName?: string;
  outcome: 'SUCCESS' | 'FAILURE' | 'PARTIAL';
  sensitivity: 'PUBLIC' | 'INTERNAL' | 'CONFIDENTIAL';
}

export interface AuditLogDetail extends AuditLogEntry {
  actorId?: string;
  resourceId?: string;
  ipAddress?: string;
  userAgent?: string;
  additionalData?: Record<string, unknown>;
}

export interface AuditLogResponse {
  content: AuditLogEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AuditLogFilter {
  page?: number;
  size?: number;
  dateFrom?: string;
  dateTo?: string;
  search?: string;
  actionTypes?: string[];
  resourceTypes?: string[];
  outcomes?: string[];
  sortField?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface ExportRequest {
  format: 'CSV' | 'JSON' | 'PDF';
  filters?: Omit<AuditLogFilter, 'page' | 'size'>;
}

export interface ExportResponse {
  exportId: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'EXPIRED';
  requestedAt: string;
  estimatedCompletionTime?: string;
}

export interface ExportStatusResponse extends ExportResponse {
  completedAt?: string;
  downloadToken?: string;
  tokenExpiresAt?: string;
  entryCount?: number;
  fileSizeBytes?: number;
  errorMessage?: string;
}

// Create the audit API slice
export const auditApi = createApi({
  reducerPath: 'auditApi',
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_BASE_URL}/api/audit`,
    credentials: 'include', // Include session cookies
  }),
  tagTypes: ['AuditLog', 'Export'],
  endpoints: (builder) => ({
    // Get paginated audit logs with filtering
    getAuditLogs: builder.query<AuditLogResponse, AuditLogFilter>({
      query: (filters) => {
        const params = new URLSearchParams();

        if (filters.page !== undefined) params.append('page', filters.page.toString());
        if (filters.size !== undefined) params.append('size', filters.size.toString());
        if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
        if (filters.dateTo) params.append('dateTo', filters.dateTo);
        if (filters.search) params.append('search', filters.search);
        if (filters.actionTypes?.length) params.append('actionTypes', filters.actionTypes.join(','));
        if (filters.resourceTypes?.length) params.append('resourceTypes', filters.resourceTypes.join(','));
        if (filters.outcomes?.length) params.append('outcomes', filters.outcomes.join(','));
        if (filters.sortField) params.append('sortField', filters.sortField);
        if (filters.sortDirection) params.append('sortDirection', filters.sortDirection);

        return {
          url: '/logs',
          params,
        };
      },
      providesTags: ['AuditLog'],
    }),

    // Get detailed audit log entry
    getAuditLogDetail: builder.query<AuditLogDetail, string>({
      query: (id) => `/logs/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'AuditLog', id }],
    }),

    // Request audit log export
    requestExport: builder.mutation<ExportResponse, ExportRequest>({
      query: (exportRequest) => ({
        url: '/export',
        method: 'POST',
        body: exportRequest,
      }),
      invalidatesTags: ['Export'],
    }),

    // Get export status
    getExportStatus: builder.query<ExportStatusResponse, string>({
      query: (exportId) => `/export/${exportId}/status`,
      providesTags: (_result, _error, exportId) => [{ type: 'Export', id: exportId }],
    }),

    // Download export (returns blob URL)
    downloadExport: builder.mutation<Blob, string>({
      query: (token) => ({
        url: `/export/${token}/download`,
        responseHandler: (response) => response.blob(),
      }),
    }),
  }),
});

// Export hooks for use in components
export const {
  useGetAuditLogsQuery,
  useGetAuditLogDetailQuery,
  useRequestExportMutation,
  useGetExportStatusQuery,
  useDownloadExportMutation,
} = auditApi;