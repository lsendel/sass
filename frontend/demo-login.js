import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  console.log('Opening the application...');
  await page.goto('http://localhost:3000/auth/login');

  console.log('Taking screenshot...');
  await page.screenshot({ path: 'login-page-demo.png', fullPage: true });

  console.log('Login page is displayed! Screenshot saved as login-page-demo.png');

  // Wait a bit so you can see the page
  await page.waitForTimeout(5000);

  await browser.close();
})();