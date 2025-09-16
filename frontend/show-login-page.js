import { chromium } from 'playwright';

(async () => {
  // Launch browser
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 }
  });
  const page = await context.newPage();

  try {
    console.log('🚀 Opening application at http://localhost:3000...');

    // Navigate to the application
    await page.goto('http://localhost:3000', { waitUntil: 'networkidle' });

    console.log('📸 Taking screenshot of current page...');

    // Take a screenshot
    await page.screenshot({
      path: 'login-page-screenshot.png',
      fullPage: true
    });

    console.log('✅ Screenshot saved as: login-page-screenshot.png');
    console.log('🌐 Current URL:', page.url());

    // Get page title
    const title = await page.title();
    console.log('📄 Page title:', title);

    // Check if we're on login page or redirected
    const currentUrl = page.url();
    if (currentUrl.includes('/auth/login')) {
      console.log('🔑 Successfully redirected to login page!');

      // Try to find login elements
      const loginHeading = await page.locator('h1, h2').filter({ hasText: /sign in|login/i }).first();
      if (await loginHeading.isVisible()) {
        const headingText = await loginHeading.textContent();
        console.log('📝 Login heading found:', headingText);
      }

      // Look for OAuth provider buttons
      const providerButtons = await page.locator('button').filter({ hasText: /google|github|microsoft/i }).count();
      console.log('🔐 OAuth provider buttons found:', providerButtons);

    } else {
      console.log('🏠 Currently on home page, looking for login redirect or auth elements...');

      // Look for any auth-related elements
      const authElements = await page.locator('text=/sign in|login|authenticate/i').count();
      console.log('🔍 Auth-related elements found:', authElements);
    }

    // Wait a moment for user to see the page
    console.log('⏱️  Keeping browser open for 10 seconds so you can see the page...');
    await page.waitForTimeout(10000);

  } catch (error) {
    console.error('❌ Error:', error.message);
    await page.screenshot({ path: 'error-screenshot.png' });
    console.log('📸 Error screenshot saved as: error-screenshot.png');
  }

  await browser.close();
  console.log('🏁 Done! Check login-page-screenshot.png to see the page.');
})();