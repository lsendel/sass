import { chromium } from 'playwright';

async function testViewport() {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext({
    viewport: { width: 1024, height: 768 }
  });

  const page = await context.newPage();

  console.log('🔍 Testing 1024x768 viewport compatibility...\n');

  try {
    // Test login page
    console.log('📋 LOGIN PAGE:');
    await page.goto('http://localhost:3000', { waitUntil: 'networkidle' });

    const loginMetrics = await page.evaluate(() => {
      const body = document.body;
      const scrollHeight = body.scrollHeight;
      const windowHeight = window.innerHeight;
      const needsScroll = scrollHeight > windowHeight;

      // Check for large elements
      const allElements = Array.from(document.querySelectorAll('*'));
      const oversized = allElements.filter(el => {
        const rect = el.getBoundingClientRect();
        return rect.height > 100 && !el.closest('main'); // Allow large main content areas
      }).map(el => ({
        tag: el.tagName.toLowerCase(),
        class: el.className,
        height: Math.round(el.getBoundingClientRect().height)
      }));

      return {
        scrollHeight,
        windowHeight,
        needsScroll,
        oversizedElements: oversized.slice(0, 5)
      };
    });

    console.log(`- Content height: ${loginMetrics.scrollHeight}px`);
    console.log(`- Window height: ${loginMetrics.windowHeight}px`);
    console.log(`- Needs scrolling: ${loginMetrics.needsScroll ? 'YES ❌' : 'NO ✅'}`);

    if (loginMetrics.oversizedElements.length > 0) {
      console.log('- Oversized elements found:');
      loginMetrics.oversizedElements.forEach(el => {
        console.log(`  • ${el.tag}${el.class ? '.' + el.class.split(' ')[0] : ''} - ${el.height}px`);
      });
    }

    // Navigate to dashboard (mock login if needed)
    console.log('\n📋 DASHBOARD PAGE:');

    try {
      await page.goto('http://localhost:3000/dashboard', { waitUntil: 'networkidle', timeout: 5000 });
    } catch (e) {
      // Try mock login route
      try {
        await page.goto('http://localhost:3000/mock-login', { waitUntil: 'networkidle', timeout: 5000 });
        await page.click('button', { timeout: 5000 });
        await page.waitForNavigation({ waitUntil: 'networkidle', timeout: 10000 });
      } catch (mockError) {
        console.log('⚠️ Could not access dashboard - trying organizations page');
        await page.goto('http://localhost:3000/organizations', { waitUntil: 'networkidle', timeout: 5000 });
      }
    }

    const dashboardMetrics = await page.evaluate(() => {
      const body = document.body;
      const scrollHeight = body.scrollHeight;
      const windowHeight = window.innerHeight;
      const needsScroll = scrollHeight > windowHeight;

      // Check icon sizes specifically
      const icons = Array.from(document.querySelectorAll('svg')).map(icon => {
        const rect = icon.getBoundingClientRect();
        return {
          width: Math.round(rect.width),
          height: Math.round(rect.height),
          classes: icon.className.baseVal || icon.getAttribute('class') || ''
        };
      }).filter(icon => icon.width > 24 || icon.height > 24); // Find icons larger than 24px

      // Check typography
      const headings = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6')).map(h => {
        const style = window.getComputedStyle(h);
        return {
          tag: h.tagName.toLowerCase(),
          fontSize: style.fontSize,
          lineHeight: style.lineHeight
        };
      });

      return {
        scrollHeight,
        windowHeight,
        needsScroll,
        largeIcons: icons.slice(0, 5),
        headings: headings.slice(0, 5)
      };
    });

    console.log(`- Content height: ${dashboardMetrics.scrollHeight}px`);
    console.log(`- Window height: ${dashboardMetrics.windowHeight}px`);
    console.log(`- Needs scrolling: ${dashboardMetrics.needsScroll ? 'YES ❌' : 'NO ✅'}`);

    if (dashboardMetrics.largeIcons.length > 0) {
      console.log('- Large icons found:');
      dashboardMetrics.largeIcons.forEach(icon => {
        console.log(`  • ${icon.width}x${icon.height}px - ${icon.classes.split(' ')[0] || 'no-class'}`);
      });
    } else {
      console.log('✅ All icons appropriately sized (≤24px)');
    }

    console.log('- Typography sizes:');
    dashboardMetrics.headings.forEach(h => {
      console.log(`  • ${h.tag}: ${h.fontSize}`);
    });

    // Take a screenshot for manual review
    await page.screenshot({
      path: 'viewport-test-1024x768.png',
      fullPage: false // Only capture viewport
    });

    console.log('\n📸 Screenshot saved as viewport-test-1024x768.png');
    console.log('\n⏱️ Keeping browser open for 15 seconds for manual review...');
    await page.waitForTimeout(15000);

  } catch (error) {
    console.error('❌ Error during viewport test:', error.message);
  } finally {
    await browser.close();
  }
}

testViewport().catch(console.error);