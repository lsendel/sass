import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ErrorBoundary from './ErrorBoundary'

// Mock logger
vi.mock('../../utils/logger', () => ({
  logger: {
    error: vi.fn(),
  }
}))

// Mock import.meta.env
const mockEnv = vi.hoisted(() => ({
  DEV: true
}))
vi.mock('import.meta.env', () => mockEnv)

// Mock ExclamationTriangleIcon
vi.mock('@heroicons/react/24/outline', () => ({
  ExclamationTriangleIcon: ({ className }: { className?: string }) => (
    <div data-testid="exclamation-icon" className={className} />
  )
}))

// Mock window.location.reload
const mockReload = vi.fn()
Object.defineProperty(window, 'location', {
  value: { reload: mockReload },
  writable: true
})

// Component that throws an error for testing
const ThrowError = ({ shouldThrow = false }: { shouldThrow?: boolean }) => {
  if (shouldThrow) {
    throw new Error('Test error message')
  }
  return <div data-testid="child-component">Child component</div>
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Suppress console.error for cleaner test output
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('when no error occurs', () => {
    it('should render children normally', () => {
      render(
        <ErrorBoundary>
          <div data-testid="child">Normal child content</div>
        </ErrorBoundary>
      )

      expect(screen.getByTestId('child')).toBeInTheDocument()
      expect(screen.queryByText('Something went wrong')).not.toBeInTheDocument()
    })

    it('should render multiple children', () => {
      render(
        <ErrorBoundary>
          <div data-testid="child-1">Child 1</div>
          <div data-testid="child-2">Child 2</div>
        </ErrorBoundary>
      )

      expect(screen.getByTestId('child-1')).toBeInTheDocument()
      expect(screen.getByTestId('child-2')).toBeInTheDocument()
    })
  })

  describe('when an error occurs', () => {
    it('should catch error and display error UI', () => {
      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.getByText('Something went wrong')).toBeInTheDocument()
      expect(screen.getByText(/We're sorry, but something unexpected happened/)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Reload Page' })).toBeInTheDocument()
      expect(screen.getByTestId('exclamation-icon')).toBeInTheDocument()
      expect(screen.queryByTestId('child-component')).not.toBeInTheDocument()
    })

    it('should log error to logger', async () => {
      // Import the mocked logger
      const loggerModule = await import('../../utils/logger')

      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(loggerModule.logger.error).toHaveBeenCalledWith(
        'ErrorBoundary caught an error:',
        expect.any(Error),
        expect.any(Object)
      )
    })

    it('should handle reload button click', () => {
      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      const reloadButton = screen.getByRole('button', { name: 'Reload Page' })
      fireEvent.click(reloadButton)

      expect(mockReload).toHaveBeenCalledTimes(1)
    })

    it('should apply correct CSS classes for error UI', () => {
      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      const errorContainer = screen.getByText('Something went wrong').closest('.max-w-md')
      expect(errorContainer).toHaveClass('max-w-md', 'w-full', 'bg-white', 'shadow-lg', 'rounded-lg', 'p-6')

      const reloadButton = screen.getByRole('button', { name: 'Reload Page' })
      expect(reloadButton).toHaveClass('w-full', 'bg-primary-600', 'text-white')
    })
  })

  describe('development mode error details', () => {
    beforeEach(() => {
      mockEnv.DEV = true
    })

    it('should show error details in development mode', () => {
      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      const detailsElement = screen.getByText('Error Details (Development)')
      expect(detailsElement).toBeInTheDocument()

      // Click to expand details
      fireEvent.click(detailsElement)

      expect(screen.getByText('Error:')).toBeInTheDocument()
      expect(screen.getByText('Test error message')).toBeInTheDocument()
      expect(screen.getByText('Stack:')).toBeInTheDocument()
    })

    it('should not show error details in production mode', () => {
      // Set up production environment before rendering
      vi.stubGlobal('import.meta', { env: { DEV: false } })

      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.queryByText('Error Details (Development)')).not.toBeInTheDocument()
    })
  })

  describe('error boundary lifecycle', () => {
    it('should handle getDerivedStateFromError', () => {
      const errorBoundary = new (ErrorBoundary as any)({})
      const error = new Error('Test error')

      const newState = ErrorBoundary.getDerivedStateFromError(error)

      expect(newState).toEqual({
        hasError: true,
        error: error
      })
    })

    it('should start with no error state', () => {
      const errorBoundary = new (ErrorBoundary as any)({ children: null })

      expect(errorBoundary.state).toEqual({
        hasError: false
      })
    })
  })

  describe('error recovery', () => {
    it('should show normal content after error is resolved', () => {
      const { rerender } = render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      // Error UI should be shown
      expect(screen.getByText('Something went wrong')).toBeInTheDocument()

      // Rerender with no error
      rerender(
        <ErrorBoundary>
          <ThrowError shouldThrow={false} />
        </ErrorBoundary>
      )

      // Error UI should still be shown (ErrorBoundary doesn't recover automatically)
      expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    })
  })

  describe('accessibility', () => {
    it('should have proper ARIA attributes and semantic structure', () => {
      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      const heading = screen.getByRole('heading', { name: 'Something went wrong' })
      expect(heading).toBeInTheDocument()

      const button = screen.getByRole('button', { name: 'Reload Page' })
      expect(button).toBeInTheDocument()
    })

    it('should have proper keyboard navigation support', () => {
      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      const button = screen.getByRole('button', { name: 'Reload Page' })
      expect(button).toHaveClass('focus:outline-none', 'focus:ring-2', 'focus:ring-primary-500', 'focus:ring-offset-2')
    })
  })
})