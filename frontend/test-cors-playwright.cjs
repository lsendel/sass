const { chromium } = require('playwright');

async function testCors() {
  console.log('🔬 CORS Debugging Test');
  console.log('======================');

  const browser = await chromium.launch({
    headless: false,
    slowMo: 1000
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Navigate to the test page
    console.log('📍 Opening CORS test page...');
    await page.goto('file://' + __dirname + '/test-cors.html');
    await page.waitForLoadState('networkidle');

    // Monitor network requests
    const requests = [];
    const responses = [];

    page.on('request', request => {
      requests.push({
        url: request.url(),
        method: request.method(),
        headers: request.headers()
      });
      console.log(`📤 Request: ${request.method()} ${request.url()}`);
    });

    page.on('response', response => {
      responses.push({
        url: response.url(),
        status: response.status(),
        headers: response.headers(),
        statusText: response.statusText()
      });
      console.log(`📥 Response: ${response.status()} ${response.url()}`);
    });

    // Click the test button
    console.log('🔘 Clicking test button...');
    await page.click('button:has-text("Test API Call")');

    // Wait for result
    await page.waitForTimeout(5000);

    // Check console logs
    const logs = [];
    page.on('console', msg => {
      logs.push(msg.text());
      console.log(`🖥️  Console: ${msg.text()}`);
    });

    // Get the result
    const result = await page.textContent('#result');
    console.log('📊 Result from page:');
    console.log(result);

    // Check if there were any failed requests
    const failedRequests = responses.filter(r => r.status >= 400);
    console.log('❌ Failed requests:', failedRequests);

    // Check for CORS-specific errors
    const corsErrors = logs.filter(log => log.includes('CORS') || log.includes('Access-Control'));
    console.log('🚫 CORS errors:', corsErrors);

    // Keep browser open for manual inspection
    console.log('🔍 Browser will stay open for 20 seconds for manual inspection...');
    await page.waitForTimeout(20000);

  } catch (error) {
    console.error('❌ Test Error:', error.message);
  } finally {
    await browser.close();
    console.log('🏁 CORS test complete!');
  }
}

testCors().catch(console.error);