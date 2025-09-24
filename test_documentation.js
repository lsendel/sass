const { chromium } = require('playwright');

async function testApiDocumentation() {
  console.log('=== API Documentation Live Testing ===');
  
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    // Test 1: Check if Swagger UI loads
    console.log('1. Testing Swagger UI accessibility...');
    const response = await page.goto('http://localhost:8082/swagger-ui.html', { 
      waitUntil: 'networkidle',
      timeout: 10000 
    });
    
    if (response && response.status() === 200) {
      console.log('✅ Swagger UI loads successfully');
      
      // Check for key elements
      const title = await page.textContent('title');
      console.log(`   Page title: ${title}`);
      
      // Check for API groups
      const apiGroups = await page.$$eval('.opblock-tag-section', sections => 
        sections.map(section => section.textContent.trim())
      );
      console.log(`   API groups found: ${apiGroups.length}`);
      
    } else {
      console.log(`❌ Swagger UI failed to load (status: ${response?.status()})`);
    }

    // Test 2: Check OpenAPI JSON endpoint
    console.log('\n2. Testing OpenAPI JSON specification...');
    const apiDocsResponse = await page.goto('http://localhost:8082/api-docs');
    
    if (apiDocsResponse && apiDocsResponse.status() === 200) {
      console.log('✅ OpenAPI JSON specification accessible');
      
      const content = await page.content();
      const isValidJson = content.includes('"openapi"') && content.includes('"info"');
      
      if (isValidJson) {
        console.log('✅ Valid OpenAPI 3.0 specification format');
      } else {
        console.log('❌ Invalid OpenAPI specification format');
      }
    } else {
      console.log(`❌ OpenAPI JSON failed to load (status: ${apiDocsResponse?.status()})`);
    }

    // Test 3: Test specific endpoints documentation
    console.log('\n3. Testing endpoint documentation...');
    await page.goto('http://localhost:8082/swagger-ui.html');
    
    // Wait for Swagger UI to fully load
    await page.waitForSelector('.swagger-ui', { timeout: 5000 });
    
    // Check for authentication endpoints
    const authSection = await page.$('.opblock-tag[data-tag="Authentication"]');
    if (authSection) {
      console.log('✅ Authentication endpoints documented');
    } else {
      console.log('❌ Authentication endpoints not found');
    }
    
    // Check for payment endpoints
    const paymentSection = await page.$('.opblock-tag[data-tag="Payments"]');
    if (paymentSection) {
      console.log('✅ Payment endpoints documented');
    } else {
      console.log('❌ Payment endpoints not found');
    }

  } catch (error) {
    console.log(`❌ Error during testing: ${error.message}`);
  } finally {
    await browser.close();
  }
}

// Run the test
testApiDocumentation().catch(console.error);
