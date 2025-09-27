import path from 'path'

import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import istanbul from 'vite-plugin-istanbul'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      react(),
      // Enable instrumentation when VITE_COVERAGE=true
      istanbul({
        include: ['src/**/*.ts', 'src/**/*.tsx'],
        exclude: ['src/test/**/*', '**/__tests__/**'],
        extension: ['.ts', '.tsx'],
        requireEnv: true,
        cypress: false,
      }),
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: 3000,
      host: env.VITE_HOST ? true : false,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
        },
      },
    },
    // Advanced Bundle Splitting for Performance Optimization
    build: {
      target: 'esnext',
      minify: 'terser',
      sourcemap: mode === 'development',
      rollupOptions: {
        output: {
          // Advanced chunk splitting strategy
          manualChunks: (id) => {
            // Vendor chunks - separate by library type
            if (id.includes('node_modules')) {
              // React ecosystem
              if (id.includes('@reduxjs') || id.includes('redux') || id.includes('react-redux')) {
                return 'vendor-redux'
              }
              if (id.includes('react') || id.includes('react-dom') || id.includes('react-router')) {
                return 'vendor-react'
              }
              // UI Libraries
              if (id.includes('@headlessui') || id.includes('@heroicons') || id.includes('tailwindcss')) {
                return 'vendor-ui'
              }
              // Form handling
              if (id.includes('react-hook-form') || id.includes('zod') || id.includes('@hookform')) {
                return 'vendor-forms'
              }
              // Payment processing
              if (id.includes('@stripe')) {
                return 'vendor-stripe'
              }
              // Utilities
              if (id.includes('date-fns') || id.includes('clsx') || id.includes('lodash')) {
                return 'vendor-utils'
              }
              // Testing libraries (shouldn't be in production but just in case)
              if (id.includes('vitest') || id.includes('@testing-library') || id.includes('playwright')) {
                return 'vendor-test'
              }
              // Default vendor chunk for other dependencies
              return 'vendor-other'
            }

            // Feature-based chunks for our application code
            if (id.includes('/src/')) {
              // Authentication module
              if (id.includes('/auth/') || id.includes('Auth')) {
                return 'feature-auth'
              }
              // Payment module
              if (id.includes('/payment') || id.includes('Payment')) {
                return 'feature-payments'
              }
              // Organization module
              if (id.includes('/organization') || id.includes('Organization')) {
                return 'feature-organizations'
              }
              // Subscription module
              if (id.includes('/subscription') || id.includes('Subscription')) {
                return 'feature-subscriptions'
              }
              // Dashboard module
              if (id.includes('/dashboard') || id.includes('Dashboard')) {
                return 'feature-dashboard'
              }
              // UI components
              if (id.includes('/components/ui/')) {
                return 'ui-components'
              }
              // Store/API layer
              if (id.includes('/store/') || id.includes('/api/')) {
                return 'store-api'
              }
              // Utilities and hooks
              if (id.includes('/utils/') || id.includes('/hooks/')) {
                return 'utils-hooks'
              }
            }
          },
        },
      },
      // Optimize chunk sizes
      chunkSizeWarningLimit: 1000,
      // Enable CSS code splitting
      cssCodeSplit: true,
    },

    // Performance optimization
    optimizeDeps: {
      include: [
        'react',
        'react-dom',
        'react-router-dom',
        '@reduxjs/toolkit',
        'react-redux',
        '@headlessui/react',
        'react-hook-form',
        'zod',
        'date-fns',
        'clsx',
      ],
      exclude: ['@stripe/stripe-js'], // Load Stripe dynamically
    },

    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
      coverage: {
        provider: 'v8',
        reporter: ['text', 'json', 'html'],
        exclude: [
          'node_modules/',
          'src/test/',
          'tests/',
          '**/*.d.ts',
          '**/*.config.*',
          '**/coverage/**',
          'src/main.tsx', // Entry point
          '**/*.test.{ts,tsx}',
          '**/*.spec.{ts,tsx}',
        ],
        include: ['src/**/*.{ts,tsx}'],
        // Enforce 85% coverage threshold
        thresholds: {
          global: {
            branches: 85,
            functions: 85,
            lines: 85,
            statements: 85,
          },
        },
        // Additional coverage configuration
        watermarks: {
          statements: [85, 95],
          functions: [85, 95],
          branches: [85, 95],
          lines: [85, 95],
        },
      },
    },
  }
})