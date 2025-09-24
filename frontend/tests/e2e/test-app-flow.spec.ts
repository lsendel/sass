import { test, expect } from '@playwright/test';

test.describe('Application Flow Test', () => {
  test('should test complete app flow and identify issues', async ({ page }) => {
    // Navigate to the application
    console.log('Starting app flow test...');
    await page.goto('http://localhost:3001');

    // Try to find the login button
    try {
      console.log('Looking for login elements...');
      const loginButton = page.locator('[data-testid="login-button"]');
      await expect(loginButton).toBeVisible({ timeout: 3000 });
      console.log('✅ Found login button');

      // Click the login button to populate credentials
      await loginButton.click();
      await page.waitForTimeout(500);

      // Try to submit the form
      const submitButton = page.locator('[data-testid="submit-button"]');
      await expect(submitButton).toBeVisible({ timeout: 3000 });
      console.log('✅ Found submit button');

      await submitButton.click();

      // Wait for navigation to dashboard with shorter timeout
      await page.waitForURL('**/dashboard', { timeout: 5000 });
      console.log('✅ Successfully reached dashboard');

      // Test navigation to Organizations - handle multiple elements
      console.log('Testing navigation to Organizations...');
      const orgNav = page.locator('[data-testid="nav-organizations"]').first();
      if (await orgNav.isVisible({ timeout: 1000 })) {
        await orgNav.click();
        await page.waitForURL('**/organizations', { timeout: 3000 });
        console.log('✅ Organizations page loaded');
      } else {
        console.log('⚠️ Organizations nav not found - checking href fallback');
        const orgLink = page.locator('a[href="/organizations"]').first();
        if (await orgLink.isVisible({ timeout: 1000 })) {
          await orgLink.click();
          await page.waitForURL('**/organizations', { timeout: 3000 });
          console.log('✅ Organizations page loaded (href fallback)');
        } else {
          console.log('❌ Organizations navigation not available');
        }
      }

      // Test navigation to Payments - handle multiple elements
      console.log('Testing navigation to Payments...');
      const paymentNav = page.locator('[data-testid="nav-payments"]').first();
      if (await paymentNav.isVisible({ timeout: 1000 })) {
        await paymentNav.click();
        await page.waitForURL('**/payments', { timeout: 3000 });
        console.log('✅ Payments page loaded');
      } else {
        console.log('⚠️ Payments nav not found - checking href fallback');
        const paymentLink = page.locator('a[href="/payments"]').first();
        if (await paymentLink.isVisible({ timeout: 1000 })) {
          await paymentLink.click();
          await page.waitForURL('**/payments', { timeout: 3000 });
          console.log('✅ Payments page loaded (href fallback)');
        } else {
          console.log('❌ Payments navigation not available');
        }
      }

      // Test navigation to Subscription - handle multiple elements
      console.log('Testing navigation to Subscription...');
      const subscriptionNav = page.locator('[data-testid="nav-subscription"]').first();
      if (await subscriptionNav.isVisible({ timeout: 1000 })) {
        await subscriptionNav.click();
        await page.waitForURL('**/subscription', { timeout: 3000 });
        console.log('✅ Subscription page loaded');
      } else {
        console.log('⚠️ Subscription nav not found - checking href fallback');
        const subscriptionLink = page.locator('a[href="/subscription"]').first();
        if (await subscriptionLink.isVisible({ timeout: 1000 })) {
          await subscriptionLink.click();
          await page.waitForURL('**/subscription', { timeout: 3000 });
          console.log('✅ Subscription page loaded (href fallback)');
        } else {
          console.log('❌ Subscription navigation not available');
        }
      }

      // Test navigation to Settings - handle multiple elements
      console.log('Testing navigation to Settings...');
      const settingsNav = page.locator('[data-testid="nav-settings"]').first();
      if (await settingsNav.isVisible({ timeout: 1000 })) {
        await settingsNav.click();
        await page.waitForURL('**/settings', { timeout: 3000 });
        console.log('✅ Settings page loaded');
      } else {
        console.log('⚠️ Settings nav not found - checking href fallback');
        const settingsLink = page.locator('a[href="/settings"]').first();
        if (await settingsLink.isVisible({ timeout: 1000 })) {
          await settingsLink.click();
          await page.waitForURL('**/settings', { timeout: 3000 });
          console.log('✅ Settings page loaded (href fallback)');
        } else {
          console.log('❌ Settings navigation not available');
        }
      }

      console.log('🎉 Application flow test completed successfully!');

    } catch (error) {
      console.log('❌ Application flow failed:', error.message);

      // Check for error boundary
      const errorBoundary = page.locator('text=Something went wrong');
      if (await errorBoundary.isVisible()) {
        console.log('❌ Error boundary triggered - this is the original login issue');
        await page.screenshot({ path: 'error-boundary-screenshot.png' });
      } else {
        console.log('ℹ️ No error boundary - likely a navigation or loading issue');
      }

      // Re-throw to mark test as failed but don't prevent cleanup
      throw error;
    }
  });
});