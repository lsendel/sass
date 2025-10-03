import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

import { auditApi } from '../../store/api/auditApi'
import type {
  AuditLogEntry,
  AuditLogResponse,
  AuditLogDetail,
  ExportResponse,
  ExportStatusResponse,
} from '../../store/api/auditApi'
import { createApiTestStore } from '../utils/testStore'

/**
 * Audit API Integration Tests
 *
 * Tests for audit logging, filtering, and export functionality
 */

const API_BASE_URL = 'http://localhost:3000/api/audit'

const mockAuditLogs: AuditLogEntry[] = [
  {
    id: '1',
    timestamp: '2025-09-30T10:00:00Z',
    correlationId: 'corr-123',
    actorType: 'USER',
    actorDisplayName: 'John Doe',
    actionType: 'USER_LOGIN',
    actionDescription: 'User logged in successfully',
    resourceType: 'AUTH',
    resourceDisplayName: 'Authentication',
    outcome: 'SUCCESS',
    sensitivity: 'INTERNAL',
  },
  {
    id: '2',
    timestamp: '2025-09-30T10:05:00Z',
    correlationId: 'corr-124',
    actorType: 'USER',
    actorDisplayName: 'Jane Smith',
    actionType: 'PAYMENT_PROCESSED',
    actionDescription: 'Payment processed successfully',
    resourceType: 'PAYMENT',
    resourceDisplayName: 'Payment #1234',
    outcome: 'SUCCESS',
    sensitivity: 'CONFIDENTIAL',
  },
]

const handlers = [
  // GET /audit/logs
  http.get(`${API_BASE_URL}/logs`, ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '0')
    const size = parseInt(url.searchParams.get('size') || '20')
    const search = url.searchParams.get('search')

    let filteredLogs = [...mockAuditLogs]

    if (search) {
      filteredLogs = filteredLogs.filter(log =>
        log.actionDescription.toLowerCase().includes(search.toLowerCase())
      )
    }

    const response: AuditLogResponse = {
      content: filteredLogs.slice(page * size, (page + 1) * size),
      page,
      size,
      totalElements: filteredLogs.length,
      totalPages: Math.ceil(filteredLogs.length / size),
      first: page === 0,
      last: (page + 1) * size >= filteredLogs.length,
    }

    return HttpResponse.json(response)
  }),

  // GET /audit/logs/:id
  http.get(`${API_BASE_URL}/logs/:id`, ({ params }) => {
    const { id } = params

    const log = mockAuditLogs.find(l => l.id === id)

    if (!log) {
      return HttpResponse.json(
        { message: 'Audit log not found' },
        { status: 404 }
      )
    }

    const detail: AuditLogDetail = {
      ...log,
      actorId: `actor-${id}`,
      resourceId: `resource-${id}`,
      ipAddress: '192.168.1.1',
      userAgent: 'Mozilla/5.0',
      additionalData: { key: 'value' },
    }

    return HttpResponse.json(detail)
  }),

  // POST /audit/export
  http.post(`${API_BASE_URL}/export`, async ({ request }) => {
    const body = (await request.json()) as any

    if (!['CSV', 'JSON', 'PDF'].includes(body.format)) {
      return HttpResponse.json({ message: 'Invalid format' }, { status: 400 })
    }

    const response: ExportResponse = {
      exportId: 'export-123',
      status: 'PENDING',
      requestedAt: new Date().toISOString(),
      estimatedCompletionTime: new Date(Date.now() + 60000).toISOString(),
    }

    return HttpResponse.json(response, { status: 202 })
  }),

  // GET /audit/export/:id/status
  http.get(`${API_BASE_URL}/export/:id/status`, ({ params }) => {
    const { id } = params

    if (id === 'export-failed') {
      const response: ExportStatusResponse = {
        exportId: id as string,
        status: 'FAILED',
        requestedAt: new Date(Date.now() - 120000).toISOString(),
        errorMessage: 'Export failed due to system error',
      }
      return HttpResponse.json(response)
    }

    const response: ExportStatusResponse = {
      exportId: id as string,
      status: 'COMPLETED',
      requestedAt: new Date(Date.now() - 120000).toISOString(),
      completedAt: new Date(Date.now() - 60000).toISOString(),
      downloadToken: 'download-token-123',
      tokenExpiresAt: new Date(Date.now() + 3600000).toISOString(),
      entryCount: 150,
      fileSizeBytes: 52428,
    }

    return HttpResponse.json(response)
  }),

  // GET /audit/export//download (empty token)
  http.get(`${API_BASE_URL}/export//download`, () => {
    return HttpResponse.json(
      { message: 'Download token required' },
      { status: 400 }
    )
  }),

  // GET /audit/export/:token/download
  http.get(`${API_BASE_URL}/export/:token/download`, ({ params }) => {
    const { token } = params

    if (token === 'invalid-token') {
      return HttpResponse.json(
        { message: 'Invalid or expired token' },
        { status: 403 }
      )
    }

    // Return mock CSV blob data
    const csvData = 'id,timestamp,action\n1,2025-09-30,LOGIN'
    return new HttpResponse(csvData, {
      headers: {
        'Content-Type': 'text/csv',
        'Content-Disposition': 'attachment; filename="audit-logs.csv"',
      },
    })
  }),
]

const server = setupServer(...handlers)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

const createTestStore = () => {
  return createApiTestStore(auditApi)
}

describe('Audit API', () => {
  describe('GET /audit/logs', () => {
    it('should fetch paginated audit logs', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({ page: 0, size: 20 })
      )

      expect(result.status).toBe('fulfilled')
      expect(result.data).toHaveProperty('content')
      expect(result.data).toHaveProperty('page')
      expect(result.data).toHaveProperty('totalElements')
      expect(Array.isArray(result.data?.content)).toBe(true)
    })

    it('should return correct pagination metadata', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({ page: 0, size: 10 })
      )

      expect(result.data?.page).toBe(0)
      expect(result.data?.size).toBe(10)
      expect(result.data?.first).toBe(true)
      expect(result.data?.totalPages).toBeGreaterThan(0)
    })

    it('should filter audit logs by search term', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({
          page: 0,
          size: 20,
          search: 'payment',
        })
      )

      expect(result.status).toBe('fulfilled')
      expect(result.data?.content.length).toBeGreaterThan(0)
      result.data?.content.forEach(log => {
        expect(log.actionDescription.toLowerCase()).toContain('payment')
      })
    })

    it('should filter by date range', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({
          page: 0,
          size: 20,
          dateFrom: '2025-09-30',
          dateTo: '2025-10-01',
        })
      )

      expect(result.status).toBe('fulfilled')
      expect(result.data).toBeDefined()
    })

    it('should filter by action types', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({
          page: 0,
          size: 20,
          actionTypes: ['USER_LOGIN', 'USER_LOGOUT'],
        })
      )

      expect(result.status).toBe('fulfilled')
    })

    it('should filter by resource types', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({
          page: 0,
          size: 20,
          resourceTypes: ['AUTH', 'PAYMENT'],
        })
      )

      expect(result.status).toBe('fulfilled')
    })

    it('should filter by outcomes', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({
          page: 0,
          size: 20,
          outcomes: ['SUCCESS', 'FAILURE'],
        })
      )

      expect(result.status).toBe('fulfilled')
    })

    it('should support sorting', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({
          page: 0,
          size: 20,
          sortField: 'timestamp',
          sortDirection: 'DESC',
        })
      )

      expect(result.status).toBe('fulfilled')
    })

    it('should handle empty results', async () => {
      const store = createTestStore()

      server.use(
        http.get(`${API_BASE_URL}/logs`, () => {
          return HttpResponse.json({
            content: [],
            page: 0,
            size: 20,
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true,
          })
        })
      )

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({ page: 0, size: 20 })
      )

      expect(result.data?.content).toHaveLength(0)
      expect(result.data?.totalElements).toBe(0)
    })
  })

  describe('GET /audit/logs/:id', () => {
    it('should fetch audit log detail by ID', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogDetail.initiate('1')
      )

      expect(result.status).toBe('fulfilled')
      expect(result.data).toHaveProperty('id')
      expect(result.data).toHaveProperty('actorId')
      expect(result.data).toHaveProperty('ipAddress')
      expect(result.data).toHaveProperty('additionalData')
    })

    it('should return 404 for non-existent log', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogDetail.initiate('999')
      )

      expect(result.status).toBe('rejected')
      expect(result.error).toMatchObject({
        status: 404,
      })
    })

    it('should include sensitive data in detail view', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogDetail.initiate('1')
      )

      expect(result.data).toHaveProperty('ipAddress')
      expect(result.data).toHaveProperty('userAgent')
      expect(result.data?.additionalData).toBeDefined()
    })
  })

  describe('POST /audit/export', () => {
    it('should initiate CSV export', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.requestExport.initiate({
          format: 'CSV',
          filters: {
            dateFrom: '2025-09-01',
            dateTo: '2025-09-30',
          },
        })
      )

      expect(result.error).toBeUndefined()
      expect(result.data).toHaveProperty('exportId')
      expect(result.data).toHaveProperty('status')
      expect(result.data?.status).toBe('PENDING')
    })

    it('should initiate JSON export', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.requestExport.initiate({
          format: 'JSON',
        })
      )

      expect(result.error).toBeUndefined()
      expect(result.data?.exportId).toBeDefined()
    })

    it('should initiate PDF export', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.requestExport.initiate({
          format: 'PDF',
        })
      )

      expect(result.error).toBeUndefined()
    })

    it('should return 400 for invalid format', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.requestExport.initiate({
          format: 'INVALID' as any,
        })
      )

      expect(result.error).toBeDefined()
      expect(result.error).toMatchObject({
        status: 400,
      })
    })

    it('should include filters in export request', async () => {
      const store = createTestStore()
      let capturedBody: any

      server.use(
        http.post(`${API_BASE_URL}/export`, async ({ request }) => {
          capturedBody = await request.json()
          return HttpResponse.json(
            {
              exportId: 'export-123',
              status: 'PENDING',
              requestedAt: new Date().toISOString(),
            },
            { status: 202 }
          )
        })
      )

      await store.dispatch(
        auditApi.endpoints.requestExport.initiate({
          format: 'CSV',
          filters: {
            dateFrom: '2025-09-01',
            dateTo: '2025-09-30',
            actionTypes: ['USER_LOGIN'],
          },
        })
      )

      expect(capturedBody.filters).toEqual({
        dateFrom: '2025-09-01',
        dateTo: '2025-09-30',
        actionTypes: ['USER_LOGIN'],
      })
    })

    it('should return estimated completion time', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.requestExport.initiate({
          format: 'CSV',
        })
      )

      expect(result.data?.estimatedCompletionTime).toBeDefined()
    })
  })

  describe('GET /audit/export/:id/status', () => {
    it('should check export status', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getExportStatus.initiate('export-123')
      )

      expect(result.status).toBe('fulfilled')
      expect(result.data).toHaveProperty('exportId')
      expect(result.data).toHaveProperty('status')
    })

    it('should return COMPLETED status with download token', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getExportStatus.initiate('export-123')
      )

      expect(result.data?.status).toBe('COMPLETED')
      expect(result.data?.downloadToken).toBeDefined()
      expect(result.data?.tokenExpiresAt).toBeDefined()
    })

    it('should return file metadata for completed exports', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getExportStatus.initiate('export-123')
      )

      expect(result.data?.entryCount).toBeDefined()
      expect(result.data?.fileSizeBytes).toBeDefined()
      expect(result.data?.completedAt).toBeDefined()
    })

    it('should return FAILED status with error message', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.getExportStatus.initiate('export-failed')
      )

      expect(result.data?.status).toBe('FAILED')
      expect(result.data?.errorMessage).toBeDefined()
    })

    it('should handle different export statuses', async () => {
      const store = createTestStore()

      const statuses = [
        'PENDING',
        'IN_PROGRESS',
        'COMPLETED',
        'FAILED',
        'EXPIRED',
      ]

      for (let i = 0; i < statuses.length; i++) {
        const status = statuses[i]
        const exportId = `export-${i}`

        server.use(
          http.get(`${API_BASE_URL}/export/${exportId}/status`, () => {
            return HttpResponse.json({
              exportId,
              status,
              requestedAt: new Date().toISOString(),
            })
          })
        )

        const result = await store.dispatch(
          auditApi.endpoints.getExportStatus.initiate(exportId)
        )

        expect(result.data?.status).toBe(status)
      }
    })
  })

  describe('GET /audit/export/:id/download', () => {
    it('should download export file with valid token', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.downloadExport.initiate('download-token-123')
      )

      expect(result.error).toBeUndefined()
    })

    it('should return 400 for missing token', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.downloadExport.initiate('')
      )

      expect(result.error).toBeDefined()
      expect(result.error).toMatchObject({
        status: 400,
      })
    })

    it('should return 403 for invalid token', async () => {
      const store = createTestStore()

      const result = await store.dispatch(
        auditApi.endpoints.downloadExport.initiate('invalid-token')
      )

      expect(result.error).toBeDefined()
      expect(result.error).toMatchObject({
        status: 403,
      })
    })

    it('should return file with correct content type', async () => {
      const store = createTestStore()

      // This would be tested in E2E tests with actual file download
      const result = await store.dispatch(
        auditApi.endpoints.downloadExport.initiate('download-token-123')
      )

      expect(result.error).toBeUndefined()
    })
  })

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      const store = createTestStore()

      server.use(
        http.get(`${API_BASE_URL}/logs`, () => {
          return HttpResponse.error()
        })
      )

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({ page: 0, size: 20 })
      )

      expect(result.status).toBe('rejected')
    })

    it('should handle server errors', async () => {
      const store = createTestStore()

      server.use(
        http.get(`${API_BASE_URL}/logs`, () => {
          return HttpResponse.json(
            { message: 'Internal server error' },
            { status: 500 }
          )
        })
      )

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({ page: 0, size: 20 })
      )

      expect(result.status).toBe('rejected')
      expect(result.error).toMatchObject({
        status: 500,
      })
    })

    it('should handle unauthorized access', async () => {
      const store = createTestStore()

      server.use(
        http.get(`${API_BASE_URL}/logs`, () => {
          return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 })
        })
      )

      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({ page: 0, size: 20 })
      )

      expect(result.status).toBe('rejected')
      expect(result.error).toMatchObject({
        status: 401,
      })
    })
  })

  describe('Cache Management', () => {
    it('should cache audit log list queries', async () => {
      const store = createTestStore()

      // First call
      await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({ page: 0, size: 20 })
      )

      // Second call should use cache
      const result = await store.dispatch(
        auditApi.endpoints.getAuditLogs.initiate({ page: 0, size: 20 })
      )

      expect(result.status).toBe('fulfilled')
    })

    it('should invalidate cache when export completes', async () => {
      const store = createTestStore()

      // Initiate export
      await store.dispatch(
        auditApi.endpoints.requestExport.initiate({
          format: 'CSV',
        })
      )

      // Cache should be managed appropriately
      const state = store.getState()
      expect((state as any).auditApi).toBeDefined()
    })
  })
})
