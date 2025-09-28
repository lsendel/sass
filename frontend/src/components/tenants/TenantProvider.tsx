/**
 * Tenant Provider Component
 *
 * Provides tenant context throughout the React application with:
 * - Automatic tenant detection and resolution
 * - Tenant branding application and customization
 * - Cross-tenant routing and navigation utilities
 * - Tenant-aware component rendering and access control
 * - Real-time tenant settings and configuration management
 */

import React, { createContext, useContext, useEffect, ReactNode } from 'react'

import { useTenantContext } from '@/hooks/useTenantContext'
import type {
  Tenant,
  TenantContext,
  TenantSettings,
} from '@/types/multitenancy'

// Create tenant context
interface TenantContextValue {
  tenant: Tenant | null
  tenantSettings: TenantSettings | undefined
  tenantContext: TenantContext | null
  isLoading: boolean
  isError: boolean
  error: any
  resolutionSource: 'domain' | 'subdomain' | 'path' | 'user' | 'none'
  isResolved: boolean

  // Utilities
  routing: {
    generateTenantUrl: (path: string, tenantSlug?: string) => string
    navigateToTenant: (tenantSlug: string, path?: string) => void
    isTenantRoute: () => boolean
    getCurrentTenantRoute: () => any
  }
  branding: {
    applyBranding: () => void
    getLogoUrl: () => string | undefined
    getDisplayName: () => string
    shouldHideProviderBranding: () => boolean
  }
  quotas: {
    isQuotaExceeded: (quotaType: any) => boolean
    getQuotaUsage: (quotaType: any) => number
    isQuotaNearLimit: (quotaType: any) => boolean
    getRemainingQuota: (quotaType: any) => number
  }
  features: {
    isFeatureEnabled: (feature: any) => boolean
    getEnabledFeatures: () => string[]
    hasPremiumFeatures: () => boolean
  }
  refetch: () => void
}

const TenantContextObj = createContext<TenantContextValue | null>(null)

interface TenantProviderProps {
  children: ReactNode
  enableBrandingAutoApply?: boolean
  fallbackComponent?: ReactNode
  loadingComponent?: ReactNode
  errorComponent?: ReactNode
}

/**
 * Tenant Provider component that manages multi-tenant context
 */
export const TenantProvider: React.FC<TenantProviderProps> = ({
  children,
  enableBrandingAutoApply = true,
  fallbackComponent,
  loadingComponent,
  errorComponent,
}) => {
  const tenantHookData = useTenantContext()

  // Auto-apply branding when tenant changes
  useEffect(() => {
    if (enableBrandingAutoApply && tenantHookData.tenant) {
      tenantHookData.branding.applyBranding()
    }
  }, [tenantHookData.tenant, tenantHookData.branding, enableBrandingAutoApply])

  // Handle loading state
  if (tenantHookData.isLoading) {
    if (loadingComponent) {
      return <>{loadingComponent}</>
    }

    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-sm text-gray-600">Loading tenant configuration...</p>
        </div>
      </div>
    )
  }

  // Handle error state
  if (tenantHookData.isError) {
    if (errorComponent) {
      return <>{errorComponent}</>
    }

    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100">
            <svg
              className="h-6 w-6 text-red-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"
              />
            </svg>
          </div>
          <h3 className="mt-4 text-sm font-medium text-gray-900">Tenant Configuration Error</h3>
          <p className="mt-2 text-sm text-gray-500">
            Unable to load tenant configuration. Please try again.
          </p>
          <button
            onClick={() => tenantHookData.refetch()}
            className="mt-4 inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  // Handle unresolved tenant (for routes that don't require tenant context)
  if (!tenantHookData.isResolved) {
    if (fallbackComponent) {
      return <>{fallbackComponent}</>
    }

    // Continue without tenant context for public routes
  }

  return (
    <TenantContextObj.Provider value={tenantHookData}>
      {children}
    </TenantContextObj.Provider>
  )
}

/**
 * Hook to consume tenant context
 */
export const useTenant = (): TenantContextValue => {
  const context = useContext(TenantContextObj)
  if (!context) {
    throw new Error('useTenant must be used within a TenantProvider')
  }
  return context
}

/**
 * Higher-order component for tenant-aware components
 */
export function withTenantContext<P extends object>(
  Component: React.ComponentType<P>
): React.ComponentType<P> {
  const TenantAwareComponent: React.FC<P> = (props) => {
    return (
      <TenantProvider>
        <Component {...props} />
      </TenantProvider>
    )
  }

  TenantAwareComponent.displayName = `withTenantContext(${Component.displayName || Component.name})`

  return TenantAwareComponent
}

/**
 * Tenant Guard component for conditional rendering based on tenant context
 */
interface TenantGuardProps {
  children: ReactNode
  requireTenant?: boolean
  allowedTiers?: string[]
  requiredFeatures?: string[]
  fallback?: ReactNode
}

export const TenantGuard: React.FC<TenantGuardProps> = ({
  children,
  requireTenant = true,
  allowedTiers = [],
  requiredFeatures = [],
  fallback = null,
}) => {
  const { tenant, tenantContext, features } = useTenant()

  // Check if tenant is required
  if (requireTenant && !tenant) {
    return <>{fallback}</>
  }

  // Check allowed tiers
  if (allowedTiers.length > 0 && tenantContext && !allowedTiers.includes(tenantContext.tier)) {
    return <>{fallback}</>
  }

  // Check required features
  if (requiredFeatures.length > 0) {
    const hasAllFeatures = requiredFeatures.every(feature =>
      features.isFeatureEnabled(feature as any)
    )
    if (!hasAllFeatures) {
      return <>{fallback}</>
    }
  }

  return <>{children}</>
}

/**
 * Tenant-aware link component
 */
interface TenantLinkProps extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
  to: string
  tenantSlug?: string
  children: ReactNode
}

export const TenantLink: React.FC<TenantLinkProps> = ({
  to,
  tenantSlug,
  children,
  ...anchorProps
}) => {
  const { routing } = useTenant()

  const tenantAwareUrl = routing.generateTenantUrl(to, tenantSlug)

  return (
    <a href={tenantAwareUrl} {...anchorProps}>
      {children}
    </a>
  )
}

/**
 * Tenant branding component that displays tenant-specific branding
 */
interface TenantBrandingProps {
  showLogo?: boolean
  showName?: boolean
  logoClassName?: string
  nameClassName?: string
  fallbackLogo?: ReactNode
}

export const TenantBranding: React.FC<TenantBrandingProps> = ({
  showLogo = true,
  showName = true,
  logoClassName = 'h-8 w-auto',
  nameClassName = 'text-xl font-semibold text-gray-900',
  fallbackLogo,
}) => {
  const { branding, tenant: _tenant } = useTenant()

  const logoUrl = branding.getLogoUrl()
  const displayName = branding.getDisplayName()

  return (
    <div className="flex items-center space-x-3">
      {showLogo && (
        <div>
          {logoUrl ? (
            <img
              className={logoClassName}
              src={logoUrl}
              alt={`${displayName} logo`}
            />
          ) : fallbackLogo ? (
            fallbackLogo
          ) : (
            <div className={`${logoClassName} bg-gray-200 rounded flex items-center justify-center`}>
              <svg
                className="h-4 w-4 text-gray-400"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
                />
              </svg>
            </div>
          )}
        </div>
      )}
      {showName && (
        <h1 className={nameClassName}>
          {displayName}
        </h1>
      )}
    </div>
  )
}

/**
 * Tenant quota indicator component
 */
interface TenantQuotaIndicatorProps {
  quotaType: any // Would be keyof TenantContext['quotas']
  showLabel?: boolean
  showPercentage?: boolean
  showRemaining?: boolean
  className?: string
  warningThreshold?: number
  criticalThreshold?: number
}

export const TenantQuotaIndicator: React.FC<TenantQuotaIndicatorProps> = ({
  quotaType,
  showLabel = true,
  showPercentage = true,
  showRemaining = false,
  className = '',
  warningThreshold = 80,
  criticalThreshold = 95,
}) => {
  const { quotas } = useTenant()

  const usage = quotas.getQuotaUsage(quotaType)
  const remaining = quotas.getRemainingQuota(quotaType)
  const isExceeded = quotas.isQuotaExceeded(quotaType)

  const getProgressColor = () => {
    if (isExceeded || usage >= criticalThreshold) return 'bg-red-600'
    if (usage >= warningThreshold) return 'bg-yellow-500'
    return 'bg-green-600'
  }

  const getBackgroundColor = () => {
    if (isExceeded || usage >= criticalThreshold) return 'bg-red-100'
    if (usage >= warningThreshold) return 'bg-yellow-100'
    return 'bg-green-100'
  }

  return (
    <div className={`space-y-2 ${className}`}>
      {showLabel && (
        <div className="flex justify-between text-sm">
          <span className="font-medium text-gray-700">
            {quotaType.charAt(0).toUpperCase() + quotaType.slice(1)} Usage
          </span>
          {showPercentage && (
            <span className={`font-medium ${
              isExceeded ? 'text-red-600' :
              usage >= warningThreshold ? 'text-yellow-600' :
              'text-gray-600'
            }`}>
              {usage.toFixed(1)}%
            </span>
          )}
        </div>
      )}

      <div className={`w-full ${getBackgroundColor()} rounded-full h-2`}>
        <div
          className={`h-2 rounded-full transition-all duration-300 ${getProgressColor()}`}
          style={{ width: `${Math.min(usage, 100)}%` }}
        />
      </div>

      {showRemaining && remaining !== Infinity && (
        <p className="text-xs text-gray-500">
          {remaining.toLocaleString()} remaining
        </p>
      )}
    </div>
  )
}

export default TenantProvider