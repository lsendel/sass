import fs from 'fs/promises'
import path from 'path'

import { FullConfig, FullResult, Reporter, Suite, TestCase, TestResult } from '@playwright/test/reporter'

/**
 * Enhanced Playwright Reporter
 * Generates comprehensive HTML reports with evidence gallery,
 * performance analytics, and failure analysis
 */
export class EnhancedEvidenceReporter implements Reporter {
  private config: FullConfig | undefined
  private results: TestResult[] = []
  private testCases: TestCase[] = []
  private startTime = 0

  onBegin(config: FullConfig, suite: Suite) {
    this.config = config
    this.startTime = Date.now()
    console.log('🎬 Starting enhanced evidence collection...')
  }

  onTestBegin(test: TestCase) {
    this.testCases.push(test)
  }

  onTestEnd(test: TestCase, result: TestResult) {
    this.results.push(result)
  }

  async onEnd(result: FullResult) {
    const endTime = Date.now()
    const duration = endTime - this.startTime

    console.log('📊 Generating enhanced evidence report...')

    try {
      // Generate comprehensive evidence report
      await this.generateEnhancedReport({
        config: this.config!,
        results: this.results,
        testCases: this.testCases,
        summary: {
          duration,
          totalTests: this.results.length,
          passed: this.results.filter(r => r.status === 'passed').length,
          failed: this.results.filter(r => r.status === 'failed').length,
          skipped: this.results.filter(r => r.status === 'skipped').length,
          flaky: this.results.filter(r => r.status === 'flaky').length,
        },
        fullResult: result,
      })

      console.log('✅ Enhanced evidence report generated successfully')
    } catch (error) {
      console.error('❌ Failed to generate enhanced evidence report:', error)
    }
  }

  private async generateEnhancedReport(data: EnhancedReportData) {
    const reportDir = path.join(this.config!.projects[0].outputDir, '..', 'enhanced-report')
    await fs.mkdir(reportDir, { recursive: true })

    // Generate main HTML report
    await this.generateMainReport(reportDir, data)

    // Generate evidence gallery
    await this.generateEvidenceGallery(reportDir, data)

    // Generate performance dashboard
    await this.generatePerformanceDashboard(reportDir, data)

    // Generate failure analysis
    await this.generateFailureAnalysis(reportDir, data)

    // Copy assets
    await this.copyAssets(reportDir)
  }

  private async generateMainReport(reportDir: string, data: EnhancedReportData) {
    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Enhanced E2E Test Report</title>
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.24.1/themes/prism-tomorrow.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.24.1/prism.min.js"></script>
</head>
<body class="bg-gray-50">
    <div class="min-h-screen">
        <!-- Header -->
        <header class="bg-white shadow-sm border-b">
            <div class="max-w-7xl mx-auto px-4 py-4">
                <div class="flex items-center justify-between">
                    <div>
                        <h1 class="text-2xl font-bold text-gray-900">Enhanced E2E Test Report</h1>
                        <p class="text-gray-600">Generated on ${new Date().toISOString()}</p>
                    </div>
                    <div class="flex space-x-4">
                        <div class="text-center">
                            <div class="text-2xl font-bold text-green-600">${data.summary.passed}</div>
                            <div class="text-sm text-gray-600">Passed</div>
                        </div>
                        <div class="text-center">
                            <div class="text-2xl font-bold text-red-600">${data.summary.failed}</div>
                            <div class="text-sm text-gray-600">Failed</div>
                        </div>
                        <div class="text-center">
                            <div class="text-2xl font-bold text-yellow-600">${data.summary.flaky}</div>
                            <div class="text-sm text-gray-600">Flaky</div>
                        </div>
                        <div class="text-center">
                            <div class="text-2xl font-bold text-gray-600">${data.summary.skipped}</div>
                            <div class="text-sm text-gray-600">Skipped</div>
                        </div>
                    </div>
                </div>
            </div>
        </header>

        <!-- Navigation -->
        <nav class="bg-white border-b">
            <div class="max-w-7xl mx-auto px-4">
                <div class="flex space-x-8">
                    <a href="index.html" class="border-b-2 border-blue-500 py-2 px-1 text-blue-600 font-medium">Overview</a>
                    <a href="evidence-gallery.html" class="py-2 px-1 text-gray-600 hover:text-gray-900">Evidence Gallery</a>
                    <a href="performance.html" class="py-2 px-1 text-gray-600 hover:text-gray-900">Performance</a>
                    <a href="failures.html" class="py-2 px-1 text-gray-600 hover:text-gray-900">Failure Analysis</a>
                </div>
            </div>
        </nav>

        <!-- Main Content -->
        <main class="max-w-7xl mx-auto px-4 py-8">
            <!-- Summary Stats -->
            <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                <div class="bg-white rounded-lg shadow p-6">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-sm font-medium text-gray-600">Total Duration</p>
                            <p class="text-2xl font-bold text-gray-900">${Math.round(data.summary.duration / 1000)}s</p>
                        </div>
                        <div class="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                            <svg class="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"></path>
                            </svg>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-lg shadow p-6">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-sm font-medium text-gray-600">Success Rate</p>
                            <p class="text-2xl font-bold text-green-600">${Math.round((data.summary.passed / data.summary.totalTests) * 100)}%</p>
                        </div>
                        <div class="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center">
                            <svg class="w-4 h-4 text-green-600" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path>
                            </svg>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-lg shadow p-6">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-sm font-medium text-gray-600">Browser Coverage</p>
                            <p class="text-2xl font-bold text-purple-600">${this.config?.projects.length || 0}</p>
                        </div>
                        <div class="w-8 h-8 bg-purple-100 rounded-full flex items-center justify-center">
                            <svg class="w-4 h-4 text-purple-600" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm0 4a1 1 0 011-1h12a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1V8z" clip-rule="evenodd"></path>
                            </svg>
                        </div>
                    </div>
                </div>

                <div class="bg-white rounded-lg shadow p-6">
                    <div class="flex items-center justify-between">
                        <div>
                            <p class="text-sm font-medium text-gray-600">Evidence Items</p>
                            <p class="text-2xl font-bold text-indigo-600">${this.countEvidenceItems(data)}</p>
                        </div>
                        <div class="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center">
                            <svg class="w-4 h-4 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd"></path>
                            </svg>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Test Results -->
            <div class="bg-white rounded-lg shadow mb-8">
                <div class="px-6 py-4 border-b border-gray-200">
                    <h2 class="text-lg font-semibold text-gray-900">Test Results</h2>
                </div>
                <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200">
                        <thead class="bg-gray-50">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Test</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Duration</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Browser</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Evidence</th>
                            </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200">
                            ${this.generateTestRowsHtml(data)}
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
</body>
</html>`

    await fs.writeFile(path.join(reportDir, 'index.html'), html)
  }

  private async generateEvidenceGallery(reportDir: string, data: EnhancedReportData) {
    // Implementation for evidence gallery
    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Evidence Gallery - Enhanced E2E Test Report</title>
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
</head>
<body class="bg-gray-50">
    <!-- Evidence gallery implementation -->
    <div class="min-h-screen">
        <h1 class="text-3xl font-bold text-center py-8">Evidence Gallery</h1>
        <div class="max-w-7xl mx-auto px-4">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <!-- Evidence items will be populated here -->
                <p class="text-gray-600 text-center col-span-full">Evidence gallery will show screenshots, videos, and network traces from test executions.</p>
            </div>
        </div>
    </div>
</body>
</html>`

    await fs.writeFile(path.join(reportDir, 'evidence-gallery.html'), html)
  }

  private async generatePerformanceDashboard(reportDir: string, data: EnhancedReportData) {
    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Performance Dashboard - Enhanced E2E Test Report</title>
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body class="bg-gray-50">
    <div class="min-h-screen">
        <h1 class="text-3xl font-bold text-center py-8">Performance Dashboard</h1>
        <div class="max-w-7xl mx-auto px-4">
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <div class="bg-white p-6 rounded-lg shadow">
                    <h2 class="text-xl font-semibold mb-4">Load Times</h2>
                    <canvas id="loadTimeChart"></canvas>
                </div>
                <div class="bg-white p-6 rounded-lg shadow">
                    <h2 class="text-xl font-semibold mb-4">Network Requests</h2>
                    <canvas id="networkChart"></canvas>
                </div>
            </div>
        </div>
    </div>
    <script>
        // Chart implementations would go here
        console.log('Performance dashboard loaded');
    </script>
</body>
</html>`

    await fs.writeFile(path.join(reportDir, 'performance.html'), html)
  }

  private async generateFailureAnalysis(reportDir: string, data: EnhancedReportData) {
    const failedTests = data.results.filter(r => r.status === 'failed')

    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Failure Analysis - Enhanced E2E Test Report</title>
    <link href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css" rel="stylesheet">
</head>
<body class="bg-gray-50">
    <div class="min-h-screen">
        <h1 class="text-3xl font-bold text-center py-8">Failure Analysis</h1>
        <div class="max-w-7xl mx-auto px-4">
            ${failedTests.length > 0 ? `
                <div class="space-y-6">
                    ${failedTests.map((result, index) => `
                        <div class="bg-white rounded-lg shadow p-6">
                            <h2 class="text-xl font-semibold text-red-600 mb-4">Failed Test ${index + 1}</h2>
                            <div class="space-y-4">
                                <div>
                                    <h3 class="font-medium text-gray-900">Error Details</h3>
                                    <pre class="bg-gray-100 p-4 rounded text-sm overflow-x-auto">${result.error?.message || 'No error message available'}</pre>
                                </div>
                                <div>
                                    <h3 class="font-medium text-gray-900">Stack Trace</h3>
                                    <pre class="bg-gray-100 p-4 rounded text-sm overflow-x-auto">${result.error?.stack || 'No stack trace available'}</pre>
                                </div>
                            </div>
                        </div>
                    `).join('')}
                </div>
            ` : `
                <div class="text-center">
                    <div class="text-green-600 text-6xl mb-4">✅</div>
                    <h2 class="text-2xl font-semibold text-gray-900 mb-2">All Tests Passed!</h2>
                    <p class="text-gray-600">No failures to analyze.</p>
                </div>
            `}
        </div>
    </div>
</body>
</html>`

    await fs.writeFile(path.join(reportDir, 'failures.html'), html)
  }

  private async copyAssets(reportDir: string) {
    // Copy any additional assets needed for the report
    const assetsDir = path.join(reportDir, 'assets')
    await fs.mkdir(assetsDir, { recursive: true })
  }

  private generateTestRowsHtml(data: EnhancedReportData): string {
    return data.results.map((result, index) => {
      const testCase = data.testCases[index]
      const statusColor = this.getStatusColor(result.status)
      const statusIcon = this.getStatusIcon(result.status)

      return `
        <tr>
            <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm font-medium text-gray-900">${testCase?.title || 'Unknown Test'}</div>
                <div class="text-sm text-gray-500">${testCase?.location?.file || 'Unknown File'}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusColor}">
                    ${statusIcon} ${result.status}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                ${Math.round(result.duration)}ms
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                ${testCase?.parent?.project()?.name || 'Unknown'}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-blue-600">
                <a href="#" class="hover:text-blue-900">View Evidence</a>
            </td>
        </tr>`
    }).join('')
  }

  private getStatusColor(status: string): string {
    switch (status) {
      case 'passed': return 'bg-green-100 text-green-800'
      case 'failed': return 'bg-red-100 text-red-800'
      case 'skipped': return 'bg-gray-100 text-gray-800'
      case 'flaky': return 'bg-yellow-100 text-yellow-800'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  private getStatusIcon(status: string): string {
    switch (status) {
      case 'passed': return '✅'
      case 'failed': return '❌'
      case 'skipped': return '⏭️'
      case 'flaky': return '🔄'
      default: return '❓'
    }
  }

  private countEvidenceItems(data: EnhancedReportData): number {
    // This would count actual evidence items from test results
    return data.results.reduce((count, result) => {
      return count + (result.attachments?.length || 0)
    }, 0)
  }
}

interface EnhancedReportData {
  config: FullConfig
  results: TestResult[]
  testCases: TestCase[]
  summary: {
    duration: number
    totalTests: number
    passed: number
    failed: number
    skipped: number
    flaky: number
  }
  fullResult: FullResult
}

export default EnhancedEvidenceReporter