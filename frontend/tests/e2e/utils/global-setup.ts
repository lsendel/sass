import { chromium, FullConfig } from '@playwright/test'

/**
 * Wait for server to be available
 */
async function waitForServer(url: string, timeout: number) {
  const startTime = Date.now()

  while (Date.now() - startTime < timeout) {
    try {
      const response = await fetch(url)
      if (response.ok || response.status < 500) {
        console.log('✅ Server is ready!')
        return
      }
    } catch (error) {
      // Server not ready yet, continue waiting
    }

    // Wait 1 second before next attempt
    await new Promise(resolve => setTimeout(resolve, 1000))
  }

  throw new Error(`Server at ${url} did not become available within ${timeout}ms`)
}

/**
 * Global setup for Playwright tests
 * Runs once before all tests across all workers
 */

async function globalSetup(config: FullConfig) {
  const { baseURL } = config.projects[0].use
  const targetURL = baseURL ?? 'http://localhost:3000'

  console.log('🚀 Starting global setup...')
  console.log(`📍 Target URL: ${targetURL}`)

  // Wait for server to be available before launching browser
  console.log('⏳ Waiting for server to be ready...')
  await waitForServer(targetURL, 60000) // Wait up to 60 seconds for server

  // Launch browser for setup
  const browser = await chromium.launch()
  const page = await browser.newPage()

  try {
    // Navigate to the app
    console.log('⏳ Navigating to application...')
    await page.goto(targetURL, { waitUntil: 'domcontentloaded', timeout: 30000 })

    // Wait for app to load - just check for body tag
    await page.waitForSelector('body', { timeout: 10000 })

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