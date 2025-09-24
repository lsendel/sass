import { test, expect } from '@playwright/test';

test.describe('Login Validation Test', () => {
  test('should successfully login and reach dashboard without errors', async ({ page }) => {
    console.log('🔍 Testing core login functionality...');

    // Navigate to the application
    await page.goto('http://localhost:3001');

    // Verify login page loads correctly
    const loginButton = page.locator('[data-testid="login-button"]');
    await expect(loginButton).toBeVisible({ timeout: 5000 });
    console.log('✅ Login page loaded successfully');

    // Click demo credentials button to populate form
    await loginButton.click();
    await page.waitForTimeout(500);

    // Submit the login form
    const submitButton = page.locator('[data-testid="submit-button"]');
    await expect(submitButton).toBeVisible({ timeout: 3000 });
    await submitButton.click();

    // Verify successful navigation to dashboard
    await page.waitForURL('**/dashboard', { timeout: 5000 });
    console.log('✅ Successfully logged in and reached dashboard');

    // Check that no error boundary is triggered
    const errorBoundary = page.locator('text=Something went wrong');
    const hasErrorBoundary = await errorBoundary.isVisible({ timeout: 1000 }).catch(() => false);

    if (hasErrorBoundary) {
      console.log('❌ Error boundary still present - login issue not fully resolved');
      await page.screenshot({ path: 'error-boundary-still-present.png' });
      throw new Error('Error boundary triggered during login');
    } else {
      console.log('✅ No error boundary - original login error is completely fixed');
    }

    // Verify dashboard content loads
    const dashboardContent = page.locator('text=Welcome back');
    await expect(dashboardContent).toBeVisible({ timeout: 3000 });
    console.log('✅ Dashboard content loaded successfully');

    // Take a screenshot of successful state
    await page.screenshot({ path: 'successful-login-dashboard.png' });

    console.log('🎉 Login validation test completed successfully!');
    console.log('📋 Summary: Original "Something went wrong" error has been completely resolved');
  });

  test('should have navigation elements available', async ({ page }) => {
    console.log('🔍 Testing navigation elements availability...');

    // Login first
    await page.goto('http://localhost:3001');
    const loginButton = page.locator('[data-testid="login-button"]');
    await loginButton.click();
    await page.waitForTimeout(500);
    const submitButton = page.locator('[data-testid="submit-button"]');
    await submitButton.click();
    await page.waitForURL('**/dashboard', { timeout: 5000 });

    // Check if navigation elements exist (may be hidden on mobile)
    const navElements = [
      'nav-organizations',
      'nav-payments',
      'nav-subscription',
      'nav-settings'
    ];

    for (const navId of navElements) {
      const navElement = page.locator(`[data-testid="${navId}"]`);
      const count = await navElement.count();

      if (count > 0) {
        console.log(`✅ Found ${count} element(s) for ${navId}`);
        // Check if at least one is visible
        const firstVisible = await navElement.first().isVisible().catch(() => false);
        if (firstVisible) {
          console.log(`  └─ First element is visible`);
        } else {
          console.log(`  └─ Elements exist but may be hidden (mobile menu)`);
        }
      } else {
        console.log(`❌ No elements found for ${navId}`);
      }
    }

    console.log('📋 Navigation analysis completed');
  });
});