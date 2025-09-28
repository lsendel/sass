/**
 * Frontend Contract Test for Metrics API
 *
 * This test validates the frontend integration with the Metrics API
 * defined in metrics-api.yaml from the client perspective.
 *
 * Tests:
 * - Metrics querying with various parameters
 * - Available metrics enumeration
 * - Real-time metrics streaming
 * - Alert rules CRUD operations
 * - Alert history tracking
 *
 * IMPORTANT: This test MUST FAIL initially since the API client doesn't exist yet.
 * This follows TDD - write tests first, then implement the client.
 */

import { describe, it, expect, beforeEach, vi, Mock } from 'vitest';
import { MetricsApiClient } from '../../services/MetricsApiClient';
import {
  MetricSeries,
  DataPoint,
  MetricDefinition,
  AlertRule,
  AlertRuleCreate,
  AlertRuleUpdate,
  AlertTrigger,
  MetricsQueryParams,
  MetricsResponse
} from '../../types/Metrics';

// Mock fetch globally for testing
global.fetch = vi.fn() as Mock;

describe('MetricsApi Contract Tests', () => {
  let apiClient: MetricsApiClient;
  const mockFetch = global.fetch as Mock;

  beforeEach(() => {
    // This WILL FAIL initially - MetricsApiClient doesn't exist yet
    apiClient = new MetricsApiClient({
      baseUrl: 'http://localhost:8080/api/v1',
      timeout: 5000
    });

    mockFetch.mockClear();
  });

  describe('queryMetrics', () => {
    it('should format request with all query parameters correctly', async () => {
      const mockMetricsResponse: MetricsResponse = {
        metrics: [
          {
            metricName: 'failed_logins_per_hour',
            tags: { module: 'auth', severity: 'high' },
            unit: 'count',
            dataPoints: [
              { timestamp: '2023-01-01T12:00:00Z', value: 15 },
              { timestamp: '2023-01-01T13:00:00Z', value: 23 }
            ]
          }
        ]
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockMetricsResponse
      });

      const queryParams: MetricsQueryParams = {
        metricNames: ['failed_logins_per_hour', 'successful_logins_per_hour'],
        fromTimestamp: '2023-01-01T00:00:00Z',
        toTimestamp: '2023-01-01T23:59:59Z',
        granularity: 'HOUR',
        tags: { module: 'auth', severity: 'high' }
      };

      await apiClient.queryMetrics(queryParams);

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/metrics?metricNames=failed_logins_per_hour,successful_logins_per_hour&fromTimestamp=2023-01-01T00:00:00Z&toTimestamp=2023-01-01T23:59:59Z&granularity=HOUR&tags=module:auth,severity:high',
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
      const mockDataPoints: DataPoint[] = [
        { timestamp: '2023-01-01T12:00:00Z', value: 42 },
        { timestamp: '2023-01-01T13:00:00Z', value: 38 },
        { timestamp: '2023-01-01T14:00:00Z', value: 51 }
      ];

      const mockMetricsResponse: MetricsResponse = {
        metrics: [
          {
            metricName: 'security_events_count',
            tags: { module: 'payment', eventType: 'fraud' },
            unit: 'count',
            dataPoints: mockDataPoints
          }
        ]
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockMetricsResponse
      });

      const result = await apiClient.queryMetrics({
        metricNames: ['security_events_count'],
        fromTimestamp: '2023-01-01T12:00:00Z',
        toTimestamp: '2023-01-01T14:00:00Z'
      });

      expect(result).toEqual(mockMetricsResponse);
      expect(result.metrics).toHaveLength(1);
      expect(result.metrics[0].dataPoints).toHaveLength(3);
      expect(result.metrics[0].dataPoints[0].value).toBe(42);
    });

    it('should handle missing required parameters (400 Bad Request)', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          message: 'Missing required parameters',
          errors: ['metricNames is required', 'fromTimestamp is required']
        })
      });

      await expect(apiClient.queryMetrics({} as MetricsQueryParams))
        .rejects
        .toThrow('Missing required parameters');
    });

    it('should handle different granularity options', async () => {
      const granularities: Array<'MINUTE' | 'HOUR' | 'DAY'> = ['MINUTE', 'HOUR', 'DAY'];

      for (const granularity of granularities) {
        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => ({ metrics: [] })
        });

        await apiClient.queryMetrics({
          metricNames: ['test_metric'],
          fromTimestamp: '2023-01-01T00:00:00Z',
          toTimestamp: '2023-01-01T23:59:59Z',
          granularity
        });

        expect(mockFetch).toHaveBeenLastCalledWith(
          expect.stringContaining(`granularity=${granularity}`),
          expect.any(Object)
        );
      }
    });
  });

  describe('listAvailableMetrics', () => {
    it('should fetch available metrics correctly', async () => {
      const mockMetricDefinitions: MetricDefinition[] = [
        {
          name: 'failed_logins_per_hour',
          description: 'Number of failed login attempts per hour',
          unit: 'count',
          type: 'COUNTER',
          tags: ['module', 'source']
        },
        {
          name: 'payment_fraud_score',
          description: 'Real-time payment fraud risk score',
          unit: 'percentage',
          type: 'GAUGE',
          tags: ['user_id', 'transaction_id']
        }
      ];

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockMetricDefinitions
      });

      const result = await apiClient.listAvailableMetrics();

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/metrics/available',
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            'Accept': 'application/json'
          })
        })
      );

      expect(result).toEqual(mockMetricDefinitions);
      expect(result).toHaveLength(2);
      expect(result[0].type).toBe('COUNTER');
      expect(result[1].type).toBe('GAUGE');
    });
  });

  describe('streamMetrics', () => {
    it('should establish SSE connection with correct parameters', async () => {
      // Mock EventSource for SSE testing
      const mockEventSource = {
        addEventListener: vi.fn(),
        close: vi.fn(),
        readyState: EventSource.OPEN,
        url: '',
        onerror: null,
        onmessage: null,
        onopen: null
      };

      global.EventSource = vi.fn().mockImplementation(() => mockEventSource);

      const metricsCallback = vi.fn();
      const errorCallback = vi.fn();

      const stream = apiClient.streamMetrics({
        metricNames: ['cpu_usage', 'memory_usage']
      }, metricsCallback, errorCallback);

      expect(global.EventSource).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/metrics/realtime?metricNames=cpu_usage,memory_usage'
      );

      expect(mockEventSource.addEventListener).toHaveBeenCalledWith('message', expect.any(Function));
      expect(mockEventSource.addEventListener).toHaveBeenCalledWith('error', expect.any(Function));

      // Test metrics event handling
      const messageHandler = mockEventSource.addEventListener.mock.calls
        .find(call => call[0] === 'message')[1];

      const mockSSEEvent = {
        data: JSON.stringify({
          metricName: 'cpu_usage',
          timestamp: '2023-01-01T12:00:00Z',
          value: 85.5,
          tags: { host: 'web-server-1' }
        })
      };

      messageHandler(mockSSEEvent);

      expect(metricsCallback).toHaveBeenCalledWith(JSON.parse(mockSSEEvent.data));
    });

    it('should handle SSE connection errors gracefully', async () => {
      const mockEventSource = {
        addEventListener: vi.fn(),
        close: vi.fn(),
        readyState: EventSource.CLOSED
      };

      global.EventSource = vi.fn().mockImplementation(() => mockEventSource);

      const metricsCallback = vi.fn();
      const errorCallback = vi.fn();

      apiClient.streamMetrics({ metricNames: ['test_metric'] }, metricsCallback, errorCallback);

      const errorHandler = mockEventSource.addEventListener.mock.calls
        .find(call => call[0] === 'error')[1];

      const mockError = new Error('SSE connection failed');
      errorHandler(mockError);

      expect(errorCallback).toHaveBeenCalledWith(mockError);
    });
  });

  describe('Alert Rules Management', () => {
    describe('listAlertRules', () => {
      it('should fetch alert rules with filters', async () => {
        const mockAlertRules: AlertRule[] = [
          {
            id: 'rule-1',
            name: 'High Failed Login Rate',
            description: 'Alert when failed logins exceed threshold',
            condition: 'failed_logins > 10',
            severity: 'HIGH',
            enabled: true,
            threshold: 10,
            timeWindow: 'PT1H',
            cooldownPeriod: 'PT30S',
            notificationChannels: ['email', 'slack'],
            escalationRules: { level1: 'email', level2: 'pagerduty' },
            createdBy: 'security-admin',
            lastTriggered: '2023-01-01T10:30:00Z',
            triggerCount: 5,
            createdAt: '2023-01-01T08:00:00Z'
          }
        ];

        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => mockAlertRules
        });

        const result = await apiClient.listAlertRules({ enabled: true });

        expect(mockFetch).toHaveBeenCalledWith(
          'http://localhost:8080/api/v1/alerts?enabled=true',
          expect.objectContaining({
            method: 'GET'
          })
        );

        expect(result).toEqual(mockAlertRules);
        expect(result[0].enabled).toBe(true);
      });
    });

    describe('createAlertRule', () => {
      it('should create alert rule with correct request format', async () => {
        const newAlertRule: AlertRuleCreate = {
          name: 'Critical Payment Fraud Alert',
          description: 'Alert on critical payment fraud events',
          condition: 'payment_fraud_score > 90',
          severity: 'CRITICAL',
          enabled: true,
          threshold: 90,
          timeWindow: 'PT5M',
          cooldownPeriod: 'PT1M',
          notificationChannels: ['email', 'pagerduty'],
          escalationRules: {
            level1: 'email',
            level2: 'pagerduty',
            level3: 'phone'
          }
        };

        const mockCreatedRule: AlertRule = {
          id: 'created-rule-id',
          createdBy: 'current-user',
          lastTriggered: null,
          triggerCount: 0,
          createdAt: '2023-01-01T12:00:00Z',
          ...newAlertRule
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 201,
          json: async () => mockCreatedRule
        });

        const result = await apiClient.createAlertRule(newAlertRule);

        expect(mockFetch).toHaveBeenCalledWith(
          'http://localhost:8080/api/v1/alerts',
          expect.objectContaining({
            method: 'POST',
            headers: expect.objectContaining({
              'Content-Type': 'application/json'
            }),
            body: JSON.stringify(newAlertRule)
          })
        );

        expect(result.id).toBe('created-rule-id');
        expect(result.name).toBe('Critical Payment Fraud Alert');
      });
    });

    describe('getAlertRule', () => {
      it('should fetch single alert rule by ID', async () => {
        const ruleId = 'test-rule-id';
        const mockAlertRule: AlertRule = {
          id: ruleId,
          name: 'Test Alert Rule',
          description: 'A test alert rule',
          condition: 'test_metric > 50',
          severity: 'MEDIUM',
          enabled: true,
          threshold: 50,
          timeWindow: 'PT10M',
          cooldownPeriod: 'PT2M',
          notificationChannels: ['email'],
          escalationRules: {},
          createdBy: 'admin',
          lastTriggered: null,
          triggerCount: 0,
          createdAt: '2023-01-01T12:00:00Z'
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => mockAlertRule
        });

        const result = await apiClient.getAlertRule(ruleId);

        expect(mockFetch).toHaveBeenCalledWith(
          `http://localhost:8080/api/v1/alerts/${ruleId}`,
          expect.objectContaining({
            method: 'GET'
          })
        );

        expect(result).toEqual(mockAlertRule);
        expect(result.id).toBe(ruleId);
      });

      it('should handle 404 Not Found for alert rules', async () => {
        const nonExistentRuleId = 'non-existent-rule';

        mockFetch.mockResolvedValueOnce({
          ok: false,
          status: 404,
          json: async () => ({ message: 'Alert rule not found' })
        });

        await expect(apiClient.getAlertRule(nonExistentRuleId))
          .rejects
          .toThrow('Alert rule not found');
      });
    });

    describe('updateAlertRule', () => {
      it('should update alert rule correctly', async () => {
        const ruleId = 'rule-to-update';
        const updateData: AlertRuleUpdate = {
          name: 'Updated Alert Rule',
          enabled: false,
          threshold: 75,
          timeWindow: 'PT15M'
        };

        const mockUpdatedRule: AlertRule = {
          id: ruleId,
          description: 'Original description',
          condition: 'original_condition',
          severity: 'HIGH',
          cooldownPeriod: 'PT30S',
          notificationChannels: ['email'],
          escalationRules: {},
          createdBy: 'admin',
          lastTriggered: null,
          triggerCount: 0,
          createdAt: '2023-01-01T12:00:00Z',
          ...updateData
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => mockUpdatedRule
        });

        const result = await apiClient.updateAlertRule(ruleId, updateData);

        expect(mockFetch).toHaveBeenCalledWith(
          `http://localhost:8080/api/v1/alerts/${ruleId}`,
          expect.objectContaining({
            method: 'PUT',
            body: JSON.stringify(updateData)
          })
        );

        expect(result.name).toBe('Updated Alert Rule');
        expect(result.enabled).toBe(false);
        expect(result.threshold).toBe(75);
      });
    });

    describe('deleteAlertRule', () => {
      it('should delete alert rule correctly', async () => {
        const ruleId = 'rule-to-delete';

        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 204
        });

        await apiClient.deleteAlertRule(ruleId);

        expect(mockFetch).toHaveBeenCalledWith(
          `http://localhost:8080/api/v1/alerts/${ruleId}`,
          expect.objectContaining({
            method: 'DELETE'
          })
        );
      });
    });

    describe('getAlertHistory', () => {
      it('should fetch alert trigger history with time range', async () => {
        const ruleId = 'test-rule';
        const mockAlertHistory: AlertTrigger[] = [
          {
            id: 'trigger-1',
            ruleId: ruleId,
            triggeredAt: '2023-01-01T10:00:00Z',
            value: 85,
            threshold: 50,
            message: 'Alert threshold exceeded',
            resolved: true,
            resolvedAt: '2023-01-01T10:15:00Z'
          },
          {
            id: 'trigger-2',
            ruleId: ruleId,
            triggeredAt: '2023-01-01T14:30:00Z',
            value: 92,
            threshold: 50,
            message: 'Alert threshold exceeded again',
            resolved: false,
            resolvedAt: null
          }
        ];

        mockFetch.mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => mockAlertHistory
        });

        const result = await apiClient.getAlertHistory(ruleId, {
          fromTimestamp: '2023-01-01T00:00:00Z',
          toTimestamp: '2023-01-01T23:59:59Z'
        });

        expect(mockFetch).toHaveBeenCalledWith(
          `http://localhost:8080/api/v1/alerts/${ruleId}/history?fromTimestamp=2023-01-01T00:00:00Z&toTimestamp=2023-01-01T23:59:59Z`,
          expect.objectContaining({
            method: 'GET'
          })
        );

        expect(result).toEqual(mockAlertHistory);
        expect(result).toHaveLength(2);
        expect(result[0].resolved).toBe(true);
        expect(result[1].resolved).toBe(false);
      });
    });
  });

  describe('Permission-based Access Control', () => {
    it('should handle 403 Forbidden for insufficient permissions', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 403,
        json: async () => ({ message: 'Insufficient permissions for metrics access' })
      });

      await expect(apiClient.queryMetrics({
        metricNames: ['restricted_metric'],
        fromTimestamp: '2023-01-01T00:00:00Z',
        toTimestamp: '2023-01-01T23:59:59Z'
      })).rejects.toThrow('Insufficient permissions for metrics access');
    });

    it('should handle 401 Unauthorized correctly', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ message: 'Authentication required' })
      });

      await expect(apiClient.listAlertRules({}))
        .rejects
        .toThrow('Authentication required. Please log in again.');
    });
  });

  describe('Performance and Error Handling', () => {
    it('should handle request timeouts', async () => {
      mockFetch.mockImplementationOnce(
        () => new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Request timeout')), 100)
        )
      );

      await expect(apiClient.queryMetrics({
        metricNames: ['slow_metric'],
        fromTimestamp: '2023-01-01T00:00:00Z',
        toTimestamp: '2023-01-01T23:59:59Z'
      })).rejects.toThrow('Failed to query metrics: Request timeout');
    });

    it('should handle malformed JSON responses', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => { throw new Error('Invalid JSON'); }
      });

      await expect(apiClient.listAvailableMetrics())
        .rejects
        .toThrow('Failed to parse response');
    });
  });
});

// NOTE: This test file will fail initially because:
// 1. MetricsApiClient doesn't exist
// 2. TypeScript interfaces don't exist
// 3. Implementation doesn't exist
// This is expected and correct for TDD approach.