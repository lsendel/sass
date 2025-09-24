# Enhanced Playwright Evidence Collection Guide

This guide covers the comprehensive evidence collection system implemented for E2E testing in the Spring Boot Modulith Payment Platform.

## 📋 Overview

The enhanced evidence collection system provides:

- **Comprehensive Screenshots**: Step-by-step visual evidence with highlighting
- **Network Activity Logging**: Complete request/response tracking
- **Performance Metrics**: Load times, memory usage, and resource analysis
- **Console Monitoring**: Error tracking and debugging information
- **Accessibility Snapshots**: WCAG compliance verification
- **Enhanced Reporting**: Visual dashboards and failure analysis

## 🚀 Quick Start

### Running Enhanced Tests

```bash
# Run enhanced evidence collection test
npm run test:e2e:enhanced

# Run with enhanced reporting
npm run test:e2e:evidence

# View enhanced HTML report
npm run test:e2e:enhanced-report
```

### Basic Usage in Tests

```typescript
import { test, expect } from './fixtures'

test('my test with evidence', async ({ page, evidenceCollector }) => {
  await page.goto('/')

  // Take screenshot with highlighting
  await evidenceCollector.takeStepScreenshot('page-loaded', {
    fullPage: true,
    highlight: ['[data-testid="main-content"]']
  })

  // Collect performance metrics
  await evidenceCollector.collectPerformanceMetrics('initial-load')

  // Capture accessibility snapshot
  await evidenceCollector.collectAccessibilitySnapshot('page-accessibility')
})
```

## 🛠️ Evidence Collector API

### Core Methods

#### `takeStepScreenshot(stepName, options)`

Captures annotated screenshots with step information.

```typescript
await evidenceCollector.takeStepScreenshot('login-page', {
  fullPage: true,              // Capture full page
  highlight: ['.login-form'],   // Highlight specific elements
  mask: ['.sensitive-data']     // Mask sensitive information
})
```

#### `collectPerformanceMetrics(stepName)`

Captures comprehensive performance data.

```typescript
const metrics = await evidenceCollector.collectPerformanceMetrics('page-load')
console.log(`Load time: ${metrics.navigation.loadComplete}ms`)
```

#### `collectAccessibilitySnapshot(stepName)`

Captures accessibility tree for WCAG analysis.

```typescript
await evidenceCollector.collectAccessibilitySnapshot('form-accessibility')
```

#### `capturePageHTML(stepName)`

Saves complete page source for debugging.

```typescript
await evidenceCollector.capturePageHTML('error-state')
```

#### `captureElementScreenshot(selector, stepName)`

Screenshots specific elements.

```typescript
await evidenceCollector.captureElementScreenshot('.error-message', 'error-display')
```

#### `waitForNetworkIdle(timeout)`

Waits for network requests to complete.

```typescript
const idleTime = await evidenceCollector.waitForNetworkIdle(5000)
console.log(`Network idle after ${idleTime}ms`)
```

### Advanced Features

#### Network Activity Monitoring

Automatically captures all network requests and responses:

```typescript
// Automatic network logging includes:
// - Request/response headers
// - Status codes and timing
// - POST data and response bodies
// - Cache and service worker usage
```

#### Console Monitoring

Captures all console output including errors:

```typescript
// Automatically logs:
// - console.log, .warn, .error messages
// - JavaScript errors and exceptions
// - Stack traces and source locations
```

#### Performance Tracking

Comprehensive performance metrics:

```typescript
const metrics = await evidenceCollector.collectPerformanceMetrics('step')
// metrics.navigation.loadComplete
// metrics.navigation.domContentLoaded
// metrics.navigation.firstPaint
// metrics.navigation.firstContentfulPaint
// metrics.resources (count)
// metrics.memory (if available)
```

## 📊 Enhanced Reporting

### Report Structure

The enhanced reporter generates multiple report types:

```
test-results/
├── enhanced-report/
│   ├── index.html              # Main dashboard
│   ├── evidence-gallery.html   # Screenshot gallery
│   ├── performance.html        # Performance charts
│   ├── failures.html          # Failure analysis
│   └── assets/                 # Supporting files
├── report/                     # Standard Playwright report
└── evidence/                   # Raw evidence files
```

### Report Features

- **Dashboard Overview**: Test summary with success rates
- **Evidence Gallery**: Visual timeline of test execution
- **Performance Analytics**: Load time trends and bottlenecks
- **Failure Analysis**: Detailed error investigation
- **Cross-browser Comparison**: Results across different browsers

## 🎯 Best Practices

### Screenshot Strategy

```typescript
test('comprehensive user journey', async ({ page, evidenceCollector }) => {
  // Take screenshots at key moments
  await evidenceCollector.takeStepScreenshot('start', { fullPage: true })

  // Navigate and capture state changes
  await page.click('.login-button')
  await evidenceCollector.takeStepScreenshot('login-initiated')

  // Highlight important elements
  await evidenceCollector.takeStepScreenshot('form-validation', {
    highlight: ['.error-message', '.required-field']
  })

  // Mask sensitive data
  await evidenceCollector.takeStepScreenshot('payment-form', {
    mask: ['.credit-card-number', '.cvv']
  })
})
```

### Performance Monitoring

```typescript
test('performance critical path', async ({ page, evidenceCollector }) => {
  // Baseline metrics
  await page.goto('/')
  await evidenceCollector.collectPerformanceMetrics('initial-load')

  // After user interactions
  await page.click('.heavy-computation-button')
  await evidenceCollector.waitForNetworkIdle()
  await evidenceCollector.collectPerformanceMetrics('post-interaction')

  // Compare metrics over time
  const finalMetrics = await evidenceCollector.collectPerformanceMetrics('final-state')
})
```

### Accessibility Testing

```typescript
test('accessibility compliance', async ({ page, evidenceCollector }) => {
  await page.goto('/form')

  // Capture accessibility state
  await evidenceCollector.collectAccessibilitySnapshot('form-initial')

  // Test with error state
  await page.click('.submit-without-data')
  await evidenceCollector.collectAccessibilitySnapshot('form-with-errors')

  // Verify focus management
  await page.keyboard.press('Tab')
  await evidenceCollector.takeStepScreenshot('focus-management')
})
```

## 🔧 Configuration

### Playwright Config Updates

The enhanced evidence system requires specific configuration:

```typescript
// playwright.config.ts
export default defineConfig({
  use: {
    trace: 'on',                    // Enable traces for all tests
    screenshot: 'only-on-failure',  // Screenshot failures
    video: 'retain-on-failure',     // Video on failures
  },

  reporter: [
    ['html'],                       // Standard report
    ['./tests/e2e/utils/enhanced-reporter.ts'], // Enhanced report
  ],

  outputDir: 'test-results/evidence', // Evidence storage
})
```

### Environment Variables

Control evidence collection behavior:

```bash
# Enable/disable specific evidence types
PLAYWRIGHT_EVIDENCE_SCREENSHOTS=true
PLAYWRIGHT_EVIDENCE_PERFORMANCE=true
PLAYWRIGHT_EVIDENCE_ACCESSIBILITY=true
PLAYWRIGHT_EVIDENCE_NETWORK=true

# Report generation
PLAYWRIGHT_ENHANCED_REPORT=true
PLAYWRIGHT_REPORT_OPEN=false
```

## 📈 Evidence Analysis

### Reading Performance Data

```javascript
// Example performance analysis
const loadTimeThreshold = 2000; // 2 seconds
if (metrics.navigation.loadComplete > loadTimeThreshold) {
  console.warn(`Slow page load: ${metrics.navigation.loadComplete}ms`);
}

// Memory usage analysis
if (metrics.memory && metrics.memory.used > metrics.memory.limit * 0.8) {
  console.warn('High memory usage detected');
}
```

### Network Analysis

```javascript
// Analyze network logs
const failedRequests = networkLogs.filter(log =>
  log.type === 'response' && log.status >= 400
);

const slowRequests = networkLogs.filter(log =>
  log.type === 'response' && log.duration > 1000
);
```

### Accessibility Analysis

```javascript
// Check for common accessibility issues
const accessibilityIssues = [
  'missing alt text',
  'insufficient color contrast',
  'missing form labels',
  'improper heading hierarchy'
];
```

## 🚨 Troubleshooting

### Common Issues

#### Evidence Not Collected

```bash
# Check evidence collector initialization
# Ensure test uses the enhanced fixture
import { test } from './fixtures' // Not @playwright/test

# Verify output directory permissions
ls -la test-results/evidence/
```

#### Large Evidence Files

```typescript
// Optimize screenshot collection
await evidenceCollector.takeStepScreenshot('step', {
  fullPage: false,    // Don't capture full page
  quality: 80         // Reduce image quality
})

// Limit network logging
// Only log specific request types
```

#### Report Generation Failures

```bash
# Check report dependencies
npm list playwright

# Manual report generation
npx playwright show-report test-results/report
```

### Performance Impact

The evidence collection system is designed to be lightweight:

- Screenshots: ~50-200KB each
- Network logs: ~1-5KB per request
- Performance metrics: ~1KB per collection
- Accessibility snapshots: ~10-50KB each

Total overhead per test: ~1-5MB depending on test complexity.

## 🔗 Integration

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Run E2E tests with evidence
  run: npm run test:e2e:evidence

- name: Upload evidence artifacts
  uses: actions/upload-artifact@v3
  if: always()
  with:
    name: playwright-evidence
    path: test-results/
    retention-days: 30
```

### Monitoring Integration

```typescript
// Send metrics to monitoring system
const metrics = await evidenceCollector.generateEvidenceReport()
await sendToMonitoring(metrics.summary)
```

## 📚 Examples

See the following example files:
- `tests/e2e/enhanced-auth-flow.spec.ts` - Complete authentication flow
- `tests/e2e/performance-testing.spec.ts` - Performance monitoring
- `tests/e2e/accessibility-testing.spec.ts` - Accessibility compliance

## 🤝 Contributing

When adding new evidence collection features:

1. Update the `EvidenceCollector` class
2. Add corresponding reporter functionality
3. Update this documentation
4. Add example tests
5. Ensure CI/CD compatibility

## 📄 License

This enhanced evidence collection system is part of the Spring Boot Modulith Payment Platform and follows the same license terms.