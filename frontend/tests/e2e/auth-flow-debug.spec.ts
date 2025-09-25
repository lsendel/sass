import { test, expect } from './fixtures'

test.describe('Authentication Flow Debug', () => {
  test('should debug complete authentication flow step by step', async ({ page }) => {
    console.log('=== Starting authentication flow debug ===')

    // Monitor Redux state changes
    await page.addInitScript(() => {
      window.addEventListener('load', () => {
        const store = (window as any).__REDUX_STORE__
        if (store) {
          let previousState = store.getState()
          store.subscribe(() => {
            const currentState = store.getState()
            if (currentState.auth !== previousState.auth) {
              console.log('AUTH STATE CHANGE:', {
                previous: previousState.auth,
                current: currentState.auth
              })
              previousState = currentState
            }
          })
        }
      })
    })

    // Monitor all network requests
    const networkLogs: string[] = []
    page.on('request', request => {
      const url = request.url()
      if (url.includes('/api/')) {
        networkLogs.push(`REQUEST: ${request.method()} ${url}`)
        console.log(`REQUEST: ${request.method()} ${url}`)
      }
    })

    page.on('response', response => {
      const url = response.url()
      if (url.includes('/api/')) {
        networkLogs.push(`RESPONSE: ${response.status()} ${url}`)
        console.log(`RESPONSE: ${response.status()} ${url}`)
      }
    })

    // Mock authentication session with detailed logging
    await page.route('/api/v1/auth/session', async (route) => {
      console.log('🔒 AUTH SESSION ROUTE INTERCEPTED!')
      const mockResponse = {
        success: true,
        data: {
          user: {
            id: '123e4567-e89b-12d3-a456-426614174000',
            email: 'test@example.com',
            firstName: 'Test',
            lastName: 'User',
            role: 'USER',
            emailVerified: true,
            organizationId: '987fcdeb-51d2-43a1-b456-426614174000',
            createdAt: '2024-01-01T00:00:00Z',
            updatedAt: '2024-01-01T00:00:00Z',
            lastLoginAt: '2024-01-01T10:00:00Z',
          },
          session: {
            activeTokens: 1,
            lastActiveAt: '2024-01-01T10:00:00Z',
            createdAt: '2024-01-01T00:00:00Z',
          },
        },
        timestamp: '2024-01-01T10:00:00Z',
      }

      console.log('📤 Sending auth response:', JSON.stringify(mockResponse, null, 2))

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockResponse),
      })
    })

    // Mock user organizations
    await page.route('/api/v1/organizations/user', async (route) => {
      console.log('🏢 ORGANIZATIONS ROUTE INTERCEPTED!')
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            items: [
              {
                id: '987fcdeb-51d2-43a1-b456-426614174000',
                name: 'Test Organization',
                plan: 'PRO',
                status: 'ACTIVE',
                billingEmail: 'billing@example.com',
                maxUsers: 50,
                currentUsers: 1,
                createdAt: '2024-01-01T00:00:00Z',
                updatedAt: '2024-01-01T00:00:00Z',
              },
            ],
          },
          timestamp: '2024-01-01T10:00:00Z',
        }),
      })
    })

    console.log('📍 Step 1: Navigate to root URL')
    await page.goto('/')
    await page.waitForTimeout(1000)

    console.log('📍 Step 2: Check current URL and page state')
    const currentUrl = page.url()
    console.log(`Current URL: ${currentUrl}`)

    // Check if we're on auth page
    const isOnAuthPage = currentUrl.includes('/auth/login')
    console.log(`Is on auth page: ${isOnAuthPage}`)

    // Get authentication state from Redux
    const authState = await page.evaluate(() => {
      const store = (window as any).__REDUX_STORE__
      return store ? store.getState().auth : null
    })
    console.log('Redux auth state:', authState)

    console.log('📍 Step 3: Wait for authentication to complete')
    // Wait for either auth to complete or timeout
    try {
      await page.waitForFunction(() => {
        const store = (window as any).__REDUX_STORE__
        if (!store) return false
        const state = store.getState()
        return state.auth.isAuthenticated === true
      }, { timeout: 10000 })
      console.log('✅ Authentication completed!')
    } catch (error) {
      console.log('⚠️ Authentication timeout, checking state...')
      const finalAuthState = await page.evaluate(() => {
        const store = (window as any).__REDUX_STORE__
        return store ? store.getState().auth : null
      })
      console.log('Final auth state:', finalAuthState)
    }

    console.log('📍 Step 4: Navigate to payments page')
    await page.goto('/payments')
    await page.waitForTimeout(2000)

    console.log('📍 Step 5: Check final page state')
    const finalUrl = page.url()
    console.log(`Final URL: ${finalUrl}`)

    const pageContent = await page.content()
    const hasLoginForm = pageContent.includes('Sign in to Platform')
    const hasPaymentsHeading = pageContent.includes('Payments')

    console.log(`Has login form: ${hasLoginForm}`)
    console.log(`Has payments heading: ${hasPaymentsHeading}`)

    console.log('📍 Step 6: Network logs')
    console.log('Network activity:', networkLogs)

    // Take screenshot for debugging
    await page.screenshot({ path: 'auth-flow-debug.png', fullPage: true })

    // For now, just ensure page loads without crashing
    await expect(page.locator('body')).toBeVisible()

    console.log('=== Authentication flow debug complete ===')
  })
})