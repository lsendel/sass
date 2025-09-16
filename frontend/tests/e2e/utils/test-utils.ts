import { Page } from '@playwright/test'

/**
 * Mock authentication for a user
 */
export async function mockAuthentication(page: Page, user = {
  id: 'user-123',
  name: 'Test User',
  email: 'test@example.com',
}) {
  await page.route('/api/v1/auth/session', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        user,
        authenticated: true,
      }),
    })
  })
}

/**
 * Mock user organizations
 */
export async function mockOrganizations(page: Page, organizations = [
  {
    id: 'org-123',
    name: 'Test Organization',
    slug: 'test-org',
    status: 'ACTIVE',
    userRole: 'OWNER',
    memberCount: 1,
    createdAt: '2024-01-01T00:00:00Z',
  },
]) {
  await page.route('/api/v1/organizations/user', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(organizations),
    })
  })
}

/**
 * Mock subscription plans
 */
export async function mockSubscriptionPlans(page: Page) {
  await page.route('/api/v1/subscriptions/plans', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 'plan-basic',
          name: 'Basic Plan',
          description: 'Perfect for small teams',
          amount: 99.00,
          interval: 'MONTH',
          trialDays: 14,
          features: {
            'Max Users': 5,
            'Storage': '10GB',
            'Support': 'Email',
          },
        },
        {
          id: 'plan-pro',
          name: 'Pro Plan',
          description: 'For growing businesses',
          amount: 299.00,
          interval: 'MONTH',
          trialDays: 14,
          features: {
            'Max Users': 25,
            'Storage': '100GB',
            'Support': 'Priority',
            'Advanced Analytics': true,
          },
        },
        {
          id: 'plan-enterprise',
          name: 'Enterprise Plan',
          description: 'For large organizations',
          amount: 999.00,
          interval: 'MONTH',
          trialDays: 30,
          features: {
            'Max Users': 'Unlimited',
            'Storage': '1TB',
            'Support': '24/7 Phone',
            'Advanced Analytics': true,
            'Custom Integrations': true,
          },
        },
      ]),
    })
  })
}

/**
 * Mock OAuth providers
 */
export async function mockOAuthProviders(page: Page) {
  await page.route('/api/v1/auth/providers', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { name: 'google', displayName: 'Google', enabled: true },
        { name: 'github', displayName: 'GitHub', enabled: true },
        { name: 'microsoft', displayName: 'Microsoft', enabled: true },
      ]),
    })
  })
}

/**
 * Mock API error response
 */
export async function mockApiError(page: Page, url: string, status = 500, message = 'Internal Server Error') {
  await page.route(url, async (route) => {
    await route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify({
        error: message,
        status,
      }),
    })
  })
}

/**
 * Mock loading state for API endpoint
 */
export async function mockLoadingState(page: Page, url: string, delay = 2000) {
  await page.route(url, async (route) => {
    await new Promise(resolve => setTimeout(resolve, delay))
    await route.continue()
  })
}

/**
 * Wait for toast notification to appear
 */
export async function waitForToast(page: Page, message: string) {
  await page.waitForSelector(`text="${message}"`, { timeout: 5000 })
}

/**
 * Login as authenticated user with all required mocks
 */
export async function loginAsUser(page: Page, user?: any, organizations?: any[]) {
  await mockAuthentication(page, user)
  await mockOrganizations(page, organizations)
  await mockOAuthProviders(page)
  await mockSubscriptionPlans(page)

  // Navigate to dashboard
  await page.goto('/dashboard')

  // Wait for authentication to complete
  await page.waitForURL('/dashboard')
}

/**
 * Fill and submit a form with retry logic
 */
export async function fillAndSubmitForm(page: Page, formData: Record<string, string>, submitButtonText = 'Submit') {
  for (const [field, value] of Object.entries(formData)) {
    await page.fill(`[name="${field}"]`, value)
  }

  await page.click(`button:has-text("${submitButtonText}")`)
}

/**
 * Mock network with realistic delays
 */
export async function mockRealisticNetwork(page: Page) {
  // Add small delays to all API calls to simulate network latency
  await page.route('/api/**', async (route) => {
    await new Promise(resolve => setTimeout(resolve, 100 + Math.random() * 200))
    await route.continue()
  })
}

/**
 * Mock offline state
 */
export async function mockOfflineState(page: Page) {
  await page.route('/api/**', async (route) => {
    await route.abort('failed')
  })
}

/**
 * Verify accessibility basics
 */
export async function checkBasicAccessibility(page: Page) {
  // Check for proper heading hierarchy
  const headings = await page.locator('h1, h2, h3, h4, h5, h6').all()
  if (headings.length > 0) {
    // Should start with h1 or h2
    const firstHeading = headings[0]
    const tagName = await firstHeading.evaluate(el => el.tagName.toLowerCase())
    if (!['h1', 'h2'].includes(tagName)) {
      throw new Error(`First heading should be h1 or h2, but found ${tagName}`)
    }
  }

  // Check for alt text on images
  const images = await page.locator('img').all()
  for (const img of images) {
    const alt = await img.getAttribute('alt')
    if (alt === null) {
      throw new Error('Image found without alt attribute')
    }
  }

  // Check for proper form labels
  const inputs = await page.locator('input[type="text"], input[type="email"], input[type="password"], textarea').all()
  for (const input of inputs) {
    const id = await input.getAttribute('id')
    const ariaLabel = await input.getAttribute('aria-label')
    const ariaLabelledby = await input.getAttribute('aria-labelledby')

    if (id) {
      const label = await page.locator(`label[for="${id}"]`).count()
      if (label === 0 && !ariaLabel && !ariaLabelledby) {
        throw new Error(`Input with id="${id}" has no associated label`)
      }
    } else if (!ariaLabel && !ariaLabelledby) {
      throw new Error('Input found without proper labeling')
    }
  }
}