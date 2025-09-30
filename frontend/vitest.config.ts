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
  coverage: {
    provider: 'v8',
    reporter: ['text', 'lcov', 'html'],
    reportsDirectory: 'coverage',
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
