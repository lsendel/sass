import { test, expect } from './fixtures'
import { mockJsonRoute, createTestOrganization, fulfillJson } from './utils/test-utils'

test.describe('Payments', () => {
  // Helper function to navigate with authentication
  const navigateWithAuth = async (page, path: string) => {
    // Navigate to root first to trigger authentication
    await page.goto('/')

    // Wait for authentication to complete by checking Redux state
    await page.waitForFunction(() => {
      const store = (window as any).__REDUX_STORE__
      if (!store) return false
      const state = store.getState()
      return state.auth?.isAuthenticated === true && state.auth?.user != null
    }, { timeout: 10000 })

    // Now navigate to the target page
    await page.goto(path)

    // Wait for the page to load
    await page.waitForLoadState('networkidle')
  }

  test.beforeEach(async ({ page }) => {
    const organizationId = '987fcdeb-51d2-43a1-b456-426614174000'

    // Mock authentication
    await mockJsonRoute(page, '/api/v1/auth/session', {
      success: true,
      data: {
        user: {
          id: '123e4567-e89b-12d3-a456-426614174000',
          email: 'test@example.com',
          firstName: 'Test',
          lastName: 'User',
          role: 'USER',
          emailVerified: true,
          organizationId,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
          lastLoginAt: '2024-01-01T10:00:00Z',
        },
        session: {
          activeTokens: 1,
          lastActiveAt: '2024-01-01T10:00:00Z',
          createdAt: '2024-01-01T00:00:00Z',
        },
      },
      timestamp: '2024-01-01T10:00:00Z',
    })

    // Mock user organizations
    await mockJsonRoute(page, '/api/v1/organizations/user', {
      success: true,
      data: {
        items: [
          {
            ...createTestOrganization({
              id: organizationId,
              name: 'Test Organization',
            }),
            plan: 'PRO',
            billingEmail: 'billing@example.com',
            maxUsers: 50,
            currentUsers: 1,
          },
        ],
      },
      timestamp: '2024-01-01T10:00:00Z',
    })

    // Mock payment statistics
    await mockJsonRoute(page, `/api/v1/organizations/${organizationId}/payments/statistics`, {
      totalSuccessfulPayments: 15,
      totalAmount: 3750.00,
      recentAmount: 1250.00,
    })

    // Mock payments list
    await mockJsonRoute(page, `/api/v1/organizations/${organizationId}/payments`, [
      {
        id: 'payment-1',
        amount: 299.00,
        currency: 'usd',
        status: 'SUCCEEDED',
        description: 'Pro Plan Subscription',
        stripePaymentIntentId: 'pi_1234567890abcdef1234567890',
        createdAt: '2024-01-15T10:30:00Z',
      },
      {
        id: 'payment-2',
        amount: 99.00,
        currency: 'usd',
        status: 'SUCCEEDED',
        description: 'Basic Plan Subscription',
        stripePaymentIntentId: 'pi_0987654321fedcba0987654321',
        createdAt: '2024-01-01T09:15:00Z',
      },
      {
        id: 'payment-3',
        amount: 199.00,
        currency: 'usd',
        status: 'FAILED',
        description: 'Standard Plan Subscription',
        stripePaymentIntentId: 'pi_failed123456789012345678901234',
        createdAt: '2023-12-28T14:20:00Z',
      },
    ])

    // Mock payment methods
    await mockJsonRoute(page, `/api/v1/organizations/${organizationId}/payment-methods`, [
      {
        id: 'pm-123',
        displayName: 'Visa ending in 4242',
        isDefault: true,
        cardDetails: {
          brand: 'visa',
          lastFour: '4242',
          expMonth: 12,
          expYear: 2025,
        },
        billingDetails: {
          email: 'test@example.com',
        },
      },
      {
        id: 'pm-456',
        displayName: 'Mastercard ending in 5555',
        isDefault: false,
        cardDetails: {
          brand: 'mastercard',
          lastFour: '5555',
          expMonth: 8,
          expYear: 2026,
        },
        billingDetails: {
          email: 'test@example.com',
        },
      },
    ])
  })

  test('should display payments page', async ({ page }) => {
    await navigateWithAuth(page, '/payments')

    // Check page header
    await expect(page.getByRole('heading', { name: 'Payments' })).toBeVisible()
    await expect(page.getByText('View your payment history and manage payment methods.')).toBeVisible()

    // Check payment methods button
    await expect(page.getByRole('button', { name: 'Payment Methods' })).toBeVisible()
  })

  test('should display payment statistics', async ({ page }) => {
    await navigateWithAuth(page, '/payments')

    // Check statistics cards
    await expect(page.getByText('Total Payments')).toBeVisible()
    await expect(page.getByText('15')).toBeVisible()

    await expect(page.getByText('Total Amount')).toBeVisible()
    await expect(page.getByText('$3,750.00')).toBeVisible()

    await expect(page.getByText('Recent (30 days)')).toBeVisible()
    await expect(page.getByText('$1,250.00')).toBeVisible()
  })

  test('should display payment history', async ({ page }) => {
    await navigateWithAuth(page, '/payments')

    // Check payment history section
    await expect(page.getByText('Payment History')).toBeVisible()

    // Check table headers
    await expect(page.getByText('Date')).toBeVisible()
    await expect(page.getByText('Description')).toBeVisible()
    await expect(page.getByText('Amount')).toBeVisible()
    await expect(page.getByText('Status')).toBeVisible()
    await expect(page.getByText('Payment ID')).toBeVisible()

    // Check payment entries
    await expect(page.getByText('Pro Plan Subscription')).toBeVisible()
    await expect(page.getByText('$299.00 USD')).toBeVisible()
    await expect(page.getByText('SUCCEEDED')).toBeVisible()

    await expect(page.getByText('Basic Plan Subscription')).toBeVisible()
    await expect(page.getByText('$99.00 USD')).toBeVisible()

    await expect(page.getByText('Standard Plan Subscription')).toBeVisible()
    await expect(page.getByText('$199.00 USD')).toBeVisible()
    await expect(page.getByText('FAILED')).toBeVisible()
  })

  test('should open payment methods modal', async ({ page }) => {
    await navigateWithAuth(page, '/payments')

    // Click payment methods button
    await page.getByRole('button', { name: 'Payment Methods' }).click()

    // Should open modal
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText('Payment Methods')).toBeVisible()
    await expect(page.getByText('Manage your payment methods for subscriptions and purchases.')).toBeVisible()
  })

  test('should display payment methods in modal', async ({ page }) => {
    await navigateWithAuth(page, '/payments')

    // Open payment methods modal
    await page.getByRole('button', { name: 'Payment Methods' }).click()

    // Check payment methods are displayed
    await expect(page.getByText('Visa ending in 4242')).toBeVisible()
    await expect(page.getByText('visa •••• 4242')).toBeVisible()
    await expect(page.getByText('(Expires 12/2025)')).toBeVisible()

    await expect(page.getByText('Mastercard ending in 5555')).toBeVisible()
    await expect(page.getByText('mastercard •••• 5555')).toBeVisible()
    await expect(page.getByText('(Expires 8/2026)')).toBeVisible()

    // Check default badge
    await expect(page.getByText('Default')).toBeVisible()

    // Check set as default button for non-default card
    await expect(page.getByRole('button', { name: 'Set as default' })).toBeVisible()
  })

  test('should set default payment method', async ({ page }) => {
    // Mock set default payment method API
    await page.route(
      '/api/v1/organizations/987fcdeb-51d2-43a1-b456-426614174000/payment-methods/pm-456/set-default',
      async (route) => {
        await fulfillJson(route, { success: true })
      }
    )

    await navigateWithAuth(page, '/payments')

    // Open payment methods modal
    await page.getByRole('button', { name: 'Payment Methods' }).click()

    // Click set as default for second payment method
    await page.getByRole('button', { name: 'Set as default' }).click()

    // Should show success message (we would need to check for toast notification)
    // In a real test, we would also verify the UI updates to show the new default
  })

  test('should remove payment method', async ({ page }) => {
    // Mock remove payment method API
    await page.route(
      '/api/v1/organizations/987fcdeb-51d2-43a1-b456-426614174000/payment-methods/pm-456/detach',
      async (route) => {
        await fulfillJson(route, { success: true })
      }
    )

    await navigateWithAuth(page, '/payments')

    // Open payment methods modal
    await page.getByRole('button', { name: 'Payment Methods' }).click()

    // Mock window.confirm
    await page.evaluate(() => {
      window.confirm = () => true
    })

    // Click remove button for non-default payment method
    await page.getByRole('button', { name: 'Remove' }).click()

    // Should show success message and remove the payment method from the list
  })

  test('should handle add payment method', async ({ page }) => {
    await navigateWithAuth(page, '/payments')

    // Open payment methods modal
    await page.getByRole('button', { name: 'Payment Methods' }).click()

    // Click add payment method button
    await page.getByRole('button', { name: 'Add Payment Method' }).click()

    // Should show info message about Stripe integration
    // In a real implementation, this would open Stripe's payment method setup
  })

  test('should handle empty payment methods state', async ({ page }) => {
    // Mock empty payment methods response
    await page.route('/api/v1/organizations/987fcdeb-51d2-43a1-b456-426614174000/payment-methods', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    })

    await navigateWithAuth(page, '/payments')

    // Open payment methods modal
    await page.getByRole('button', { name: 'Payment Methods' }).click()

    // Should show empty state
    await expect(page.getByText('No payment methods')).toBeVisible()
    await expect(page.getByText('Add a payment method to start making payments.')).toBeVisible()
  })

  test('should handle empty payments state', async ({ page }) => {
    // Mock empty payments response
    await page.route('/api/v1/organizations/987fcdeb-51d2-43a1-b456-426614174000/payments', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    })

    await navigateWithAuth(page, '/payments')

    // Should show empty state
    await expect(page.getByText('No payments yet')).toBeVisible()
    await expect(page.getByText('Your payment history will appear here once you make a payment.')).toBeVisible()
  })

  test('should close payment methods modal', async ({ page }) => {
    await navigateWithAuth(page, '/payments')

    // Open payment methods modal
    await page.getByRole('button', { name: 'Payment Methods' }).click()

    // Modal should be visible
    await expect(page.getByRole('dialog')).toBeVisible()

    // Close modal by clicking X button
    await page.getByRole('button', { name: 'Close' }).click()

    // Modal should be hidden
    await expect(page.getByRole('dialog')).not.toBeVisible()
  })
})
