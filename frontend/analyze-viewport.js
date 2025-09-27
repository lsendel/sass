import { chromium } from 'playwright';

async function analyzeApplication() {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext({
    viewport: { width: 1024, height: 768 }
  });

  const page = await context.newPage();

  console.log('🔍 Analyzing application at 1024x768 viewport...\n');

  try {
    // Go to login page first
    await page.goto('http://localhost:3000', { waitUntil: 'networkidle' });

    console.log('📋 LOGIN PAGE ANALYSIS:');

    // Check if content fits in viewport
    const bodyHeight = await page.evaluate(() => document.body.scrollHeight);
    const viewportHeight = 768;

    console.log(`- Body height: ${bodyHeight}px`);
    console.log(`- Viewport height: ${viewportHeight}px`);
    console.log(`- Needs scrolling: ${bodyHeight > viewportHeight ? 'YES ❌' : 'NO ✅'}`);

    // Check for oversized elements
    const largeElements = await page.evaluate(() => {
      const elements = Array.from(document.querySelectorAll('*'));
      const large = [];

      elements.forEach(el => {
        const rect = el.getBoundingClientRect();
        const style = window.getComputedStyle(el);

        // Check for oversized icons, text, or spacing
        if (rect.width > 80 || rect.height > 80) {
          if (el.tagName === 'SVG' || el.classList.contains('icon') ||
              style.fontSize && parseInt(style.fontSize) > 32) {
            large.push({
              tag: el.tagName,
              classes: Array.from(el.classList).join(' '),
              width: Math.round(rect.width),
              height: Math.round(rect.height),
              fontSize: style.fontSize
            });
          }
        }
      });

      return large.slice(0, 10); // Limit to first 10
    });

    if (largeElements.length > 0) {
      console.log('\n🔍 Large elements found:');
      largeElements.forEach((el, i) => {
        console.log(`  ${i+1}. ${el.tag} (${el.classes}) - ${el.width}x${el.height}px`);
        if (el.fontSize) console.log(`     Font size: ${el.fontSize}`);
      });
    } else {
      console.log('\n✅ No oversized elements detected');
    }

    // Navigate to dashboard (mock login)
    console.log('\n📋 DASHBOARD PAGE ANALYSIS:');

    // Try to find a way to get to dashboard - look for mock login or direct access
    try {
      await page.goto('http://localhost:3000/dashboard', { waitUntil: 'networkidle', timeout: 5000 });
    } catch {
      // If dashboard is protected, try mock login
      try {
        await page.goto('http://localhost:3000/mock-login', { waitUntil: 'networkidle', timeout: 5000 });
        await page.click('button', { timeout: 5000 });
        await page.waitForNavigation({ waitUntil: 'networkidle', timeout: 10000 });
      } catch {
        console.log('⚠️ Could not access dashboard - authentication required');
        await browser.close();
        return;
      }
    }

    // Analyze dashboard
    const dashboardHeight = await page.evaluate(() => document.body.scrollHeight);
    console.log(`- Dashboard height: ${dashboardHeight}px`);
    console.log(`- Needs scrolling: ${dashboardHeight > viewportHeight ? 'YES ❌' : 'NO ✅'}`);

    // Check for navigation elements
    const nav = await page.evaluate(() => {
      const navElements = document.querySelectorAll('nav, [role="navigation"], .nav, .navbar, .sidebar');
      return Array.from(navElements).map(el => ({
        tag: el.tagName,
        classes: Array.from(el.classList).join(' '),
        height: Math.round(el.getBoundingClientRect().height),
        visible: el.offsetParent !== null
      }));
    });

    if (nav.length > 0) {
      console.log('\n🧭 Navigation elements:');
      nav.forEach((el, i) => {
        console.log(`  ${i+1}. ${el.tag} (${el.classes}) - ${el.height}px high, visible: ${el.visible}`);
      });
    }

    console.log('\n⏱️ Keeping browser open for 10 seconds for manual inspection...');
    await page.waitForTimeout(10000);

  } catch (error) {
    console.error('❌ Error analyzing application:', error.message);
  } finally {
    await browser.close();
  }
}

analyzeApplication().catch(console.error);
