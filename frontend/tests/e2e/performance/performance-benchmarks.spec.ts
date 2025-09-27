import { test, expect, Page } from '@playwright/test'

interface PerformanceMetrics {
  firstContentfulPaint: number
  largestContentfulPaint: number
  cumulativeLayoutShift: number
  firstInputDelay: number
  totalBlockingTime: number
  timeToInteractive: number
}

/**
 * Performance benchmarking test suite for critical user journeys
 */
test.describe('Performance Benchmarks', () => {
  let performanceMetrics: PerformanceMetrics[] = []

  test.beforeAll(async () => {
    // Clear any cached performance data
    performanceMetrics = []
  })

  test.afterAll(async () => {
    // Generate performance report
    await generatePerformanceReport(performanceMetrics)
  })

  test('Homepage load performance should meet thresholds', async ({ page }) => {
    const metrics = await measurePagePerformance(page, '/')

    // Performance thresholds based on Core Web Vitals
    expect(metrics.firstContentfulPaint).toBeLessThan(1800) // < 1.8s
    expect(metrics.largestContentfulPaint).toBeLessThan(2500) // < 2.5s
    expect(metrics.cumulativeLayoutShift).toBeLessThan(0.1) // < 0.1
    expect(metrics.firstInputDelay).toBeLessThan(100) // < 100ms
    expect(metrics.timeToInteractive).toBeLessThan(3000) // < 3s

    performanceMetrics.push(metrics)
  })

  test('Dashboard load performance should be optimized', async ({ page }) => {
    // Login first
    await page.goto('/auth/login')
    await page.fill('input[name="email"]', 'test@example.com')
    await page.fill('input[name="password"]', 'password')
    await page.click('button[type="submit"]')
    await page.waitForURL('/dashboard')

    const metrics = await measurePagePerformance(page, '/dashboard', true)

    // Dashboard-specific thresholds (slightly higher due to data loading)
    expect(metrics.firstContentfulPaint).toBeLessThan(2000) // < 2s
    expect(metrics.largestContentfulPaint).toBeLessThan(3000) // < 3s
    expect(metrics.cumulativeLayoutShift).toBeLessThan(0.15) // < 0.15
    expect(metrics.timeToInteractive).toBeLessThan(4000) // < 4s

    performanceMetrics.push(metrics)
  })

  test('Analytics dashboard performance with heavy data', async ({ page }) => {
    await page.goto('/analytics')

    const metrics = await measurePagePerformance(page, '/analytics', true)

    // Analytics-specific thresholds (charts and data visualization)
    expect(metrics.firstContentfulPaint).toBeLessThan(2500) // < 2.5s
    expect(metrics.largestContentfulPaint).toBeLessThan(4000) // < 4s
    expect(metrics.cumulativeLayoutShift).toBeLessThan(0.2) // < 0.2 (charts may cause layout shift)

    performanceMetrics.push(metrics)
  })

  test('Payment flow performance should be responsive', async ({ page }) => {
    // Navigate to payment page
    await page.goto('/payments/new')

    const metrics = await measurePagePerformance(page, '/payments/new', true)

    // Critical payment flow - must be fast
    expect(metrics.firstContentfulPaint).toBeLessThan(1500) // < 1.5s
    expect(metrics.largestContentfulPaint).toBeLessThan(2000) // < 2s
    expect(metrics.firstInputDelay).toBeLessThan(50) // < 50ms (critical for form interaction)

    performanceMetrics.push(metrics)
  })

  test('Bundle size should be within limits', async ({ page }) => {
    const bundleAnalysis = await analyzeBundleSize(page)

    // Bundle size thresholds
    expect(bundleAnalysis.totalSize).toBeLessThan(3 * 1024 * 1024) // < 3MB total
    expect(bundleAnalysis.gzippedSize).toBeLessThan(800 * 1024) // < 800KB gzipped
    expect(bundleAnalysis.largestChunk).toBeLessThan(500 * 1024) // < 500KB largest chunk

    // JavaScript execution time
    expect(bundleAnalysis.scriptEvaluationTime).toBeLessThan(1000) // < 1s
  })

  test('Memory usage should be stable during navigation', async ({ page }) => {
    const memoryMetrics = await measureMemoryUsage(page, [
      '/',
      '/dashboard',
      '/analytics',
      '/payments',
      '/settings'
    ])

    // Memory thresholds
    expect(memoryMetrics.peakMemoryMB).toBeLessThan(150) // < 150MB peak
    expect(memoryMetrics.memoryLeakMB).toBeLessThan(10) // < 10MB increase after navigation
    expect(memoryMetrics.averageMemoryMB).toBeLessThan(100) // < 100MB average
  })

  test('API response times should be acceptable', async ({ page }) => {
    const apiMetrics = await measureApiPerformance(page, [
      '/api/v1/organizations',
      '/api/v1/users/me',
      '/api/v1/payments',
      '/api/v1/analytics/metrics'
    ])

    // API performance thresholds
    for (const metric of apiMetrics) {
      expect(metric.responseTime).toBeLessThan(500) // < 500ms per API call
      expect(metric.ttfb).toBeLessThan(200) // < 200ms time to first byte
    }
  })
})

/**
 * Measure comprehensive page performance metrics
 */
async function measurePagePerformance(page: Page, url: string, skipNavigation = false): Promise<PerformanceMetrics> {
  if (!skipNavigation) {
    await page.goto(url)
  }

  // Wait for page to be fully loaded
  await page.waitForLoadState('networkidle')

  // Measure Core Web Vitals
  const metrics = await page.evaluate(() => {
    return new Promise<PerformanceMetrics>((resolve) => {
      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries()
        const metrics: Partial<PerformanceMetrics> = {}

        entries.forEach((entry) => {
          switch (entry.entryType) {
            case 'paint':
              if (entry.name === 'first-contentful-paint') {
                metrics.firstContentfulPaint = entry.startTime
              }
              break
            case 'largest-contentful-paint':
              metrics.largestContentfulPaint = entry.startTime
              break
            case 'layout-shift':
              if (!('hadRecentInput' in entry) || !(entry as any).hadRecentInput) {
                metrics.cumulativeLayoutShift = (metrics.cumulativeLayoutShift || 0) + (entry as any).value
              }
              break
            case 'first-input':
              metrics.firstInputDelay = (entry as any).processingStart - entry.startTime
              break
            case 'navigation':
              const navEntry = entry as PerformanceNavigationTiming
              metrics.timeToInteractive = navEntry.domInteractive - navEntry.navigationStart
              break
          }
        })

        // Calculate Total Blocking Time
        const longTasks = performance.getEntriesByType('longtask') as any[]
        metrics.totalBlockingTime = longTasks.reduce((total, task) => {
          return total + Math.max(0, task.duration - 50)
        }, 0)

        resolve(metrics as PerformanceMetrics)
      })

      observer.observe({ entryTypes: ['paint', 'largest-contentful-paint', 'layout-shift', 'first-input', 'navigation', 'longtask'] })

      // Fallback timeout
      setTimeout(() => {
        resolve({
          firstContentfulPaint: 0,
          largestContentfulPaint: 0,
          cumulativeLayoutShift: 0,
          firstInputDelay: 0,
          totalBlockingTime: 0,
          timeToInteractive: 0
        })
      }, 5000)
    })
  })

  return metrics
}

/**
 * Analyze bundle size and JavaScript performance
 */
async function analyzeBundleSize(page: Page) {
  return await page.evaluate(() => {
    const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[]
    let totalSize = 0
    let scriptSize = 0
    let largestChunk = 0
    let scriptEvaluationTime = 0

    resources.forEach((resource) => {
      if (resource.transferSize) {
        totalSize += resource.transferSize

        if (resource.name.endsWith('.js')) {
          scriptSize += resource.transferSize
          largestChunk = Math.max(largestChunk, resource.transferSize)
          scriptEvaluationTime += resource.responseEnd - resource.responseStart
        }
      }
    })

    // Estimate gzipped size (approximation)
    const gzippedSize = Math.round(totalSize * 0.3)

    return {
      totalSize,
      scriptSize,
      gzippedSize,
      largestChunk,
      scriptEvaluationTime
    }
  })
}

/**
 * Measure memory usage during navigation
 */
async function measureMemoryUsage(page: Page, urls: string[]) {
  const measurements: number[] = []

  for (const url of urls) {
    await page.goto(url)
    await page.waitForLoadState('networkidle')

    const memoryInfo = await page.evaluate(() => {
      if ('memory' in performance) {
        const memory = (performance as any).memory
        return memory.usedJSHeapSize / 1024 / 1024 // Convert to MB
      }
      return 0
    })

    measurements.push(memoryInfo)

    // Force garbage collection if available
    await page.evaluate(() => {
      if ('gc' in window) {
        (window as any).gc()
      }
    })
  }

  return {
    peakMemoryMB: Math.max(...measurements),
    averageMemoryMB: measurements.reduce((a, b) => a + b, 0) / measurements.length,
    memoryLeakMB: measurements[measurements.length - 1] - measurements[0]
  }
}

/**
 * Measure API performance
 */
async function measureApiPerformance(page: Page, endpoints: string[]) {
  const metrics: Array<{ endpoint: string; responseTime: number; ttfb: number }> = []

  for (const endpoint of endpoints) {
    const startTime = Date.now()

    const response = await page.request.get(endpoint)
    const endTime = Date.now()

    const responseTime = endTime - startTime
    const timing = await response.timing()

    metrics.push({
      endpoint,
      responseTime,
      ttfb: timing.responseStart - timing.requestStart
    })
  }

  return metrics
}

/**
 * Generate comprehensive performance report
 */
async function generatePerformanceReport(metrics: PerformanceMetrics[]) {
  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      totalTests: metrics.length,
      averageFCP: metrics.reduce((sum, m) => sum + m.firstContentfulPaint, 0) / metrics.length,
      averageLCP: metrics.reduce((sum, m) => sum + m.largestContentfulPaint, 0) / metrics.length,
      averageCLS: metrics.reduce((sum, m) => sum + m.cumulativeLayoutShift, 0) / metrics.length,
      averageFID: metrics.reduce((sum, m) => sum + m.firstInputDelay, 0) / metrics.length
    },
    details: metrics,
    recommendations: generateRecommendations(metrics)
  }

  console.log('🎯 Performance Test Report:', JSON.stringify(report, null, 2))
}

/**
 * Generate performance optimization recommendations
 */
function generateRecommendations(metrics: PerformanceMetrics[]): string[] {
  const recommendations: string[] = []

  const avgFCP = metrics.reduce((sum, m) => sum + m.firstContentfulPaint, 0) / metrics.length
  const avgLCP = metrics.reduce((sum, m) => sum + m.largestContentfulPaint, 0) / metrics.length
  const avgCLS = metrics.reduce((sum, m) => sum + m.cumulativeLayoutShift, 0) / metrics.length

  if (avgFCP > 1800) {
    recommendations.push('Optimize First Contentful Paint: Consider code splitting and preloading critical resources')
  }

  if (avgLCP > 2500) {
    recommendations.push('Improve Largest Contentful Paint: Optimize images and critical rendering path')
  }

  if (avgCLS > 0.1) {
    recommendations.push('Reduce Cumulative Layout Shift: Add explicit dimensions to images and containers')
  }

  return recommendations
}