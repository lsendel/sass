import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

const mockConsole = {
  log: vi.fn(),
  error: vi.fn(),
  warn: vi.fn(),
  info: vi.fn(),
  debug: vi.fn(),
}

const originalConsole = {
  log: console.log,
  error: console.error,
  warn: console.warn,
  info: console.info,
  debug: console.debug,
}

const loadLogger = async (isDev: boolean) => {
  vi.resetModules()
  const mod = await import('./logger')
  mod.setLoggerEnvironment(isDev)
  return mod
}

describe('logger', () => {
  beforeEach(() => {
    Object.assign(console, mockConsole)
    vi.clearAllMocks()
  })

  afterEach(async () => {
    Object.assign(console, originalConsole)
    const mod = await import('./logger')
    mod.setLoggerEnvironment(null)
    vi.resetModules()
  })

  describe('development mode behavior', () => {
    it('should log messages in development', async () => {
      const { logger } = await loadLogger(true)

      logger.log('test message', { data: 'value' })
      expect(console.log).toHaveBeenCalledWith('test message', { data: 'value' })
    })

    it('should log error messages in development', async () => {
      const { logger } = await loadLogger(true)

      const error = new Error('test')
      logger.error('error message', error)
      expect(console.error).toHaveBeenCalledWith('error message', error)
    })

    it('should log warning messages in development', async () => {
      const { logger } = await loadLogger(true)

      logger.warn('warning message', 123)
      expect(console.warn).toHaveBeenCalledWith('warning message', 123)
    })

    it('should log info messages in development', async () => {
      const { logger } = await loadLogger(true)

      logger.info('info message')
      expect(console.info).toHaveBeenCalledWith('info message')
    })

    it('should log debug messages in development', async () => {
      const { logger } = await loadLogger(true)

      logger.debug('debug message', true, null)
      expect(console.debug).toHaveBeenCalledWith('debug message', true, null)
    })

    it('should handle multiple arguments', async () => {
      const { logger } = await loadLogger(true)

      logger.log('arg1', 'arg2', 'arg3', { nested: { data: 'test' } })
      expect(console.log).toHaveBeenCalledWith('arg1', 'arg2', 'arg3', { nested: { data: 'test' } })
    })

    it('should handle no arguments', async () => {
      const { logger } = await loadLogger(true)

      logger.log()
      expect(console.log).toHaveBeenCalledWith()
    })
  })

  describe('production mode behavior', () => {
    it('should not log messages in production', async () => {
      const { logger } = await loadLogger(false)

      logger.log('test message')
      expect(console.log).not.toHaveBeenCalled()
    })

    it('should not log error messages in production', async () => {
      const { logger } = await loadLogger(false)

      logger.error('error message')
      expect(console.error).not.toHaveBeenCalled()
    })

    it('should not log warning messages in production', async () => {
      const { logger } = await loadLogger(false)

      logger.warn('warning message')
      expect(console.warn).not.toHaveBeenCalled()
    })

    it('should not log info messages in production', async () => {
      const { logger } = await loadLogger(false)

      logger.info('info message')
      expect(console.info).not.toHaveBeenCalled()
    })

    it('should not log debug messages in production', async () => {
      const { logger } = await loadLogger(false)

      logger.debug('debug message')
      expect(console.debug).not.toHaveBeenCalled()
    })
  })

  describe('logger interface', () => {
    it('should expose all expected logger methods', async () => {
      const { logger } = await loadLogger(true)

      expect(typeof logger.log).toBe('function')
      expect(typeof logger.error).toBe('function')
      expect(typeof logger.warn).toBe('function')
      expect(typeof logger.info).toBe('function')
      expect(typeof logger.debug).toBe('function')
    })

    it('should be callable with various argument types', async () => {
      const { logger } = await loadLogger(true)

      const testCases = [
        [],
        ['string'],
        [123],
        [true],
        [null],
        [undefined],
        [{ object: 'value' }],
        [['array', 'items']],
        ['mixed', 123, true, { data: 'test' }],
      ]

      testCases.forEach((args) => {
        mockConsole.log.mockClear()
        logger.log(...args)
        expect(console.log).toHaveBeenCalledWith(...args)
      })
    })
  })
})
