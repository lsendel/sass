import fs from 'node:fs';
import path from 'node:path';

import type { Reporter, File, Task } from 'vitest';

interface TestSummary {
  testRunId: string;
  startTime: string;
  endTime: string;
  duration: number;
  environment: {
    node: string;
    platform: string;
    arch: string;
    ci: boolean;
    branch: string;
    commit: string;
  };
  summary: {
    total: number;
    passed: number;
    failed: number;
    skipped: number;
    duration: number;
  };
  files: Array<{
    name: string;
    duration: number;
    status: string;
    tests: number;
    passed: number;
    failed: number;
  }>;
}

/**
 * Evidence Reporter for Vitest
 *
 * Captures comprehensive test evidence including:
 * - Test execution summary
 * - Environment details
 * - File-level results
 * - Timestamps and durations
 */
export default class EvidenceReporter implements Reporter {
  private evidenceDir: string;
  private startTime: Date;
  private testRunId: string;

  constructor() {
    const dateStr = new Date().toISOString().split('T')[0];
    this.evidenceDir = path.join(
      process.cwd(),
      'test-evidence',
      dateStr
    );
    if (!fs.existsSync(this.evidenceDir)) {
      fs.mkdirSync(this.evidenceDir, { recursive: true });
    }
    this.startTime = new Date();
    this.testRunId = crypto.randomUUID();
  }

  onInit() {
    console.log('\n📊 Evidence Reporter Initialized');
    console.log(`📁 Evidence directory: ${this.evidenceDir}`);
    console.log(`🆔 Test Run ID: ${this.testRunId}\n`);
  }

  async onFinished(files: File[] = []) {
    const endTime = new Date();
    const duration = endTime.getTime() - this.startTime.getTime();

    // Calculate totals
    let totalTests = 0;
    let passed = 0;
    let failed = 0;
    let skipped = 0;

    const fileResults = files.map((file) => {
      const tests = this.countTests(file.tasks || []);
      totalTests += tests.total;
      passed += tests.passed;
      failed += tests.failed;
      skipped += tests.skipped;

      return {
        name: file.name,
        duration: file.result?.duration || 0,
        status: file.result?.state || 'unknown',
        tests: tests.total,
        passed: tests.passed,
        failed: tests.failed,
      };
    });

    const summary: TestSummary = {
      testRunId: this.testRunId,
      startTime: this.startTime.toISOString(),
      endTime: endTime.toISOString(),
      duration,
      environment: {
        node: process.version,
        platform: process.platform,
        arch: process.arch,
        ci: process.env.CI === 'true',
        branch: process.env.GITHUB_REF_NAME || process.env.BRANCH || 'unknown',
        commit: process.env.GITHUB_SHA || process.env.COMMIT_SHA || 'unknown',
      },
      summary: {
        total: totalTests,
        passed,
        failed,
        skipped,
        duration,
      },
      files: fileResults,
    };

    // Save summary JSON
    fs.writeFileSync(
      path.join(this.evidenceDir, 'test-summary.json'),
      JSON.stringify(summary, null, 2)
    );

    // Create human-readable report
    const report = this.generateTextReport(summary);
    fs.writeFileSync(
      path.join(this.evidenceDir, 'test-report.txt'),
      report
    );

    // Console output
    console.log('\n✅ Evidence Collection Complete');
    console.log(`📊 Total Tests: ${totalTests}`);
    console.log(`✅ Passed: ${passed}`);
    console.log(`❌ Failed: ${failed}`);
    console.log(`⏭️  Skipped: ${skipped}`);
    console.log(`⏱️  Duration: ${(duration / 1000).toFixed(2)}s`);
    console.log(`📁 Evidence saved: ${this.evidenceDir}\n`);
  }

  private countTests(tasks: Task[]): {
    total: number;
    passed: number;
    failed: number;
    skipped: number;
  } {
    let total = 0;
    let passed = 0;
    let failed = 0;
    let skipped = 0;

    for (const task of tasks) {
      if (task.type === 'test') {
        total++;
        if (task.result?.state === 'pass') passed++;
        else if (task.result?.state === 'fail') failed++;
        else if (task.mode === 'skip') skipped++;
      }

      if ('tasks' in task && Array.isArray((task as any).tasks)) {
        const subCounts = this.countTests((task as any).tasks);
        total += subCounts.total;
        passed += subCounts.passed;
        failed += subCounts.failed;
        skipped += subCounts.skipped;
      }
    }

    return { total, passed, failed, skipped };
  }

  private generateTextReport(summary: TestSummary): string {
    const lines = [
      '='.repeat(80),
      'TEST EXECUTION EVIDENCE REPORT',
      '='.repeat(80),
      '',
      `Test Run ID: ${summary.testRunId}`,
      `Start Time:  ${summary.startTime}`,
      `End Time:    ${summary.endTime}`,
      `Duration:    ${(summary.duration / 1000).toFixed(2)}s`,
      '',
      'ENVIRONMENT',
      '-'.repeat(80),
      `Node:        ${summary.environment.node}`,
      `Platform:    ${summary.environment.platform} (${summary.environment.arch})`,
      `CI:          ${summary.environment.ci ? 'Yes' : 'No'}`,
      `Branch:      ${summary.environment.branch}`,
      `Commit:      ${summary.environment.commit.substring(0, 8)}`,
      '',
      'TEST SUMMARY',
      '-'.repeat(80),
      `Total Tests: ${summary.summary.total}`,
      `Passed:      ${summary.summary.passed} (${((summary.summary.passed / summary.summary.total) * 100).toFixed(1)}%)`,
      `Failed:      ${summary.summary.failed} (${((summary.summary.failed / summary.summary.total) * 100).toFixed(1)}%)`,
      `Skipped:     ${summary.summary.skipped}`,
      '',
      'FILE RESULTS',
      '-'.repeat(80),
    ];

    for (const file of summary.files) {
      const passRate = file.tests > 0 ? ((file.passed / file.tests) * 100).toFixed(1) : '0.0';
      lines.push(`${file.name}`);
      lines.push(`  Tests: ${file.tests} | Passed: ${file.passed} | Failed: ${file.failed} | Pass Rate: ${passRate}%`);
      lines.push(`  Duration: ${(file.duration / 1000).toFixed(2)}s | Status: ${file.status}`);
      lines.push('');
    }

    lines.push('='.repeat(80));
    lines.push(`Generated: ${new Date().toISOString()}`);
    lines.push('='.repeat(80));

    return lines.join('\n');
  }
}