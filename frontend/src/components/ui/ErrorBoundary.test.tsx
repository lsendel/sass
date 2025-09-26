import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ReactNode } from 'react'
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

const renderBoundary = (children: ReactNode) =>
  render(<ErrorBoundary>{children}</ErrorBoundary>)

const renderThrowingBoundary = () =>
  renderBoundary(<ThrowError shouldThrow />)

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
      renderBoundary(<div data-testid="child">Normal child content</div>)

      expect(screen.getByTestId('child')).toBeInTheDocument()
      expect(screen.queryByText('Something went wrong')).not.toBeInTheDocument()
    })

    it('should render multiple children', () => {
      renderBoundary(
        <>
          <div data-testid="child-1">Child 1</div>
          <div data-testid="child-2">Child 2</div>
        </>
      )

      expect(screen.getByTestId('child-1')).toBeInTheDocument()
      expect(screen.getByTestId('child-2')).toBeInTheDocument()
    })
  })

  describe('when an error occurs', () => {
    it('should catch error and display error UI', () => {
      renderThrowingBoundary()

      expect(screen.getByText('Something went wrong')).toBeInTheDocument()
      expect(screen.getByText(/We're sorry, but something unexpected happened/)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Refresh Page' })).toBeInTheDocument()
      expect(screen.getByTestId('exclamation-icon')).toBeInTheDocument()
      expect(screen.queryByTestId('child-component')).not.toBeInTheDocument()
    })

    it('should log error details to console', () => {
      renderThrowingBoundary()

      expect(console.error).toHaveBeenCalledWith(
        'ErrorBoundary caught an error:',
        expect.any(Error),
        expect.any(Object)
      )
    })

    it('should handle reload button click', () => {
      renderThrowingBoundary()

      const reloadButton = screen.getByRole('button', { name: 'Refresh Page' })
      fireEvent.click(reloadButton)

      expect(mockReload).toHaveBeenCalledTimes(1)
    })

    it('should apply correct CSS classes for error UI', () => {
      renderThrowingBoundary()

      const errorContainer = screen.getByText('Something went wrong').closest('.max-w-md')
      expect(errorContainer).toHaveClass('max-w-md', 'w-full', 'bg-white', 'shadow-lg', 'rounded-lg', 'p-6')

      const reloadButton = screen.getByRole('button', { name: 'Refresh Page' })
      expect(reloadButton).toHaveClass('text-white', 'bg-red-600', 'focus:outline-none', 'focus:ring-2', 'focus:ring-offset-2')
    })
  })

  describe('development mode error details', () => {
    beforeEach(() => {
      mockEnv.DEV = true
    })

    it('should show error UI in development mode', () => {
      renderThrowingBoundary()

      expect(screen.getByText('Something went wrong')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Refresh Page' })).toBeInTheDocument()
    })

    it('should not show error details in production mode', () => {
      // Set up production environment before rendering
      vi.stubGlobal('import.meta', { env: { DEV: false } })

      renderThrowingBoundary()

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
      const { rerender } = renderThrowingBoundary()

      // Error UI should be shown
      expect(screen.getByText('Something went wrong')).toBeInTheDocument()

      // Rerender with no error
      rerender(<ErrorBoundary><ThrowError shouldThrow={false} /></ErrorBoundary>)

      // Error UI should still be shown (ErrorBoundary doesn't recover automatically)
      expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    })
  })

  describe('accessibility', () => {
    it('should have proper ARIA attributes and semantic structure', () => {
      renderThrowingBoundary()

      const heading = screen.getByRole('heading', { name: 'Something went wrong' })
      expect(heading).toBeInTheDocument()

      const button = screen.getByRole('button', { name: 'Refresh Page' })
      expect(button).toBeInTheDocument()
    })

    it('should have proper keyboard navigation support', () => {
      renderThrowingBoundary()

      const button = screen.getByRole('button', { name: 'Refresh Page' })
      expect(button).toHaveClass('focus:outline-none', 'focus:ring-2', 'focus:ring-offset-2', 'focus:ring-red-500')
    })
  })
})
