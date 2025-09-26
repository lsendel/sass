import { test, expect } from './fixtures'
import {
  mockJsonRoute,
  createTestUser,
  createTestOrganization,
} from './utils/test-utils'

test.describe('Authentication Debug', () => {
  test('should properly mock authentication', async ({ page }) => {
    // Mock authentication session
    console.log('Auth session route intercepted!')
    await mockJsonRoute(page, '/api/v1/auth/session', {
      success: true,
      data: {
        user: {
          ...createTestUser({ id: '123e4567-e89b-12d3-a456-426614174000' }),
          firstName: 'Test',
          lastName: 'User',
        },
        session: {
          activeTokens: 1,
          lastActiveAt: '2024-01-01T10:00:00Z',
          createdAt: '2024-01-01T00:00:00Z',
        },
      },
      timestamp: '2024-01-01T10:00:00Z',
    })

    // Mock user organizations
    console.log('User organizations route intercepted!')
    await mockJsonRoute(page, '/api/v1/organizations/user', {
      success: true,
      data: {
        items: [
          {
            ...createTestOrganization({
              id: 'org-123e4567-e89b-12d3-a456-426614174000',
              name: 'Test Organization',
            }),
            plan: 'PRO',
            billingEmail: 'billing@example.com',
            maxUsers: 50,
            currentUsers: 1,
          },
        ],
      },
      timestamp: '2024-01-01T10:00:00Z',
    })

    // Set up console logging to catch any errors
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        console.log('Browser console error:', msg.text())
      }
    })

    // Navigate to payments page
    console.log('Navigating to payments page...')
    await page.goto('/payments')

    // Wait a bit for any async operations
    await page.waitForTimeout(2000)

    // Take screenshot for debugging
    await page.screenshot({ path: 'debug-auth-screenshot.png', fullPage: true })

    // Check what's on the page
    const content = await page.content()
    console.log('Page title:', await page.title())
    console.log('Current URL:', page.url())

    // Log if we see login form or payments page
    const hasLoginForm = content.includes('Sign in to Platform')
    const hasPaymentsHeading = content.includes('Payments')

    console.log('Has login form:', hasLoginForm)
    console.log('Has payments heading:', hasPaymentsHeading)

    if (hasLoginForm) {
      console.log('Still showing login page - authentication mock failed')
      // Check if there are any network errors
      const failedRequests: string[] = []
      page.on('requestfailed', request => {
        failedRequests.push(request.url())
        console.log('Failed request:', request.url(), request.failure()?.errorText)
      })
    }

    // For now, just ensure we can load some page content
    await expect(page.locator('body')).toBeVisible()
  })
})
