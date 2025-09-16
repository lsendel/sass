const { chromium } = require('playwright');

async function showLoginPage() {
    const browser = await chromium.launch({ headless: false, slowMo: 1000 });
    const page = await browser.newPage();

    try {
        console.log('🚀 Navigating to the payment platform...');
        await page.goto('http://localhost:3000');

        // Wait for the page to load
        await page.waitForLoadState('networkidle', { timeout: 10000 });

        console.log('📸 Taking screenshot of login page...');
        await page.screenshot({
            path: 'login-page.png',
            fullPage: true
        });

        console.log('✅ Login page captured! Screenshot saved as login-page.png');
        console.log('🔍 Current page title:', await page.title());
        console.log('🌐 Current URL:', page.url());

        // Wait 5 seconds to show the page
        console.log('👀 Displaying the login page for 10 seconds...');
        await page.waitForTimeout(10000);

    } catch (error) {
        console.error('❌ Error showing login page:', error.message);
    } finally {
        await browser.close();
    }
}

showLoginPage();