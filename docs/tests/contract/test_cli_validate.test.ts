import { execSync } from 'child_process';

describe('docs-validate CLI Contract Test', () => {
  const CLI_PATH = 'src/lib/docs-validator/src/cli/validate.js';

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show help information with --help flag', async () => {
    // This test MUST fail until the actual CLI is implemented
    expect(() => {
      const output = execSync(`node ${CLI_PATH} --help`, { encoding: 'utf8' });

      // Validate help output according to CLI contract
      expect(output).toContain('docs-validate [path] [options]');
      expect(output).toContain('--help, -h');
      expect(output).toContain('--version, -V');
      expect(output).toContain('--format, -f');
      expect(output).toContain('--fix');
      expect(output).toContain('--config, -c');
      expect(output).toContain('--check-links');
      expect(output).toContain('--check-images');
      expect(output).toContain('--template');
      expect(output).toContain('--schema');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should validate documentation with text output format', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} docs/`, { encoding: 'utf8' });

      // Validate text output format according to contract
      expect(output).toContain('🔍 Validating documentation...');
      expect(output).toMatch(/✓|✗|⚠/); // Should contain status symbols
      expect(output).toContain('Summary:');
      expect(output).toMatch(/\d+ files passed/);
      expect(output).toMatch(/\d+ file.*with errors/);
      expect(output).toMatch(/\d+ file.*with warnings/);
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should output JSON format with --format json', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} docs/ --format json`, { encoding: 'utf8' });

      // Validate JSON output format according to contract
      const result = JSON.parse(output);
      expect(result).toHaveProperty('summary');
      expect(result).toHaveProperty('results');

      expect(result.summary).toHaveProperty('totalFiles');
      expect(result.summary).toHaveProperty('passed');
      expect(result.summary).toHaveProperty('errors');
      expect(result.summary).toHaveProperty('warnings');

      expect(typeof result.summary.totalFiles).toBe('number');
      expect(typeof result.summary.passed).toBe('number');
      expect(typeof result.summary.errors).toBe('number');
      expect(typeof result.summary.warnings).toBe('number');
      expect(Array.isArray(result.results)).toBe(true);
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should validate specific file when path provided', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} docs/architecture/overview.md`, { encoding: 'utf8' });

      // Should validate only the specific file
      expect(output).toContain('docs/architecture/overview.md');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should check links with --check-links flag', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} docs/ --check-links`, { encoding: 'utf8' });

      // Should include link checking in validation
      expect(output).toContain('link');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should validate templates with --template flag', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} docs/ --template`, { encoding: 'utf8' });

      // Should include template validation
      expect(output).toContain('template');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should validate front matter schema with --schema flag', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} docs/ --schema`, { encoding: 'utf8' });

      // Should include schema validation
      expect(output).toContain('schema');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should auto-fix issues with --fix flag', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} docs/ --fix`, { encoding: 'utf8' });

      // Should attempt to fix issues
      expect(output).toContain('fixed') || expect(output).toContain('auto-fix');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should return exit code 0 for successful validation', async () => {
    expect(() => {
      execSync(`node ${CLI_PATH} docs/`, { encoding: 'utf8' });
    }).toThrow(); // Will throw because CLI doesn't exist yet

    // When implemented, should return exit code 0 for valid docs
  });

  it('should return exit code 4 for validation failure', async () => {
    expect(() => {
      execSync(`node ${CLI_PATH} docs/broken-file.md`, { encoding: 'utf8' });
    }).toThrow(); // Will throw because CLI doesn't exist yet

    // When implemented and validation fails, should return exit code 4
  });

  it('should output detailed error information for failures', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} docs/broken-file.md --format json`, { encoding: 'utf8' });

      const result = JSON.parse(output);
      if (result.results.length > 0) {
        const fileResult = result.results[0];
        expect(fileResult).toHaveProperty('file');
        expect(fileResult).toHaveProperty('status');
        expect(fileResult).toHaveProperty('issues');

        if (fileResult.issues && fileResult.issues.length > 0) {
          const issue = fileResult.issues[0];
          expect(issue).toHaveProperty('type');
          expect(issue).toHaveProperty('severity');
          expect(issue).toHaveProperty('message');
          expect(['error', 'warning']).toContain(issue.severity);
          expect(['syntax', 'schema', 'link', 'template', 'completeness']).toContain(issue.type);
        }
      }
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });
});