import React, { Component, ErrorInfo, ReactNode } from 'react'
import { ExclamationTriangleIcon } from '@heroicons/react/24/outline'

interface Props {
  children: ReactNode
  fallback?: ReactNode
  onError?: (error: Error, errorInfo: ErrorInfo) => void
  resetOnPropsChange?: boolean
  resetKeys?: Array<string | number>
}

interface State {
  hasError: boolean
  error?: Error
  errorInfo?: ErrorInfo
  eventId?: string
}

/**
 * Enhanced Error Boundary with logging correlation and better UX.
 * Features:
 * - Correlation ID generation for tracking
 * - Detailed error logging
 * - Customizable fallback UI
 * - Error recovery mechanisms
 * - Performance monitoring integration
 */
export class EnhancedErrorBoundary extends Component<Props, State> {
  private resetTimeoutId: number | null = null

  constructor(props: Props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(error: Error): State {
    // Generate correlation ID for error tracking
    const eventId = `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

    return {
      hasError: true,
      error,
      eventId,
    }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    const { onError } = this.props
    const { eventId } = this.state

    // Enhanced error logging with correlation
    this.logErrorWithCorrelation(error, errorInfo, eventId)

    // Call custom error handler if provided
    onError?.(error, errorInfo)

    // Update state with error info
    this.setState({ errorInfo })
  }

  componentDidUpdate(prevProps: Props) {
    const { resetOnPropsChange, resetKeys } = this.props
    const { hasError } = this.state

    // Reset error boundary when specified props change
    if (hasError && prevProps.resetKeys !== resetKeys && resetOnPropsChange) {
      if (resetKeys?.some((key, idx) => prevProps.resetKeys?.[idx] !== key)) {
        this.resetErrorBoundary()
      }
    }
  }

  componentWillUnmount() {
    if (this.resetTimeoutId) {
      clearTimeout(this.resetTimeoutId)
    }
  }

  private logErrorWithCorrelation = (
    error: Error,
    errorInfo: ErrorInfo,
    eventId?: string
  ) => {
    const correlationId = eventId || `error_${Date.now()}`

    // Structured error logging
    const errorData = {
      correlationId,
      timestamp: new Date().toISOString(),
      error: {
        name: error.name,
        message: error.message,
        stack: error.stack,
      },
      errorInfo: {
        componentStack: errorInfo.componentStack,
      },
      userAgent: navigator.userAgent,
      url: window.location.href,
      userId: this.getUserId(),
      sessionId: this.getSessionId(),
    }

    // Log to console with correlation ID
    console.group(`🚨 Error Boundary [${correlationId}]`)
    console.error('Error:', error)
    console.error('Error Info:', errorInfo)
    console.error('Full Context:', errorData)
    console.groupEnd()

    // Send to error reporting service (if available)
    this.reportError(errorData)
  }

  private getUserId = (): string | null => {
    try {
      // Try to get user ID from Redux store or localStorage
      const state = (window as any).__REDUX_STORE__?.getState?.()
      return state?.auth?.user?.id || localStorage.getItem('userId') || null
    } catch {
      return null
    }
  }

  private getSessionId = (): string | null => {
    try {
      return sessionStorage.getItem('sessionId') || null
    } catch {
      return null
    }
  }

  private reportError = (errorData: any) => {
    try {
      // Report to external service (Sentry, LogRocket, etc.)
      if (typeof window !== 'undefined' && (window as any).Sentry) {
        (window as any).Sentry.captureException(errorData.error, {
          tags: {
            component: 'ErrorBoundary',
            correlationId: errorData.correlationId,
          },
          extra: errorData,
        })
      }

      // Report to custom analytics
      if (typeof window !== 'undefined' && (window as any).analytics) {
        (window as any).analytics.track('Error Boundary Triggered', {
          correlationId: errorData.correlationId,
          errorType: errorData.error.name,
          errorMessage: errorData.error.message,
        })
      }
    } catch (reportingError) {
      console.warn('Failed to report error:', reportingError)
    }
  }

  private resetErrorBoundary = () => {
    this.setState({ hasError: false, error: undefined, errorInfo: undefined, eventId: undefined })
  }

  private handleRetry = () => {
    this.resetErrorBoundary()
  }

  private handleReload = () => {
    window.location.reload()
  }

  private copyErrorDetails = () => {
    const { error, errorInfo, eventId } = this.state

    const errorDetails = {
      correlationId: eventId,
      timestamp: new Date().toISOString(),
      error: error?.message,
      stack: error?.stack,
      componentStack: errorInfo?.componentStack,
      url: window.location.href,
    }

    navigator.clipboard.writeText(JSON.stringify(errorDetails, null, 2))
      .then(() => {
        alert('Error details copied to clipboard')
      })
      .catch(() => {
        console.log('Error details:', errorDetails)
        alert('Error details logged to console')
      })
  }

  render() {
    const { hasError, error, eventId } = this.state
    const { children, fallback } = this.props

    if (hasError) {
      if (fallback) {
        return fallback
      }

      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
          <div className="max-w-md w-full space-y-8">
            <div className="text-center">
              <div className="mx-auto h-16 w-16 bg-red-100 rounded-full flex items-center justify-center">
                <ExclamationTriangleIcon className="h-8 w-8 text-red-600" />
              </div>

              <h2 className="mt-6 text-3xl font-extrabold text-gray-900">
                Something went wrong
              </h2>

              <p className="mt-2 text-sm text-gray-600">
                An unexpected error occurred. Our team has been notified.
              </p>

              {eventId && (
                <p className="mt-2 text-xs text-gray-500 font-mono">
                  Error ID: {eventId}
                </p>
              )}
            </div>

            <div className="space-y-4">
              <button
                onClick={this.handleRetry}
                className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              >
                Try Again
              </button>

              <button
                onClick={this.handleReload}
                className="group relative w-full flex justify-center py-2 px-4 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
              >
                Reload Page
              </button>

              {process.env.NODE_ENV === 'development' && (
                <details className="mt-4">
                  <summary className="cursor-pointer text-sm text-gray-600 hover:text-gray-800">
                    Error Details (Development)
                  </summary>
                  <div className="mt-2 p-4 bg-gray-100 rounded-md">
                    <pre className="text-xs text-gray-800 whitespace-pre-wrap overflow-auto max-h-40">
                      {error?.stack}
                    </pre>
                    <button
                      onClick={this.copyErrorDetails}
                      className="mt-2 text-xs text-primary-600 hover:text-primary-800 underline"
                    >
                      Copy Error Details
                    </button>
                  </div>
                </details>
              )}
            </div>

            <div className="text-center">
              <p className="text-xs text-gray-500">
                If this problem persists, please{' '}
                <a
                  href="mailto:support@example.com"
                  className="text-primary-600 hover:text-primary-800 underline"
                >
                  contact support
                </a>
              </p>
            </div>
          </div>
        </div>
      )
    }

    return children
  }
}

// Hook for functional components to use error boundaries
export const useErrorHandler = () => {
  return (error: Error, errorInfo?: ErrorInfo) => {
    // Generate correlation ID
    const eventId = `hook_error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

    // Log error with correlation
    console.group(`🚨 Error Handler [${eventId}]`)
    console.error('Error:', error)
    if (errorInfo) {
      console.error('Error Info:', errorInfo)
    }
    console.groupEnd()

    // Re-throw to be caught by nearest error boundary
    throw error
  }
}

// Specialized error boundaries for different contexts
export const APIErrorBoundary: React.FC<{ children: ReactNode }> = ({ children }) => (
  <EnhancedErrorBoundary
    fallback={
      <div className="p-4 bg-red-50 border border-red-200 rounded-md">
        <div className="flex">
          <ExclamationTriangleIcon className="h-5 w-5 text-red-400" />
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">
              API Error
            </h3>
            <p className="mt-1 text-sm text-red-700">
              Failed to load data. Please try refreshing the page.
            </p>
          </div>
        </div>
      </div>
    }
  >
    {children}
  </EnhancedErrorBoundary>
)

export const ComponentErrorBoundary: React.FC<{
  children: ReactNode
  componentName?: string
}> = ({ children, componentName = 'Component' }) => (
  <EnhancedErrorBoundary
    fallback={
      <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-md">
        <div className="flex">
          <ExclamationTriangleIcon className="h-5 w-5 text-yellow-400" />
          <div className="ml-3">
            <h3 className="text-sm font-medium text-yellow-800">
              {componentName} Error
            </h3>
            <p className="mt-1 text-sm text-yellow-700">
              This component encountered an error. The rest of the page should work normally.
            </p>
          </div>
        </div>
      </div>
    }
  >
    {children}
  </EnhancedErrorBoundary>
)

export default EnhancedErrorBoundary