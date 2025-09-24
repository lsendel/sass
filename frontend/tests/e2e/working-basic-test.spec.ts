import { test, expect } from '@playwright/test'

// Helper function to analyze navigation elements
async function analyzeNavigationElements(page: any) {
  console.log('🔍 Analyzing navigation elements on dashboard...')

  // Look for various types of navigation elements
  const navSelectors = [
    'nav',
    '[role="navigation"]',
    '.nav',
    '.navigation',
    '[data-testid*="nav"]',
    'header nav',
    'aside nav',
    '.sidebar'
  ]

  for (const selector of navSelectors) {
    const count = await page.locator(selector).count()
    if (count > 0) {
      console.log(`Found ${count} elements with selector: ${selector}`)
    }
  }

  // Look for common navigation links
  const linkTexts = ['Dashboard', 'Organizations', 'Payments', 'Subscription', 'Settings', 'Profile']

  for (const text of linkTexts) {
    const exactMatch = await page.locator(`text="${text}"`).count()
    const partialMatch = await page.locator(`text=${text}`).count()
    if (exactMatch > 0 || partialMatch > 0) {
      console.log(`Found navigation link: ${text} (exact: ${exactMatch}, partial: ${partialMatch})`)
    }
  }

  // Look for any links at all
  const allLinks = await page.locator('a').count()
  console.log(`Total links found: ${allLinks}`)

  if (allLinks > 0) {
    const linkTexts = await page.locator('a').allTextContents()
    console.log('Link texts:', linkTexts.slice(0, 10)) // Show first 10 links
  }

  // Take screenshot for analysis
  await page.screenshot({ path: 'test-results/dashboard-navigation-analysis.png', fullPage: true })
}

test.describe('Basic Working Test', () => {
  test('should load the application and handle authentication', async ({ page }) => {
    // Navigate to the app
    await page.goto('/')

    console.log('Initial URL:', page.url())

    // Check if we're redirected to login (which is expected)
    if (page.url().includes('/auth/login')) {
      console.log('✅ Correctly redirected to login page')

      // Try to find login form elements
      const loginButton = await page.locator('[data-testid="login-button"]').count()
      const emailInput = await page.locator('[data-testid="email-input"]').count()
      const passwordInput = await page.locator('[data-testid="password-input"]').count()

      console.log(`Found login elements: button=${loginButton}, email=${emailInput}, password=${passwordInput}`)

      // Try alternative selectors if data-testid doesn't exist
      if (loginButton === 0) {
        const alternativeButtons = await page.locator('button').count()
        const submitButtons = await page.locator('button[type="submit"], input[type="submit"]').count()
        console.log(`Alternative buttons: ${alternativeButtons}, submit buttons: ${submitButtons}`)
      }

      if (emailInput === 0) {
        const emailFields = await page.locator('input[type="email"], input[name="email"]').count()
        console.log(`Email fields found: ${emailFields}`)
      }

      // Take a screenshot of the login page
      await page.screenshot({ path: 'test-results/login-page.png', fullPage: true })

    } else {
      console.log('✅ App loaded without authentication required')
      await page.screenshot({ path: 'test-results/app-loaded.png', fullPage: true })
    }

    // Check if we can see the page content
    const body = await page.locator('body')
    await expect(body).toBeVisible()

    console.log('✅ App loaded successfully')
    console.log('Final URL:', page.url())
  })

  test('should attempt login and navigate to dashboard', async ({ page }) => {
    await page.goto('/')

    // Check if we're on the login page
    if (page.url().includes('/auth/login')) {
      console.log('On login page, attempting login...')

      // Try different approaches to find and fill login form

      // Approach 1: Use data-testid attributes
      try {
        const emailInput = page.locator('[data-testid="email-input"]')
        const passwordInput = page.locator('[data-testid="password-input"]')
        const submitButton = page.locator('[data-testid="submit-button"]')

        if (await emailInput.count() > 0) {
          await emailInput.fill('demo@example.com')
          await passwordInput.fill('DemoPassword123!')
          await submitButton.click()
          console.log('✅ Used data-testid selectors for login')
        } else {
          throw new Error('data-testid selectors not found')
        }
      } catch (error) {
        console.log('data-testid selectors not found, trying alternative selectors...')

        // Approach 2: Use standard input selectors
        try {
          const emailInput = page.locator('input[type="email"], input[name="email"]').first()
          const passwordInput = page.locator('input[type="password"], input[name="password"]').first()
          const submitButton = page.locator('button[type="submit"], input[type="submit"]').first()

          await emailInput.fill('demo@example.com')
          await passwordInput.fill('DemoPassword123!')
          await submitButton.click()
          console.log('✅ Used standard input selectors for login')
        } catch (error2) {
          console.log('❌ Could not find login form elements')

          // Take screenshot for debugging
          await page.screenshot({ path: 'test-results/login-form-not-found.png', fullPage: true })

          // Just continue to see what happens
        }
      }

      // Wait a moment for navigation or error messages
      await page.waitForTimeout(2000)

      console.log('After login attempt, URL:', page.url())

      // Take screenshot of result
      await page.screenshot({ path: 'test-results/after-login-attempt.png', fullPage: true })

      // If we're now on dashboard, look for navigation
      if (page.url().includes('/dashboard')) {
        console.log('✅ Successfully reached dashboard')
        await analyzeNavigationElements(page)
      } else {
        console.log('Still on login page or redirected elsewhere')
      }
    }
  })
})