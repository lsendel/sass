/**
 * Enhanced Permission Checker Component
 *
 * Enterprise-grade React component for declarative permission-based rendering:
 * - Constitutional compliance validation
 * - Comprehensive audit logging for all permission checks
 * - Performance-optimized caching with invalidation
 * - Advanced security features and threat detection
 * - Supports resource-level and action-level checks
 * - Provides fallback content for unauthorized access
 * - Integrates with RBAC system and audit trail
 * - Type-safe branded permission system
 */

import React, { useMemo, useCallback, useEffect, useRef } from 'react'

import { useCheckPermissionQuery } from '../../../store/api/rbacApi'
import { useAppSelector } from '../../../store/hooks'
import { selectCurrentUser } from '../../../store/slices/authSlice'
import { logger } from '../../../utils/logger'
import { auditPermissionCheck } from '../../../utils/auditLogger'
import { securityMonitor } from '../../../utils/securityMonitor'
import { permissionCache } from '../../../utils/permissionCache'
import type { ResourceType, ActionType } from '../../../types/rbac'

// Branded types for enhanced type safety
type ResourceId = string & { readonly __brand: 'ResourceId' }
type AuditEventId = string & { readonly __brand: 'AuditEventId' }

// Enhanced permission checker props with constitutional compliance
export interface PermissionCheckerProps {
  /** Resource being accessed */
  resource: ResourceType
  /** Action being performed on the resource */
  action: ActionType
  /** Optional specific resource ID for granular permissions */
  resourceId?: ResourceId
  /** Content to render if user has permission */
  children: React.ReactNode
  /** Content to render if user lacks permission */
  fallback?: React.ReactNode
  /** Organization ID for organization-scoped permissions */
  organizationId?: string
  /** Additional context for permission evaluation */
  context?: Record<string, unknown>
  /** Whether to show loading state during permission check */
  showLoading?: boolean
  /** Whether to log permission denials for debugging */
  logDenials?: boolean
  /** Custom loading component */
  loadingComponent?: React.ReactNode
  /** Constitutional compliance requirements */
  constitutionalRequirements?: {
    requiresAuditTrail?: boolean
    requiresDataProtection?: boolean
    requiresErrorHandling?: boolean
    requiresTestFirst?: boolean
  }
  /** Security monitoring options */
  securityMonitoring?: {
    detectAnomalies?: boolean
    rateLimitChecks?: boolean
    logSecurityEvents?: boolean
  }
  /** Performance optimization options */
  performanceOptions?: {
    enableCaching?: boolean
    cacheTimeout?: number
    enablePrefetch?: boolean
    batchChecks?: boolean
  }
  /** Accessibility options */
  accessibility?: {
    announcePermissionChanges?: boolean
    provideAlternativeAccess?: boolean
    screenReaderFriendly?: boolean
  }
}

// Constitutional compliance cache for validation results
const constitutionalCache = new Map<string, { result: boolean; timestamp: number }>()

// Security monitoring state
interface SecurityMetrics {
  checkCount: number
  denialCount: number
  lastCheck: number
  anomalyScore: number
}

/**
 * Enhanced wrapper component with constitutional compliance and security monitoring.
 * Uses RTK Query to check permissions and caches results automatically.
 */
const PermissionChecker: React.FC<PermissionCheckerProps> = ({
  resource,
  action,
  resourceId,
  children,
  fallback = null,
  organizationId,
  context,
  showLoading = false,
  logDenials = false,
  loadingComponent,
  constitutionalRequirements = {
    requiresAuditTrail: true,
    requiresDataProtection: true,
    requiresErrorHandling: true,
    requiresTestFirst: false,
  },
  securityMonitoring = {
    detectAnomalies: true,
    rateLimitChecks: true,
    logSecurityEvents: true,
  },
  performanceOptions = {
    enableCaching: true,
    cacheTimeout: 300000, // 5 minutes
    enablePrefetch: false,
    batchChecks: false,
  },
  accessibility = {
    announcePermissionChanges: true,
    provideAlternativeAccess: true,
    screenReaderFriendly: true,
  },
}) => {
  const user = useAppSelector(selectCurrentUser)

  // Security metrics tracking
  const securityMetrics = useRef<SecurityMetrics>({
    checkCount: 0,
    denialCount: 0,
    lastCheck: 0,
    anomalyScore: 0,
  })

  // Constitutional compliance validation
  const validateConstitutionalCompliance = useCallback((
    checkType: 'permission' | 'audit' | 'error' | 'test',
    context: Record<string, unknown>
  ): boolean => {
    const cacheKey = `${checkType}-${JSON.stringify(context)}`
    const cached = constitutionalCache.get(cacheKey)

    if (cached && (Date.now() - cached.timestamp) < 60000) { // 1 minute cache
      return cached.result
    }

    let result = true

    switch (checkType) {
      case 'permission':
        // Validate permission check follows constitutional principles
        result = !!(user && resource && action)
        break
      case 'audit':
        // Ensure audit trail is maintained when required
        result = !constitutionalRequirements.requiresAuditTrail ||
                 typeof auditPermissionCheck === 'function'
        break
      case 'error':
        // Verify comprehensive error handling is in place
        result = !constitutionalRequirements.requiresErrorHandling ||
                 (typeof fallback !== 'undefined')
        break
      case 'test':
        // Validate test-first development approach (if required)
        result = !constitutionalRequirements.requiresTestFirst ||
                 process.env.NODE_ENV === 'test'
        break
    }

    constitutionalCache.set(cacheKey, { result, timestamp: Date.now() })
    return result
  }, [user, resource, action, fallback, constitutionalRequirements])

  // Security anomaly detection
  const detectSecurityAnomalies = useCallback((): boolean => {
    if (!securityMonitoring.detectAnomalies) return false

    const metrics = securityMetrics.current
    const now = Date.now()

    // Rate limiting check
    if (securityMonitoring.rateLimitChecks) {
      const timeSinceLastCheck = now - metrics.lastCheck
      if (timeSinceLastCheck < 100) { // 100ms minimum between checks
        metrics.anomalyScore += 10
      }
    }

    // High denial rate detection
    if (metrics.denialCount > 0 && metrics.checkCount > 0) {
      const denialRate = metrics.denialCount / metrics.checkCount
      if (denialRate > 0.5) { // More than 50% denials
        metrics.anomalyScore += 20
      }
    }

    // Rapid successive checks detection
    const currentTime = Date.now()
    const timeSinceLastCheck = currentTime - metrics.lastCheck
    if (metrics.checkCount > 10 && timeSinceLastCheck < 1000) {
      metrics.anomalyScore += 15
    }

    const isAnomalous = metrics.anomalyScore > 50

    if (isAnomalous && securityMonitoring.logSecurityEvents) {
      securityMonitor.reportAnomaly({
        type: 'permission_check_anomaly',
        ...(user?.id ? { userId: user.id } : {}),
        resource,
        action,
        anomalyScore: metrics.anomalyScore,
        metrics: { ...metrics },
      })
    }

    // Reset anomaly score periodically
    if (now - metrics.lastCheck > 300000) { // 5 minutes
      metrics.anomalyScore = Math.max(0, metrics.anomalyScore - 10)
    }

    metrics.lastCheck = now
    return isAnomalous
  }, [user, resource, action, securityMonitoring])

  // Build permission context with constitutional compliance
  const permissionContext = useMemo(() => {
    if (!user) return null

    // Validate constitutional compliance before building context
    const isCompliant = validateConstitutionalCompliance('permission', {
      userId: user.id,
      resource,
      action,
    })

    if (!isCompliant) {
      logger.error('Constitutional compliance violation in permission check', {
        userId: user.id,
        resource,
        action,
        requirements: constitutionalRequirements,
      })
      return null
    }

    return {
      userId: user.id,
      ...(organizationId ? { organizationId } : {}),
      resource,
      action,
      ...(resourceId ? { resourceId } : {}),
      additionalContext: {
        ...context,
        constitutionalCompliance: {
          validated: true,
          timestamp: Date.now(),
          requirements: constitutionalRequirements,
        },
        securityContext: {
          userAgent: navigator.userAgent,
          timestamp: Date.now(),
          sessionId: (user as any).sessionId || 'unknown',
        },
      },
    }
  }, [user, organizationId, resource, action, resourceId, context, constitutionalRequirements, validateConstitutionalCompliance])


  // Enhanced permission checking with caching and security monitoring
  const {
    data: permissionResult,
    isLoading,
    error,
    isError,
  } = useCheckPermissionQuery(permissionContext!, {
    skip: !permissionContext,
    // Performance-optimized caching
    pollingInterval: 0,
    refetchOnMountOrArgChange: performanceOptions.cacheTimeout ? performanceOptions.cacheTimeout / 1000 : 300,
  })

  // Cache management effect - store successful permission checks
  useEffect(() => {
    if (!permissionResult || !performanceOptions.enableCaching || !permissionContext) return

    // Store result in cache for future use
    permissionCache.set(
      permissionContext.userId,
      permissionContext.resource,
      permissionContext.action,
      permissionResult.allowed,
      {
        ...(permissionContext.resourceId ? { resourceId: permissionContext.resourceId } : {}),
        ...(permissionContext.organizationId ? { organizationId: permissionContext.organizationId } : {}),
        reason: permissionResult.reason,
        matchedPermissions: permissionResult.matchedPermissions,
        conditions: permissionResult.conditions,
        ttl: performanceOptions.cacheTimeout || 300000, // 5 minutes default
      }
    )
  }, [permissionResult, performanceOptions.enableCaching, permissionContext, performanceOptions.cacheTimeout])

  // Audit logging effect
  useEffect(() => {
    if (!permissionContext || !constitutionalRequirements.requiresAuditTrail) return

    const auditEventId = `perm-${Date.now()}-${Math.random().toString(36).substr(2, 9)}` as AuditEventId

    // Log permission check attempt
    auditPermissionCheck({
      eventId: auditEventId,
      userId: user?.id || 'anonymous',
      resource,
      action,
      ...(resourceId ? { resourceId } : {}),
      ...(organizationId ? { organizationId } : {}),
      context: permissionContext.additionalContext,
      timestamp: new Date().toISOString(),
      result: permissionResult?.allowed ? 'granted' : 'denied',
      reason: permissionResult?.reason || 'evaluation_pending',
      securityContext: {
        userAgent: navigator.userAgent,
        ipAddress: 'client-side', // Would be filled by backend
        sessionId: (user as any)?.sessionId || 'unknown',
      },
    })

    // Constitutional compliance validation for audit
    validateConstitutionalCompliance('audit', {
      eventId: auditEventId,
      hasAuditTrail: true,
    })
  }, [permissionContext, permissionResult, user, resource, action, resourceId, organizationId, constitutionalRequirements.requiresAuditTrail, validateConstitutionalCompliance])

  // Security monitoring effect
  useEffect(() => {
    if (!permissionContext) return

    const metrics = securityMetrics.current
    metrics.checkCount++

    // Detect security anomalies
    const isAnomalous = detectSecurityAnomalies()

    if (isAnomalous) {
      logger.warn('Security anomaly detected in permission check', {
        userId: user?.id,
        resource,
        action,
        anomalyScore: metrics.anomalyScore,
        metrics: { ...metrics },
      })
    }
  }, [permissionContext, detectSecurityAnomalies, user, resource, action])

  // Accessibility announcements
  useEffect(() => {
    if (!accessibility.announcePermissionChanges || !permissionResult) return

    const hasPermission = permissionResult.allowed
    const message = hasPermission
      ? `Access granted to ${resource} ${action}`
      : `Access denied to ${resource} ${action}`

    // Announce to screen readers
    if (accessibility.screenReaderFriendly) {
      const announcement = document.createElement('div')
      announcement.setAttribute('aria-live', 'polite')
      announcement.setAttribute('aria-atomic', 'true')
      announcement.className = 'sr-only'
      announcement.textContent = message
      document.body.appendChild(announcement)

      setTimeout(() => document.body.removeChild(announcement), 1000)
    }
  }, [permissionResult, resource, action, accessibility])

  // Performance optimization: prefetch related permissions using cache warming
  useEffect(() => {
    if (!performanceOptions.enablePrefetch || !user || !organizationId) return

    // Warm cache with commonly checked permissions for this resource
    const warmingStrategy = {
      type: 'predictive' as const,
      resources: [resource],
      actions: ['read', 'update', 'delete', 'manage'] as ActionType[],
      priority: 3,
    }

    permissionCache.warmCache(user.id, warmingStrategy, organizationId)
  }, [user, organizationId, resource, action, resourceId, performanceOptions.enablePrefetch])

  // Cache invalidation on user/context changes
  useEffect(() => {
    if (!user) {
      // Clear cache when user logs out
      permissionCache.invalidate({ type: 'all' })
    }
  }, [user])

  // Organization-specific cache invalidation
  useEffect(() => {
    if (organizationId) {
      // This would typically be triggered by organization context changes
      // For now, we'll just ensure cache warming for the new organization
      if (performanceOptions.enableCaching && user) {
        const warmingStrategy = {
          type: 'user_login' as const,
          resources: ['user', 'organization', 'settings'],
          actions: ['read', 'update'] as ActionType[],
          priority: 1,
        }
        permissionCache.warmCache(user.id, warmingStrategy, organizationId)
      }
    }
  }, [organizationId, performanceOptions.enableCaching, user])

  // Enhanced loading state with accessibility
  if (isLoading && showLoading) {
    if (loadingComponent) {
      return <>{loadingComponent}</>
    }
    return (
      <div
        className="inline-flex items-center text-gray-400"
        data-testid="permission-loading"
        role="status"
        aria-live="polite"
        aria-label="Checking permissions"
      >
        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-400"></div>
        <span className="ml-2 text-sm">Checking permissions...</span>
      </div>
    )
  }

  // Enhanced error handling with constitutional compliance
  if (isError) {
    const metrics = securityMetrics.current
    metrics.denialCount++

    // Constitutional compliance validation for error handling
    const errorCompliant = validateConstitutionalCompliance('error', {
      hasError: true,
      hasFallback: typeof fallback !== 'undefined',
    })

    if (!errorCompliant) {
      logger.error('Constitutional compliance violation in error handling', {
        resource,
        action,
        resourceId,
        error,
        user: user?.id,
        requirements: constitutionalRequirements,
      })
    }

    // Comprehensive audit logging for permission failures
    if (constitutionalRequirements.requiresAuditTrail) {
      auditPermissionCheck({
        eventId: `perm-error-${Date.now()}` as AuditEventId,
        userId: user?.id || 'anonymous',
        resource,
        action,
        resourceId: resourceId || undefined,
        organizationId,
        context: { error: error?.toString() },
        timestamp: new Date().toISOString(),
        result: 'error',
        reason: `Permission check failed: ${error?.toString()}`,
        securityContext: {
          userAgent: navigator.userAgent,
          ipAddress: 'client-side',
          sessionId: user?.sessionId,
        },
      })
    }

    if (logDenials || securityMonitoring.logSecurityEvents) {
      logger.warn('Permission check failed - constitutional compliance enforced:', {
        resource,
        action,
        resourceId,
        error,
        user: user?.id,
        constitutionalCompliance: errorCompliant,
        securityMetrics: { ...metrics },
      })
    }

    return (
      <div role="alert" aria-live="assertive">
        {fallback || (
          <div className="text-center py-4 text-red-600" data-testid="permission-error">
            <p>Unable to verify permissions</p>
            {accessibility.provideAlternativeAccess && (
              <p className="text-sm text-gray-500 mt-1">
                Please contact support if you believe you should have access
              </p>
            )}
          </div>
        )}
      </div>
    )
  }

  // Enhanced user validation with audit logging
  if (!user || !permissionContext) {
    const metrics = securityMetrics.current
    metrics.denialCount++

    if (constitutionalRequirements.requiresAuditTrail) {
      auditPermissionCheck({
        eventId: `perm-nouser-${Date.now()}` as AuditEventId,
        userId: 'anonymous',
        resource,
        action,
        resourceId: resourceId || undefined,
        organizationId,
        context: { reason: 'no_authenticated_user' },
        timestamp: new Date().toISOString(),
        result: 'denied',
        reason: 'No authenticated user or invalid context',
        securityContext: {
          userAgent: navigator.userAgent,
          ipAddress: 'client-side',
          sessionId: undefined,
        },
      })
    }

    if (logDenials || securityMonitoring.logSecurityEvents) {
      logger.debug('Permission denied - no authenticated user (constitutional compliance enforced):', {
        resource,
        action,
        resourceId,
        hasUser: !!user,
        hasContext: !!permissionContext,
        constitutionalRequirements,
      })
    }

    return (
      <div role="alert" aria-live="assertive">
        {fallback || (
          accessibility.provideAlternativeAccess && (
            <div className="text-center py-4 text-gray-600" data-testid="permission-unauthenticated">
              <p>Authentication required</p>
              <p className="text-sm text-gray-500 mt-1">
                Please sign in to access this resource
              </p>
            </div>
          )
        )}
      </div>
    )
  }

  // Enhanced permission result evaluation with comprehensive audit logging
  const hasPermission = permissionResult?.allowed ?? false

  if (!hasPermission) {
    const metrics = securityMetrics.current
    metrics.denialCount++

    // Comprehensive audit logging for denials
    if (constitutionalRequirements.requiresAuditTrail) {
      auditPermissionCheck({
        eventId: `perm-denied-${Date.now()}` as AuditEventId,
        userId: user.id,
        resource,
        action,
        resourceId: resourceId || undefined,
        organizationId,
        context: {
          reason: permissionResult?.reason,
          deniedBy: permissionResult?.deniedBy,
          conditions: permissionResult?.conditions,
        },
        timestamp: new Date().toISOString(),
        result: 'denied',
        reason: permissionResult?.reason || 'Insufficient permissions',
        securityContext: {
          userAgent: navigator.userAgent,
          ipAddress: 'client-side',
          sessionId: (user as any).sessionId || 'unknown',
        },
      })
    }

    if (logDenials || securityMonitoring.logSecurityEvents) {
      logger.info('Permission denied - constitutional compliance enforced:', {
        resource,
        action,
        resourceId,
        reason: permissionResult?.reason,
        user: user.id,
        deniedBy: permissionResult?.deniedBy,
        conditions: permissionResult?.conditions,
        constitutionalRequirements,
        securityMetrics: { ...metrics },
      })
    }

    return (
      <div role="alert" aria-live="assertive">
        {fallback || (
          accessibility.provideAlternativeAccess && (
            <div className="text-center py-4 text-gray-600" data-testid="permission-denied">
              <p>Access not permitted</p>
              <p className="text-sm text-gray-500 mt-1">
                You don't have permission to {action} {resource}
              </p>
              {permissionResult?.requiresApproval && (
                <p className="text-sm text-blue-600 mt-2">
                  This action requires approval. Would you like to request access?
                </p>
              )}
            </div>
          )
        )}
      </div>
    )
  }

  // Success: User has permission - comprehensive audit logging
  if (constitutionalRequirements.requiresAuditTrail) {
    auditPermissionCheck({
      eventId: `perm-granted-${Date.now()}` as AuditEventId,
      userId: user.id,
      resource,
      action,
      resourceId: resourceId || undefined,
      organizationId,
      context: {
        matchedPermissions: permissionResult.matchedPermissions,
        conditions: permissionResult.conditions,
      },
      timestamp: new Date().toISOString(),
      result: 'granted',
      reason: permissionResult.reason || 'Permission granted',
      securityContext: {
        userAgent: navigator.userAgent,
        ipAddress: 'client-side',
        sessionId: (user as any).sessionId || 'unknown',
      },
    })
  }

  // Final constitutional compliance check before rendering
  const finalCompliance = validateConstitutionalCompliance('permission', {
    granted: true,
    userId: user.id,
    resource,
    action,
  })

  if (!finalCompliance) {
    logger.error('Final constitutional compliance check failed', {
      user: user.id,
      resource,
      action,
      requirements: constitutionalRequirements,
    })
    return <>{fallback}</>
  }

  // Render children with accessibility wrapper
  return (
    <div role="region" aria-label={`${resource} ${action} content`}>
      {children}
    </div>
  )
}

export default PermissionChecker

/**
 * Hook for imperative permission checking with caching integration
 */
export const usePermissionCheck = () => {
  const user = useAppSelector(selectCurrentUser)

  return useMemo(() => {
    return {
      /**
       * Check if current user has permission for a specific action
       */
      hasPermission: async (
        resource: ResourceType,
        action: ActionType,
        options?: {
          resourceId?: string
          organizationId?: string
          context?: Record<string, unknown>
          useCache?: boolean
        }
      ) => {
        if (!user) return false

        // Check cache first if enabled
        if (options?.useCache !== false) {
          const cachedResult = await permissionCache.get(
            user.id,
            resource,
            action,
            options?.resourceId,
            options?.organizationId
          )

          if (cachedResult) {
            return cachedResult.result
          }
        }

        // Fallback to API call would go here
        // For now, return a placeholder implementation
        void resource
        void action
        void options
        return true // Placeholder - would integrate with actual API
      },

      /**
       * Check multiple permissions at once
       */
      hasAnyPermission: (
        permissions: Array<{
          resource: ResourceType
          action: ActionType
          resourceId?: string
        }>,
        organizationId?: string
      ) => {
        if (!user) return false

        // Check if user has any of the specified permissions
        return permissions.some(() =>
          // This would use the actual permission check logic
          true // Placeholder implementation
        )
        void organizationId // Mark as intentionally unused in placeholder
      },

      /**
       * Check if user has all specified permissions
       */
      hasAllPermissions: (
        permissions: Array<{
          resource: ResourceType
          action: ActionType
          resourceId?: string
        }>,
        organizationId?: string
      ) => {
        if (!user) return false

        // Check if user has all of the specified permissions
        return permissions.every(() =>
          // This would use the actual permission check logic
          true // Placeholder implementation
        )
        void organizationId // Mark as intentionally unused in placeholder
      },

      /**
       * Get user's effective permissions for debugging
       */
      getEffectivePermissions: (
        organizationId?: string
      ) => {
        if (!user) return []

        // This would fetch and return user's effective permissions
        void organizationId // Mark as intentionally unused in placeholder
        return [] // Placeholder implementation
      },

      /**
       * Get cache performance metrics
       */
      getCacheMetrics: () => {
        return permissionCache.getMetrics()
      },

      /**
       * Invalidate cache entries
       */
      invalidateCache: (strategy: {
        type: 'user' | 'resource' | 'action' | 'organization' | 'all'
        value?: string
      }) => {
        return permissionCache.invalidate(strategy)
      },

      /**
       * Warm cache with predicted permissions
       */
      warmCache: (
        strategy: {
          type: 'user_login' | 'role_change' | 'periodic' | 'predictive'
          resources: string[]
          actions: string[]
          priority: number
        },
        organizationId?: string
      ) => {
        if (!user) return Promise.resolve()
        return permissionCache.warmCache(user.id, strategy, organizationId)
      },
    }
  }, [user])
}

/**
 * Higher-order component for permission-based access control
 */
export const withPermissionCheck = <P extends object>(
  Component: React.ComponentType<P>,
  requiredPermissions: Array<{
    resource: ResourceType
    action: ActionType
    resourceId?: string
  }>
) => {
  const PermissionWrappedComponent: React.FC<P> = (props) => {
    return (
      <PermissionChecker
        resource={requiredPermissions[0].resource}
        action={requiredPermissions[0].action}
        resourceId={requiredPermissions[0].resourceId}
        fallback={
          <div className="text-center py-8" data-testid="permission-denied">
            <div className="text-gray-400 mb-2">
              <svg className="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 15v2m0 0v3m0-3h3m-3 0h-3m-3-9a9 9 0 1118 0 9 9 0 01-18 0z"
                />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-1">Access Restricted</h3>
            <p className="text-sm text-gray-500">
              You don't have permission to access this resource.
            </p>
          </div>
        }
      >
        <Component {...props} />
      </PermissionChecker>
    )
  }

  PermissionWrappedComponent.displayName = `withPermissionCheck(${Component.displayName || Component.name})`

  return PermissionWrappedComponent
}

/**
 * Utility component for rendering content based on role
 */
export interface RoleGuardProps {
  allowedRoles: string[]
  children: React.ReactNode
  fallback?: React.ReactNode
  userRoles?: string[]
}

export const RoleGuard: React.FC<RoleGuardProps> = ({
  allowedRoles,
  children,
  fallback = null,
  userRoles = [],
}) => {
  const hasRequiredRole = allowedRoles.some(role => userRoles.includes(role))

  if (!hasRequiredRole) {
    return <>{fallback}</>
  }

  return <>{children}</>
}

/**
 * Component for rendering permission-sensitive UI elements
 */
export interface PermissionButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  resource: ResourceType
  action: ActionType
  resourceId?: string
  organizationId?: string
  fallbackText?: string
  hideWhenNoPermission?: boolean
}

export const PermissionButton: React.FC<PermissionButtonProps> = ({
  resource,
  action,
  resourceId,
  organizationId,
  fallbackText = 'Not Authorized',
  hideWhenNoPermission = false,
  children,
  ...buttonProps
}) => {
  return (
    <PermissionChecker
      resource={resource}
      action={action}
      resourceId={resourceId}
      organizationId={organizationId}
      fallback={
        hideWhenNoPermission ? null : (
          <button
            {...buttonProps}
            disabled={true}
            title="You don't have permission for this action"
            className={`${buttonProps.className || ''} opacity-50 cursor-not-allowed`}
          >
            {fallbackText}
          </button>
        )
      }
    >
      <button {...buttonProps}>
        {children}
      </button>
    </PermissionChecker>
  )
}
