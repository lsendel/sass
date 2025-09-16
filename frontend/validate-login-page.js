import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({
    headless: false,
    slowMo: 500
  });

  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 }
  });

  const page = await context.newPage();

  // Enable console and error logging
  page.on('console', msg => console.log('PAGE LOG:', msg.text()));
  page.on('pageerror', error => console.log('PAGE ERROR:', error.message));

  try {
    console.log('🚀 Validating login page at http://localhost:3000...');

    // Navigate to the application
    await page.goto('http://localhost:3000', { waitUntil: 'networkidle', timeout: 15000 });

    // Wait for page to fully load
    await page.waitForTimeout(3000);

    console.log('📄 Current URL:', page.url());
    console.log('📄 Page title:', await page.title());

    // Take initial screenshot
    await page.screenshot({ path: 'validation-initial.png', fullPage: true });
    console.log('📸 Initial screenshot: validation-initial.png');

    // Validate login page elements
    console.log('\n🔍 VALIDATING LOGIN PAGE ELEMENTS...');

    // Check for Payment Platform heading
    const heading = await page.locator('h2:has-text("Payment Platform")').first();
    const headingExists = await heading.count() > 0;
    console.log('✅ Payment Platform heading:', headingExists ? 'FOUND' : 'NOT FOUND');

    // Check for tagline
    const tagline = await page.locator('text=Secure subscription management for your business');
    const taglineExists = await tagline.count() > 0;
    console.log('✅ Tagline:', taglineExists ? 'FOUND' : 'NOT FOUND');

    // Check for logo/brand element
    const logo = await page.locator('[class*="bg-primary-600"]').first();
    const logoExists = await logo.count() > 0;
    console.log('✅ Logo element:', logoExists ? 'FOUND' : 'NOT FOUND');

    // Navigate specifically to login page
    console.log('\n🔑 Navigating to /auth/login...');
    await page.goto('http://localhost:3000/auth/login', { waitUntil: 'networkidle', timeout: 15000 });
    await page.waitForTimeout(2000);

    // Take login page screenshot
    await page.screenshot({ path: 'validation-login-page.png', fullPage: true });
    console.log('📸 Login page screenshot: validation-login-page.png');

    // Check for specific login page content
    console.log('\n🔍 CHECKING LOGIN PAGE CONTENT...');

    const bodyText = await page.locator('body').textContent();
    console.log('📝 Page content preview:', bodyText.substring(0, 300) + '...');

    // Look for AuthLayout structure
    const authCard = await page.locator('.bg-white.shadow').first();
    const authCardExists = await authCard.count() > 0;
    console.log('✅ Auth card container:', authCardExists ? 'FOUND' : 'NOT FOUND');

    // Check for error message
    const errorMessage = await page.locator('text=Failed to load authentication providers');
    const errorExists = await errorMessage.count() > 0;
    console.log('✅ Auth providers error:', errorExists ? 'FOUND' : 'NOT FOUND');

    // Check for footer
    const footer = await page.locator('text=© 2024 Payment Platform');
    const footerExists = await footer.count() > 0;
    console.log('✅ Footer:', footerExists ? 'FOUND' : 'NOT FOUND');

    // Try to find the LoginPage component content
    console.log('\n🔍 LOOKING FOR LOGIN FORM ELEMENTS...');

    const buttons = await page.locator('button').all();
    console.log('🔘 Total buttons found:', buttons.length);

    const inputs = await page.locator('input').all();
    console.log('📝 Total input fields found:', inputs.length);

    const forms = await page.locator('form').all();
    console.log('📋 Total forms found:', forms.length);

    // Check for specific OAuth providers
    const googleButton = await page.locator('button:has-text("Google"), [data-provider="google"]');
    const githubButton = await page.locator('button:has-text("GitHub"), [data-provider="github"]');
    const microsoftButton = await page.locator('button:has-text("Microsoft"), [data-provider="microsoft"]');

    console.log('🔐 Google OAuth button:', await googleButton.count() > 0 ? 'FOUND' : 'NOT FOUND');
    console.log('🔐 GitHub OAuth button:', await githubButton.count() > 0 ? 'FOUND' : 'NOT FOUND');
    console.log('🔐 Microsoft OAuth button:', await microsoftButton.count() > 0 ? 'FOUND' : 'NOT FOUND');

    // Check page structure
    console.log('\n🏗️  PAGE STRUCTURE ANALYSIS...');
    const mainContent = await page.locator('main, [role="main"]');
    console.log('📄 Main content area:', await mainContent.count() > 0 ? 'FOUND' : 'NOT FOUND');

    // Check for React components loading
    const reactComponents = await page.locator('[data-reactroot], #root').first();
    const reactLoaded = await reactComponents.count() > 0;
    console.log('⚛️  React app loaded:', reactLoaded ? 'YES' : 'NO');

    // Take a focused screenshot of the auth card
    if (authCardExists) {
      await authCard.screenshot({ path: 'validation-auth-card.png' });
      console.log('📸 Auth card screenshot: validation-auth-card.png');
    }

    console.log('\n⏱️  Keeping browser open for 10 seconds to inspect...');
    await page.waitForTimeout(10000);

  } catch (error) {
    console.error('❌ Validation Error:', error.message);
    await page.screenshot({ path: 'validation-error.png', fullPage: true });
    console.log('📸 Error screenshot: validation-error.png');
  }

  await browser.close();
  console.log('\n🏁 Validation complete! Check the screenshot files for visual confirmation.');
})();