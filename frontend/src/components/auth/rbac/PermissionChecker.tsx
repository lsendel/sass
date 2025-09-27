/**
 * Permission Checker Component
 *
 * React component for declarative permission-based rendering:
 * - Conditionally renders content based on user permissions
 * - Supports resource-level and action-level checks
 * - Caches permission results for performance
 * - Provides fallback content for unauthorized access
 * - Integrates with RBAC system
 */

import React, { useMemo } from 'react'

import { useCheckPermissionQuery } from '../../../store/api/rbacApi'
import { useAppSelector } from '../../../store/hooks'
import { selectCurrentUser } from '../../../store/slices/authSlice'
import { logger } from '../../../utils/logger'
import type { ResourceType, ActionType } from '../../../types/rbac'

export interface PermissionCheckerProps {
  /** Resource being accessed */
  resource: ResourceType
  /** Action being performed on the resource */
  action: ActionType
  /** Optional specific resource ID for granular permissions */
  resourceId?: string
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
}

/**
 * Wrapper component that conditionally renders children based on user permissions.
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
}) => {
  const user = useAppSelector(selectCurrentUser)

  // Build permission context
  const permissionContext = useMemo(() => {
    if (!user) return null

    return {
      userId: user.id,
      organizationId,
      resource,
      action,
      resourceId,
      additionalContext: context,
    }
  }, [user, organizationId, resource, action, resourceId, context])

  // Check permission via API
  const {
    data: permissionResult,
    isLoading,
    error,
    isError,
  } = useCheckPermissionQuery(permissionContext!, {
    skip: !permissionContext,
    // Cache permission results for 5 minutes
    pollingInterval: 0,
    refetchOnMountOrArgChange: 300, // 5 minutes
  })

  // Handle loading state
  if (isLoading && showLoading) {
    if (loadingComponent) {
      return <>{loadingComponent}</>
    }
    return (
      <div className="inline-flex items-center text-gray-400" data-testid="permission-loading">
        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-400"></div>
        <span className="ml-2 text-sm">Checking permissions...</span>
      </div>
    )
  }

  // Handle error state - deny access on error for security
  if (isError) {
    if (logDenials) {
      logger.warn('Permission check failed:', {
        resource,
        action,
        resourceId,
        error,
        user: user?.id,
      })
    }
    return <>{fallback}</>
  }

  // Handle no user - deny access
  if (!user || !permissionContext) {
    if (logDenials) {
      logger.debug('Permission denied - no authenticated user:', {
        resource,
        action,
        resourceId,
      })
    }
    return <>{fallback}</>
  }

  // Handle permission result
  const hasPermission = permissionResult?.allowed ?? false

  if (!hasPermission) {
    if (logDenials) {
      logger.info('Permission denied:', {
        resource,
        action,
        resourceId,
        reason: permissionResult?.reason,
        user: user.id,
      })
    }
    return <>{fallback}</>
  }

  // User has permission - render children
  return <>{children}</>
}

export default PermissionChecker

/**
 * Hook for imperative permission checking
 */
export const usePermissionCheck = () => {
  const user = useAppSelector(selectCurrentUser)

  return useMemo(() => {
    return {
      /**
       * Check if current user has permission for a specific action
       */
      hasPermission: (
        resource: ResourceType,
        action: ActionType,
        options?: {
          resourceId?: string
          organizationId?: string
          context?: Record<string, unknown>
        }
      ) => {
        if (!user) return false

        // This would typically use a cached permission check
        // For now, we'll return a simple implementation
        // In a real app, this would integrate with the permission API
        void resource
        void action
        void options
        return true // Placeholder
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
