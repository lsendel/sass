import { lazy } from 'react'

// Performance monitoring and optimization utilities
export class PerformanceMonitor {
  private static instance: PerformanceMonitor
  private metrics = new Map<string, number>()
  private observers = new Map<string, PerformanceObserver>()

  public static getInstance(): PerformanceMonitor {
    if (!PerformanceMonitor.instance) {
      PerformanceMonitor.instance = new PerformanceMonitor()
    }
    return PerformanceMonitor.instance
  }

  public init(): void {
    this.setupCoreWebVitals()
    this.setupResourceTiming()
    this.setupNavigationTiming()
    this.setupUserTiming()
  }

  private setupCoreWebVitals(): void {
    // Largest Contentful Paint
    const lcpObserver = new PerformanceObserver(list => {
      const entries = list.getEntries()
      const lastEntry = entries[
        entries.length - 1
      ] as PerformanceNavigationTiming
      this.metrics.set('LCP', lastEntry.startTime)
      this.reportMetric('LCP', lastEntry.startTime)
    })
    lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] })
    this.observers.set('LCP', lcpObserver)

    // First Input Delay
    const fidObserver = new PerformanceObserver(list => {
      const entries = list.getEntries()
      entries.forEach((entry: any) => {
        const delay = entry.processingStart - entry.startTime
        this.metrics.set('FID', delay)
        this.reportMetric('FID', delay)
      })
    })
    fidObserver.observe({ entryTypes: ['first-input'] })
    this.observers.set('FID', fidObserver)

    // Cumulative Layout Shift
    let clsValue = 0
    const clsObserver = new PerformanceObserver(list => {
      const entries = list.getEntries()
      entries.forEach((entry: any) => {
        if (!entry.hadRecentInput) {
          clsValue += entry.value
        }
      })
      this.metrics.set('CLS', clsValue)
      this.reportMetric('CLS', clsValue)
    })
    clsObserver.observe({ entryTypes: ['layout-shift'] })
    this.observers.set('CLS', clsObserver)
  }

  private setupResourceTiming(): void {
    const resourceObserver = new PerformanceObserver(list => {
      const entries = list.getEntries()
      entries.forEach(entry => {
        if (entry.entryType === 'resource') {
          const resourceEntry = entry as PerformanceResourceTiming
          const loadTime =
            resourceEntry.responseEnd - resourceEntry.requestStart

          // Track slow resources
          if (loadTime > 1000) {
            console.warn(
              `Slow resource detected: ${resourceEntry.name} took ${loadTime}ms`
            )
            this.reportSlowResource(resourceEntry.name, loadTime)
          }
        }
      })
    })
    resourceObserver.observe({ entryTypes: ['resource'] })
    this.observers.set('resource', resourceObserver)
  }

  private setupNavigationTiming(): void {
    const navigationObserver = new PerformanceObserver(list => {
      const entries = list.getEntries()
      entries.forEach(entry => {
        if (entry.entryType === 'navigation') {
          const navEntry = entry as PerformanceNavigationTiming

          const metrics = {
            TTFB: navEntry.responseStart - navEntry.requestStart,
            DOMContentLoaded:
              navEntry.domContentLoadedEventEnd -
              navEntry.domContentLoadedEventStart,
            LoadComplete: navEntry.loadEventEnd - navEntry.loadEventStart,
            TotalPageLoad: navEntry.loadEventEnd - navEntry.fetchStart,
          }

          Object.entries(metrics).forEach(([key, value]) => {
            this.metrics.set(key, value)
            this.reportMetric(key, value)
          })
        }
      })
    })
    navigationObserver.observe({ entryTypes: ['navigation'] })
    this.observers.set('navigation', navigationObserver)
  }

  private setupUserTiming(): void {
    const userTimingObserver = new PerformanceObserver(list => {
      const entries = list.getEntries()
      entries.forEach(entry => {
        if (entry.entryType === 'measure') {
          this.metrics.set(entry.name, entry.duration)
          this.reportMetric(entry.name, entry.duration)
        }
      })
    })
    userTimingObserver.observe({ entryTypes: ['measure'] })
    this.observers.set('userTiming', userTimingObserver)
  }

  private reportMetric(name: string, value: number): void {
    // Report to analytics
    if (typeof window !== 'undefined' && (window as any).gtag) {
      ;(window as any).gtag('event', 'performance_metric', {
        metric_name: name,
        value: Math.round(value),
        custom_map: { metric_value: value },
      })
    }

    // Log warnings for poor performance
    const thresholds = {
      LCP: 2500, // Good: < 2.5s
      FID: 100, // Good: < 100ms
      CLS: 0.1, // Good: < 0.1
      TTFB: 600, // Good: < 600ms
      TotalPageLoad: 3000, // Warning: > 3s
    }

    const threshold = thresholds[name as keyof typeof thresholds]
    if (threshold && value > threshold) {
      console.warn(
        `Poor ${name} performance: ${value}ms (threshold: ${threshold}ms)`
      )
    }
  }

  private reportSlowResource(url: string, loadTime: number): void {
    if (typeof window !== 'undefined' && (window as any).gtag) {
      ;(window as any).gtag('event', 'slow_resource', {
        resource_url: url,
        load_time: Math.round(loadTime),
      })
    }
  }

  public measureFunction<T>(name: string, fn: () => T): T {
    performance.mark(`${name}-start`)
    const result = fn()
    performance.mark(`${name}-end`)
    performance.measure(name, `${name}-start`, `${name}-end`)
    return result
  }

  public async measureAsyncFunction<T>(
    name: string,
    fn: () => Promise<T>
  ): Promise<T> {
    performance.mark(`${name}-start`)
    const result = await fn()
    performance.mark(`${name}-end`)
    performance.measure(name, `${name}-start`, `${name}-end`)
    return result
  }

  public getMetrics(): Record<string, number> {
    return Object.fromEntries(this.metrics)
  }

  public cleanup(): void {
    this.observers.forEach(observer => observer.disconnect())
    this.observers.clear()
    this.metrics.clear()
  }
}

// Lazy loading utilities
export const LazyDashboard = lazy(() =>
  import('../pages/dashboard/DashboardPage').then(module => ({
    default: module.default,
  }))
)

export const LazyOrganizations = lazy(() =>
  import('../pages/organizations/OrganizationsPage').then(module => ({
    default: module.default,
  }))
)

export const LazySubscription = lazy(() =>
  import('../pages/subscription/SubscriptionPage').then(module => ({
    default: module.default,
  }))
)

export const LazySettings = lazy(() =>
  import('../pages/settings/SettingsPage').then(module => ({
    default: module.default,
  }))
)

// Resource hints manager
export class ResourceHintManager {
  private static preloadedResources = new Set<string>()
  private static prefetchedResources = new Set<string>()

  public static preconnect(origin: string): void {
    if (typeof document === 'undefined') return

    const link = document.createElement('link')
    link.rel = 'preconnect'
    link.href = origin
    link.crossOrigin = 'anonymous'
    document.head.appendChild(link)
  }

  public static preload(url: string, as: string, crossorigin?: string): void {
    if (typeof document === 'undefined' || this.preloadedResources.has(url))
      return

    const link = document.createElement('link')
    link.rel = 'preload'
    link.href = url
    link.as = as
    if (crossorigin) link.crossOrigin = crossorigin

    document.head.appendChild(link)
    this.preloadedResources.add(url)
  }

  public static prefetch(url: string): void {
    if (typeof document === 'undefined' || this.prefetchedResources.has(url))
      return

    const link = document.createElement('link')
    link.rel = 'prefetch'
    link.href = url

    document.head.appendChild(link)
    this.prefetchedResources.add(url)
  }

  public static preloadRoute(route: string): void {
    // Preload critical resources for a route
    const routeResources = {
      '/dashboard': [
        '/api/v1/organizations',
        '/api/v1/subscriptions/statistics',
      ],
      '/organizations': ['/api/v1/organizations'],
      '/subscription': ['/api/v1/subscriptions', '/api/v1/subscriptions/plans'],
    }

    const resources = routeResources[route as keyof typeof routeResources] || []
    resources.forEach(resource => this.prefetch(resource))
  }
}

// Image optimization component
export interface OptimizedImageProps {
  src: string
  alt: string
  width?: number
  height?: number
  priority?: boolean
  sizes?: string
  quality?: number
  className?: string
}

export const createOptimizedImageSrc = (
  src: string,
  width?: number,
  quality = 75
): string => {
  if (!src.startsWith('http') || src.includes('?')) return src

  const params = new URLSearchParams()
  if (width) params.set('w', width.toString())
  params.set('q', quality.toString())
  params.set('format', 'webp')

  return `${src}?${params.toString()}`
}

// Bundle analyzer utility
export const analyzeBundleSize = (): Promise<void> => {
  return new Promise(resolve => {
    if (typeof window === 'undefined') {
      resolve()
      return
    }

    // Measure bundle sizes
    const resources = performance.getEntriesByType(
      'resource'
    ) as PerformanceResourceTiming[]
    const jsResources = resources.filter(r => r.name.endsWith('.js'))
    const cssResources = resources.filter(r => r.name.endsWith('.css'))

    const totalJSSize = jsResources.reduce((total, resource) => {
      return total + (resource.transferSize || 0)
    }, 0)

    const totalCSSSize = cssResources.reduce((total, resource) => {
      return total + (resource.transferSize || 0)
    }, 0)

    console.group('Bundle Analysis')
    console.log(`Total JS Size: ${(totalJSSize / 1024).toFixed(2)} KB`)
    console.log(`Total CSS Size: ${(totalCSSSize / 1024).toFixed(2)} KB`)
    console.log(`JS Resources: ${jsResources.length}`)
    console.log(`CSS Resources: ${cssResources.length}`)

    // Warn about large bundles
    if (totalJSSize > 500 * 1024) {
      // 500KB threshold
      console.warn('Large JS bundle detected. Consider code splitting.')
    }

    console.groupEnd()
    resolve()
  })
}

// Initialization function
export const initializePerformanceOptimizations = (): void => {
  if (typeof window === 'undefined') return

  // Initialize performance monitoring
  const monitor = PerformanceMonitor.getInstance()
  monitor.init()

  // Preconnect to external domains
  ResourceHintManager.preconnect('https://api.stripe.com')
  ResourceHintManager.preconnect('https://fonts.googleapis.com')
  ResourceHintManager.preconnect('https://fonts.gstatic.com')

  // Analyze bundle on load
  window.addEventListener('load', () => {
    setTimeout(analyzeBundleSize, 1000)
  })

  // Cleanup on unload
  window.addEventListener('beforeunload', () => {
    monitor.cleanup()
  })
}

// Export singleton instance
export const performanceMonitor = PerformanceMonitor.getInstance()
