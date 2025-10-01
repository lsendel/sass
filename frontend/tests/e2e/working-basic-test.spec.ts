import type { Page } from '@playwright/test'

import { test, expect } from './fixtures'
import type { EvidenceCollector } from './utils/evidence-collector'
import { performDemoLogin, verifyNavigationLinks } from './utils/test-utils'

async function analyzeNavigationElements(page: Page, evidenceCollector: EvidenceCollector) {
  console.log('🔍 Analyzing navigation elements on dashboard...')

  const navSelectors = [
    'nav',
    '[role="navigation"]',
    '[data-testid*="nav"]',
    'header nav',
    'aside nav',
  ]

  for (const selector of navSelectors) {
    const count = await page.locator(selector).count()
    if (count > 0) {
      console.log(`Found ${count} elements with selector: ${selector}`)
    }
  }

  const linkTexts = ['Dashboard', 'Organizations', 'Payments', 'Subscription', 'Settings', 'Profile']
  for (const text of linkTexts) {
    const matches = await page.locator(`text=${text}`).count()
    if (matches > 0) {
      console.log(`Navigation link detected for: ${text}`)
    }
  }

  const allLinks = await page.locator('a').count()
  console.log(`Total links found: ${allLinks}`)

  await evidenceCollector.takeStepScreenshot('dashboard-navigation-analysis', { fullPage: true })
}

test.describe('Basic Working Test', () => {
  test('should load the application and handle authentication', async ({ page, evidenceCollector }) => {
    // Navigate to the app
    await page.goto('/', { waitUntil: 'networkidle' })

    // Take screenshot after page loads
    await evidenceCollector.takeStepScreenshot('page-loaded', { fullPage: true })

    console.log('Initial URL:', page.url())

    // Wait for body to be visible
    await expect(page.locator('body')).toBeVisible({ timeout: 10000 })

    // Check if we're on login page
    if (page.url().includes('/auth/login')) {
      console.log('✅ Correctly redirected to login page')

      // Wait for login form to be visible
      await page.waitForLoadState('domcontentloaded')
      await page.waitForTimeout(500) // Small wait for React hydration

      await evidenceCollector.takeStepScreenshot('login-page', { fullPage: true })

      const loginElements = await Promise.all([
        page.locator('[data-testid="login-button"]').count(),
        page.locator('[data-testid="email-input"]').count(),
        page.locator('[data-testid="password-input"]').count(),
      ])
      console.log(`Found login elements: button=${loginElements[0]}, email=${loginElements[1]}, password=${loginElements[2]}`)
    } else {
      console.log('✅ App loaded without authentication required')
      await evidenceCollector.takeStepScreenshot('app-loaded', { fullPage: true })
    }

    console.log('✅ App loaded successfully')
    console.log('Final URL:', page.url())
  })

  test('should attempt login and navigate to dashboard', async ({ page, evidenceCollector }) => {
    // Take screenshot before login attempt
    await page.goto('/auth/login', { waitUntil: 'networkidle' })
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(500)
    await evidenceCollector.takeStepScreenshot('before-login', { fullPage: true })

    // Perform login
    await performDemoLogin(page)

    // Take screenshot after successful login
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await evidenceCollector.takeStepScreenshot('after-login-dashboard', { fullPage: true })

    // Verify navigation links
    await verifyNavigationLinks(page, [
      { name: 'Dashboard', href: '/dashboard', testId: 'nav-dashboard' },
      { name: 'Organizations', href: '/organizations', testId: 'nav-organizations' },
      { name: 'Payments', href: '/payments', testId: 'nav-payments' },
      { name: 'Subscription', href: '/subscription', testId: 'nav-subscription' },
      { name: 'Settings', href: '/settings', testId: 'nav-settings' },
    ])

    await analyzeNavigationElements(page, evidenceCollector)
  })
})
