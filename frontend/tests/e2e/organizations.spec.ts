import { test, expect } from './fixtures'

test.describe('Organizations', () => {
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
            memberCount: 3,
            createdAt: '2024-01-01T00:00:00Z',
            settings: {
              description: 'A test organization for development',
            },
          },
          {
            id: 'org-456',
            name: 'Another Organization',
            slug: 'another-org',
            status: 'ACTIVE',
            userRole: 'ADMIN',
            memberCount: 8,
            createdAt: '2024-02-01T00:00:00Z',
          },
        ]),
      })
    })
  })

  test('should display organizations list', async ({ page }) => {
    await page.goto('/organizations')

    // Check page header
    await expect(page.getByRole('heading', { name: 'Organizations' })).toBeVisible()
    await expect(page.getByText('Manage your organizations and team memberships.')).toBeVisible()

    // Check organizations are listed
    await expect(page.getByText('Test Organization')).toBeVisible()
    await expect(page.getByText('Another Organization')).toBeVisible()

    // Check organization details
    await expect(page.getByText('/test-org')).toBeVisible()
    await expect(page.getByText('/another-org')).toBeVisible()
    await expect(page.getByText('3 members')).toBeVisible()
    await expect(page.getByText('8 members')).toBeVisible()

    // Check role badges
    await expect(page.getByText('OWNER')).toBeVisible()
    await expect(page.getByText('ADMIN')).toBeVisible()
  })

  test('should navigate to organization detail page', async ({ page }) => {
    // Mock organization members
    await page.route('/api/v1/organizations/org-123/members', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'member-1',
            email: 'test@example.com',
            role: 'OWNER',
            status: 'ACTIVE',
            user: {
              id: 'user-123',
              name: 'Test User',
            },
            joinedAt: '2024-01-01T00:00:00Z',
          },
          {
            id: 'member-2',
            email: 'member@example.com',
            role: 'MEMBER',
            status: 'ACTIVE',
            user: {
              id: 'user-456',
              name: 'Team Member',
            },
            joinedAt: '2024-01-15T00:00:00Z',
          },
        ]),
      })
    })

    await page.goto('/organizations')

    // Click on organization link
    await page.getByRole('link', { name: /test organization/i }).click()

    // Should navigate to organization detail page
    await expect(page).toHaveURL('/organizations/test-org')

    // Should display organization details
    await expect(page.getByRole('heading', { name: 'Test Organization' })).toBeVisible()
    await expect(page.getByText('Organization settings and member management')).toBeVisible()
  })

  test('should display organization members', async ({ page }) => {
    // Mock organization members
    await page.route('/api/v1/organizations/org-123/members', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'member-1',
            email: 'test@example.com',
            role: 'OWNER',
            status: 'ACTIVE',
            user: {
              id: 'user-123',
              name: 'Test User',
            },
            joinedAt: '2024-01-01T00:00:00Z',
          },
          {
            id: 'member-2',
            email: 'member@example.com',
            role: 'MEMBER',
            status: 'PENDING',
            invitedAt: '2024-01-20T00:00:00Z',
          },
        ]),
      })
    })

    await page.goto('/organizations/test-org')

    // Should display members section
    await expect(page.getByText('Members')).toBeVisible()
    await expect(page.getByText('People who have access to this organization')).toBeVisible()

    // Should display member list
    await expect(page.getByText('Test User')).toBeVisible()
    await expect(page.getByText('test@example.com')).toBeVisible()
    await expect(page.getByText('member@example.com')).toBeVisible()

    // Should display member roles and status
    await expect(page.getByText('OWNER')).toBeVisible()
    await expect(page.getByText('MEMBER')).toBeVisible()
    await expect(page.getByText('ACTIVE')).toBeVisible()
    await expect(page.getByText('PENDING')).toBeVisible()
  })

  test('should open create organization modal', async ({ page }) => {
    await page.goto('/organizations')

    // Click new organization button
    await page.getByRole('button', { name: 'New Organization' }).click()

    // Should open modal
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText('Create New Organization')).toBeVisible()
    await expect(page.getByText('Create a new organization to manage your team and subscriptions.')).toBeVisible()

    // Should have form fields
    await expect(page.getByRole('textbox', { name: 'Organization Name' })).toBeVisible()
    await expect(page.getByRole('textbox', { name: 'URL Slug' })).toBeVisible()
    await expect(page.getByRole('textbox', { name: 'Description (Optional)' })).toBeVisible()
  })

  test('should create new organization', async ({ page }) => {
    // Mock create organization API
    await page.route('/api/v1/organizations', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'org-new',
            name: 'New Organization',
            slug: 'new-organization',
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

    await page.goto('/organizations')

    // Open create modal
    await page.getByRole('button', { name: 'New Organization' }).click()

    // Fill form
    await page.getByRole('textbox', { name: 'Organization Name' }).fill('New Organization')
    await page.getByRole('textbox', { name: 'Description (Optional)' }).fill('A brand new organization')

    // Submit form
    await page.getByRole('button', { name: 'Create Organization' }).click()

    // Should close modal and show success message
    await expect(page.getByRole('dialog')).not.toBeVisible()

    // Note: In a real test, we would mock the organizations list refetch
    // and verify the new organization appears in the list
  })

  test('should open invite member form', async ({ page }) => {
    await page.goto('/organizations/test-org')

    // Click invite member button
    await page.getByRole('button', { name: 'Invite Member' }).click()

    // Should show invite form
    await expect(page.getByText('Invite New Member')).toBeVisible()
    await expect(page.getByText('Send an invitation to join this organization.')).toBeVisible()

    // Should have form fields
    await expect(page.getByRole('textbox', { name: 'Email address' })).toBeVisible()
    await expect(page.getByRole('combobox', { name: 'Role' })).toBeVisible()
  })

  test('should invite new member', async ({ page }) => {
    // Mock invite member API
    await page.route('/api/v1/organizations/org-123/members/invite', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'invitation-123',
          email: 'newmember@example.com',
          role: 'MEMBER',
          status: 'PENDING',
        }),
      })
    })

    await page.goto('/organizations/test-org')

    // Open invite form
    await page.getByRole('button', { name: 'Invite Member' }).click()

    // Fill form
    await page.getByRole('textbox', { name: 'Email address' }).fill('newmember@example.com')
    await page.getByRole('combobox', { name: 'Role' }).selectOption('MEMBER')

    // Submit form
    await page.getByRole('button', { name: 'Send Invitation' }).click()

    // Should hide form and show success message
    await expect(page.getByText('Invite New Member')).not.toBeVisible()
  })

  test('should handle empty organizations state', async ({ page }) => {
    // Mock empty organizations response
    await page.route('/api/v1/organizations/user', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      })
    })

    await page.goto('/organizations')

    // Should show empty state
    await expect(page.getByText('No organizations')).toBeVisible()
    await expect(page.getByText('Get started by creating your first organization.')).toBeVisible()
    await expect(page.getByRole('button', { name: 'New Organization' })).toBeVisible()
  })
})
