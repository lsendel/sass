import { chromium, FullConfig } from '@playwright/test'

/**
 * Global teardown for Playwright tests
 * Runs once after all tests across all workers have completed
 */

async function globalTeardown(config: FullConfig) {
  const { baseURL } = config.projects[0].use

  console.log('🧹 Starting global teardown...')

  // Launch browser for cleanup
  const browser = await chromium.launch()
  const page = await browser.newPage()

  try {
    await page.goto(baseURL || 'http://localhost:3000')

    // Clean up test data
    console.log('🗑️ Cleaning up test data...')
    await cleanupTestData(page)

    // Generate test artifacts
    console.log('📊 Generating test artifacts...')
    await generateTestArtifacts()

    console.log('✅ Global teardown completed successfully')

  } catch (error) {
    console.error('❌ Global teardown failed:', error)
    // Don't throw error to avoid masking test failures
  } finally {
    await browser.close()
  }
}

async function cleanupTestData(page: any) {
  try {
    // Clean up any test data created during setup
    await page.evaluate(() => {
      // Clear test-specific localStorage
      localStorage.removeItem('e2e-test-mode')

      // Clear any other test state
      if (window.__E2E_TEST__) {
        delete window.__E2E_TEST__
      }
    })

    // Clean up test users, organizations, etc. from backend
    // This would typically call your backend API to clean up test data

  } catch (error) {
    console.warn('⚠️ Failed to clean up test data:', error)
  }
}

async function generateTestArtifacts() {
  try {
    const fs = await import('fs')
    const path = await import('path')

    // Create test results summary
    const testResultsPath = path.join(process.cwd(), 'test-results')

    if (!fs.existsSync(testResultsPath)) {
      fs.mkdirSync(testResultsPath, { recursive: true })
    }

    // Generate summary report
    const summary = {
      timestamp: new Date().toISOString(),
      testRun: {
        completed: true,
        environment: process.env.NODE_ENV || 'test',
        baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',
      },
      cleanup: {
        completed: true,
        timestamp: new Date().toISOString(),
      }
    }

    fs.writeFileSync(
      path.join(testResultsPath, 'test-summary.json'),
      JSON.stringify(summary, null, 2)
    )

    console.log('📋 Test summary generated')

  } catch (error) {
    console.warn('⚠️ Failed to generate test artifacts:', error)
  }
}

export default globalTeardown