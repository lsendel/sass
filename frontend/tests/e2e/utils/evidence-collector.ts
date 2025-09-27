import path from 'path'
import fs from 'fs/promises'

import { Page, TestInfo } from '@playwright/test'

/**
 * Enhanced Evidence Collector for Playwright Tests
 * Provides comprehensive evidence collection including screenshots,
 * network logs, console logs, performance metrics, and more.
 */
export class EvidenceCollector {
  private page: Page
  private testInfo: TestInfo
  private networkLogs: NetworkLog[] = []
  private consoleLogs: ConsoleLog[] = []
  private performanceMetrics: PerformanceMetric[] = []
  private startTime: number
  private stepCounter = 0

  constructor(page: Page, testInfo: TestInfo) {
    this.page = page
    this.testInfo = testInfo
    this.startTime = Date.now()
    this.setupCollectors()
  }

  /**
   * Set up evidence collectors
   */
  private async setupCollectors() {
    // Network request/response logging
    this.page.on('request', (request) => {
      this.networkLogs.push({
        timestamp: Date.now() - this.startTime,
        type: 'request',
        url: request.url(),
        method: request.method(),
        headers: request.headers(),
        postData: request.postData(),
        resourceType: request.resourceType(),
      })
    })

    this.page.on('response', (response) => {
      this.networkLogs.push({
        timestamp: Date.now() - this.startTime,
        type: 'response',
        url: response.url(),
        status: response.status(),
        statusText: response.statusText(),
        headers: response.headers(),
        fromCache: response.fromCache(),
        fromServiceWorker: response.fromServiceWorker(),
      })
    })

    // Console logging
    this.page.on('console', (msg) => {
      this.consoleLogs.push({
        timestamp: Date.now() - this.startTime,
        type: msg.type(),
        text: msg.text(),
        location: msg.location(),
        args: msg.args().map(arg => arg.toString()),
      })
    })

    // Error logging
    this.page.on('pageerror', (error) => {
      this.consoleLogs.push({
        timestamp: Date.now() - this.startTime,
        type: 'error',
        text: error.message,
        location: { url: this.page.url(), lineNumber: 0, columnNumber: 0 },
        args: [error.stack || ''],
      })
    })
  }

  /**
   * Take screenshot with step annotation
   */
  async takeStepScreenshot(stepName: string, options: {
    fullPage?: boolean
    highlight?: string[]
    mask?: string[]
  } = {}) {
    this.stepCounter++
    const timestamp = Date.now() - this.startTime

    // Highlight elements if specified
    if (options.highlight) {
      for (const selector of options.highlight) {
        try {
          await this.page.locator(selector).highlight()
        } catch (e) {
          console.warn(`Could not highlight element: ${selector}`)
        }
      }
    }

    const screenshotPath = `step-${this.stepCounter.toString().padStart(3, '0')}-${stepName.replace(/[^a-zA-Z0-9]/g, '-')}.png`

    await this.page.screenshot({
      path: path.join(this.testInfo.outputDir, screenshotPath),
      fullPage: options.fullPage || true,
      mask: options.mask ? options.mask.map(s => this.page.locator(s)) : undefined,
    })

    // Attach to test results
    await this.testInfo.attach(`Step ${this.stepCounter}: ${stepName}`, {
      path: path.join(this.testInfo.outputDir, screenshotPath),
      contentType: 'image/png',
    })

    return {
      step: this.stepCounter,
      name: stepName,
      timestamp,
      screenshot: screenshotPath,
    }
  }

  /**
   * Capture current page HTML
   */
  async capturePageHTML(stepName: string) {
    const timestamp = Date.now() - this.startTime
    const htmlPath = `step-${this.stepCounter}-${stepName.replace(/[^a-zA-Z0-9]/g, '-')}-page.html`

    const html = await this.page.content()
    await fs.writeFile(path.join(this.testInfo.outputDir, htmlPath), html)

    await this.testInfo.attach(`Page HTML: ${stepName}`, {
      path: path.join(this.testInfo.outputDir, htmlPath),
      contentType: 'text/html',
    })

    return {
      name: stepName,
      timestamp,
      html: htmlPath,
    }
  }

  /**
   * Collect performance metrics
   */
  async collectPerformanceMetrics(stepName: string) {
    const timestamp = Date.now() - this.startTime

    const metrics = await this.page.evaluate(() => {
      const perfEntries = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming
      const paintEntries = performance.getEntriesByType('paint')
      const resourceEntries = performance.getEntriesByType('resource')

      return {
        navigation: {
          loadComplete: perfEntries.loadEventEnd - perfEntries.loadEventStart,
          domContentLoaded: perfEntries.domContentLoadedEventEnd - perfEntries.domContentLoadedEventStart,
          firstPaint: paintEntries.find(entry => entry.name === 'first-paint')?.startTime || 0,
          firstContentfulPaint: paintEntries.find(entry => entry.name === 'first-contentful-paint')?.startTime || 0,
        },
        resources: resourceEntries.length,
        memory: (performance as any).memory ? {
          used: (performance as any).memory.usedJSHeapSize,
          total: (performance as any).memory.totalJSHeapSize,
          limit: (performance as any).memory.jsHeapSizeLimit,
        } : null,
      }
    })

    this.performanceMetrics.push({
      timestamp,
      step: stepName,
      metrics,
    })

    return metrics
  }

  /**
   * Collect accessibility snapshot
   */
  async collectAccessibilitySnapshot(stepName: string) {
    const timestamp = Date.now() - this.startTime

    const snapshot = await this.page.accessibility.snapshot()
    const snapshotPath = `step-${this.stepCounter}-${stepName.replace(/[^a-zA-Z0-9]/g, '-')}-a11y.json`

    await fs.writeFile(
      path.join(this.testInfo.outputDir, snapshotPath),
      JSON.stringify(snapshot, null, 2)
    )

    await this.testInfo.attach(`Accessibility: ${stepName}`, {
      path: path.join(this.testInfo.outputDir, snapshotPath),
      contentType: 'application/json',
    })

    return {
      name: stepName,
      timestamp,
      snapshot: snapshotPath,
    }
  }

  /**
   * Capture element screenshots
   */
  async captureElementScreenshot(selector: string, stepName: string) {
    const element = this.page.locator(selector)
    if (await element.count() === 0) {
      console.warn(`Element not found for screenshot: ${selector}`)
      return null
    }

    const screenshotPath = `element-${stepName.replace(/[^a-zA-Z0-9]/g, '-')}.png`

    await element.screenshot({
      path: path.join(this.testInfo.outputDir, screenshotPath),
    })

    await this.testInfo.attach(`Element: ${stepName}`, {
      path: path.join(this.testInfo.outputDir, screenshotPath),
      contentType: 'image/png',
    })

    return screenshotPath
  }

  /**
   * Wait for and capture network activity
   */
  async waitForNetworkIdle(timeout = 5000) {
    const startTime = Date.now()
    let lastRequestTime = Date.now()

    const requestListener = () => {
      lastRequestTime = Date.now()
    }

    this.page.on('request', requestListener)

    try {
      while (Date.now() - lastRequestTime < 2000 && Date.now() - startTime < timeout) {
        await this.page.waitForTimeout(100)
      }
    } finally {
      this.page.off('request', requestListener)
    }

    return Date.now() - startTime
  }

  /**
   * Generate comprehensive evidence report
   */
  async generateEvidenceReport() {
    const endTime = Date.now()
    const totalDuration = endTime - this.startTime

    const report: EvidenceReport = {
      testInfo: {
        title: this.testInfo.title,
        file: this.testInfo.file,
        duration: totalDuration,
        startTime: new Date(this.startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        status: this.testInfo.status,
      },
      networkLogs: this.networkLogs,
      consoleLogs: this.consoleLogs,
      performanceMetrics: this.performanceMetrics,
      summary: {
        totalNetworkRequests: this.networkLogs.filter(log => log.type === 'request').length,
        failedRequests: this.networkLogs.filter(log => log.type === 'response' && log.status >= 400).length,
        consoleErrors: this.consoleLogs.filter(log => log.type === 'error').length,
        consoleWarnings: this.consoleLogs.filter(log => log.type === 'warning').length,
        averageResponseTime: this.calculateAverageResponseTime(),
      },
    }

    const reportPath = 'evidence-report.json'
    await fs.writeFile(
      path.join(this.testInfo.outputDir, reportPath),
      JSON.stringify(report, null, 2)
    )

    await this.testInfo.attach('Evidence Report', {
      path: path.join(this.testInfo.outputDir, reportPath),
      contentType: 'application/json',
    })

    return report
  }

  private calculateAverageResponseTime(): number {
    const responses = this.networkLogs.filter(log => log.type === 'response')
    if (responses.length === 0) return 0

    const requests = this.networkLogs.filter(log => log.type === 'request')
    const responseTimes: number[] = []

    responses.forEach(response => {
      const matchingRequest = requests.find(req =>
        req.url === response.url && req.timestamp < response.timestamp
      )
      if (matchingRequest) {
        responseTimes.push(response.timestamp - matchingRequest.timestamp)
      }
    })

    return responseTimes.length > 0
      ? responseTimes.reduce((sum, time) => sum + time, 0) / responseTimes.length
      : 0
  }
}

// Types
interface NetworkLog {
  timestamp: number
  type: 'request' | 'response'
  url: string
  method?: string
  status?: number
  statusText?: string
  headers: Record<string, string>
  postData?: string | null
  resourceType?: string
  fromCache?: boolean
  fromServiceWorker?: boolean
}

interface ConsoleLog {
  timestamp: number
  type: string
  text: string
  location: { url: string; lineNumber: number; columnNumber: number }
  args: string[]
}

interface PerformanceMetric {
  timestamp: number
  step: string
  metrics: any
}

interface EvidenceReport {
  testInfo: {
    title: string
    file: string
    duration: number
    startTime: string
    endTime: string
    status?: string
  }
  networkLogs: NetworkLog[]
  consoleLogs: ConsoleLog[]
  performanceMetrics: PerformanceMetric[]
  summary: {
    totalNetworkRequests: number
    failedRequests: number
    consoleErrors: number
    consoleWarnings: number
    averageResponseTime: number
  }
}

/**
 * Enhanced test fixture with evidence collection
 */
export async function withEvidenceCollection(page: Page, testInfo: TestInfo) {
  const collector = new EvidenceCollector(page, testInfo)

  // Add evidence collection methods to page
  ;(page as any).evidence = collector

  return collector
}