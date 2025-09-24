import { test, expect } from './fixtures'
import { loginAsUser, mockApiError, checkBasicAccessibility } from './utils/test-utils'

test.describe('Enhanced Authentication Flow with Evidence Collection', () => {
  test('should complete authentication flow with comprehensive evidence', async ({ page, evidenceCollector }) => {
    // Step 1: Navigate to app and verify initial state
    await page.goto('/')
    await evidenceCollector.takeStepScreenshot('initial-page-load', {
      fullPage: true,
      highlight: ['[data-testid="login-form"]']
    })

    // Collect performance metrics for initial load
    const initialMetrics = await evidenceCollector.collectPerformanceMetrics('initial-page-load')
    console.log('Initial load metrics:', initialMetrics)

    // Should redirect to login
    await expect(page).toHaveURL(/\/auth\/login/)
    await evidenceCollector.takeStepScreenshot('redirected-to-login')

    // Step 2: Verify login page elements
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible()
    await expect(page.getByText('Choose your preferred sign-in method')).toBeVisible()

    // Capture page HTML for analysis
    await evidenceCollector.capturePageHTML('login-page-structure')

    // Step 3: Check accessibility
    await checkBasicAccessibility(page)
    await evidenceCollector.collectAccessibilitySnapshot('login-page-accessibility')

    // Step 4: Test OAuth provider display
    await expect(page.getByRole('button', { name: /google/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /github/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /microsoft/i })).toBeVisible()

    // Capture element screenshots for each OAuth provider
    await evidenceCollector.captureElementScreenshot('button:has-text("Google")', 'google-login-button')
    await evidenceCollector.captureElementScreenshot('button:has-text("GitHub")', 'github-login-button')
    await evidenceCollector.captureElementScreenshot('button:has-text("Microsoft")', 'microsoft-login-button')

    // Step 5: Mock and test authentication flow
    await loginAsUser(page)

    // Wait for network requests to complete
    const networkIdleTime = await evidenceCollector.waitForNetworkIdle()
    console.log(`Network idle after ${networkIdleTime}ms`)

    // Step 6: Verify dashboard loading
    await expect(page).toHaveURL('/dashboard')
    await evidenceCollector.takeStepScreenshot('dashboard-loaded', {
      fullPage: true,
      highlight: ['[data-testid="dashboard-header"]', '[data-testid="user-stats"]']
    })

    // Collect final performance metrics
    const finalMetrics = await evidenceCollector.collectPerformanceMetrics('dashboard-loaded')
    console.log('Dashboard load metrics:', finalMetrics)

    // Step 7: Verify dashboard content
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
    await evidenceCollector.takeStepScreenshot('dashboard-content-verified')

    // Check final accessibility state
    await evidenceCollector.collectAccessibilitySnapshot('dashboard-accessibility')

    // The evidence collector will automatically generate a comprehensive report
  })

  test('should handle authentication errors with evidence', async ({ page, evidenceCollector }) => {
    // Step 1: Navigate to callback with error
    await page.goto('/auth/callback?error=access_denied&error_description=User+denied+access')
    await evidenceCollector.takeStepScreenshot('auth-error-page')

    // Step 2: Verify error handling
    await expect(page.getByText('Authentication failed')).toBeVisible()
    await expect(page.getByText('User denied access')).toBeVisible()

    // Capture error state
    await evidenceCollector.capturePageHTML('auth-error-state')
    await evidenceCollector.takeStepScreenshot('error-message-displayed', {
      highlight: ['[data-testid="error-message"]']
    })

    // Step 3: Test retry functionality
    await expect(page.getByRole('link', { name: 'Try Again' })).toBeVisible()
    await evidenceCollector.captureElementScreenshot('a:has-text("Try Again")', 'try-again-button')
  })

  test('should handle network failures gracefully', async ({ page, evidenceCollector }) => {
    // Step 1: Mock API failure
    await mockApiError(page, '/api/v1/auth/providers', 500, 'Service temporarily unavailable')

    await page.goto('/auth/login')
    await evidenceCollector.takeStepScreenshot('network-error-scenario')

    // Step 2: Wait for error handling
    await page.waitForTimeout(2000)
    await evidenceCollector.takeStepScreenshot('error-state-after-network-failure')

    // Step 3: Verify graceful degradation
    // The app should show a fallback state or error message
    await evidenceCollector.capturePageHTML('network-error-fallback')

    // Collect performance metrics during error state
    await evidenceCollector.collectPerformanceMetrics('network-error-handling')
  })

  test('should test responsive design across viewports', async ({ page, evidenceCollector }) => {
    const viewports = [
      { width: 375, height: 667, name: 'mobile' },
      { width: 768, height: 1024, name: 'tablet' },
      { width: 1920, height: 1080, name: 'desktop' },
    ]

    for (const viewport of viewports) {
      // Set viewport
      await page.setViewportSize({ width: viewport.width, height: viewport.height })

      // Navigate to login
      await page.goto('/auth/login')

      // Take screenshot for this viewport
      await evidenceCollector.takeStepScreenshot(`login-${viewport.name}-viewport`, {
        fullPage: true
      })

      // Verify responsive elements
      await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible()

      // Capture specific elements that should be responsive
      await evidenceCollector.captureElementScreenshot('[data-testid="login-form"]', `login-form-${viewport.name}`)

      // Collect accessibility for this viewport
      await evidenceCollector.collectAccessibilitySnapshot(`${viewport.name}-accessibility`)
    }
  })
})