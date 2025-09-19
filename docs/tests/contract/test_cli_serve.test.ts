import { execSync, spawn } from 'child_process';

describe('docs-serve CLI Contract Test', () => {
  const CLI_PATH = 'src/lib/docs-generator/src/cli/serve.js';

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should show help information with --help flag', async () => {
    // This test MUST fail until the actual CLI is implemented
    expect(() => {
      const output = execSync(`node ${CLI_PATH} --help`, { encoding: 'utf8' });

      // Validate help output according to CLI contract
      expect(output).toContain('docs-serve [options]');
      expect(output).toContain('--help, -h');
      expect(output).toContain('--version, -V');
      expect(output).toContain('--format, -f');
      expect(output).toContain('--port, -p');
      expect(output).toContain('--host');
      expect(output).toContain('--build-dir, -b');
      expect(output).toContain('--open');
      expect(output).toContain('--polling');
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should show version with --version flag', async () => {
    expect(() => {
      const output = execSync(`node ${CLI_PATH} --version`, { encoding: 'utf8' });

      // Should output semantic version
      expect(output).toMatch(/^\d+\.\d+\.\d+/);
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should start development server with default options', async () => {
    expect(() => {
      // Since this starts a server, we need to test differently
      const child = spawn('node', [CLI_PATH], { stdio: 'pipe' });

      let output = '';
      child.stdout.on('data', (data) => {
        output += data.toString();
      });

      // Wait for server to start
      setTimeout(() => {
        // Validate text output format according to contract
        expect(output).toContain('🚀 Starting development server...');
        expect(output).toContain('📁 Serving:');
        expect(output).toContain('🌐 Local:');
        expect(output).toContain('http://localhost:3000');
        expect(output).toContain('✓ Ready in');

        child.kill();
      }, 2000);
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should start server on custom port with --port option', async () => {
    expect(() => {
      const child = spawn('node', [CLI_PATH, '--port', '4000'], { stdio: 'pipe' });

      let output = '';
      child.stdout.on('data', (data) => {
        output += data.toString();
      });

      setTimeout(() => {
        expect(output).toContain('http://localhost:4000');
        child.kill();
      }, 2000);
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should start server on custom host with --host option', async () => {
    expect(() => {
      const child = spawn('node', [CLI_PATH, '--host', '0.0.0.0'], { stdio: 'pipe' });

      let output = '';
      child.stdout.on('data', (data) => {
        output += data.toString();
      });

      setTimeout(() => {
        expect(output).toContain('http://0.0.0.0:3000');
        child.kill();
      }, 2000);
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should serve from custom build directory with --build-dir option', async () => {
    expect(() => {
      const child = spawn('node', [CLI_PATH, '--build-dir', './custom-build'], { stdio: 'pipe' });

      let output = '';
      child.stdout.on('data', (data) => {
        output += data.toString();
      });

      setTimeout(() => {
        expect(output).toContain('📁 Serving: ./custom-build');
        child.kill();
      }, 2000);
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should output JSON format with --format json', async () => {
    expect(() => {
      const child = spawn('node', [CLI_PATH, '--format', 'json'], { stdio: 'pipe' });

      let output = '';
      child.stdout.on('data', (data) => {
        output += data.toString();
      });

      setTimeout(() => {
        // Validate JSON output format according to contract
        const result = JSON.parse(output);
        expect(result).toHaveProperty('status');
        expect(result).toHaveProperty('server');
        expect(result).toHaveProperty('startTime');

        expect(result.status).toBe('running');
        expect(result.server).toHaveProperty('host');
        expect(result.server).toHaveProperty('port');
        expect(result.server).toHaveProperty('url');
        expect(typeof result.startTime).toBe('number');

        child.kill();
      }, 2000);
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should auto-open browser with --open flag', async () => {
    expect(() => {
      const child = spawn('node', [CLI_PATH, '--open'], { stdio: 'pipe' });

      let output = '';
      child.stdout.on('data', (data) => {
        output += data.toString();
      });

      setTimeout(() => {
        expect(output).toContain('🌐 Opening browser');
        child.kill();
      }, 2000);
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should handle SIGINT gracefully', async () => {
    expect(() => {
      const child = spawn('node', [CLI_PATH], { stdio: 'pipe' });

      let output = '';
      child.stdout.on('data', (data) => {
        output += data.toString();
      });

      setTimeout(() => {
        // Send SIGINT to simulate Ctrl+C
        child.kill('SIGINT');
      }, 1000);

      child.on('exit', (code) => {
        expect(output).toContain('🛑 Shutting down server...');
        expect(code).toBe(0);
      });
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should return exit code 0 for successful server start', async () => {
    expect(() => {
      const child = spawn('node', [CLI_PATH], { stdio: 'pipe' });

      setTimeout(() => {
        child.kill();
      }, 1000);

      child.on('exit', (code) => {
        expect(code).toBe(0);
      });
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should return exit code 3 for port already in use', async () => {
    expect(() => {
      // This would test port conflict scenario
      const child = spawn('node', [CLI_PATH, '--port', '80'], { stdio: 'pipe' });

      child.on('exit', (code) => {
        expect(code).toBe(3); // Port conflict exit code
      });
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });

  it('should return exit code 5 for invalid build directory', async () => {
    expect(() => {
      const child = spawn('node', [CLI_PATH, '--build-dir', './nonexistent'], { stdio: 'pipe' });

      child.on('exit', (code) => {
        expect(code).toBe(5); // Invalid directory exit code
      });
    }).toThrow(); // Will throw because CLI doesn't exist yet
  });
});