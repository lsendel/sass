/**
 * Frontend Contract Test for Dashboard API
 *
 * This test validates the frontend integration with the Dashboard API
 * defined in dashboard-api.yaml from the client perspective.
 *
 * Tests:
 * - CRUD operations for dashboards and widgets
 * - Request formatting and response parsing
 * - Error handling for different HTTP status codes
 * - Dashboard data fetching with time ranges
 *
 * IMPORTANT: This test MUST FAIL initially since the API client doesn't exist yet.
 * This follows TDD - write tests first, then implement the client.
 */

import { describe, it, expect, beforeEach, vi, Mock } from 'vitest';
import { DashboardApiClient } from '../../services/DashboardApiClient';
import {
  Dashboard,
  DashboardCreate,
  DashboardUpdate,
  Widget,
  WidgetCreate,
  WidgetUpdate,
  WidgetData,
  WidgetPosition
} from '../../types/Dashboard';

// Mock fetch globally for testing
global.fetch = vi.fn() as Mock;

describe('DashboardApi Contract Tests', () => {
  let apiClient: DashboardApiClient;
  const mockFetch = global.fetch as Mock;

  beforeEach(() => {
    // This WILL FAIL initially - DashboardApiClient doesn't exist yet
    apiClient = new DashboardApiClient({
      baseUrl: 'http://localhost:8080/api/v1',
      timeout: 5000
    });

    mockFetch.mockClear();
  });

  describe('listDashboards', () => {
    it('should format request with filter parameters correctly', async () => {
      const mockDashboards: Dashboard[] = [
        {
          id: 'dash-1',
          name: 'Security Overview',
          description: 'Main security dashboard',
          widgets: [],
          permissions: ['ROLE_SECURITY_ANALYST'],
          isDefault: true,
          tags: ['security', 'overview'],
          owner: 'analyst1',
          shared: false,
          createdAt: '2023-01-01T12:00:00Z',
          lastModified: '2023-01-01T12:00:00Z'
        }
      ];

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockDashboards
      });

      // Test with query parameters
      await apiClient.listDashboards({
        shared: true,
        tags: ['security', 'alerts']
      });

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/dashboards?shared=true&tags=security,alerts',
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            'Accept': 'application/json',
            'Authorization': expect.any(String)
          })
        })
      );
    });

    it('should handle successful response with proper typing', async () => {
      const mockDashboards: Dashboard[] = [
        {
          id: 'test-dashboard',
          name: 'Test Dashboard',
          description: 'A test dashboard',
          widgets: [
            {
              id: 'widget-1',
              name: 'Test Widget',
              type: 'CHART',
              configuration: { chartType: 'line' },
              position: { x: 0, y: 0, width: 6, height: 4 },
              permissions: ['ROLE_SECURITY_ANALYST'],
              refreshInterval: 'PT30S',
              dataSource: 'security-events',
              createdBy: 'analyst1',
              createdAt: '2023-01-01T12:00:00Z',
              lastModified: '2023-01-01T12:00:00Z'
            }
          ],
          permissions: ['ROLE_SECURITY_ANALYST'],
          isDefault: false,
          tags: ['test'],
          owner: 'analyst1',
          shared: false,
          createdAt: '2023-01-01T12:00:00Z',
          lastModified: '2023-01-01T12:00:00Z'
        }
      ];

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockDashboards
      });

      const result = await apiClient.listDashboards({});

      expect(result).toEqual(mockDashboards);
      expect(result).toHaveLength(1);
      expect(result[0].widgets).toHaveLength(1);
      expect(result[0].widgets[0].type).toBe('CHART');
    });
  });

  describe('createDashboard', () => {
    it('should format POST request with correct body', async () => {
      const newDashboard: DashboardCreate = {
        name: 'New Security Dashboard',
        description: 'A new dashboard for security monitoring',
        permissions: ['ROLE_SECURITY_ANALYST', 'ROLE_ADMIN'],
        tags: ['security', 'custom'],
        shared: false,
        isDefault: false
      };

      const mockCreatedDashboard: Dashboard = {
        id: 'created-dashboard-id',
        createdAt: '2023-01-01T12:00:00Z',
        lastModified: '2023-01-01T12:00:00Z',
        owner: 'current-user',
        widgets: [],
        ...newDashboard
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: async () => mockCreatedDashboard
      });

      const result = await apiClient.createDashboard(newDashboard);

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/dashboards',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }),
          body: JSON.stringify(newDashboard)
        })
      );

      expect(result).toEqual(mockCreatedDashboard);
      expect(result.id).toBe('created-dashboard-id');
    });

    it('should handle validation errors (400 Bad Request)', async () => {
      const invalidDashboard = {
        // Missing required name field
        permissions: ['ROLE_SECURITY_ANALYST']
      };

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          message: 'Invalid dashboard data',
          errors: ['name is required', 'permissions cannot be empty']
        })
      });

      await expect(apiClient.createDashboard(invalidDashboard as DashboardCreate))
        .rejects
        .toThrow('Invalid dashboard data');
    });
  });

  describe('getDashboard', () => {
    it('should fetch single dashboard by ID correctly', async () => {
      const dashboardId = 'test-dashboard-id';
      const mockDashboard: Dashboard = {
        id: dashboardId,
        name: 'Fetched Dashboard',
        description: 'Dashboard fetched by ID',
        widgets: [
          {
            id: 'widget-1',
            name: 'Security Events',
            type: 'TABLE',
            configuration: { maxRows: 50 },
            position: { x: 0, y: 0, width: 12, height: 8 },
            permissions: ['ROLE_SECURITY_ANALYST'],
            refreshInterval: 'PT1M',
            dataSource: 'security-events',
            createdBy: 'analyst',
            createdAt: '2023-01-01T12:00:00Z',
            lastModified: '2023-01-01T12:00:00Z'
          }
        ],
        permissions: ['ROLE_SECURITY_ANALYST'],
        isDefault: true,
        tags: ['security'],
        owner: 'analyst',
        shared: true,
        createdAt: '2023-01-01T12:00:00Z',
        lastModified: '2023-01-01T12:00:00Z'
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockDashboard
      });

      const result = await apiClient.getDashboard(dashboardId);

      expect(mockFetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/dashboards/${dashboardId}`,
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            'Accept': 'application/json'
          })
        })
      );

      expect(result).toEqual(mockDashboard);
      expect(result.id).toBe(dashboardId);
      expect(result.widgets).toHaveLength(1);
    });

    it('should handle 404 Not Found correctly', async () => {
      const nonExistentId = 'non-existent-dashboard';

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        json: async () => ({ message: 'Dashboard not found' })
      });

      await expect(apiClient.getDashboard(nonExistentId))
        .rejects
        .toThrow('Dashboard not found');
    });

    it('should handle 403 Access Denied correctly', async () => {
      const restrictedDashboardId = 'restricted-dashboard';

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 403,
        json: async () => ({ message: 'Access denied to dashboard' })
      });

      await expect(apiClient.getDashboard(restrictedDashboardId))
        .rejects
        .toThrow('Access denied to dashboard');
    });
  });

  describe('updateDashboard', () => {
    it('should format PUT request correctly', async () => {
      const dashboardId = 'dashboard-to-update';
      const updateData: DashboardUpdate = {
        name: 'Updated Dashboard Name',
        description: 'Updated description',
        shared: true,
        tags: ['security', 'updated']
      };

      const mockUpdatedDashboard: Dashboard = {
        id: dashboardId,
        widgets: [],
        permissions: ['ROLE_SECURITY_ANALYST'],
        isDefault: false,
        owner: 'analyst',
        createdAt: '2023-01-01T12:00:00Z',
        lastModified: '2023-01-02T12:00:00Z',
        ...updateData
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUpdatedDashboard
      });

      const result = await apiClient.updateDashboard(dashboardId, updateData);

      expect(mockFetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/dashboards/${dashboardId}`,
        expect.objectContaining({
          method: 'PUT',
          headers: expect.objectContaining({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }),
          body: JSON.stringify(updateData)
        })
      );

      expect(result.name).toBe('Updated Dashboard Name');
      expect(result.shared).toBe(true);
      expect(result.lastModified).toBe('2023-01-02T12:00:00Z');
    });
  });

  describe('deleteDashboard', () => {
    it('should send DELETE request correctly', async () => {
      const dashboardId = 'dashboard-to-delete';

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204
      });

      await apiClient.deleteDashboard(dashboardId);

      expect(mockFetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/dashboards/${dashboardId}`,
        expect.objectContaining({
          method: 'DELETE'
        })
      );
    });

    it('should handle 403 Cannot Delete correctly', async () => {
      const protectedDashboardId = 'protected-dashboard';

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 403,
        json: async () => ({ message: 'Cannot delete dashboard' })
      });

      await expect(apiClient.deleteDashboard(protectedDashboardId))
        .rejects
        .toThrow('Cannot delete dashboard');
    });
  });

  describe('Widget Management', () => {
    describe('addWidget', () => {
      it('should add widget to dashboard correctly', async () => {
        const dashboardId = 'test-dashboard';
        const newWidget: WidgetCreate = {
          name: 'Failed Logins Chart',
          type: 'CHART',
          configuration: {
            chartType: 'line',
            dataQuery: 'failed_logins_per_hour'
          },
          position: {
            x: 0,
            y: 0,
            width: 8,
            height: 6
          },
          permissions: ['ROLE_SECURITY_ANALYST'],
          refreshInterval: 'PT1M',
          dataSource: 'security-events'
        };

        const mockCreatedWidget: Widget = {
          id: 'created-widget-id',
          createdBy: 'analyst',
          createdAt: '2023-01-01T12:00:00Z',
          lastModified: '2023-01-01T12:00:00Z',
          ...newWidget
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 201,
          json: async () => mockCreatedWidget
        });

        const result = await apiClient.addWidget(dashboardId, newWidget);

        expect(mockFetch).toHaveBeenCalledWith(
          `http://localhost:8080/api/v1/dashboards/${dashboardId}/widgets`,
          expect.objectContaining({
            method: 'POST',
            headers: expect.objectContaining({
              'Content-Type': 'application/json'
            }),
            body: JSON.stringify(newWidget)
          })
        );

        expect(result.id).toBe('created-widget-id');
        expect(result.name).toBe('Failed Logins Chart');
      });
    });

    describe('updateWidget', () => {
      it('should update widget correctly', async () => {
        const dashboardId = 'test-dashboard';
        const widgetId = 'widget-to-update';
        const updateData: WidgetUpdate = {
          name: 'Updated Widget Name',
          configuration: { chartType: 'bar' },
          refreshInterval: 'PT2M'
        };

        const mockUpdatedWidget: Widget = {
          id: widgetId,
          type: 'CHART',
          position: { x: 0, y: 0, width: 6, height: 4 },
          permissions: ['ROLE_SECURITY_ANALYST'],
          dataSource: 'security-events',
          createdBy: 'analyst',
          createdAt: '2023-01-01T12:00:00Z',
          lastModified: '2023-01-02T12:00:00Z',
          ...updateData
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => mockUpdatedWidget
        });

        const result = await apiClient.updateWidget(dashboardId, widgetId, updateData);

        expect(mockFetch).toHaveBeenCalledWith(
          `http://localhost:8080/api/v1/dashboards/${dashboardId}/widgets/${widgetId}`,
          expect.objectContaining({
            method: 'PUT',
            body: JSON.stringify(updateData)
          })
        );

        expect(result.name).toBe('Updated Widget Name');
        expect(result.refreshInterval).toBe('PT2M');
      });
    });

    describe('removeWidget', () => {
      it('should remove widget correctly', async () => {
        const dashboardId = 'test-dashboard';
        const widgetId = 'widget-to-remove';

        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 204
        });

        await apiClient.removeWidget(dashboardId, widgetId);

        expect(mockFetch).toHaveBeenCalledWith(
          `http://localhost:8080/api/v1/dashboards/${dashboardId}/widgets/${widgetId}`,
          expect.objectContaining({
            method: 'DELETE'
          })
        );
      });
    });
  });

  describe('getDashboardData', () => {
    it('should fetch dashboard data with time range correctly', async () => {
      const dashboardId = 'test-dashboard';
      const mockWidgetData: Record<string, WidgetData> = {
        'widget-1': {
          data: [
            { timestamp: '2023-01-01T12:00:00Z', value: 45 },
            { timestamp: '2023-01-01T13:00:00Z', value: 52 }
          ],
          lastUpdated: '2023-01-01T14:00:00Z',
          nextUpdate: '2023-01-01T14:01:00Z'
        },
        'widget-2': {
          data: [
            { type: 'CRITICAL', count: 5 },
            { type: 'HIGH', count: 12 }
          ],
          lastUpdated: '2023-01-01T14:00:00Z',
          nextUpdate: '2023-01-01T14:05:00Z'
        }
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockWidgetData
      });

      const result = await apiClient.getDashboardData(dashboardId, '24h');

      expect(mockFetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/dashboards/${dashboardId}/data?timeRange=24h`,
        expect.objectContaining({
          method: 'GET'
        })
      );

      expect(result).toEqual(mockWidgetData);
      expect(Object.keys(result)).toHaveLength(2);
      expect(result['widget-1'].data).toHaveLength(2);
    });

    it('should handle different time range parameters', async () => {
      const dashboardId = 'test-dashboard';
      const timeRanges = ['1h', '6h', '24h', '7d', '30d'];

      for (const timeRange of timeRanges) {
        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => ({})
        });

        await apiClient.getDashboardData(dashboardId, timeRange);

        expect(mockFetch).toHaveBeenCalledWith(
          `http://localhost:8080/api/v1/dashboards/${dashboardId}/data?timeRange=${timeRange}`,
          expect.any(Object)
        );
      }

      expect(mockFetch).toHaveBeenCalledTimes(timeRanges.length);
    });
  });

  describe('Error Handling', () => {
    it('should handle network timeouts', async () => {
      mockFetch.mockImplementationOnce(
        () => new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Request timeout')), 100)
        )
      );

      await expect(apiClient.listDashboards({}))
        .rejects
        .toThrow('Failed to fetch dashboards: Request timeout');
    });

    it('should handle malformed JSON responses', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => { throw new Error('Invalid JSON'); }
      });

      await expect(apiClient.listDashboards({}))
        .rejects
        .toThrow('Failed to parse response');
    });
  });
});

// NOTE: This test file will fail initially because:
// 1. DashboardApiClient doesn't exist
// 2. TypeScript interfaces don't exist
// 3. Implementation doesn't exist
// This is expected and correct for TDD approach.