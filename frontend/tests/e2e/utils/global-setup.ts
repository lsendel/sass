import { chromium, FullConfig } from '@playwright/test'

/**
 * Global setup for Playwright tests
 * Runs once before all tests across all workers
 */

async function globalSetup(config: FullConfig) {
  const { baseURL } = config.projects[0].use

  console.log('🚀 Starting global setup...')

  // Launch browser for setup
  const browser = await chromium.launch()
  const page = await browser.newPage()

  try {
    // Wait for the app to be ready
    console.log('⏳ Waiting for application to be ready...')
    await page.goto(baseURL ?? 'http://localhost:3000')

    // Wait for app to load
    await page.waitForSelector('[data-testid="app-ready"]', {
      timeout: 30000,
      state: 'attached'
    }).catch(() => {
      // If app-ready selector doesn't exist, just wait for body
      return page.waitForSelector('body')
    })

    // Set up test data if needed
    console.log('📊 Setting up test data...')

    // Create test users, organizations, etc.
    await setupTestData(page)

    // Verify API connectivity
    console.log('🔗 Verifying API connectivity...')
    await verifyApiConnectivity(page)

    console.log('✅ Global setup completed successfully')

  } catch (error) {
    console.error('❌ Global setup failed:', error)
    throw error
  } finally {
    await browser.close()
  }
}

async function setupTestData(page: any) {
  // Set up any required test data
  // This could include creating test users, organizations, etc.

  try {
    // Example: Set up mock authentication state
    await page.evaluate(() => {
      // Set up any global test state
      window.__E2E_TEST__ = true

      // Set up test authentication tokens if needed
      localStorage.setItem('e2e-test-mode', 'true')
    })

    // Create test organizations, users, payment methods, etc.
    // This would typically call your backend API to set up test data

  } catch (error) {
    console.warn('⚠️ Failed to set up test data:', error)
    // Don't fail the setup if test data creation fails
  }
}

async function verifyApiConnectivity(page: any) {
  try {
    // Verify that the API is accessible
    const response = await page.evaluate(async () => {
      const response = await fetch('/api/health', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      })
      return {
        status: response.status,
        ok: response.ok,
      }
    })

    if (!response.ok) {
      console.warn('⚠️ API health check failed, but continuing with tests')
    } else {
      console.log('✅ API connectivity verified')
    }
  } catch (error) {
    console.warn('⚠️ Could not verify API connectivity:', error)
    // Don't fail setup if API is not available - tests might use mocks
  }
}

export default globalSetup