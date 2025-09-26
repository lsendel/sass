import { test, expect } from './fixtures'
import {
  performDemoLogin,
  verifyNavigationLinks,
  navigateToSection,
  type NavigationLink,
} from './utils/test-utils'

const NAV_LINKS: NavigationLink[] = [
  { name: 'Dashboard', href: '/dashboard', testId: 'nav-dashboard', waitForSelector: 'h1:has-text("Dashboard")' },
  { name: 'Organizations', href: '/organizations', testId: 'nav-organizations' },
  { name: 'Payments', href: '/payments', testId: 'nav-payments' },
  { name: 'Subscription', href: '/subscription', testId: 'nav-subscription' },
  { name: 'Settings', href: '/settings', testId: 'nav-settings' },
]

test.describe('Navigation Verification', () => {
  test('Mock authentication and navigation verification', async ({ page, evidenceCollector }) => {
    await page.goto('/auth/login')
    await expect(page.locator('text=Sign in to Platform')).toBeVisible()

    await performDemoLogin(page)

    await verifyNavigationLinks(page, NAV_LINKS)
    await evidenceCollector.takeStepScreenshot('navigation-overview', { fullPage: true })

    const navLinks = page.locator('nav a')
    const linkCount = await navLinks.count()
    expect(linkCount).toBeGreaterThanOrEqual(5)

    for (const link of NAV_LINKS) {
      await navigateToSection(page, link)
      await expect(page.locator('text=Failed to load')).toHaveCount(0)
    }
  })

  test('Verify unauthenticated users cannot access navigation', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/auth\/login/)
    await expect(page.locator('nav a')).toHaveCount(0)
  })

  test('Verify authentication state after mock login', async ({ page }) => {
    await performDemoLogin(page)

    const navLinks = page.locator('nav a')
    const linkCount = await navLinks.count()
    expect(linkCount).toBeGreaterThanOrEqual(5)
    await expect(page.locator('text=Welcome')).toBeVisible({ timeout: 10000 })
  })
})
