import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// Mock console methods
const mockConsole = {
  log: vi.fn(),
  error: vi.fn(),
  warn: vi.fn(),
  info: vi.fn(),
  debug: vi.fn()
}

// Store original console
const originalConsole = {
  log: console.log,
  error: console.error,
  warn: console.warn,
  info: console.info,
  debug: console.debug
}

describe('logger', () => {
  beforeEach(() => {
    // Mock console methods
    Object.assign(console, mockConsole)
    vi.clearAllMocks()
  })

  afterEach(() => {
    // Restore original console
    Object.assign(console, originalConsole)
  })

  describe('development mode behavior', () => {
    beforeEach(() => {
      // Mock import.meta.env.DEV to be true
      vi.stubGlobal('import.meta', { env: { DEV: true } })
    })

    it('should log messages in development', async () => {
      // Dynamically import to get fresh module with mocked env
      const { logger } = await import('./logger')

      logger.log('test message', { data: 'value' })
      expect(console.log).toHaveBeenCalledWith('test message', { data: 'value' })
    })

    it('should log error messages in development', async () => {
      const { logger } = await import('./logger')

      logger.error('error message', new Error('test'))
      expect(console.error).toHaveBeenCalledWith('error message', new Error('test'))
    })

    it('should log warning messages in development', async () => {
      const { logger } = await import('./logger')

      logger.warn('warning message', 123)
      expect(console.warn).toHaveBeenCalledWith('warning message', 123)
    })

    it('should log info messages in development', async () => {
      const { logger } = await import('./logger')

      logger.info('info message')
      expect(console.info).toHaveBeenCalledWith('info message')
    })

    it('should log debug messages in development', async () => {
      const { logger } = await import('./logger')

      logger.debug('debug message', true, null)
      expect(console.debug).toHaveBeenCalledWith('debug message', true, null)
    })

    it('should handle multiple arguments', async () => {
      const { logger } = await import('./logger')

      logger.log('arg1', 'arg2', 'arg3', { nested: { data: 'test' } })
      expect(console.log).toHaveBeenCalledWith('arg1', 'arg2', 'arg3', { nested: { data: 'test' } })
    })

    it('should handle no arguments', async () => {
      const { logger } = await import('./logger')

      logger.log()
      expect(console.log).toHaveBeenCalledWith()
    })
  })

  describe('production mode behavior', () => {
    beforeEach(() => {
      // Mock import.meta.env.DEV to be false for production
      vi.stubGlobal('import.meta', { env: { DEV: false } })
    })

    it('should not log messages in production', async () => {
      // Clear module cache to get fresh import with new env
      vi.resetModules()
      const { logger } = await import('./logger')

      logger.log('test message')
      expect(console.log).not.toHaveBeenCalled()
    })

    it('should not log error messages in production', async () => {
      vi.resetModules()
      const { logger } = await import('./logger')

      logger.error('error message')
      expect(console.error).not.toHaveBeenCalled()
    })

    it('should not log warning messages in production', async () => {
      vi.resetModules()
      const { logger } = await import('./logger')

      logger.warn('warning message')
      expect(console.warn).not.toHaveBeenCalled()
    })

    it('should not log info messages in production', async () => {
      vi.resetModules()
      const { logger } = await import('./logger')

      logger.info('info message')
      expect(console.info).not.toHaveBeenCalled()
    })

    it('should not log debug messages in production', async () => {
      vi.resetModules()
      const { logger } = await import('./logger')

      logger.debug('debug message')
      expect(console.debug).not.toHaveBeenCalled()
    })
  })

  describe('logger interface', () => {
    beforeEach(() => {
      vi.stubGlobal('import.meta', { env: { DEV: true } })
    })

    it('should expose all expected logger methods', async () => {
      const { logger } = await import('./logger')

      expect(typeof logger.log).toBe('function')
      expect(typeof logger.error).toBe('function')
      expect(typeof logger.warn).toBe('function')
      expect(typeof logger.info).toBe('function')
      expect(typeof logger.debug).toBe('function')
    })

    it('should be callable with various argument types', async () => {
      const { logger } = await import('./logger')

      const testCases = [
        [],
        ['string'],
        [123],
        [true],
        [null],
        [undefined],
        [{ object: 'value' }],
        [['array', 'items']],
        ['mixed', 123, true, { data: 'test' }]
      ]

      testCases.forEach((args, index) => {
        mockConsole.log.mockClear()
        logger.log(...args)
        expect(console.log).toHaveBeenCalledWith(...args)
      })
    })
  })
})