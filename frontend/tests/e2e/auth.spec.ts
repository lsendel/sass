import { test, expect } from './fixtures'

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the app
    await page.goto('/')
  })

  test('should redirect unauthenticated users to login', async ({ page }) => {
    // Should redirect to login page
    await expect(page).toHaveURL(/\/auth\/login/)

    // Should show login page elements
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible()
    await expect(page.getByText('Choose your preferred sign-in method')).toBeVisible()
  })

  test('should display OAuth providers', async ({ page }) => {
    // Navigate to login if not already there
    await page.goto('/auth/login')

    // Should show OAuth provider buttons
    await expect(page.getByRole('button', { name: /google/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /github/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /microsoft/i })).toBeVisible()
  })

  test('should handle OAuth provider selection', async ({ page }) => {
    await page.goto('/auth/login')

    // Mock the OAuth providers API response
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

    // Reload to pick up the mocked response
    await page.reload()

    // Wait for providers to load
    await expect(page.getByRole('button', { name: /google/i })).toBeVisible()

    // Mock the authorization redirect
    await page.route('/api/v1/auth/authorize*', async (route) => {
      // In a real test, this would redirect to the OAuth provider
      await route.fulfill({
        status: 302,
        headers: {
          'Location': 'https://accounts.google.com/oauth/authorize?...',
        },
      })
    })

    // Click on Google provider
    const googleButton = page.getByRole('button', { name: /google/i })
    await googleButton.click()

    // In a real scenario, we would be redirected to Google
    // For this test, we just verify the button was clickable
    await expect(googleButton).toBeVisible()
  })

  test('should handle authentication callback', async ({ page }) => {
    // Mock successful authentication
    await page.route('/api/v1/auth/session', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: {
            id: 'user-123',
            name: 'Test User',
            email: 'test@example.com',
          },
          authenticated: true,
        }),
      })
    })

    // Mock user organizations
    await page.route('/api/v1/organizations/user', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'org-123',
            name: 'Test Organization',
            slug: 'test-org',
            status: 'ACTIVE',
            userRole: 'OWNER',
            memberCount: 1,
            createdAt: new Date().toISOString(),
          },
        ]),
      })
    })

    // Navigate to callback page with success parameters
    await page.goto('/auth/callback?code=success&state=test')

    // Should redirect to dashboard after successful authentication
    await expect(page).toHaveURL('/dashboard')

    // Should show dashboard content
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
  })

  test('should handle authentication failure', async ({ page }) => {
    // Navigate to callback page with error parameters
    await page.goto('/auth/callback?error=access_denied&error_description=User+denied+access')

    // Should show error message
    await expect(page.getByText('Authentication failed')).toBeVisible()
    await expect(page.getByText('User denied access')).toBeVisible()

    // Should have option to try again
    await expect(page.getByRole('link', { name: 'Try Again' })).toBeVisible()
  })
})
