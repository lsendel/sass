import { test as base, expect } from '@playwright/test'
import fs from 'fs'
import path from 'path'

// Extended Playwright test that captures Istanbul coverage from the browser
// and writes it to coverage/.nyc_output per test. This enables E2E code
// coverage reporting with nyc/istanbul when the app is instrumented.

export const test = base.extend({
  page: async ({ page }, use, testInfo) => {
    await use(page)

    try {
      // Try to read window.__coverage__ from the page
      const coverage = await page.evaluate(() => {
        const g = globalThis as Record<string, unknown>
        const c = g['__coverage__']
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
})

export { expect }
