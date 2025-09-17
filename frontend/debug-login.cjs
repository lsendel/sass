const { chromium } = require('playwright');

async function debugLogin() {
  console.log('🔍 Debug Login Page - Detailed Analysis...');

  const browser = await chromium.launch({
    headless: false,
    slowMo: 1000
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Navigate to login page
    console.log('📍 Navigating to login page...');
    await page.goto('http://localhost:3000/auth/login');
    await page.waitForLoadState('networkidle');

    // Wait a bit for React to hydrate
    await page.waitForTimeout(3000);

    // Check current state
    console.log('📄 Current URL:', page.url());
    console.log('📄 Page title:', await page.title());

    // Look for network requests
    const responses = [];
    page.on('response', response => {
      responses.push({
        url: response.url(),
        status: response.status(),
        contentType: response.headers()['content-type']
      });
    });

    // Force a reload to capture all requests
    console.log('🔄 Reloading page to capture network requests...');
    await page.reload();
    await page.waitForLoadState('networkidle');

    // Show network requests
    console.log('🌐 Network Requests:');
    responses.forEach(resp => {
      if (resp.url.includes('auth') || resp.url.includes('8082')) {
        console.log(`   ${resp.status} - ${resp.url} (${resp.contentType || 'unknown'})`);
      }
    });

    // Check for loading states
    const loadingElements = await page.locator('[data-testid*="loading"], .loading, [class*="loading"]').count();
    console.log('⏳ Loading elements found:', loadingElements);

    // Check for error messages
    const errorElements = await page.locator('[data-testid*="error"], .error, [class*="error"]').count();
    console.log('❌ Error elements found:', errorElements);

    // Look for auth method query status
    const pageContent = await page.content();
    console.log('🔍 Page contains "Authentication Unavailable":', pageContent.includes('Authentication Unavailable'));
    console.log('🔍 Page contains "passwordAuthEnabled":', pageContent.includes('passwordAuthEnabled'));
    console.log('🔍 Page contains "enablePasswordAuth":', pageContent.includes('enablePasswordAuth'));

    // Check React state/props in dev tools
    await page.evaluate(() => {
      console.log('React Dev Tools check...');
      const root = document.querySelector('#root');
      if (root && root._reactInternalInstance) {
        console.log('React instance found');
      }
    });

    // Test direct API call from page context
    const apiTest = await page.evaluate(async () => {
      try {
        console.log('Testing direct API call from page...');
        const response = await fetch('http://localhost:8082/api/v1/auth/methods');
        const data = await response.json();
        return { success: true, status: response.status, data };
      } catch (error) {
        return { success: false, error: error.message };
      }
    });

    console.log('🧪 Direct API test from page:', JSON.stringify(apiTest, null, 2));

    // Take screenshot
    await page.screenshot({ path: 'debug-login-state.png', fullPage: true });
    console.log('📸 Screenshot saved: debug-login-state.png');

    // Keep browser open for manual inspection
    console.log('🔍 Browser will stay open for 30 seconds for manual inspection...');
    await page.waitForTimeout(30000);

  } catch (error) {
    console.error('❌ Error:', error.message);
  } finally {
    await browser.close();
  }
}

debugLogin().catch(console.error);