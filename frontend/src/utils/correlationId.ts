/**
 * Correlation ID utilities for request/error tracking.
 * Provides consistent correlation ID generation and management.
 */

let currentCorrelationId: string | null = null

/**
 * Generates a new correlation ID.
 */
export const generateCorrelationId = (): string => {
  const timestamp = Date.now().toString(36)
  const random = Math.random().toString(36).substr(2, 9)
  return `${timestamp}_${random}`
}

/**
 * Gets the current correlation ID or generates a new one.
 */
export const getCorrelationId = (): string => {
  if (!currentCorrelationId) {
    currentCorrelationId = generateCorrelationId()
  }
  return currentCorrelationId
}

/**
 * Sets a new correlation ID.
 */
export const setCorrelationId = (id: string): void => {
  currentCorrelationId = id
}

/**
 * Clears the current correlation ID.
 */
export const clearCorrelationId = (): void => {
  currentCorrelationId = null
}

/**
 * Executes a function with a specific correlation ID context.
 */
export const withCorrelationId = <T>(
  correlationId: string,
  fn: () => T
): T => {
  const previousId = currentCorrelationId
  setCorrelationId(correlationId)

  try {
    return fn()
  } finally {
    if (previousId) {
      setCorrelationId(previousId)
    } else {
      clearCorrelationId()
    }
  }
}

/**
 * Creates a correlation ID middleware for async operations.
 */
export const correlationMiddleware = (correlationId?: string) => {
  return (next: () => Promise<any>) => {
    const id = correlationId || generateCorrelationId()

    return withCorrelationId(id, () => {
      console.log(`[${id}] Starting operation`)

      return next()
        .then(result => {
          console.log(`[${id}] Operation completed successfully`)
          return result
        })
        .catch(error => {
          console.error(`[${id}] Operation failed:`, error)
          throw error
        })
    })
  }
}