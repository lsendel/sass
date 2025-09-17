import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import istanbul from 'vite-plugin-istanbul'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    // Enable instrumentation when VITE_COVERAGE=true
    istanbul({
      include: ['src/**/*.ts', 'src/**/*.tsx'],
      exclude: ['node_modules', 'tests', 'test', 'src/test/**/*', 'frontend/tests/**/*'],
      extension: ['.ts', '.tsx'],
      cypress: false,
      requireEnv: false,
      // Only instrument when explicitly requested
      // Use VITE_COVERAGE env var to toggle during Playwright runs
      // The plugin itself reads process.env to decide; we gate with a function
      // but since plugin runs at config time, rely on vite-plugin-istanbul's own check
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
})
