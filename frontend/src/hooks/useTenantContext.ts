/**
 * Tenant Context Hook
 *
 * Provides tenant-aware context and utilities for multi-tenant applications:
 * - Current tenant detection from routing
 * - Tenant-scoped API calls and data access
 * - Domain/subdomain tenant resolution
 * - Cross-tenant operations for admin users
 * - Tenant branding and customization
 */

import { useMemo, useEffect, useState } from 'react'
import { useLocation, useParams } from 'react-router-dom'

import { useAppSelector } from '@/store/hooks'
import {
  useGetTenantQuery,
  useGetTenantBySlugQuery,
  useGetTenantSettingsQuery,
  // createTenantContextSelector,
} from '@/store/api/tenantApi'
import { selectCurrentUser } from '@/store/slices/authSlice'
import type {
  TenantId,
  TenantSlug,
  TenantContext,
  // Tenant,
  // TenantSettings,
  TenantRoute,
} from '@/types/multitenancy'
import { createTenantId, createTenantSlug } from '@/types/multitenancy'

/**
 * Hook for managing tenant context in multi-tenant applications
 */
export const useTenantContext = () => {
  const location = useLocation()
  const params = useParams()
  const currentUser = useAppSelector(selectCurrentUser)
  const [resolvedTenant, setResolvedTenant] = useState<{
    tenantId?: TenantId
    tenantSlug?: TenantSlug
    source: 'domain' | 'subdomain' | 'path' | 'user' | 'none'
  }>({ source: 'none' })

  // Resolve tenant from various sources
  useEffect(() => {
    const resolveTenantFromContext = () => {
      // 1. Check custom domain
      const hostname = window.location.hostname
      const isCustomDomain = !hostname.includes('.example.com') && hostname !== 'localhost'

      if (isCustomDomain) {
        // Custom domain - tenant identified by domain
        setResolvedTenant({
          source: 'domain',
          // Domain resolution would be handled by backend
        })
        return
      }

      // 2. Check subdomain
      const subdomainMatch = /^([^.]+)\.example\.com$/.exec(hostname)
      if (subdomainMatch) {
        const subdomain = subdomainMatch[1]
        if (subdomain !== 'www' && subdomain !== 'app') {
          setResolvedTenant({
            tenantSlug: createTenantSlug(subdomain),
            source: 'subdomain',
          })
          return
        }
      }

      // 3. Check URL path parameters
      if (params.tenantSlug) {
        setResolvedTenant({
          tenantSlug: createTenantSlug(params.tenantSlug),
          source: 'path',
        })
        return
      }

      if (params.tenantId) {
        setResolvedTenant({
          tenantId: createTenantId(params.tenantId),
          source: 'path',
        })
        return
      }

      // 4. Use current user's default tenant
      // if (currentUser?.defaultTenantId) {
      //   setResolvedTenant({
      //     tenantId: createTenantId(currentUser.defaultTenantId),
      //     source: 'user',
      //   })
      //   return
      // }

      // 5. No tenant context
      setResolvedTenant({ source: 'none' })
    }

    resolveTenantFromContext()
  }, [location, params, currentUser])

  // Fetch tenant data based on resolution
  const tenantByIdQuery = useGetTenantQuery(
    { tenantId: resolvedTenant.tenantId! },
    { skip: !resolvedTenant.tenantId }
  )

  const tenantBySlugQuery = useGetTenantBySlugQuery(
    { slug: resolvedTenant.tenantSlug! },
    { skip: !resolvedTenant.tenantSlug }
  )

  // Determine active tenant
  const activeTenant = useMemo(() => {
    if (tenantByIdQuery.data) return tenantByIdQuery.data
    if (tenantBySlugQuery.data) return tenantBySlugQuery.data
    return null
  }, [tenantByIdQuery.data, tenantBySlugQuery.data])

  // Fetch tenant settings if we have an active tenant
  const tenantSettingsQuery = useGetTenantSettingsQuery(
    { tenantId: activeTenant?.id! },
    { skip: !activeTenant?.id }
  )

  // Build tenant context
  const tenantContext = useMemo((): TenantContext | null => {
    if (!activeTenant) return null

    return {
      tenantId: activeTenant.id,
      tenantSlug: activeTenant.slug,
      tier: activeTenant.tier,
      features: activeTenant.features,
      quotas: activeTenant.quotas,
      userId: currentUser?.id || '',
      organizationId: currentUser?.organizationId || '',
    }
  }, [activeTenant, currentUser])

  // Tenant-aware routing utilities
  const routing = useMemo(() => ({
    /**
     * Generate tenant-aware URL for a path
     */
    generateTenantUrl: (path: string, tenantSlug?: string): string => {
      const targetSlug = tenantSlug || activeTenant?.slug
      if (!targetSlug) return path

      // Use subdomain routing if available
      if (resolvedTenant.source === 'subdomain' || resolvedTenant.source === 'domain') {
        return path
      }

      // Use path-based routing
      return `/t/${targetSlug}${path}`
    },

    /**
     * Navigate to a tenant-specific route
     */
    navigateToTenant: (tenantSlug: string, path = '/') => {
      const url = routing.generateTenantUrl(path, tenantSlug)
      window.location.href = url
    },

    /**
     * Check if current route is tenant-scoped
     */
    isTenantRoute: (): boolean => {
      return resolvedTenant.source !== 'none'
    },

    /**
     * Get current tenant route information
     */
    getCurrentTenantRoute: (): TenantRoute | null => {
      if (!activeTenant) return null

      return {
        path: location.pathname,
        tenantId: activeTenant.id,
        isCustomDomain: resolvedTenant.source === 'domain',
        subdomain: resolvedTenant.source === 'subdomain' ?
          window.location.hostname.split('.')[0] : '',
      }
    },
  }), [activeTenant, location, resolvedTenant])

  // Tenant branding utilities
  const branding = useMemo(() => ({
    /**
     * Apply tenant branding to the application
     */
    applyBranding: () => {
      if (!activeTenant?.branding) return

      const root = document.documentElement
      const { branding } = activeTenant

      // Apply CSS custom properties
      root.style.setProperty('--tenant-primary', branding.primaryColor)
      root.style.setProperty('--tenant-secondary', branding.secondaryColor)

      if (branding.fontFamily) {
        root.style.setProperty('--tenant-font', branding.fontFamily)
      }

      // Apply theme
      root.setAttribute('data-theme', branding.theme)

      // Update favicon
      if (branding.faviconUrl) {
        const favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
        if (favicon) {
          favicon.href = branding.faviconUrl
        }
      }

      // Inject custom CSS
      if (branding.customCss) {
        let styleElement = document.querySelector('#tenant-custom-styles')
        if (!styleElement) {
          styleElement = document.createElement('style')
          styleElement.id = 'tenant-custom-styles'
          document.head.appendChild(styleElement)
        }
        styleElement.textContent = branding.customCss
      }
    },

    /**
     * Get tenant-specific logo URL
     */
    getLogoUrl: (): string | undefined => {
      return activeTenant?.branding?.logoUrl
    },

    /**
     * Get tenant display name
     */
    getDisplayName: (): string => {
      return activeTenant?.displayName || activeTenant?.name || 'Application'
    },

    /**
     * Check if provider branding should be hidden
     */
    shouldHideProviderBranding: (): boolean => {
      return activeTenant?.branding?.hideProviderBranding || false
    },
  }), [activeTenant])

  // Quota checking utilities
  const quotas = useMemo(() => ({
    /**
     * Check if tenant has reached a specific quota limit
     */
    isQuotaExceeded: (quotaType: keyof TenantContext['quotas']): boolean => {
      if (!tenantContext?.quotas) return false

      const quota = tenantContext.quotas[quotaType]
      if ((quota as any).limit === -1) return false // Unlimited

      return (quota as any).current >= (quota as any).limit
    },

    /**
     * Get quota usage percentage
     */
    getQuotaUsage: (quotaType: keyof TenantContext['quotas']): number => {
      if (!tenantContext?.quotas) return 0

      const quota = tenantContext.quotas[quotaType]
      if ((quota as any).limit === -1) return 0 // Unlimited

      return Math.min(((quota as any).current / (quota as any).limit) * 100, 100)
    },

    /**
     * Check if quota is approaching limit (80% threshold)
     */
    isQuotaNearLimit: (quotaType: keyof TenantContext['quotas']): boolean => {
      return quotas.getQuotaUsage(quotaType) >= 80
    },

    /**
     * Get remaining quota amount
     */
    getRemainingQuota: (quotaType: keyof TenantContext['quotas']): number => {
      if (!tenantContext?.quotas) return 0

      const quota = tenantContext.quotas[quotaType]
      if ((quota as any).limit === -1) return Infinity // Unlimited

      return Math.max((quota as any).limit - (quota as any).current, 0)
    },
  }), [tenantContext])

  // Feature flag utilities
  const features = useMemo(() => ({
    /**
     * Check if a feature is enabled for the current tenant
     */
    isFeatureEnabled: (feature: keyof TenantContext['features']): boolean => {
      if (!tenantContext?.features) return false
      return tenantContext.features[feature]
    },

    /**
     * Get list of enabled features
     */
    getEnabledFeatures: (): string[] => {
      if (!tenantContext?.features) return []

      return Object.entries(tenantContext.features)
        .filter(([, enabled]) => enabled)
        .map(([feature]) => feature)
    },

    /**
     * Check if tenant has access to premium features
     */
    hasPremiumFeatures: (): boolean => {
      if (!tenantContext) return false
      return ['professional', 'enterprise', 'white_label'].includes(tenantContext.tier)
    },
  }), [tenantContext])

  // Loading states
  const isLoading = tenantByIdQuery.isLoading || tenantBySlugQuery.isLoading
  const isError = tenantByIdQuery.isError || tenantBySlugQuery.isError
  const error = tenantByIdQuery.error || tenantBySlugQuery.error

  return {
    // Core tenant data
    tenant: activeTenant,
    tenantSettings: tenantSettingsQuery.data,
    tenantContext,

    // Resolution info
    resolutionSource: resolvedTenant.source,
    isResolved: resolvedTenant.source !== 'none',

    // Utilities
    routing,
    branding,
    quotas,
    features,

    // Loading states
    isLoading,
    isError,
    error,

    // Refresh function
    refetch: () => {
      tenantByIdQuery.refetch()
      tenantBySlugQuery.refetch()
      tenantSettingsQuery.refetch()
    },
  }
}

/**
 * Hook for multi-tenant admin operations
 * Only available to users with cross-tenant access
 */
export const useMultiTenantAdmin = () => {
  const currentUser = useAppSelector(selectCurrentUser)

  const isMultiTenantAdmin = useMemo(() => {
    // Check if user has cross-tenant admin permissions
    return currentUser?.role ?
      ['system_admin', 'platform_admin', 'tenant_manager'].includes(currentUser.role) : false
  }, [currentUser])

  return {
    isMultiTenantAdmin,

    /**
     * Switch to a different tenant context (admin only)
     */
    switchTenantContext: (tenantSlug: string) => {
      if (!isMultiTenantAdmin) {
        throw new Error('Insufficient permissions for tenant switching')
      }

      // Navigate to tenant context
      window.location.href = `/admin/tenants/${tenantSlug}`
    },

    /**
     * Generate impersonation token for tenant access
     */
    generateImpersonationUrl: (tenantSlug: string, userId?: string) => {
      if (!isMultiTenantAdmin) {
        throw new Error('Insufficient permissions for tenant impersonation')
      }

      const baseUrl = `/admin/impersonate/${tenantSlug}`
      return userId ? `${baseUrl}?userId=${userId}` : baseUrl
    },
  }
}

/**
 * Utility hook for tenant-aware API calls
 */
export const useTenantAwareApi = () => {
  const { tenantContext } = useTenantContext()

  return {
    /**
     * Add tenant context to API headers
     */
    withTenantHeaders: (headers: Record<string, string> = {}) => {
      if (!tenantContext) return headers

      return {
        ...headers,
        'X-Tenant-ID': tenantContext.tenantId,
        'X-Tenant-Slug': tenantContext.tenantSlug,
      }
    },

    /**
     * Add tenant context to query parameters
     */
    withTenantParams: (params: Record<string, any> = {}) => {
      if (!tenantContext) return params

      return {
        ...params,
        tenantId: tenantContext.tenantId,
      }
    },

    tenantContext,
  }
}

export default useTenantContext