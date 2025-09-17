import { test, expect } from './fixtures'
import { loginAsUser, waitForToast } from './utils/test-utils'

test.describe('Settings', () => {
  test.beforeEach(async ({ page }) => {
    // Mock auth/session and orgs to be logged in
    await loginAsUser(page, {
      id: 'user-123',
      name: 'Test User',
      email: 'test@example.com',
    })

    // Mock current user profile
    await page.route('**/api/v1/users/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'user-123',
          name: 'Test User',
          email: 'test@example.com',
          preferences: {
            timezone: 'UTC',
            language: 'en',
            notifications: { email: true, push: true, sms: false },
          },
        }),
      })
    })

    await page.goto('/settings')
    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible()
  })

  test('should edit and save profile information', async ({ page }) => {
    // Ensure Profile tab is active
    await expect(page.getByRole('button', { name: /Profile/i })).toHaveClass(/border-primary-500/)

    // Mock update profile API
    await page.route('**/api/v1/users/me/profile', async (route) => {
      const body = await route.request().postDataJSON()
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'user-123',
          name: body.name,
          email: 'test@example.com',
          preferences: body.preferences,
        }),
      })
    })

    // Update fields
    await page.fill('input[placeholder="Your full name"]', 'QA User')
    await page.selectOption('select[name="timezone"]', 'Europe/Paris')
    await page.selectOption('select[name="language"]', 'fr')

    // Save
    await page.getByRole('button', { name: 'Save Changes' }).click()

    await waitForToast(page, 'Profile updated successfully')
  })

  test('should update notification preferences', async ({ page }) => {
    // Go to Notifications tab
    await page.getByRole('button', { name: /Notifications/i }).click()
    await expect(page.getByText('Notification Preferences')).toBeVisible()

    // Mock preferences update
    await page.route('**/api/v1/users/me/profile', async (route) => {
      const body = await route.request().postDataJSON()
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'user-123',
          name: 'Test User',
          email: 'test@example.com',
          preferences: body.preferences,
        }),
      })
    })

    // Toggle checkboxes by order (email, push, sms)
    const checkboxes = page.locator('input[type="checkbox"]')
    await checkboxes.nth(0).check() // email
    await checkboxes.nth(1).uncheck() // push
    await checkboxes.nth(2).check() // sms

    // Save
    await page.getByRole('button', { name: 'Save Changes' }).click()
    await waitForToast(page, 'Profile updated successfully')
  })

  test('should show security controls', async ({ page }) => {
    await page.getByRole('button', { name: /Security/i }).click()
    await expect(page.getByText('Security Settings')).toBeVisible()
    await expect(page.getByText('OAuth Authentication')).toBeVisible()
    await expect(page.getByText('Two-Factor Authentication')).toBeVisible()

    // Button should be clickable
    await page.getByRole('button', { name: 'Enable' }).click()
  })

  test('should show account management actions', async ({ page }) => {
    await page.getByRole('button', { name: /Account/i }).click()
    await expect(page.getByText('Account Management')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Export Data' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Delete' })).toBeVisible()
  })
})
