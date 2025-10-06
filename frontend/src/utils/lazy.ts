import { lazy, ComponentType, LazyExoticComponent } from 'react'

/**
 * Enhanced lazy loading utilities with error handling and preloading.
 */

interface LazyLoadOptions {
  /**
   * Fallback component while loading
   */
  fallback?: ComponentType

  /**
   * Whether to preload the component on hover/focus
   */
  preload?: boolean

  /**
   * Retry attempts on load failure
   */
  retryAttempts?: number

  /**
   * Delay before retry (in ms)
   */
  retryDelay?: number
}

/**
 * Creates a lazy-loaded component with enhanced features.
 */
export const createLazyComponent = <T extends ComponentType<any>>(
  factory: () => Promise<{ default: T }>,
  options: LazyLoadOptions = {}
): LazyExoticComponent<T> => {
  const { retryAttempts = 3, retryDelay = 1000 } = options

  const enhancedFactory = async (): Promise<{ default: T }> => {
    let lastError: Error

    for (let attempt = 1; attempt <= retryAttempts; attempt++) {
      try {
        return await factory()
      } catch (error) {
        lastError = error as Error

        if (attempt < retryAttempts) {
          console.warn(`Failed to load component (attempt ${attempt}/${retryAttempts}):`, error)

          // Wait before retrying
          await new Promise(resolve => setTimeout(resolve, retryDelay * attempt))
        }
      }
    }

    // If all attempts failed, throw the last error
    throw lastError!
  }

  return lazy(enhancedFactory)
}

/**
 * Preloads a lazy component.
 */
export const preloadComponent = (
  componentFactory: () => Promise<{ default: ComponentType<any> }>
): Promise<void> => {
  return componentFactory()
    .then(() => {
      console.debug('Component preloaded successfully')
    })
    .catch(error => {
      console.warn('Failed to preload component:', error)
    })
}

// Lazy loaded page components with preloading
export const LazyDashboardPage = createLazyComponent(
  () => import('../pages/dashboard/DashboardPage'),
  { preload: true }
)

export const LazyOrganizationsPage = createLazyComponent(
  () => import('../pages/organizations/OrganizationsPage')
)

export const LazyOrganizationPage = createLazyComponent(
  () => import('../pages/organizations/OrganizationPage')
)

export const LazySubscriptionPage = createLazyComponent(
  () => import('../pages/subscription/SubscriptionPage')
)

export const LazySettingsPage = createLazyComponent(
  () => import('../pages/settings/SettingsPage')
)

// Heavy UI components that should be lazy loaded
export const LazyPerformanceDashboard = createLazyComponent(
  () => import('../components/performance/PerformanceDashboard'),
  { preload: false } // Only load when needed
)

export const LazyChartsBundle = createLazyComponent(
  () => import('../components/charts/ChartsBundle'),
  { preload: false }
)

export const LazyStripeElements = createLazyComponent(
  () => import('../components/payments/StripeElements'),
  { preload: false }
)

// Preload critical components on app start
export const preloadCriticalComponents = (): void => {
  // Preload dashboard since it's the most common landing page
  preloadComponent(() => import('../pages/dashboard/DashboardPage'))

  // Preload common UI components
  preloadComponent(() => import('../components/ui/LoadingStates'))
  preloadComponent(() => import('../components/ui/StatsCard'))
}

// Preload components based on user interaction patterns
export const preloadForRoute = (route: string): void => {
  switch (route) {
    case '/organizations':
      preloadComponent(() => import('../pages/organizations/OrganizationsPage'))
      preloadComponent(() => import('../pages/organizations/OrganizationPage'))
      break

    case '/subscription':
      preloadComponent(() => import('../pages/subscription/SubscriptionPage'))
      preloadComponent(() => import('../components/payments/StripeElements'))
      break

    case '/settings':
      preloadComponent(() => import('../pages/settings/SettingsPage'))
      break

    default:
      break
  }
}

/**
 * Hook to preload components on hover/focus for better UX.
 */
export const usePreloadOnHover = () => {
  const preloadOnHover = (componentFactory: () => Promise<any>) => {
    return {
      onMouseEnter: () => preloadComponent(componentFactory),
      onFocus: () => preloadComponent(componentFactory),
    }
  }

  return { preloadOnHover }
}