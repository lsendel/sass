/**
 * Frontend Contract Test for Security Events API
 *
 * This test validates the frontend integration with the SecurityEvents API
 * defined in security-events-api.yaml from the client perspective.
 *
 * Tests:
 * - API client correctly formats requests
 * - Response parsing and type validation
 * - Error handling for different HTTP status codes
 * - WebSocket integration for real-time events
 *
 * IMPORTANT: This test MUST FAIL initially since the API client doesn't exist yet.
 * This follows TDD - write tests first, then implement the client.
 */

import { describe, it, expect, beforeEach, vi, Mock } from 'vitest';
import { SecurityEventsApiClient } from '../../services/SecurityEventsApiClient';
import {
  SecurityEvent,
  SecurityEventCreate,
  SecurityEventUpdate,
  PageInfo,
  SecurityEventResponse
} from '../../types/SecurityEvent';

// Mock fetch globally for testing
global.fetch = vi.fn() as Mock;

describe('SecurityEventsApi Contract Tests', () => {
  let apiClient: SecurityEventsApiClient;
  const mockFetch = global.fetch as Mock;

  beforeEach(() => {
    // This WILL FAIL initially - SecurityEventsApiClient doesn't exist yet
    apiClient = new SecurityEventsApiClient({
      baseUrl: 'http://localhost:8080/api/v1',
      timeout: 5000
    });

    mockFetch.mockClear();
  });

  describe('listSecurityEvents', () => {
    it('should format request with all query parameters correctly', async () => {
      // Mock successful response
      const mockResponse: SecurityEventResponse = {
        content: [],
        page: {
          number: 0,
          size: 50,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true
        }
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse,
        headers: new Headers({ 'content-type': 'application/json' })
      });

      // Test with all possible query parameters
      await apiClient.listSecurityEvents({
        eventType: 'LOGIN_ATTEMPT',
        severity: 'HIGH',
        fromTimestamp: '2023-01-01T00:00:00Z',
        toTimestamp: '2023-12-31T23:59:59Z',
        userId: 'user123',
        page: 0,
        size: 25
      });

      // Verify fetch was called with correct URL and parameters
      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/security-events?eventType=LOGIN_ATTEMPT&severity=HIGH&fromTimestamp=2023-01-01T00:00:00Z&toTimestamp=2023-12-31T23:59:59Z&userId=user123&page=0&size=25',
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
      const mockEvents: SecurityEvent[] = [
        {
          id: 'test-id',
          eventType: 'LOGIN_ATTEMPT',
          severity: 'MEDIUM',
          timestamp: '2023-01-01T12:00:00Z',
          sourceModule: 'auth',
          sourceIp: '192.168.1.1',
          userAgent: 'Test Browser',
          details: { success: false },
          correlationId: 'test-corr',
          resolved: false,
          userId: 'user123',
          sessionId: 'session123'
        }
      ];

      const mockResponse: SecurityEventResponse = {
        content: mockEvents,
        page: {
          number: 0,
          size: 50,
          totalElements: 1,
          totalPages: 1,
          first: true,
          last: true
        }
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockResponse
      });

      const result = await apiClient.listSecurityEvents({});

      expect(result).toEqual(mockResponse);
      expect(result.content).toHaveLength(1);
      expect(result.content[0].eventType).toBe('LOGIN_ATTEMPT');
    });

    it('should handle 403 Forbidden response correctly', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 403,
        json: async () => ({ message: 'Insufficient permissions' })
      });

      await expect(apiClient.listSecurityEvents({}))
        .rejects
        .toThrow('Insufficient permissions to access security events');
    });

    it('should handle network errors gracefully', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      await expect(apiClient.listSecurityEvents({}))
        .rejects
        .toThrow('Failed to fetch security events: Network error');
    });
  });

  describe('createSecurityEvent', () => {
    it('should format POST request with correct body', async () => {
      const newEvent: SecurityEventCreate = {
        eventType: 'PAYMENT_FRAUD',
        severity: 'CRITICAL',
        sourceModule: 'payment',
        sourceIp: '203.0.113.1',
        userAgent: 'Suspicious Agent',
        userId: 'fraud-user',
        sessionId: 'fraud-session',
        correlationId: 'fraud-corr',
        details: {
          amount: 10000,
          fraudScore: 95
        }
      };

      const mockCreatedEvent: SecurityEvent = {
        id: 'created-id',
        timestamp: '2023-01-01T12:00:00Z',
        resolved: false,
        ...newEvent
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: async () => mockCreatedEvent
      });

      const result = await apiClient.createSecurityEvent(newEvent);

      expect(mockFetch).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/security-events',
        expect.objectContaining({
          method: 'POST',
          headers: expect.objectContaining({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }),
          body: JSON.stringify(newEvent)
        })
      );

      expect(result).toEqual(mockCreatedEvent);
      expect(result.id).toBe('created-id');
    });

    it('should handle validation errors (400 Bad Request)', async () => {
      const invalidEvent = {
        eventType: 'INVALID_TYPE',
        severity: 'CRITICAL'
        // Missing required fields
      };

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
        json: async () => ({
          message: 'Invalid event data',
          errors: ['eventType is invalid', 'sourceModule is required']
        })
      });

      await expect(apiClient.createSecurityEvent(invalidEvent as SecurityEventCreate))
        .rejects
        .toThrow('Invalid event data');
    });
  });

  describe('getSecurityEvent', () => {
    it('should fetch single event by ID correctly', async () => {
      const eventId = 'test-event-id';
      const mockEvent: SecurityEvent = {
        id: eventId,
        eventType: 'API_ABUSE',
        severity: 'HIGH',
        timestamp: '2023-01-01T12:00:00Z',
        sourceModule: 'api',
        sourceIp: '198.51.100.1',
        userAgent: 'Bot/1.0',
        details: { requestCount: 1000 },
        correlationId: 'api-abuse-corr',
        resolved: false
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockEvent
      });

      const result = await apiClient.getSecurityEvent(eventId);

      expect(mockFetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/security-events/${eventId}`,
        expect.objectContaining({
          method: 'GET',
          headers: expect.objectContaining({
            'Accept': 'application/json'
          })
        })
      );

      expect(result).toEqual(mockEvent);
      expect(result.id).toBe(eventId);
    });

    it('should handle 404 Not Found correctly', async () => {
      const nonExistentId = 'non-existent-id';

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        json: async () => ({ message: 'Event not found' })
      });

      await expect(apiClient.getSecurityEvent(nonExistentId))
        .rejects
        .toThrow('Security event not found');
    });
  });

  describe('updateSecurityEvent', () => {
    it('should format PATCH request correctly', async () => {
      const eventId = 'event-to-update';
      const updateData: SecurityEventUpdate = {
        resolved: true,
        resolvedBy: 'security-analyst'
      };

      const mockUpdatedEvent: SecurityEvent = {
        id: eventId,
        eventType: 'LOGIN_ATTEMPT',
        severity: 'MEDIUM',
        timestamp: '2023-01-01T12:00:00Z',
        sourceModule: 'auth',
        sourceIp: '192.168.1.1',
        userAgent: 'Browser/1.0',
        details: {},
        correlationId: 'test-corr',
        resolved: true,
        resolvedBy: 'security-analyst',
        resolvedAt: '2023-01-01T12:30:00Z'
      };

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUpdatedEvent
      });

      const result = await apiClient.updateSecurityEvent(eventId, updateData);

      expect(mockFetch).toHaveBeenCalledWith(
        `http://localhost:8080/api/v1/security-events/${eventId}`,
        expect.objectContaining({
          method: 'PATCH',
          headers: expect.objectContaining({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }),
          body: JSON.stringify(updateData)
        })
      );

      expect(result.resolved).toBe(true);
      expect(result.resolvedBy).toBe('security-analyst');
      expect(result.resolvedAt).toBeDefined();
    });
  });

  describe('streamSecurityEvents', () => {
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

      // Mock global EventSource
      global.EventSource = vi.fn().mockImplementation(() => mockEventSource);

      const eventCallback = vi.fn();
      const errorCallback = vi.fn();

      const stream = apiClient.streamSecurityEvents({
        eventTypes: ['LOGIN_ATTEMPT', 'PAYMENT_FRAUD'],
        severities: ['CRITICAL', 'HIGH']
      }, eventCallback, errorCallback);

      expect(global.EventSource).toHaveBeenCalledWith(
        'http://localhost:8080/api/v1/security-events/stream?eventTypes=LOGIN_ATTEMPT,PAYMENT_FRAUD&severities=CRITICAL,HIGH'
      );

      expect(mockEventSource.addEventListener).toHaveBeenCalledWith('message', expect.any(Function));
      expect(mockEventSource.addEventListener).toHaveBeenCalledWith('error', expect.any(Function));

      // Test event handling
      const messageHandler = mockEventSource.addEventListener.mock.calls
        .find(call => call[0] === 'message')[1];

      const mockSSEEvent = {
        data: JSON.stringify({
          id: 'streaming-event',
          eventType: 'LOGIN_ATTEMPT',
          severity: 'HIGH',
          timestamp: '2023-01-01T12:00:00Z'
        })
      };

      messageHandler(mockSSEEvent);

      expect(eventCallback).toHaveBeenCalledWith(JSON.parse(mockSSEEvent.data));
    });

    it('should handle SSE connection errors', async () => {
      const mockEventSource = {
        addEventListener: vi.fn(),
        close: vi.fn(),
        readyState: EventSource.CLOSED
      };

      global.EventSource = vi.fn().mockImplementation(() => mockEventSource);

      const eventCallback = vi.fn();
      const errorCallback = vi.fn();

      apiClient.streamSecurityEvents({}, eventCallback, errorCallback);

      const errorHandler = mockEventSource.addEventListener.mock.calls
        .find(call => call[0] === 'error')[1];

      const mockError = new Error('SSE connection failed');
      errorHandler(mockError);

      expect(errorCallback).toHaveBeenCalledWith(mockError);
    });
  });

  describe('Authentication Integration', () => {
    it('should include authentication headers in all requests', async () => {
      // Mock auth token
      const mockToken = 'mock-jwt-token';
      apiClient.setAuthToken(mockToken);

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ content: [], page: {} })
      });

      await apiClient.listSecurityEvents({});

      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            'Authorization': `Bearer ${mockToken}`
          })
        })
      );
    });

    it('should handle 401 Unauthorized responses', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
        json: async () => ({ message: 'Invalid or expired token' })
      });

      await expect(apiClient.listSecurityEvents({}))
        .rejects
        .toThrow('Authentication required. Please log in again.');
    });
  });
});

// NOTE: This test file will fail initially because:
// 1. SecurityEventsApiClient doesn't exist
// 2. TypeScript interfaces don't exist
// 3. Implementation doesn't exist
// This is expected and correct for TDD approach.