import { chromium } from 'playwright';

(async () => {
  console.log('🚀 Launching browser to demonstrate OAuth login...');
  console.log('');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('📋 AUTHENTICATION INFORMATION:');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('');
  console.log('This application uses OAuth2/PKCE authentication.');
  console.log('NO USERNAME/PASSWORD required - instead you use:');
  console.log('');
  console.log('🔐 Available Login Methods:');
  console.log('   1. Google Account   - Sign in with Google');
  console.log('   2. GitHub Account   - Sign in with GitHub');
  console.log('   3. Microsoft Account - Sign in with Microsoft');
  console.log('');
  console.log('⚙️  Current Status:');
  console.log('   - OAuth providers need to be configured with real credentials');
  console.log('   - You would need to set up OAuth apps on Google/GitHub/Microsoft');
  console.log('   - Then add the client IDs and secrets to application.yml');
  console.log('');
  console.log('🧪 For Testing/Development:');
  console.log('   - Use mock authentication (needs to be enabled)');
  console.log('   - Or configure real OAuth credentials');
  console.log('');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('');

  const browser = await chromium.launch({
    headless: false,
    slowMo: 500
  });

  const page = await browser.newPage();
  await page.setViewportSize({ width: 1400, height: 900 });

  console.log('📱 Opening login page...');
  await page.goto('http://localhost:3000/auth/login');

  // Highlight the OAuth provider area
  await page.evaluate(() => {
    const errorMessage = document.querySelector('p');
    if (errorMessage && errorMessage.textContent.includes('Failed to load')) {
      errorMessage.style.border = '2px solid red';
      errorMessage.style.padding = '10px';
      errorMessage.style.backgroundColor = '#fee';

      // Add helpful message
      const helpDiv = document.createElement('div');
      helpDiv.innerHTML = `
        <div style="margin-top: 20px; padding: 20px; background: #f0f9ff; border: 2px solid #0ea5e9; border-radius: 8px;">
          <h3 style="margin: 0 0 10px 0; color: #0284c7;">🔐 How to Login:</h3>
          <p style="margin: 5px 0; color: #475569;">This app uses OAuth2 authentication (no username/password).</p>
          <p style="margin: 5px 0; color: #475569;">Once configured, you would click on:</p>
          <ul style="margin: 10px 0; padding-left: 20px; color: #475569;">
            <li>Google - to sign in with your Google account</li>
            <li>GitHub - to sign in with your GitHub account</li>
            <li>Microsoft - to sign in with your Microsoft account</li>
          </ul>
          <p style="margin: 10px 0 0 0; color: #94a3b8; font-size: 14px;">
            OAuth providers need to be configured in application.yml with real client IDs
          </p>
        </div>
      `;
      errorMessage.parentElement.appendChild(helpDiv);
    }
  });

  console.log('📸 Taking annotated screenshot...');
  await page.screenshot({ path: 'oauth-login-explanation.png', fullPage: true });

  console.log('');
  console.log('✅ Login page displayed with OAuth information!');
  console.log('');
  console.log('📝 To enable authentication, you need to:');
  console.log('   1. Create OAuth apps on Google/GitHub/Microsoft developer consoles');
  console.log('   2. Get the client ID and client secret for each');
  console.log('   3. Add them to backend/src/main/resources/application.yml');
  console.log('   4. Configure redirect URIs to http://localhost:8080/api/v1/oauth2/callback/{provider}');
  console.log('');
  console.log('⏰ Browser will remain open for 45 seconds...');

  await page.waitForTimeout(45000);

  console.log('✨ Demo complete!');
  await browser.close();
})().catch(console.error);