import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { vi } from 'vitest'

import {
  OptimisticIndicator,
  OptimisticBadge,
  OptimisticListItem,
  OptimisticOverlay,
  OptimisticProgress
} from './OptimisticComponents'

describe('OptimisticComponents', () => {
  describe('OptimisticIndicator', () => {
    it('shows pending status with spinner', () => {
      render(<OptimisticIndicator status="pending" />)

      const icon = screen.getByTestId('optimistic-indicator-pending')
      expect(icon).toBeInTheDocument()
      expect(icon).toHaveClass('animate-spin')
    })

    it('shows confirmed status with check icon', () => {
      render(<OptimisticIndicator status="confirmed" />)

      const icon = screen.getByTestId('optimistic-indicator-confirmed')
      expect(icon).toBeInTheDocument()
      expect(icon).toHaveClass('text-green-500')
    })

    it('shows failed status with warning icon', () => {
      render(<OptimisticIndicator status="failed" />)

      const icon = screen.getByTestId('optimistic-indicator-failed')
      expect(icon).toBeInTheDocument()
      expect(icon).toHaveClass('text-red-500')
    })

    it('shows rolled back status with X icon', () => {
      render(<OptimisticIndicator status="rolledBack" />)

      const icon = screen.getByTestId('optimistic-indicator-rolledBack')
      expect(icon).toBeInTheDocument()
      expect(icon).toHaveClass('text-gray-500')
    })
  })

  describe('OptimisticBadge', () => {
    it('shows pending badge with text', () => {
      render(<OptimisticBadge status="pending" showText={true} />)

      expect(screen.getByText('Processing')).toBeInTheDocument()
    })

    it('shows confirmed badge', () => {
      render(<OptimisticBadge status="confirmed" />)

      expect(screen.getByText('Saved')).toBeInTheDocument()
    })

    it('hides text when showText is false', () => {
      render(<OptimisticBadge status="pending" showText={false} />)

      expect(screen.queryByText('Processing')).not.toBeInTheDocument()
      expect(screen.getByTitle('Processing...')).toBeInTheDocument()
    })
  })

  describe('OptimisticListItem', () => {
    it('renders children with pending status styling', () => {
      render(
        <OptimisticListItem status="pending">
          <div>Test content</div>
        </OptimisticListItem>
      )

      expect(screen.getByText('Test content')).toBeInTheDocument()
      expect(screen.getByText('Processing')).toBeInTheDocument()

      const container = screen.getByTestId('optimistic-list-item')
      expect(container).toHaveClass('bg-blue-50', 'border-blue-200', 'opacity-75')
    })

    it('shows retry and cancel buttons for failed items', () => {
      const mockRetry = vi.fn()
      const mockCancel = vi.fn()

      render(
        <OptimisticListItem
          status="failed"
          onRetry={mockRetry}
          onCancel={mockCancel}
        >
          <div>Failed content</div>
        </OptimisticListItem>
      )

      expect(screen.getByText('Failed content')).toBeInTheDocument()
      expect(screen.getAllByText('Failed', { selector: 'span' })[0]).toBeInTheDocument()

      const retryButton = screen.getByText('Retry')
      const cancelButton = screen.getByText('Cancel')

      expect(retryButton).toBeInTheDocument()
      expect(cancelButton).toBeInTheDocument()

      retryButton.click()
      cancelButton.click()

      expect(mockRetry).toHaveBeenCalled()
      expect(mockCancel).toHaveBeenCalled()
    })

    it('does not show action buttons for confirmed items', () => {
      render(
        <OptimisticListItem status="confirmed">
          <div>Confirmed content</div>
        </OptimisticListItem>
      )

      expect(screen.getByText('Confirmed content')).toBeInTheDocument()
      expect(screen.queryByText('Retry')).not.toBeInTheDocument()
      expect(screen.queryByText('Cancel')).not.toBeInTheDocument()
    })
  })

  describe('OptimisticOverlay', () => {
    it('does not render when inactive', () => {
      render(
        <OptimisticOverlay
          isActive={false}
          pendingCount={0}
          failedCount={0}
        />
      )

      expect(screen.queryByText(/processing/)).not.toBeInTheDocument()
    })

    it('shows pending operations count', () => {
      render(
        <OptimisticOverlay
          isActive={true}
          pendingCount={3}
          failedCount={0}
        />
      )

      expect(screen.getByText('3 processing')).toBeInTheDocument()
    })

    it('shows failed operations count', () => {
      render(
        <OptimisticOverlay
          isActive={true}
          pendingCount={1}
          failedCount={2}
        />
      )

      expect(screen.getByText('1 processing')).toBeInTheDocument()
      expect(screen.getByText('2 failed')).toBeInTheDocument()
    })
  })

  describe('OptimisticProgress', () => {
    it('shows progress with correct percentages', () => {
      render(
        <OptimisticProgress
          total={10}
          completed={6}
          failed={2}
        />
      )

      expect(screen.getByText('6 completed')).toBeInTheDocument()
      expect(screen.getByText('2 pending')).toBeInTheDocument()
      expect(screen.getByText('2 failed')).toBeInTheDocument()

      // Check that progress bars have correct widths
      const completedBar = screen.getByTestId('optimistic-progress-completed')
      const failedBar = screen.getByTestId('optimistic-progress-failed')

      expect(completedBar).toHaveStyle('width: 60%')
      expect(failedBar).toHaveStyle('width: 20%')
    })

    it('handles zero total gracefully', () => {
      render(
        <OptimisticProgress
          total={0}
          completed={0}
          failed={0}
        />
      )

      expect(screen.getByText('0 completed')).toBeInTheDocument()
      expect(screen.getByText('0 pending')).toBeInTheDocument()
      expect(screen.getByText('0 failed')).toBeInTheDocument()
    })
  })
})
