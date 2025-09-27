import { expect } from '@playwright/test'
import type { Page, Route } from '@playwright/test'

import type { Organization } from '@/store/api/organizationApi'
import type { User } from '@/types/api'

export type OrganizationFixture = Organization & Record<string, unknown>

export type UserFixture = User

type JsonLike = Record<string, unknown> | unknown[]

const DEFAULT_TIMESTAMP = '2024-01-01T00:00:00Z'

const DEFAULT_USER: User = {
  id: '123e4567-e89b-12d3-a456-426614174000',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  role: 'USER',
  emailVerified: true,
  organizationId: '987fcdeb-51d2-43a1-b456-426614174000',
  createdAt: DEFAULT_TIMESTAMP,
  updatedAt: DEFAULT_TIMESTAMP,
  lastLoginAt: DEFAULT_TIMESTAMP,
}

const jsonResponse = (data: JsonLike, status = 200) => ({
  status,
  contentType: 'application/json',
  body: JSON.stringify(data),
})

export async function mockJsonRoute(page: Page, url: string, data: JsonLike, status = 200) {
  await page.route(url, async (route) => {
    await fulfillJson(route, data, status)
  })
}

export async function fulfillJson(route: Route, data: JsonLike, status = 200) {
  await route.fulfill(jsonResponse(data, status))
}

export function createTestUser(overrides: Partial<UserFixture> = {}): UserFixture {
  return {
    ...DEFAULT_USER,
    ...overrides,
  }
}

export function createTestOrganization(overrides: Partial<OrganizationFixture> = {}): OrganizationFixture {
  return {
    id: '987fcdeb-51d2-43a1-b456-426614174000',
    name: 'Test Organization',
    slug: 'test-org',
    ownerId: DEFAULT_USER.id,
    settings: {},
    createdAt: DEFAULT_TIMESTAMP,
    updatedAt: DEFAULT_TIMESTAMP,
    status: 'ACTIVE',
    userRole: 'OWNER',
    memberCount: 1,
    ...overrides,
  }
}

/**
 * Mock authentication for a user
 */
export async function mockAuthentication(page: Page, userOverrides: Partial<UserFixture> = {}) {
  const user = createTestUser(userOverrides)
  await mockJsonRoute(page, '/api/v1/auth/session', {
    user,
    authenticated: true,
  })
}

/**
 * Mock user organizations
 */
export async function mockOrganizations(page: Page, organizations: OrganizationFixture[] = [createTestOrganization()]) {
  await mockJsonRoute(page, '/api/v1/organizations/user', organizations)
}

export async function mockOrganizationMembers(
  page: Page,
  organizationId: string,
  members: Array<Record<string, unknown>>
) {
  await mockJsonRoute(page, `/api/v1/organizations/${organizationId}/members`, members)
}

/**
 * Mock subscription plans
 */
export async function mockSubscriptionPlans(page: Page) {
  await mockJsonRoute(page, '/api/v1/subscriptions/plans', [
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
  ])
}

/**
 * Mock OAuth providers
 */
export async function mockOAuthProviders(page: Page) {
  await mockJsonRoute(page, '/api/v1/auth/providers', [
    { name: 'google', displayName: 'Google', enabled: true },
    { name: 'github', displayName: 'GitHub', enabled: true },
    { name: 'microsoft', displayName: 'Microsoft', enabled: true },
  ])
}

/**
 * Mock API error response
 */
export async function mockApiError(page: Page, url: string, status = 500, message = 'Internal Server Error') {
  await mockJsonRoute(page, url, { error: message, status }, status)
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
export async function loginAsUser(
  page: Page,
  user?: Partial<UserFixture>,
  organizations?: OrganizationFixture[]
) {
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
 * Perform the standard demo login flow through the UI.
 * Useful for near-real E2E scenarios that do not rely on API mocks.
 */
export async function performDemoLogin(
  page: Page,
  options: {
    startUrl?: string | null
    email?: string
    password?: string
    waitForUrl?: string | RegExp
  } = {}
) {
  const {
    startUrl = '/auth/login',
    email = 'demo@example.com',
    password = 'DemoPassword123!',
    waitForUrl = /\/dashboard$/,
  } = options

  if (startUrl) {
    await page.goto(startUrl)
  }

  const emailInput = page.locator('[data-testid="email-input"]')
  const passwordInput = page.locator('[data-testid="password-input"]')
  const submitButton = page.locator('[data-testid="submit-button"]')

  await expect(emailInput, 'Email input should be visible on the login page').toBeVisible({ timeout: 5000 })
  await expect(passwordInput, 'Password input should be visible on the login page').toBeVisible({ timeout: 5000 })

  // Some environments provide a helper button to populate demo credentials.
  const demoButton = page.locator('[data-testid="login-button"]')
  const demoButtonVisible = await demoButton.first().isVisible().catch(() => false)

  if (demoButtonVisible) {
    await demoButton.first().click()
    // Allow any scripted autofill to run before validating values.
    await page.waitForTimeout(150)
  }

  const currentEmail = await emailInput.inputValue()
  const currentPassword = await passwordInput.inputValue()

  if (!currentEmail) {
    await emailInput.fill(email)
  }
  if (!currentPassword) {
    await passwordInput.fill(password)
  }

  await expect(submitButton, 'Submit button should be actionable').toBeVisible({ timeout: 5000 })
  await submitButton.click()

  await page.waitForURL(waitForUrl, { timeout: 10000 })
}

export interface NavigationLink {
  name: string
  href: string
  testId?: string
  waitForSelector?: string
}

/**
 * Ensure expected navigation links exist and are visible.
 */
export async function verifyNavigationLinks(page: Page, links: readonly NavigationLink[]) {
  for (const link of links) {
    const navLink = page.locator(`nav a[href="${link.href}"]`).first()
    await expect(navLink, `Nav link for ${link.name} should be visible`).toBeVisible()
    await expect(navLink).toContainText(link.name)
  }
}

/**
 * Navigate to a section using either data-testid or href fallback.
 */
export async function navigateToSection(page: Page, link: NavigationLink) {
  const byTestId = link.testId ? page.locator(`[data-testid="${link.testId}"]`).first() : null
  const canUseTestId = byTestId ? await byTestId.isVisible().catch(() => false) : false

  if (canUseTestId && byTestId) {
    await byTestId.click()
  } else {
    const byHref = page.locator(`a[href="${link.href}"]`).first()
    await expect(byHref, `Fallback nav link for ${link.name} should exist`).toBeVisible()
    await byHref.click()
  }

  await page.waitForURL(`**${link.href}`, { timeout: 10000 })

  if (link.waitForSelector) {
    await expect(page.locator(link.waitForSelector)).toBeVisible()
  }
}

/**
 * Assert that the global error boundary is not present.
 */
export async function assertNoErrorBoundary(page: Page) {
  await expect(page.locator('text=Something went wrong')).toHaveCount(0)
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
