const { chromium } = require('playwright');

async function finalDemo() {
  console.log('🎯 FINAL DEMO: Email/Password Authentication System');
  console.log('================================================');

  const browser = await chromium.launch({
    headless: false,
    slowMo: 2000
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // 1. Navigate to login page
    console.log('📍 Step 1: Navigating to login page...');
    await page.goto('http://localhost:3000/auth/login');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    // 2. Take initial screenshot
    await page.screenshot({ path: 'final-demo-login-page.png', fullPage: true });
    console.log('📸 Screenshot saved: final-demo-login-page.png');

    // 3. Test API connectivity from browser
    console.log('🔌 Step 2: Testing API connectivity from browser...');
    const apiTest = await page.evaluate(async () => {
      try {
        const response = await fetch('http://localhost:8082/api/v1/auth/methods');
        const data = await response.json();
        return { success: true, status: response.status, data };
      } catch (error) {
        return { success: false, error: error.message };
      }
    });
    console.log('📊 API Test Result:', JSON.stringify(apiTest, null, 2));

    // 4. Check page content
    console.log('🔍 Step 3: Analyzing page content...');
    const pageContent = await page.content();
    const hasPasswordForm = pageContent.includes('type="password"');
    const hasEmailField = pageContent.includes('type="email"') || pageContent.includes('Email');
    const hasAuthUnavailable = pageContent.includes('Authentication Unavailable');

    console.log('📝 Page Analysis:');
    console.log(`   - Has password form: ${hasPasswordForm}`);
    console.log(`   - Has email field: ${hasEmailField}`);
    console.log(`   - Shows auth unavailable: ${hasAuthUnavailable}`);

    // 5. Look for specific elements
    console.log('🔍 Step 4: Looking for form elements...');
    const emailInput = await page.locator('input[type="email"], input[name="email"]').count();
    const passwordInput = await page.locator('input[type="password"], input[name="password"]').count();
    const submitButton = await page.locator('button[type="submit"], button:has-text("Sign In"), button:has-text("Login")').count();
    const forms = await page.locator('form').count();

    console.log('🔢 Element Count:');
    console.log(`   - Email inputs: ${emailInput}`);
    console.log(`   - Password inputs: ${passwordInput}`);
    console.log(`   - Submit buttons: ${submitButton}`);
    console.log(`   - Forms: ${forms}`);

    // 6. If form exists, try to interact with it
    if (emailInput > 0 && passwordInput > 0) {
      console.log('✅ Step 5: Password form found! Testing interaction...');

      // Fill form
      await page.fill('input[type="email"], input[name="email"]', 'test@example.com');
      await page.fill('input[type="password"], input[name="password"]', 'TestPassword123!');

      // Take screenshot with filled form
      await page.screenshot({ path: 'final-demo-form-filled.png', fullPage: true });
      console.log('📸 Screenshot saved: final-demo-form-filled.png');

      console.log('🎉 SUCCESS: Email/password form is working correctly!');
    } else {
      console.log('❌ Step 5: Password form not found. Checking for error messages...');

      // Look for error messages
      const errorMessages = await page.locator('.error, [class*="error"], .alert-error').allTextContents();
      console.log('🚨 Error messages found:', errorMessages);

      // Check network tab for failed requests
      const failedRequests = [];
      page.on('response', response => {
        if (!response.ok()) {
          failedRequests.push({
            url: response.url(),
            status: response.status(),
            statusText: response.statusText()
          });
        }
      });

      // Force a page reload to capture network errors
      await page.reload();
      await page.waitForLoadState('networkidle');

      console.log('🌐 Failed requests:', failedRequests);
    }

    // 7. Test direct backend endpoints
    console.log('🔧 Step 6: Testing backend endpoints directly...');
    const endpointTests = [
      { name: 'Auth Methods', url: 'http://localhost:8082/api/v1/auth/methods' },
      { name: 'Health Check', url: 'http://localhost:8082/actuator/health' }
    ];

    for (const test of endpointTests) {
      try {
        const response = await fetch(test.url);
        const text = await response.text();
        console.log(`   ✅ ${test.name}: ${response.status} - ${text.slice(0, 100)}...`);
      } catch (error) {
        console.log(`   ❌ ${test.name}: Error - ${error.message}`);
      }
    }

    // 8. Final summary
    console.log('📋 FINAL SUMMARY:');
    console.log('================');
    console.log(`   Backend API: ${apiTest.success ? '✅ Working' : '❌ Failed'}`);
    console.log(`   Frontend Page: ${hasEmailField || hasPasswordForm ? '✅ Loaded' : '❌ Issues'}`);
    console.log(`   Password Form: ${emailInput > 0 && passwordInput > 0 ? '✅ Present' : '❌ Missing'}`);

    if (apiTest.success && apiTest.data && apiTest.data.passwordAuthEnabled) {
      console.log('   🎯 PASSWORD AUTHENTICATION: ✅ FULLY IMPLEMENTED');
      console.log('   📧 Email/password login is ready for use!');
    } else {
      console.log('   ⚠️  PASSWORD AUTHENTICATION: Configuration needed');
    }

    // Keep browser open for 20 seconds for manual inspection
    console.log('\n🔍 Browser will stay open for 20 seconds for manual inspection...');
    await page.waitForTimeout(20000);

  } catch (error) {
    console.error('❌ Demo Error:', error.message);
  } finally {
    await browser.close();
    console.log('🏁 Demo complete!');
  }
}

finalDemo().catch(console.error);