import { test, expect } from '@playwright/test'
import { loginAsUser, mockApiError, waitForToast, checkBasicAccessibility } from './utils/test-utils'

test.describe('Complete User Journey', () => {
  test('should complete full user onboarding and subscription flow', async ({ page }) => {
    // Start from unauthenticated state
    await page.goto('/')

    // Should redirect to login
    await expect(page).toHaveURL(/\/auth\/login/)

    // Mock successful authentication flow
    await loginAsUser(page)

    // Should be on dashboard
    await expect(page).toHaveURL('/dashboard')
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()

    // Navigate to organizations
    await page.getByRole('link', { name: 'Organizations' }).click()
    await expect(page).toHaveURL('/organizations')

    // Create a new organization
    await page.getByRole('button', { name: 'New Organization' }).click()
    await expect(page.getByRole('dialog')).toBeVisible()

    // Mock create organization API
    await page.route('/api/v1/organizations', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'org-new',
            name: 'My New Company',
            slug: 'my-new-company',
            status: 'ACTIVE',
            userRole: 'OWNER',
            memberCount: 1,
            createdAt: new Date().toISOString(),
          }),
        })
      } else {
        await route.continue()
      }
    })

    // Fill organization form
    await page.getByRole('textbox', { name: 'Organization Name' }).fill('My New Company')
    await page.getByRole('textbox', { name: 'Description (Optional)' }).fill('A test company for our new product')
    await page.getByRole('button', { name: 'Create Organization' }).click()

    // Should close modal
    await expect(page.getByRole('dialog')).not.toBeVisible()

    // Navigate to subscription page
    await page.getByRole('link', { name: 'Subscription' }).click()
    await expect(page).toHaveURL('/subscription')

    // Should show no subscription state
    await expect(page.getByText('No Active Subscription')).toBeVisible()
    await page.getByRole('button', { name: 'Choose a Plan' }).click()

    // Should open plan selection modal
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText('Choose a Plan')).toBeVisible()

    // Select Pro plan
    await page.getByText('Pro Plan').click()
    await expect(page.getByRole('button', { name: 'Start Subscription' })).toBeEnabled()

    // Mock create subscription API
    await page.route('/api/v1/organizations/org-123/subscription', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'sub-new',
            planId: 'plan-pro',
            status: 'TRIALING',
            currentPeriodStart: new Date().toISOString(),
            currentPeriodEnd: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
          }),
        })
      } else {
        await route.continue()
      }
    })

    // Start subscription
    await page.getByRole('button', { name: 'Start Subscription' }).click()

    // Should close modal
    await expect(page.getByRole('dialog')).not.toBeVisible()

    // Navigate to payments to set up payment method
    await page.getByRole('link', { name: 'Payments' }).click()
    await expect(page).toHaveURL('/payments')

    // Open payment methods modal
    await page.getByRole('button', { name: 'Payment Methods' }).click()
    await expect(page.getByRole('dialog')).toBeVisible()

    // Should show empty state initially
    await expect(page.getByText('No payment methods')).toBeVisible()

    // Simulate adding a payment method (in real app, this would open Stripe)
    await page.getByRole('button', { name: 'Add Payment Method' }).click()

    // Close payment methods modal
    await page.getByRole('button', { name: 'Close' }).click()
    await expect(page.getByRole('dialog')).not.toBeVisible()

    // Navigate to organization detail page
    await page.getByRole('link', { name: 'Organizations' }).click()
    await page.getByRole('link', { name: /test organization/i }).click()
    await expect(page).toHaveURL('/organizations/test-org')

    // Invite a team member
    await page.getByRole('button', { name: 'Invite Member' }).click()
    await expect(page.getByText('Invite New Member')).toBeVisible()

    // Mock invite member API
    await page.route('/api/v1/organizations/org-123/members/invite', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'invitation-123',
          email: 'teammate@example.com',
          role: 'MEMBER',
          status: 'PENDING',
        }),
      })
    })

    // Fill and submit invite form
    await page.getByRole('textbox', { name: 'Email address' }).fill('teammate@example.com')
    await page.getByRole('combobox', { name: 'Role' }).selectOption('MEMBER')
    await page.getByRole('button', { name: 'Send Invitation' }).click()

    // Should hide invite form
    await expect(page.getByText('Invite New Member')).not.toBeVisible()

    // Navigate to settings
    await page.getByRole('link', { name: 'Settings' }).click()
    await expect(page).toHaveURL('/settings')

    // Update profile information
    await expect(page.getByText('Profile Information')).toBeVisible()
    await page.getByRole('textbox', { name: 'Full Name' }).fill('John Doe Updated')
    await page.getByRole('combobox', { name: 'Timezone' }).selectOption('America/New_York')

    // Mock update profile API
    await page.route('/api/v1/user/profile', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'user-123',
            name: 'John Doe Updated',
            email: 'test@example.com',
            preferences: {
              timezone: 'America/New_York',
              language: 'en',
            },
          }),
        })
      } else {
        await route.continue()
      }
    })

    await page.getByRole('button', { name: 'Save Changes' }).click()

    // Navigate back to dashboard to see the complete setup
    await page.getByRole('link', { name: 'Dashboard' }).click()
    await expect(page).toHaveURL('/dashboard')

    // Verify dashboard shows updated information
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
    await expect(page.getByText('John Doe Updated')).toBeVisible()

    // Test logout
    await page.getByRole('button', { name: 'User menu' }).click()

    // Mock logout API
    await page.route('/api/v1/auth/logout', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true }),
      })
    })

    await page.getByRole('menuitem', { name: 'Sign out' }).click()

    // Should redirect to login
    await expect(page).toHaveURL(/\/auth\/login/)
  })

  test('should handle error states gracefully', async ({ page }) => {
    await loginAsUser(page)

    // Test API error handling on organizations page
    await mockApiError(page, '/api/v1/organizations/user', 500, 'Server Error')

    await page.getByRole('link', { name: 'Organizations' }).click()
    await expect(page.getByText('Failed to load organizations')).toBeVisible()

    // Test form validation errors
    await page.goto('/organizations')

    // Try to create organization with invalid data
    await page.getByRole('button', { name: 'New Organization' }).click()
    await page.getByRole('button', { name: 'Create Organization' }).click()

    // Should show validation errors
    await expect(page.getByText('Organization name is required')).toBeVisible()
    await expect(page.getByText('Slug is required')).toBeVisible()

    // Close modal
    await page.getByRole('button', { name: 'Cancel' }).click()

    // Test network error handling
    await page.goto('/payments')

    // Mock network failure
    await mockApiError(page, '/api/v1/organizations/org-123/payments', 0, 'Network Error')
    await page.reload()

    // Should handle network errors gracefully
    // Note: Exact error handling depends on implementation
  })

  test('should be accessible', async ({ page }) => {
    await loginAsUser(page)

    // Test accessibility on major pages
    const pages = [
      '/dashboard',
      '/organizations',
      '/payments',
      '/subscription',
      '/settings',
    ]

    for (const pagePath of pages) {
      await page.goto(pagePath)
      await checkBasicAccessibility(page)
    }
  })

  test('should work on mobile devices', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })

    await loginAsUser(page)

    // Test mobile navigation
    const mobileMenuToggle = page.getByRole('button', { name: /toggle navigation/i })
    await expect(mobileMenuToggle).toBeVisible()

    // Open mobile menu
    await mobileMenuToggle.click()

    // Should show navigation items
    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Organizations' })).toBeVisible()

    // Test navigation works on mobile
    await page.getByRole('link', { name: 'Organizations' }).click()
    await expect(page).toHaveURL('/organizations')

    // Test modal behavior on mobile
    await page.getByRole('button', { name: 'New Organization' }).click()
    await expect(page.getByRole('dialog')).toBeVisible()

    // Modal should be properly sized for mobile
    const modal = page.getByRole('dialog')
    const modalBox = await modal.boundingBox()
    const viewport = page.viewportSize()

    if (modalBox && viewport) {
      // Modal should not exceed viewport width
      expect(modalBox.width).toBeLessThanOrEqual(viewport.width)
    }

    // Close modal
    await page.getByRole('button', { name: 'Cancel' }).click()

    // Test form inputs on mobile
    await page.goto('/settings')

    // Form fields should be properly sized and accessible
    const nameInput = page.getByRole('textbox', { name: 'Full Name' })
    await expect(nameInput).toBeVisible()

    // Test that input is large enough for mobile interaction
    const inputBox = await nameInput.boundingBox()
    if (inputBox) {
      expect(inputBox.height).toBeGreaterThanOrEqual(44) // iOS minimum touch target
    }
  })

  test('should handle slow network conditions', async ({ page }) => {
    await loginAsUser(page)

    // Simulate slow network
    await page.route('/api/**', async (route) => {
      await new Promise(resolve => setTimeout(resolve, 2000)) // 2 second delay
      await route.continue()
    })

    await page.goto('/organizations')

    // Should show loading state
    await expect(page.getByTestId('loading-spinner')).toBeVisible()

    // Should eventually load content
    await expect(page.getByText('Test Organization')).toBeVisible({ timeout: 10000 })
  })
})