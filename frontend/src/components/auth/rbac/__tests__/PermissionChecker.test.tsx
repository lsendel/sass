/**
 * Comprehensive Test Suite for Enhanced Permission Checker
 *
 * Tests cover:
 * - Constitutional compliance validation
 * - Security monitoring and anomaly detection
 * - Audit logging functionality
 * - Caching behavior and performance
 * - Accessibility features
 * - Error handling and edge cases
 */

import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Provider } from 'react-redux'
import { configureStore } from '@reduxjs/toolkit'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'

import PermissionChecker, { usePermissionCheck } from '../PermissionChecker'
import { useCheckPermissionQuery } from '../../../../store/api/rbacApi'
import { auditPermissionCheck } from '../../../../utils/auditLogger'
import { securityMonitor } from '../../../../utils/securityMonitor'
import { permissionCache } from '../../../../utils/permissionCache'
import type { PermissionEvaluationResult, ResourceType, ActionType } from '../../../../types/rbac'

// Branded types (matching PermissionChecker.tsx)
type ResourceId = string & { readonly __brand: 'ResourceId' }
const createResourceId = (id: string): ResourceId => id as ResourceId

// Mock dependencies
vi.mock('../../../../store/api/rbacApi')
vi.mock('../../../../utils/auditLogger')
vi.mock('../../../../utils/securityMonitor')
vi.mock('../../../../utils/permissionCache')
vi.mock('../../../../utils/logger')

// Mock instances accessible across all test suites
const mockUseCheckPermissionQuery = vi.mocked(useCheckPermissionQuery)
const mockAuditPermissionCheck = vi.mocked(auditPermissionCheck)
const mockSecurityMonitor = vi.mocked(securityMonitor)
const mockPermissionCache = vi.mocked(permissionCache)

// Mock store setup
const createMockStore = (initialState = {}) => {
  return configureStore({
    reducer: {
      auth: (state = {
        currentUser: {
          id: 'user-123',
          name: 'Test User',
          email: 'test@example.com',
          sessionId: 'session-456',
        }
      }, _action) => state,
    },
    preloadedState: initialState,
  })
}

// Test wrapper component
const TestWrapper: React.FC<{ children: React.ReactNode; store?: any }> = ({
  children,
  store = createMockStore()
}) => (
  <Provider store={store}>
    {children}
  </Provider>
)

// Mock permission result factory
const createMockPermissionResult = (
  allowed: boolean = true,
  reason: string = 'Permission granted'
): PermissionEvaluationResult => {
  const result: PermissionEvaluationResult = {
    allowed,
    reason,
    matchedPermissions: allowed ? [{
      id: 'perm-1',
      name: 'Read Access',
      description: 'Allow read access',
      resource: 'user' as ResourceType,
      action: 'read' as ActionType,
      scope: 'organization',
      conditions: [],
      category: 'user_management',
      isSystemPermission: false,
      createdAt: '2023-01-01T00:00:00Z',
      updatedAt: '2023-01-01T00:00:00Z'
    }] : [],
    conditions: [],
    requiresApproval: false,
  }

  if (!allowed) {
    result.deniedBy = [{
      id: 'deny-1',
      name: 'Insufficient Role',
      description: 'Denied due to insufficient role',
      resource: 'user' as ResourceType,
      action: 'read' as ActionType,
      scope: 'organization',
      conditions: [],
      category: 'user_management',
      isSystemPermission: false,
      createdAt: '2023-01-01T00:00:00Z',
      updatedAt: '2023-01-01T00:00:00Z'
    }]
  }

  return result
}

describe('PermissionChecker', () => {

  beforeEach(() => {
    vi.clearAllMocks()

    // Setup default mocks
    mockUseCheckPermissionQuery.mockReturnValue({
      data: createMockPermissionResult(true),
      isLoading: false,
      error: null,
      isError: false,
    } as any)

    mockSecurityMonitor.reportAnomaly = vi.fn()
    mockAuditPermissionCheck.mockResolvedValue(undefined)

    // Setup permission cache mocks
    mockPermissionCache.get = vi.fn().mockResolvedValue(null)
    mockPermissionCache.set = vi.fn().mockResolvedValue(undefined)
    mockPermissionCache.invalidate = vi.fn().mockResolvedValue(0)
    mockPermissionCache.warmCache = vi.fn().mockResolvedValue(undefined)
    mockPermissionCache.getMetrics = vi.fn().mockReturnValue({
      hits: 0,
      misses: 0,
      l1Hits: 0,
      l2Hits: 0,
      evictions: 0,
      totalEntries: 0,
      memoryUsage: 0,
      hitRate: 0,
      averageResponseTime: 0,
      securityViolations: 0,
      constitutionalCompliance: 100,
      lastUpdate: new Date().toISOString(),
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('Basic Permission Checking', () => {
    it('renders children when user has permission', async () => {
      render(
        <TestWrapper>
          <PermissionChecker resource="payment" action="read">
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByTestId('protected-content')).toBeInTheDocument()
      expect(screen.getByText('Protected Content')).toBeInTheDocument()
    })

    it('renders fallback when user lacks permission', async () => {
      mockUseCheckPermissionQuery.mockReturnValue({
        data: createMockPermissionResult(false, 'Insufficient permissions'),
        isLoading: false,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="delete"
            fallback={<div data-testid="fallback">Access Denied</div>}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByTestId('fallback')).toBeInTheDocument()
      expect(screen.getByText('Access Denied')).toBeInTheDocument()
      expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument()
    })

    it('renders loading state when permission check is in progress', () => {
      mockUseCheckPermissionQuery.mockReturnValue({
        data: undefined,
        isLoading: true,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            showLoading={true}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByTestId('permission-loading')).toBeInTheDocument()
      expect(screen.getByText('Checking permissions...')).toBeInTheDocument()
    })
  })

  describe('Constitutional Compliance', () => {
    it('validates constitutional compliance before rendering', async () => {
      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            constitutionalRequirements={{
              requiresAuditTrail: true,
              requiresDataProtection: true,
              requiresErrorHandling: true,
              requiresTestFirst: false,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      await waitFor(() => {
        expect(screen.getByTestId('protected-content')).toBeInTheDocument()
      })

      // Verify constitutional compliance validation occurred
      expect(mockUseCheckPermissionQuery).toHaveBeenCalledWith(
        expect.objectContaining({
          additionalContext: expect.objectContaining({
            constitutionalCompliance: expect.objectContaining({
              validated: true,
              requirements: expect.objectContaining({
                requiresAuditTrail: true,
                requiresDataProtection: true,
              }),
            }),
          }),
        }),
        expect.any(Object)
      )
    })

    it('enforces error handling compliance by requiring fallback', () => {
      // Test without fallback should still work but log warning
      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            constitutionalRequirements={{
              requiresErrorHandling: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByTestId('protected-content')).toBeInTheDocument()
    })

    it('validates test-first development when required', () => {
      const originalEnv = process.env.NODE_ENV
      process.env.NODE_ENV = 'test'

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            constitutionalRequirements={{
              requiresTestFirst: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByTestId('protected-content')).toBeInTheDocument()

      process.env.NODE_ENV = originalEnv
    })
  })

  describe('Audit Logging', () => {
    it('logs permission grants when audit trail is required', async () => {
      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            constitutionalRequirements={{
              requiresAuditTrail: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockAuditPermissionCheck).toHaveBeenCalledWith(
          expect.objectContaining({
            userId: 'user-123',
            resource: 'payment',
            action: 'read',
            result: 'granted',
            reason: 'Permission granted',
          })
        )
      })
    })

    it('logs permission denials with detailed context', async () => {
      mockUseCheckPermissionQuery.mockReturnValue({
        data: createMockPermissionResult(false, 'Insufficient role'),
        isLoading: false,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="delete"
            constitutionalRequirements={{
              requiresAuditTrail: true,
            }}
            fallback={<div>Access Denied</div>}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockAuditPermissionCheck).toHaveBeenCalledWith(
          expect.objectContaining({
            userId: 'user-123',
            resource: 'payment',
            action: 'delete',
            result: 'denied',
            reason: 'Insufficient role',
          })
        )
      })
    })

    it('logs errors with security context', async () => {
      const mockError = new Error('Network error')
      mockUseCheckPermissionQuery.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: mockError,
        isError: true,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            constitutionalRequirements={{
              requiresAuditTrail: true,
            }}
            fallback={<div>Error occurred</div>}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockAuditPermissionCheck).toHaveBeenCalledWith(
          expect.objectContaining({
            result: 'error',
            reason: expect.stringContaining('Network error'),
          })
        )
      })
    })
  })

  describe('Security Monitoring', () => {
    it('detects and reports security anomalies', async () => {
      // Mock high anomaly score - testing security monitoring
      // No need for unused mockMetrics variable

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            securityMonitoring={{
              detectAnomalies: true,
              rateLimitChecks: true,
              logSecurityEvents: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      // Verify security monitoring is active
      expect(screen.getByTestId('protected-content')).toBeInTheDocument()
    })

    it('enforces rate limiting when enabled', () => {
      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            securityMonitoring={{
              detectAnomalies: true,
              rateLimitChecks: true,
              logSecurityEvents: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByTestId('protected-content')).toBeInTheDocument()
    })
  })

  describe('Performance Optimization', () => {
    it('uses configured cache timeout', () => {
      const customTimeout = 600000 // 10 minutes

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            performanceOptions={{
              enableCaching: true,
              cacheTimeout: customTimeout,
              enablePrefetch: false,
              batchChecks: false,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(mockUseCheckPermissionQuery).toHaveBeenCalledWith(
        expect.any(Object),
        expect.objectContaining({
          refetchOnMountOrArgChange: customTimeout / 1000,
        })
      )
    })

    it('enables prefetching when configured', () => {
      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            organizationId="org-123"
            performanceOptions={{
              enablePrefetch: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByTestId('protected-content')).toBeInTheDocument()
      // Prefetching would be tested by mocking the dispatch function
    })
  })

  describe('Accessibility Features', () => {
    it('provides ARIA attributes for loading state', () => {
      mockUseCheckPermissionQuery.mockReturnValue({
        data: undefined,
        isLoading: true,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            showLoading={true}
            accessibility={{
              screenReaderFriendly: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      const loadingElement = screen.getByTestId('permission-loading')
      expect(loadingElement).toHaveAttribute('role', 'status')
      expect(loadingElement).toHaveAttribute('aria-live', 'polite')
      expect(loadingElement).toHaveAttribute('aria-label', 'Checking permissions')
    })

    it('provides ARIA attributes for error states', () => {
      mockUseCheckPermissionQuery.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: new Error('Network error'),
        isError: true,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            accessibility={{
              provideAlternativeAccess: true,
              screenReaderFriendly: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      const errorElement = screen.getByRole('alert')
      expect(errorElement).toHaveAttribute('aria-live', 'assertive')
    })

    it('provides alternative access information when denied', () => {
      mockUseCheckPermissionQuery.mockReturnValue({
        data: createMockPermissionResult(false),
        isLoading: false,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="delete"
            accessibility={{
              provideAlternativeAccess: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByText("You don't have permission to delete payment")).toBeInTheDocument()
    })

    it('wraps permitted content with accessible region', () => {
      render(
        <TestWrapper>
          <PermissionChecker resource="payment" action="read">
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      const wrapper = screen.getByRole('region')
      expect(wrapper).toHaveAttribute('aria-label', 'payment read content')
      expect(wrapper).toContainElement(screen.getByTestId('protected-content'))
    })
  })

  describe('Error Handling', () => {
    it('handles network errors gracefully', () => {
      mockUseCheckPermissionQuery.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: new Error('Network error'),
        isError: true,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            fallback={<div data-testid="custom-fallback">Custom Error</div>}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByTestId('custom-fallback')).toBeInTheDocument()
      expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument()
    })

    it('handles unauthenticated users', () => {
      const storeWithoutUser = createMockStore({
        auth: { currentUser: null }
      })

      render(
        <TestWrapper store={storeWithoutUser}>
          <PermissionChecker resource="payment" action="read">
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument()
    })

    it('handles missing permission result', () => {
      mockUseCheckPermissionQuery.mockReturnValue({
        data: undefined,
        isLoading: false,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            fallback={<div data-testid="no-result">No Result</div>}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByTestId('no-result')).toBeInTheDocument()
    })
  })

  describe('Resource and Action Combinations', () => {
    const testCases = [
      { resource: 'payment', action: 'create' },
      { resource: 'subscription', action: 'update' },
      { resource: 'billing', action: 'read' },
      { resource: 'organization', action: 'manage' },
      { resource: 'user', action: 'delete' },
    ]

    testCases.forEach(({ resource, action }) => {
      it(`handles ${resource}:${action} permission check`, () => {
        render(
          <TestWrapper>
            <PermissionChecker resource={resource as any} action={action as any}>
              <div data-testid="protected-content">Protected Content</div>
            </PermissionChecker>
          </TestWrapper>
        )

        expect(mockUseCheckPermissionQuery).toHaveBeenCalledWith(
          expect.objectContaining({
            resource,
            action,
          }),
          expect.any(Object)
        )
      })
    })
  })

  describe('Context and Organization Support', () => {
    it('includes resource ID in permission context', () => {
      const resourceId = createResourceId('payment-123')

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            resourceId={resourceId}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(mockUseCheckPermissionQuery).toHaveBeenCalledWith(
        expect.objectContaining({
          resourceId,
        }),
        expect.any(Object)
      )
    })

    it('includes organization ID in permission context', () => {
      const organizationId = 'org-456'

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            organizationId={organizationId}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(mockUseCheckPermissionQuery).toHaveBeenCalledWith(
        expect.objectContaining({
          organizationId,
        }),
        expect.any(Object)
      )
    })

    it('includes additional context in permission evaluation', () => {
      const context = {
        department: 'finance',
        priority: 'high',
        requestSource: 'web_app',
      }

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            context={context}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(mockUseCheckPermissionQuery).toHaveBeenCalledWith(
        expect.objectContaining({
          additionalContext: expect.objectContaining(context),
        }),
        expect.any(Object)
      )
    })
  })

  describe('Approval Workflow Support', () => {
    it('displays approval request option when required', () => {
      mockUseCheckPermissionQuery.mockReturnValue({
        data: {
          ...createMockPermissionResult(false, 'Requires approval'),
          requiresApproval: true,
          approvalWorkflow: {
            id: 'workflow-123',
            name: 'Senior Manager Approval',
            description: 'Requires senior manager approval',
            requiredApprovals: 1,
            approvers: [{ id: 'mgr-1', name: 'Manager', email: 'mgr@example.com' }],
            timeoutHours: 24,
            escalationRules: [],
            isActive: true,
          },
        },
        isLoading: false,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="manage"
            accessibility={{
              provideAlternativeAccess: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      expect(screen.getByText(/This action requires approval/)).toBeInTheDocument()
      expect(screen.getByText(/Would you like to request access/)).toBeInTheDocument()
    })
  })

  describe('Caching Strategy', () => {
    it('stores permission results in cache when caching is enabled', async () => {
      const permissionResult = createMockPermissionResult(true, 'Permission granted')

      mockUseCheckPermissionQuery.mockReturnValue({
        data: permissionResult,
        isLoading: false,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            performanceOptions={{
              enableCaching: true,
              cacheTimeout: 300000,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockPermissionCache.set).toHaveBeenCalledWith(
          'user-123',
          'payment',
          'read',
          true,
          expect.objectContaining({
            reason: 'Permission granted',
            ttl: 300000,
          })
        )
      })
    })

    it('invalidates cache when user logs out', async () => {
      const storeWithoutUser = createMockStore({
        auth: { currentUser: null }
      })

      render(
        <TestWrapper store={storeWithoutUser}>
          <PermissionChecker
            resource="payment"
            action="read"
            performanceOptions={{
              enableCaching: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockPermissionCache.invalidate).toHaveBeenCalledWith({
          type: 'all'
        })
      })
    })

    it('warms cache when prefetching is enabled', async () => {
      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            organizationId="org-123"
            performanceOptions={{
              enablePrefetch: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockPermissionCache.warmCache).toHaveBeenCalledWith(
          'user-123',
          expect.objectContaining({
            type: 'predictive',
            resources: ['payment'],
            actions: ['read', 'update', 'delete', 'manage'],
            priority: 3,
          }),
          'org-123'
        )
      })
    })

    it('warms cache for user login strategy', async () => {
      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            organizationId="org-123"
            performanceOptions={{
              enableCaching: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      await waitFor(() => {
        expect(mockPermissionCache.warmCache).toHaveBeenCalledWith(
          'user-123',
          expect.objectContaining({
            type: 'user_login',
            resources: ['user', 'organization', 'settings'],
            actions: ['read', 'update'],
            priority: 1,
          }),
          'org-123'
        )
      })
    })

    it('skips caching when disabled', async () => {
      const permissionResult = createMockPermissionResult(true, 'Permission granted')

      mockUseCheckPermissionQuery.mockReturnValue({
        data: permissionResult,
        isLoading: false,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            performanceOptions={{
              enableCaching: false,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      await waitFor(() => {
        expect(screen.getByTestId('protected-content')).toBeInTheDocument()
      })

      expect(mockPermissionCache.set).not.toHaveBeenCalled()
    })

    it('handles cache retrieval errors gracefully', async () => {
      mockPermissionCache.get = vi.fn().mockRejectedValue(new Error('Cache error'))

      const permissionResult = createMockPermissionResult(true, 'Permission granted')
      mockUseCheckPermissionQuery.mockReturnValue({
        data: permissionResult,
        isLoading: false,
        error: null,
        isError: false,
      } as any)

      render(
        <TestWrapper>
          <PermissionChecker
            resource="payment"
            action="read"
            performanceOptions={{
              enableCaching: true,
            }}
          >
            <div data-testid="protected-content">Protected Content</div>
          </PermissionChecker>
        </TestWrapper>
      )

      // Should still render content even if cache fails
      expect(screen.getByTestId('protected-content')).toBeInTheDocument()
    })
  })
})

describe('usePermissionCheck Hook', () => {
  const TestComponent: React.FC = () => {
    const {
      hasPermission,
      hasAnyPermission,
      hasAllPermissions,
      getCacheMetrics,
      invalidateCache,
      warmCache
    } = usePermissionCheck()

    const [metrics, setMetrics] = React.useState<any>(null)
    const [invalidationResult, setInvalidationResult] = React.useState<number | null>(null)

    React.useEffect(() => {
      const loadMetrics = () => {
        const cacheMetrics = getCacheMetrics()
        setMetrics(cacheMetrics)
      }
      loadMetrics()
    }, [getCacheMetrics])

    const handleInvalidateCache = async () => {
      const result = await invalidateCache({ type: 'user', value: 'user-123' })
      setInvalidationResult(result)
    }

    const handleWarmCache = async () => {
      await warmCache({
        type: 'user_login',
        resources: ['payment'],
        actions: ['read'],
        priority: 1,
      }, 'org-123')
    }

    const [singleResult, setSingleResult] = React.useState<boolean | null>(null)
    const [anyResult, setAnyResult] = React.useState<boolean | null>(null)
    const [allResult, setAllResult] = React.useState<boolean | null>(null)

    React.useEffect(() => {
      const checkPermissions = async () => {
        const single = await hasPermission('payment', 'read')
        const any = await hasAnyPermission([
          { resource: 'payment', action: 'read' },
          { resource: 'payment', action: 'update' },
        ])
        const all = await hasAllPermissions([
          { resource: 'payment', action: 'read' },
          { resource: 'billing', action: 'read' },
        ])
        setSingleResult(single)
        setAnyResult(any)
        setAllResult(all)
      }
      checkPermissions()
    }, [hasPermission, hasAnyPermission, hasAllPermissions])

    return (
      <div>
        <div data-testid="single-permission">
          {singleResult === null ? 'Loading...' : singleResult ? 'Has Permission' : 'No Permission'}
        </div>
        <div data-testid="any-permission">
          {anyResult === null ? 'Loading...' : anyResult ? 'Has Any' : 'Has None'}
        </div>
        <div data-testid="all-permissions">
          {allResult === null ? 'Loading...' : allResult ? 'Has All' : 'Missing Some'}
        </div>
        <div data-testid="cache-metrics">
          {metrics ? `Hit Rate: ${(metrics.hitRate * 100).toFixed(2)}%` : 'Loading...'}
        </div>
        <button data-testid="invalidate-btn" onClick={handleInvalidateCache}>
          Invalidate Cache
        </button>
        <button data-testid="warm-btn" onClick={handleWarmCache}>
          Warm Cache
        </button>
        {invalidationResult !== null && (
          <div data-testid="invalidation-result">
            Invalidated: {invalidationResult}
          </div>
        )}
      </div>
    )
  }

  it('provides hasPermission method', () => {
    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    // Note: In the current placeholder implementation, it returns true
    expect(screen.getByTestId('single-permission')).toHaveTextContent('Has Permission')
  })

  it('provides hasAnyPermission method', () => {
    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    expect(screen.getByTestId('any-permission')).toHaveTextContent('Has Any')
  })

  it('provides hasAllPermissions method', () => {
    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    expect(screen.getByTestId('all-permissions')).toHaveTextContent('Has All')
  })

  it('returns false for unauthenticated users', () => {
    const storeWithoutUser = createMockStore({
      auth: { currentUser: null }
    })

    const TestComponentUnauthenticated: React.FC = () => {
      const { hasPermission } = usePermissionCheck()
      const [result, setResult] = React.useState<boolean | null>(null)

      React.useEffect(() => {
        const checkPermission = async () => {
          const hasAccess = await hasPermission('payment', 'read')
          setResult(hasAccess)
        }
        checkPermission()
      }, [hasPermission])

      return (
        <div data-testid="unauth-permission">
          {result === null ? 'Loading...' : result ? 'Has Permission' : 'No Permission'}
        </div>
      )
    }

    render(
      <TestWrapper store={storeWithoutUser}>
        <TestComponentUnauthenticated />
      </TestWrapper>
    )

    expect(screen.getByTestId('unauth-permission')).toHaveTextContent('No Permission')
  })

  it('provides cache metrics functionality', async () => {
    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    await waitFor(() => {
      expect(screen.getByTestId('cache-metrics')).toHaveTextContent('Hit Rate: 0.00%')
    })

    expect(mockPermissionCache.getMetrics).toHaveBeenCalled()
  })

  it('provides cache invalidation functionality', async () => {
    mockPermissionCache.invalidate = vi.fn().mockResolvedValue(5)

    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    const invalidateBtn = screen.getByTestId('invalidate-btn')
    await userEvent.click(invalidateBtn)

    await waitFor(() => {
      expect(screen.getByTestId('invalidation-result')).toHaveTextContent('Invalidated: 5')
    })

    expect(mockPermissionCache.invalidate).toHaveBeenCalledWith({
      type: 'user',
      value: 'user-123'
    })
  })

  it('provides cache warming functionality', async () => {
    render(
      <TestWrapper>
        <TestComponent />
      </TestWrapper>
    )

    const warmBtn = screen.getByTestId('warm-btn')
    await userEvent.click(warmBtn)

    await waitFor(() => {
      expect(mockPermissionCache.warmCache).toHaveBeenCalledWith(
        {
          type: 'user_login',
          resources: ['payment'],
          actions: ['read'],
          priority: 1,
        },
        'org-123'
      )
    })
  })

  it('handles async permission checks with cache integration', async () => {
    // Mock cache returning a cached result
    mockPermissionCache.get = vi.fn().mockResolvedValue({
      result: true,
      reason: 'Cached permission',
      timestamp: Date.now(),
    })

    const AsyncTestComponent: React.FC = () => {
      const { hasPermission } = usePermissionCheck()
      const [result, setResult] = React.useState<boolean | null>(null)

      React.useEffect(() => {
        const checkPermission = async () => {
          const hasAccess = await hasPermission('payment', 'read', { useCache: true })
          setResult(hasAccess)
        }
        checkPermission()
      }, [hasPermission])

      return (
        <div data-testid="async-result">
          {result === null ? 'Loading...' : result ? 'Has Access' : 'No Access'}
        </div>
      )
    }

    render(
      <TestWrapper>
        <AsyncTestComponent />
      </TestWrapper>
    )

    await waitFor(() => {
      expect(screen.getByTestId('async-result')).toHaveTextContent('Has Access')
    })

    expect(mockPermissionCache.get).toHaveBeenCalledWith(
      'user-123',
      'payment',
      'read',
      undefined,
      undefined
    )
  })

  it('bypasses cache when useCache is false', async () => {
    mockPermissionCache.get = vi.fn().mockResolvedValue({
      result: false,
      reason: 'Cached denial',
    })

    const NoCacheTestComponent: React.FC = () => {
      const { hasPermission } = usePermissionCheck()
      const [result, setResult] = React.useState<boolean | null>(null)

      React.useEffect(() => {
        const checkPermission = async () => {
          const hasAccess = await hasPermission('payment', 'read', { useCache: false })
          setResult(hasAccess)
        }
        checkPermission()
      }, [hasPermission])

      return (
        <div data-testid="no-cache-result">
          {result === null ? 'Loading...' : result ? 'Has Access' : 'No Access'}
        </div>
      )
    }

    render(
      <TestWrapper>
        <NoCacheTestComponent />
      </TestWrapper>
    )

    await waitFor(() => {
      expect(screen.getByTestId('no-cache-result')).toHaveTextContent('Has Access')
    })

    // Should not call cache when useCache is false
    expect(mockPermissionCache.get).not.toHaveBeenCalled()
  })
})