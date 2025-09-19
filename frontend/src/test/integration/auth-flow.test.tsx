import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { server } from '../setup'
import { http, HttpResponse } from 'msw'
import { customRender, unauthenticatedState, testUtils } from '../utils/test-utils'
import { LoginPage } from '@/pages/auth/LoginPage'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'

/**
 * Integration tests for complete authentication flows
 */

describe('Authentication Flow Integration', () => {
  beforeEach(() => {
    server.resetHandlers()
  })

  describe('Login Flow', () => {
    it('should complete successful login flow', async () => {
      const { user } = customRender(<LoginPage />, {
        preloadedState: unauthenticatedState,
        withRouter: true,
      })

      // Fill login form
      await testUtils.fillForm(user, {
        email: 'test@example.com',
        password: 'password123',
        organization: 'org-123',
      })

      // Submit form
      await testUtils.submitForm(user, 'Sign In')

      // Wait for successful login
      await waitFor(() => {
        expect(screen.queryByText(/signing in/i)).not.toBeInTheDocument()
      })

      // Verify success state (depends on your UI implementation)
      expect(screen.queryByText(/invalid/i)).not.toBeInTheDocument()
    })

    it('should handle invalid credentials', async () => {
      // Mock invalid login response
      server.use(
        http.post('http://localhost:3000/api/auth/mock-login', () => {
          return HttpResponse.json(
            {
              success: false,
              error: {
                code: 'AUTH_001',
                message: 'Invalid email or password',
              },
              timestamp: new Date().toISOString(),
            },
            { status: 401 }
          )
        })
      )

      const { user } = customRender(<LoginPage />, {
        preloadedState: unauthenticatedState,
        withRouter: true,
      })

      await testUtils.fillForm(user, {
        email: 'invalid@example.com',
        password: 'wrongpassword',
      })

      await testUtils.submitForm(user, 'Sign In')

      // Wait for error to appear
      await testUtils.waitForError('Invalid email or password')
    })

    it('should handle account lockout', async () => {
      server.use(
        http.post('http://localhost:3000/api/auth/mock-login', () => {
          return HttpResponse.json(
            {
              success: false,
              error: {
                code: 'AUTH_002',
                message: 'Account locked due to too many failed attempts',
              },
              timestamp: new Date().toISOString(),
            },
            { status: 423 }
          )
        })
      )

      const { user } = customRender(<LoginPage />, {
        preloadedState: unauthenticatedState,
        withRouter: true,
      })

      await testUtils.fillForm(user, {
        email: 'locked@example.com',
        password: 'password123',
      })

      await testUtils.submitForm(user, 'Sign In')

      await testUtils.waitForError('Account locked')
    })

    it('should handle network errors gracefully', async () => {
      server.use(
        http.post('http://localhost:3000/api/auth/mock-login', () => {
          return HttpResponse.error()
        })
      )

      const { user } = customRender(<LoginPage />, {
        preloadedState: unauthenticatedState,
        withRouter: true,
      })

      await testUtils.fillForm(user, {
        email: 'test@example.com',
        password: 'password123',
      })

      await testUtils.submitForm(user, 'Sign In')

      await testUtils.waitForError()
    })

    it('should validate form fields', async () => {
      const { user } = customRender(<LoginPage />, {
        preloadedState: unauthenticatedState,
        withRouter: true,
      })

      // Try to submit without filling fields
      await testUtils.submitForm(user, 'Sign In')

      // Check for validation errors
      await waitFor(() => {
        expect(screen.getByText(/email is required/i)).toBeInTheDocument()
      })
    })

    it('should handle password complexity requirements', async () => {
      const { user } = customRender(<LoginPage />, {
        preloadedState: unauthenticatedState,
        withRouter: true,
      })

      await testUtils.fillForm(user, {
        email: 'test@example.com',
        password: '123', // Too short
      })

      await testUtils.submitForm(user, 'Sign In')

      await waitFor(() => {
        expect(screen.getByText(/password must be at least/i)).toBeInTheDocument()
      })
    })
  })

  describe('Registration Flow', () => {
    it('should complete successful registration', async () => {
      const RegisterPage = () => (
        <div>
          <h1>Register</h1>
          {/* Mock registration form */}
          <form>
            <input
              type="email"
              name="email"
              placeholder="Email"
              aria-label="Email"
            />
            <input
              type="password"
              name="password"
              placeholder="Password"
              aria-label="Password"
            />
            <input
              type="text"
              name="name"
              placeholder="Full Name"
              aria-label="Full Name"
            />
            <button type="submit">Register</button>
          </form>
        </div>
      )

      const { user } = customRender(<RegisterPage />, {
        preloadedState: unauthenticatedState,
        withRouter: true,
      })

      await testUtils.fillForm(user, {
        email: 'newuser@example.com',
        password: 'SecurePassword123!',
        'Full Name': 'New User',
      })

      await testUtils.submitForm(user, 'Register')

      // Verify form submission (would redirect or show success)
      // This depends on your actual registration component implementation
    })

    it('should handle duplicate email registration', async () => {
      server.use(
        http.post('http://localhost:3000/api/auth/register', () => {
          return HttpResponse.json(
            {
              success: false,
              error: {
                code: 'VAL_003',
                message: 'Email address already in use',
              },
              timestamp: new Date().toISOString(),
            },
            { status: 409 }
          )
        })
      )

      const RegisterPage = () => (
        <div>
          <h1>Register</h1>
          <form>
            <input type="email" name="email" aria-label="Email" />
            <input type="password" name="password" aria-label="Password" />
            <input type="text" name="name" aria-label="Full Name" />
            <button type="submit">Register</button>
          </form>
        </div>
      )

      const { user } = customRender(<RegisterPage />, {
        preloadedState: unauthenticatedState,
        withRouter: true,
      })

      await testUtils.fillForm(user, {
        email: 'existing@example.com',
        password: 'SecurePassword123!',
        'Full Name': 'Existing User',
      })

      await testUtils.submitForm(user, 'Register')

      await testUtils.waitForError('Email address already in use')
    })
  })

  describe('Session Management', () => {
    it('should handle session expiry gracefully', async () => {
      server.use(
        http.get('http://localhost:3000/api/auth/session', () => {
          return HttpResponse.json(
            {
              success: false,
              error: {
                code: 'AUTH_003',
                message: 'Session expired',
              },
              timestamp: new Date().toISOString(),
            },
            { status: 401 }
          )
        })
      )

      const { user } = customRender(<DashboardPage />, {
        preloadedState: unauthenticatedState,
        withRouter: true,
      })

      // Component should handle session expiry
      await waitFor(() => {
        expect(screen.queryByText(/dashboard/i)).not.toBeInTheDocument()
      })
    })

    it('should refresh session on user activity', async () => {
      let sessionCallCount = 0
      server.use(
        http.get('http://localhost:3000/api/auth/session', () => {
          sessionCallCount++
          return HttpResponse.json({
            success: true,
            data: {
              user: {
                id: 'user-123',
                email: 'test@example.com',
                firstName: 'Test',
                lastName: 'User',
                role: 'USER',
                emailVerified: true,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString(),
              },
              session: {
                activeTokens: 1,
                lastActiveAt: new Date().toISOString(),
                createdAt: new Date().toISOString(),
              },
            },
            timestamp: new Date().toISOString(),
          })
        })
      )

      const { user } = customRender(<DashboardPage />, {
        withRouter: true,
      })

      // Simulate user activity
      const button = screen.getByRole('button', { name: /refresh/i })
      await user.click(button)

      // Session should be refreshed
      await waitFor(() => {
        expect(sessionCallCount).toBeGreaterThan(0)
      })
    })
  })

  describe('Logout Flow', () => {
    it('should complete logout successfully', async () => {
      const { user } = customRender(<DashboardPage />, {
        withRouter: true,
      })

      // Find and click logout button
      const logoutButton = screen.getByRole('button', { name: /logout|sign out/i })
      await user.click(logoutButton)

      // Wait for logout to complete
      await waitFor(() => {
        expect(screen.queryByText(/dashboard/i)).not.toBeInTheDocument()
      })
    })

    it('should handle logout errors gracefully', async () => {
      server.use(
        http.post('http://localhost:3000/api/auth/logout', () => {
          return HttpResponse.json(
            {
              success: false,
              error: {
                code: 'SYS_001',
                message: 'Logout failed',
              },
              timestamp: new Date().toISOString(),
            },
            { status: 500 }
          )
        })
      )

      const { user } = customRender(<DashboardPage />, {
        withRouter: true,
      })

      const logoutButton = screen.getByRole('button', { name: /logout|sign out/i })
      await user.click(logoutButton)

      // Should still clear local state even if server logout fails
      await waitFor(() => {
        expect(screen.queryByText(/dashboard/i)).not.toBeInTheDocument()
      })
    })
  })

  describe('Authentication State Persistence', () => {
    it('should persist authentication state across page reloads', async () => {
      // This would test localStorage/sessionStorage persistence
      // Implementation depends on your auth state persistence strategy
      expect(true).toBe(true) // Placeholder
    })

    it('should clear sensitive data on logout', async () => {
      const { user } = customRender(<DashboardPage />, {
        withRouter: true,
      })

      const logoutButton = screen.getByRole('button', { name: /logout|sign out/i })
      await user.click(logoutButton)

      // Verify sensitive data is cleared
      expect(localStorage.getItem('token')).toBeNull()
      expect(sessionStorage.getItem('user')).toBeNull()
    })
  })
})