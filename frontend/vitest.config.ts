/// <reference types="vitest/config" />
import path from 'path'
import { fileURLToPath } from 'node:url'

import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { storybookTest } from '@storybook/addon-vitest/vitest-plugin'
const dirname =
  typeof __dirname !== 'undefined'
    ? __dirname
    : path.dirname(fileURLToPath(import.meta.url))

// More info at: https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon
const enableStorybookProject = false // Temporarily disabled to focus on unit tests

const testConfig: Parameters<typeof defineConfig>[0]['test'] = {
  globals: true,
  environment: 'jsdom',
  setupFiles: ['./src/test/setup.ts'],
  env: {
    VITE_API_BASE_URL: 'http://localhost:3000/api/v1',
  },
  reporters: [
    'default',
    'html',
    'json',
    'junit',
    './src/test/reporters/evidenceReporter.ts',
  ],
  outputFile: {
    html: './test-results/html/index.html',
    json: './test-results/json/results.json',
    junit: './test-results/junit/results.xml',
  },
  coverage: {
    provider: 'v8',
    reporter: ['text', 'lcov', 'html', 'json'],
    reportsDirectory: 'coverage',
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
  exclude: [
    '**/node_modules/**',
    '**/dist/**',
    '**/cypress/**',
    '**/tests/e2e/**',
    // Exclude Playwright E2E tests
    '**/.{idea,git,cache,output,temp}/**',
    '**/{karma,rollup,webpack,vite,vitest,jest,ava,babel,nyc,cypress,tsup,build}.config.*',
  ],
  include: ['src/**/*.{test,spec}.{ts,tsx}'],
  // Parallel execution configuration
  pool: 'threads',
  poolOptions: {
    threads: {
      singleThread: false,
      maxThreads: 6, // Use 6 threads for 12-core system (optimal for I/O bound tests)
      minThreads: 2,
      useAtomics: true, // Enable faster communication between threads
    },
  },
  maxConcurrency: 5, // Max concurrent tests per worker
  fileParallelism: true, // Run test files in parallel
  isolate: true, // Isolate test contexts (default, but explicit for clarity)
  // Performance optimizations
  testTimeout: 10000, // 10 second timeout for tests
  hookTimeout: 10000, // 10 second timeout for hooks
  teardownTimeout: 5000, // 5 second timeout for teardown
  // Sequence settings for better parallel execution
  sequence: {
    shuffle: false, // Keep test order deterministic
    concurrent: false, // Run tests within a file sequentially (safer for MSW)
  },
}

if (enableStorybookProject) {
  testConfig.projects = [
    {
      extends: true,
      plugins: [
        // The plugin will run tests for the stories defined in your Storybook config
        // See options at: https://storybook.js.org/docs/next/writing-tests/integrations/vitest-addon#storybooktest
        storybookTest({
          configDir: path.join(dirname, '.storybook'),
        }),
      ],
      test: {
        name: 'storybook',
        browser: {
          enabled: true,
          headless: true,
          provider: 'playwright',
          instances: [
            {
              browser: 'chromium',
            },
          ],
        },
        setupFiles: ['.storybook/vitest.setup.ts'],
      },
    },
  ]
}

export default defineConfig({
  plugins: [react()],
  test: testConfig,
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
