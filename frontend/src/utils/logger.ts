import { getCorrelationId } from './correlationId'

/**
 * Enhanced logger with correlation ID support and structured logging.
 */

export type LogLevel = 'debug' | 'info' | 'warn' | 'error'

interface LogContext {
  correlationId?: string
  userId?: string
  sessionId?: string
  component?: string
  action?: string
  metadata?: Record<string, any>
}

interface LogEntry {
  level: LogLevel
  message: string
  timestamp: string
  correlationId: string
  context?: LogContext
  error?: {
    name: string
    message: string
    stack?: string
  }
}

class Logger {
  private logLevel: LogLevel = 'info'

  constructor() {
    // Set log level based on environment
    if (process.env.NODE_ENV === 'development') {
      this.logLevel = 'debug'
    } else if (process.env.NODE_ENV === 'test') {
      this.logLevel = 'warn'
    }
  }

  private shouldLog(level: LogLevel): boolean {
    const levels: Record<LogLevel, number> = {
      debug: 0,
      info: 1,
      warn: 2,
      error: 3,
    }

    return levels[level] >= levels[this.logLevel]
  }

  private createLogEntry(
    level: LogLevel,
    message: string,
    context?: LogContext,
    error?: Error
  ): LogEntry {
    return {
      level,
      message,
      timestamp: new Date().toISOString(),
      correlationId: context?.correlationId || getCorrelationId(),
      context,
      error: error
        ? {
            name: error.name,
            message: error.message,
            stack: error.stack,
          }
        : undefined,
    }
  }

  private formatLogMessage(entry: LogEntry): string {
    const { level, message, correlationId, timestamp } = entry
    return `[${timestamp}] [${level.toUpperCase()}] [${correlationId}] ${message}`
  }

  private sendToExternalService(entry: LogEntry): void {
    // Send to external logging service in production
    if (process.env.NODE_ENV === 'production') {
      try {
        // Example: Send to analytics or logging service
        if (typeof window !== 'undefined' && (window as any).analytics) {
          (window as any).analytics.track('Application Log', {
            ...entry,
            environment: process.env.NODE_ENV,
          })
        }
      } catch (error) {
        // Fail silently to prevent logging errors from breaking the app
        console.warn('Failed to send log to external service:', error)
      }
    }
  }

  debug(message: string, context?: LogContext): void {
    if (!this.shouldLog('debug')) return

    const entry = this.createLogEntry('debug', message, context)

    console.debug(this.formatLogMessage(entry), entry.context)
    this.sendToExternalService(entry)
  }

  info(message: string, context?: LogContext): void {
    if (!this.shouldLog('info')) return

    const entry = this.createLogEntry('info', message, context)

    console.info(this.formatLogMessage(entry), entry.context)
    this.sendToExternalService(entry)
  }

  warn(message: string, context?: LogContext): void {
    if (!this.shouldLog('warn')) return

    const entry = this.createLogEntry('warn', message, context)

    console.warn(this.formatLogMessage(entry), entry.context)
    this.sendToExternalService(entry)
  }

  error(message: string, error?: Error, context?: LogContext): void {
    if (!this.shouldLog('error')) return

    const entry = this.createLogEntry('error', message, context, error)

    console.error(this.formatLogMessage(entry), entry.context, entry.error)
    this.sendToExternalService(entry)
  }

  /**
   * Log API requests with correlation tracking.
   */
  apiRequest(
    method: string,
    url: string,
    context?: Omit<LogContext, 'action'>
  ): void {
    this.info(`API Request: ${method} ${url}`, {
      ...context,
      action: 'api_request',
    })
  }

  /**
   * Log API responses with correlation tracking.
   */
  apiResponse(
    method: string,
    url: string,
    status: number,
    duration: number,
    context?: Omit<LogContext, 'action'>
  ): void {
    const level = status >= 400 ? 'error' : status >= 300 ? 'warn' : 'info'

    this[level](`API Response: ${method} ${url} - ${status} (${duration}ms)`, {
      ...context,
      action: 'api_response',
      metadata: {
        status,
        duration,
      },
    })
  }

  /**
   * Log user actions for analytics.
   */
  userAction(action: string, context?: Omit<LogContext, 'action'>): void {
    this.info(`User Action: ${action}`, {
      ...context,
      action: 'user_action',
    })
  }

  /**
   * Log performance metrics.
   */
  performance(
    metric: string,
    value: number,
    unit: string = 'ms',
    context?: Omit<LogContext, 'action'>
  ): void {
    this.info(`Performance: ${metric} = ${value}${unit}`, {
      ...context,
      action: 'performance_metric',
      metadata: {
        metric,
        value,
        unit,
      },
    })
  }

  /**
   * Log security events.
   */
  security(event: string, context?: Omit<LogContext, 'action'>): void {
    this.warn(`Security Event: ${event}`, {
      ...context,
      action: 'security_event',
    })
  }

  /**
   * Create a child logger with preset context.
   */
  child(context: LogContext): Logger {
    const childLogger = new Logger()
    childLogger.logLevel = this.logLevel

    // Override methods to include preset context
    const originalMethods = {
      debug: childLogger.debug.bind(childLogger),
      info: childLogger.info.bind(childLogger),
      warn: childLogger.warn.bind(childLogger),
      error: childLogger.error.bind(childLogger),
    }

    childLogger.debug = (message: string, additionalContext?: LogContext) =>
      originalMethods.debug(message, { ...context, ...additionalContext })

    childLogger.info = (message: string, additionalContext?: LogContext) =>
      originalMethods.info(message, { ...context, ...additionalContext })

    childLogger.warn = (message: string, additionalContext?: LogContext) =>
      originalMethods.warn(message, { ...context, ...additionalContext })

    childLogger.error = (
      message: string,
      error?: Error,
      additionalContext?: LogContext
    ) => originalMethods.error(message, error, { ...context, ...additionalContext })

    return childLogger
  }
}

// Export singleton instance
export const logger = new Logger()

// Export class for testing
export { Logger }

// Convenience functions
export const createComponentLogger = (componentName: string) =>
  logger.child({ component: componentName })

export const createApiLogger = () =>
  logger.child({ component: 'api' })

export default logger