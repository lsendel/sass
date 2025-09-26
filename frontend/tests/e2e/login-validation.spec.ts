import { test, expect } from './fixtures'
import {
  assertNoErrorBoundary,
  navigateToSection,
  performDemoLogin,
  verifyNavigationLinks,
} from './utils/test-utils'

test.describe('Login Validation Test', () => {
  test('should successfully login and reach dashboard without errors', async ({ page, evidenceCollector }) => {
    console.log('🔍 Testing core login functionality...')

    await performDemoLogin(page)

    await assertNoErrorBoundary(page)
    await expect(page.locator('text=Welcome back')).toBeVisible({ timeout: 3000 })

    await evidenceCollector.takeStepScreenshot('dashboard-after-login', { fullPage: true })

    console.log('🎉 Login validation test completed successfully!')
  })

  test('should have navigation elements available', async ({ page }) => {
    console.log('🔍 Testing navigation elements availability...')

    await performDemoLogin(page)

    const navLinks = [
      { name: 'Organizations', href: '/organizations', testId: 'nav-organizations' },
      { name: 'Payments', href: '/payments', testId: 'nav-payments' },
      { name: 'Subscription', href: '/subscription', testId: 'nav-subscription' },
      { name: 'Settings', href: '/settings', testId: 'nav-settings' },
    ]

    await verifyNavigationLinks(page, [{ name: 'Dashboard', href: '/dashboard', testId: 'nav-dashboard' }, ...navLinks])

    for (const link of navLinks) {
      await navigateToSection(page, link)
      await page.goBack({ waitUntil: 'networkidle' })
    }

    console.log('📋 Navigation analysis completed')
  })
})
