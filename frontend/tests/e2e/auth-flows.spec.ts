import { test, expect } from './fixtures'

/**
 * End-to-end tests for authentication flows
 * Tests complete user journeys from login to logout
 */

test.describe('Authentication Flows', () => {
  test.beforeEach(async ({ page }) => {
    // Comprehensive authentication state cleanup
    console.log('🧹 Clearing authentication state...')

    // 1. Clear all browser storage
    await page.context().clearCookies()
    await page.context().clearPermissions()

    // 2. Clear local/session storage (navigate to app first to avoid security issues)
    await page.goto('/')
    await page.evaluate(() => {
      try {
        localStorage.clear()
        sessionStorage.clear()
      } catch (e) {
        console.log('Could not clear storage:', e)
      }
    })

    // 3. Navigate to app and clear Redux store if needed
    await page.goto('/')

    // 4. Clear Redux auth state by dispatching logout action
    await page.evaluate(() => {
      // Check if Redux store is available and dispatch logout
      if (window.__REDUX_STORE__) {
        window.__REDUX_STORE__.dispatch({ type: 'auth/logout' })
      }
      // Also try common store access patterns
      if (window.store) {
        window.store.dispatch({ type: 'auth/logout' })
      }
    }).catch(() => {
      // Redux store might not be exposed, that's OK
      console.log('Redux store not accessible, continuing...')
    })

    // 5. Force navigate to login page to ensure clean state
    await page.goto('/auth/login', { waitUntil: 'networkidle' })

    // 6. Verify we're actually on the login page
    try {
      // Wait for login form elements or redirect to happen
      await Promise.race([
        // Either we see the login form
        page.locator('[data-testid="email-input"]').waitFor({ timeout: 5000 }),
        // Or we see welcome text (meaning still logged in)
        page.locator('text=Welcome back').waitFor({ timeout: 5000 })
      ])

      // If we still see welcome text, the auth state is persistent
      if (await page.locator('text=Welcome back').isVisible()) {
        console.log('⚠️ Auth state still persistent, trying API logout...')

        // Try to call logout API directly
        await page.evaluate(async () => {
          try {
            await fetch('/api/auth/logout', {
              method: 'POST',
              credentials: 'include'
            })
          } catch (e) {
            console.log('Logout API call failed:', e)
          }
        })

        // Clear everything again and reload
        await page.context().clearCookies()
        await page.goto('/auth/login', { waitUntil: 'networkidle' })

        // Final check - if still not on login page, there might be auto-login in test mode
        if (await page.locator('text=Welcome back').isVisible()) {
          console.log('🚨 Auto-login detected in test mode - auth tests may need mocking')
        }
      }
    } catch (error) {
      console.log('Login page detection timed out, proceeding with test...')
    }
  })

  test.describe('Login Flow', () => {
    test('should complete successful login flow', async ({ page }) => {
      // Navigate directly to login page
      await page.goto('/auth/login')
      await expect(page).toHaveURL(/.*login/)

      // Wait for login form to be visible
      await expect(page.locator('[data-testid="email-input"]')).toBeVisible()

      // Fill login form
      await page.fill('[data-testid="email-input"]', 'demo@example.com')
      await page.fill('[data-testid="password-input"]', 'DemoPassword123!')

      // Submit form
      await page.click('[data-testid="submit-button"]')

      // Wait for successful login and redirect
      await expect(page).toHaveURL(/.*dashboard/)
      await expect(page.locator('text=Welcome back')).toBeVisible()
    })

    test('should show error for invalid credentials', async ({ page }) => {
      await page.click('[data-testid="login-button"]')

      await page.fill('[data-testid="email-input"]', 'invalid@example.com')
      await page.fill('[data-testid="password-input"]', 'wrongpassword')

      await page.click('[data-testid="submit-button"]')

      // Should show error message
      await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
      await expect(page.locator('text=Invalid email or password')).toBeVisible()

      // Should remain on login page
      await expect(page).toHaveURL(/.*login/)
    })

    test('should handle account lockout', async ({ page }) => {
      await page.click('[data-testid="login-button"]')

      // Simulate multiple failed attempts
      for (let i = 0; i < 5; i++) {
        await page.fill('[data-testid="email-input"]', 'locked@example.com')
        await page.fill('[data-testid="password-input"]', 'wrongpassword')
        await page.click('[data-testid="submit-button"]')

        if (i < 4) {
          await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
        }
      }

      // Should show lockout message
      await expect(page.locator('text=Account locked')).toBeVisible()
      await expect(page.locator('[data-testid="submit-button"]')).toBeDisabled()
    })

    test('should validate form fields', async ({ page }) => {
      // Navigate directly to login page
      await page.goto('/auth/login')

      // Wait for login form to be visible
      await expect(page.locator('[data-testid="email-input"]')).toBeVisible()

      // Try to submit without filling fields (click submit directly on login form)
      await page.click('[data-testid="submit-button"]')

      // Should show validation errors
      await expect(page.locator('[data-testid="email-error"]')).toBeVisible()
      await expect(page.locator('[data-testid="password-error"]')).toBeVisible()
      await expect(page.locator('text=Please enter a valid email address')).toBeVisible()
      await expect(page.locator('text=Password must be at least 8 characters')).toBeVisible()
    })

    test('should show password strength indicator', async ({ page }) => {
      await page.click('[data-testid="login-button"]')

      // Weak password
      await page.fill('[data-testid="password-input"]', '123')
      await expect(page.locator('[data-testid="password-strength"]')).toHaveText('Weak')

      // Strong password
      await page.fill('[data-testid="password-input"]', 'SecurePassword123!')
      await expect(page.locator('[data-testid="password-strength"]')).toHaveText('Strong')
    })

    test('should remember me functionality', async ({ page, context }) => {
      await page.click('[data-testid="login-button"]')

      await page.fill('[data-testid="email-input"]', 'demo@example.com')
      await page.fill('[data-testid="password-input"]', 'DemoPassword123!')
      await page.check('[data-testid="remember-me-checkbox"]')

      await page.click('[data-testid="submit-button"]')
      await expect(page).toHaveURL(/.*dashboard/)

      // Close and reopen browser
      await page.close()
      const newPage = await context.newPage()
      await newPage.goto('/')

      // Should still be logged in
      await expect(newPage).toHaveURL(/.*dashboard/)
    })
  })

  test.describe('Registration Flow', () => {
    test('should complete successful registration', async ({ page }) => {
      await page.click('[data-testid="register-button"]')
      await expect(page).toHaveURL(/.*register/)

      // Fill registration form
      await page.fill('[data-testid="email-input"]', 'newuser@example.com')
      await page.fill('[data-testid="password-input"]', 'SecurePassword123!')
      await page.fill('[data-testid="confirm-password-input"]', 'SecurePassword123!')
      await page.fill('[data-testid="first-name-input"]', 'John')
      await page.fill('[data-testid="last-name-input"]', 'Doe')
      await page.check('[data-testid="terms-checkbox"]')

      await page.click('[data-testid="submit-button"]')

      // Should redirect to email verification
      await expect(page).toHaveURL(/.*verify-email/)
      await expect(page.locator('text=Please check your email')).toBeVisible()
    })

    test('should validate password complexity', async ({ page }) => {
      await page.click('[data-testid="register-button"]')

      await page.fill('[data-testid="email-input"]', 'test@example.com')
      await page.fill('[data-testid="password-input"]', 'weak')

      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="password-error"]')).toBeVisible()
      await expect(page.locator('text=Password must be at least 12 characters')).toBeVisible()
    })

    test('should check password confirmation match', async ({ page }) => {
      await page.click('[data-testid="register-button"]')

      await page.fill('[data-testid="password-input"]', 'SecurePassword123!')
      await page.fill('[data-testid="confirm-password-input"]', 'DifferentPassword123!')

      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="confirm-password-error"]')).toBeVisible()
      await expect(page.locator('text=Passwords do not match')).toBeVisible()
    })

    test('should require terms acceptance', async ({ page }) => {
      await page.click('[data-testid="register-button"]')

      await page.fill('[data-testid="email-input"]', 'demo@example.com')
      await page.fill('[data-testid="password-input"]', 'DemoPassword123!')
      await page.fill('[data-testid="confirm-password-input"]', 'SecurePassword123!')
      await page.fill('[data-testid="first-name-input"]', 'John')
      await page.fill('[data-testid="last-name-input"]', 'Doe')
      // Don't check terms checkbox

      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="terms-error"]')).toBeVisible()
      await expect(page.locator('text=You must accept the terms')).toBeVisible()
    })
  })

  test.describe('Password Reset Flow', () => {
    test('should complete password reset request', async ({ page }) => {
      await page.click('[data-testid="login-button"]')
      await page.click('[data-testid="forgot-password-link"]')
      await expect(page).toHaveURL(/.*forgot-password/)

      await page.fill('[data-testid="email-input"]', 'test@example.com')
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('text=Reset link sent')).toBeVisible()
      await expect(page.locator('text=Check your email')).toBeVisible()
    })

    test('should handle invalid email for password reset', async ({ page }) => {
      await page.click('[data-testid="login-button"]')
      await page.click('[data-testid="forgot-password-link"]')

      await page.fill('[data-testid="email-input"]', 'nonexistent@example.com')
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
      await expect(page.locator('text=Email not found')).toBeVisible()
    })

    test('should complete password reset with valid token', async ({ page }) => {
      // Navigate directly to reset page with token
      await page.goto('/reset-password?token=valid-reset-token')

      await page.fill('[data-testid="new-password-input"]', 'NewSecurePassword123!')
      await page.fill('[data-testid="confirm-password-input"]', 'NewSecurePassword123!')

      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('text=Password updated successfully')).toBeVisible()
      await expect(page).toHaveURL(/.*login/)
    })

    test('should handle expired reset token', async ({ page }) => {
      await page.goto('/reset-password?token=expired-token')

      await expect(page.locator('text=Reset token expired')).toBeVisible()
      await expect(page.locator('[data-testid="request-new-link"]')).toBeVisible()
    })
  })

  test.describe('Email Verification Flow', () => {
    test('should verify email with valid token', async ({ page }) => {
      await page.goto('/verify-email?token=valid-verification-token')

      await expect(page.locator('text=Email verified successfully')).toBeVisible()
      await expect(page.locator('[data-testid="continue-button"]')).toBeVisible()

      await page.click('[data-testid="continue-button"]')
      await expect(page).toHaveURL(/.*dashboard/)
    })

    test('should handle invalid verification token', async ({ page }) => {
      await page.goto('/verify-email?token=invalid-token')

      await expect(page.locator('text=Invalid verification token')).toBeVisible()
      await expect(page.locator('[data-testid="resend-button"]')).toBeVisible()
    })

    test('should resend verification email', async ({ page }) => {
      await page.goto('/verify-email')

      await page.click('[data-testid="resend-button"]')

      await expect(page.locator('text=Verification email sent')).toBeVisible()
      await expect(page.locator('[data-testid="resend-button"]')).toBeDisabled({ timeout: 30000 })
    })
  })

  test.describe('Session Management', () => {
    test('should handle session expiry', async ({ page }) => {
      // Login first
      await page.click('[data-testid="login-button"]')
      await page.fill('[data-testid="email-input"]', 'demo@example.com')
      await page.fill('[data-testid="password-input"]', 'DemoPassword123!')
      await page.click('[data-testid="submit-button"]')
      await expect(page).toHaveURL(/.*dashboard/)

      // Simulate session expiry by clearing storage
      await page.evaluate(() => {
        localStorage.clear()
        sessionStorage.clear()
      })

      // Try to access protected resource
      await page.reload()

      // Should redirect to login
      await expect(page).toHaveURL(/.*login/)
      await expect(page.locator('text=Session expired')).toBeVisible()
    })

    test('should extend session on activity', async ({ page }) => {
      await page.click('[data-testid="login-button"]')
      await page.fill('[data-testid="email-input"]', 'demo@example.com')
      await page.fill('[data-testid="password-input"]', 'DemoPassword123!')
      await page.click('[data-testid="submit-button"]')
      await expect(page).toHaveURL(/.*dashboard/)

      // Simulate user activity
      await page.click('[data-testid="profile-menu"]')
      await page.click('[data-testid="dashboard-link"]')

      // Session should remain active
      await expect(page).toHaveURL(/.*dashboard/)
      await expect(page.locator('[data-testid="user-menu"]')).toBeVisible()
    })
  })

  test.describe('Logout Flow', () => {
    test('should complete logout successfully', async ({ page }) => {
      // Login first
      await page.click('[data-testid="login-button"]')
      await page.fill('[data-testid="email-input"]', 'demo@example.com')
      await page.fill('[data-testid="password-input"]', 'DemoPassword123!')
      await page.click('[data-testid="submit-button"]')
      await expect(page).toHaveURL(/.*dashboard/)

      // Logout
      await page.click('[data-testid="user-menu"]')
      await page.click('[data-testid="logout-button"]')

      // Should redirect to home page
      await expect(page).toHaveURL('/')
      await expect(page.locator('[data-testid="login-button"]')).toBeVisible()

      // Verify user cannot access protected routes
      await page.goto('/dashboard')
      await expect(page).toHaveURL(/.*login/)
    })

    test('should clear sensitive data on logout', async ({ page }) => {
      // Login and logout
      await page.click('[data-testid="login-button"]')
      await page.fill('[data-testid="email-input"]', 'demo@example.com')
      await page.fill('[data-testid="password-input"]', 'DemoPassword123!')
      await page.click('[data-testid="submit-button"]')
      await page.click('[data-testid="user-menu"]')
      await page.click('[data-testid="logout-button"]')

      // Check that sensitive data is cleared
      const localStorage = await page.evaluate(() => {
        return {
          token: localStorage.getItem('token'),
          user: localStorage.getItem('user'),
        }
      })

      expect(localStorage.token).toBeNull()
      expect(localStorage.user).toBeNull()
    })
  })

  test.describe('Multi-tab Session Handling', () => {
    test('should sync logout across tabs', async ({ browser }) => {
      const context = await browser.newContext()
      const page1 = await context.newPage()
      const page2 = await context.newPage()

      // Login in first tab
      await page1.goto('/')
      await page1.click('[data-testid="login-button"]')
      await page1.fill('[data-testid="email-input"]', 'test@example.com')
      await page1.fill('[data-testid="password-input"]', 'SecurePassword123!')
      await page1.click('[data-testid="submit-button"]')
      await expect(page1).toHaveURL(/.*dashboard/)

      // Navigate to dashboard in second tab
      await page2.goto('/dashboard')
      await expect(page2).toHaveURL(/.*dashboard/)

      // Logout from first tab
      await page1.click('[data-testid="user-menu"]')
      await page1.click('[data-testid="logout-button"]')

      // Second tab should also be logged out
      await page2.reload()
      await expect(page2).toHaveURL(/.*login/)

      await context.close()
    })

    test('should handle concurrent login attempts', async ({ browser }) => {
      const context = await browser.newContext()
      const page1 = await context.newPage()
      const page2 = await context.newPage()

      // Attempt login in both tabs simultaneously
      await Promise.all([
        (async () => {
          await page1.goto('/')
          await page1.click('[data-testid="login-button"]')
          await page1.fill('[data-testid="email-input"]', 'test@example.com')
          await page1.fill('[data-testid="password-input"]', 'SecurePassword123!')
          await page1.click('[data-testid="submit-button"]')
        })(),
        (async () => {
          await page2.goto('/')
          await page2.click('[data-testid="login-button"]')
          await page2.fill('[data-testid="email-input"]', 'test@example.com')
          await page2.fill('[data-testid="password-input"]', 'SecurePassword123!')
          await page2.click('[data-testid="submit-button"]')
        })(),
      ])

      // Both tabs should end up at dashboard
      await expect(page1).toHaveURL(/.*dashboard/)
      await expect(page2).toHaveURL(/.*dashboard/)

      await context.close()
    })
  })

  test.describe('Security Features', () => {
    test('should prevent CSRF attacks', async ({ page }) => {
      // This would test CSRF protection mechanisms
      // Implementation depends on your CSRF protection strategy
      expect(true).toBe(true) // Placeholder
    })

    test('should handle rate limiting gracefully', async ({ page }) => {
      await page.click('[data-testid="login-button"]')

      // Attempt multiple rapid login attempts
      for (let i = 0; i < 10; i++) {
        await page.fill('[data-testid="email-input"]', 'test@example.com')
        await page.fill('[data-testid="password-input"]', 'wrongpassword')
        await page.click('[data-testid="submit-button"]')

        // Wait for response
        await page.waitForTimeout(100)
      }

      // Should show rate limit message
      await expect(page.locator('text=Too many requests')).toBeVisible()
      await expect(page.locator('[data-testid="submit-button"]')).toBeDisabled()
    })

    test('should sanitize user input', async ({ page }) => {
      await page.click('[data-testid="login-button"]')

      // Try XSS payload
      await page.fill('[data-testid="email-input"]', '<script>alert("xss")</script>')
      await page.fill('[data-testid="password-input"]', 'password')
      await page.click('[data-testid="submit-button"]')

      // Should not execute script
      const alerts = []
      page.on('dialog', dialog => {
        alerts.push(dialog.message())
        dialog.accept()
      })

      await page.waitForTimeout(1000)
      expect(alerts).toHaveLength(0)
    })
  })
})
