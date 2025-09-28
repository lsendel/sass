import '@testing-library/jest-dom'
import { expect, afterEach, beforeAll, afterAll } from 'vitest'
import { cleanup } from '@testing-library/react'
import * as matchers from '@testing-library/jest-dom/matchers'

import { handlers } from './mocks/handlers'

// Extend Vitest's expect with jest-dom matchers
expect.extend(matchers)

type SetupServer = Awaited<ReturnType<typeof import('msw/node')['setupServer']>>

interface Lifecycle {
  setup: () => Promise<void> | void
  reset: () => void | Promise<void>
  teardown: () => Promise<void> | void
}

let serverInstance: SetupServer | undefined

declare global {
   
  var __MSW_SERVER__: SetupServer | undefined
}

// More reliable runtime detection
const isNodeRuntime = typeof process !== 'undefined' &&
                     !!process.versions?.node &&
                     typeof window === 'undefined'

// Comprehensive browser test detection
const isBrowserTest = typeof process !== 'undefined' && (
  process.env?.VITEST_BROWSER === 'true' ||
  process.env?.VITEST_BROWSER === '1' ||
  typeof window !== 'undefined'
)

// Check if we're in a browser environment at module level
const isBrowserEnvironment = typeof window !== 'undefined' || typeof globalThis !== 'undefined' && 'window' in globalThis

// Use sync initialization to avoid top-level await
const createLifecycle = (): Lifecycle => {
  // Skip MSW setup completely in browser environments
  if (!isNodeRuntime || isBrowserTest || isBrowserEnvironment) {
    return {
      setup: () => undefined,
      reset: () => undefined,
      teardown: () => undefined,
    }
  }

  let setupPromise: Promise<void> | null = null

  return {
    setup: async () => {
      if (!setupPromise) {
        setupPromise = (async () => {
          try {
            // Triple-check runtime environment before importing MSW to prevent browser imports
            if (typeof window !== 'undefined' ||
                typeof globalThis !== 'undefined' && 'window' in globalThis ||
                process.env?.VITEST_BROWSER) {
              if (process.env.NODE_ENV === 'test') {
                console.warn('Skipping MSW setup in browser environment')
              }
              return
            }

            // Use conditional import to avoid static analysis in browser environments
            const importMSW = new Function('return import("msw/node")')
            const mswModule = await importMSW()
            serverInstance = globalThis.__MSW_SERVER__ ?? mswModule.setupServer(...handlers)
            globalThis.__MSW_SERVER__ = serverInstance
            serverInstance.listen({ onUnhandledRequest: 'error' })
          } catch (error) {
            if (process.env.NODE_ENV === 'test') {
              console.warn('MSW server unavailable in this environment. Falling back to no-op handlers.', error)
            }
          }
        })()
      }
      await setupPromise
    },
    reset: () => {
      if (serverInstance) {
        serverInstance.resetHandlers()
      }
    },
    teardown: () => {
      if (serverInstance) {
        serverInstance.close()
      }
    },
  }
}

const lifecycle = createLifecycle()

const fallbackServer = (() => {
  const warn = () => {
    if (process.env.NODE_ENV === 'test') {
      console.warn('Attempted to use MSW server in an environment where it is not initialized.')
    }
  }

  return {
    listen: warn,
    resetHandlers: warn,
    close: warn,
    use: warn,
  } as unknown as SetupServer
})()

export const server = globalThis.__MSW_SERVER__ ?? fallbackServer

beforeAll(async () => {
  await lifecycle.setup()
})

afterEach(async () => {
  await lifecycle.reset()
  cleanup()
})

afterAll(async () => {
  await lifecycle.teardown()
})

// Mock IntersectionObserver
;(globalThis as any).IntersectionObserver = class IntersectionObserver {
  constructor() {}
  observe() {
    return null
  }
  disconnect() {
    return null
  }
  unobserve() {
    return null
  }
}

// Mock ResizeObserver
;(globalThis as any).ResizeObserver = class ResizeObserver {
  constructor() {}
  observe() {
    return null
  }
  disconnect() {
    return null
  }
  unobserve() {
    return null
  }
}

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => {},
  }),
})

// Mock scrollTo
Object.defineProperty(window, 'scrollTo', {
  writable: true,
  value: () => {},
})

// Mock crypto.randomUUID
Object.defineProperty(globalThis, 'crypto', {
  value: {
    randomUUID: () => Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)
  }
})

// Mock fetch for tests that don't use MSW
;((globalThis as any).fetch) = (globalThis as any).fetch || (() => Promise.resolve({ json: () => Promise.resolve({}) } as Response))

// Console error suppression for known testing issues
const originalError = console.error
beforeAll(() => {
  console.error = (...args: any[]) => {
    if (
      typeof args[0] === 'string' &&
      (args[0].includes('Warning: ReactDOM.render is deprecated') ||
       args[0].includes('Warning: validateDOMNesting'))
    ) {
      return
    }
    originalError.call(console, ...args)
  }
})

afterAll(() => {
  console.error = originalError
})

// Set test environment variables
if (typeof process !== 'undefined' && process.env) {
  process.env.NODE_ENV = 'test'
  process.env.VITE_API_BASE_URL = 'http://localhost:3000/api'
}
