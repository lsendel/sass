import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({
    headless: false,
    slowMo: 1000 // Slow down actions for visibility
  });

  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 }
  });

  const page = await context.newPage();

  // Enable console logging from the page
  page.on('console', msg => console.log('PAGE LOG:', msg.text()));
  page.on('pageerror', error => console.log('PAGE ERROR:', error.message));

  try {
    console.log('🚀 Opening application at http://localhost:3000...');

    // Navigate to the application
    await page.goto('http://localhost:3000', { waitUntil: 'networkidle', timeout: 10000 });

    // Wait for page to load
    await page.waitForTimeout(2000);

    console.log('📄 Current URL:', page.url());
    console.log('📄 Page title:', await page.title());

    // Take initial screenshot
    await page.screenshot({ path: 'page-initial.png', fullPage: true });
    console.log('📸 Initial page screenshot: page-initial.png');

    // Check if there's any content
    const bodyText = await page.locator('body').textContent();
    console.log('📝 Page content length:', bodyText.length);
    console.log('📝 First 200 chars:', bodyText.substring(0, 200));

    // Try to navigate to login page directly
    console.log('🔑 Trying to navigate to /auth/login...');
    await page.goto('http://localhost:3000/auth/login', { waitUntil: 'networkidle', timeout: 10000 });

    await page.waitForTimeout(2000);

    console.log('📄 Login page URL:', page.url());

    // Take login page screenshot
    await page.screenshot({ path: 'login-page-detailed.png', fullPage: true });
    console.log('📸 Login page screenshot: login-page-detailed.png');

    // Look for login elements
    const headings = await page.locator('h1, h2, h3').allTextContents();
    console.log('📝 Headings found:', headings);

    const buttons = await page.locator('button').allTextContents();
    console.log('🔘 Buttons found:', buttons);

    const links = await page.locator('a').allTextContents();
    console.log('🔗 Links found:', links.slice(0, 10)); // First 10 links

    // Check for specific login elements
    const loginForm = await page.locator('form').count();
    console.log('📋 Forms found:', loginForm);

    const inputs = await page.locator('input').count();
    console.log('📝 Input fields found:', inputs);

    // Look for OAuth buttons
    const oauthButtons = await page.locator('button:has-text("Google"), button:has-text("GitHub"), button:has-text("Microsoft")').count();
    console.log('🔐 OAuth buttons found:', oauthButtons);

    console.log('⏱️  Keeping browser open for 15 seconds...');
    await page.waitForTimeout(15000);

  } catch (error) {
    console.error('❌ Error:', error.message);
    await page.screenshot({ path: 'error-detailed.png' });
  }

  await browser.close();
  console.log('🏁 Done! Check the screenshot files.');
})();