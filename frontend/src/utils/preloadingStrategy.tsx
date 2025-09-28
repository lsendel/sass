/**
 * Intelligent Preloading Strategy
 *
 * Advanced preloading system that analyzes user behavior and preloads
 * likely next routes and resources based on user patterns and navigation.
 */

import React from 'react'
import { logger } from './logger'
import { performanceMonitor } from './performance'

// Types for preloading strategy
export interface PreloadStrategy {
  route: string
  priority: 'high' | 'medium' | 'low'
  confidence: number // 0-1 probability user will navigate here
  dependencies: string[] // Required chunks/resources
}

export interface UserNavigationPattern {
  route: string
  timestamp: number
  duration: number
  exitRoute?: string
  metadata?: Record<string, any>
}

export interface PreloadResult {
  route: string
  success: boolean
  loadTime: number
  error?: Error
}

// Navigation pattern analyzer
class NavigationPatternAnalyzer {
  private patterns: UserNavigationPattern[] = []
  private readonly maxPatterns = 50 // Keep last 50 navigation events

  public recordNavigation(pattern: UserNavigationPattern): void {
    this.patterns.push(pattern)

    // Keep only recent patterns
    if (this.patterns.length > this.maxPatterns) {
      this.patterns = this.patterns.slice(-this.maxPatterns)
    }

    logger.debug('Navigation pattern recorded:', pattern)
  }

  public predictNextRoutes(currentRoute: string, limit = 3): PreloadStrategy[] {
    const predictions: Map<string, { count: number; avgDuration: number }> = new Map()

    // Analyze patterns where user was on current route
    const relevantPatterns = this.patterns.filter(p => p.route === currentRoute && p.exitRoute)

    if (relevantPatterns.length === 0) {
      // Return default strategies for new routes
      return this.getDefaultStrategies(currentRoute)
    }

    // Count transitions and calculate average time spent
    relevantPatterns.forEach(pattern => {
      if (!pattern.exitRoute) return

      const existing = predictions.get(pattern.exitRoute)
      predictions.set(pattern.exitRoute, {
        count: (existing?.count || 0) + 1,
        avgDuration: existing
          ? (existing.avgDuration + pattern.duration) / 2
          : pattern.duration
      })
    })

    // Convert to strategies with confidence scores
    const strategies: PreloadStrategy[] = []
    const totalTransitions = relevantPatterns.length

    predictions.forEach((data, route) => {
      const confidence = data.count / totalTransitions
      const priority = this.calculatePriority(confidence, data.avgDuration)

      strategies.push({
        route,
        priority,
        confidence,
        dependencies: this.getRouteDependencies(route)
      })
    })

    // Sort by confidence and limit results
    return strategies
      .sort((a, b) => b.confidence - a.confidence)
      .slice(0, limit)
  }

  private calculatePriority(confidence: number, avgDuration: number): 'high' | 'medium' | 'low' {
    // High priority: high confidence (>60%) and user stays long (>5s)
    if (confidence > 0.6 && avgDuration > 5000) return 'high'

    // Medium priority: decent confidence (>30%) or high confidence with short stays
    if (confidence > 0.3 || (confidence > 0.5 && avgDuration > 2000)) return 'medium'

    return 'low'
  }

  private getDefaultStrategies(currentRoute: string): PreloadStrategy[] {
    // Default preloading strategies based on common navigation patterns
    const defaultMap: Record<string, PreloadStrategy[]> = {
      '/': [
        { route: '/dashboard', priority: 'high', confidence: 0.8, dependencies: ['feature-dashboard'] },
        { route: '/organizations', priority: 'medium', confidence: 0.6, dependencies: ['feature-organizations'] },
        { route: '/payments', priority: 'low', confidence: 0.3, dependencies: ['feature-payments'] }
      ],
      '/auth/login': [
        { route: '/dashboard', priority: 'high', confidence: 0.9, dependencies: ['feature-dashboard'] }
      ],
      '/dashboard': [
        { route: '/organizations', priority: 'medium', confidence: 0.5, dependencies: ['feature-organizations'] },
        { route: '/payments', priority: 'medium', confidence: 0.4, dependencies: ['feature-payments'] },
        { route: '/subscription', priority: 'low', confidence: 0.3, dependencies: ['feature-subscriptions'] }
      ],
      '/organizations': [
        { route: '/payments', priority: 'medium', confidence: 0.4, dependencies: ['feature-payments'] },
        { route: '/subscription', priority: 'low', confidence: 0.3, dependencies: ['feature-subscriptions'] }
      ]
    }

    return defaultMap[currentRoute] || []
  }

  private getRouteDependencies(route: string): string[] {
    // Map routes to their required chunks
    const dependencyMap: Record<string, string[]> = {
      '/dashboard': ['feature-dashboard', 'ui-components'],
      '/organizations': ['feature-organizations', 'ui-components'],
      '/organizations/*': ['feature-organizations', 'ui-components', 'vendor-forms'],
      '/payments': ['feature-payments', 'vendor-stripe', 'ui-components'],
      '/subscription': ['feature-subscriptions', 'vendor-stripe', 'ui-components'],
      '/auth/*': ['feature-auth', 'vendor-forms'],
    }

    // Find matching pattern
    for (const [pattern, deps] of Object.entries(dependencyMap)) {
      if (pattern.includes('*') && route.startsWith(pattern.replace('*', ''))) {
        return deps
      }
      if (pattern === route) {
        return deps
      }
    }

    return ['ui-components'] // Default dependencies
  }

  public getAnalytics() {
    const routeFrequency: Record<string, number> = {}
    const avgDurations: Record<string, number> = {}

    this.patterns.forEach(pattern => {
      routeFrequency[pattern.route] = (routeFrequency[pattern.route] || 0) + 1
      avgDurations[pattern.route] = pattern.duration
    })

    return {
      totalPatterns: this.patterns.length,
      routeFrequency,
      avgDurations
    }
  }
}

// Intelligent resource preloader
class IntelligentPreloader {
  private preloadedRoutes: Set<string> = new Set()
  private preloadedChunks: Set<string> = new Set()
  private preloadResults: PreloadResult[] = []
  private analyzer: NavigationPatternAnalyzer

  constructor() {
    this.analyzer = new NavigationPatternAnalyzer()
    this.initializeIntersectionObserver()
  }

  public async preloadRoute(strategy: PreloadStrategy): Promise<PreloadResult> {
    const startTime = performance.now()
    const result: PreloadResult = {
      route: strategy.route,
      success: false,
      loadTime: 0
    }

    try {
      logger.info(`Preloading route ${strategy.route} with ${strategy.priority} priority`)

      // Preload route components
      await this.preloadRouteComponents(strategy.route)

      // Preload required chunks
      await Promise.all(
        strategy.dependencies.map(chunk => this.preloadChunk(chunk))
      )

      result.success = true
      result.loadTime = performance.now() - startTime

      this.preloadedRoutes.add(strategy.route)
      logger.info(`Successfully preloaded route ${strategy.route} in ${result.loadTime.toFixed(2)}ms`)

    } catch (error) {
      result.error = error as Error
      result.loadTime = performance.now() - startTime
      logger.error(`Failed to preload route ${strategy.route}:`, error)
    }

    this.preloadResults.push(result)
    return result
  }

  private async preloadRouteComponents(route: string): Promise<void> {
    // Dynamic imports based on route patterns
    const routeImports: Record<string, () => Promise<any>> = {
      '/dashboard': () => import('../pages/dashboard/DashboardPage'),
      '/organizations': () => import('../pages/organizations/OrganizationsPage'),
      '/payments': () => import('../pages/payments/PaymentsPage'),
      '/subscription': () => import('../pages/subscription/SubscriptionPage'),
    }

    const importFn = routeImports[route]
    if (importFn) {
      await importFn()
    }
  }

  private async preloadChunk(chunkName: string): Promise<void> {
    if (this.preloadedChunks.has(chunkName)) {
      return // Already preloaded
    }

    // Create link element for chunk preloading
    const link = document.createElement('link')
    link.rel = 'preload'
    link.as = 'script'
    link.crossOrigin = 'anonymous'

    // In production, we'd need to map chunk names to actual file names
    // For now, we'll simulate the preload
    await new Promise((resolve) => {
      setTimeout(resolve, 10) // Simulate network delay
    })

    this.preloadedChunks.add(chunkName)
    logger.debug(`Preloaded chunk: ${chunkName}`)
  }

  public recordNavigation(pattern: UserNavigationPattern): void {
    this.analyzer.recordNavigation(pattern)

    // Trigger predictive preloading for next likely routes
    this.triggerPredictivePreloading(pattern.route)
  }

  private async triggerPredictivePreloading(currentRoute: string): Promise<void> {
    const strategies = this.analyzer.predictNextRoutes(currentRoute)

    // Process strategies by priority
    const highPriority = strategies.filter(s => s.priority === 'high')
    const mediumPriority = strategies.filter(s => s.priority === 'medium')
    const lowPriority = strategies.filter(s => s.priority === 'low')

    // Preload high priority immediately
    await Promise.all(
      highPriority.map(strategy => this.preloadRoute(strategy))
    )

    // Preload medium priority after a short delay
    setTimeout(async () => {
      await Promise.all(
        mediumPriority.map(strategy => this.preloadRoute(strategy))
      )
    }, 1000)

    // Preload low priority when browser is idle
    if ('requestIdleCallback' in window) {
      requestIdleCallback(async () => {
        await Promise.all(
          lowPriority.map(strategy => this.preloadRoute(strategy))
        )
      }, { timeout: 5000 })
    }
  }

  private initializeIntersectionObserver(): void {
    // Preload resources when they come into viewport
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const target = entry.target as HTMLElement
          const preloadRoute = target.dataset.preloadRoute

          if (preloadRoute && !this.preloadedRoutes.has(preloadRoute)) {
            const strategy: PreloadStrategy = {
              route: preloadRoute,
              priority: 'medium',
              confidence: 0.7,
              dependencies: this.analyzer['getRouteDependencies'](preloadRoute)
            }

            this.preloadRoute(strategy)
          }
        }
      })
    }, { threshold: 0.1 })

    // Observe navigation links
    setTimeout(() => {
      document.querySelectorAll('a[data-preload-route]').forEach(link => {
        observer.observe(link)
      })
    }, 1000)
  }

  public getPreloadingStats() {
    return {
      preloadedRoutes: Array.from(this.preloadedRoutes),
      preloadedChunks: Array.from(this.preloadedChunks),
      results: this.preloadResults,
      analytics: this.analyzer.getAnalytics()
    }
  }
}

// Global preloader instance
export const intelligentPreloader = new IntelligentPreloader()

// React hook for navigation tracking
export const useNavigationTracking = () => {
  React.useEffect(() => {
    let startTime = performance.now()
    let currentRoute = window.location.pathname

    const handleRouteChange = () => {
      const endTime = performance.now()
      const duration = endTime - startTime
      const exitRoute = window.location.pathname

      // Record navigation pattern
      intelligentPreloader.recordNavigation({
        route: currentRoute,
        timestamp: Date.now(),
        duration,
        exitRoute: exitRoute !== currentRoute ? exitRoute : undefined
      })

      // Update for next navigation
      currentRoute = exitRoute
      startTime = endTime
    }

    // Listen for route changes
    window.addEventListener('popstate', handleRouteChange)

    // Also listen for programmatic navigation (React Router)
    const originalPushState = history.pushState
    const originalReplaceState = history.replaceState

    history.pushState = function(...args) {
      handleRouteChange()
      return originalPushState.apply(this, args)
    }

    history.replaceState = function(...args) {
      handleRouteChange()
      return originalReplaceState.apply(this, args)
    }

    // Cleanup
    return () => {
      window.removeEventListener('popstate', handleRouteChange)
      history.pushState = originalPushState
      history.replaceState = originalReplaceState
    }
  }, [])

  return {
    getPreloadingStats: () => intelligentPreloader.getPreloadingStats(),
    preloadRoute: (route: string) => {
      const strategy: PreloadStrategy = {
        route,
        priority: 'medium',
        confidence: 0.8,
        dependencies: []
      }
      return intelligentPreloader.preloadRoute(strategy)
    }
  }
}

// Component for adding preload hints to navigation links
export interface PreloadLinkProps extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
  to: string
  preloadPriority?: 'high' | 'medium' | 'low'
}

export const PreloadLink: React.FC<PreloadLinkProps> = ({
  to,
  preloadPriority = 'medium',
  children,
  ...props
}) => {
  React.useEffect(() => {
    // Add data attribute for intersection observer
    const links = document.querySelectorAll(`a[href="${to}"]`)
    links.forEach(link => {
      link.setAttribute('data-preload-route', to)
      link.setAttribute('data-preload-priority', preloadPriority)
    })
  }, [to, preloadPriority])

  return (
    <a
      {...props}
      href={to}
      data-preload-route={to}
      data-preload-priority={preloadPriority}
    >
      {children}
    </a>
  )
}

// Auto-initialize navigation tracking
if (typeof window !== 'undefined') {
  // Start performance monitoring for preloading
  performanceMonitor.startUserJourney('preloading-session')

  // Clean up on page unload
  window.addEventListener('beforeunload', () => {
    const stats = intelligentPreloader.getPreloadingStats()
    logger.info('Final preloading statistics:', stats)

    performanceMonitor.completeUserJourney('preloading-session')
  })
}