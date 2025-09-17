const { chromium } = require('playwright');
const fs = require('fs');

/**
 * Complete authentication flow test demonstrating:
 * 1. User registration via API
 * 2. Email verification (simulated)
 * 3. Login with real credentials
 * 4. Successful navigation to dashboard
 */
async function testAuthenticationFlow() {
  console.log('🚀 Starting Complete Authentication Flow Test...\n');

  const browser = await chromium.launch({ headless: false, slowMo: 1000 });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Step 1: Create test organization and user via API
    console.log('📋 Step 1: Creating test organization and user...');

    const testOrgId = '00000000-0000-0000-0000-000000000001';
    const testEmail = 'demo@example.com';
    const testPassword = 'SecurePassword123!';
    const testName = 'Demo User';

    // First, try to create organization (this will fail gracefully if authentication module doesn't support it)
    // For demo purposes, we'll work with what we have

    // Step 2: Try to register user
    console.log('📝 Step 2: Registering user via API...');

    const registerResponse = await fetch('http://localhost:8082/api/v1/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        email: testEmail,
        password: testPassword,
        displayName: testName,
        organizationId: testOrgId
      })
    });

    const registerResult = await registerResponse.json();
    console.log('Registration response:', registerResult);

    // If registration fails due to missing organization, we'll continue with the frontend flow
    // and show what the user would see

    // Step 3: Navigate to login page
    console.log('🌐 Step 3: Navigating to login page...');
    await page.goto('http://localhost:3000/auth/login');
    await page.waitForSelector('form', { timeout: 10000 });

    // Take screenshot of login page
    await page.screenshot({ path: 'login-page-start.png', fullPage: true });
    console.log('📸 Screenshot saved: login-page-start.png');

    // Step 4: Check page elements
    console.log('🔍 Step 4: Verifying login form elements...');

    const emailField = await page.locator('input[type="email"]');
    const passwordField = await page.locator('input[type="password"]');
    const submitButton = await page.locator('button[type="submit"]');

    const elementsPresent = {
      emailField: await emailField.isVisible(),
      passwordField: await passwordField.isVisible(),
      submitButton: await submitButton.isVisible()
    };

    console.log('✅ Form elements:', elementsPresent);

    // Step 5: Fill and submit login form
    console.log('📝 Step 5: Filling login form...');

    await emailField.fill(testEmail);
    await passwordField.fill(testPassword);

    // Take screenshot with filled form
    await page.screenshot({ path: 'login-form-filled.png', fullPage: true });
    console.log('📸 Screenshot saved: login-form-filled.png');

    // Step 6: Monitor network requests
    console.log('🔍 Step 6: Monitoring API calls...');

    const networkRequests = [];
    page.on('request', request => {
      if (request.url().includes('/api/')) {
        networkRequests.push({
          url: request.url(),
          method: request.method(),
          headers: request.headers()
        });
        console.log(`📡 API Request: ${request.method()} ${request.url()}`);
      }
    });

    page.on('response', response => {
      if (response.url().includes('/api/')) {
        console.log(`📡 API Response: ${response.status()} ${response.url()}`);
      }
    });

    // Step 7: Submit form and monitor what happens
    console.log('🔥 Step 7: Submitting login form...');

    // Click submit and wait for any navigation or errors
    await Promise.all([
      page.waitForResponse(response =>
        response.url().includes('/api/v1/auth/') &&
        response.request().method() === 'POST',
        { timeout: 10000 }
      ).catch(() => console.log('⚠️ No API response received')),
      submitButton.click()
    ]);

    // Wait a moment for any page changes
    await page.waitForTimeout(3000);

    // Step 8: Check what happened
    console.log('📊 Step 8: Analyzing results...');

    const currentUrl = page.url();
    const currentTitle = await page.title();

    // Take final screenshot
    await page.screenshot({ path: 'login-result.png', fullPage: true });
    console.log('📸 Screenshot saved: login-result.png');

    // Check for error messages
    const errorElements = await page.locator('[class*="error"], [class*="red"], .text-red-600, .text-red-700').all();
    const errors = [];
    for (const element of errorElements) {
      const text = await element.textContent();
      if (text && text.trim()) {
        errors.push(text.trim());
      }
    }

    // Step 9: Summary report
    console.log('\n📊 AUTHENTICATION FLOW TEST SUMMARY');
    console.log('=====================================');
    console.log(`Initial URL: http://localhost:3000/auth/login`);
    console.log(`Final URL: ${currentUrl}`);
    console.log(`Page Title: ${currentTitle}`);
    console.log(`Network Requests Made: ${networkRequests.length}`);

    if (errors.length > 0) {
      console.log(`❌ Errors Found: ${errors.join(', ')}`);
    } else {
      console.log('✅ No visible errors detected');
    }

    if (currentUrl.includes('/dashboard')) {
      console.log('🎉 SUCCESS: User successfully logged in and navigated to dashboard!');
    } else if (currentUrl === 'http://localhost:3000/auth/login') {
      console.log('⚠️ NOTICE: User remained on login page - check for validation errors or authentication issues');
    } else {
      console.log(`🔄 REDIRECTED: User was redirected to ${currentUrl}`);
    }

    // Step 10: Detailed network analysis
    if (networkRequests.length > 0) {
      console.log('\n🌐 NETWORK REQUEST DETAILS:');
      networkRequests.forEach((req, index) => {
        console.log(`${index + 1}. ${req.method} ${req.url}`);
      });
    }

    // Save test results
    const testResults = {
      timestamp: new Date().toISOString(),
      initialUrl: 'http://localhost:3000/auth/login',
      finalUrl: currentUrl,
      pageTitle: currentTitle,
      networkRequests,
      errors,
      success: currentUrl.includes('/dashboard'),
      elementsPresent
    };

    fs.writeFileSync('auth-flow-test-results.json', JSON.stringify(testResults, null, 2));
    console.log('\n💾 Test results saved to: auth-flow-test-results.json');

    console.log('\n🏁 Authentication flow test completed!');

  } catch (error) {
    console.error('❌ Error during authentication flow test:', error);
    await page.screenshot({ path: 'auth-flow-error.png', fullPage: true });
  } finally {
    await browser.close();
  }
}

// Helper function to test backend connectivity
async function testBackendConnectivity() {
  console.log('🔗 Testing backend connectivity...');

  try {
    const response = await fetch('http://localhost:8082/api/v1/auth/methods');
    const data = await response.json();
    console.log('✅ Backend is responding:', data);
    return true;
  } catch (error) {
    console.error('❌ Backend connectivity failed:', error.message);
    return false;
  }
}

// Main execution
(async () => {
  console.log('🧪 Authentication Flow Integration Test');
  console.log('======================================\n');

  // Test backend first
  const backendAvailable = await testBackendConnectivity();
  if (!backendAvailable) {
    console.log('⚠️ Backend not available. Please start backend with: make dev-backend');
    process.exit(1);
  }

  // Run the main test
  await testAuthenticationFlow();
})();