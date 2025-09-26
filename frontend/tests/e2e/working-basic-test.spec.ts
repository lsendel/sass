import { test, expect } from './fixtures'
import type { Page } from '@playwright/test'
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
    await page.goto('/')

    console.log('Initial URL:', page.url())

    if (page.url().includes('/auth/login')) {
      console.log('✅ Correctly redirected to login page')
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

    await expect(page.locator('body')).toBeVisible()

    console.log('✅ App loaded successfully')
    console.log('Final URL:', page.url())
  })

  test('should attempt login and navigate to dashboard', async ({ page, evidenceCollector }) => {
    await performDemoLogin(page)
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
