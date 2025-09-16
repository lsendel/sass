import { test, expect } from '@playwright/test'

test.describe('Subscription', () => {
  test.beforeEach(async ({ page }) => {
    // Mock authentication
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
            createdAt: '2024-01-01T00:00:00Z',
          },
        ]),
      })
    })

    // Mock available plans
    await page.route('/api/v1/subscriptions/plans', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'plan-basic',
            name: 'Basic Plan',
            description: 'Perfect for small teams',
            amount: 99.00,
            interval: 'MONTH',
            trialDays: 14,
            features: {
              'Max Users': 5,
              'Storage': '10GB',
              'Support': 'Email',
            },
          },
          {
            id: 'plan-pro',
            name: 'Pro Plan',
            description: 'For growing businesses',
            amount: 299.00,
            interval: 'MONTH',
            trialDays: 14,
            features: {
              'Max Users': 25,
              'Storage': '100GB',
              'Support': 'Priority',
              'Advanced Analytics': true,
            },
          },
          {
            id: 'plan-enterprise',
            name: 'Enterprise Plan',
            description: 'For large organizations',
            amount: 999.00,
            interval: 'MONTH',
            trialDays: 30,
            features: {
              'Max Users': 'Unlimited',
              'Storage': '1TB',
              'Support': '24/7 Phone',
              'Advanced Analytics': true,
              'Custom Integrations': true,
            },
          },
        ]),
      })
    })

    // Mock active subscription
    await page.route('/api/v1/organizations/org-123/subscription', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'sub-123',
          planId: 'plan-pro',
          status: 'ACTIVE',
          currentPeriodStart: '2024-01-01T00:00:00Z',
          currentPeriodEnd: '2024-02-01T00:00:00Z',
          cancelAt: null,
        }),
      })
    })

    // Mock invoices
    await page.route('/api/v1/organizations/org-123/invoices', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'inv-123',
            invoiceNumber: 'INV-2024-001',
            totalAmount: 299.00,
            currency: 'usd',
            status: 'PAID',
            createdAt: '2024-01-01T00:00:00Z',
          },
          {
            id: 'inv-124',
            invoiceNumber: 'INV-2023-012',
            totalAmount: 299.00,
            currency: 'usd',
            status: 'PAID',
            createdAt: '2023-12-01T00:00:00Z',
          },
        ]),
      })
    })
  })

  test('should display subscription page with active subscription', async ({ page }) => {
    await page.goto('/subscription')

    // Check page header
    await expect(page.getByRole('heading', { name: 'Subscription' })).toBeVisible()
    await expect(page.getByText('Manage your subscription plan and view invoices.')).toBeVisible()

    // Check current subscription section
    await expect(page.getByText('Current Subscription')).toBeVisible()
    await expect(page.getByText('Your subscription details and billing information.')).toBeVisible()
  })

  test('should display subscription details', async ({ page }) => {
    await page.goto('/subscription')

    // Check subscription details
    await expect(page.getByText('Pro Plan')).toBeVisible()
    await expect(page.getByText('Active')).toBeVisible()
    await expect(page.getByText('$299.00 / month')).toBeVisible()

    // Check current period
    await expect(page.getByText('Current Period')).toBeVisible()
    await expect(page.getByText('Jan 1, 2024 - Feb 1, 2024')).toBeVisible()

    // Check action buttons
    await expect(page.getByRole('button', { name: 'Change Plan' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Cancel Subscription' })).toBeVisible()
  })

  test('should display invoice history', async ({ page }) => {
    await page.goto('/subscription')

    // Check invoice history section
    await expect(page.getByText('Invoice History')).toBeVisible()

    // Check table headers
    await expect(page.getByText('Invoice Number')).toBeVisible()
    await expect(page.getByText('Date')).toBeVisible()
    await expect(page.getByText('Amount')).toBeVisible()
    await expect(page.getByText('Status')).toBeVisible()

    // Check invoice entries
    await expect(page.getByText('INV-2024-001')).toBeVisible()
    await expect(page.getByText('INV-2023-012')).toBeVisible()
    await expect(page.getByText('$299.00 USD')).toBeVisible()
    await expect(page.getByText('PAID')).toBeVisible()
  })

  test('should open upgrade plan modal', async ({ page }) => {
    await page.goto('/subscription')

    // Click change plan button
    await page.getByRole('button', { name: 'Change Plan' }).click()

    // Should open modal
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText('Change Your Plan')).toBeVisible()
    await expect(page.getByText('Select a new plan for your subscription. Changes will take effect immediately.')).toBeVisible()
  })

  test('should display available plans in modal', async ({ page }) => {
    await page.goto('/subscription')

    // Open upgrade modal
    await page.getByRole('button', { name: 'Change Plan' }).click()

    // Check plans are displayed
    await expect(page.getByText('Basic Plan')).toBeVisible()
    await expect(page.getByText('Perfect for small teams')).toBeVisible()
    await expect(page.getByText('$99')).toBeVisible()
    await expect(page.getByText('14 day free trial')).toBeVisible()

    await expect(page.getByText('Pro Plan')).toBeVisible()
    await expect(page.getByText('For growing businesses')).toBeVisible()
    await expect(page.getByText('$299')).toBeVisible()
    await expect(page.getByText('Current')).toBeVisible() // Current plan badge

    await expect(page.getByText('Enterprise Plan')).toBeVisible()
    await expect(page.getByText('For large organizations')).toBeVisible()
    await expect(page.getByText('$999')).toBeVisible()
    await expect(page.getByText('30 day free trial')).toBeVisible()

    // Check features
    await expect(page.getByText('Max Users: 5')).toBeVisible()
    await expect(page.getByText('Storage: 10GB')).toBeVisible()
    await expect(page.getByText('Max Users: 25')).toBeVisible()
    await expect(page.getByText('Advanced Analytics')).toBeVisible()
  })

  test('should select and change plan', async ({ page }) => {
    // Mock change subscription plan API
    await page.route('/api/v1/organizations/org-123/subscription/sub-123/change-plan', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'sub-123',
          planId: 'plan-enterprise',
          status: 'ACTIVE',
        }),
      })
    })

    await page.goto('/subscription')

    // Open upgrade modal
    await page.getByRole('button', { name: 'Change Plan' }).click()

    // Select Enterprise plan
    await page.getByText('Enterprise Plan').click()

    // Should show selected state
    await expect(page.getByRole('button', { name: 'Change Plan' })).toBeEnabled()

    // Click change plan button
    await page.getByRole('button', { name: 'Change Plan' }).click()

    // Should close modal and show success message
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('should cancel subscription', async ({ page }) => {
    // Mock cancel subscription API
    await page.route('/api/v1/organizations/org-123/subscription/sub-123/cancel', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'sub-123',
          status: 'ACTIVE',
          cancelAt: '2024-02-01T00:00:00Z',
        }),
      })
    })

    await page.goto('/subscription')

    // Mock window.confirm
    await page.evaluate(() => {
      window.confirm = () => true
    })

    // Click cancel subscription button
    await page.getByRole('button', { name: 'Cancel Subscription' }).click()

    // Should show success message and update UI
  })

  test('should display canceled subscription state', async ({ page }) => {
    // Mock canceled subscription
    await page.route('/api/v1/organizations/org-123/subscription', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'sub-123',
          planId: 'plan-pro',
          status: 'ACTIVE',
          currentPeriodStart: '2024-01-01T00:00:00Z',
          currentPeriodEnd: '2024-02-01T00:00:00Z',
          cancelAt: '2024-02-01T00:00:00Z',
        }),
      })
    })

    await page.goto('/subscription')

    // Should show cancellation notice
    await expect(page.getByText('Cancels On')).toBeVisible()
    await expect(page.getByText('Feb 1, 2024')).toBeVisible()

    // Should show reactivate button instead of cancel
    await expect(page.getByRole('button', { name: 'Reactivate Subscription' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Cancel Subscription' })).not.toBeVisible()
  })

  test('should reactivate subscription', async ({ page }) => {
    // Mock canceled subscription
    await page.route('/api/v1/organizations/org-123/subscription', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'sub-123',
          planId: 'plan-pro',
          status: 'ACTIVE',
          cancelAt: '2024-02-01T00:00:00Z',
        }),
      })
    })

    // Mock reactivate subscription API
    await page.route('/api/v1/organizations/org-123/subscription/sub-123/reactivate', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'sub-123',
          status: 'ACTIVE',
          cancelAt: null,
        }),
      })
    })

    await page.goto('/subscription')

    // Click reactivate button
    await page.getByRole('button', { name: 'Reactivate Subscription' }).click()

    // Should show success message and update UI
  })

  test('should handle no subscription state', async ({ page }) => {
    // Mock no subscription response
    await page.route('/api/v1/organizations/org-123/subscription', async (route) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Not found' }),
      })
    })

    await page.goto('/subscription')

    // Should show no subscription state
    await expect(page.getByText('No Active Subscription')).toBeVisible()
    await expect(page.getByText('You don\'t have an active subscription. Choose a plan to get started.')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Choose a Plan' })).toBeVisible()
  })

  test('should create new subscription from no subscription state', async ({ page }) => {
    // Mock no subscription response
    await page.route('/api/v1/organizations/org-123/subscription', async (route) => {
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Not found' }),
      })
    })

    // Mock create subscription API
    await page.route('/api/v1/organizations/org-123/subscription', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'sub-new',
            planId: 'plan-basic',
            status: 'TRIALING',
          }),
        })
      } else {
        await route.continue()
      }
    })

    await page.goto('/subscription')

    // Click choose a plan button
    await page.getByRole('button', { name: 'Choose a Plan' }).click()

    // Should open modal with "Choose a Plan" title
    await expect(page.getByText('Choose a Plan')).toBeVisible()
    await expect(page.getByText('Select a plan to start your subscription.')).toBeVisible()

    // Select basic plan
    await page.getByText('Basic Plan').click()

    // Click start subscription button
    await page.getByRole('button', { name: 'Start Subscription' }).click()

    // Should close modal and show success message
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })

  test('should close upgrade plan modal', async ({ page }) => {
    await page.goto('/subscription')

    // Open upgrade modal
    await page.getByRole('button', { name: 'Change Plan' }).click()

    // Modal should be visible
    await expect(page.getByRole('dialog')).toBeVisible()

    // Close modal by clicking cancel
    await page.getByRole('button', { name: 'Cancel' }).click()

    // Modal should be hidden
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })
})