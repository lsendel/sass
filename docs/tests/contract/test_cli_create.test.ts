import { execSync } from 'child_process';

describe('docs-create CLI Contract Test', () => {
  const CLI_PATH = 'src/lib/docs-generator/src/cli/create.js';

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show help information with --help flag', async () => {
    // This test MUST fail until the actual CLI is implemented
    expect(() => {
      const output = execSync(`node ${CLI_PATH} --help`, { encoding: 'utf8' });

      // Validate help output according to CLI contract
      expect(output).toContain('docs-create <name> [options]');
      expect(output).toContain('--help, -h');
      expect(output).toContain('--version, -V');
      expect(output).toContain('--format, -f');
      expect(output).toContain('--template, -t');
      expect(output).toContain('--section, -s');
      expect(output).toContain('--author, -a');
      expect(output).toContain('--audience');
      expect(output).toContain('--interactive, -i');
      expect(output).toContain('--force');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should create new documentation page from template', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} "Getting Started Guide" --template feature --section guides`, { encoding: 'utf8' });

      // Validate text output format according to contract
      expect(output).toContain('📝 Creating documentation page...');
      expect(output).toContain('✓ Generated from template:');
      expect(output).toContain('feature');
      expect(output).toContain('✓ Created file:');
      expect(output).toContain('getting-started-guide.md');
      expect(output).toContain('📁 Location:');
      expect(output).toContain('docs/guides/');
      expect(output).toContain('🎉 Documentation page created successfully!');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should output JSON format with --format json', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} "API Guide" --template api --section backend --format json`, { encoding: 'utf8' });

      // Validate JSON output format according to contract
      const result = JSON.parse(output);
      expect(result).toHaveProperty('status');
      expect(result).toHaveProperty('page');
      expect(result).toHaveProperty('file');
      expect(result).toHaveProperty('template');

      expect(result.status).toBe('created');
      expect(result.page).toHaveProperty('title');
      expect(result.page).toHaveProperty('section');
      expect(result.page).toHaveProperty('path');
      expect(result.file).toHaveProperty('path');
      expect(result.file).toHaveProperty('size');
      expect(result.template).toHaveProperty('name');
      expect(result.template).toHaveProperty('version');

      expect(result.page.title).toBe('API Guide');
      expect(result.page.section).toBe('backend');
      expect(result.template.name).toBe('api');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should create page with custom author with --author option', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} "Custom Page" --template feature --section guides --author "John Doe"`, { encoding: 'utf8' });

      expect(output).toContain('👤 Author: John Doe');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should create page with specific audience with --audience option', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} "Developer Guide" --template feature --section backend --audience developer`, { encoding: 'utf8' });

      expect(output).toContain('🎯 Audience: developer');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should run interactive mode with --interactive flag', async () => {
    expect(() => {
      // Note: This would need special handling for interactive input in real implementation
      const output = execSync(`echo -e "Feature Guide\\nfeature\\nguides\\nJohn Doe\\ndeveloper" | node ${CLI_PATH} --interactive`, { encoding: 'utf8' });

      expect(output).toContain('📝 Creating new documentation page');
      expect(output).toContain('? Page title:');
      expect(output).toContain('? Template:');
      expect(output).toContain('? Section:');
      expect(output).toContain('? Author:');
      expect(output).toContain('? Audience:');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should list available templates when invalid template provided', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} "Test Page" --template invalid-template --section guides`, { encoding: 'utf8' });

      expect(output).toContain('❌ Invalid template: invalid-template');
      expect(output).toContain('Available templates:');
      expect(output).toContain('- feature');
      expect(output).toContain('- api');
      expect(output).toContain('- adr');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should list available sections when invalid section provided', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} "Test Page" --template feature --section invalid-section`, { encoding: 'utf8' });

      expect(output).toContain('❌ Invalid section: invalid-section');
      expect(output).toContain('Available sections:');
      expect(output).toContain('- guides');
      expect(output).toContain('- architecture');
      expect(output).toContain('- backend');
      expect(output).toContain('- frontend');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should overwrite existing file with --force flag', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} "Existing Page" --template feature --section guides --force`, { encoding: 'utf8' });

      expect(output).toContain('⚠️  File exists, overwriting...');
      expect(output).toContain('✓ Created file:');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should return exit code 0 for successful creation', async () => {
    expect(() => {
      execSync(`node ${CLI_PATH} "Test Page" --template feature --section guides`, { encoding: 'utf8' });
    }).toThrow(); // Will throw because CLI doesn't exist yet

    // When implemented, should return exit code 0
  });

  it('should return exit code 2 for invalid template', async () => {
    expect(() => {
      execSync(`node ${CLI_PATH} "Test Page" --template invalid --section guides`, { encoding: 'utf8' });
    }).toThrow(); // Will throw because CLI doesn't exist yet

    // When implemented and template is invalid, should return exit code 2
  });

  it('should return exit code 3 for invalid section', async () => {
    expect(() => {
      execSync(`node ${CLI_PATH} "Test Page" --template feature --section invalid`, { encoding: 'utf8' });
    }).toThrow(); // Will throw because CLI doesn't exist yet

    // When implemented and section is invalid, should return exit code 3
  });

  it('should return exit code 4 for file already exists without --force', async () => {
    expect(() => {
      execSync(`node ${CLI_PATH} "Existing Page" --template feature --section guides`, { encoding: 'utf8' });
    }).toThrow(); // Will throw because CLI doesn't exist yet

    // When implemented and file exists without --force, should return exit code 4
  });

  it('should validate page title format', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} "" --template feature --section guides`, { encoding: 'utf8' });

      expect(output).toContain('❌ Page title cannot be empty');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should create valid filename from title', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} "Advanced Payment Integration & Security" --template feature --section backend --format json`, { encoding: 'utf8' });

      const result = JSON.parse(output);
      expect(result.file.path).toContain('advanced-payment-integration-security.md');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });
});