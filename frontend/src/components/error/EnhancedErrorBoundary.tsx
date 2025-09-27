import React, { Component, ErrorInfo, ReactNode } from 'react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

// Error types that can be handled differently
interface ErrorDetails {
  name: string;
  message: string;
  stack?: string;
  componentStack?: string;
  correlationId?: string;
  timestamp: number;
  userAgent: string;
  url: string;
  userId?: string;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorDetails: ErrorDetails | null;
  errorId: string | null;
  retryCount: number;
}

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: (error: ErrorDetails, retry: () => void) => ReactNode;
  onError?: (error: ErrorDetails) => void;
  maxRetries?: number;
  enableErrorReporting?: boolean;
}

// Error reporting service
class ErrorReportingService {
  private static correlationId: string | null = null;

  static setCorrelationId(id: string): void {
    this.correlationId = id;
  }

  static async reportError(errorDetails: ErrorDetails): Promise<void> {
    try {
      // Report to your error tracking service (e.g., Sentry, LogRocket, etc.)
      if (import.meta.env.PROD) {
        await fetch('/api/v1/errors', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Correlation-ID': this.correlationId ?? crypto.randomUUID(),
          },
          body: JSON.stringify(errorDetails),
        });
      } else {
        console.error('Error Boundary Report:', errorDetails);
      }
    } catch (reportingError) {
      console.error('Failed to report error:', reportingError);
    }
  }

  static generateErrorId(): string {
    return `err_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }
}

export class EnhancedErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  private retryTimeouts: NodeJS.Timeout[] = [];

  constructor(props: ErrorBoundaryProps) {
    super(props);

    this.state = {
      hasError: false,
      error: null,
      errorDetails: null,
      errorId: null,
      retryCount: 0,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return {
      hasError: true,
      error,
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    const errorDetails: ErrorDetails = {
      name: error.name,
      message: error.message,
      stack: error.stack,
      componentStack: errorInfo.componentStack || undefined,
      correlationId: this.generateCorrelationId(),
      timestamp: Date.now(),
      userAgent: navigator.userAgent,
      url: window.location.href,
      userId: this.getCurrentUserId(),
    };

    const errorId = ErrorReportingService.generateErrorId();

    this.setState({
      errorDetails,
      errorId,
    });

    // Report error if enabled
    if (this.props.enableErrorReporting !== false) {
      ErrorReportingService.reportError(errorDetails)
        .catch(reportingError => {
          console.error('Error reporting failed:', reportingError);
        });
    }

    // Call custom error handler
    this.props.onError?.(errorDetails);

    // Log to console in development
    if (import.meta.env.DEV) {
      console.group('🚨 Error Boundary Caught Error');
      console.error('Error:', error);
      console.error('Component Stack:', errorInfo.componentStack);
      console.error('Error Details:', errorDetails);
      console.groupEnd();
    }
  }

  componentWillUnmount(): void {
    // Clean up any pending retry timeouts
    this.retryTimeouts.forEach(timeout => clearTimeout(timeout));
  }

  private generateCorrelationId(): string {
    return crypto.randomUUID();
  }

  private getCurrentUserId(): string | undefined {
    // Get from your auth store/context
    try {
      const userData = localStorage.getItem('user');
      if (userData) {
        const user = JSON.parse(userData) as { id?: string };
        return user.id;
      }
    } catch {
      // Ignore parsing errors
    }
    return undefined;
  }

  private handleRetry = (): void => {
    const maxRetries = this.props.maxRetries ?? 3;

    if (this.state.retryCount >= maxRetries) {
      return;
    }

    // Add exponential backoff
    const delay = Math.pow(2, this.state.retryCount) * 1000;

    const timeout = setTimeout(() => {
      this.setState(prevState => ({
        hasError: false,
        error: null,
        errorDetails: null,
        errorId: null,
        retryCount: prevState.retryCount + 1,
      }));
    }, delay);

    this.retryTimeouts.push(timeout);
  };

  private handleReload = (): void => {
    window.location.reload();
  };

  private handleReportIssue = (): void => {
    const { errorDetails, errorId } = this.state;

    if (!errorDetails || !errorId) return;

    const issueBody = `
Error ID: ${errorId}
Correlation ID: ${errorDetails.correlationId}
Timestamp: ${new Date(errorDetails.timestamp).toISOString()}
URL: ${errorDetails.url}
User Agent: ${errorDetails.userAgent}

Error: ${errorDetails.name}: ${errorDetails.message}

Stack Trace:
${errorDetails.stack}

Component Stack:
${errorDetails.componentStack}
    `.trim();

    const githubUrl = `https://github.com/your-org/your-repo/issues/new?title=${encodeURIComponent(
      `Error: ${errorDetails.name} - ${errorDetails.message}`
    )}&body=${encodeURIComponent(issueBody)}`;

    window.open(githubUrl, '_blank');
  };

  render(): ReactNode {
    const { hasError, errorDetails, errorId, retryCount } = this.state;
    const { children, fallback, maxRetries = 3 } = this.props;

    if (hasError && errorDetails) {
      // Use custom fallback if provided
      if (fallback) {
        return fallback(errorDetails, this.handleRetry);
      }

      // Default error UI
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
          <Card className="w-full max-w-md">
            <CardHeader className="text-center">
              <div className="mx-auto mb-4 h-12 w-12 text-red-500">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"
                  />
                </svg>
              </div>
              <CardTitle className="text-xl font-semibold text-gray-900">
                Something went wrong
              </CardTitle>
              <CardDescription className="text-gray-600">
                We encountered an unexpected error. Don&apos;t worry, we&apos;ve been notified.
              </CardDescription>
            </CardHeader>

            <CardContent className="space-y-4">
              {/* Error details for development */}
              {import.meta.env.DEV && (
                <div className="bg-gray-100 rounded-lg p-3">
                  <details className="text-sm">
                    <summary className="cursor-pointer font-medium text-gray-700 hover:text-gray-900">
                      Error Details (Development)
                    </summary>
                    <div className="mt-2 space-y-2 text-xs font-mono text-gray-600">
                      <div><strong>Error ID:</strong> {errorId}</div>
                      <div><strong>Correlation ID:</strong> {errorDetails.correlationId}</div>
                      <div><strong>Type:</strong> {errorDetails.name}</div>
                      <div><strong>Message:</strong> {errorDetails.message}</div>
                      <div><strong>Retry Count:</strong> {retryCount}/{maxRetries}</div>
                    </div>
                  </details>
                </div>
              )}

              {/* Action buttons */}
              <div className="flex flex-col space-y-2">
                {retryCount < maxRetries && (
                  <Button onClick={this.handleRetry} className="w-full">
                    Try Again {retryCount > 0 && `(${retryCount}/${maxRetries})`}
                  </Button>
                )}

                <Button
                  variant="outline"
                  onClick={this.handleReload}
                  className="w-full"
                >
                  Reload Page
                </Button>

                {import.meta.env.DEV && (
                  <Button
                    variant="ghost"
                    onClick={this.handleReportIssue}
                    className="w-full text-xs"
                  >
                    Report Issue on GitHub
                  </Button>
                )}
              </div>

              {/* Error ID for user reference */}
              <div className="text-center">
                <p className="text-xs text-gray-500">
                  Error ID: <code className="bg-gray-100 px-1 rounded">{errorId}</code>
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  Reference this ID when contacting support
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      );
    }

    return children;
  }
}

// Higher-order component for easy error boundary wrapping
export function withErrorBoundary<P extends object>(
  Component: React.ComponentType<P>,
  errorBoundaryProps?: Omit<ErrorBoundaryProps, 'children'>
) {
  const WrappedComponent = (props: P) => (
    <EnhancedErrorBoundary {...errorBoundaryProps}>
      <Component {...props} />
    </EnhancedErrorBoundary>
  );

  WrappedComponent.displayName = `withErrorBoundary(${Component.displayName || Component.name})`;

  return WrappedComponent;
}

// Hook for throwing errors that will be caught by error boundaries
export function useErrorHandler() {
  return React.useCallback((error: Error | string) => {
    const errorToThrow = typeof error === 'string' ? new Error(error) : error;

    // Use React's error boundary mechanism
    throw errorToThrow;
  }, []);
}

// Async error handler hook
export function useAsyncErrorHandler() {
  const throwError = useErrorHandler();

  return React.useCallback((asyncFn: () => Promise<void>) => {
    return asyncFn().catch(error => {
      // Convert async errors to sync errors for error boundaries
      setTimeout(() => throwError(error), 0);
    });
  }, [throwError]);
}

// Context for error boundary configuration
export const ErrorBoundaryContext = React.createContext<{
  reportError: (error: ErrorDetails) => void;
  setCorrelationId: (id: string) => void;
}>({
  reportError: ErrorReportingService.reportError,
  setCorrelationId: ErrorReportingService.setCorrelationId,
});

// Provider component
export function ErrorBoundaryProvider({ children }: { children: ReactNode }) {
  const contextValue = React.useMemo(() => ({
    reportError: ErrorReportingService.reportError,
    setCorrelationId: ErrorReportingService.setCorrelationId,
  }), []);

  return (
    <ErrorBoundaryContext.Provider value={contextValue}>
      {children}
    </ErrorBoundaryContext.Provider>
  );
}

// Hook to access error boundary context
export function useErrorBoundary() {
  const context = React.useContext(ErrorBoundaryContext);

  if (!context) {
    throw new Error('useErrorBoundary must be used within an ErrorBoundaryProvider');
  }

  return context;
}
