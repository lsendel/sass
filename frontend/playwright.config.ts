import { defineConfig, devices } from '@playwright/test'
import path from 'path'

/**
 * Comprehensive Playwright configuration for E2E testing
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './tests/e2e',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
    ['html', { outputFolder: path.join('test-results', 'report'), open: 'never' }],
    ['json', { outputFile: path.join('test-results', 'results.json') }],
    ['junit', { outputFile: path.join('test-results', 'results.xml') }],
    ['line'],
    ['allure-playwright', { outputFolder: 'test-results/allure-results' }],
  ],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3000',

    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',

    /* Take screenshot on failure */
    screenshot: 'only-on-failure',

    /* Record video on failure */
    video: 'retain-on-failure',

    /* Timeout for each action */
    actionTimeout: 10000,

    /* Global timeout for each test */
    navigationTimeout: 30000,

    /* Ignore HTTPS errors */
    ignoreHTTPSErrors: true,

    /* Viewport settings */
    viewport: { width: 1280, height: 720 },

    /* Locale and timezone */
    locale: 'en-US',
    timezoneId: 'America/New_York',

    /* Additional browser context options */
    permissions: ['geolocation'],
    colorScheme: 'light',
  },

  /* Global timeout for each test */
  timeout: 30000,

  /* Timeout for expect() assertions */
  expect: {
    timeout: 5000,
  },

  // Global output directory for attachments (screenshots, videos, traces)
  outputDir: path.join('test-results', 'evidence'),

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },

    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },

    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },

    /* Test against mobile viewports. */
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },

    /* Test against branded browsers. */
    // {
    //   name: 'Microsoft Edge',
    //   use: { ...devices['Desktop Edge'], channel: 'msedge' },
    // },
    // {
    //   name: 'Google Chrome',
    //   use: { ...devices['Desktop Chrome'], channel: 'chrome' },
    // },
  ],

  /* Run your local dev server before starting the tests */
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
    env: {
      NODE_ENV: 'test',
      VITE_API_BASE_URL: 'http://localhost:8080/api',
    },
  },

  /* Global setup and teardown */
  globalSetup: require.resolve('./tests/e2e/utils/global-setup.ts'),
  globalTeardown: require.resolve('./tests/e2e/utils/global-teardown.ts'),

  /* Test filters */
  grep: process.env.PLAYWRIGHT_GREP ? new RegExp(process.env.PLAYWRIGHT_GREP) : undefined,
  grepInvert: process.env.PLAYWRIGHT_GREP_INVERT ? new RegExp(process.env.PLAYWRIGHT_GREP_INVERT) : undefined,

  /* Report slow tests */
  reportSlowTests: {
    max: 5,
    threshold: 15000, // 15 seconds
  },
})
