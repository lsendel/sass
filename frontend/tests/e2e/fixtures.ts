import fs from 'fs'
import path from 'path'

import { test as base, expect } from '@playwright/test'

import { EvidenceCollector, withEvidenceCollection } from './utils/evidence-collector'

// Extended Playwright test that captures Istanbul coverage and enhanced evidence
interface MyFixtures {
  evidenceCollector: EvidenceCollector
}

export const test = base.extend<MyFixtures>({
  page: async ({ page }, use, testInfo) => {
    await use(page)

    try {
      // Try to read window.__coverage__ from the page
      const coverage = await page.evaluate(() => {
        const g = globalThis as Record<string, unknown>
        const c = g.__coverage__
        return (c && typeof c === 'object') ? (c as Record<string, unknown>) : null
      })
      if (coverage) {
        const outDir = path.join(process.cwd(), 'coverage', '.nyc_output')
        fs.mkdirSync(outDir, { recursive: true })
        const filenameSafe = testInfo.titlePath.join(' - ').replace(/[^a-z0-9-_\s]/gi, '_').replace(/\s+/g, '_')
        const filePath = path.join(outDir, `playwright-coverage-${filenameSafe}.json`)
        fs.writeFileSync(filePath, JSON.stringify(coverage))
      }
    } catch {
      // Ignore coverage capture errors to not fail tests
    }
  },

  evidenceCollector: async ({ page }, use, testInfo) => {
    // Set up enhanced evidence collection
    const collector = await withEvidenceCollection(page, testInfo)

    // Don't take initial screenshot here - let tests control when to capture
    // This prevents white/blank page screenshots before content loads

    // Provide collector to test
    await use(collector)

    // Generate final evidence report
    try {
      await collector.takeStepScreenshot('test-end', { fullPage: true })
      await collector.generateEvidenceReport()
    } catch (error) {
      console.warn('Failed to generate evidence report:', error)
    }
  },
})

export { expect }
