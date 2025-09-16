import { chromium } from 'playwright';

(async () => {
  console.log('🚀 Launching browser to show the login page...');

  const browser = await chromium.launch({
    headless: false,
    slowMo: 1000  // Slow down actions for better visibility
  });

  const page = await browser.newPage();

  // Set a larger viewport for better visibility
  await page.setViewportSize({ width: 1200, height: 800 });

  console.log('📱 Navigating to login page...');
  await page.goto('http://localhost:3000/auth/login');

  console.log('📸 Taking a screenshot...');
  await page.screenshot({ path: 'current-login-page.png', fullPage: true });

  console.log('🎭 Login page is now displayed in the browser!');
  console.log('📋 You can see:');
  console.log('   - The Payment Platform branding');
  console.log('   - The login interface');
  console.log('   - OAuth provider buttons (if loaded)');
  console.log('   - Responsive design');

  console.log('⏰ Keeping browser open for 30 seconds for demonstration...');
  console.log('   (Browser will auto-close after 30 seconds)');

  // Keep the page open for 30 seconds so you can see it
  await page.waitForTimeout(30000);

  console.log('✅ Demo complete! Closing browser...');
  await browser.close();
})().catch(console.error);