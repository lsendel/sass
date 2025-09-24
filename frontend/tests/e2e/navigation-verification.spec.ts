import { test, expect } from '@playwright/test'

/**
 * Simple navigation verification test using the actual MockLoginPage
 * This test validates the solution we documented in NAVIGATION-SOLUTION.md
 */
test.describe('Navigation Verification', () => {
  test('Mock authentication and navigation verification', async ({ page }) => {
    // Step 1: Navigate to login page
    await page.goto('http://localhost:3000/auth/login')

    // Step 2: Verify we're on the login page
    await expect(page.locator('text=Sign in to Platform')).toBeVisible()

    // Step 3: Use the demo credentials from MockLoginPage
    await page.fill('input[type="email"]', 'demo@example.com')
    await page.fill('input[type="password"]', 'DemoPassword123!')

    // Step 4: Click the login button
    await page.click('button[type="submit"]')

    // Step 5: Wait for redirect to dashboard
    await page.waitForURL(/.*\/dashboard/, { timeout: 10000 })

    // Step 6: Verify we're authenticated and on dashboard
    await expect(page).toHaveURL(/.*\/dashboard/)

    // Step 7: Verify all 5 navigation links are present and visible
    const expectedNavigation = [
      { name: 'Dashboard', href: '/dashboard' },
      { name: 'Organizations', href: '/organizations' },
      { name: 'Payments', href: '/payments' },
      { name: 'Subscription', href: '/subscription' },
      { name: 'Settings', href: '/settings' }
    ]

    // Check each navigation link (using first() to handle desktop/mobile duplicates)
    for (const nav of expectedNavigation) {
      const link = page.locator(`nav a[href="${nav.href}"]`).first()
      await expect(link).toBeVisible()
      await expect(link).toContainText(nav.name)
    }

    // Step 8: Verify navigation links exist (may be duplicated for desktop/mobile)
    const navLinks = page.locator('nav a')
    const linkCount = await navLinks.count()
    expect(linkCount).toBeGreaterThanOrEqual(5)

    // Step 9: Test navigation to each page
    for (const nav of expectedNavigation) {
      await page.click(`nav a[href="${nav.href}"]`, { first: true })
      await expect(page).toHaveURL(new RegExp(`.*${nav.href}`))

      // Verify page loaded successfully (no error messages)
      const errorMessage = page.locator('text=Failed to load')
      await expect(errorMessage).not.toBeVisible()
    }
  })

  test('Verify unauthenticated users cannot access navigation', async ({ page }) => {
    // Step 1: Try to access dashboard directly without authentication
    await page.goto('http://localhost:3000/dashboard')

    // Step 2: Should be redirected to login page
    await expect(page).toHaveURL(/.*\/auth\/login/)

    // Step 3: Verify navigation is not visible when not authenticated
    const navLinks = page.locator('nav a')
    await expect(navLinks).toHaveCount(0)
  })

  test('Verify authentication state after mock login', async ({ page }) => {
    // Login with mock credentials
    await page.goto('http://localhost:3000/auth/login')
    await page.fill('input[type="email"]', 'demo@example.com')
    await page.fill('input[type="password"]', 'DemoPassword123!')
    await page.click('button[type="submit"]')
    await page.waitForURL(/.*\/dashboard/)

    // Verify we're successfully authenticated (navigation is visible)
    const navLinks = page.locator('nav a')
    const linkCount = await navLinks.count()
    expect(linkCount).toBeGreaterThanOrEqual(5)

    // Verify we can access protected content
    await expect(page.locator('text=Welcome')).toBeVisible({ timeout: 10000 })
  })
})