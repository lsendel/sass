# API Test Evidence & Reporting Plan

**Date:** 2025-09-30
**Purpose:** Comprehensive strategy for capturing, storing, and reporting API test results with industry best practices
**Priority:** High - Required for compliance, auditing, and continuous improvement

---

## 🎯 Objectives

1. **Evidence Capture**: Automatically save test results, logs, and artifacts
2. **Compliance**: Meet audit and regulatory requirements (SOC 2, ISO 27001, GDPR)
3. **Traceability**: Link tests to requirements, user stories, and bugs
4. **Visualization**: Provide clear dashboards and reports for stakeholders
5. **Historical Analysis**: Track test trends, flakiness, and quality metrics over time
6. **CI/CD Integration**: Seamless integration with automated pipelines

---

## 📋 Evidence Types & Best Practices

### 1. Test Execution Reports

**What to Capture:**
- Test name, suite, and unique identifier
- Execution timestamp (ISO 8601 format)
- Test status (passed, failed, skipped, flaky)
- Execution duration
- Environment details (OS, browser, Node version, dependencies)
- Test author and last modifier
- Retry attempts and outcomes

**Best Practices:**
```typescript
// Generate structured test reports
{
  "testRunId": "uuid-v4",
  "timestamp": "2025-09-30T12:00:00.000Z",
  "environment": {
    "node": "20.10.0",
    "os": "darwin",
    "ci": true,
    "branch": "main",
    "commit": "abc123"
  },
  "summary": {
    "total": 150,
    "passed": 145,
    "failed": 3,
    "skipped": 2,
    "duration": 5760
  },
  "tests": [...]
}
```

**Tools:**
- ✅ **Vitest HTML Reporter** - Built-in, beautiful HTML reports
- ✅ **Vitest JSON Reporter** - Machine-readable for processing
- ✅ **Vitest JUnit Reporter** - Standard XML format for CI/CD
- ⭐ **Allure Reporter** - Industry-standard, rich visualizations

---

### 2. API Request/Response Logs

**What to Capture:**
- HTTP method, URL, headers (sanitized)
- Request body (with PII redaction)
- Response status code, headers
- Response body (with sensitive data masked)
- Request/response timestamps
- Network timing (DNS, connect, TLS, transfer)
- Correlation IDs for tracing

**Best Practices:**
```typescript
// MSW request/response logger
{
  "requestId": "req-uuid",
  "correlationId": "correlation-uuid",
  "method": "POST",
  "url": "http://localhost:3000/api/v1/auth/login",
  "request": {
    "headers": {
      "content-type": "application/json",
      // Redact: Authorization, Cookie
    },
    "body": {
      "email": "***@example.com", // Masked PII
      "password": "***", // Always mask
      "organizationId": "org-123"
    }
  },
  "response": {
    "status": 200,
    "headers": {...},
    "body": {
      "user": { "id": "user-123", "email": "***@example.com" },
      "token": "***" // Always mask tokens
    }
  },
  "timing": {
    "startTime": "2025-09-30T12:00:00.000Z",
    "endTime": "2025-09-30T12:00:00.150Z",
    "duration": 150
  }
}
```

**Tools:**
- ✅ **MSW Request Logger** - Custom middleware for MSW
- ✅ **Pino Logger** - Structured JSON logging
- ✅ **Winston** - Alternative logging framework

---

### 3. Code Coverage

**What to Capture:**
- Line coverage, branch coverage, function coverage, statement coverage
- Coverage per file, module, and overall
- Uncovered lines with source code context
- Coverage trends over time
- Diff coverage (change in coverage per PR)

**Best Practices:**
```bash
# Generate comprehensive coverage reports
npm run test:coverage

# Output formats:
# - HTML (browseable reports)
# - LCOV (standard format for tools)
# - JSON (machine-readable)
# - Text (terminal summary)
```

**Tools:**
- ✅ **Vitest Coverage (v8)** - Built-in, fast
- ✅ **Codecov** - Cloud-based coverage tracking
- ✅ **Coveralls** - Alternative coverage service
- ⭐ **SonarQube** - Comprehensive code quality + coverage

**Coverage Goals:**
- **Critical Paths:** >90% coverage (auth, payment, audit)
- **Feature Code:** >80% coverage
- **Utility Code:** >70% coverage
- **Overall:** >85% coverage

---

### 4. Screenshots & Videos (E2E Tests)

**What to Capture:**
- Screenshots on test failure
- Video recording of full test execution
- Browser console logs
- Network activity (HAR files)
- DOM snapshots at failure point

**Best Practices:**
```typescript
// Playwright auto-capture on failure
test.use({
  screenshot: 'only-on-failure',
  video: 'retain-on-failure',
  trace: 'retain-on-failure',
});

// Manual capture for critical steps
await page.screenshot({
  path: `evidence/${testName}-step-${stepNumber}.png`,
  fullPage: true
});
```

**Tools:**
- ✅ **Playwright Traces** - Time-travel debugger
- ✅ **Playwright Videos** - MP4 recordings
- ✅ **Playwright Screenshots** - PNG captures

---

### 5. Performance Metrics

**What to Capture:**
- API response times (p50, p95, p99)
- Test execution time trends
- Memory usage during tests
- CPU utilization
- Flaky test identification
- Slowest tests report

**Best Practices:**
```typescript
// Performance measurement
{
  "endpoint": "/api/v1/auth/login",
  "metrics": {
    "samples": 100,
    "p50": 50,    // 50ms at 50th percentile
    "p95": 120,   // 120ms at 95th percentile
    "p99": 200,   // 200ms at 99th percentile
    "max": 350,
    "min": 30,
    "avg": 75,
    "stdDev": 45
  },
  "sla": {
    "target": 200,  // SLA target: 200ms
    "met": true
  }
}
```

**Tools:**
- ✅ **k6** - Load testing and performance
- ✅ **Artillery** - Alternative load testing
- ⭐ **Grafana + Prometheus** - Real-time metrics
- ✅ **Lighthouse CI** - Frontend performance

---

### 6. Test Artifacts & Attachments

**What to Store:**
- Test logs (stdout, stderr)
- Generated files (exports, downloads)
- Mock data snapshots
- Database state dumps (for integration tests)
- Configuration files used during test

**Best Practices:**
```typescript
// Attach artifacts to test report
test('export audit logs', async ({ page }) => {
  const downloadPath = await exportAuditLogs();

  // Attach to Allure report
  await allure.attachment('Exported CSV',
    fs.readFileSync(downloadPath),
    'text/csv'
  );
});
```

---

## 🏗️ Implementation Architecture

### Directory Structure

```
frontend/
├── test-results/              # Vitest output directory
│   ├── html/                  # HTML reports
│   ├── json/                  # JSON reports
│   └── junit/                 # JUnit XML reports
├── coverage/                  # Coverage reports
│   ├── html/                  # Browseable coverage
│   ├── lcov.info              # LCOV format
│   └── coverage-summary.json  # JSON summary
├── allure-results/            # Allure raw results
├── allure-report/             # Allure generated report
├── test-evidence/             # Custom evidence storage
│   ├── logs/                  # Test execution logs
│   │   └── YYYY-MM-DD/        # Organized by date
│   ├── requests/              # HTTP request/response logs
│   │   └── YYYY-MM-DD/
│   ├── performance/           # Performance metrics
│   │   └── YYYY-MM-DD/
│   ├── screenshots/           # Test screenshots
│   │   └── YYYY-MM-DD/
│   └── artifacts/             # Other test artifacts
│       └── YYYY-MM-DD/
└── src/
    └── test/
        ├── reporters/         # Custom reporters
        │   ├── evidenceReporter.ts
        │   ├── allureReporter.ts
        │   └── performanceReporter.ts
        └── utils/
            ├── testLogger.ts
            └── evidenceCollector.ts
```

---

## 🔧 Implementation Steps

### Phase 1: Core Reporting Infrastructure (Week 1)

#### Step 1.1: Configure Vitest Reporters

```typescript
// vitest.config.ts
export default defineConfig({
  test: {
    reporters: [
      'default',                    // Console output
      'html',                       // HTML report
      'json',                       // JSON output
      'junit',                      // JUnit XML
      './src/test/reporters/evidenceReporter.ts' // Custom
    ],
    outputFile: {
      html: './test-results/html/index.html',
      json: './test-results/json/results.json',
      junit: './test-results/junit/results.xml',
    },
    coverage: {
      enabled: true,
      provider: 'v8',
      reporter: ['html', 'lcov', 'json', 'text'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.test.{ts,tsx}',
        'src/**/*.spec.{ts,tsx}',
        'src/test/**',
      ],
      thresholds: {
        lines: 85,
        functions: 85,
        branches: 80,
        statements: 85,
      },
    },
  },
});
```

#### Step 1.2: Create Custom Evidence Reporter

```typescript
// src/test/reporters/evidenceReporter.ts
import type { Reporter } from 'vitest';
import fs from 'fs-extra';
import path from 'path';

export default class EvidenceReporter implements Reporter {
  private evidenceDir: string;
  private startTime: Date;

  constructor() {
    this.evidenceDir = path.join(
      process.cwd(),
      'test-evidence',
      new Date().toISOString().split('T')[0]
    );
    fs.ensureDirSync(this.evidenceDir);
  }

  onInit() {
    this.startTime = new Date();
    console.log('📊 Evidence Reporter initialized');
    console.log(`📁 Evidence directory: ${this.evidenceDir}`);
  }

  async onFinished(files, errors) {
    const summary = {
      testRunId: crypto.randomUUID(),
      startTime: this.startTime.toISOString(),
      endTime: new Date().toISOString(),
      duration: Date.now() - this.startTime.getTime(),
      environment: {
        node: process.version,
        platform: process.platform,
        arch: process.arch,
        ci: process.env.CI === 'true',
        branch: process.env.BRANCH || 'unknown',
        commit: process.env.COMMIT_SHA || 'unknown',
      },
      files: files.map(file => ({
        name: file.name,
        duration: file.result?.duration || 0,
        status: file.result?.state || 'unknown',
        tests: file.tasks?.length || 0,
      })),
      errors: errors?.length || 0,
    };

    // Save summary
    await fs.writeJson(
      path.join(this.evidenceDir, 'test-summary.json'),
      summary,
      { spaces: 2 }
    );

    console.log('✅ Evidence saved:', this.evidenceDir);
  }
}
```

#### Step 1.3: Implement MSW Request Logger

```typescript
// src/test/utils/mswLogger.ts
import fs from 'fs-extra';
import path from 'path';
import type { RequestHandler } from 'msw';

interface RequestLog {
  requestId: string;
  timestamp: string;
  method: string;
  url: string;
  request: {
    headers: Record<string, string>;
    body: any;
  };
  response: {
    status: number;
    headers: Record<string, string>;
    body: any;
  };
  duration: number;
}

class MSWLogger {
  private logs: RequestLog[] = [];
  private logDir: string;

  constructor() {
    this.logDir = path.join(
      process.cwd(),
      'test-evidence/requests',
      new Date().toISOString().split('T')[0]
    );
    fs.ensureDirSync(this.logDir);
  }

  log(entry: RequestLog) {
    // Sanitize sensitive data
    const sanitized = this.sanitize(entry);
    this.logs.push(sanitized);
  }

  private sanitize(entry: RequestLog): RequestLog {
    const sanitized = { ...entry };

    // Mask sensitive headers
    if (sanitized.request.headers.authorization) {
      sanitized.request.headers.authorization = '***';
    }
    if (sanitized.request.headers.cookie) {
      sanitized.request.headers.cookie = '***';
    }

    // Mask passwords in request body
    if (sanitized.request.body?.password) {
      sanitized.request.body.password = '***';
    }

    // Mask tokens in response
    if (sanitized.response.body?.token) {
      sanitized.response.body.token = '***';
    }

    // Mask PII (email)
    if (sanitized.request.body?.email) {
      sanitized.request.body.email = this.maskEmail(
        sanitized.request.body.email
      );
    }
    if (sanitized.response.body?.user?.email) {
      sanitized.response.body.user.email = this.maskEmail(
        sanitized.response.body.user.email
      );
    }

    return sanitized;
  }

  private maskEmail(email: string): string {
    const [local, domain] = email.split('@');
    return `${local[0]}***@${domain}`;
  }

  async flush() {
    const filename = `requests-${Date.now()}.json`;
    await fs.writeJson(
      path.join(this.logDir, filename),
      this.logs,
      { spaces: 2 }
    );
    console.log(`📝 Logged ${this.logs.length} requests to ${filename}`);
    this.logs = [];
  }
}

export const mswLogger = new MSWLogger();

// Middleware for MSW handlers
export function withLogging(handler: RequestHandler): RequestHandler {
  return async (info) => {
    const requestId = crypto.randomUUID();
    const startTime = Date.now();

    // Log request
    const request = {
      headers: Object.fromEntries(info.request.headers),
      body: await info.request.clone().json().catch(() => null),
    };

    // Execute handler
    const response = await handler(info);

    // Log response
    const responseBody = await response.clone().json().catch(() => null);
    const duration = Date.now() - startTime;

    mswLogger.log({
      requestId,
      timestamp: new Date().toISOString(),
      method: info.request.method,
      url: info.request.url,
      request,
      response: {
        status: response.status,
        headers: Object.fromEntries(response.headers),
        body: responseBody,
      },
      duration,
    });

    return response;
  };
}
```

---

### Phase 2: Advanced Reporting (Week 2)

#### Step 2.1: Integrate Allure Reporter

```bash
# Install dependencies
npm install -D @vitest/ui allure-commandline allure-vitest

# Generate Allure reports
npm run test -- --reporter=allure-vitest
allure generate allure-results --clean -o allure-report
allure open allure-report
```

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config';
import AllureReporter from 'allure-vitest/reporter';

export default defineConfig({
  test: {
    reporters: [
      'default',
      new AllureReporter({
        resultsDir: './allure-results',
        categories: [
          {
            name: 'Critical',
            matchedStatuses: ['failed'],
            messageRegex: '.*auth.*|.*payment.*',
          },
        ],
      }),
    ],
  },
});
```

#### Step 2.2: Performance Monitoring

```typescript
// src/test/utils/performanceMonitor.ts
import fs from 'fs-extra';
import path from 'path';

interface PerformanceMetric {
  endpoint: string;
  method: string;
  samples: number[];
  timestamp: string;
}

class PerformanceMonitor {
  private metrics: Map<string, number[]> = new Map();
  private metricsDir: string;

  constructor() {
    this.metricsDir = path.join(
      process.cwd(),
      'test-evidence/performance',
      new Date().toISOString().split('T')[0]
    );
    fs.ensureDirSync(this.metricsDir);
  }

  record(endpoint: string, duration: number) {
    if (!this.metrics.has(endpoint)) {
      this.metrics.set(endpoint, []);
    }
    this.metrics.get(endpoint)!.push(duration);
  }

  async generateReport() {
    const report = Array.from(this.metrics.entries()).map(([endpoint, samples]) => {
      const sorted = [...samples].sort((a, b) => a - b);
      const p50 = sorted[Math.floor(samples.length * 0.5)];
      const p95 = sorted[Math.floor(samples.length * 0.95)];
      const p99 = sorted[Math.floor(samples.length * 0.99)];
      const avg = samples.reduce((a, b) => a + b, 0) / samples.length;

      return {
        endpoint,
        metrics: {
          samples: samples.length,
          p50,
          p95,
          p99,
          min: Math.min(...samples),
          max: Math.max(...samples),
          avg: Math.round(avg),
        },
        sla: {
          target: 200,
          met: p95 < 200,
        },
      };
    });

    await fs.writeJson(
      path.join(this.metricsDir, `performance-${Date.now()}.json`),
      report,
      { spaces: 2 }
    );

    return report;
  }
}

export const performanceMonitor = new PerformanceMonitor();
```

---

### Phase 3: CI/CD Integration (Week 3)

#### Step 3.1: GitHub Actions Workflow

```yaml
# .github/workflows/api-tests.yml
name: API Tests with Evidence

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main]

jobs:
  api-tests:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Run API tests
        run: npm run test:api
        env:
          CI: true
          BRANCH: ${{ github.ref_name }}
          COMMIT_SHA: ${{ github.sha }}

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: |
            test-results/
            coverage/
            test-evidence/
          retention-days: 30

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          files: ./coverage/lcov.info
          flags: frontend-api

      - name: Generate Allure Report
        if: always()
        run: |
          npm run allure:generate
          npm run allure:report

      - name: Deploy Allure Report to GitHub Pages
        if: always() && github.ref == 'refs/heads/main'
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./allure-report
          destination_dir: allure/${{ github.run_number }}

      - name: Comment PR with results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const summary = JSON.parse(
              fs.readFileSync('test-evidence/test-summary.json', 'utf8')
            );

            const body = `
            ## 🧪 API Test Results

            - **Total Tests:** ${summary.totalTests}
            - **Passed:** ✅ ${summary.passed}
            - **Failed:** ❌ ${summary.failed}
            - **Duration:** ${summary.duration}ms

            [View Full Report](https://your-org.github.io/your-repo/allure/${context.runNumber})
            `;

            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: body
            });
```

---

## 📊 Reporting Dashboards

### 1. Local Development Dashboard

```bash
# View HTML report
npm run test:report

# View Allure report
npm run allure:open

# View coverage
npm run coverage:open
```

### 2. CI/CD Dashboard

- **GitHub Actions:** Test results in Actions tab
- **Codecov:** Coverage trends and PR comments
- **Allure Report:** Deployed to GitHub Pages
- **SonarQube:** Code quality + coverage analysis

### 3. Custom Dashboard (Optional)

```typescript
// Build custom dashboard with:
- React + TypeScript
- Chart.js / Recharts for visualizations
- Parse test-evidence JSON files
- Display:
  * Test trends over time
  * Flaky test detection
  * Coverage heatmaps
  * Performance metrics
  * Failure analysis
```

---

## 🔐 Security & Compliance

### Data Privacy
- ✅ Always redact PII (emails, names, addresses)
- ✅ Mask all credentials (passwords, tokens, API keys)
- ✅ Sanitize logs before storage
- ✅ Implement data retention policies (30-90 days)

### Access Control
- ✅ Restrict access to test evidence (internal only)
- ✅ Use GitHub artifact retention settings
- ✅ Encrypt evidence at rest (if storing externally)

### Compliance
- ✅ GDPR: No personal data in test evidence
- ✅ SOC 2: Audit trail of test executions
- ✅ ISO 27001: Evidence retention and archival

---

## 📈 Success Metrics

### Quality Metrics
- **Test Pass Rate:** Target >98%
- **Code Coverage:** Target >85%
- **Flaky Test Rate:** Target <2%
- **Test Execution Time:** Target <6 minutes

### Evidence Metrics
- **Evidence Capture Rate:** 100% of test runs
- **Report Generation Time:** <2 minutes
- **Storage Size:** Monitor and optimize
- **Report Access Time:** <30 seconds

---

## 🚀 Quick Start Commands

```bash
# Run tests with full evidence collection
npm run test:evidence

# Generate all reports
npm run reports:generate

# Open reports locally
npm run reports:open

# Clean old evidence (>30 days)
npm run evidence:clean

# Export evidence for audit
npm run evidence:export -- --from=2025-09-01 --to=2025-09-30
```

---

## 📦 Package.json Scripts

```json
{
  "scripts": {
    "test:api": "vitest run src/test/api/",
    "test:api:watch": "vitest watch src/test/api/",
    "test:evidence": "vitest run --reporter=html --reporter=json --reporter=junit --reporter=./src/test/reporters/evidenceReporter.ts",
    "test:coverage": "vitest run --coverage",
    "test:report": "open test-results/html/index.html",
    "coverage:open": "open coverage/html/index.html",
    "allure:generate": "allure generate allure-results --clean",
    "allure:report": "allure generate allure-results --clean -o allure-report",
    "allure:open": "allure open allure-report",
    "reports:generate": "npm run test:evidence && npm run allure:generate",
    "reports:open": "npm run test:report && npm run coverage:open && npm run allure:open",
    "evidence:clean": "node scripts/cleanEvidence.js",
    "evidence:export": "node scripts/exportEvidence.js"
  }
}
```

---

## 🎓 Best Practices Summary

1. **Automate Everything:** No manual evidence collection
2. **Sanitize Sensitive Data:** Always redact PII and credentials
3. **Multiple Formats:** HTML (human), JSON (machine), JUnit (CI)
4. **Historical Tracking:** Trend analysis and regression detection
5. **Fast Feedback:** Reports available within minutes of test completion
6. **Compliance First:** Design for audit requirements from day one
7. **Storage Management:** Implement retention policies and archival
8. **Accessibility:** Make reports easy to find and navigate
9. **Integration:** Seamless CI/CD and tool integration
10. **Continuous Improvement:** Regularly review and enhance reporting

---

**Last Updated:** 2025-09-30
**Status:** 📋 Planning Complete - Ready for Implementation
**Next:** Implement Phase 1 (Core Reporting Infrastructure)