import React from 'react'
import { clsx } from 'clsx'
import {
  CheckCircleIcon,
  ExclamationTriangleIcon,
  ArrowPathIcon,
  XMarkIcon,
} from '@heroicons/react/24/outline'
import type { OptimisticUpdate } from '../../hooks/useOptimisticUpdates'

interface OptimisticIndicatorProps {
  status: 'pending' | 'confirmed' | 'failed' | 'rolledBack'
  className?: string
  size?: 'sm' | 'md' | 'lg'
}

export const OptimisticIndicator: React.FC<OptimisticIndicatorProps> = ({
  status,
  className,
  size = 'md'
}) => {
  const sizeClasses = {
    sm: 'h-3 w-3',
    md: 'h-4 w-4',
    lg: 'h-5 w-5'
  }

  const getStatusConfig = () => {
    switch (status) {
      case 'pending':
        return {
          icon: ArrowPathIcon,
          color: 'text-blue-500',
          animate: 'animate-spin',
          title: 'Processing...'
        }
      case 'confirmed':
        return {
          icon: CheckCircleIcon,
          color: 'text-green-500',
          title: 'Confirmed'
        }
      case 'failed':
        return {
          icon: ExclamationTriangleIcon,
          color: 'text-red-500',
          title: 'Failed'
        }
      case 'rolledBack':
        return {
          icon: XMarkIcon,
          color: 'text-gray-500',
          title: 'Rolled back'
        }
    }
  }

  const config = getStatusConfig()
  const Icon = config.icon

  return (
    <Icon
      className={clsx(
        sizeClasses[size],
        config.color,
        config.animate,
        className
      )}
      title={config.title}
      data-testid={`optimistic-indicator-${status}`}
    />
  )
}

interface OptimisticBadgeProps {
  status: 'pending' | 'confirmed' | 'failed' | 'rolledBack'
  className?: string
  showText?: boolean
}

export const OptimisticBadge: React.FC<OptimisticBadgeProps> = ({
  status,
  className,
  showText = true
}) => {
  const getStatusConfig = () => {
    switch (status) {
      case 'pending':
        return {
          text: 'Processing',
          bg: 'bg-blue-100',
          textColor: 'text-blue-800',
          border: 'border-blue-200'
        }
      case 'confirmed':
        return {
          text: 'Saved',
          bg: 'bg-green-100',
          textColor: 'text-green-800',
          border: 'border-green-200'
        }
      case 'failed':
        return {
          text: 'Failed',
          bg: 'bg-red-100',
          textColor: 'text-red-800',
          border: 'border-red-200'
        }
      case 'rolledBack':
        return {
          text: 'Reverted',
          bg: 'bg-gray-100',
          textColor: 'text-gray-800',
          border: 'border-gray-200'
        }
    }
  }

  const config = getStatusConfig()

  return (
    <span
      className={clsx(
        'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium border',
        config.bg,
        config.textColor,
        config.border,
        className
      )}
    >
      <OptimisticIndicator status={status} size="sm" className="mr-1.5" />
      {showText && config.text}
    </span>
  )
}

interface OptimisticListItemProps {
  children: React.ReactNode
  status: 'pending' | 'confirmed' | 'failed' | 'rolledBack'
  onRetry?: () => void
  onCancel?: () => void
  className?: string
}

export const OptimisticListItem: React.FC<OptimisticListItemProps> = ({
  children,
  status,
  onRetry,
  onCancel,
  className
}) => {
  const getItemClasses = () => {
    switch (status) {
      case 'pending':
        return 'bg-blue-50 border-blue-200 opacity-75'
      case 'confirmed':
        return 'bg-white border-gray-200'
      case 'failed':
        return 'bg-red-50 border-red-200'
      case 'rolledBack':
        return 'bg-gray-50 border-gray-300 opacity-50'
    }
  }

  return (
    <div
      className={clsx(
        'relative border rounded-lg p-4 transition-all duration-200',
        getItemClasses(),
        className
      )}
      data-testid="optimistic-list-item"
    >
      {/* Status indicator */}
      <div className="absolute top-2 right-2">
        <OptimisticBadge status={status} />
      </div>

      {/* Content */}
      <div className={clsx({ 'pr-20': status !== 'confirmed' })}>
        {children}
      </div>

      {/* Action buttons for failed items */}
      {status === 'failed' && (onRetry || onCancel) && (
        <div className="absolute top-2 right-20 flex space-x-1">
          {onRetry && (
            <button
              onClick={onRetry}
              className="text-xs text-red-600 hover:text-red-800 underline hover:no-underline"
              title="Retry operation"
            >
              Retry
            </button>
          )}
          {onCancel && (
            <button
              onClick={onCancel}
              className="text-xs text-gray-600 hover:text-gray-800 underline hover:no-underline"
              title="Cancel operation"
            >
              Cancel
            </button>
          )}
        </div>
      )}
    </div>
  )
}

interface OptimisticOverlayProps {
  isActive: boolean
  pendingCount: number
  failedCount: number
  className?: string
}

export const OptimisticOverlay: React.FC<OptimisticOverlayProps> = ({
  isActive,
  pendingCount,
  failedCount,
  className
}) => {
  if (!isActive) return null

  return (
    <div
      className={clsx(
        'fixed top-4 right-4 z-50 bg-white border border-gray-200 rounded-lg shadow-lg p-3',
        className
      )}
    >
      <div className="flex items-center space-x-3">
        {pendingCount > 0 && (
          <div className="flex items-center text-blue-600">
            <ArrowPathIcon className="h-4 w-4 mr-1 animate-spin" />
            <span className="text-sm font-medium">
              {pendingCount} processing
            </span>
          </div>
        )}

        {failedCount > 0 && (
          <div className="flex items-center text-red-600">
            <ExclamationTriangleIcon className="h-4 w-4 mr-1" />
            <span className="text-sm font-medium">
              {failedCount} failed
            </span>
          </div>
        )}
      </div>
    </div>
  )
}

interface OptimisticProgressProps {
  total: number
  completed: number
  failed: number
  className?: string
}

export const OptimisticProgress: React.FC<OptimisticProgressProps> = ({
  total,
  completed,
  failed,
  className
}) => {
  const completedPercentage = total > 0 ? (completed / total) * 100 : 0
  const failedPercentage = total > 0 ? (failed / total) * 100 : 0

  return (
    <div className={clsx('w-full', className)}>
      {/* Progress bar */}
      <div className="w-full bg-gray-200 rounded-full h-2 overflow-hidden">
        <div
          className="bg-green-500 h-full transition-all duration-300"
          style={{ width: `${completedPercentage}%` }}
          data-testid="optimistic-progress-completed"
        />
        <div
          className="bg-red-500 h-full transition-all duration-300"
          style={{ width: `${failedPercentage}%`, marginTop: '-8px' }}
          data-testid="optimistic-progress-failed"
        />
      </div>

      {/* Stats */}
      <div className="flex justify-between items-center mt-2 text-xs text-gray-600">
        <span>{completed} completed</span>
        <span>{total - completed - failed} pending</span>
        <span className="text-red-600">{failed} failed</span>
      </div>
    </div>
  )
}
