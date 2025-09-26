import { test, expect } from './fixtures'
import {
  mockAuthentication,
  mockOrganizations,
  mockJsonRoute,
  createTestOrganization,
} from './utils/test-utils'

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthentication(page)

    await mockOrganizations(page, [
      createTestOrganization({
        id: 'org-123',
        memberCount: 5,
      }),
    ])

    await mockJsonRoute(page, '/api/v1/statistics/dashboard', {
      totalOrganizations: 1,
      totalMembers: 5,
      activeSubscriptions: 1,
      totalRevenue: 2500.00,
      recentActivity: [
        {
          id: 'activity-1',
          type: 'subscription_created',
          description: 'New subscription created',
          timestamp: '2024-01-15T10:30:00Z',
          organizationName: 'Test Organization',
        },
        {
          id: 'activity-2',
          type: 'member_invited',
          description: 'New member invited',
          timestamp: '2024-01-14T15:45:00Z',
          organizationName: 'Test Organization',
        },
      ],
    })

    await page.goto('/dashboard')
  })

  test('should display dashboard header', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
    await expect(page.getByText('Welcome back! Here\'s what\'s happening with your organizations.')).toBeVisible()
  })

  test('should display statistics cards', async ({ page }) => {
    // Check for statistics cards
    await expect(page.getByText('Organizations')).toBeVisible()
    await expect(page.getByText('1')).toBeVisible() // Total organizations

    await expect(page.getByText('Team Members')).toBeVisible()
    await expect(page.getByText('5')).toBeVisible() // Total members

    await expect(page.getByText('Active Subscriptions')).toBeVisible()

    await expect(page.getByText('Total Revenue')).toBeVisible()
    await expect(page.getByText('$2,500.00')).toBeVisible()
  })

  test('should display recent activity', async ({ page }) => {
    await expect(page.getByText('Recent Activity')).toBeVisible()

    // Check for activity items
    await expect(page.getByText('New subscription created')).toBeVisible()
    await expect(page.getByText('New member invited')).toBeVisible()
    await expect(page.getByText('Test Organization')).toBeVisible()
  })

  test('should navigate to organizations from dashboard', async ({ page }) => {
    // Click on organizations link/button
    await page.getByRole('link', { name: 'Organizations' }).first().click()

    await expect(page).toHaveURL('/organizations')
  })

  test('should display user profile in navigation', async ({ page }) => {
    // Check for user profile elements in navigation
    await expect(page.getByText('Test User')).toBeVisible()
    await expect(page.getByText('test@example.com')).toBeVisible()
  })

  test('should handle navigation menu', async ({ page }) => {
    // Test main navigation links
    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Organizations' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Payments' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Subscription' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Settings' })).toBeVisible()

    // Test navigation to different sections
    await page.getByRole('link', { name: 'Payments' }).click()
    await expect(page).toHaveURL('/payments')

    await page.getByRole('link', { name: 'Dashboard' }).click()
    await expect(page).toHaveURL('/dashboard')
  })

  test('should handle responsive navigation on mobile', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })

    // Check if mobile menu toggle is visible
    const mobileMenuToggle = page.getByRole('button', { name: /toggle navigation/i })
    await expect(mobileMenuToggle).toBeVisible()

    // Open mobile menu
    await mobileMenuToggle.click()

    // Check if navigation items are visible in mobile menu
    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Organizations' })).toBeVisible()
  })

  test('should handle logout', async ({ page }) => {
    // Mock logout endpoint
    await mockJsonRoute(page, '/api/v1/auth/logout', { success: true })

    // Find and click logout button
    const userMenu = page.getByRole('button', { name: 'User menu' })
    await userMenu.click()

    const logoutButton = page.getByRole('menuitem', { name: 'Sign out' })
    await logoutButton.click()

    // Should redirect to login page
    await expect(page).toHaveURL(/\/auth\/login/)
  })
})
